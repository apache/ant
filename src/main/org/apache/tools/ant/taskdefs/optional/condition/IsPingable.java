/*
 * Copyright  2004-2005 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
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

package org.apache.tools.ant.taskdefs.optional.condition;

import org.apache.tools.ant.ProjectComponent;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.condition.Condition;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.URL;
import java.net.MalformedURLException;
import java.io.IOException;

/**
 * Test for a host being reachable using ICMP "ping" packets.
 * Ping packets are very reliable for assessing reachability in a LAN or WAN,
 * but they do not get through any well-configured firewall.
 * <p/>
 * This condition turns unknown host exceptions into false conditions. This is
 * because on a laptop, DNS is one of the first services when the network goes; you
 * are implicitly offline.
 * <p/>
 * If a URL is supplied instead of a host, the hostname is extracted
 * and used in the test - all other parts of the URL are discarded.
 * <p/>
 * The test may not work through firewalls, that is, something may be reachable
 * using a protocol such as HTTP, while the lower level ICMP packets get dropped
 * on the floor. Similarly, a host may detected as reachable with ICMP, but
 * not reachable on other ports (i.e. port 80), because of firewalls.
 * <p/>
 * Requires Java1.5+ to work
 *
 * @ant.condition name="isreachable"
 * @since Ant1.7
 */
public class IsPingable extends ProjectComponent implements Condition {

    private String host;
    private String url;

    /**
     * The default timeout.
     */
    public static final int DEFAULT_TIMEOUT = 30;
    private int timeout = DEFAULT_TIMEOUT;
    /**
     * Error when no hostname is defined
     */
    public static final String ERROR_NO_HOSTNAME = "No hostname defined";
    /**
     * Error when invalid timeout value is defined
     */
    public static final String ERROR_BAD_TIMEOUT = "Invalid timeout value";
    /**
     * Unknown host message is seen.
     */
    public static final String ERROR_UNKNOWN_HOST = "Unknown host:";
    /**
     * Network error message is seen.
     */
    public static final String ERROR_ON_NETWORK = "network error to ";
    public static final String ERROR_BOTH_TARGETS = "Both url and host have been specified";

    /**
     * The host to ping.
     *
     * @param host the host to ping.
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * A URL to extract the hostname from
     *
     * @param url
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * Timeout for the reachability test -in seconds.
     *
     * @param timeout the timeout in seconds.
     */
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    /**
     * emptyness test
     *
     * @param string param to check
     * @return true if it is empty
     */
    private boolean empty(String string) {
        return string == null || string.length() == 0;
    }

    /**
     * Is this condition true?
     *
     * @return true if the condition is true.
     * @throws org.apache.tools.ant.BuildException
     *          if an error occurs
     */
    public boolean eval() throws BuildException {
        if (empty(host) && empty(url)) {
            throw new BuildException(ERROR_NO_HOSTNAME);
        }
        if (timeout < 0) {
            throw new BuildException(ERROR_BAD_TIMEOUT);
        }
        String target = host;
        if (!empty(url)) {
            if (!empty(host)) {
                throw new BuildException(ERROR_BOTH_TARGETS);
            }
            try {
                //get the host of a url
                URL realURL = new URL(url);
                target = realURL.getHost();
            } catch (MalformedURLException e) {
                throw new BuildException("Bad URL " + url, e);
            }
        }
        try {
            log("Probing host " + target, Project.MSG_VERBOSE);
            InetAddress address = InetAddress.getByName(target);
            log("Host address =" + address.getHostAddress(),
                    Project.MSG_VERBOSE);
            final boolean reachable = address.isReachable(timeout * 1000);
            log("host is " + (reachable ? "" : "not") + " reachable",
                    Project.MSG_VERBOSE);
            return reachable;
        } catch (UnknownHostException e) {
            log(ERROR_UNKNOWN_HOST + target);
            return false;
        } catch (IOException e) {
            log(ERROR_ON_NETWORK + target + ": " + e.toString());
            return false;
        }
    }
}
