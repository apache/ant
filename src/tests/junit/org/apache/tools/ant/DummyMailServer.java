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

package org.apache.tools.ant;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Callable;

/**
 * A utility class that pretends to be a mail transfer agent. This
 * has minimal functionality and is meant to be used only in tests
 */
public class DummyMailServer implements Runnable, Callable<Void> {

    private final String host;
    private final int port;


    private StringBuilder sb = null;
    private volatile boolean stop = false;

    ServerSocket ssock = null;
    Socket sock = null;
    BufferedWriter out = null;
    BufferedReader in = null;
    private boolean data = false;  // state engine: false=envelope, true=message

    public DummyMailServer(int port) {
        this("localhost", port);
    }

    public DummyMailServer(final String host, final int port) {
        this.host = host;
        this.port = port;
    }

    public int getPort() {
        return this.port;
    }

    public String getHost() {
        return this.host;
    }

    public void run() {
        call();
    }

    public Void call() {

        try {
            ssock = new ServerSocket(port);
            sock = ssock.accept(); // wait for connection
            in = new BufferedReader(new InputStreamReader(
                    sock.getInputStream()));
            out = new BufferedWriter(new OutputStreamWriter(
                    sock.getOutputStream()));
            sb = new StringBuilder();
            send("220 test SMTP EmailTaskTest\r\n");
            while (!stop) {
                String response = in.readLine();
                if (response == null) {
                    stop = true;
                    break;
                }
                sb.append(response).append("\r\n");

                if (!data && response.startsWith("EHLO")) {
                    // reject Extended HLO semantics, since we don't support it
                    send("500 EHLO unsupported\r\n");
                } else if (!data && response.startsWith("HELO")) {
                    send("250 " + host + " Hello " + host + " "
                            + "[127.0.0.1], pleased to meet you\r\n");
                } else if (!data && response.startsWith("MAIL")) {
                    send("250\r\n");
                } else if (!data && response.startsWith("RCPT")) {
                    send("250\r\n");
                } else if (!data && response.startsWith("DATA")) {
                    send("354\r\n");
                    data = true;
                } else if (data && response.equals(".")) {
                    send("250\r\n");
                    data = false;
                } else if (!data && response.startsWith("QUIT")) {
                    send("221\r\n");
                    stop = true;
                } else if (!data) {
                    send("500 5.5.1 Command unrecognized: \""
                            + response + "\"\r\n");
                    stop = true;
                } else {
                    // sb.append(response + "\r\n");
                }
            } // while
        } catch (IOException ioe) {
            if (stop) {
                // asked to stop, so ignore the exception and move on
                return null;
            }
            throw new BuildException(ioe);
        } finally {
            disconnect();
        }
        return null;
    }

    private void send(String retmsg) throws IOException {
        out.write(retmsg);
        out.flush();
        sb.append(retmsg);
    }

    public void disconnect() {
        this.stop = true;
        if (out != null) {
            try {
                out.flush();
                out.close();
                out = null;
            } catch (IOException e) {
                // ignore
            }
        }
        if (in != null) {
            try {
                in.close();
                in = null;
            } catch (IOException e) {
                // ignore
            }
        }
        if (sock != null) {
            try {
                sock.close();
                sock = null;
            } catch (IOException e) {
                // ignore
            }
        }
        if (ssock != null) {
            try {
                ssock.close();
                ssock = null;
            } catch (IOException e) {
                // ignore
            }
        }
    }

    public synchronized String getResult() {
        this.stop = true;
        return sb.toString();
    }

    /**
     * Starts and returns a new dummy mail server to be used in tests
     *
     * @return
     */
    public static DummyMailServer startMailServer() {
        return startMailServer("localhost");
    }

    /**
     * Starts and returns a new dummy mail server to be used in tests
     *
     * @param host The host on which the mail server will open a server socket to listen on
     * @return
     */
    public static DummyMailServer startMailServer(final String host) {
        final int port = TestHelper.getMaybeAvailablePort();
        final DummyMailServer mailServer = new DummyMailServer(host, port);
        final Thread thread = new Thread(mailServer);
        thread.setDaemon(true);
        thread.start();
        return mailServer;
    }
}
