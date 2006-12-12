/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.tools.ant.taskdefs.condition;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.util.JavaEnvUtils;
import org.apache.tools.ant.util.ReflectWrapper;
import org.apache.tools.ant.util.StringUtils;

/**
 * &lt;hasfreespace&gt;
 * <p>Condition returns true if selected partition 
 * has the requested space, false otherwise.</p>
 * @since Ant 1.7
 */
public class HasFreeSpace implements Condition {

	private String partition;
	private String needed;
	
	public boolean eval() throws BuildException {
		try {
			if (JavaEnvUtils.isAtLeastJavaVersion("1.6")) {
				//reflection to avoid bootstrap/build problems
				String j6FileUtils = "org.apache.tools.ant.util.java16.Java6FileUtils";
				ReflectWrapper w = new ReflectWrapper(getClass().getClassLoader(), j6FileUtils);
				long free = ((Long)w.invoke("freeSpace", String.class, partition)).longValue();
				return free >= StringUtils.parseHumanSizes(needed);
			} else {
				throw new BuildException("HasFreeSpace condition not supported on Java5 or less.");
			}
		} catch (Exception e) {
			throw new BuildException(e);
		}
	}

	/**
	 * The partition/device to check
	 * @return
	 */
	public String getPartition() {
		return partition;
	}

	public void setPartition(String partition) {
		this.partition = partition;
	}

	/**
	 * The amount of free space required
	 * @return the amount required
	 */
	public String getNeeded() {
		return needed;
	}

	public void setNeeded(String needed) {
		this.needed = needed;
	}
}