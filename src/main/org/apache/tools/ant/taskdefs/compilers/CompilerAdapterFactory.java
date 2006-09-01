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

package org.apache.tools.ant.taskdefs.compilers;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.util.ClasspathUtils;
import org.apache.tools.ant.util.JavaEnvUtils;

/**
 * Creates the necessary compiler adapter, given basic criteria.
 *
 * @since Ant 1.3
 */
public final class CompilerAdapterFactory {
    private static final String MODERN_COMPILER = "com.sun.tools.javac.Main";

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
     * <li>modern, javac1.3, javac1.4, javac1.5 = the compiler of JDK 1.3+
     * <li>jvc, microsoft = the command line compiler from Microsoft's SDK
     * for Java / Visual J++
     * <li>kjc = the kopi compiler</li>
     * <li>gcj = the gcj compiler from gcc</li>
     * <li>sj, symantec = the Symantec Java compiler</li>
     * <li><i>a fully qualified classname</i> = the name of a compiler
     * adapter
     * </ul>
     *
     * @param compilerType either the name of the desired compiler, or the
     * full classname of the compiler's adapter.
     * @param task a task to log through.
     * @return the compiler adapter
     * @throws BuildException if the compiler type could not be resolved into
     * a compiler adapter.
     */
    public static CompilerAdapter getCompiler(String compilerType, Task task)
        throws BuildException {
            boolean isClassicCompilerSupported = true;
            //as new versions of java come out, add them to this test
            if (!JavaEnvUtils.isJavaVersion(JavaEnvUtils.JAVA_1_2)
                && !JavaEnvUtils.isJavaVersion(JavaEnvUtils.JAVA_1_3)) {
                isClassicCompilerSupported = false;
            }

            if (compilerType.equalsIgnoreCase("jikes")) {
                return new Jikes();
            }
            if (compilerType.equalsIgnoreCase("extJavac")) {
                return new JavacExternal();
            }
            if (compilerType.equalsIgnoreCase("classic")
                || compilerType.equalsIgnoreCase("javac1.1")
                || compilerType.equalsIgnoreCase("javac1.2")) {
                if (isClassicCompilerSupported) {
                    return new Javac12();
                } else {
                    task.log("This version of java does "
                                             + "not support the classic "
                                             + "compiler; upgrading to modern",
                                             Project.MSG_WARN);
                    compilerType = "modern";
                }
            }
            //on java<=1.3 the modern falls back to classic if it is not found
            //but on java>=1.4 we just bail out early
            if (compilerType.equalsIgnoreCase("modern")
                || compilerType.equalsIgnoreCase("javac1.3")
                || compilerType.equalsIgnoreCase("javac1.4")
                || compilerType.equalsIgnoreCase("javac1.5")
                || compilerType.equalsIgnoreCase("javac1.6")) {
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
                                                 + MODERN_COMPILER
                                                 + " is not on the "
                                                 + "classpath.\n"
                                                 + "Perhaps JAVA_HOME does not"
                                                 + " point to the JDK.\n"
                                + "It is currently set to \""
                                + JavaEnvUtils.getJavaHome()
                                + "\"");
                    }
                }
            }

            if (compilerType.equalsIgnoreCase("jvc")
                || compilerType.equalsIgnoreCase("microsoft")) {
                return new Jvc();
            }
            if (compilerType.equalsIgnoreCase("kjc")) {
                return new Kjc();
            }
            if (compilerType.equalsIgnoreCase("gcj")) {
                return new Gcj();
            }
            if (compilerType.equalsIgnoreCase("sj")
                || compilerType.equalsIgnoreCase("symantec")) {
                return new Sj();
            }
            return resolveClassName(compilerType);
        }

    /**
     * query for the Modern compiler existing
     * @return true if classic os on the classpath
     */
    private static boolean doesModernCompilerExist() {
        try {
            Class.forName(MODERN_COMPILER);
            return true;
        } catch (ClassNotFoundException cnfe) {
            try {
                ClassLoader cl = CompilerAdapterFactory.class.getClassLoader();
                if (cl != null) {
                    cl.loadClass(MODERN_COMPILER);
                    return true;
                }
            } catch (ClassNotFoundException cnfe2) {
                // Ignore Exception
            }
        }
        return false;
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
        return (CompilerAdapter) ClasspathUtils.newInstance(className,
                CompilerAdapterFactory.class.getClassLoader(),
                CompilerAdapter.class);
    }

}
