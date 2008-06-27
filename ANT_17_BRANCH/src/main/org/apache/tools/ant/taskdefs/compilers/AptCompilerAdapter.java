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
import org.apache.tools.ant.taskdefs.Apt;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.Path;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.Vector;


/**
 * The implementation of the apt compiler for JDK 1.5
 * <p/>
 * As usual, the low level entry points for Java tools are neither documented or
 * stable; this entry point may change from that of 1.5.0_01-b08 without any
 * warning at all. The IDE decompile of the tool entry points is as follows:
 * <pre>
 * public class Main {
 * public Main() ;
 * <p/>
 * public static transient void main(String... strings) ;
 * <p/>
 * public static transient int process(String... strings);
 * <p/>
 * public static transient int process(PrintWriter printWriter,
 *      String... strings) ;
 * public static transient int process(
 *      AnnotationProcessorFactory annotationProcessorFactory,
 *      String... strings) ;
 * <p/>
 * public static transient int process(
 *      AnnotationProcessorFactory annotationProcessorFactory,
 *      PrintWriter printWriter,
 *      String... strings);
 * private static transient int processing(
 *      AnnotationProcessorFactory annotationProcessorFactory,
 *      PrintWriter printWriter,
 *      String... strings) ;
 * }
 * </pre>
 *
 * This Adapter is designed to run Apt in-JVM, an option that is not actually
 * exposed to end-users, because it was too brittle during beta testing; classpath
 * problems being the core issue.
 *
 *
 *
 * @since Ant 1.7
 */
public class AptCompilerAdapter extends DefaultCompilerAdapter {

    /**
     * Integer returned by the Apt compiler to indicate success.
     */
    private static final int APT_COMPILER_SUCCESS = 0;
    /**
     * class in tools.jar that implements APT
     */
    public static final String APT_ENTRY_POINT = "com.sun.tools.apt.Main";

    /**
     * method used to compile.
     */
    public static final String APT_METHOD_NAME = "process";

    /**
     * Get the facade task that fronts this adapter
     *
     * @return task instance
     * @see DefaultCompilerAdapter#getJavac()
     */
    protected Apt getApt() {
        return (Apt) getJavac();
    }

    /**
     * Using the front end arguments, set up the command line to run Apt
     *
     * @param apt task
     * @param cmd command that is set up with the various switches from the task
     *            options
     */
    static void setAptCommandlineSwitches(Apt apt, Commandline cmd) {

        if (!apt.isCompile()) {
            cmd.createArgument().setValue("-nocompile");
        }

        // Process the factory class
        String factory = apt.getFactory();
        if (factory != null) {
            cmd.createArgument().setValue("-factory");
            cmd.createArgument().setValue(factory);
        }

        // Process the factory path
        Path factoryPath = apt.getFactoryPath();
        if (factoryPath != null) {
            cmd.createArgument().setValue("-factorypath");
            cmd.createArgument().setPath(factoryPath);
        }

        File preprocessDir = apt.getPreprocessDir();
        if (preprocessDir != null) {
            cmd.createArgument().setValue("-s");
            cmd.createArgument().setFile(preprocessDir);
        }

        // Process the processor options
        Vector options = apt.getOptions();
        Enumeration elements = options.elements();
        Apt.Option opt;
        StringBuffer arg = null;
        while (elements.hasMoreElements()) {
            opt = (Apt.Option) elements.nextElement();
            arg = new StringBuffer();
            arg.append("-A").append(opt.getName());
            if (opt.getValue() != null) {
                arg.append("=").append(opt.getValue());
            }
            cmd.createArgument().setValue(arg.toString());
        }
    }

    /**
     * using our front end task, set up the command line switches
     *
     * @param cmd command line to set up
     */
    protected void setAptCommandlineSwitches(Commandline cmd) {
        Apt apt = getApt();
        setAptCommandlineSwitches(apt, cmd);
    }

    /**
     * Run the compilation.
     * @return true on success.
     * @throws BuildException if the compilation has problems.
     */
    public boolean execute() throws BuildException {
        attributes.log("Using apt compiler", Project.MSG_VERBOSE);
        //set up the javac options
        Commandline cmd = setupModernJavacCommand();
        //then add the Apt options
        setAptCommandlineSwitches(cmd);

        //finally invoke APT
        // Use reflection to be able to build on all JDKs:
        try {
            Class c = Class.forName(APT_ENTRY_POINT);
            Object compiler = c.newInstance();
            Method compile = c.getMethod(APT_METHOD_NAME,
                    new Class[]{(new String[]{}).getClass()});
            int result = ((Integer) compile.invoke
                    (compiler, new Object[]{cmd.getArguments()}))
                    .intValue();
            return (result == APT_COMPILER_SUCCESS);
        } catch (BuildException be) {
            //rethrow build exceptions
            throw be;
        } catch (Exception ex) {
            //cast everything else to a build exception
            throw new BuildException("Error starting apt compiler",
                    ex, location);
        }
    }
}
