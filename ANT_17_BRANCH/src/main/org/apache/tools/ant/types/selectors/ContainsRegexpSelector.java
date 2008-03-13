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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.Parameter;
import org.apache.tools.ant.types.RegularExpression;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.resources.FileResource;
import org.apache.tools.ant.types.resources.selectors.ResourceSelector;
import org.apache.tools.ant.util.regexp.Regexp;

/**
 * Selector that filters files based on a regular expression.
 *
 * @since Ant 1.6
 */
public class ContainsRegexpSelector extends BaseExtendSelector
        implements ResourceSelector {

    private String userProvidedExpression = null;
    private RegularExpression myRegExp = null;
    private Regexp myExpression = null;
    /** Key to used for parameterized custom selector */
    public static final String EXPRESSION_KEY = "expression";

    /**
     * Creates a new <code>ContainsRegexpSelector</code> instance.
     */
    public ContainsRegexpSelector() {
    }

    /**
     * @return a string describing this object
     */
    public String toString() {
        StringBuffer buf = new StringBuffer(
                "{containsregexpselector expression: ");
        buf.append(userProvidedExpression);
        buf.append("}");
        return buf.toString();
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
                if (EXPRESSION_KEY.equalsIgnoreCase(paramname)) {
                    setExpression(parameters[i].getValue());
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
        String teststr = null;
        BufferedReader in = null;

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

        try {
            in = new BufferedReader(new InputStreamReader(r.getInputStream()));
        } catch (Exception e) {
            throw new BuildException("Could not get InputStream from "
                    + r.toLongString(), e);
        }
        try {
            teststr = in.readLine();

            while (teststr != null) {

                if (myExpression.matches(teststr)) {
                    return true;
                }
                teststr = in.readLine();
            }

            return false;
        } catch (IOException ioe) {
            throw new BuildException("Could not read " + r.toLongString());
        } finally {
            try {
                in.close();
            } catch (Exception e) {
                throw new BuildException("Could not close "
                                         + r.toLongString());
            }
        }
    }
}

