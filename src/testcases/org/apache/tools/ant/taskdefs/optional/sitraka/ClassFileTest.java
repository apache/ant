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
        assertHasMethod("void <init>()", 2, methods);
        assertHasMethod("void testTwoLines()", 2, methods);
        assertHasMethod("void testOneLine()", 
                        // in JDK 1.4 we get four lines
                        3 + 
                        (JavaEnvUtils.isJavaVersion(JavaEnvUtils.JAVA_1_1)
                         || JavaEnvUtils.isJavaVersion(JavaEnvUtils.JAVA_1_2)
                         || JavaEnvUtils.isJavaVersion(JavaEnvUtils.JAVA_1_3)
                         ? 0 : 1),
                        methods);
    }

    protected void assertHasMethod(String methodsig, int line, MethodInfo[] methods) {
        boolean found = false;
        for (int i = 0; i < methods.length; i++) {
            MethodInfo method = methods[i];
            if (methodsig.equals(method.getFullSignature())) {
                assertEquals(methodsig, line, method.getNumberOfLines());
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
