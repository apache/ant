/*
 * Copyright  2001-2002,2004 The Apache Software Foundation
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

package org.apache.tools.ant.taskdefs.optional.junit;

import java.lang.reflect.Method;
import junit.framework.Test;
import junit.framework.TestCase;

/**
 * Work around for some changes to the public JUnit API between
 * different JUnit releases.
 *
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
     */
    public static String getTestCaseName(Test t) {
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

}
