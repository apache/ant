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

package org.apache.tools.ant.taskdefs.condition;

import org.apache.tools.ant.BuildException;

/**
 * Is one string part of another string?
 *
 *
 * @since Ant 1.5
 */
public class Contains implements Condition {

    private String string, subString;
    private boolean caseSensitive = true;

    /**
     * The string to search in.
     * @param string the string to search in
     * @since Ant 1.5
     */
    public void setString(String string) {
        this.string = string;
    }

    /**
     * The string to search for.
     * @param subString the string to search for
     * @since Ant 1.5
     */
    public void setSubstring(String subString) {
        this.subString = subString;
    }

    /**
     * Whether to search ignoring case or not.
     * @param b if false, ignore case
     * @since Ant 1.5
     */
    public void setCasesensitive(boolean b) {
        caseSensitive = b;
    }

    /**
     * @since Ant 1.5
     * @return true if the substring is within the string
     * @exception BuildException if the attributes are not set correctly
     */
    @Override
    public boolean eval() throws BuildException {
        if (string == null || subString == null) {
            throw new BuildException(
                "both string and substring are required in contains");
        }

        return caseSensitive
            ? string.contains(subString)
            : string.toLowerCase().contains(subString.toLowerCase());
    }
}
