/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2002 The Apache Software Foundation.  All rights
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

package org.apache.tools.ant.taskdefs.optional.junit;

import java.lang.reflect.Method;
import junit.framework.Test;
import junit.framework.TestCase;

/**
 * Work around for some changes to the public JUnit API between
 * different JUnit releases.
 *
 * @author Stefan Bodewig
 * @version $Revision$
 */
public class JUnitVersionHelper {

    private static Method testCaseName = null;
    static {
        try {
            testCaseName = TestCase.class.getMethod("getName", new Class[0]);
        } catch (NoSuchMethodException e) {
            // pre JUnit 3.7
            try {
                testCaseName = TestCase.class.getMethod("name", new Class[0]);
            } catch (NoSuchMethodException e2) {}
        }
    }

    /**
     * JUnit 3.7 introduces TestCase.getName() and subsequent versions
     * of JUnit remove the old name() method.  This method provides
     * access to the name of a TestCase via reflection that is
     * supposed to work with version before and after JUnit 3.7.
     *
     * <p>since Ant 1.5.1 this method will invoke &quot;<code>public
     * String getName()</code>&quot; on any implementation of Test if
     * it exists.</p>
     */
    public static String getTestCaseName(Test t) {
        if (t instanceof TestCase && testCaseName != null) {
            try {
                return (String) testCaseName.invoke(t, new Object[0]);
            } catch (Throwable e) {}
        } else {
            try {
                Method getNameMethod = null;
                try {
                    getNameMethod = 
                        t.getClass().getMethod("getName", new Class [0]);
                } catch (NoSuchMethodException e) {
                    getNameMethod = t.getClass().getMethod("name", 
                                                           new Class [0]);
                }
                if (getNameMethod != null &&
                    getNameMethod.getReturnType() == String.class) {
                    return (String) getNameMethod.invoke(t, new Object[0]);
                }
            } catch (Throwable e) {}
        }
        return "unknown";
    }

}
