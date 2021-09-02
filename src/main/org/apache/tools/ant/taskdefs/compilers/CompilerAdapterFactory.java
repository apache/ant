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

import java.util.Arrays;
import java.util.List;

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

    public static final String COMPILER_JIKES = "jikes";
    public static final String COMPILER_GCJ = "gcj";
    public static final String COMPILER_SYMANTEC_ALIAS = "sj";
    public static final String COMPILER_SYMANTEC = "symantec";
    public static final String COMPILER_JVC_ALIAS = "microsoft";
    public static final String COMPILER_JVC = "jvc";
    public static final String COMPILER_KJC = "kjc";

    public static final String COMPILER_JAVAC_1_1 = "javac1.1";
    public static final String COMPILER_JAVAC_1_2 = "javac1.2";
    public static final String COMPILER_JAVAC_1_3 = "javac1.3";
    public static final String COMPILER_JAVAC_1_4 = "javac1.4";
    public static final String COMPILER_JAVAC_1_5 = "javac1.5";
    public static final String COMPILER_JAVAC_1_6 = "javac1.6";
    public static final String COMPILER_JAVAC_1_7 = "javac1.7";
    public static final String COMPILER_JAVAC_1_8 = "javac1.8";
    public static final String COMPILER_JAVAC_9_ALIAS = "javac1.9";
    public static final String COMPILER_JAVAC_9 = "javac9";
    public static final String COMPILER_JAVAC_10_PLUS = "javac10+";

    public static final String COMPILER_CLASSIC = "classic";
    public static final String COMPILER_MODERN = "modern";
    public static final String COMPILER_EXTJAVAC = "extJavac";

    public static final String COMPILER_MODERN_CLASSNAME = Javac13.class.getName();
    public static final String COMPILER_EXTJAVAC_CLASSNAME = JavacExternal.class.getName();

    private static final List<String> JDK_COMPILERS = Arrays.asList(
        COMPILER_JAVAC_1_1,
        COMPILER_JAVAC_1_2,
        COMPILER_JAVAC_1_3,
        COMPILER_JAVAC_1_4,
        COMPILER_JAVAC_1_5,
        COMPILER_JAVAC_1_6,
        COMPILER_JAVAC_1_7,
        COMPILER_JAVAC_1_8,
        COMPILER_JAVAC_9_ALIAS,
        COMPILER_JAVAC_9,
        COMPILER_JAVAC_10_PLUS,
        COMPILER_CLASSIC,
        COMPILER_MODERN,
        COMPILER_EXTJAVAC,
        COMPILER_MODERN_CLASSNAME,
        COMPILER_EXTJAVAC_CLASSNAME
    );

    private static final List<String> FORKED_JDK_COMPILERS = Arrays.asList(
        COMPILER_EXTJAVAC,
        COMPILER_EXTJAVAC_CLASSNAME
    );

    private static final List<String> JDK_COMPILER_NICKNAMES = Arrays.asList(
        COMPILER_CLASSIC,
        COMPILER_MODERN,
        COMPILER_EXTJAVAC,
        COMPILER_MODERN_CLASSNAME,
        COMPILER_EXTJAVAC_CLASSNAME
    );

    private static final List<String> CLASSIC_JDK_COMPILERS = Arrays.asList(
        COMPILER_JAVAC_1_1,
        COMPILER_JAVAC_1_2
    );

    private static final List<String> MODERN_JDK_COMPILERS = Arrays.asList(
        COMPILER_JAVAC_1_3,
        COMPILER_JAVAC_1_4,
        COMPILER_JAVAC_1_5,
        COMPILER_JAVAC_1_6,
        COMPILER_JAVAC_1_7,
        COMPILER_JAVAC_1_8,
        COMPILER_JAVAC_9_ALIAS,
        COMPILER_JAVAC_9,
        COMPILER_JAVAC_10_PLUS,
        COMPILER_MODERN_CLASSNAME
    );

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
        if (COMPILER_JIKES.equalsIgnoreCase(compilerType)) {
            return new Jikes();
        }
        if (isForkedJavac(compilerType)) {
            return new JavacExternal();
        }
        if (COMPILER_CLASSIC.equalsIgnoreCase(compilerType)
            || isClassicJdkCompiler(compilerType)) {
            task.log(
                "This version of java does not support the classic compiler; upgrading to modern",
                Project.MSG_WARN);
            compilerType = COMPILER_MODERN;
        }
        if (COMPILER_MODERN.equalsIgnoreCase(compilerType)
            || isModernJdkCompiler(compilerType)) {
            // does the modern compiler exist?
            if (doesModernCompilerExist()) {
                return new Javac13();
            }
            throw new BuildException(
                "Unable to find a javac compiler;\n%s is not on the classpath.\nPerhaps JAVA_HOME does not point to the JDK.\nIt is currently set to \"%s\"",
                MODERN_COMPILER, JavaEnvUtils.getJavaHome());
        }

        if (COMPILER_JVC.equalsIgnoreCase(compilerType)
            || COMPILER_JVC_ALIAS.equalsIgnoreCase(compilerType)) {
            return new Jvc();
        }
        if (COMPILER_KJC.equalsIgnoreCase(compilerType)) {
            return new Kjc();
        }
        if (COMPILER_GCJ.equalsIgnoreCase(compilerType)) {
            return new Gcj();
        }
        if (COMPILER_SYMANTEC_ALIAS.equalsIgnoreCase(compilerType)
            || COMPILER_SYMANTEC.equalsIgnoreCase(compilerType)) {
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

    /**
     * Is the compiler implementation a forked jdk compiler?
     *
     * @param compilerImpl the name of the compiler implementation
     * @since 1.10.12
     */
    public static boolean isForkedJavac(final String compilerName) {
        return containsIgnoreCase(FORKED_JDK_COMPILERS, compilerName);
    }

    /**
     * Is the compiler implementation a jdk compiler?
     *
     * @param compilerImpl the name of the compiler implementation
     * @since 1.10.12
     */
    public static boolean isJdkCompiler(final String compilerName) {
        return containsIgnoreCase(JDK_COMPILERS, compilerName);
    }

    /**
     * Is the compiler implementation a jdk compiler without specified version?
     *
     * @param compilerImpl the name of the compiler implementation
     * @since 1.10.12
     */
    public static boolean isJdkCompilerNickname(final String compilerName) {
        return containsIgnoreCase(JDK_COMPILER_NICKNAMES, compilerName);
    }

    /**
     * Does the compiler correspond to "classic"?
     *
     * @param compilerImpl the name of the compiler implementation
     * @since 1.10.12
     */
    public static boolean isClassicJdkCompiler(final String compilerName) {
        return containsIgnoreCase(CLASSIC_JDK_COMPILERS, compilerName);
    }

    /**
     * Does the compiler correspond to "modern"?
     *
     * @param compilerImpl the name of the compiler implementation
     * @since 1.10.12
     */
    public static boolean isModernJdkCompiler(final String compilerName) {
        return containsIgnoreCase(MODERN_JDK_COMPILERS, compilerName);
    }

    private static boolean containsIgnoreCase(final List<String> compilers, final String compilerName) {
        return compilerName != null && compilers.stream().anyMatch(compilerName::equalsIgnoreCase);
    }
}
