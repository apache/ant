/*
 * Copyright  2001-2004 The Apache Software Foundation
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

import com.ibm.ivj.toolserver.servletclasses.servlet.ServletException;
import com.ibm.ivj.toolserver.servletclasses.servlet.http.HttpServlet;
import com.ibm.ivj.toolserver.servletclasses.servlet.http.HttpServletRequest;
import com.ibm.ivj.toolserver.servletclasses.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.util.StringUtils;

/**
 * Abstract base class to provide common services for the
 * VAJ tool API servlets
 *
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
            util.log("Error occurred: " + e.getMessage(), VAJUtil.MSG_ERR);
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
