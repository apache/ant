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

package org.apache.tools.ant.util;

import org.apache.tools.ant.Project;

/**
 * Code to do proxy setup. This is just factored out of the main system just to
 * keep everything else less convoluted.
 * @since Ant1.7
 */

public class ProxySetup {

    /**
     * owner project; used for logging and extracting properties
     */
    private Project owner;

    /**
     * Java1.5 property that enables use of system proxies.
     * @value
     */
    public static final String USE_SYSTEM_PROXIES="java.net.useSystemProxies";
    public static final String HTTP_PROXY_HOST = "http.proxyHost";
    public static final String HTTP_PROXY_PORT = "http.proxyPort";
    public static final String HTTPS_PROXY_HOST = "https.proxyHost";
    public static final String HTTPS_PROXY_PORT = "https.proxyPort";
    public static final String FTP_PROXY_HOST = "ftp.proxyHost";
    public static final String FTP_PROXY_PORT = "ftp.proxyPort";
    public static final String HTTP_NON_PROXY_HOSTS = "http.nonProxyHosts";
    public static final String HTTPS_NON_PROXY_HOSTS = "https.nonProxyHosts";
    public static final String FTP_NON_PROXY_HOSTS = "ftp.nonProxyHosts";
    public static final String HTTP_PROXY_USERNAME = "http.proxyUser";
    public static final String HTTP_PROXY_PASSWORD = "http.proxyPassword";
    public static final String SOCKS_PROXY_HOST = "socksProxyHost";
    public static final String SOCKS_PROXY_PORT = "socksProxyPort";
    public static final String SOCKS_PROXY_USERNAME = "java.net.socks.username";
    public static final String SOCKS_PROXY_PASSWORD = "java.net.socks.password";


    /**
     * create a proxy setup class bound to this project
     * @param owner
     */
    public ProxySetup(Project owner) {
        this.owner = owner;
    }

    /**
     * Get the current system property settings
     * @return current value; null for none or no access
     */
    public static String getSystemProxySetting() {
        try {
            return System.getProperty(USE_SYSTEM_PROXIES);
        } catch (SecurityException e) {
            //if you cannot read it, you won't be able to write it either
            return null;
        }
    }

    /**
     * turn proxies on;
     * if the proxy key is already set to some value: leave alone.
     * if an ant property of the value {@link #USE_SYSTEM_PROXIES}
     * is set, use that instead. Else set to "true".
     */
    public void enableProxies() {
        if(!(getSystemProxySetting() != null)) {
            String proxies =owner.getProperty(USE_SYSTEM_PROXIES);
            if(proxies ==null || Project.toBoolean(proxies)) {
                proxies ="true";
            }
            String message = "setting " + USE_SYSTEM_PROXIES + " to " + proxies;
            try {
                owner.log(message,Project.MSG_DEBUG);
                System.setProperty(USE_SYSTEM_PROXIES,proxies);
            } catch (SecurityException e) {
                //log security exceptions and continue; it aint that
                //important and may be quite common running Ant embedded.
                owner.log("Security Exception when "+message);
            }
        }
    }

}
