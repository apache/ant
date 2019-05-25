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
 * Contains one helper method to create a backslash u escape
 *
 * @since Ant 1.8.3
 */
public class UnicodeUtil {

    private UnicodeUtil() {
    }

    /**
     * returns the unicode representation of a char without the leading backslash
     * @param ch a character
     * @return unicode representation of a char for property files
     */
    public static StringBuffer EscapeUnicode(char ch) {
        StringBuffer unicodeBuf = new StringBuffer("u0000");
        String s = Integer.toHexString(ch);
        //replace the last 0s by the chars contained in s
        for (int i = 0; i < s.length(); i++) {
            unicodeBuf.setCharAt(unicodeBuf.length()
                                 - s.length() + i,
                                 s.charAt(i));
        }
        return unicodeBuf;
    }
}
