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
package org.apache.tools.ant.types.resources.selectors;

import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.selectors.SelectorUtils;

/**
 * Name ResourceSelector.
 * @since Ant 1.7
 */
public class Name implements ResourceSelector {
    private String pattern;
    private boolean cs = true;

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
     * Return true if this Resource is selected.
     * @param r the Resource to check.
     * @return whether the Resource was selected.
     */
    public boolean isSelected(Resource r) {
        String n = r.getName();
        if (SelectorUtils.match(pattern, n, cs)) {
            return true;
        }
        String s = r.toString();
        return s.equals(n) ? false : SelectorUtils.match(pattern, s, cs);
    }

}
