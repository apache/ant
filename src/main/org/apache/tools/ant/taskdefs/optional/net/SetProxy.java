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

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.Properties;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.util.ProxySetup;

/**
 * Sets Java's web proxy properties, so that tasks and code run in
 * the same JVM can have through-the-firewall access to remote web sites,
 * and remote ftp sites.
 * You can nominate an http and ftp proxy, or a socks server, reset the server
 * settings, or do nothing at all.
 * <p>
 * Examples
 * <pre>&lt;setproxy/&gt;</pre>
 * do nothing
 * <pre>&lt;setproxy proxyhost="firewall"/&gt;</pre>
 * set the proxy to firewall:80
 * <pre>&lt;setproxy proxyhost="firewall" proxyport="81"/&gt;</pre>
 * set the proxy to firewall:81
 * <pre>&lt;setproxy proxyhost=""/&gt;</pre>
 * stop using the http proxy; don't change the socks settings
 * <pre>&lt;setproxy socksproxyhost="socksy"/&gt;</pre>
 * use socks via socksy:1080
 * <pre>&lt;setproxy socksproxyhost=""/&gt;</pre>
 * stop using the socks server.
 * <p>
 * You can set a username and password for http with the <code>proxyHost</code>
 * and <code>proxyPassword</code> attributes. These can also be
 * used against SOCKS5 servers.
 * </p>
 * @see <a href="https://docs.oracle.com/javase/8/docs/technotes/guides/net/properties.html">
 *  https://docs.oracle.com/javase/8/docs/technotes/guides/net/properties.html</a>
  *@since       Ant 1.5
 * @ant.task category="network"
 */
public class SetProxy extends Task {
    private static final int HTTP_PORT = 80;
    private static final int SOCKS_PORT = 1080;
    // CheckStyle:VisibilityModifier OFF - bc
    /**
     * proxy details
     */
    protected String proxyHost = null;

    /**
     * name of proxy port
     */
    protected int proxyPort = HTTP_PORT;

    // CheckStyle:VisibilityModifier ON

    /**
     * socks host.
     */
    private String socksProxyHost = null;

    /**
     * Socks proxy port. Default is 1080.
     */
    private int socksProxyPort = SOCKS_PORT;


    /**
     * list of non proxy hosts
     */
    private String nonProxyHosts = null;

    /**
     * user for http only
     */
    private String proxyUser = null;

    /**
     * password for http only
     */
    private String proxyPassword = null;

    /**
     * the HTTP/ftp proxy host. Set this to "" for the http proxy
     * option to be disabled
     *
     * @param hostname the new proxy hostname
     */
    public void setProxyHost(String hostname) {
        proxyHost = hostname;
    }


    /**
     * the HTTP/ftp proxy port number; default is 80
     *
     * @param port port number of the proxy
     */
    public void setProxyPort(int port) {
        proxyPort = port;
    }

    /**
     * The name of a Socks server. Set to "" to turn socks
     * proxying off.
     *
     * @param host The new SocksProxyHost value
     */
    public void setSocksProxyHost(String host) {
        this.socksProxyHost = host;
    }


    /**
     * Set the ProxyPort for socks connections. The default value is 1080
     *
     * @param port The new SocksProxyPort value
     */
    public void setSocksProxyPort(int port) {
        this.socksProxyPort = port;
    }


    /**
     * A list of hosts to bypass the proxy on. These should be separated
     * with the vertical bar character '|'. Only in Java 1.4 does ftp use
     * this list.
     * e.g. fozbot.corp.sun.com|*.eng.sun.com
     * @param nonProxyHosts lists of hosts to talk direct to
     */
    public void setNonProxyHosts(String nonProxyHosts) {
        this.nonProxyHosts = nonProxyHosts;
    }

    /**
     * set the proxy user. Probably requires a password to accompany this
     * setting. Default=""
     * @param proxyUser username
     * @since Ant1.6
     */
    public void setProxyUser(String proxyUser) {
        this.proxyUser = proxyUser;
    }

    /**
     * Set the password for the proxy. Used only if the proxyUser is set.
     * @param proxyPassword password to go with the username
     * @since Ant1.6
     */
    public void setProxyPassword(String proxyPassword) {
        this.proxyPassword = proxyPassword;
    }

