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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Parameter;
import org.apache.tools.ant.types.RegularExpression;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.resources.FileResource;
import org.apache.tools.ant.types.resources.selectors.ResourceSelector;
import org.apache.tools.ant.util.regexp.Regexp;
import org.apache.tools.ant.util.regexp.RegexpUtil;

/**
 * Selector that filters files based on a regular expression.
 *
 * @since Ant 1.6
 */
public class ContainsRegexpSelector extends BaseExtendSelector
        implements ResourceSelector {

    /** Key to used for parameterized custom selector */
    public static final String EXPRESSION_KEY = "expression";
    /** Parameter name for the casesensitive attribute. */
    private static final String CS_KEY = "casesensitive";
    /** Parameter name for the multiline attribute. */
    private static final String ML_KEY = "multiline";
    /** Parameter name for the singleline attribute. */
    private static final String SL_KEY = "singleline";

    private String userProvidedExpression = null;
    private RegularExpression myRegExp = null;
    private Regexp myExpression = null;
    private boolean caseSensitive = true;
    private boolean multiLine = false;
    private boolean singleLine = false;

    /**
     * @return a string describing this object
     */
    public String toString() {
        return String.format("{containsregexpselector expression: %s}",
            userProvidedExpression);
    }

    /**
     * The regular expression used to search the file.
     *
     * @param theexpression this must match a line in the file to be selected.
     */
    public void setExpression(String theexpression) {
        this.userProvidedExpression = theexpression;
    }

    /**
     * Whether to ignore case or not.
     * @param b if false, ignore case.
     * @since Ant 1.8.2
     */
    public void setCaseSensitive(boolean b) {
        caseSensitive = b;
    }

    /**
     * Whether to match should be multiline.
     * @param b the value to set.
     * @since Ant 1.8.2
     */
    public void setMultiLine(boolean b) {
        multiLine = b;
    }

    /**
     * Whether to treat input as singleline ('.' matches newline).
     * Corresponds to java.util.regex.Pattern.DOTALL.
     * @param b the value to set.
     * @since Ant 1.8.2
     */
    public void setSingleLine(boolean b) {
        singleLine = b;
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
                if (EXPRESSION_KEY.equalsIgnoreCase(paramname)) {
                    setExpression(parameter.getValue());
                } else if (CS_KEY.equalsIgnoreCase(paramname)) {
                    setCaseSensitive(Project.toBoolean(parameter.getValue()));
                } else if (ML_KEY.equalsIgnoreCase(paramname)) {
                    setMultiLine(Project.toBoolean(parameter.getValue()));
                } else if (SL_KEY.equalsIgnoreCase(paramname)) {
                    setSingleLine(Project.toBoolean(parameter.getValue()));
                } else {
                    setError("Invalid parameter " + paramname);
                }
            }
        }
    }

    /**
     * Checks that an expression was specified.
     *
     */
    public void verifySettings() {
        if (userProvidedExpression == null) {
            setError("The expression attribute is required");
        }
    }

    /**
     * Tests a regular expression against each line of text in the file.
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
     * Tests a regular expression against each line of text in a Resource.
     *
     * @param r the Resource to check.
     * @return whether the Resource is selected or not
     */
    public boolean isSelected(Resource r) {
        // throw BuildException on error
        validate();

        if (r.isDirectory()) {
            return true;
        }

        if (myRegExp == null) {
            myRegExp = new RegularExpression();
            myRegExp.setPattern(userProvidedExpression);
            myExpression = myRegExp.getRegexp(getProject());
        }

        try (BufferedReader in =
            new BufferedReader(new InputStreamReader(r.getInputStream()))) {
            try {
                String teststr = in.readLine();

                while (teststr != null) {
                    if (myExpression.matches(teststr, RegexpUtil
                        .asOptions(caseSensitive, multiLine, singleLine))) {
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
