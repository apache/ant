/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999-2001 The Apache Software Foundation.  All rights
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
 * 4. The names "The Jakarta Project", "Ant", and "Apache Software
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

package org.apache.tools.ant.taskdefs.optional.http;

import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.reflect.*;
import org.apache.tools.ant.*;

/**
 * proxy definition task. This allows all web tasks in the build file
 * executed after this task to access the web through a proxy server
 *
 * @created March 17, 2001
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
     * set a proxy host. the port should be defined too
     *
     * @param hostname the new proxy hostname
     */
    public void setProxyHost(String hostname) {
        proxyHost = hostname;
    }


    /**
     * set the proxy port number.
     *
     * @param port port number of the proxy
     */
    public void setProxyPort(int port) {
        proxyPort = port;
    }


    /**
     * accessor to proxy hostname
     *
     * @return the hostname or null
     */

    public String getProxyHost() {
        return proxyHost;
    }


    /**
     * accessor to proxy hostname
     *
     * @return the port number
     */

    public int getProxyPort() {
        return proxyPort;
    }


    /**
     * Set the SocksProxyHost attribute
     *
     * @param host The new SocksProxyHost value
     */
    public void setSocksProxyHost(String host) {
        this.socksProxyHost = host;
    }


    /**
     * Set the SocksProxyPort attribute
     *
     * @param port The new SocksProxyPort value
     */
    public void setSocksProxyPort(int port) {
        this.socksProxyPort = port;
    }



    /**
     * if the proxy port and host settings are not null, then the settings
     * get applied these settings last beyond the life of the object and
     * apply to all network connections
     *
     * @return true if the settings were applied
     */

    public void applyWebProxySettings() {
        boolean settingsChanged=false;
        Properties prop = System.getProperties();
        if (getProxyHost() != null) {
            log("Setting proxy to " + getProxyHost() + ":" + getProxyPort(),
                    Project.MSG_VERBOSE);
            prop.put("http.proxyHost", getProxyHost());
            prop.put("http.proxyPort", String.valueOf(getProxyPort()));
            prop.put("https.proxyHost", getProxyHost());
            prop.put("https.proxyPort", String.valueOf(getProxyPort()));
            prop.put("ftp.proxyHost", getProxyHost());
            prop.put("ftp.proxyPort", String.valueOf(getProxyPort()));
            settingsChanged=true;
        }

        //socks
        if (socksProxyHost != null) {
            log("Setting proxy to " + getProxyHost() + ":" + getProxyPort(),
                    Project.MSG_VERBOSE);
            prop.put("socksProxyHost", socksProxyHost);
            prop.put("socksProxyPort", Integer.toString(socksProxyPort));
            settingsChanged=true;
        }

        //for Java1.1 we need to tell the system that the settings are new
        if(settingsChanged && project.getJavaVersion() == Project.JAVA_1_1) {
            prop.put("http.proxySet", "true");
            sun.net.www.http.HttpClient.resetProperties();
        }
        legacyResetProxySettingsCall();
    }


    /**
     * make a call to sun.net.www.http.HttpClient.resetProperties();
     * this is only needed for java 1.1; reflection is used to stop the compiler
     * whining, and in case cleanroom JVMs dont have the class.
     * @return Description of the Returned Value
     * @returns
     */

    protected boolean legacyResetProxySettingsCall() {
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
     * @exception BuildException Thrown in unrecoverable error.
     */
    public void execute()
        throws BuildException {
        applyWebProxySettings();
    }

}

