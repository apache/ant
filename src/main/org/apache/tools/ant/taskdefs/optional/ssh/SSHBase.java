/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2003 The Apache Software Foundation.  All rights
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

package org.apache.tools.ant.taskdefs.optional.ssh;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.JSch;

import org.apache.tools.ant.Task;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

/**
 * Base class for Ant tasks using jsch.
 *
 * @author charliehubbard76@yahoo.com
 * @author riznob@hotmail.com
 * @since Ant 1.6
 */
public abstract class SSHBase extends Task implements LogListener {

    /** Default listen port for SSH daemon */
    private static final int SSH_PORT = 22;

    private String host;
    private String keyfile;
    private String knownHosts;
    private boolean trust = false;
    private int port = SSH_PORT;
    private boolean failOnError = true;
    private SSHUserInfo userInfo;

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
    public void setHost(String host) {
        this.host = host;
    }

    public String getHost() {
        return host;
    }

    public void setFailonerror(boolean failure) {
        failOnError = failure;
    }

    public boolean getFailonerror() {
        return failOnError;
    }

    /**
     * Username known to remote host.
     *
     * @param username  The new username value
     */
    public void setUsername(String username) {
        userInfo.setName(username);
    }


    /**
     * Sets the password for the user.
     *
     * @param password  The new password value
     */
    public void setPassword(String password) {
        userInfo.setPassword(password);
    }

    /**
     * Sets the keyfile for the user.
     *
     * @param keyfile  The new keyfile value
     */
    public void setKeyfile(String keyfile) {
        userInfo.setKeyfile(keyfile);
    }

    /**
     * Sets the passphrase for the users key.
     *
     * @param passphrase  The new passphrase value
     */
    public void setPassphrase(String passphrase) {
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
    public void setKnownhosts(String knownHosts) {
        this.knownHosts = knownHosts;
    }

    /**
     * Setting this to true trusts hosts whose identity is unknown.
     *
     * @param yesOrNo if true trust the identity of unknown hosts.
     */
    public void setTrust(boolean yesOrNo) {
        userInfo.setTrust(yesOrNo);
    }

    /**
     * Changes the port used to connect to the remote host.
     *
     * @param port port number of remote host.
     */
    public void setPort(int port) {
        this.port = port;
    }

    public int getPort() {
        return port;
    }

    public void init() throws BuildException {
        super.init();
        this.knownHosts = System.getProperty("user.home") + "/.ssh/known_hosts";
        this.trust = false;
        this.port = SSH_PORT;
    }

    protected Session openSession() throws JSchException {
        JSch jsch = new JSch();
        if (null != userInfo.getKeyfile()) {
            jsch.addIdentity(userInfo.getKeyfile());
        }

        if (knownHosts != null) {
            log("Using known hosts: " + knownHosts, Project.MSG_DEBUG);
            jsch.setKnownHosts(knownHosts);
        }

        Session session = jsch.getSession(userInfo.getName(), host, port);
        session.setUserInfo(userInfo);
        log("Connecting to " + host + ":" + port);
        session.connect();
        return session;
    }

    protected SSHUserInfo getUserInfo() {
        return userInfo;
    }
}
