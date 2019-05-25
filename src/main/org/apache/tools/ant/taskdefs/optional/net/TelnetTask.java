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

package org.apache.tools.ant.taskdefs.optional.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.List;
import java.util.Vector;

import org.apache.commons.net.telnet.TelnetClient;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

/**
 * Automates the telnet protocol.
 *
 */
public class TelnetTask extends Task {
    private static final int WAIT_INTERVAL = 250;
    private static final int TELNET_PORT = 23;

    /**
     *  The userid to login with, if automated login is used
     */
    private String userid  = null;

    /**
     *  The password to login with, if automated login is used
     */
    private String password = null;

    /**
     *  The server to connect to.
     */
    private String server  = null;

    /**
     *  The tcp port to connect to.
     */
    private int port = TELNET_PORT;

    /**
     *  The list of read/write commands for this session
     */
    private List<TelnetSubTask> telnetTasks = new Vector<>();

    /**
     *  If true, adds a CR to beginning of login script
     */
    private boolean addCarriageReturn = false;

    /**
     *  Default time allowed for waiting for a valid response
     *  for all child reads.  A value of 0 means no limit.
     */
    private Integer defaultTimeout = null;

    /**
     *  Verify that all parameters are included.
     *  Connect and possibly login
     *  Iterate through the list of Reads and writes
     * @throws BuildException on error
     */
    @Override
    public void execute() throws BuildException {
       /**  A server name is required to continue */
       if (server == null) {
           throw new BuildException("No Server Specified");
       }
       /**  A userid and password must appear together
        *   if they appear.  They are not required.
        */
       if (userid == null && password != null) {
           throw new BuildException("No Userid Specified");
       }
       if (password == null && userid != null) {
           throw new BuildException("No Password Specified");
       }

       /**  Create the telnet client object */
       AntTelnetClient telnet = null;
       boolean success = false;
       try {
           telnet = new AntTelnetClient();
           try {
               telnet.connect(server, port);
           } catch (IOException e) {
               throw new BuildException("Can't connect to " + server);
           }
           /**  Login if userid and password were specified */
           if (userid != null && password != null) { //NOSONAR
               login(telnet);
           }
           /**  Process each sub command */
           for (TelnetSubTask task : telnetTasks) {
               if (task instanceof TelnetRead && defaultTimeout != null) {
                   ((TelnetRead) task).setDefaultTimeout(defaultTimeout);
               }
               task.execute(telnet);
           }
           success = true;
       } finally {
           if (telnet != null && telnet.isConnected()) {
               try {
                   telnet.disconnect();
               } catch (IOException e) {
                    String msg = "Error disconnecting from " + server;
                    if (success) {
                        throw new BuildException(msg); //NOSONAR
                    } else { // don't hide inner exception
                        log(msg, Project.MSG_ERR);
                    }
               }
           }
       }
    }

    /**
     *  Process a 'typical' login.  If it differs, use the read
     *  and write tasks explicitly
     */
    private void login(AntTelnetClient telnet) {
       if (addCarriageReturn) {
          telnet.sendString("\n", true);
       }
       telnet.waitForString("ogin:");
       telnet.sendString(userid, true);
       telnet.waitForString("assword:");
       telnet.sendString(password, false);
    }

    /**
     * Set the the login id to use on the server;
     * required if <code>password</code> is set.
     * @param u a <code>String</code> value
     */
    public void setUserid(String u) {
        this.userid = u;
    }

    /**
     *  Set the the login password to use
     * required if <code>userid</code> is set.
     * @param p a <code>String</code> value
     */
    public void setPassword(String p) {
        this.password = p;
    }

    /**
     *  Set the hostname or address of the remote server.
     * @param m a <code>String</code> value
     */
    public void setServer(String m) {
        this.server = m;
    }

    /**
     *  Set the tcp port to connect to; default is 23.
     * @param p an <code>int</code> value
     */
    public void setPort(int p) {
        this.port = p;
    }

    /**
     *  send a carriage return after connecting; optional, defaults to false.
     * @param b a <code>boolean</code> value
     */
    public void setInitialCR(boolean b) {
       this.addCarriageReturn = b;
    }

    /**
     * set a default timeout in seconds to wait for a response,
     * zero means forever (the default)
     * @param i an <code>Integer</code> value
     */
    public void setTimeout(Integer i) {
       this.defaultTimeout = i;
    }

    /**
     *  A string to wait for from the server.
     *  A subTask &lt;read&gt; tag was found.  Create the object,
     *  Save it in our list, and return it.
     * @return a read telnet sub task
     */

    public TelnetSubTask createRead() {
        TelnetSubTask task = new TelnetRead();
        telnetTasks.add(task);
        return task;
    }

    /**
     *  Add text to send to the server
     *  A subTask &lt;write&gt; tag was found.  Create the object,
     *  Save it in our list, and return it.
     * @return a write telnet sub task
     */
    public TelnetSubTask createWrite() {
        TelnetSubTask task = new TelnetWrite();
        telnetTasks.add(task);
        return task;
    }

