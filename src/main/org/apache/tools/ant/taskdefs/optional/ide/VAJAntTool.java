/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2002 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "Ant" and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
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
            VAJLocalUtil.getWorkspace().loadedProjectNamed(
                                                           info.getVAJProjectName()).setToolRepositoryData(td);
        } catch (Throwable t) {
            throw new BuildException("BuildInfo for Project "
                                     + info.getVAJProjectName() + " could not be saved", t);
        }
    }
}
