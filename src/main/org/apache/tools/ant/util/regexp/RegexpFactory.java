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

/***
 * Regular expression factory, which will create Regexp objects.  The
 * actual implementation class depends on the System or Ant Property:
 * <code>ant.regexp.regexpimpl</code>.
 *
 */
public class RegexpFactory extends RegexpMatcherFactory {

    /***
     * Create a new regular expression matcher instance.
     * @return the matcher instance
     * @throws BuildException on error
     */
    public Regexp newRegexp() throws BuildException {
        return newRegexp(null);
    }

    /***
     * Create a new regular expression matcher instance.
     *
     * @param p Project whose ant.regexp.regexpimpl property will be used.
     * @return the matcher instance
     * @throws BuildException on error
     */
    public Regexp newRegexp(Project p) throws BuildException {
        String systemDefault;
        if (p == null) {
            systemDefault = System.getProperty(MagicNames.REGEXP_IMPL);
        } else {
            systemDefault = p.getProperty(MagicNames.REGEXP_IMPL);
        }

        if (systemDefault != null) {
            return createRegexpInstance(systemDefault);
            // TODO     should we silently catch possible exceptions and try to
            //         load a different implementation?
        }

        return new Jdk14RegexpRegexp();
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
    protected Regexp createRegexpInstance(String classname) throws BuildException {
        return ClasspathUtils.newInstance(classname,
            RegexpFactory.class.getClassLoader(), Regexp.class);
    }

}
