/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2002 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in
 * the documentation and/or other materials provided with the
 * distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 * any, must include the following acknowlegement:
 * "This product includes software developed by the
 * Apache Software Foundation (http://www.apache.org/)."
 * Alternately, this acknowlegement may appear in the software itself,
 * if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "Ant" and "Apache Software
 * Foundation" must not be used to endorse or promote products derived
 * from this software without prior written permission. For written
 * permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 * nor may "Apache" appear in their names without prior written
 * permission of the Apache Group.
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

package org.apache.tools.ant.taskdefs.optional.net;

import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.reflect.*;
import org.apache.tools.ant.*;
import org.apache.tools.ant.util.JavaEnvUtils;

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
 * stop using the socks server
 
 
 
 * @see <a href="http://java.sun.com/j2se/1.4/docs/guide/net/properties.html">
 *  java 1.4 network property list</a>
 * @author Steve Loughran
  *@since       Ant 1.5
 * @ant.task
 */
public class SetProxy extends Task {

    /**
     * proxy details
     */
    protected String proxyHost = null;

    /**
     * name of proxy port
     */
    protected int proxyPort = 80;

    /**
     * socks host.
     */
    private String socksProxyHost = null;
    
    /**
     * socks proxy port. 1080 is the default
     */
    private int socksProxyPort = 1080;


    /**
     * list of non proxy hosts
     */ 
    private String nonProxyHosts = null;

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
     * if the proxy port and host settings are not null, then the settings
     * get applied these settings last beyond the life of the object and
     * apply to all network connections
     * Relevant docs: buglist #4183340
     * @return true if the settings were applied
     */

    public void applyWebProxySettings() {
        boolean settingsChanged = false;
        boolean enablingProxy = false;
        Properties sysprops = System.getProperties();
        if (proxyHost != null) {
            settingsChanged = true;
            if (proxyHost.length() != 0) {
                traceSettingInfo();
                enablingProxy = true;
                sysprops.put("http.proxyHost", proxyHost);
                String portString = Integer.toString(proxyPort);
                sysprops.put("http.proxyPort", portString);
                sysprops.put("https.proxyHost", proxyHost);
                sysprops.put("https.proxyPort", portString);
                sysprops.put("ftp.proxyHost", proxyHost);
                sysprops.put("ftp.proxyPort", portString);
                if (nonProxyHosts != null) {
                    sysprops.put("http.nonProxyHosts", nonProxyHosts);
                    sysprops.put("ftp.nonProxyHosts", nonProxyHosts);
                }                    
            } else {
                log("resetting http proxy", Project.MSG_VERBOSE);
                sysprops.remove("http.proxyPort");
                sysprops.remove("https.proxyHost");
                sysprops.remove("https.proxyPort");
                sysprops.remove("ftp.proxyHost");
                sysprops.remove("ftp.proxyPort");
            }
        }

        //socks
        if (socksProxyHost != null) {
            settingsChanged = true;
            if (socksProxyHost.length() != 0) {
                enablingProxy = true;
                sysprops.put("socksProxyHost", socksProxyHost);
                sysprops.put("socksProxyPort", Integer.toString(socksProxyPort));
            } else {
                log("resetting socks proxy", Project.MSG_VERBOSE);
                sysprops.remove("socksProxyHost");
                sysprops.remove("socksProxyPort");
            }
        }
        

        //for Java1.1 we need to tell the system that the settings are new
        if (settingsChanged &&
            JavaEnvUtils.isJavaVersion(JavaEnvUtils.JAVA_1_1)) {
            legacyResetProxySettingsCall(enablingProxy);
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
     * make a call to sun.net.www.http.HttpClient.resetProperties();
     * this is only needed for java 1.1; reflection is used to stop the compiler
     * whining, and in case cleanroom JVMs dont have the class.
     * @return true if we did something
     */

    protected boolean legacyResetProxySettingsCall(boolean setProxy) {
        System.getProperties().put("http.proxySet", new Boolean(setProxy).toString());
        try {
            Class c = Class.forName("sun.net.www.http.HttpClient");
            Method reset = c.getMethod("resetProperties", null);
            reset.invoke(null, null);
            return true;
        }
        catch (ClassNotFoundException cnfe) {
            return false;
        }
        catch (NoSuchMethodException e) {
            return false;
        }
        catch (IllegalAccessException e) {
            return false;
        }
        catch (InvocationTargetException e) {
            return false;
        }
    }


    /**
     * Does the work.
     *
     * @exception BuildException thrown in unrecoverable error.
     */
    public void execute() throws BuildException {
        applyWebProxySettings();
    }

}

