/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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
import org.apache.tools.ant.types.Path;
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
        return getCompiler(compilerType, task, null);
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
     * @param classpath the classpath to use when looking up an
     * adapter class
     * @return the compiler adapter
     * @throws BuildException if the compiler type could not be resolved into
     * a compiler adapter.
     * @since Ant 1.8.0
     */
    public static CompilerAdapter getCompiler(String compilerType, Task task,
        Path classpath) throws BuildException {
        if ("jikes".equalsIgnoreCase(compilerType)) {
            return new Jikes();
        }
        if ("extjavac".equalsIgnoreCase(compilerType)) {
            return new JavacExternal();
        }
        if ("classic".equalsIgnoreCase(compilerType)
            || "javac1.1".equalsIgnoreCase(compilerType)
            || "javac1.2".equalsIgnoreCase(compilerType)) {
            task.log(
                "This version of java does not support the classic compiler; upgrading to modern",
                Project.MSG_WARN);
            compilerType = "modern";
        }
        //on java<=1.3 the modern falls back to classic if it is not found
        //but on java>=1.4 we just bail out early
        if ("modern".equalsIgnoreCase(compilerType)
            || "javac1.3".equalsIgnoreCase(compilerType)
            || "javac1.4".equalsIgnoreCase(compilerType)
            || "javac1.5".equalsIgnoreCase(compilerType)
            || "javac1.6".equalsIgnoreCase(compilerType)
            || "javac1.7".equalsIgnoreCase(compilerType)
            || "javac1.8".equalsIgnoreCase(compilerType)
            || "javac1.9".equalsIgnoreCase(compilerType)
            || "javac9".equalsIgnoreCase(compilerType)
            || "javac10+".equalsIgnoreCase(compilerType)) {
            // does the modern compiler exist?
            if (doesModernCompilerExist()) {
                return new Javac13();
            }
            throw new BuildException(
                "Unable to find a javac compiler;\n%s is not on the classpath.\nPerhaps JAVA_HOME does not point to the JDK.\nIt is currently set to \"%s\"",
                MODERN_COMPILER, JavaEnvUtils.getJavaHome());
        }

        if ("jvc".equalsIgnoreCase(compilerType)
            || "microsoft".equalsIgnoreCase(compilerType)) {
            return new Jvc();
        }
        if ("kjc".equalsIgnoreCase(compilerType)) {
            return new Kjc();
        }
        if ("gcj".equalsIgnoreCase(compilerType)) {
            return new Gcj();
        }
        if ("sj".equalsIgnoreCase(compilerType)
            || "symantec".equalsIgnoreCase(compilerType)) {
            return new Sj();
        }
        return resolveClassName(compilerType,
            // Memory-Leak in line below
            task.getProject().createClassLoader(classpath));
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
     * @param loader the classloader to use
     * @throws BuildException This is the fit that is thrown if className
     * isn't an instance of CompilerAdapter.
     */
    private static CompilerAdapter resolveClassName(String className,
                                                    ClassLoader loader)
        throws BuildException {
        return ClasspathUtils.newInstance(className,
                loader != null ? loader :
                CompilerAdapterFactory.class.getClassLoader(),
                CompilerAdapter.class);
    }

}
