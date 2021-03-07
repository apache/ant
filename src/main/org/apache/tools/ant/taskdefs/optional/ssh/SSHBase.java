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

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.jcraft.jsch.ConfigRepository;
import com.jcraft.jsch.OpenSSHConfig;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Environment.Variable;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

/**
 * Base class for Ant tasks using jsch.
 *
 * @since Ant 1.6
 */
public abstract class SSHBase extends Task implements LogListener {

    /** Default listen port for SSH daemon */
    private static final int SSH_PORT = 22;

    private String host;
    private String knownHosts;
    private int port = SSH_PORT;
    private boolean failOnError = true;
    private boolean verbose;
    private final SSHUserInfo userInfo;
    private String sshConfig;
    private int serverAliveCountMax = 3;
    private int serverAliveIntervalSeconds = 0;
    private final Map<String, String> additionalConfig = new HashMap<>();

    /**
     * Constructor for SSHBase.
     */
    public SSHBase() {
        super();
        userInfo = new SSHUserInfo();
    }

    /**
     * Remote host, either DNS name or IP.
     *
     * @param host  The new host value
     */
    public void setHost(final String host) {
        this.host = host;
    }

    /**
     * Get the host.
     * @return the host
     */
    public String getHost() {
        return host;
    }

    /**
     * Set the failonerror flag.
     * Default is true
     * @param failure if true throw a build exception when a failure occurs,
     *                otherwise just log the failure and continue
     */
    public void setFailonerror(final boolean failure) {
        failOnError = failure;
    }

    /**
     * Get the failonerror flag.
     * @return the failonerror flag
     */
    public boolean getFailonerror() {
        return failOnError;
    }

    /**
     * Set the verbose flag.
     * @param verbose if true output more verbose logging
     * @since Ant 1.6.2
     */
    public void setVerbose(final boolean verbose) {
        this.verbose = verbose;
    }

    /**
     * Get the verbose flag.
     * @return the verbose flag
     * @since Ant 1.6.2
     */
    public boolean getVerbose() {
        return verbose;
    }

    /**
     * Get the OpenSSH config file (~/.ssh/config).
     * @return the OpenSSH config file
     * @since Ant 1.10.8
     */
    public String getSshConfig() {
        return sshConfig;
    }

    /**
     * Set the OpenSSH config file (~/.ssh/config).
     * @param sshConfig the OpenSSH config file
     * @since Ant 1.10.8
     */
    public void setSshConfig(String sshConfig) {
        this.sshConfig = sshConfig;
    }

    /**
     * Set the serverAliveCountMax value.
     * @param countMax int
     * @since Ant 1.9.7
     */
    public void setServerAliveCountMax(final int countMax) {
        if (countMax <= 0) {
            throw new BuildException("ssh server alive count max setting cannot be negative or zero");
        }
        this.serverAliveCountMax = countMax;
    }

    /**
     * Get the serverAliveCountMax value.
     * @return the serverAliveCountMax value
     * @since Ant 1.9.7
     */
    public int getServerAliveCountMax() {
        return serverAliveCountMax;
    }

    /**
     * Set the serverAliveIntervalSeconds value in seconds.
     * @param interval int
     * @since Ant 1.9.7
     */
    public void setServerAliveIntervalSeconds(final int interval) {
        if (interval < 0) {
            throw new BuildException("ssh server alive interval setting cannot be negative");
        }
        this.serverAliveIntervalSeconds = interval;
    }

    /**
     * Get the serverAliveIntervalSeconds value in seconds.
     * @return the serverAliveIntervalSeconds value in seconds
     * @since Ant 1.9.7
     */
    public int getServerAliveIntervalSeconds() {
        return serverAliveIntervalSeconds;
    }

    /**
     * Username known to remote host.
     *
     * @param username  The new username value
     */
    public void setUsername(final String username) {
        userInfo.setName(username);
    }


    /**
     * Sets the password for the user.
     *
     * @param password  The new password value
     */
    public void setPassword(final String password) {
        userInfo.setPassword(password);
    }

    /**
     * Sets the keyfile for the user.
     *
     * @param keyfile  The new keyfile value
     */
    public void setKeyfile(final String keyfile) {
        userInfo.setKeyfile(keyfile);
    }