    /**
     *  This class is the parent of the Read and Write tasks.
     *  It handles the common attributes for both.
     */
    public class TelnetSubTask {
        // CheckStyle:VisibilityModifier OFF - bc
        protected String taskString = "";
        // CheckStyle:VisibilityModifier ON
        /**
         * Execute the subtask.
         * @param telnet the client
         * @throws BuildException always as it is not allowed to instantiate this object
         */
        public void execute(AntTelnetClient telnet)
                throws BuildException {
            throw new BuildException("Shouldn't be able instantiate a SubTask directly");
        }

        /**
         *  the message as nested text
         * @param s the nested text
         */
        public void addText(String s) {
            setString(getProject().replaceProperties(s));
        }

        /**
         * the message as an attribute
         * @param s a <code>String</code> value
         */
        public void setString(String s) {
           taskString += s;
        }
    }

    /**
     *  Sends text to the connected server
     */
    public class TelnetWrite extends TelnetSubTask {
        private boolean echoString = true;
        /**
         * Execute the write task.
         * @param telnet the task to use
         * @throws BuildException on error
         */
        @Override
        public void execute(AntTelnetClient telnet)
               throws BuildException {
           telnet.sendString(taskString, echoString);
        }

        /**
         * Whether or not the message should be echoed to the log.
         * Defaults to <code>true</code>.
         * @param b a <code>boolean</code> value
         */
        public void setEcho(boolean b) {
           echoString = b;
        }
    }

    /**
     *  Reads the output from the connected server
     *  until the required string is found or we time out.
     */
    public class TelnetRead extends TelnetSubTask {
        private Integer timeout = null;
        /**
         * Execute the read task.
         * @param telnet the task to use
         * @throws BuildException on error
         */
        @Override
        public void execute(AntTelnetClient telnet)
               throws BuildException {
            telnet.waitForString(taskString, timeout);
        }
        /**
         *  a timeout value that overrides any task wide timeout.
         * @param i an <code>Integer</code> value
         */
        public void setTimeout(Integer i) {
           this.timeout = i;
        }

        /**
         * Sets the default timeout if none has been set already
         * @param defaultTimeout an <code>Integer</code> value
         * @ant.attribute ignore="true"
         */
        public void setDefaultTimeout(Integer defaultTimeout) {
           if (timeout == null) {
              timeout = defaultTimeout;
           }
        }
    }

    /**
     *  This class handles the abstraction of the telnet protocol.
     *  Currently it is a wrapper around <a
     *  href="https://jakarta.apache.org/commons/net/index.html">Jakarta
     *  Commons Net</a>.
     */
    public class AntTelnetClient extends TelnetClient {
        /**
         * Read from the telnet session until the string we are
         * waiting for is found
         * @param s The string to wait on
         */
        public void waitForString(String s) {
            waitForString(s, null);
        }

        /**
         * Read from the telnet session until the string we are
         * waiting for is found or the timeout has been reached
         * @param s The string to wait on
         * @param timeout The maximum number of seconds to wait
         */
        public void waitForString(String s, Integer timeout) {
            InputStream is = this.getInputStream();
            try {
                StringBuilder sb = new StringBuilder();
                int windowStart = -s.length();
                if (timeout == null || timeout == 0) {
                    while (windowStart < 0
                           || !sb.substring(windowStart).equals(s)) {
                        sb.append((char) is.read());
                        windowStart++;
                    }
                } else {
                    Calendar endTime = Calendar.getInstance();
                    endTime.add(Calendar.SECOND, timeout);
                    while (windowStart < 0
                           || !sb.substring(windowStart).equals(s)) {
                        while (Calendar.getInstance().before(endTime)
                               && is.available() == 0) {
                            Thread.sleep(WAIT_INTERVAL);
                        }
                        if (is.available() == 0) {
                            log("Read before running into timeout: "
                                + sb.toString(), Project.MSG_DEBUG);
                            throw new BuildException(
                                "Response timed-out waiting for \"" + s + '\"',
                                getLocation());
                        }
                        sb.append((char) is.read());
                        windowStart++;
                    }
                }
                log(sb.toString(), Project.MSG_INFO);
            } catch (BuildException be) {
                throw be;
            } catch (Exception e) {
                throw new BuildException(e, getLocation());
            }
        }

        /**
         * Write this string to the telnet session.
         * @param s          the string to write
         * @param echoString if true log the string sent
         */
        public void sendString(String s, boolean echoString) {
            OutputStream os = this.getOutputStream();
            try {
                os.write((s + "\n").getBytes());
                if (echoString) {
                    log(s, Project.MSG_INFO);
                }
                os.flush();
            } catch (Exception e) {
                throw new BuildException(e, getLocation());
            }
        }
    }
}
