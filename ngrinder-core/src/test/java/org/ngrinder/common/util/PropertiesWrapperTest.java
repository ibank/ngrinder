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
package org.ngrinder.common.util;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

import java.util.Properties;

import org.junit.Test;

/**
 * Class description.
 * 
 * @author Mavlarn
 * @since
 */
public class PropertiesWrapperTest {

	/**
	 * Test method for
	 * {@link org.ngrinder.common.util.PropertiesWrapper#PropertiesWrapper(java.util.Properties)}.
	 */
	@Test
	public void testPropertiesWrapper() {
		Properties prop = new Properties();
		prop.put("key1", "1");
		prop.put("key2", "value2");
		PropertiesWrapper propWrapper = new PropertiesWrapper(prop);

		propWrapper.addProperty("key3", "3");
		propWrapper.addProperty("key4", "value4");

		int value1 = propWrapper.getPropertyInt("key1", 0);
		assertThat(value1, is(1));
		int value3 = propWrapper.getPropertyInt("key3", 0);
		assertThat(value3, is(3));
		int noValue = propWrapper.getPropertyInt("NoValueKey", 0);
		assertThat(noValue, is(0));

		String value2 = propWrapper.getProperty("key2", "null");
		assertThat(value2, is("value2"));
		String value4 = propWrapper.getProperty("key4", "null");
		assertThat(value4, is("value4"));
		String nullValueStr = propWrapper.getProperty("NoValueKey", "null");
		assertThat(nullValueStr, is("null"));

		String newValue4 = propWrapper.getProperty("key4", "No value found for:{}");
		assertThat(newValue4, is("value4"));
		nullValueStr = propWrapper.getProperty("NoValueKey", "null", "No value found for:{}");
		assertThat(nullValueStr, is("null"));

		boolean boolVal = propWrapper.getPropertyBoolean("BoolKey", false);
		assertThat(boolVal, is(false));
		prop.put("BoolKey", "true");
		boolVal = propWrapper.getPropertyBoolean("BoolKey", false);
		assertThat(boolVal, is(true));

	}
}
