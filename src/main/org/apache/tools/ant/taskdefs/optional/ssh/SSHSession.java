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

package org.apache.tools.ant.taskdefs.optional.ssh;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.TaskContainer;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

/**
 * Establishes an ssh session with a remote machine, optionally
 * establishing port forwarding, then executes any nested task(s)
 * before closing the session.
 * @since Ant 1.8.0
 */
public class SSHSession extends SSHBase {

    /** units are milliseconds, default is 0=infinite */
    private long maxwait = 0;

    private Vector localTunnels = new Vector();
    private Set localPortsUsed = new TreeSet();
    private Vector remoteTunnels = new Vector();
    private Set remotePortsUsed = new TreeSet();
    private NestedSequential nestedSequential = null;

    private static final String TIMEOUT_MESSAGE =
        "Timeout period exceeded, connection dropped.";


    /** Optional Vector holding the nested tasks */
    private Vector nestedTasks = new Vector();

    /**
     * Add a nested task to Sequential.
     * <p>
     * @param nestedTask        Nested task to execute Sequential
     * <p>
     */
    public void addTask(Task nestedTask) {
        nestedTasks.addElement(nestedTask);
    }

    /**
     * The connection can be dropped after a specified number of
     * milliseconds. This is sometimes useful when a connection may be
     * flaky. Default is 0, which means &quot;wait forever&quot;.
     *
     * @param timeout  The new timeout value in seconds
     */
    public void setTimeout(long timeout) {
        maxwait = timeout;
    }

    /**
     * Changes the comma-delimited list of local tunnels to establish
     * on the connection.
     *
     * @param tunnels a comma-delimited list of lport:rhost:rport
     * tunnel specifications
     */
    public void setLocaltunnels(String tunnels) {
        String[] specs = tunnels.split(", ");
        for (int i = 0; i < specs.length; i++) {
            if (specs[i].length() > 0) {
                String[] spec = specs[i].split(":", 3);
                int lport = Integer.parseInt(spec[0]);
                String rhost = spec[1];
                int rport = Integer.parseInt(spec[2]);
                LocalTunnel tunnel = createLocalTunnel();
                tunnel.setLPort(lport);
                tunnel.setRHost(rhost);
                tunnel.setRPort(rport);
            }
        }
    }

    /**
     * Changes the comma-delimited list of remote tunnels to establish
     * on the connection.
     *
     * @param tunnels a comma-delimited list of rport:lhost:lport
     * tunnel specifications
     */
    public void setRemotetunnels(String tunnels) {
        String[] specs = tunnels.split(", ");
        for (int i = 0; i < specs.length; i++) {
            if (specs[i].length() > 0) {
                String[] spec = specs[i].split(":", 3);
                int rport = Integer.parseInt(spec[0]);
                String lhost = spec[1];
                int lport = Integer.parseInt(spec[2]);
                RemoteTunnel tunnel = createRemoteTunnel();
                tunnel.setRPort(rport);
                tunnel.setLHost(lhost);
                tunnel.setLPort(lport);
            }
        }
    }


