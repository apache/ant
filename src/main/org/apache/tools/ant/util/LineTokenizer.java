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
import java.io.Reader;

import org.apache.tools.ant.ProjectComponent;

/**
 * class to tokenize the input as lines separated
 * by \r (mac style), \r\n (dos/windows style) or \n (unix style)
 * @since Ant 1.6
 */
public class LineTokenizer extends ProjectComponent
    implements Tokenizer {
    private static final int NOT_A_CHAR = -2;
    private String  lineEnd = "";
    private int     pushed = NOT_A_CHAR;
    private boolean includeDelims = false;

    /**
     * attribute includedelims - whether to include
     * the line ending with the line, or to return
     * it in the posttoken
     * default false
     * @param includeDelims if true include /r and /n in the line
     */

    public void setIncludeDelims(boolean includeDelims) {
        this.includeDelims = includeDelims;
    }

    /**
     * get the next line from the input
     *
     * @param in the input reader
     * @return the line excluding /r or /n, unless includedelims is set
     * @exception IOException if an error occurs reading
     */
    public String getToken(Reader in) throws IOException {
        int ch;
        if (pushed == NOT_A_CHAR) {
            ch = in.read();
        } else {
            ch = pushed;
            pushed = NOT_A_CHAR;
        }
        if (ch == -1) {
            return null;
        }

        lineEnd = "";
        StringBuilder line = new StringBuilder();

        int state = 0;
        while (ch != -1) {
            if (state == 0) {
                if (ch == '\r') {
                    state = 1;
                } else if (ch == '\n') {
                    lineEnd = "\n";
                    break;
                } else {
                    line.append((char) ch);
                }
            } else {
                state = 0;
                if (ch == '\n') {
                    lineEnd = "\r\n";
                } else {
                    pushed = ch;
                    lineEnd = "\r";
                }
                break;
            }
            ch = in.read();
        }
        if (ch == -1 && state == 1) {
            lineEnd = "\r";
        }

        if (includeDelims) {
            line.append(lineEnd);
        }
        return line.toString();
    }

    /**
     * @return the line ending character(s) for the current line
     */
    @Override
    public String getPostToken() {
        return includeDelims ? "" : lineEnd;
    }

}

