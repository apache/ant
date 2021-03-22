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

package org.apache.tools.ant.util;

import java.io.OutputStream;

/**
 * OutputStream that completely discards all data written to it.
 *
 * @since Ant 1.10.10
 */
public class NullOutputStream extends OutputStream {

    /**
     * Shared instance which is safe to use concurrently as the stream
     * doesn't hold any state at all.
     */
    public static NullOutputStream INSTANCE = new NullOutputStream();

    private NullOutputStream() { }

    /**
     * Doesn't do anything.
     */
    @Override
    public void write(byte[] b) { }

    /**
     * Doesn't do anything.
     */
    @Override
    public void write(byte[] b, int off, int len) { }

    /**
     * Doesn't do anything.
     */
    @Override
    public void write(int i) { }
}
