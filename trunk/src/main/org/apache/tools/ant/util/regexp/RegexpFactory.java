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
package org.apache.tools.ant.util.regexp;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.util.JavaEnvUtils;

/***
 * Regular expression factory, which will create Regexp objects.  The
 * actual implementation class depends on the System or Ant Property:
 * <code>ant.regexp.regexpimpl</code>.
 *
 */
public class RegexpFactory extends RegexpMatcherFactory {

    /** Constructor for RegexpFactory */
    public RegexpFactory() {
    }

    /***
     * Create a new regular expression matcher instance.
     * @return the matcher instance
     * @throws BuildException on error
     */
    public Regexp newRegexp() throws BuildException {
        return (Regexp) newRegexp(null);
    }

    /***
     * Create a new regular expression matcher instance.
     *
     * @param p Project whose ant.regexp.regexpimpl property will be used.
     * @return the matcher instance
     * @throws BuildException on error
     */
    public Regexp newRegexp(Project p) throws BuildException {
        String systemDefault = null;
        if (p == null) {
            systemDefault = System.getProperty("ant.regexp.regexpimpl");
        } else {
            systemDefault = p.getProperty("ant.regexp.regexpimpl");
        }

        if (systemDefault != null) {
            return createRegexpInstance(systemDefault);
            // XXX     should we silently catch possible exceptions and try to
            //         load a different implementation?
        }

        Throwable cause = null;

        try {
            testAvailability("java.util.regex.Matcher");
            return createRegexpInstance("org.apache.tools.ant.util.regexp.Jdk14RegexpRegexp");
        } catch (BuildException be) {
            cause = orCause(cause, be, JavaEnvUtils.getJavaVersionNumber() < 14);
        }

        try {
            testAvailability("org.apache.oro.text.regex.Pattern");
            return createRegexpInstance("org.apache.tools.ant.util.regexp.JakartaOroRegexp");
        } catch (BuildException be) {
            cause = orCause(cause, be, true);
        }

        try {
            testAvailability("org.apache.regexp.RE");
            return createRegexpInstance("org.apache.tools.ant.util.regexp.JakartaRegexpRegexp");
        } catch (BuildException be) {
            cause = orCause(cause, be, true);
        }

        throw new BuildException(
            "No supported regular expression matcher found"
            + (cause != null ? ": " + cause : ""), cause);
    }

    /**
     * Wrapper over RegexpMatcherFactory.createInstance that ensures that
     * we are dealing with a Regexp implementation.
     * @param classname the name of the class to use.
     * @return the instance.
     * @throws BuildException if there is a problem.
     * @since 1.3
     *
     * @see RegexpMatcherFactory#createInstance(String)
     */
    protected Regexp createRegexpInstance(String classname)
        throws BuildException {

        RegexpMatcher m = createInstance(classname);
        if (m instanceof Regexp) {
            return (Regexp) m;
        } else {
            throw new BuildException(classname + " doesn't implement the Regexp interface");
        }
    }

}
