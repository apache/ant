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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectComponent;

/**
 * Condition to wait for a TCP/IP socket to have a listener. Its attributes are:
 *   server - the name of the server.
 *   port - the port number of the socket.
 *
 * @since Ant 1.5
 */
public class Socket extends ProjectComponent implements Condition {
    private String server = null;
    private int port = 0;

    /**
     * Set the server attribute
     *
     * @param server the server name
     */
    public void setServer(String server) {
        this.server = server;
    }

    /**
     * Set the port attribute
     *
     * @param port the port number of the socket
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * @return true if a socket can be created
     * @exception BuildException if the attributes are not set
     */
    @Override
    public boolean eval() throws BuildException {
        if (server == null) {
            throw new BuildException("No server specified in socket condition");
        }
        if (port == 0) {
            throw new BuildException("No port specified in socket condition");
        }
        log("Checking for listener at " + server + ":" + port,
            Project.MSG_VERBOSE);
        try (java.net.Socket s = new java.net.Socket(server, port)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }

}
