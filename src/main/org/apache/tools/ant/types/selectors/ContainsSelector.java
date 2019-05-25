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

package org.apache.tools.ant.types.selectors;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Parameter;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.resources.FileResource;
import org.apache.tools.ant.types.resources.selectors.ResourceSelector;

/**
 * Selector that filters files/resources based on whether they contain a
 * particular string.
 *
 * @since 1.5
 */
public class ContainsSelector extends BaseExtendSelector implements ResourceSelector {

    /** Key to used for parameterized custom selector */
    public static final String EXPRESSION_KEY = "expression";
    /** Used for parameterized custom selector */
    public static final String CONTAINS_KEY = "text";
    /** Used for parameterized custom selector */
    public static final String CASE_KEY = "casesensitive";
    /** Used for parameterized custom selector */
    public static final String WHITESPACE_KEY = "ignorewhitespace";

    private String contains = null;
    private boolean casesensitive = true;
    private boolean ignorewhitespace = false;
    private String encoding = null;

    /**
     * @return a string describing this object
     */
    public String toString() {
        return String.format("{containsselector text: \"%s\" casesensitive: %s ignorewhitespace: %s}",
                contains, casesensitive, ignorewhitespace);
    }

    /**
     * The string to search for within a file.
     *
     * @param contains the string that a file must contain to be selected.
     */
    public void setText(String contains) {
        this.contains = contains;
    }

    /**
     * The encoding of the resources processed
     * @since Ant 1.9.0
     * @param encoding encoding of the resources processed
     */
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    /**
     * Whether to ignore case in the string being searched.
     *
     * @param casesensitive whether to pay attention to case sensitivity
     */
    public void setCasesensitive(boolean casesensitive) {
        this.casesensitive = casesensitive;
    }

    /**
     * Whether to ignore whitespace in the string being searched.
     *
     * @param ignorewhitespace whether to ignore any whitespace
     *        (spaces, tabs, etc.) in the searchstring
     */
    public void setIgnorewhitespace(boolean ignorewhitespace) {
        this.ignorewhitespace = ignorewhitespace;
    }

    /**
     * When using this as a custom selector, this method will be called.
     * It translates each parameter into the appropriate setXXX() call.
     *
     * @param parameters the complete set of parameters for this selector
     */
    public void setParameters(Parameter... parameters) {
        super.setParameters(parameters);
        if (parameters != null) {
            for (Parameter parameter : parameters) {
                String paramname = parameter.getName();
                if (CONTAINS_KEY.equalsIgnoreCase(paramname)) {
                    setText(parameter.getValue());
                } else if (CASE_KEY.equalsIgnoreCase(paramname)) {
                    setCasesensitive(Project.toBoolean(
                            parameter.getValue()));
                } else if (WHITESPACE_KEY.equalsIgnoreCase(paramname)) {
                    setIgnorewhitespace(Project.toBoolean(
                            parameter.getValue()));
                } else {
                    setError("Invalid parameter " + paramname);
                }
            }
        }
    }

    /**
     * Checks to make sure all settings are kosher. In this case, it
     * means that the pattern attribute has been set.
     *
     */
    public void verifySettings() {
        if (contains == null) {
            setError("The text attribute is required");
        }
    }

    /**
     * The heart of the matter. This is where the selector gets to decide
     * on the inclusion of a file in a particular fileset.
     *
     * @param basedir the base directory the scan is being done from
     * @param filename is the name of the file to check
     * @param file is a java.io.File object the selector can use
     * @return whether the file should be selected or not
     */
    public boolean isSelected(File basedir, String filename, File file) {
        return isSelected(new FileResource(file));
    }

    /**
     * The heart of the matter. This is where the selector gets to decide
     * on the inclusion of a Resource.
     *
     * @param r the Resource to check.
     * @return whether the Resource is selected.
     */
    public boolean isSelected(Resource r) {
        // throw BuildException on error
        validate();

        if (r.isDirectory() || contains.isEmpty()) {
            return true;
        }

        String userstr = contains;
        if (!casesensitive) {
            userstr = contains.toLowerCase();
        }
        if (ignorewhitespace) {
            userstr = SelectorUtils.removeWhitespace(userstr);
        }
        try (BufferedReader in = new BufferedReader(
            new InputStreamReader(r.getInputStream(), encoding == null
                ? Charset.defaultCharset() : Charset.forName(encoding)))) {
            try {
                String teststr = in.readLine();
                while (teststr != null) {
                    if (!casesensitive) {
                        teststr = teststr.toLowerCase();
                    }
                    if (ignorewhitespace) {
                        teststr = SelectorUtils.removeWhitespace(teststr);
                    }
                    if (teststr.contains(userstr)) {
                        return true;
                    }
                    teststr = in.readLine();
                }
                return false;
            } catch (IOException ioe) {
                throw new BuildException("Could not read " + r.toLongString());
            }
        } catch (IOException e) {
            throw new BuildException(
                "Could not get InputStream from " + r.toLongString(), e);
        }
    }
}
