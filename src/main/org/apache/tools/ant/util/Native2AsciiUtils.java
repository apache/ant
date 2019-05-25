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

/**
 * Contains helper methods for Ant's built-in implementation of native2ascii.
 *
 * @since Ant 1.9.8
 */
public class Native2AsciiUtils {

    private static final int MAX_ASCII = 127;

    /**
     * Replaces non-ASCII characters with their Unicode-Escapes.
     * <p>Expects to be called once per line if applied to a file.</p>
     * @param line the input line
     * @return the translated line
     */
    public static String native2ascii(String line) {
        StringBuilder sb = new StringBuilder();
        for (char c : line.toCharArray()) {
            if (c <= MAX_ASCII) {
                sb.append(c);
            } else {
                sb.append(String.format("\\u%04x", (int) c));
            }
        }
        return sb.toString();
    }

    /**
     * Replaces Unicode-Escapes.
     * <p>Expects to be called once per line if applied to a file.</p>
     * @param line the input line
     * @return the translated line
     */
    public static String ascii2native(String line) {
        StringBuilder sb = new StringBuilder();
        int inputLen = line.length();
        for (int i = 0; i < inputLen; i++) {
            char c = line.charAt(i);
            if (c != '\\' || i >= inputLen - 5) {
                sb.append(c);
            } else { // backslash with enough remaining characters
                char u = line.charAt(++i);
                if (u == 'u') {
                    int unescaped = tryParse(line, i + 1);
                    if (unescaped >= 0) {
                        sb.append((char) unescaped);
                        i += 4;
                        continue;
                    }
                }
                // not a unicode escape
                sb.append(c).append(u);
            }
        }
        return sb.toString();
    }

    private static int tryParse(String line, int startIdx) {
        try {
            return Integer.parseInt(line.substring(startIdx, startIdx + 4), 16);
        } catch (NumberFormatException ex) {
            return -1;
        }
    }
}