    /**
     * if the proxy port and host settings are not null, then the settings
     * get applied these settings last beyond the life of the object and
     * apply to all network connections
     * Relevant docs: buglist #4183340
     */
    public void applyWebProxySettings() {
        boolean settingsChanged = false;
        boolean enablingProxy = false;
        Properties sysprops = System.getProperties();
        if (proxyHost != null) {
            settingsChanged = true;
            if (!proxyHost.isEmpty()) {
                traceSettingInfo();
                enablingProxy = true;
                sysprops.put(ProxySetup.HTTP_PROXY_HOST, proxyHost);
                String portString = Integer.toString(proxyPort);
                sysprops.put(ProxySetup.HTTP_PROXY_PORT, portString);
                sysprops.put(ProxySetup.HTTPS_PROXY_HOST, proxyHost);
                sysprops.put(ProxySetup.HTTPS_PROXY_PORT, portString);
                sysprops.put(ProxySetup.FTP_PROXY_HOST, proxyHost);
                sysprops.put(ProxySetup.FTP_PROXY_PORT, portString);
                if (nonProxyHosts != null) {
                    sysprops.put(ProxySetup.HTTP_NON_PROXY_HOSTS, nonProxyHosts);
                    sysprops.put(ProxySetup.HTTPS_NON_PROXY_HOSTS, nonProxyHosts);
                    sysprops.put(ProxySetup.FTP_NON_PROXY_HOSTS, nonProxyHosts);
                }
                if (proxyUser != null) {
                    sysprops.put(ProxySetup.HTTP_PROXY_USERNAME, proxyUser);
                    sysprops.put(ProxySetup.HTTP_PROXY_PASSWORD, proxyPassword);
                }
            } else {
                log("resetting http proxy", Project.MSG_VERBOSE);
                sysprops.remove(ProxySetup.HTTP_PROXY_HOST);
                sysprops.remove(ProxySetup.HTTP_PROXY_PORT);
                sysprops.remove(ProxySetup.HTTP_PROXY_USERNAME);
                sysprops.remove(ProxySetup.HTTP_PROXY_PASSWORD);
                sysprops.remove(ProxySetup.HTTPS_PROXY_HOST);
                sysprops.remove(ProxySetup.HTTPS_PROXY_PORT);
                sysprops.remove(ProxySetup.FTP_PROXY_HOST);
                sysprops.remove(ProxySetup.FTP_PROXY_PORT);
            }
        }

        //socks
        if (socksProxyHost != null) {
            settingsChanged = true;
            if (!socksProxyHost.isEmpty()) {
                enablingProxy = true;
                sysprops.put(ProxySetup.SOCKS_PROXY_HOST, socksProxyHost);
                sysprops.put(ProxySetup.SOCKS_PROXY_PORT, Integer.toString(socksProxyPort));
                if (proxyUser != null) {
                    //this may be a java1.4 thingy only
                    sysprops.put(ProxySetup.SOCKS_PROXY_USERNAME, proxyUser);
                    sysprops.put(ProxySetup.SOCKS_PROXY_PASSWORD, proxyPassword);
                }

            } else {
                log("resetting socks proxy", Project.MSG_VERBOSE);
                sysprops.remove(ProxySetup.SOCKS_PROXY_HOST);
                sysprops.remove(ProxySetup.SOCKS_PROXY_PORT);
                sysprops.remove(ProxySetup.SOCKS_PROXY_USERNAME);
                sysprops.remove(ProxySetup.SOCKS_PROXY_PASSWORD);
            }
        }

        if (proxyUser != null) {
            if (enablingProxy) {
                Authenticator.setDefault(new ProxyAuth(proxyUser,
                                                       proxyPassword));
            } else if (settingsChanged) {
                Authenticator.setDefault(new ProxyAuth("", ""));
            }
        }
    }

    /**
     * list out what is going on
     */
    private void traceSettingInfo() {
        log("Setting proxy to "
                + (proxyHost != null ? proxyHost : "''")
                + ":" + proxyPort,
                Project.MSG_VERBOSE);
    }

    /**
     * Does the work.
     *
     * @exception BuildException thrown in unrecoverable error.
     */
    @Override
    public void execute() throws BuildException {
        applyWebProxySettings();
    }

    /**
     * @since 1.6.3
     */
    private static final class ProxyAuth extends Authenticator {
        private PasswordAuthentication auth;

        private ProxyAuth(String user, String pass) {
            auth = new PasswordAuthentication(user, pass.toCharArray());
        }

        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
            return auth;
        }
    }
}

