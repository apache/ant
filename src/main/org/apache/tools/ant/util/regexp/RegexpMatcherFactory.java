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
package org.apache.tools.ant.util.regexp;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.MagicNames;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.util.ClasspathUtils;

/**
 * Simple Factory Class that produces an implementation of RegexpMatcher based on the system
 * property <code>ant.regexp.regexpimpl</code> and the classes available.
 *
 * <p>
 * In a more general framework this class would be abstract and have a static newInstance method.
 * </p>
 *
 */
public class RegexpMatcherFactory {

    /***
     * Create a new regular expression instance.
     * @return the matcher
     * @throws BuildException on error
     */
    public RegexpMatcher newRegexpMatcher() throws BuildException {
        return newRegexpMatcher(null);
    }

    /***
     * Create a new regular expression instance.
     *
     * @param p Project whose ant.regexp.regexpimpl property will be used.
     * @return the matcher
     * @throws BuildException on error
     */
    public RegexpMatcher newRegexpMatcher(Project p) throws BuildException {
        String systemDefault;
        if (p == null) {
            systemDefault = System.getProperty(MagicNames.REGEXP_IMPL);
        } else {
            systemDefault = p.getProperty(MagicNames.REGEXP_IMPL);
        }

        if (systemDefault != null) {
            return createInstance(systemDefault);
            // TODO     should we silently catch possible exceptions and try to
            //         load a different implementation?
        }

        return new Jdk14RegexpMatcher();
    }

    /**
     * Create an instance of a matcher from a classname.
     *
     * @param className a <code>String</code> value
     * @return a <code>RegexpMatcher</code> value
     * @exception BuildException if an error occurs
     */
    protected RegexpMatcher createInstance(String className) throws BuildException {
        return ClasspathUtils.newInstance(className,
            RegexpMatcherFactory.class.getClassLoader(), RegexpMatcher.class);
    }

    /**
     * Test if a particular class is available to be used.
     *
     * @param className a <code>String</code> value
     * @exception BuildException if an error occurs
     */
    protected void testAvailability(String className) throws BuildException {
        try {
            Class.forName(className);
        } catch (Throwable t) {
            throw new BuildException(t);
        }
    }

    /**
     * Checks if a RegExp-Matcher is available.
     * @param project  The project to check for (may be <code>null</code>)
     * @return <code>true</code> if available otherwise <code>false</code>
     */
    public static boolean regexpMatcherPresent(Project project) {
        try {
            // The factory throws a BuildException if no usable matcher
            // cannot be instantiated. We don't need the matcher itself here.
            new RegexpMatcherFactory().newRegexpMatcher(project);
            return true;
        } catch (Throwable ex) {
            return false;
        }
    }
}
