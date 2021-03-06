/* 
 * Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 */
package org.ngrinder.script.service;

import static org.ngrinder.common.util.Preconditions.checkNotEmpty;
import static org.ngrinder.common.util.Preconditions.checkNotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import net.grinder.engine.agent.LocalScriptTestDriveService;
import net.grinder.util.thread.Condition;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.ngrinder.infra.config.Config;
import org.ngrinder.model.IFileEntry;
import org.ngrinder.model.User;
import org.ngrinder.script.model.FileEntry;
import org.ngrinder.script.model.FileType;
import org.ngrinder.service.IScriptValidationService;
import org.python.core.CompileMode;
import org.python.core.CompilerFlags;
import org.python.core.PySyntaxError;
import org.python.core.PyTuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * Script Validation Service.
 * 
 * @author JunHo Yoon
 * @since 3.0
 */
@Component
public class ScriptValidationService implements IScriptValidationService {

	private static final Logger LOG = LoggerFactory.getLogger(ScriptValidationService.class);
	@Autowired
	private LocalScriptTestDriveService localScriptTestDriveService;

	@Autowired
	private FileEntryService fileEntryService;

	@Autowired
	private Config config;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ngrinder.script.service.IScriptValidationService#validateScript(org.ngrinder.model.User,
	 * org.ngrinder.model.IFileEntry, boolean, java.lang.String)
	 */
	@Override
	public String validateScript(User user, IFileEntry scriptEntry, boolean useScriptInSVN, String hostString) {
		try {
			checkNotNull(scriptEntry, "scriptEntity should be not null");
			checkNotEmpty(scriptEntry.getPath(), "scriptEntity path should be provided");
			if (!useScriptInSVN) {
				checkNotEmpty(scriptEntry.getContent(), "scriptEntity content should be provided");
			}
			checkNotNull(user, "user should be provided");
			String result = checkSyntaxErrors(scriptEntry.getContent());
			if (result != null) {
				return result;
			}
			File scriptDirectory = config.getHome().getScriptDirectory(user);
			FileUtils.deleteDirectory(scriptDirectory);
			scriptDirectory.mkdirs();

			// Copy logback... first
			if (config.getSystemProperties().getPropertyBoolean("ngrinder.dist.logback", true)) {
				InputStream io = null;
				FileOutputStream fos = null;
				try {
					io = new ClassPathResource("/logback/logback-worker.xml").getInputStream();
					fos = new FileOutputStream(new File(scriptDirectory, "logback-worker.xml"));
					IOUtils.copy(io, fos);
				} catch (IOException e) {
					LOG.error("error while writing logback-worker", e);
				} finally {
					IOUtils.closeQuietly(io);
					IOUtils.closeQuietly(fos);
				}
			}

			String basePath = FilenameUtils.getPath(scriptEntry.getPath());

			// Get all lib and resources in the script path
			List<FileEntry> fileEntries = fileEntryService.getLibAndResourcesEntries(user,
							checkNotEmpty(scriptEntry.getPath()), null);

			// Distribute each files in that folder.
			for (FileEntry each : fileEntries) {
				// Directory is not subject to be distributed.
				if (each.getFileType() == FileType.DIR) {
					continue;
				}
				String path = FilenameUtils.getPath(each.getPath());
				path = path.substring(basePath.length());
				File toDir = new File(scriptDirectory, path);
				fileEntryService.writeContentTo(user, each.getPath(), toDir);
			}

			File scriptFile = new File(scriptDirectory, FilenameUtils.getName(scriptEntry.getPath()));

			if (useScriptInSVN) {
				fileEntryService.writeContentTo(user, scriptEntry.getPath(), scriptDirectory);
			} else {
				FileUtils.writeStringToFile(scriptFile, scriptEntry.getContent(),
								StringUtils.defaultIfBlank(scriptEntry.getEncoding(), "UTF-8"));
			}
			File doValidate = localScriptTestDriveService.doValidate(scriptDirectory, scriptFile, new Condition(),
							config.isSecurityEnabled(), hostString);
			List<String> readLines = FileUtils.readLines(doValidate);
			StringBuffer output = new StringBuffer();
			String path = config.getHome().getDirectory().getAbsolutePath();
			for (String each : readLines) {
				if (!each.startsWith("*sys-package-mgr")) {
					each = each.replace(path, "${NGRINDER_HOME}");
					output.append(each).append("\n");
				}
			}
			return output.toString();
		} catch (IOException e) {
			LOG.error("Error while distributing files on {} for {}", user, scriptEntry.getPath());
			LOG.error("Error details ", e);
		}
		return StringUtils.EMPTY;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ngrinder.script.service.IScriptValidationService#checkSyntaxErrors(java.lang.String)
	 */
	@Override
	public String checkSyntaxErrors(String script) {
		try {
			org.python.core.ParserFacade.parse(script, CompileMode.exec, "unnamed", new CompilerFlags(
							CompilerFlags.PyCF_DONT_IMPLY_DEDENT | CompilerFlags.PyCF_ONLY_AST));

		} catch (PySyntaxError e) {
			try {
				PyTuple pyTuple = (PyTuple) ((PyTuple) e.value).get(1);
				Integer line = (Integer) pyTuple.get(1);
				Integer column = (Integer) pyTuple.get(2);
				String lineString = (String) pyTuple.get(3);
				StringBuilder buf = new StringBuilder(lineString);
				if (lineString.length() >= column) {
					buf.insert(column, "^");
				}
				return "Error occured\n" + " - Invalid Syntax Error on line " + line + " / column " + column + "\n"
								+ buf.toString();
			} catch (Exception ex) {
				return "Error occured while evalation PySyntaxError";
			}
		}
		return null;

	}

}
