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

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Class that can be used to wrap <tt>System.out</tt> and <tt>System.err</tt>
 * without getting anxious about any client closing the stream.
 *
 * <p>
 * In code-language it means that it is not necessary to do:
 * <pre>
 * if (out != System.out && out!= System.err) {
 *   out.close();
 * }
 * </pre>
 * </p>
 *
 */
public class KeepAliveOutputStream extends FilterOutputStream {

    /**
     * Constructor of KeepAliveOutputStream.
     *
     * @param out an OutputStream value, it shoudl be standard output.
     */
    public KeepAliveOutputStream(OutputStream out) {
        super(out);
    }

    /**
     * This method does nothing.
     * @throws IOException as we are overridding FilterOutputStream.
     */
    public void close() throws IOException {
        // do not close the stream
    }
}
