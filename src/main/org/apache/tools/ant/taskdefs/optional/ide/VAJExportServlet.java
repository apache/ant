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
 * A Remote Access to Tools Servlet to extract package
 * sets from the Workbench to the local file system.
 * The following table describes the servlet parameters.
 *
 * <table border="1">
 *   <tr>
 *     <td><strong>Parameter</strong></td>
 *     <td><strong>Values</strong></td>
 *     <td><strong>Description</strong></td>
 *   </tr>
 *   <tr>
 *     <td>dir</td>
 *     <td>Any valid directory name on the server.</td>
 *     <td>The directory to export the files to on the machine
 *         where the servlet is being run.  If the directory
 *         doesn't exist, it will be created.<p>
 *         Relative paths are relative to
 *         IBMVJava/ide/tools/com-ibm-ivj-toolserver,
 *         where IBMVJava is the VisualAge for Java installation
 *         directory.</td>
 *   </tr>
 *   <tr>
 *     <td>include</td>
 *     <td>See below.</td>
 *     <td>The pattern used to indicate which projects and
 *         packages to export.</td>
 *   </tr>
 *   <tr>
 *     <td>exclude</td>
 *     <td>See below</td>
 *     <td>The pattern used to indicate which projects and
 *         packages <em>not</em> to export.</td>
 *   </tr>
 *   <tr>
 *     <td>cls</td>
 *     <td>"yes" or "no" (without the quotes)</td>
 *     <td>Export class files.  Defaults to "no".</td>
 *   </tr>
 *   <tr>
 *     <td>src</td>
 *     <td>"yes" or "no" (without the quotes)</td>
 *     <td>Export source files.  Defaults to "yes".</td>
 *   </tr>
 *   <tr>
 *     <td>res</td>
 *     <td>"yes" or "no" (without the quotes)</td>
 *     <td>Export resource files associated with the included project(s).
 *         Defaults to "yes".</td>
 *   </tr>
 *   <tr>
 *     <td>dex</td>
 *     <td>"yes" or "no" (without the quotes)</td>
 *     <td>Use the default exclusion patterns.  Defaults to "yes".
 *         See below for an explanation of default excludes.</td>
 *   </tr>
 *   <tr>
 *     <td>owr</td>
 *     <td>"yes" or "no" (without the quotes)</td>
 *     <td>Overwrite any existing files.  Defaults to "yes".</td>
 *   </tr>
 * </table>
 *
 * <p>The vajexport servlet uses include and exclude parameters to form
 *    the criteria for selecting packages to export. The parameter is
 *    broken up into ProjectName/packageNameSegments, where ProjectName
 *    is what you expect, and packageNameSegments is a partial (or complete)
 *    package name, separated by forward slashes, rather than periods.
 *    Each packageNameSegment can have wildcard characters.</p>
 *
 * <table border="1">
 *   <tr>
 *     <td><strong>Wildcard Characters</strong></td>
 *     <td><strong>Description</strong></td>
 *   </tr>
 *   <tr>
 *     <td>*</td>
 *     <td>Match zero or more characters in that segment.</td>
 *   </tr>
 *   <tr>
 *     <td>?</td>
 *     <td>Match one character in that segment.</td>
 *   </tr>
 *   <tr>
 *     <td>**</td>
 *     <td>Matches all characters in zero or more segments.</td>
 *   </tr>
 * </table>
 *
 * @author Wolf Siberski, based on servlets written by Glenn McAllister
 */
public class VAJExportServlet extends VAJToolsServlet {
    // constants for servlet param names
    public static final String WITH_DEBUG_INFO = "deb";
    public static final String OVERWRITE_PARAM = "owr";

    /**
     * Respond to a request to export packages from the Workbench.
     */
    protected void executeRequest() {
        getUtil().exportPackages(
                         new File(getFirstParamValueString(DIR_PARAM)),
                         getParamValues(INCLUDE_PARAM),
                         getParamValues(EXCLUDE_PARAM),
                         getBooleanParam(CLASSES_PARAM, false),
                         getBooleanParam(WITH_DEBUG_INFO, false),
                         getBooleanParam(RESOURCES_PARAM, true),
                         getBooleanParam(SOURCES_PARAM, true),
                         getBooleanParam(DEFAULT_EXCLUDES_PARAM, true),
                         getBooleanParam(OVERWRITE_PARAM, true));
    }
}
