/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002 The Apache Software Foundation.  All rights
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
 * 4. The names "The Jakarta Project", "Ant", and "Apache Software
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
package org.apache.tools.ant.taskdefs.optional.rjunit;

import java.util.Enumeration;

import junit.runner.TestCollector;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectComponent;

/**
 * Base test implementation that implements the if/unless logic.
 *
 */
public abstract class BaseTestElement
        extends ProjectComponent implements TestCollector {

    /** run the test only if this property is present */
    private String ifProperty;

    /** run the test unless this property is present */
    private String unlessProperty;

    public final Enumeration collectTests() {
        if (shouldRun()) {
            return getTests();
        }
        return ArrayEnumeration.NULL_ENUMERATION;
    }

    public final void setIf(final String value) {
        ifProperty = value;
    }

    public final void setUnless(final String value) {
        unlessProperty = value;
    }

    /**
     * Implementation of the test collection process
     * @return the enumeration of fully qualified classname representing
     * a JUnit Test.
     */
    protected abstract Enumeration getTests();

    /**
     * check whether this test should be run or not.
     * @return whether or not the test should run based on
     * the presence of <tt>if</tt> and <tt>unless</tt> properties.
     * @see #setIf(String)
     * @see #setUnless(String)
     */
    protected boolean shouldRun() {
        final Project project = getProject();
        if ( ifProperty != null &&
                project.getProperty(ifProperty) == null ){
            return false;
        }
        if (unlessProperty != null &&
                project.getProperty(unlessProperty) != null) {
            return false;
        }
        return true;
    }
}
