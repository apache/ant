/*
 * Copyright  2001-2002,2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
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

package org.apache.tools.ant.taskdefs.optional.sitraka;

import java.util.Vector;
import org.apache.tools.ant.util.regexp.RegexpMatcher;
import org.apache.tools.ant.util.regexp.RegexpMatcherFactory;

/**
 * Filters information from coverage, somewhat similar to a <tt>FileSet</tt>.
 *
 * @author <a href="mailto:sbailliez@imediation.com">Stephane Bailliez</a>
 */
public class ReportFilters {

    /** user defined filters */
    protected Vector filters = new Vector();

    /** cached matcher for each filter */
    protected Vector matchers = null;

    public ReportFilters() {
    }

    public void addInclude(Include incl) {
        filters.addElement(incl);
    }

    public void addExclude(Exclude excl) {
        filters.addElement(excl);
    }

    public int size() {
        return filters.size();
    }

    /**
     * Check whether a given &lt;classname&gt;&lt;method&gt;() is accepted by the list
     * of filters or not.
     * @param methodname the full method name in the format &lt;classname&gt;&lt;method&gt;()
     */
    public boolean accept(String methodname) {
        // I'm deferring matcher instantiations at runtime to avoid computing
        // the filters at parsing time
        if (matchers == null) {
            createMatchers();
        }
        boolean result = false;
        // assert filters.size() == matchers.size()
        final int size = filters.size();
        for (int i = 0; i < size; i++) {
            FilterElement filter = (FilterElement) filters.elementAt(i);
            RegexpMatcher matcher = (RegexpMatcher) matchers.elementAt(i);
            if (filter instanceof Include) {
                result = result || matcher.matches(methodname);
            } else if (filter instanceof Exclude) {
                result = result && !matcher.matches(methodname);
            } else {
                //not possible
                throw new IllegalArgumentException("Invalid filter element: "
                    + filter.getClass().getName());
            }
        }
        return result;
    }

    /** should be called only once to cache matchers */
    protected void createMatchers() {
        RegexpMatcherFactory factory = new RegexpMatcherFactory();
        final int size = filters.size();
        matchers = new Vector();
        for (int i = 0; i < size; i++) {
            FilterElement filter = (FilterElement) filters.elementAt(i);
            RegexpMatcher matcher = factory.newRegexpMatcher();
            String pattern = filter.getAsPattern();
            matcher.setPattern(pattern);
            matchers.addElement(matcher);
        }
    }


    /** default abstract filter element class */
    public abstract static class FilterElement {
        protected String clazz = "*"; // default is all classes
        protected String method = "*"; // default is all methods

        public void setClass(String value) {
            clazz = value;
        }

        public void setMethod(String value) {
            method = value;
        }

        public String getAsPattern() {
            StringBuffer buf = new StringBuffer(toString());
            StringUtil.replace(buf, ".", "\\.");
            StringUtil.replace(buf, "*", ".*");
            StringUtil.replace(buf, "(", "\\(");
            StringUtil.replace(buf, ")", "\\)");
            return buf.toString();
        }

        public String toString() {
            return clazz + "." + method + "()";
        }
    }

    /** concrete include class */
    public static class Include extends FilterElement {
    }

    /** concrete exclude class */
    public static class Exclude extends FilterElement {
    }
}

