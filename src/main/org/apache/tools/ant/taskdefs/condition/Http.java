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

package org.apache.tools.ant.taskdefs.condition;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Locale;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectComponent;
import org.apache.tools.ant.taskdefs.Get;

/**
 * Condition to wait for a HTTP request to succeed. Its attribute(s) are:
 *   url - the URL of the request.
 *   errorsBeginAt - number at which errors begin at; default=400.
 *   requestMethod - HTTP request method to use; GET, HEAD, etc. default=GET
 *   readTimeout - The read timeout in ms. default=0
 * @since Ant 1.5
 */
public class Http extends ProjectComponent implements Condition {
    private static final int ERROR_BEGINS = 400;
    private static final String DEFAULT_REQUEST_METHOD = "GET";
    private static final String HTTP = "http";
    private static final String HTTPS = "https";

    private String spec = null;
    private String requestMethod = DEFAULT_REQUEST_METHOD;
    private boolean followRedirects = true;

    private int errorsBeginAt = ERROR_BEGINS;
    private int readTimeout = 0;

    /**
     * Set the url attribute
     * @param url the url of the request
     */
    public void setUrl(String url) {
        spec = url;
    }

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
     *               HttpURLConnection.setRequestMethod() method,
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
     * Whether redirects sent by the server should be followed,
     * defaults to true.
     *
     * @param f boolean
     * @since Ant 1.9.7
     */
    public void setFollowRedirects(boolean f) {
        followRedirects = f;
    }

    /**
     * Sets the read timeout. Any value &lt; 0 will be ignored
     *
     * @param t the timeout value in milli seconds
     *
     * @see java.net.HttpURLConnection#setReadTimeout
     * @since Ant 1.10.6
     */
    public void setReadTimeout(int t) {
        if(t >= 0) {
            this.readTimeout = t;
        }
    }

    /**
     * @return true if the HTTP request succeeds
     * @exception BuildException if an error occurs
     */
    @Override
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
                    int code = request((HttpURLConnection) conn, url);
                    log("Result code for " + spec + " was " + code,
                        Project.MSG_VERBOSE);
                    return code > 0 && code < errorsBeginAt;
                }
            } catch (ProtocolException pe) {
                throw new BuildException("Invalid HTTP protocol: "
                                         + requestMethod, pe);
            } catch (IOException e) {
                return false;
            }
        } catch (MalformedURLException e) {
            throw new BuildException("Badly formed URL: " + spec, e);
        }
        return true;
    }

    private int request(final HttpURLConnection http, final URL url) throws IOException {
        http.setRequestMethod(requestMethod);
        http.setInstanceFollowRedirects(followRedirects);
        http.setReadTimeout(readTimeout);
        final int firstStatusCode = http.getResponseCode();
        if (this.followRedirects && Get.isMoved(firstStatusCode)) {
            final String newLocation = http.getHeaderField("Location");
            final URL newURL = new URL(newLocation);
            if (redirectionAllowed(url, newURL)) {
                final URLConnection newConn = newURL.openConnection();
                if (newConn instanceof HttpURLConnection) {
                    log("Following redirect from " + url + " to " + newURL);
                    return request((HttpURLConnection) newConn, newURL);
                }
            }
        }
        return firstStatusCode;
    }

    private boolean redirectionAllowed(final URL from, final URL to) {
        if (from.equals(to)) {
            // most simple case of an infinite redirect loop
            return false;
        }
        if (!(from.getProtocol().equals(to.getProtocol())
              || (HTTP.equals(from.getProtocol())
                  && HTTPS.equals(to.getProtocol())))) {
            log("Redirection detected from "
                + from.getProtocol() + " to " + to.getProtocol()
                + ". Protocol switch unsafe, not allowed.");
            return false;
        }
        return true;
    }
}
