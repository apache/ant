/* 
 * Copyright  2001-2004 Apache Software Foundation
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
package org.apache.tools.ant.taskdefs.optional.sitraka;

import java.io.IOException;
import java.io.InputStream;

import junit.framework.TestCase;
import org.apache.tools.ant.util.JavaEnvUtils;
import org.apache.tools.ant.taskdefs.optional.sitraka.bytecode.ClassFile;
import org.apache.tools.ant.taskdefs.optional.sitraka.bytecode.MethodInfo;

/**
 * Nothing special about this testcase...
 *
 * @author <a href="mailto:sbailliez@apache.org">Stephane Bailliez</a>
 * @author <a href="mailto:martijn@kruithof.xs4all.nl">Martijn Kruithof</a>
 */
public class ClassFileTest extends TestCase {
    public ClassFileTest(String s) {
        super(s);
    }

    public void testVector() throws IOException {
        String classname = ClassTest.class.getName().replace('.', '/') + ".class";
        InputStream is = getClass().getClassLoader().getResourceAsStream(classname);
		assertNotNull("Unable to find resource " + classname + "in caller classloader");
        ClassFile clazzfile = new ClassFile(is);
        assertEquals("ClassTest", clazzfile.getName());
        assertEquals("ClassFileTest.java", clazzfile.getSourceFile());
        MethodInfo[] methods = clazzfile.getMethods();
        assertEquals(3, methods.length);
        assertHasMethod("void <init>()", 1, methods);
        assertHasMethod("void testTwoLines()", 2, methods);
        assertHasMethod("void testOneLine()", 3, methods);
    }

    protected void assertHasMethod(String methodsig, int line, MethodInfo[] methods) {
        boolean found = false;
        for (int i = 0; i < methods.length; i++) {
            MethodInfo method = methods[i];
            if (methodsig.equals(method.getFullSignature())) {

                assertTrue(methodsig, method.getNumberOfLines() >= line);
                return;
            }
        }
        fail("Could not find method " + methodsig);
    }
}

class ClassTest {

    // 2 lines
    public ClassTest() {
    }

    // 2 lines
    public void testTwoLines() {
        System.out.println("This is 1 line");
    }

    // 1 line
    public void testOneLine() {
        try {
            throw new Exception();
        } catch (Exception e) {
        }
    }
}
