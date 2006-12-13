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

package org.apache.tools.ant.taskdefs.optional.junit;

import java.lang.reflect.Method;
import junit.framework.Test;
import junit.framework.TestCase;

/**
 * Work around for some changes to the public JUnit API between
 * different JUnit releases.
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
            } catch (NoSuchMethodException e2) {
                // ignore
            }
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
     *
     * <p>Since Ant 1.7 also checks for JUnit4TestCaseFacade explicitly.
     * This is used by junit.framework.JUnit4TestAdapter.</p>
     * @param t the test.
     * @return the name of the test.
     */
    public static String getTestCaseName(Test t) {
        if (t != null && t.getClass().getName().equals("junit.framework.JUnit4TestCaseFacade")) {
            // Self-describing as of JUnit 4 (#38811). But trim "(ClassName)".
            String name = t.toString();
            if (name.endsWith(")")) {
                int paren = name.lastIndexOf('(');
                return name.substring(0, paren);
            } else {
                return name;
            }
        }
        if (t instanceof TestCase && testCaseName != null) {
            try {
                return (String) testCaseName.invoke(t, new Object[0]);
            } catch (Throwable e) {
                // ignore
            }
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
                if (getNameMethod != null
                    && getNameMethod.getReturnType() == String.class) {
                    return (String) getNameMethod.invoke(t, new Object[0]);
                }
            } catch (Throwable e) {
                // ignore
            }
        }
        return "unknown";
    }

    /**
     * Tries to find the name of the class which a test represents
     * across JUnit 3 and 4.
     */
    static String getTestCaseClassName(Test test) {
        String className = test.getClass().getName();
        if (test instanceof JUnitTaskMirrorImpl.VmExitErrorTest) {
            className = ((JUnitTaskMirrorImpl.VmExitErrorTest) test).getClassName();
        } else
        if (className.equals("junit.framework.JUnit4TestCaseFacade")) {
            // JUnit 4 wraps solo tests this way. We can extract
            // the original test name with a little hack.
            String name = test.toString();
            int paren = name.lastIndexOf('(');
            if (paren != -1 && name.endsWith(")")) {
                className = name.substring(paren + 1, name.length() - 1);
            }
        }
        return className;
    }

}
