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

package org.apache.tools.ant;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * As the name suggests, utility class used in tests
 */
public class TestHelper {

    /**
     * Find a TCP/IP port which may continue to be available.
     * <br />
     * The returned port is available since a socket has successfully bound to it, but this availability is not ensured
     * after this method since the associated socket is released and some other process can now use it.
     */
    public static int getMaybeAvailablePort() {
        try (ServerSocket s = new ServerSocket(0)) {
            s.setReuseAddress(true);
            return s.getLocalPort();
        } catch (IOException e) {
            // ignore
        }
        throw new IllegalStateException("No TCP/IP port available");
    }
}
