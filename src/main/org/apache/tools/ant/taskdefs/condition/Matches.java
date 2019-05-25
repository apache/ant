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
import org.apache.tools.ant.ProjectComponent;
import org.apache.tools.ant.types.RegularExpression;
import org.apache.tools.ant.util.regexp.Regexp;
import org.apache.tools.ant.util.regexp.RegexpUtil;

/**
 * Simple regular expression condition.
 *
 * @since Ant 1.7
 */
public class Matches extends ProjectComponent implements Condition {

    private String  string;
    private boolean caseSensitive = true;
    private boolean multiLine = false;
    private boolean singleLine = false;
    private RegularExpression regularExpression;

    /**
     * Set the string
     *
     * @param string the string to match
     */
    public void setString(String string) {
        this.string = string;
    }

    /**
     * Set the regular expression to match against
     *
     * @param pattern the regular expression pattern
     */
    public void setPattern(String pattern) {
        if (regularExpression != null) {
            throw new BuildException(
                "Only one regular expression is allowed.");
        }
        regularExpression = new RegularExpression();
        regularExpression.setPattern(pattern);
    }

    /**
     * A regular expression.
     * You can use this element to refer to a previously
     * defined regular expression datatype instance
     * @param regularExpression the regular expression object
     *                          to be configured as an element
     */
    public void addRegexp(RegularExpression regularExpression) {
        if (this.regularExpression != null) {
            throw new BuildException(
                "Only one regular expression is allowed.");
        }
        this.regularExpression = regularExpression;
    }

    /**
     * Whether to ignore case or not.
     * @param b if false, ignore case.
     * @since Ant 1.7
     */
    public void setCasesensitive(boolean b) {
        caseSensitive = b;
    }

    /**
     * Whether to match should be multiline.
     * @param b the value to set.
     */
    public void setMultiline(boolean b) {
        multiLine = b;
    }

    /**
     * Whether to treat input as singleline ('.' matches newline).
     * Corresponds to java.util.regex.Pattern.DOTALL.
     * @param b the value to set.
     */
    public void setSingleLine(boolean b) {
        singleLine = b;
    }

    /**
     * @return true if the string matches the regular expression pattern
     * @exception BuildException if the attributes are not set correctly
     */
    public boolean eval() throws BuildException {
        if (string == null) {
            throw new BuildException(
                "Parameter string is required in matches.");
        }
        if (regularExpression == null) {
            throw new BuildException("Missing pattern in matches.");
        }
        int options = RegexpUtil.asOptions(caseSensitive, multiLine, singleLine);
        Regexp regexp = regularExpression.getRegexp(getProject());
        return regexp.matches(string, options);
    }
}
