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
 * @since ant 1.6.1
 */
public class CvsVersion extends AbstractCvsTask {
    static final long VERSION_1_11_2 = 11102;
    static final long MULTIPLY = 100;
    private String clientVersion;
    private String serverVersion;
    private String clientVersionProperty;
    private String serverVersionProperty;

    /**
     * Get the CVS client version
     * @return CVS client version
     */
    public String getClientVersion() {
        return clientVersion;
    }
    /**
     * Get the CVS server version
     * @return CVS server version
     */
    public String getServerVersion() {
        return serverVersion;
    }
    /**
     * Set a property where to store the CVS client version
     * @param clientVersionProperty  property for CVS client version
     */
    public void setClientVersionProperty(String clientVersionProperty) {
        this.clientVersionProperty = clientVersionProperty;
    }

    /**
     * Set a property where to store the CVS server version
     * @param serverVersionProperty  property for CVS server version
     */
    public void setServerVersionProperty(String serverVersionProperty) {
        this.serverVersionProperty = serverVersionProperty;
    }
    /**
     * Find out if the server version supports log with S option
     * @return  boolean indicating if the server version supports log with S option
     */
    public boolean supportsCvsLogWithSOption() {
        if (serverVersion == null) {
            return false;
        }
        StringTokenizer tokenizer = new StringTokenizer(serverVersion, ".");
        long counter = MULTIPLY * MULTIPLY;
        long version = 0;
        while (tokenizer.hasMoreTokens()) {
            String s = tokenizer.nextToken();
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
