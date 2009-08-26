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
package org.apache.tools.ant.filters;

import java.io.IOException;
import java.io.Reader;

/**
 * Like the Unix uniq(1) command, only returns lines that are
 * different from their ancestor line.
 *
 * <p>This filter is probably most useful if used together with a sortfilter.</p>
 *
 * @since Ant 1.8.0
 */
public class UniqFilter extends BaseFilterReader implements ChainableReader {

    private String lastLine = null;
    private String currentLine = null;

    public UniqFilter() { }

    public UniqFilter(Reader rdr) {
        super(rdr);
    }

    public int read() throws IOException {
        int ch = -1;
        if (currentLine != null) {
            ch = currentLine.charAt(0);
            if (currentLine.length() == 1) {
                currentLine = null;
            } else {
                currentLine = currentLine.substring(1);
            }
        } else {
            do {
                currentLine = readLine();
            } while (lastLine != null && currentLine != null
                     && lastLine.equals(currentLine));
            lastLine = currentLine;
            if (currentLine != null) {
                return read();
            }
        }
        return ch;
    }

    public Reader chain(final Reader rdr) {
        UniqFilter newFilter = new UniqFilter(rdr);
        newFilter.setInitialized(true);
        return newFilter;
    }
}
