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

import java.util.Vector;

import java.net.HttpURLConnection;
import java.util.Enumeration;
import java.net.URL;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.File;

/**
 * Helper class for VAJ tasks. Holds Workspace singleton and
 * wraps IvjExceptions into BuildExceptions
 *
 * @author Wolf Siberski, TUI Infotec GmbH
 */
class VAJRemoteUtil implements VAJUtil{
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
                               boolean exportClasses, boolean exportDebugInfo, boolean exportResources,
                               boolean exportSources, boolean useDefaultExcludes, boolean overwrite) {
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
            + dir.getAbsolutePath().replace('\\', '/') + "&"
            + VAJToolsServlet.CLASSES_PARAM + "=" + includeClasses + "&"
            + VAJToolsServlet.RESOURCES_PARAM + "=" + includeResources + "&"
            + VAJToolsServlet.SOURCES_PARAM + "=" + includeSources + "&"
            + VAJToolsServlet.DEFAULT_EXCLUDES_PARAM + "=" + useDefaultExcludes;

        if (includePatterns != null) {
            for (int i = 0; i < includePatterns.length; i++){
                result = result + "&" + VAJExportServlet.INCLUDE_PARAM + "="
                    + includePatterns[i].replace(' ', '+').replace('\\', '/');
            }
        }
        if (excludePatterns != null) {
            for (int i = 0; i < excludePatterns.length; i++){
                result = result + "&" + VAJExportServlet.EXCLUDE_PARAM + "="
                    + excludePatterns[i].replace(' ', '+').replace('\\', '/');
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
