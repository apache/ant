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

package org.apache.tools.ant.taskdefs.compilers;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.util.JavaEnvUtils;

/**
 * Creates the necessary compiler adapter, given basic criteria.
 *
 * @author <a href="mailto:jayglanville@home.com">J D Glanville</a>
 * @since Ant 1.3
 */
public class CompilerAdapterFactory {

    /** This is a singleton -- can't create instances!! */
    private CompilerAdapterFactory() {
    }

    /**
     * Based on the parameter passed in, this method creates the necessary
     * factory desired.
     *
     * The current mapping for compiler names are as follows:
     * <ul><li>jikes = jikes compiler
     * <li>classic, javac1.1, javac1.2 = the standard compiler from JDK
     * 1.1/1.2
     * <li>modern, javac1.3, javac1.4 = the compiler of JDK 1.3+
     * <li>jvc, microsoft = the command line compiler from Microsoft's SDK
     * for Java / Visual J++
     * <li>kjc = the kopi compiler</li>
     * <li>gcj = the gcj compiler from gcc</li>
     * <li>sj, symantec = the Symantec Java compiler</li>
     * <li><i>a fully quallified classname</i> = the name of a compiler
     * adapter
     * </ul>
     *
     * @param compilerType either the name of the desired compiler, or the
     * full classname of the compiler's adapter.
     * @param task a task to log through.
     * @throws BuildException if the compiler type could not be resolved into
     * a compiler adapter.
     */
    public static CompilerAdapter getCompiler(String compilerType, Task task) 
        throws BuildException {
            boolean isClassicCompilerSupported = true;
            //as new versions of java come out, add them to this test
            if (JavaEnvUtils.isJavaVersion(JavaEnvUtils.JAVA_1_4)) {
                isClassicCompilerSupported = false;
            }

            if (compilerType.equalsIgnoreCase("jikes")) {
                return new Jikes();
            }
            if (compilerType.equalsIgnoreCase("extJavac")) {
                return new JavacExternal();
            }       
            if (compilerType.equalsIgnoreCase("classic") ||
                compilerType.equalsIgnoreCase("javac1.1") ||
                compilerType.equalsIgnoreCase("javac1.2")) {
                if (isClassicCompilerSupported) {
                    return new Javac12();
                } else {
                    task.log("This version of java does "
                                             + "not support the classic "
                                             + "compiler; upgrading to modern",
                                             Project.MSG_WARN);
                    compilerType="modern";
                }
            }
            //on java<=1.3 the modern falls back to classic if it is not found
            //but on java>=1.4 we just bail out early
            if (compilerType.equalsIgnoreCase("modern") ||
                compilerType.equalsIgnoreCase("javac1.3") ||
                compilerType.equalsIgnoreCase("javac1.4")) {
                // does the modern compiler exist?
                if (doesModernCompilerExist()) {
                    return new Javac13();
                } else {
                    if (isClassicCompilerSupported) {
                        task.log("Modern compiler not found - looking for "
                                 + "classic compiler", Project.MSG_WARN);
                        return new Javac12();
                    } else {
                        throw new BuildException("Unable to find a javac " 
                                                 + "compiler;\n"
                                                 + "com.sun.tools.javac.Main "
                                                 + "is not on the " 
                                                 + "classpath.\n"
                                                 + "Perhaps JAVA_HOME does not"
                                                 + " point to the JDK");
                    }
                }
            }

            if (compilerType.equalsIgnoreCase("jvc") ||
                compilerType.equalsIgnoreCase("microsoft")) {
                return new Jvc();
            }
            if (compilerType.equalsIgnoreCase("kjc")) {
                return new Kjc();
            }
            if (compilerType.equalsIgnoreCase("gcj")) {
                return new Gcj();
            }
            if (compilerType.equalsIgnoreCase("sj") ||
                compilerType.equalsIgnoreCase("symantec")) {
                return new Sj();
            }
            return resolveClassName(compilerType);
        }

    /**
     * query for the Modern compiler existing
     * @return true iff classic os on the classpath
     */ 
    private static boolean doesModernCompilerExist() {
        try {
            Class.forName("com.sun.tools.javac.Main");
            return true;
        } catch (ClassNotFoundException cnfe) {
            return false;
        }
    }
    
    /**
     * Tries to resolve the given classname into a compiler adapter.
     * Throws a fit if it can't.
     *
     * @param className The fully qualified classname to be created.
     * @throws BuildException This is the fit that is thrown if className
     * isn't an instance of CompilerAdapter.
     */
    private static CompilerAdapter resolveClassName(String className)
        throws BuildException {
        try {
            Class c = Class.forName(className);
            Object o = c.newInstance();
            return (CompilerAdapter) o;
        } catch (ClassNotFoundException cnfe) {
            throw new BuildException("Compiler Adapter '"+className 
                    + "' can\'t be found.", cnfe);
        } catch (ClassCastException cce) {
            throw new BuildException(className + " isn\'t the classname of "
                    + "a compiler adapter.", cce);
        } catch (Throwable t) {
            // for all other possibilities
            throw new BuildException("Compiler Adapter "+className 
                    + " caused an interesting exception.", t);
        }
    }

}
