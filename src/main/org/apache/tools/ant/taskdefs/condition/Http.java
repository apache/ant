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

package org.apache.tools.ant.taskdefs.condition;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectComponent;


import java.net.MalformedURLException;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.net.URL;

/**
 * Condition to wait for a HTTP request to succeed. Its attribute(s) are:
 *   url - the URL of the request.
 *   errorsBeginAt - number at which errors begin at; default=400. 
 * @author <a href="mailto:denis@network365.com">Denis Hennessy</a>
 * @since Ant 1.5
 */
public class Http extends ProjectComponent implements Condition {
    private String spec = null;

    public void setUrl(String url) {
        spec = url;
    }

    private int errorsBeginAt=400;
    
    public void setErrorsBeginAt(int errorsBeginAt) {
        this.errorsBeginAt=errorsBeginAt;
    }
    
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
                    int code = http.getResponseCode();
                    log("Result code for " + spec + " was " + code, 
                        Project.MSG_VERBOSE);
                    if (code > 0 && code < errorsBeginAt) {
                        return true;
                    } else {
                        return false;
                    }
                }
            } catch (java.io.IOException e) {
                return false;
            }
        } catch (MalformedURLException e) {
            throw new BuildException("Badly formed URL: " + spec, e);
        }
        return true;
    }
}
