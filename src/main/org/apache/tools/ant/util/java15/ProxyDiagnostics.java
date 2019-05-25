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

package org.apache.tools.ant.util.java15;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.tools.ant.BuildException;

/**
 * This class exists to create a string that tells diagnostics about the current
 * state of proxy diagnostics.
 * It does this in its toString operator.
 * Java1.5+ is needed to compile this class; its interface is classic typeless
 * Java.
 * @since Ant 1.7
 */
public class ProxyDiagnostics {

    private URI destURI;

    /** {@value} */
    public static final String DEFAULT_DESTINATION = "https://ant.apache.org/";

    /**
     * create a diagnostics binding for a specific URI
     * @param destination dest to bind to
     * @throws BuildException if the URI is malformed.
     */
    public ProxyDiagnostics(String destination) {
        try {
            this.destURI = new URI(destination);
        } catch (URISyntaxException e) {
            throw new BuildException(e);
        }
    }

    /**
     * create a proxy diagnostics tool bound to
     * {@link #DEFAULT_DESTINATION}
     */
    public ProxyDiagnostics() {
        this(DEFAULT_DESTINATION);
    }

    /**
     * Get the diagnostics for proxy information.
     * @return the information.
     */
    @Override
    public String toString() {
        ProxySelector selector = ProxySelector.getDefault();
        StringBuilder result = new StringBuilder();
        for (Proxy proxy : selector.select(destURI)) {
            SocketAddress address = proxy.address();
            if (address == null) {
                result.append("Direct connection\n");
                continue;
            }
            result.append(proxy);
            if (address instanceof InetSocketAddress) {
                InetSocketAddress ina = (InetSocketAddress) address;
                result.append(' ');
                result.append(ina.getHostName());
                result.append(':');
                result.append(ina.getPort());
                if (ina.isUnresolved()) {
                    result.append(" [unresolved]");
                } else {
                    InetAddress addr = ina.getAddress();
                    result.append(" [");
                    result.append(addr.getHostAddress());
                    result.append(']');
                }
            }
            result.append('\n');
        }
        return result.toString();
    }
}
