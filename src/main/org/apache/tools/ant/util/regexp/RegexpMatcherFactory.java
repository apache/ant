/*
 * Copyright  2000-2004 The Apache Software Foundation
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

package org.apache.tools.ant.util.regexp;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

/**
 * Simple Factory Class that produces an implementation of
 * RegexpMatcher based on the system property
 * <code>ant.regexp.matcherimpl</code> and the classes
 * available.
 *
 * <p>In a more general framework this class would be abstract and
 * have a static newInstance method.</p>
 *
 */
public class RegexpMatcherFactory {

    public RegexpMatcherFactory() {
    }

    /***
     * Create a new regular expression instance.
     */
    public RegexpMatcher newRegexpMatcher() throws BuildException {
        return newRegexpMatcher(null);
    }

    /***
     * Create a new regular expression instance.
     *
     * @param p Project whose ant.regexp.regexpimpl property will be used.
     */
    public RegexpMatcher newRegexpMatcher(Project p)
        throws BuildException {
        String systemDefault = null;
        if (p == null) {
            systemDefault = System.getProperty("ant.regexp.regexpimpl");
        } else {
            systemDefault = p.getProperty("ant.regexp.regexpimpl");
        }

        if (systemDefault != null) {
            return createInstance(systemDefault);
            // XXX     should we silently catch possible exceptions and try to
            //         load a different implementation?
        }

        try {
            testAvailability("java.util.regex.Matcher");
            return createInstance("org.apache.tools.ant.util.regexp.Jdk14RegexpMatcher");
        } catch (BuildException be) {
            // ignore
        }

        try {
            testAvailability("org.apache.oro.text.regex.Pattern");
            return createInstance("org.apache.tools.ant.util.regexp.JakartaOroMatcher");
        } catch (BuildException be) {
            // ignore
        }

        try {
            testAvailability("org.apache.regexp.RE");
            return createInstance("org.apache.tools.ant.util.regexp.JakartaRegexpMatcher");
        } catch (BuildException be) {
            // ignore
        }

        throw new BuildException("No supported regular expression matcher found");
   }

    protected RegexpMatcher createInstance(String className)
        throws BuildException {
        try {
            Class implClass = Class.forName(className);
            return (RegexpMatcher) implClass.newInstance();
        } catch (Throwable t) {
            throw new BuildException(t);
        }
    }

    protected void testAvailability(String className) throws BuildException {
        try {
            Class.forName(className);
        } catch (Throwable t) {
            throw new BuildException(t);
        }
    }
}
