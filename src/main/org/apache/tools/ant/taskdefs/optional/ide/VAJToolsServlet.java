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



import java.io.IOException;



import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.util.StringUtils;

/**
 * Abstract base class to provide common services for the
 * VAJ tool API servlets
 *
 * @author Wolf Siberski, based on servlets written by Glenn McAllister
 */
public abstract class VAJToolsServlet extends HttpServlet {
    /**
     * Adaptation of VAJUtil for servlet context.
     */
    class VAJLocalServletUtil extends VAJLocalUtil {
        public void log(String msg, int level) {
            try {
                if (msg != null) {
                    msg = msg.replace('\r', ' ');
                    int i = 0;
                    while (i < msg.length()) {
                        int nlPos = msg.indexOf('\n', i);
                        if (nlPos == -1) {
                            nlPos = msg.length();
                        }
                        response.getWriter().println(Integer.toString(level)
                                                     + " " + msg.substring(i, nlPos));
                        i = nlPos + 1;
                    }
                }
            } catch (IOException e) {
                throw new BuildException("logging failed. msg was: "
                                         + e.getMessage());
            }
        }
    }

    // constants for servlet param names
    public static final String DIR_PARAM = "dir";
    public static final String INCLUDE_PARAM = "include";
    public static final String EXCLUDE_PARAM = "exclude";
    public static final String CLASSES_PARAM = "cls";
    public static final String SOURCES_PARAM = "src";
    public static final String RESOURCES_PARAM = "res";
    public static final String DEFAULT_EXCLUDES_PARAM = "dex";
    public static final String PROJECT_NAME_PARAM = "project";


    // current request
    HttpServletRequest  request;

    // response to current request
    HttpServletResponse response;

    // implementation of VAJUtil used by the servlet
    VAJUtil util;


    /**
     * Execute the request by calling the appropriate
     * VAJ tool API methods. This method must be implemented
     * by the concrete servlets
     */
    protected abstract void executeRequest();

    /**
     * Respond to a HTTP request. This method initializes
     * the servlet and handles errors.
     * The real work is done in the abstract method executeRequest()
     */
    public void doGet(HttpServletRequest req, HttpServletResponse res)
        throws ServletException, IOException {
        try {
            response = res;
            request = req;
            initRequest();
            executeRequest();
        } catch (BuildException e) {
            util.log("Error occured: " + e.getMessage(), VAJUtil.MSG_ERR);
        } catch (Exception e) {
            try {
                if (!(e instanceof BuildException)) {
                    String trace = StringUtils.getStackTrace(e);
                    util.log("Program error in " + this.getClass().getName()
                             + ":\n" + trace, VAJUtil.MSG_ERR);
                }
            } catch (Throwable t) {
                t.printStackTrace();
            } finally {
                if (!(e instanceof BuildException)) {
                    throw new ServletException(e.getMessage());
                }
            }
        }
    }

    /**
     * initialize the servlet.
     */
    protected void initRequest() throws IOException {
        response.setContentType("text/ascii");
        if (util == null) {
            util = new VAJLocalServletUtil();
        }
    }

    /**
     * Get the VAJUtil implementation
     */
    VAJUtil getUtil() {
        return util;
    }

    /**
     * Get the boolean value of a parameter.
     */
    protected boolean getBooleanParam(String param) {
        return getBooleanParam(param, false);
    }

    /**
     * Get the boolean value of a parameter, with a default value if
     * the parameter hasn't been passed to the servlet.
     */
    protected boolean getBooleanParam(String param, boolean defaultValue) {
        String value = getFirstParamValueString(param);
        if (value != null) {
            return toBoolean(value);
        } else {
            return defaultValue;
        }
    }

    /**
     * Returns the first encountered value for a parameter.
     */
    protected String getFirstParamValueString(String param) {
        String[] paramValuesArray = request.getParameterValues(param);
        if (paramValuesArray == null) {
            return null;
        }
        return paramValuesArray[0];
    }

    /**
     * Returns all values for a parameter.
     */
    protected String[] getParamValues(String param) {
        return request.getParameterValues(param);
    }

    /**
     * A utility method to translate the strings "yes", "true", and "ok"
     * to boolean true, and everything else to false.
     */
    protected boolean toBoolean(String string) {
        String lower = string.toLowerCase();
        return (lower.equals("yes") || lower.equals("true") || lower.equals("ok"));
    }
}
