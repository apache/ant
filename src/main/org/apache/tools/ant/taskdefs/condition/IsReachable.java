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

package org.apache.tools.ant.taskdefs.condition;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectComponent;

/**
 * <p>Test for a host being reachable using ICMP "ping" packets &amp; echo operations.
 * Ping packets are very reliable for assessing reachability in a LAN or WAN,
 * but they do not get through any well-configured firewall. Echo (port 7) may.</p>
 *
 * <p>This condition turns unknown host exceptions into false conditions. This is
 * because on a laptop, DNS is one of the first services lost when the network
 * goes; you are implicitly offline.</p>
 *
 * <p>If a URL is supplied instead of a host, the hostname is extracted and used in
 * the test--all other parts of the URL are discarded.</p>
 *
 * <p>The test may not work through firewalls; that is, something may be reachable
 * using a protocol such as HTTP, while the lower level ICMP packets get dropped
 * on the floor. Similarly, a host may be detected as reachable with ICMP, but not
 * reachable on other ports (i.e. port 80), because of firewalls.</p>
 *
 * @since Ant 1.7
 */
public class IsReachable extends ProjectComponent implements Condition {
    /**
     * The default timeout.
     */
    public static final int DEFAULT_TIMEOUT = 30;

    private static final int SECOND = 1000; // millis per second

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
    private static final String WARN_UNKNOWN_HOST = "Unknown host: ";
    /**
     * Network error message is seen.
     */
    public static final String ERROR_ON_NETWORK = "network error to ";
    /** Error message when url and host are specified. */
    public static final String ERROR_BOTH_TARGETS
        = "Both url and host have been specified";
    /** Error message when no reachably test avail. */
    public static final String MSG_NO_REACHABLE_TEST
        = "cannot do a proper reachability test on this Java version";
    /** Error message when an invalid url is used. */
    public static final String ERROR_BAD_URL = "Bad URL ";
    /** Error message when no hostname in url. */
    public static final String ERROR_NO_HOST_IN_URL = "No hostname in URL ";
    /**
     * The method name to look for in InetAddress
     * @deprecated Since 1.10.6
     */
    @Deprecated
    public static final String METHOD_NAME = "isReachable";

    private String host;
    private String url;

    private int timeout = DEFAULT_TIMEOUT;

    /**
     * Set the host to ping.
     *
     * @param host the host to ping.
     */
    public void setHost(final String host) {
        this.host = host;
    }

    /**
     * Set the URL from which to extract the hostname.
     *
     * @param url a URL object.
     */
    public void setUrl(final String url) {
        this.url = url;
    }

    /**
     * Set the timeout for the reachability test in seconds.
     *
     * @param timeout the timeout in seconds.
     */
    public void setTimeout(final int timeout) {
        this.timeout = timeout;
    }

    /**
     * emptyness test
     *
     * @param string param to check
     *
     * @return true if it is isNullOrEmpty
     */
    private boolean isNullOrEmpty(final String string) {
        return string == null || string.isEmpty();
    }

    /**
     * Evaluate the condition.
     *
     * @return true if the condition is true.
     *
     * @throws BuildException
     *          if an error occurs
     */
    @Override
    public boolean eval() throws BuildException {
        if (isNullOrEmpty(host) && isNullOrEmpty(url)) {
            throw new BuildException(ERROR_NO_HOSTNAME);
        }
        if (timeout < 0) {
            throw new BuildException(ERROR_BAD_TIMEOUT);
        }
        String target = host;
        if (!isNullOrEmpty(url)) {
            if (!isNullOrEmpty(host)) {
                throw new BuildException(ERROR_BOTH_TARGETS);
            }
            try {
                //get the host of a url
                final URL realURL = new URL(url);
                target = realURL.getHost();
                if (isNullOrEmpty(target)) {
                    throw new BuildException(ERROR_NO_HOST_IN_URL + url);
                }
            } catch (final MalformedURLException e) {
                throw new BuildException(ERROR_BAD_URL + url, e);
            }
        }
        log("Probing host " + target, Project.MSG_VERBOSE);
        InetAddress address;
        try {
            address = InetAddress.getByName(target);
        } catch (final UnknownHostException e1) {
            log(WARN_UNKNOWN_HOST + target);
            return false;
        }
        log("Host address = " + address.getHostAddress(),
                Project.MSG_VERBOSE);
        boolean reachable;
        try {
            reachable = address.isReachable(timeout * SECOND);
        } catch (final IOException ioe) {
            reachable = false;
            log(ERROR_ON_NETWORK + target + ": " + ioe.toString());
        }

        log("host is" + (reachable ? "" : " not") + " reachable", Project.MSG_VERBOSE);
        return reachable;
    }
}
