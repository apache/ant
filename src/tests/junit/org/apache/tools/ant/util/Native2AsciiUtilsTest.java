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

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class Native2AsciiUtilsTest {

    @Test
    public void doesntTouchAscii() {
        StringBuilder sb = new StringBuilder();
        for (char i = 0; i < 128; i++) {
            sb.append(i);
        }
        assertEquals(sb.toString(), Native2AsciiUtils.native2ascii(sb.toString()));
    }

    @Test
    public void escapes() {
        assertEquals("\\u00e4\\u00f6\\u00fc",
                     Native2AsciiUtils.native2ascii("\u00e4\u00f6\u00fc"));
    }

    @Test
    public void pads() {
        assertEquals("\\u00e4\\u01f6\\u12fc",
                     Native2AsciiUtils.native2ascii("\u00e4\u01f6\u12fc"));
    }

    @Test
    public void doesntTouchNonEscapes() {
        StringBuilder sb = new StringBuilder();
        for (char i = 0; i < 128; i++) {
            sb.append(i);
        }
        assertEquals(sb.toString(), Native2AsciiUtils.ascii2native(sb.toString()));
    }

    @Test
    public void unescapes() {
        assertEquals("\u00e4\u00f6\u00fc",
                     Native2AsciiUtils.ascii2native("\\u00e4\\u00f6\\u00fc"));
    }

    @Test
    public void leavesNonUnicodeBackslashesAlone() {
        assertEquals("\\abcdef", Native2AsciiUtils.ascii2native("\\abcdef"));
        assertEquals("\\u012j", Native2AsciiUtils.ascii2native("\\u012j"));
    }

    @Test
    public void dealsWithUnfinishedEscapes() {
        assertEquals("\u00e4", Native2AsciiUtils.ascii2native("\\u00e4"));
        assertEquals("\\u00e", Native2AsciiUtils.ascii2native("\\u00e"));
        assertEquals("\\u00", Native2AsciiUtils.ascii2native("\\u00"));
        assertEquals("\\u0", Native2AsciiUtils.ascii2native("\\u0"));
    }

}
