/*
 * Copyright  2001-2004 Apache Software Foundation
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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Enumeration;
import java.util.Vector;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/**
 * Helper class for VAJ tasks. Holds Workspace singleton and
 * wraps IvjExceptions into BuildExceptions
 *
 * @author Wolf Siberski, TUI Infotec GmbH
 * @author Martin Landers, Beck et al. projects
 */
class VAJRemoteUtil implements VAJUtil {
    // calling task
    Task caller;

    // VAJ remote tool server
    String remoteServer;

    public VAJRemoteUtil(Task caller, String remote) {
        this.caller = caller;
        this.remoteServer = remote;
    }

    /**
     * export the array of Packages
     */
    public void exportPackages(File destDir,
                               String[] includePatterns, String[] excludePatterns,
                               boolean exportClasses, boolean exportDebugInfo,
                               boolean exportResources, boolean exportSources,
                               boolean useDefaultExcludes, boolean overwrite) {
        try {
            String request = "http://" + remoteServer + "/servlet/vajexport?"
                + VAJExportServlet.WITH_DEBUG_INFO + "=" + exportDebugInfo + "&"
                + VAJExportServlet.OVERWRITE_PARAM + "=" + overwrite + "&"
                + assembleImportExportParams(destDir,
                                              includePatterns, excludePatterns,
                                              exportClasses, exportResources,
                                              exportSources, useDefaultExcludes);
            sendRequest(request);
        } catch (Exception ex) {
            throw new BuildException(ex);
        }
    }

    /**
     * Do the import.
     */
    public void importFiles(
                            String importProject, File srcDir,
                            String[] includePatterns, String[] excludePatterns,
                            boolean importClasses, boolean importResources,
                            boolean importSources, boolean useDefaultExcludes) {
        try {
            String request = "http://" + remoteServer + "/servlet/vajimport?"
                + VAJImportServlet.PROJECT_NAME_PARAM + "="
                + importProject + "&"
                + assembleImportExportParams(srcDir,
                                              includePatterns, excludePatterns,
                                              importClasses, importResources,
                                              importSources, useDefaultExcludes);
            sendRequest(request);
        } catch (Exception ex) {
            throw new BuildException(ex);
        }

    }

    /**
     * Assemble string for parameters common for import and export
     * Helper method to remove double code.
     */
    private String assembleImportExportParams(
                                              File dir,
                                              String[] includePatterns, String[] excludePatterns,
                                              boolean includeClasses, boolean includeResources,
                                              boolean includeSources, boolean useDefaultExcludes) {
        String result =
            VAJToolsServlet.DIR_PARAM + "="
            + URLEncoder.encode(dir.getPath()) + "&"
            + VAJToolsServlet.CLASSES_PARAM + "=" + includeClasses + "&"
            + VAJToolsServlet.RESOURCES_PARAM + "=" + includeResources + "&"
            + VAJToolsServlet.SOURCES_PARAM + "=" + includeSources + "&"
            + VAJToolsServlet.DEFAULT_EXCLUDES_PARAM + "=" + useDefaultExcludes;

        if (includePatterns != null) {
            for (int i = 0; i < includePatterns.length; i++) {
                result = result + "&" + VAJExportServlet.INCLUDE_PARAM + "="
                    + URLEncoder.encode(includePatterns[i]);
            }
        }
        if (excludePatterns != null) {
            for (int i = 0; i < excludePatterns.length; i++) {
                result = result + "&" + VAJExportServlet.EXCLUDE_PARAM + "="
                    + URLEncoder.encode(excludePatterns[i]);
            }
        }

        return result;
    }

    /**
     * Load specified projects.
     */
    public void loadProjects(Vector projectDescriptions) {
        try {
            String request = "http://" + remoteServer + "/servlet/vajload?";
            String delimiter = "";
            for (Enumeration e = projectDescriptions.elements(); e.hasMoreElements();) {
                VAJProjectDescription pd = (VAJProjectDescription) e.nextElement();
                request = request
                    + delimiter + VAJLoadServlet.PROJECT_NAME_PARAM
                    + "=" + pd.getName().replace(' ', '+')
                    + "&" + VAJLoadServlet.VERSION_PARAM
                    + "=" + pd.getVersion().replace(' ', '+');
                //the first param needs no delimiter, but all other
                delimiter = "&";
            }
            sendRequest(request);
        } catch (Exception ex) {
            throw new BuildException(ex);
        }
    }

    /**
     * logs a message.
     */
    public void log(String msg, int level) {
        caller.log(msg, level);
    }

    /**
     * Sends a servlet request.
     */
    private void sendRequest(String request) {
        boolean requestFailed = false;
        try {
            log("Request: " + request, MSG_DEBUG);

            //must be HTTP connection
            URL requestUrl = new URL(request);
            HttpURLConnection connection =
                (HttpURLConnection) requestUrl.openConnection();

            InputStream is = null;
            // retry three times
            for (int i = 0; i < 3; i++) {
                try {
                    is = connection.getInputStream();
                    break;
                } catch (IOException ex) {
                    // ignore
                }
            }
            if (is == null) {
                log("Can't get " + request, MSG_ERR);
                throw new BuildException("Couldn't execute " + request);
            }

            // log the response
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line = br.readLine();
            while (line != null) {
                int level = MSG_ERR;
                try {
                    // the first char of each line contains the log level
                    level = Integer.parseInt(line.substring(0, 1));
                    if (level == MSG_ERR) {
                        requestFailed = true;
                    }
                } catch (Exception e) {
                    log("Response line doesn't contain log level!", MSG_ERR);
                }
                log(line.substring(2), level);
                line = br.readLine();
            }

        } catch (IOException ex) {
            log("Error sending tool request to VAJ" + ex, MSG_ERR);
            throw new BuildException("Couldn't execute " + request);
        }
        if (requestFailed) {
            throw new BuildException("VAJ tool request failed");
        }
    }
}
