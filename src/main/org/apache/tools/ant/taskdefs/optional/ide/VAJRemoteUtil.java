/*
 * Copyright  2001-2005 The Apache Software Foundation
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
            String request = "http://" + remoteServer + "/servlet/vajexport";
            Vector parameters = new Vector();
            parameters.addElement(new URLParam(VAJExportServlet.WITH_DEBUG_INFO, exportDebugInfo));
            parameters.addElement(new URLParam(VAJExportServlet.OVERWRITE_PARAM, overwrite));
            assembleImportExportParams(parameters, destDir,
                                       includePatterns, excludePatterns,
                                       exportClasses, exportResources,
                                       exportSources, useDefaultExcludes);
            sendRequest(request, parameters);
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
            String request = "http://" + remoteServer + "/servlet/vajimport";
            Vector parameters = new Vector();
            parameters.addElement(new
                                  URLParam(VAJImportServlet.PROJECT_NAME_PARAM, importProject));
            assembleImportExportParams(parameters, srcDir,
                                       includePatterns, excludePatterns,
                                       importClasses, importResources,
                                       importSources, useDefaultExcludes);
            sendRequest(request, parameters);
        } catch (Exception ex) {
            throw new BuildException(ex);
        }

    }

    /**
     * Add parameters common for import and export to vector
     * Helper method to remove double code.
     */
    private void assembleImportExportParams(Vector parameters,
                                            File dir,
                                            String[] includePatterns, String[] excludePatterns,
                                            boolean includeClasses, boolean includeResources,
                                            boolean includeSources, boolean useDefaultExcludes) {
        parameters.addElement(new URLParam(VAJToolsServlet.DIR_PARAM, dir.getPath()));
        parameters.addElement(new URLParam(VAJToolsServlet.CLASSES_PARAM, includeClasses));
        parameters.addElement(new URLParam(VAJToolsServlet.RESOURCES_PARAM, includeResources));
        parameters.addElement(new URLParam(VAJToolsServlet.SOURCES_PARAM, includeSources));
        parameters.addElement(new URLParam(VAJToolsServlet.DEFAULT_EXCLUDES_PARAM, useDefaultExcludes));

        if (includePatterns != null) {
            for (int i = 0; i < includePatterns.length; i++) {
                parameters.addElement(new
                                      URLParam(VAJExportServlet.INCLUDE_PARAM, includePatterns[i]));
            }
        }
        if (excludePatterns != null) {
            for (int i = 0; i < excludePatterns.length; i++) {
                parameters.addElement(new
                                      URLParam(VAJExportServlet.EXCLUDE_PARAM, excludePatterns[i]));
            }
        }
    }

    /**
     * Load specified projects.
     */
    public void loadProjects(Vector projectDescriptions) {
        try {
            String request = "http://" + remoteServer + "/servlet/vajload";
            Vector parameters = new Vector();
            for (Enumeration e = projectDescriptions.elements(); e.hasMoreElements();) {
                VAJProjectDescription pd = (VAJProjectDescription) e.nextElement();
                parameters.addElement(new
                                      URLParam(VAJLoadServlet.PROJECT_NAME_PARAM, pd.getName()));
                parameters.addElement(new
                                      URLParam(VAJLoadServlet.VERSION_PARAM, pd.getVersion())); 
            }
            sendRequest(request, parameters);
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

    
    private class URLParam {
        private String name;
        private String value;
        public URLParam(String name, String value) {
            this.name = name;
            this.value = value;
        }
        public URLParam(String name, boolean value) {
            this.name = name;
            this.value = (new Boolean(value)).toString();
        }
        public void setValue(String value) { this.value = value; }
        public void setName(String name) { this.name = name; }
        public String getName() { return name; }
        public String getValue() { return value; }
    }
    
    /**
     * Sends a servlet request.
     *
     * The passed URL and parameter list are combined into a
     * valid URL (with proper URL encoding for the parameters)
     * and the URL is requested.
     *
     * @param request Request URL without trailing characters (no ?)
     * @param parameters Vector of URLParam objects to append as parameters.
     */
    private void sendRequest(String request, Vector parameters) {
        boolean requestFailed = false;
        
        // Build request & URL encode parameters
        String url = request + "?";
        for (int i=0; i<parameters.size(); i++) {
            URLParam p = (URLParam)parameters.elementAt(i);
            url += p.getName() + "=" + URLEncoder.encode(p.getValue());
            url += (i==parameters.size()-1)?"":"&";
        }


        try {
            log("Request: " + url, MSG_DEBUG);

            //must be HTTP connection
            URL requestUrl = new URL(url);
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
                log("Can't get " + url, MSG_ERR);
                throw new BuildException("Couldn't execute " + url);
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
            throw new BuildException("Couldn't execute " + url);
        }
        if (requestFailed) {
            throw new BuildException("VAJ tool request failed");
        }
    }
}
