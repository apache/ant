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

package org.apache.tools.ant.taskdefs;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

/**
 * Sets properties to the host provided, or localhost if no information is
 * provided. The default properties are NAME, FQDN, ADDR4, ADDR6;
 *
 * @since Ant 1.8
 * @ant.task category="utility"
 */


public class HostInfo extends Task {
    private static final String DEF_REM_ADDR6 = "::";

    private static final String DEF_REM_ADDR4 = "0.0.0.0"; //NOSONAR

    private static final String DEF_LOCAL_ADDR6 = "::1";

    private static final String DEF_LOCAL_ADDR4 = "127.0.0.1"; //NOSONAR

    private static final String DEF_LOCAL_NAME = "localhost";
    private static final String DEF_DOMAIN = "localdomain";

    private static final String DOMAIN = "DOMAIN";

    private static final String NAME = "NAME";

    private static final String ADDR4 = "ADDR4";

    private static final String ADDR6 = "ADDR6";

    private String prefix = "";

    private String host;

    private InetAddress nameAddr;

    private InetAddress best6;

    private InetAddress best4;

    private List<InetAddress> inetAddrs;

    /**
     * Set a prefix for the properties. If the prefix does not end with a "."
     * one is automatically added.
     *
     * @param aPrefix
     *            the prefix to use.
     * @since Ant 1.8
     */
    public void setPrefix(String aPrefix) {
        prefix = aPrefix;
        if (!prefix.endsWith(".")) {
            prefix += ".";
        }
    }

    /**
     * Set the host to be retrieved.
     *
     * @param aHost
     *            the name or the address of the host, data for the local host
     *            will be retrieved if omitted.
     * @since Ant 1.8
     */
    public void setHost(String aHost) {
        host = aHost;
    }

    /**
     * set the properties.
     *
     * @throws BuildException
     *             on error.
     */
    @Override
    public void execute() throws BuildException {
        if (host == null || host.isEmpty()) {
            executeLocal();
        } else {
            executeRemote();
        }
    }

    private void executeLocal() {
        try {
            inetAddrs = new LinkedList<>();
            Collections.list(NetworkInterface.getNetworkInterfaces())
                    .forEach(netInterface -> inetAddrs.addAll(Collections.list(netInterface.getInetAddresses())));
            selectAddresses();

            if (nameAddr != null && hasHostName(nameAddr)) {
                setDomainAndName(nameAddr.getCanonicalHostName());
            } else {
                setProperty(DOMAIN, DEF_DOMAIN);
                setProperty(NAME, DEF_LOCAL_NAME);
            }
            if (best4 != null) {
                setProperty(ADDR4, best4.getHostAddress());
            } else {
                setProperty(ADDR4, DEF_LOCAL_ADDR4);
            }
            if (best6 != null) {
                setProperty(ADDR6, best6.getHostAddress());
            } else {
                setProperty(ADDR6, DEF_LOCAL_ADDR6);
            }
        } catch (Exception e) {
            log("Error retrieving local host information", e, Project.MSG_WARN);
            setProperty(DOMAIN, DEF_DOMAIN);
            setProperty(NAME, DEF_LOCAL_NAME);
            setProperty(ADDR4, DEF_LOCAL_ADDR4);
            setProperty(ADDR6, DEF_LOCAL_ADDR6);
        }
    }

    private boolean hasHostName(InetAddress addr) {
        return !addr.getHostAddress().equals(addr.getCanonicalHostName());
    }

    private void selectAddresses() {
        for (InetAddress current : inetAddrs) {
            if (!current.isMulticastAddress()) {
                if (current instanceof Inet4Address) {
                    best4 = selectBestAddress(best4, current);
                } else if (current instanceof Inet6Address) {
                    best6 = selectBestAddress(best6, current);
                }
            }
        }

        nameAddr = selectBestAddress(best4, best6);
    }

    private InetAddress selectBestAddress(InetAddress bestSoFar,
            InetAddress current) {
        InetAddress best = bestSoFar;
        if (best == null) {
            // none selected so far, so this one is better.
            best = current;
        } else if (current == null || current.isLoopbackAddress()) {
            // definitely not better than the previously selected address.
        } else if (current.isLinkLocalAddress()) {
            // link local considered better than loopback
            if (best.isLoopbackAddress()) {
                best = current;
            }
        } else if (current.isSiteLocalAddress()) {
            // site local considered better than link local (and loopback)
            // address with hostname resolved considered better than
            // address without hostname
            if (best.isLoopbackAddress()
                    || best.isLinkLocalAddress()
                    || (best.isSiteLocalAddress() && !hasHostName(best))) {
                best = current;
            }
        } else {
            // current is a "Global address", considered better than
            // site local (and better than link local, loopback)
            // address with hostname resolved considered better than
            // address without hostname
            if (best.isLoopbackAddress()
                    || best.isLinkLocalAddress()
                    || best.isSiteLocalAddress()
                    || !hasHostName(best)) {
                best = current;
            }
        }
        return best;
    }

    private void executeRemote() {
        try {
            inetAddrs = Arrays.asList(InetAddress.getAllByName(host));

            selectAddresses();

            if (nameAddr != null && hasHostName(nameAddr)) {
                setDomainAndName(nameAddr.getCanonicalHostName());
            } else {
                setDomainAndName(host);
            }
            if (best4 != null) {
                setProperty(ADDR4, best4.getHostAddress());
            } else {
                setProperty(ADDR4, DEF_REM_ADDR4);
            }
            if (best6 != null) {
                setProperty(ADDR6, best6.getHostAddress());
            } else {
                setProperty(ADDR6, DEF_REM_ADDR6);
            }
        } catch (Exception e) {
            log("Error retrieving remote host information for host:" + host
                    + ".", e, Project.MSG_WARN);
            setDomainAndName(host);
            setProperty(ADDR4, DEF_REM_ADDR4);
            setProperty(ADDR6, DEF_REM_ADDR6);
        }
    }

    private void setDomainAndName(String fqdn) {
        int idx = fqdn.indexOf('.');
        if (idx > 0) {
            setProperty(NAME, fqdn.substring(0, idx));
            setProperty(DOMAIN, fqdn.substring(idx + 1));
        } else {
            setProperty(NAME, fqdn);
            setProperty(DOMAIN, DEF_DOMAIN);
        }
    }

    private void setProperty(String name, String value) {
        getProject().setNewProperty(prefix + name, value);
    }

}