    /**
     * Sets the passphrase for the users key.
     *
     * @param passphrase  The new passphrase value
     */
    public void setPassphrase(final String passphrase) {
        userInfo.setPassphrase(passphrase);
    }

    /**
     * Sets the path to the file that has the identities of
     * all known hosts.  This is used by SSH protocol to validate
     * the identity of the host.  The default is
     * <i>${user.home}/.ssh/known_hosts</i>.
     *
     * @param knownHosts a path to the known hosts file.
     */
    public void setKnownhosts(final String knownHosts) {
        this.knownHosts = knownHosts;
    }

    /**
     * Setting this to true trusts hosts whose identity is unknown.
     *
     * @param yesOrNo if true trust the identity of unknown hosts.
     */
    public void setTrust(final boolean yesOrNo) {
        userInfo.setTrust(yesOrNo);
    }

    /**
     * Changes the port used to connect to the remote host.
     *
     * @param port port number of remote host.
     */
    public void setPort(final int port) {
        this.port = port;
    }

    /**
     * Get the port attribute.
     * @return the port
     */
    public int getPort() {
        return port;
    }

    public void addConfiguredAdditionalConfig(final Variable v) {
        additionalConfig.put(v.getKey(), v.getValue());
    }

    /**
     * Initialize the task.
     * This initializes the known hosts and sets the default port.
     * @throws BuildException on error
     */
    @Override
    public void init() throws BuildException {
        super.init();
        this.knownHosts = System.getProperty("user.home") + "/.ssh/known_hosts";
        this.port = SSH_PORT;
    }

    /**
     * Load the SSH configuration file.
     * @throws BuildException on error
     */
    protected void loadSshConfig() throws BuildException {
        if (sshConfig != null && (userInfo.getName() == null || userInfo.getKeyfile() == null)) {
            if (!new File(sshConfig).exists()) {
                throw new BuildException("The SSH configuration file specified doesn't exist: " + sshConfig);
            }

            log("Loading SSH configuration file " + sshConfig, Project.MSG_DEBUG);
            ConfigRepository.Config config = null;
            try {
                config = OpenSSHConfig.parseFile(sshConfig).getConfig(host);
            } catch (IOException e) {
                throw new BuildException("Failed to load the SSH configuration file " + sshConfig, e);
            }

            if (config.getHostname() != null) {
                host = config.getHostname();
            }

            if (userInfo.getName() == null) {
                userInfo.setName(config.getUser());
            }

            if (userInfo.getKeyfile() == null) {
                log("Using SSH key file " + config.getValue("IdentityFile") + " for host " + host, Project.MSG_INFO);
                userInfo.setKeyfile(config.getValue("IdentityFile"));
            }
        }
    }

    /**
     * Open an ssh session.
     * @return the opened session
     * @throws JSchException on error
     */
    protected Session openSession() throws JSchException {
        final JSch jsch = new JSch();
        final SSHBase base = this;
        if (verbose) {
            JSch.setLogger(new com.jcraft.jsch.Logger() {
                @Override
                public boolean isEnabled(final int level) {
                    return true;
                }

                @Override
                public void log(final int level, final String message) {
                    base.log(message, Project.MSG_INFO);
                }
            });
        }
        if (null != userInfo.getKeyfile()) {
            jsch.addIdentity(userInfo.getKeyfile());
        }

        if (!userInfo.getTrust() && knownHosts != null) {
            log("Using known hosts: " + knownHosts, Project.MSG_DEBUG);
            jsch.setKnownHosts(knownHosts);
        }

        final Session session = jsch.getSession(userInfo.getName(), host, port);
        session.setConfig("PreferredAuthentications",
                "publickey,keyboard-interactive,password");
        session.setUserInfo(userInfo);

        if (getServerAliveIntervalSeconds() > 0) {
            session.setServerAliveCountMax(getServerAliveCountMax());
            session.setServerAliveInterval(getServerAliveIntervalSeconds() * 1000);
        }

        additionalConfig.forEach((k,v) -> {
            log("Setting additional config value " + k, Project.MSG_DEBUG);
            session.setConfig(k, v);
        });

        log("Connecting to " + host + ":" + port);
        session.connect();
        return session;
    }

    /**
     * Get the user information.
     * @return the user information
     */
    protected SSHUserInfo getUserInfo() {
        return userInfo;
    }
}
