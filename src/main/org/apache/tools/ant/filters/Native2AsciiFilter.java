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

import org.apache.tools.ant.util.Native2AsciiUtils;

/**
 * A filter that performs translations from characters to their
 * Unicode-escape sequences and vice-versa.
 *
 * @since Ant 1.9.8
 */
public class Native2AsciiFilter extends TokenFilter.ChainableReaderFilter {
    private boolean reverse;

    /**
     * Flag the conversion to run in the reverse sense,
     * that is Ascii to Native encoding.
     *
     * @param reverse True if the conversion is to be reversed,
     *                otherwise false;
     */
    public void setReverse(boolean reverse) {
        this.reverse = reverse;
    }

    @Override
    public String filter(String line) {
        return reverse
            ? Native2AsciiUtils.ascii2native(line)
            : Native2AsciiUtils.native2ascii(line);
    }
}
