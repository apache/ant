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

import java.util.Locale;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectComponent;

/**
 * Condition to wait for a HTTP request to succeed. Its attribute(s) are:
 *   url - the URL of the request.
 *   errorsBeginAt - number at which errors begin at; default=400.
 *   requestMethod - HTTP request method to use; GET, HEAD, etc. default=GET
 * @since Ant 1.5
 */
public class Http extends ProjectComponent implements Condition {
    private static final int ERROR_BEGINS = 400;
    private static final String DEFAULT_REQUEST_METHOD = "GET";

    private String spec = null;
    private String requestMethod = DEFAULT_REQUEST_METHOD;


    /**
     * Set the url attribute
     * @param url the url of the request
     */
    public void setUrl(String url) {
        spec = url;
    }

    private int errorsBeginAt = ERROR_BEGINS;

    /**
     * Set the errorsBeginAt attribute
     * @param errorsBeginAt number at which errors begin at, default is
     *                      400
     */
    public void setErrorsBeginAt(int errorsBeginAt) {
        this.errorsBeginAt = errorsBeginAt;
    }

    /**
     * Sets the method to be used when issuing the HTTP request.
     *
     * @param method The HTTP request method to use. Valid values are
     *               the same as those accepted by the
     *               HttpURLConnection.setRequestMetho d() method,
     *               such as "GET", "HEAD", "TRACE", etc. The default
     *               if not specified is "GET".
     *
     * @see java.net.HttpURLConnection#setRequestMethod
     * @since Ant 1.8.0
     */
    public void setRequestMethod(String method) {
        this.requestMethod = method == null ? DEFAULT_REQUEST_METHOD
            : method.toUpperCase(Locale.ENGLISH);
    }

    /**
     * @return true if the HTTP request succeeds
     * @exception BuildException if an error occurs
     */
    public boolean eval() throws BuildException {
        if (spec == null) {
            throw new BuildException("No url specified in http condition");
        }
        log("Checking for " + spec, Project.MSG_VERBOSE);
        try {
            URL url = new URL(spec);
            try {
                URLConnection conn = url.openConnection();
                if (conn instanceof HttpURLConnection) {
                    HttpURLConnection http = (HttpURLConnection) conn;
                    http.setRequestMethod(requestMethod);
                    int code = http.getResponseCode();
                    log("Result code for " + spec + " was " + code,
                        Project.MSG_VERBOSE);
                    if (code > 0 && code < errorsBeginAt) {
                        return true;
                    }
                    return false;
                }
            } catch (java.net.ProtocolException pe) {
                throw new BuildException("Invalid HTTP protocol: "
                                         + requestMethod, pe);
            } catch (java.io.IOException e) {
                return false;
            }
        } catch (MalformedURLException e) {
            throw new BuildException("Badly formed URL: " + spec, e);
        }
        return true;
    }
}
