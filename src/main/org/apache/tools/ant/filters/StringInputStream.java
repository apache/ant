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
package org.apache.tools.ant.filters;

import java.io.StringReader;

import org.apache.tools.ant.util.ReaderInputStream;

/**
 * Wraps a String as an InputStream.
 *
 */
public class StringInputStream extends ReaderInputStream {

    /**
     * Composes a stream from a String
     *
     * @param source The string to read from. Must not be <code>null</code>.
     */
    public StringInputStream(String source) {
        super(new StringReader(source));
    }

    /**
     * Composes a stream from a String with the specified encoding
     *
     * @param source The string to read from. Must not be <code>null</code>.
     * @param encoding The encoding scheme.  Also must not be <code>null</code>.
     */
    public StringInputStream(String source, String encoding) {
        super(new StringReader(source), encoding);
    }

}
