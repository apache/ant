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
import java.io.IOException;

/**
 * Test for a host being reachable using ICMP "ping" packets.
 * Ping packets are very reliable for assessing reachability in a LAN or WAN,
 * but they do not get through any well-configured firewall.
 *
 * This condition turns unknown host exceptions into false conditions. This is
 * because on a laptop, DNS is one of the first services when the network goes; you
 * are implicitly offline.
 * Requires Java1.5+ to work
 * @since Ant1.7
 */
public class IsPingable extends ProjectComponent implements Condition  {

    private String host;
    /** The default timeout. */
    public static final int DEFAULT_TIMEOUT = 30;
    private int timeout = DEFAULT_TIMEOUT;
    /** Error when no hostname is defined */
    public static final String ERROR_NO_HOSTNAME = "No hostname defined";
    /** Error when invalid timeout value is defined */
    public static final String ERROR_BAD_TIMEOUT = "Invalid timeout value";
    /** Unknown host message is seen. */
    public static final String ERROR_UNKNOWN_HOST = "Unknown host:";
    /** Network error message is seen. */
    public static final String ERROR_ON_NETWORK = "network error to ";

    /**
     * The host to ping.
     * @param host the host to ping.
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * Timeout for the reachability test -in seconds.
     * @param timeout the timeout in seconds.
     */
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    /**
     * Is this condition true?
     *
     * @return true if the condition is true.
     * @throws org.apache.tools.ant.BuildException
     *          if an error occurs
     */
    public boolean eval() throws BuildException {
        if (host == null || host.length() == 0) {
            throw new BuildException(ERROR_NO_HOSTNAME);
        }
        if (timeout < 0) {
            throw new BuildException(ERROR_BAD_TIMEOUT);
        }
        try {
            InetAddress address = InetAddress.getByName(host);
            return address.isReachable(timeout * 1000);
        } catch (UnknownHostException e) {
            log(ERROR_UNKNOWN_HOST + host, Project.MSG_VERBOSE);
            return false;
        } catch (IOException e) {
            log(ERROR_ON_NETWORK + host + ": " + e.toString(),
                    Project.MSG_VERBOSE);
            return false;
        }
    }
}
