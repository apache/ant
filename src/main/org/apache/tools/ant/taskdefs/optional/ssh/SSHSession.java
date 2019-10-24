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

package org.apache.tools.ant.taskdefs.optional.ssh;

import java.util.ArrayList;
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

    private final List<LocalTunnel> localTunnels = new Vector<>();
    private final Set<Integer> localPortsUsed = new TreeSet<>();
    private final List<RemoteTunnel> remoteTunnels = new Vector<>();
    private final Set<Integer> remotePortsUsed = new TreeSet<>();
    private NestedSequential nestedSequential = null;

    private static final String TIMEOUT_MESSAGE =
        "Timeout period exceeded, connection dropped.";


    /** Optional Vector holding the nested tasks */
    private final List<Task> nestedTasks = new Vector<>();

    /**
     * Add a nested task to Sequential.
     *
     * @param nestedTask Nested task to execute sequentially
     */
    public void addTask(final Task nestedTask) {
        nestedTasks.add(nestedTask);
    }

    /**
     * The connection can be dropped after a specified number of
     * milliseconds. This is sometimes useful when a connection may be
     * flaky. Default is 0, which means &quot;wait forever&quot;.
     *
     * @param timeout  The new timeout value in seconds
     */
    public void setTimeout(final long timeout) {
        maxwait = timeout;
    }

    /**
     * Changes the comma-delimited list of local tunnels to establish
     * on the connection.
     *
     * @param tunnels a comma-delimited list of lport:rhost:rport
     * tunnel specifications
     */
    public void setLocaltunnels(final String tunnels) {
        for (String tunnelSpec : tunnels.split(", ")) {
            if (!tunnelSpec.isEmpty()) {
                final String[] spec = tunnelSpec.split(":", 3);
                final int lport = Integer.parseInt(spec[0]);
                final String rhost = spec[1];
                final int rport = Integer.parseInt(spec[2]);
                final LocalTunnel tunnel = createLocalTunnel();
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
    public void setRemotetunnels(final String tunnels) {
        for (String tunnelSpec : tunnels.split(", ")) {
            if (!tunnelSpec.isEmpty()) {
                final String[] spec = tunnelSpec.split(":", 3);
                final int rport = Integer.parseInt(spec[0]);
                final String lhost = spec[1];
                final int lport = Integer.parseInt(spec[2]);
                final RemoteTunnel tunnel = createRemoteTunnel();
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
    @Override
    public void execute() throws BuildException {
        if (getHost() == null) {
            throw new BuildException("Host is required.");
        }
        
        loadSshConfig();
        
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

            for (LocalTunnel tunnel : localTunnels) {
                session.setPortForwardingL(tunnel.getLPort(),
                                           tunnel.getRHost(),
                                           tunnel.getRPort());
            }

            for (RemoteTunnel tunnel : remoteTunnels) {
                session.setPortForwardingR(tunnel.getRPort(),
                                           tunnel.getLHost(),
                                           tunnel.getLPort());
            }

            nestedSequential.getNested().forEach(Task::perform);
            // completed successfully

        } catch (final JSchException e) {
            if (e.getMessage().contains("session is down")) {
                if (getFailonerror()) {
                    throw new BuildException(TIMEOUT_MESSAGE, e);
                }
                log(TIMEOUT_MESSAGE, Project.MSG_ERR);
            } else {
                if (getFailonerror()) {
                    throw new BuildException(e);
                }
                log("Caught exception: " + e.getMessage(), Project.MSG_ERR);
            }
        } catch (final BuildException e) {
            // avoid wrapping it into yet another BuildException further down
            throw e;
        } catch (final Exception e) {
            if (getFailonerror()) {
                throw new BuildException(e);
            }
            log("Caught exception: " + e.getMessage(), Project.MSG_ERR);
        } finally {
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }

    public LocalTunnel createLocalTunnel() {
        final LocalTunnel tunnel = new LocalTunnel();
        localTunnels.add(tunnel);
        return tunnel;
    }

    public RemoteTunnel createRemoteTunnel() {
        final RemoteTunnel tunnel = new RemoteTunnel();
        remoteTunnels.add(tunnel);
        return tunnel;
    }

    public class LocalTunnel {

        int lport = 0;
        String rhost = null;
        int rport = 0;

        public void setLPort(final int lport) {
            final Integer portKey = lport;
            if (localPortsUsed.contains(portKey)) {
                throw new BuildException(
                    "Multiple local tunnels defined to use same local port %d",
                    lport);
            }
            localPortsUsed.add(portKey);
            this.lport = lport;
        }

        public void setRHost(final String rhost) {
            this.rhost = rhost;
        }

        public void setRPort(final int rport) {
            this.rport = rport;
        }

        public int getLPort() {
            if (lport == 0) {
                throw new BuildException("lport is required for LocalTunnel.");
            }
            return lport;
        }

        public String getRHost() {
            if (rhost == null) {
                throw new BuildException("rhost is required for LocalTunnel.");
            }
            return rhost;
        }

        public int getRPort() {
            if (rport == 0) {
                throw new BuildException("rport is required for LocalTunnel.");
            }
            return rport;
        }
    }

    public class RemoteTunnel {

        int lport = 0;
        String lhost = null;
        int rport = 0;

        public void setLPort(final int lport) {
            this.lport = lport;
        }

        public void setLHost(final String lhost) {
            this.lhost = lhost;
        }

        public void setRPort(final int rport) {
            final Integer portKey = rport;
            if (remotePortsUsed.contains(portKey)) {
                throw new BuildException(
                    "Multiple remote tunnels defined to use same remote port %d",
                    rport);
            }
            remotePortsUsed.add(portKey);
            this.rport = rport;
        }

        public int getLPort() {
            if (lport == 0) {
                throw new BuildException("lport is required for RemoteTunnel.");
            }
            return lport;
        }

        public String getLHost() {
            if (lhost == null) {
                throw new BuildException("lhost is required for RemoteTunnel.");
            }
            return lhost;
        }

        public int getRPort() {
            if (rport == 0) {
                throw new BuildException("rport is required for RemoteTunnel.");
            }
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
        private final List<Task> nested = new ArrayList<>();

        /**
         * Add a task or type to the container.
         *
         * @param task an unknown element.
         */
        @Override
        public void addTask(final Task task) {
            nested.add(task);
        }

        /**
         * @return the list of unknown elements
         */
        public List<Task> getNested() {
            return nested;
        }
    }

}
