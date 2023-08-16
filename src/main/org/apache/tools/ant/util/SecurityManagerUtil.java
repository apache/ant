/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.tools.ant.util;

import org.apache.tools.ant.MagicNames;
import org.apache.tools.ant.Project;

/**
 * @since Ant 1.10.14
 */
public final class SecurityManagerUtil {

    private static final boolean isJava18OrHigher = JavaEnvUtils.isAtLeastJavaVersion("18");
    private static final boolean sysPropWarnOnSecMgrUsage =
            Boolean.getBoolean(MagicNames.WARN_SECURITY_MANAGER_USAGE);

    /**
     * {@return true if {@code SecurityManager} usage is allowed in current Java runtime. false
     * otherwise}
     */
    public static boolean isSetSecurityManagerAllowed() {
        if (isJava18OrHigher) {
            return false;
        }
        return true;
    }

    /**
     * {@return true if {@code SecurityManager} usage should only be logged as a warning. false
     * otherwise}
     */
    public static boolean warnOnSecurityManagerUsage(final Project project) {
        if (project == null) {
            return sysPropWarnOnSecMgrUsage;
        }
        final String val = project.getProperty(MagicNames.WARN_SECURITY_MANAGER_USAGE);
        if (val == null) {
            return sysPropWarnOnSecMgrUsage;
        }
        return Boolean.parseBoolean(val);
    }
}
