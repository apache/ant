/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.todo.taskdefs.sitraka;

/**
 * Socket element for connection. <tt>&lt;socket/&gt;</tt> defaults to host
 * 127.0.0.1 and port 4444 Otherwise it requires the host and port attributes to
 * be set: <tt> &lt;socket host=&quote;175.30.12.1&quote;
 * port=&quote;4567&quote;/&gt; </tt>
 *
 * @author RT
 */
public class Socket
{

    /**
     * default to localhost
     */
    private String host = "127.0.0.1";

    /**
     * default to 4444
     */
    private int port = 4444;

    public void setHost( String value )
    {
        host = value;
    }

    public void setPort( Integer value )
    {
        port = value.intValue();
    }

    /**
     * if no host is set, returning ':&lt;port&gt;', will take localhost
     *
     * @return Description of the Returned Value
     */
    public String toString()
    {
        return host + ":" + port;
    }
}
