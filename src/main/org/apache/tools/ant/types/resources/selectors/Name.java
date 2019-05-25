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
package org.apache.tools.ant.types.resources.selectors;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.RegularExpression;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.selectors.SelectorUtils;
import org.apache.tools.ant.util.regexp.Regexp;
import org.apache.tools.ant.util.regexp.RegexpUtil;

/**
 * Name ResourceSelector.
 * @since Ant 1.7
 */
public class Name implements ResourceSelector {
    private String regex = null;
    private String pattern;
    private boolean cs = true;
    private boolean handleDirSep = false;

    // caches for performance reasons
    private RegularExpression reg;
    private Regexp expression;

    private Project project;

    public void setProject(Project p) {
        project = p;
    }

    /**
     * Set the pattern to compare names against.
     * @param n the pattern String to set.
     */
    public void setName(String n) {
        pattern = n;
    }

    /**
     * Get the pattern used by this Name ResourceSelector.
     * @return the String selection pattern.
     */
    public String getName() {
        return pattern;
    }

    /**
     * Set the regular expression to compare names against.
     * @param r the regex to set.
     * @since Ant 1.8.0
     */
    public void setRegex(String r) {
        regex = r;
        reg = null;
    }

    /**
     * Get the regular expression used by this Name ResourceSelector.
     * @return the String selection pattern.
     * @since Ant 1.8.0
     */
    public String getRegex() {
        return regex;
    }

    /**
     * Set whether the name comparisons are case-sensitive.
     * @param b boolean case-sensitivity flag.
     */
    public void setCaseSensitive(boolean b) {
        cs = b;
    }

    /**
     * Learn whether this Name ResourceSelector is case-sensitive.
     * @return boolean case-sensitivity flag.
     */
    public boolean isCaseSensitive() {
        return cs;
    }

    /**
     * Attribute specifying whether to ignore the difference
     * between / and \ (the two common directory characters).
     * @param handleDirSep a boolean, default is false.
     * @since Ant 1.8.0
     */
    public void setHandleDirSep(boolean handleDirSep) {
        this.handleDirSep = handleDirSep;
    }

    /**
     * Whether the difference between / and \ (the two common
     * directory characters) is ignored.
     * @return boolean
     * @since Ant 1.8.0
     */
    public boolean doesHandledirSep() {
        return handleDirSep;
    }

    /**
     * Return true if this Resource is selected.
     * @param r the Resource to check.
     * @return whether the Resource was selected.
     */
    public boolean isSelected(Resource r) {
        String n = r.getName();
        if (matches(n)) {
            return true;
        }
        String s = r.toString();
        return !s.equals(n) && matches(s);
    }

    private boolean matches(String name) {
        if (pattern != null) {
            return SelectorUtils.match(modify(pattern), modify(name), cs);
        }
        if (reg == null) {
            reg = new RegularExpression();
            reg.setPattern(regex);
            expression = reg.getRegexp(project);
        }
        return expression.matches(modify(name), RegexpUtil.asOptions(cs));
    }

    private String modify(String s) {
        if (s == null || !handleDirSep || !s.contains("\\")) {
            return s;
        }
        return s.replace('\\', '/');
    }
}
