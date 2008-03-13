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

import java.lang.reflect.Constructor;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.optional.rjunit.formatter.FilterFormatter;
import org.apache.tools.ant.taskdefs.optional.rjunit.formatter.FilterStackFormatter;
import org.apache.tools.ant.taskdefs.optional.rjunit.formatter.Formatter;
import org.apache.tools.ant.types.EnumeratedAttribute;

/**
 * A filter element that can be used inside a ResultFormatterElement to denote
 * a filtering. Note that the filtering order correspond to the element
 * order. The first element being the top filter, the last element
 * being the bottom filter.
 *
 * <pre>
 * <!ELEMENT filter>
 * <!ATTLIST filter type (stack) required>
 * <!ATTLIST filter classname CDATA required>
 * </pre>
 *
 */
public class FilterElement {

    /** filter classname, is should inherit from FilterFormatter */
    private String classname;

    /**
     * Called by introspection on <tt>type</tt> attribute.
     * @see FilterAttribute
     */
    public void setType(FilterAttribute fa) {
        setClassName(fa.getClassName());
    }

    /**
     * Called by introspection on <tt>classname</tt> attribute.
     * It must inherit from <tt>FilterFormatter</tt>
     * @see FilterFormatter
     */
    public void setClassName(String name) {
        classname = name;
    }

    /**
     * Wrap this filter around a given formatter.
     * @throws BuildException if any error happens when creating this filter.
     */
    public Formatter createFilterFormatter(Formatter f) throws BuildException {
        try {
            Class clazz = Class.forName(classname);
            if (!FilterFormatter.class.isAssignableFrom(clazz)) {
                throw new BuildException(clazz + " must be a FilterFormatter.");
            }
            Constructor ctor = clazz.getConstructor(new Class[]{Formatter.class});
            return (Formatter) ctor.newInstance(new Object[]{f});
        } catch (BuildException e) {
            throw e;
        } catch (Exception e) {
            throw new BuildException(e);
        }
    }

    /** a predefined set of filters w/ their class mapping */
    public static class FilterAttribute extends EnumeratedAttribute {
        /** the predefined alias for filters */
        private final static String[] VALUES = {"stack"};

        /** the class corresponding to the alias (in the same order) */
        private final static String[] CLASSNAMES = {FilterStackFormatter.class.getName()};

        public String[] getValues() {
            return VALUES;
        }

        /** get the classname matching the alias */
        public String getClassName() {
            return CLASSNAMES[getIndex()];
        }
    }

}
