/*
 * Copyright  2001-2002,2004 Apache Software Foundation
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
package org.apache.tools.ant.taskdefs.optional.sitraka;

/**
 * Define a host and port to connect to if you want to do remote viewing.
 * <tt>&lt;socket/&gt;</tt> defaults to host 127.0.0.1 and port 4444
 *
 * Otherwise it requires the host and port attributes to be set:
 * <tt>
 * &lt;socket host=&quote;175.30.12.1&quote; port=&quote;4567&quote;/&gt;
 * </tt>
 */
public class Socket {

    /** default to localhost */
    private String host = "127.0.0.1";

    /** default to 4444 */
    private int port = 4444;

    /**
     * the host name/ip of the machine on which the Viewer is running;
     * defaults to localhost.
     */
    public void setHost(String value) {
        host = value;
    }

    /**
     * Optional port number for the viewer; default is 4444
     */
    public void setPort(Integer value) {
        port = value.intValue();
    }

    /** if no host is set, returning ':&lt;port&gt;', will take localhost */
    public String toString() {
        return host + ":" + port;
    }
}
