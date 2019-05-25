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
 * Class to tokenize the input as areas separated
 * by white space, or by a specified list of
 * delim characters. Behaves like java.util.StringTokenizer.
 * If the stream starts with delim characters, the first
 * token will be an empty string (unless the treat delims
 * as tokens flag is set).
 * @since Ant 1.7
 */
public class StringTokenizer extends ProjectComponent implements Tokenizer {
    private static final int NOT_A_CHAR = -2;
    private String intraString = "";
    private int    pushed = NOT_A_CHAR;
    private char[] delims = null;
    private boolean delimsAreTokens = false;
    private boolean suppressDelims = false;
    private boolean includeDelims = false;

    /**
     * attribute delims - the delimiter characters
     * @param delims a string containing the delimiter characters
     */
    public void setDelims(String delims) {
        this.delims = StringUtils.resolveBackSlash(delims).toCharArray();
    }

    /**
     * attribute delimsaretokens - treat delimiters as
     * separate tokens.
     * @param delimsAreTokens true if delimiters are to be separate
     */

    public void setDelimsAreTokens(boolean delimsAreTokens) {
        this.delimsAreTokens = delimsAreTokens;
    }
    /**
     * attribute suppressdelims - suppress delimiters.
     * default - false
     * @param suppressDelims if true do not report delimiters
     */
    public void setSuppressDelims(boolean suppressDelims) {
        this.suppressDelims = suppressDelims;
    }

    /**
     * attribute includedelims - treat delimiters as part
     * of the token.
     * default - false
     * @param includeDelims if true add delimiters to the token
     */
    public void setIncludeDelims(boolean includeDelims) {
        this.includeDelims = includeDelims;
    }

    /**
     * find and return the next token
     *
     * @param in the input stream
     * @return the token
     * @exception IOException if an error occurs reading
     */
    public String getToken(Reader in) throws IOException {
        int ch = -1;
        if (pushed != NOT_A_CHAR) {
            ch = pushed;
            pushed = NOT_A_CHAR;
        } else {
            ch = in.read();
        }
        if (ch == -1) {
            return null;
        }
        boolean inToken = true;
        intraString = "";
        StringBuilder word = new StringBuilder();
        StringBuilder padding = new StringBuilder();
        while (ch != -1) {
            char c = (char) ch;
            boolean isDelim = isDelim(c);
            if (inToken) {
                if (isDelim) {
                    if (delimsAreTokens) {
                        if (word.length() > 0) {
                            pushed = ch;
                        } else {
                            word.append(c);
                        }
                        break;
                    }
                    padding.append(c);
                    inToken = false;
                } else {
                    word.append(c);
                }
            } else if (isDelim) {
                padding.append(c);
            } else {
                pushed = ch;
                break;
            }
            ch = in.read();
        }
        intraString = padding.toString();
        if (includeDelims) {
            word.append(intraString);
        }
        return word.toString();
    }

    /**
     * @return the intratoken string
     */
    @Override
    public String getPostToken() {
        return suppressDelims || includeDelims ? "" : intraString;
    }

    private boolean isDelim(char ch) {
        if (delims == null) {
            return Character.isWhitespace(ch);
        }
        for (char delim : delims) {
            if (delim == ch) {
                return true;
            }
        }
        return false;
    }
}
