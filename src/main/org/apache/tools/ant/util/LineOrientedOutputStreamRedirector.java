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

import java.io.IOException;
import java.io.OutputStream;
/**
 * provide a concrete implementation of LineOrientedOutputStream
 * @since Ant 1.8.3
 *
 */
public class LineOrientedOutputStreamRedirector
        extends
            LineOrientedOutputStream {
    private OutputStream stream;
    public LineOrientedOutputStreamRedirector(OutputStream stream) {
        this.stream = stream;
    }
    
    protected void processLine(String line) throws IOException {
        stream.write((line + System.getProperty("line.separator")).getBytes());
    }
    
    public void close() throws IOException {
        super.close();
        stream.close();
    }

    public void flush() throws IOException {
        super.flush();
        stream.flush();
    }
}
