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
package org.apache.tools.ant.taskdefs.optional.junit.remote;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.apache.tools.ant.taskdefs.optional.junit.TestRunListener;

/**
 * The server that will receive events from a remote client.
 *
 * <i>
 * This code is based on the code from Erich Gamma made for the
 * JUnit plugin for Eclipse. {@link http://www.eclipse.org} and is merged
 * with code originating from Ant 1.4.x.
 * </i>
 *
 * @see TestRunner
 * @author <a href="mailto:sbailliez@apache.org">Stephane Bailliez</a>
 */
public class Server {

    /** the port where the server is listening */
    private int port = -1;

    /** the server socket */
    private ServerSocket server;

    /** the client that is connected to the server */
    private Socket client;

    /** the reader in charge of interpreting messages from the client */
    private MessageReader reader = new MessageReader();

    /** writer used to send message to clients */
    private MessageWriter writer;

    public Server(int port) {
        this.port = port;
    }

    protected void finalize() throws Exception {
        cancel();
        shutdown();
    }

    /**
     * add a new listener
     * @param listener a instance of a listener.
     */
    public void addListener(TestRunListener listener) {
        reader.addListener(listener);
    }

    /**
     * remove an existing listener
     * @param listener a instance of a listener.
     */
    public void removeListener(TestRunListener listener) {
        reader.removeListener(listener);
    }

    /** return whether there is a client running or not */
    public boolean isRunning() {
        return client != null;
    }

    /** start a server to the specified port */
    public void start() {
        Worker worker = new Worker();
        worker.start();
    }

    /** cancel the connection to the client */
    public void cancel() {
        if (isRunning()) {
            writer.sendMessage(MessageIds.TEST_STOP);
        }
    }

    /** shutdown the server and any running client */
    public void shutdown() {
        if (writer != null) {
            writer.close();
            writer = null;
        }
        if (reader != null) {
            //@fixme what about the stream ?
            reader = null;
        }
        try {
            if (client != null) {
                client.shutdownInput();
                client.shutdownOutput();
                client.close();
                client = null;
            }
        } catch (IOException e) {
        }
        try {
            if (server != null) {
                server.close();
                server = null;
            }
        } catch (IOException e) {
        }
    }

//-----

    private class Worker extends Thread {
        public void run() {
            try {
                server = new ServerSocket(port);
                client = server.accept();
                writer = new MessageWriter(client.getOutputStream());
                reader.process(client.getInputStream());
            } catch (IOException e) {
                //@fixme this stacktrace might be normal when closing
                // the socket. So decompose the above in distinct steps
                e.printStackTrace();
            } finally {
                cancel();
                shutdown();
            }
        }
    }
}
