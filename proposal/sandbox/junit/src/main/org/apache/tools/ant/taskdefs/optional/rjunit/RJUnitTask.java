/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002 The Apache Software Foundation.  All rights
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
 * 4. The names "The Jakarta Project", "Ant", and "Apache Software
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
package org.apache.tools.ant.taskdefs.optional.rjunit;

import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/**
 * The core JUnit task.
 *
 */
public class RJUnitTask extends Task {

    private final static Resources RES =
            ResourceManager.getPackageResources(RJUnitTask.class);

    /** port to run the server on */
    private int port = -1;

    /** timeout period in ms */
    private long timeout = -1;

    /** client configuraiton element */
    private ClientElement client = null;

    /** server configuration element */
    private ServerElement server = null;

// task implementation

    public void execute() throws BuildException {
        if (client == null && server == null) {
            throw new BuildException("Invalid state: need to be server, client or both");
        }

        // 1) server and client
        if (server != null && client != null) {
            ServerWorker worker = new ServerWorker();
            worker.start();
            client.execute();
            Exception caught = null;
            try {
                worker.join();
                caught = worker.getException();
            } catch (InterruptedException e){
                caught = e;
            }
            if (caught != null){
                throw new BuildException(caught);
            }
            return;
        }

        // 2) server only (waiting for client)
        if (server != null && client == null) {
            server.execute();
            return;
        }

        // 3) client only (connecting to server)
        if (server == null && client != null) {
            client.execute();
            return;
        }
    }

// Ant bean accessors

    public void setPort(int port) {
        this.port = port;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    /**
     * create a new client in charge of running tests and sending
     * the results to the server that collect them.
     */
    public ClientElement createClient() {
        if (client == null) {
            client = new ClientElement(this);
        }
        return client;
    }

    /**
     * create a new client in charge of running tests and sending
     * the results to the server that collect them.
     */
    public ServerElement createServer() {
        if (server == null) {
            server = new ServerElement(this);
        }
        return server;
    }


    /** the worker to run the server on */
    class ServerWorker extends Thread {
        private Exception caught = null;

        public void run() {
            try {
                server.execute();
            } catch (Exception e) {
                caught = e;
                e.printStackTrace();
            }
        }

        public Exception getException() {
            return caught;
        }
    }
}
