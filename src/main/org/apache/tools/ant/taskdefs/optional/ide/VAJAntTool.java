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


import com.ibm.ivj.util.base.Project;
import com.ibm.ivj.util.base.ToolData;
import org.apache.tools.ant.BuildException;


/**
 * This class is the equivalent to org.apache.tools.ant.Main for the
 * VAJ tool environment. It's main is called when the user selects
 * Tools->Ant Build from the VAJ project menu.
 * Additionally this class provides methods to save build info for
 * a project in the repository and load it from the repository
 *
 * @author Wolf Siberski
 */
public class VAJAntTool {
    private static final String TOOL_DATA_KEY = "AntTool";


    /**
     * Loads the BuildInfo for the specified VAJ project from the
     * tool data for this project.
     * If there is no build info stored for that project, a new
     * default BuildInfo is returned
     *
     * @return BuildInfo buildInfo build info for the specified project
     * @param projectName String project name
     */
    public static VAJBuildInfo loadBuildData(String projectName) {
        VAJBuildInfo result = null;
        try {
            Project project =
                VAJLocalUtil.getWorkspace().loadedProjectNamed(projectName);
            if (project.testToolRepositoryData(TOOL_DATA_KEY)) {
                ToolData td = project.getToolRepositoryData(TOOL_DATA_KEY);
                String data = (String) td.getData();
                result = VAJBuildInfo.parse(data);
            } else {
                result = new VAJBuildInfo();
            }
            result.setVAJProjectName(projectName);
        } catch (Throwable t) {
            throw new BuildException("BuildInfo for Project "
                                     + projectName + " could not be loaded" + t);
        }
        return result;
    }


    /**
     * Starts the application.
     *
     * @param args an array of command-line arguments. VAJ puts the
     *             VAJ project name into args[1] when starting the
     *             tool from the project context menu
     */
    public static void main(java.lang.String[] args) {
        try {
            VAJBuildInfo info;
            if (args.length >= 2 && args[1] instanceof String) {
                String projectName = (String) args[1];
                info = loadBuildData(projectName);
            } else {
                info = new VAJBuildInfo();
            }

            VAJAntToolGUI mainFrame = new VAJAntToolGUI(info);
            mainFrame.show();
        } catch (Throwable t) {
            // if all error handling fails, output at least
            // something on the console
            t.printStackTrace();
        }
    }


    /**
     * Saves the BuildInfo for a project in the VAJ repository.
     *
     * @param info BuildInfo build info to save
     */
    public static void saveBuildData(VAJBuildInfo info) {
        String data = info.asDataString();
        try {
            ToolData td = new ToolData(TOOL_DATA_KEY, data);
            VAJLocalUtil.getWorkspace().loadedProjectNamed(info.getVAJProjectName()).setToolRepositoryData(td);
        } catch (Throwable t) {
            throw new BuildException("BuildInfo for Project "
                                     + info.getVAJProjectName() + " could not be saved", t);
        }
    }
}
