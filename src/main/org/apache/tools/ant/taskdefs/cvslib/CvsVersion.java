/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2004 The Apache Software Foundation.  All rights
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
package org.apache.tools.ant.taskdefs.cvslib;

import org.apache.tools.ant.taskdefs.AbstractCvsTask;

import java.io.ByteArrayOutputStream;
import java.util.StringTokenizer;

/**
 * this task allows to find out the client and the server version of a
 * CVS installation
 *
 * example usage :
 * &lt;cvsversion
 * cvsRoot=&quot;:pserver:anoncvs@cvs.apache.org:/home/cvspublic&quot;
 * passfile=&quot;c:/programme/cygwin/home/antoine/.cvspass&quot;
 * clientversionproperty=&quot;apacheclient&quot;
 * serverversionproperty=&quot;apacheserver&quot;   /&gt;
 *
 * the task can be used also in the API by calling its execute method,
 * then calling getServerVersion and/or getClientVersion
 *
 * @ant.task category="scm"
 * @author Antoine Levy-Lambert
 */
public class CvsVersion extends AbstractCvsTask {
    static final long VERSION_1_11_2 = 11102;
    static final long MULTIPLY = 100;
    private String clientVersion;
    private String serverVersion;
    private String clientVersionProperty;
    private String serverVersionProperty;
    /**
     * get the CVS client version
     * @return CVS client version
     */
    public String getClientVersion() {
        return clientVersion;
    }
    /**
     * get the CVS server version
     * @return CVS server version
     */
    public String getServerVersion() {
        return serverVersion;
    }
    /**
     * set a property where to store the CVS client version
     * @param clientVersionProperty  property for CVS client version
     */
    public void setClientVersionProperty(String clientVersionProperty) {
        this.clientVersionProperty = clientVersionProperty;
    }

    /**
     * set a property where to store the CVS server version
     * @param serverVersionProperty  property for CVS server version
     */
    public void setServerVersionProperty(String serverVersionProperty) {
        this.serverVersionProperty = serverVersionProperty;
    }
    /**
     * find out if the server version supports log with S option
     * @return  boolean indicating if the server version supports log with S option
     */
    public boolean supportsCvsLogWithSOption() {
        if (serverVersion == null) {
            return false;
        }
        StringTokenizer mySt = new StringTokenizer(serverVersion, ".");
        long versionNumber;
        long counter = MULTIPLY * MULTIPLY;
        long version = 0;
        while (mySt.hasMoreTokens()) {
            String s = mySt.nextToken();
            int i = 0;
            for (i = 0; i < s.length(); i++) {
                if (!Character.isDigit(s.charAt(i))) {
                    break;
                }
            }
            String s2 = s.substring(0, i);
            version = version + counter * Long.parseLong(s2);
            if (counter == 1) {
                break;
            }
            counter = counter / MULTIPLY;
        }
        return (version >= VERSION_1_11_2);
    }
    /**
     * the execute method running CvsVersion
     */
    public void execute() {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        this.setOutputStream(bos);
        ByteArrayOutputStream berr = new ByteArrayOutputStream();
        this.setErrorStream(berr);
        setCommand("version");
        super.execute();
        String output = bos.toString();
        StringTokenizer st = new StringTokenizer(output);
        boolean client = false;
        boolean server = false;
        boolean cvs = false;
        while (st.hasMoreTokens()) {
            String currentToken = st.nextToken();
            if (currentToken.equals("Client:")) {
                client = true;
            } else if (currentToken.equals("Server:")) {
                server = true;
            } else if (currentToken.equals("(CVS)")) {
                cvs = true;
            }
            if (client && cvs) {
                if (st.hasMoreTokens()) {
                    clientVersion = st.nextToken();
                }
                client = false;
                cvs = false;
            } else if (server && cvs) {
                if (st.hasMoreTokens()) {
                    serverVersion = st.nextToken();
                }
                server = false;
                cvs = false;
            }

        }
        if (clientVersionProperty != null) {
            getProject().setNewProperty(clientVersionProperty, clientVersion);
        }
        if (serverVersionProperty != null) {
            getProject().setNewProperty(serverVersionProperty, serverVersion);
        }
    }
}
