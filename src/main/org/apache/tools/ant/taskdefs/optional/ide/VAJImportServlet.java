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


/**
 * A Remote Access to Tools Servlet to import a Project
 * from files into the Repository. The following
 * table describes the servlet parameters.
 *
 * <table>
 *   <tr>
 *     <td>Parameter</td>
 *     <td>Description</td>
 *   </tr>
 *   <tr>
 *     <td>project</td>
 *     <td>The name of the project where you want the imported
 *         items to go.
 *     </td>
 *   </tr>
 *   <tr>
 *     <td>dir</td>
 *     <td>The directory you want to import from.</td>
 *   </tr>
 * </table>
 *
 * @author Wolf Siberski, based on servlets written by Glenn McAllister
 */
public class VAJImportServlet extends VAJToolsServlet {
    /**
     * Respond to a request to import files to the Repository
     */
    protected void executeRequest() {
        getUtil().importFiles(
                      getFirstParamValueString(PROJECT_NAME_PARAM),
                      new File(getFirstParamValueString(DIR_PARAM)),
                      getParamValues(INCLUDE_PARAM),
                      getParamValues(EXCLUDE_PARAM),
                      getBooleanParam(CLASSES_PARAM, false),
                      getBooleanParam(RESOURCES_PARAM, true),
                      getBooleanParam(SOURCES_PARAM, true),
                      false);
        // no default excludes, because they
        // are already added on client side
        // getBooleanParam(DEFAULT_EXCLUDES_PARAM, true)

    }
}
