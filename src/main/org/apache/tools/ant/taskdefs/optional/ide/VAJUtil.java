/* 
 * Copyright  2001-2002,2004 Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
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

package org.apache.tools.ant.taskdefs.optional.ide;

import java.io.File;
import java.util.Vector;

/**
 * Helper interface for VAJ tasks. Encapsulates
 * the interface to the VAJ tool API.
 *
 * @author Wolf Siberski, TUI Infotec GmbH
 */
interface VAJUtil {
    // log levels
    static final int MSG_DEBUG = 4;
    static final int MSG_ERR = 0;
    static final int MSG_INFO = 2;
    static final int MSG_VERBOSE = 3;
    static final int MSG_WARN = 1;

    /**
     * export the array of Packages
     */
    void exportPackages(
        File dest,
        String[] includePatterns, String[] excludePatterns,
        boolean exportClasses, boolean exportDebugInfo,
        boolean exportResources, boolean exportSources,
        boolean useDefaultExcludes, boolean overwrite);

    /**
     * Do the import.
     */
    void importFiles(
        String importProject, File srcDir,
        String[] includePatterns, String[] excludePatterns,
        boolean importClasses, boolean importResources,
        boolean importSources, boolean useDefaultExcludes);

    /**
     * Load specified projects.
     */
    void loadProjects(Vector projectDescriptions);

    /**
     * Logs a message with the specified log level.
     */
    void log(String msg, int level);
}
