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

package org.apache.tools.ant.types.selectors;

import java.io.File;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Parameter;

/**
 * Selector that filters files based on the filename.
 *
 * @since 1.5
 */
public class FilenameSelector extends BaseExtendSelector {

    private String pattern = null;
    private boolean casesensitive = true;

    private boolean negated = false;
    /** Used for parameterized custom selector */
    public static final String NAME_KEY = "name";
    /** Used for parameterized custom selector */
    public static final String CASE_KEY = "casesensitive";
    /** Used for parameterized custom selector */
    public static final String NEGATE_KEY = "negate";

    /**
     * Creates a new <code>FilenameSelector</code> instance.
     *
     */
    public FilenameSelector() {
    }

    /**
     * @return a string describing this object
     */
    public String toString() {
        StringBuffer buf = new StringBuffer("{filenameselector name: ");
        buf.append(pattern);
        buf.append(" negate: ");
        if (negated) {
            buf.append("true");
        } else {
            buf.append("false");
        }
        buf.append(" casesensitive: ");
        if (casesensitive) {
            buf.append("true");
        } else {
            buf.append("false");
        }
        buf.append("}");
        return buf.toString();
    }

    /**
     * The name of the file, or the pattern for the name, that
     * should be used for selection.
     *
     * @param pattern the file pattern that any filename must match
     *                against in order to be selected.
     */
    public void setName(String pattern) {
        pattern = pattern.replace('/', File.separatorChar).replace('\\',
                File.separatorChar);
        if (pattern.endsWith(File.separator)) {
            pattern += "**";
        }
        this.pattern = pattern;
    }

    /**
     * Whether to ignore case when checking filenames.
     *
     * @param casesensitive whether to pay attention to case sensitivity
     */
    public void setCasesensitive(boolean casesensitive) {
        this.casesensitive = casesensitive;
    }

    /**
     * You can optionally reverse the selection of this selector,
     * thereby emulating an &lt;exclude&gt; tag, by setting the attribute
     * negate to true. This is identical to surrounding the selector
     * with &lt;not&gt;&lt;/not&gt;.
     *
     * @param negated whether to negate this selection
     */
    public void setNegate(boolean negated) {
        this.negated = negated;
    }

    /**
     * When using this as a custom selector, this method will be called.
     * It translates each parameter into the appropriate setXXX() call.
     *
     * @param parameters the complete set of parameters for this selector
     */
    public void setParameters(Parameter[] parameters) {
        super.setParameters(parameters);
        if (parameters != null) {
            for (int i = 0; i < parameters.length; i++) {
                String paramname = parameters[i].getName();
                if (NAME_KEY.equalsIgnoreCase(paramname)) {
                    setName(parameters[i].getValue());
                } else if (CASE_KEY.equalsIgnoreCase(paramname)) {
                    setCasesensitive(Project.toBoolean(
                            parameters[i].getValue()));
                } else if (NEGATE_KEY.equalsIgnoreCase(paramname)) {
                    setNegate(Project.toBoolean(parameters[i].getValue()));
                } else {
                    setError("Invalid parameter " + paramname);
                }
            }
        }
    }

    /**
     * Checks to make sure all settings are kosher. In this case, it
     * means that the name attribute has been set.
     *
     */
    public void verifySettings() {
        if (pattern == null) {
            setError("The name attribute is required");
        }
    }

    /**
     * The heart of the matter. This is where the selector gets to decide
     * on the inclusion of a file in a particular fileset. Most of the work
     * for this selector is offloaded into SelectorUtils, a static class
     * that provides the same services for both FilenameSelector and
     * DirectoryScanner.
     *
     * @param basedir the base directory the scan is being done from
     * @param filename is the name of the file to check
     * @param file is a java.io.File object the selector can use
     * @return whether the file should be selected or not
     */
    public boolean isSelected(File basedir, String filename, File file) {
        validate();

        return (SelectorUtils.matchPath(pattern, filename,
                casesensitive) == !(negated));
    }

}

