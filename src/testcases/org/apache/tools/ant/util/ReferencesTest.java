/*
 * Copyright  2002,2004 Apache Software Foundation
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

package org.apache.tools.ant.util;

import junit.framework.TestCase;


/**
 * this test just makes sure that we really do create weak references
 * on java1.2+, and is written to not import the WeakishReference12 class
 * because then we'd have to make the class conditional on java1.2+.
 */
public class ReferencesTest extends TestCase{

    public ReferencesTest(String s) {
        super(s);
    }

    /**
     * look at the type of a reference we have created
     */
    public static void testReferencesAreSoft() {
        boolean isJava11=JavaEnvUtils.isJavaVersion(JavaEnvUtils.JAVA_1_1);
        WeakishReference reference = WeakishReference.createReference(new Object());
        if ((reference.getClass().getName().indexOf("HardReference") > 0 )
            && !isJava11) {
            fail("We should be creating soft references in this version of Java");
        }
    }

}
