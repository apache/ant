/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2003 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "Ant" and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */
package org.apache.tools.ant.util.regexp;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

/***
 * Regular expression factory, which will create Regexp objects.  The
 * actual implementation class depends on the System or Ant Property:
 * <code>ant.regexp.regexpimpl</code>.
 *
 * @author Matthew Inger <a href="mailto:mattinger@mindless.com">mattinger@mindless.com</a>
 * @version $Revision$
 */
public class RegexpFactory extends RegexpMatcherFactory {
    public RegexpFactory() {
    }

    /***
     * Create a new regular expression matcher instance.
     */
    public Regexp newRegexp() throws BuildException {
        return (Regexp) newRegexp(null);
    }

    /***
     * Create a new regular expression matcher instance.
     *
     * @param p Project whose ant.regexp.regexpimpl property will be used.
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

        try {
            testAvailability("java.util.regex.Matcher");
            return createRegexpInstance("org.apache.tools.ant.util.regexp.Jdk14RegexpRegexp");
        } catch (BuildException be) {
            // ignore
        }

        try {
            testAvailability("org.apache.oro.text.regex.Pattern");
            return createRegexpInstance("org.apache.tools.ant.util.regexp.JakartaOroRegexp");
        } catch (BuildException be) {
            // ignore
        }

        try {
            testAvailability("org.apache.regexp.RE");
            return createRegexpInstance("org.apache.tools.ant.util.regexp.JakartaRegexpRegexp");
        } catch (BuildException be) {
            // ignore
        }

        throw new BuildException("No supported regular expression matcher found");
    }

    /**
     * Wrapper over RegexpMatcherFactory.createInstance that ensures that
     * we are dealing with a Regexp implementation.
     *
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
