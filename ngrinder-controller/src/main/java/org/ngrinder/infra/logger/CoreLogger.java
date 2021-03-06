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
package org.ngrinder.infra.logger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provide core logger which is always visible in LOG.
 * Even verbose mode is off, LOG reported by this Logger is always shown in log file.
 * 
 * This logger is subject to used to report the major execution steps of each perftest.
 * 
 * @author JunHo Yoon
 * @since 3.0
 */
public abstract class CoreLogger {

	/**
	 * Core logger.
	 */
	public static final Logger LOGGER = LoggerFactory.getLogger(CoreLogger.class);
}