    /**
     * Establish the ssh session and execute all nestedTasks
     *
     * @exception BuildException if one of the nested tasks fails, or
     * network error or bad parameter.
     */
    public void execute() throws BuildException {
        if (getHost() == null) {
            throw new BuildException("Host is required.");
        }
        if (getUserInfo().getName() == null) {
            throw new BuildException("Username is required.");
        }
        if (getUserInfo().getKeyfile() == null
            && getUserInfo().getPassword() == null) {
            throw new BuildException("Password or Keyfile is required.");
        }
        if (nestedSequential == null) {
            throw new BuildException("Missing sequential element.");
        }


        Session session = null;
        try {
            // establish the session
            session = openSession();
            session.setTimeout((int) maxwait);

            for (Iterator i = localTunnels.iterator(); i.hasNext();) {
                LocalTunnel tunnel = (LocalTunnel) i.next();
                session.setPortForwardingL(tunnel.getLPort(),
                                           tunnel.getRHost(),
                                           tunnel.getRPort());
            }

            for (Iterator i = remoteTunnels.iterator(); i.hasNext();) {
                RemoteTunnel tunnel = (RemoteTunnel) i.next();
                session.setPortForwardingR(tunnel.getRPort(),
                                           tunnel.getLHost(),
                                           tunnel.getLPort());
            }

            for (Iterator i = nestedSequential.getNested().iterator();
                 i.hasNext();) {
                Task nestedTask = (Task) i.next();
                nestedTask.perform();
            }
            // completed successfully

        } catch (JSchException e) {
            if (e.getMessage().indexOf("session is down") >= 0) {
                if (getFailonerror()) {
                    throw new BuildException(TIMEOUT_MESSAGE, e);
                } else {
                    log(TIMEOUT_MESSAGE, Project.MSG_ERR);
                }
            } else {
                if (getFailonerror()) {
                    throw new BuildException(e);
                } else {
                    log("Caught exception: " + e.getMessage(),
                        Project.MSG_ERR);
                }
            }
        } catch (BuildException e) {
            // avoid wrapping it into yet another BuildException further down
            throw e;
        } catch (Exception e) {
            if (getFailonerror()) {
                throw new BuildException(e);
            } else {
                log("Caught exception: " + e.getMessage(), Project.MSG_ERR);
            }
        } finally {
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }

    public LocalTunnel createLocalTunnel() {
        LocalTunnel tunnel = new LocalTunnel();
        localTunnels.add(tunnel);
        return tunnel;
    }

    public RemoteTunnel createRemoteTunnel() {
        RemoteTunnel tunnel = new RemoteTunnel();
        remoteTunnels.add(tunnel);
        return tunnel;
    }

    public class LocalTunnel {
        public LocalTunnel() {}

        int lport = 0;
        String rhost = null;
        int rport = 0;
        public void setLPort(int lport) {
            Integer portKey = new Integer(lport);
            if (localPortsUsed.contains(portKey))
                throw new BuildException("Multiple local tunnels defined to"
                                         + " use same local port " + lport);
            localPortsUsed.add(portKey);
            this.lport = lport;
        }
        public void setRHost(String rhost) { this.rhost = rhost; }
        public void setRPort(int rport) { this.rport = rport; }
        public int getLPort() {
            if (lport == 0) throw new BuildException("lport is required for"
                                                     + " LocalTunnel.");
            return lport;
        }
        public String getRHost() {
            if (rhost == null) throw new BuildException("rhost is required"
                                                        + " for LocalTunnel.");
            return rhost;
        }
        public int getRPort() {
            if (rport == 0) throw new BuildException("rport is required for"
                                                     + " LocalTunnel.");
            return rport;
        }
    }

    public class RemoteTunnel {
        public RemoteTunnel() {}

        int lport = 0;
        String lhost = null;
        int rport = 0;
        public void setLPort(int lport) { this.lport = lport; }
        public void setLHost(String lhost) { this.lhost = lhost; }
        public void setRPort(int rport) {
            Integer portKey = new Integer(rport);
            if (remotePortsUsed.contains(portKey))
                throw new BuildException("Multiple remote tunnels defined to"
                                         + " use same remote port " + rport);
            remotePortsUsed.add(portKey);
            this.rport = rport;
        }
        public int getLPort() {
            if (lport == 0) throw new BuildException("lport is required for"
                                                     + " RemoteTunnel.");
            return lport;
        }
        public String getLHost() {
            if (lhost == null) throw new BuildException("lhost is required for"
                                                        + " RemoteTunnel.");
            return lhost;
        }
        public int getRPort() {
            if (rport == 0) throw new BuildException("rport is required for"
                                                     + " RemoteTunnel.");
            return rport;
        }
    }

    /**
     * This is the sequential nested element of the macrodef.
     *
     * @return a sequential element to be configured.
     */
    public NestedSequential createSequential() {
        if (this.nestedSequential != null) {
            throw new BuildException("Only one sequential allowed");
        }
        this.nestedSequential = new NestedSequential();
        return this.nestedSequential;
    }

    /**
     * The class corresponding to the sequential nested element.
     * This is a simple task container.
     */
    public static class NestedSequential implements TaskContainer {
        private List nested = new ArrayList();

        /**
         * Add a task or type to the container.
         *
         * @param task an unknown element.
         */
        public void addTask(Task task) {
            nested.add(task);
        }

        /**
         * @return the list of unknown elements
         */
        public List getNested() {
            return nested;
        }
    }

}
