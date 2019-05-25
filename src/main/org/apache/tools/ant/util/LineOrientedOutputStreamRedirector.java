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

import java.io.IOException;
import java.io.OutputStream;

/**
 * Output stream which buffer and redirect a stream line by line.
 * <p>
 * If the source stream doesn't end with a end of line, one will be added. This
 * is particularly useful in combination with the OutputStreamFunneler so each
 * funneled stream get its line.
 *
 * @since Ant 1.8.3
 */
public class LineOrientedOutputStreamRedirector
        extends LineOrientedOutputStream {

    private OutputStream stream;

    public LineOrientedOutputStreamRedirector(OutputStream stream) {
        this.stream = stream;
    }

    @Override
    protected void processLine(byte[] b) throws IOException {
        stream.write(b);
        stream.write(System.lineSeparator().getBytes());
    }

    @Override
    protected void processLine(String line) throws IOException {
        stream.write(String.format("%s%n", line).getBytes());
    }

    @Override
    public void close() throws IOException {
        super.close();
        stream.close();
    }

    @Override
    public void flush() throws IOException {
        super.flush();
        stream.flush();
    }
}
