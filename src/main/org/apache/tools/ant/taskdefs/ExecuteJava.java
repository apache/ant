/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000 The Apache Software Foundation.  All rights
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
 * 4. The names "The Jakarta Project", "Ant", and "Apache Software
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


package org.apache.tools.ant.taskdefs;

import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.CommandlineJava;
import org.apache.tools.ant.types.Path;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.io.*;

/*
 *
 * @author thomas.haas@softwired-inc.com
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 */
public class ExecuteJava {

    private Commandline javaCommand = null;
    private Path classpath = null;
    private CommandlineJava.SysProperties sysProperties = null;
    private PrintStream out;

    public void setJavaCommand(Commandline javaCommand) {
        this.javaCommand = javaCommand;
    }

    public void setClasspath(Path p) {
        classpath = p;
    }

    public void setSystemProperties(CommandlineJava.SysProperties s) {
        sysProperties = s;
    }

    /**
     * All output (System.out as well as System.err) will be written
     * to this Stream.
     */
    public void setOutput(PrintStream out) {
        this.out = out;
    }

    public void execute(Project project) throws BuildException{
        PrintStream sOut = System.out;
        PrintStream sErr = System.err;

        final String classname = javaCommand.getExecutable();
        final Object[] argument = { javaCommand.getArguments() };
        try {
            if (sysProperties != null) {
                sysProperties.setSystem();
            }

            if (out != null) {
                System.setErr(out);
                System.setOut(out);
            }

            final Class[] param = { Class.forName("[Ljava.lang.String;") };
            Class target = null;
            if (classpath == null) {
                target = Class.forName(classname);
            } else {
                AntClassLoader loader = new AntClassLoader(project, classpath, false);
                loader.setIsolated(true);
                target = loader.forceLoadClass(classname);
            }
            final Method main = target.getMethod("main", param);
            main.invoke(null, argument);

        } catch (NullPointerException e) {
            throw new BuildException("Could not find main() method in " + classname);
        } catch (ClassNotFoundException e) {
            throw new BuildException("Could not find " + classname + ". Make sure you have it in your classpath");
        } catch (InvocationTargetException e) {
            Throwable t = e.getTargetException();
            if (!(t instanceof SecurityException)) {
                throw new BuildException(t);
            }
            // else ignore because the security exception is thrown
            // if the invoked application tried to call System.exit()
        } catch (Exception e) {
            throw new BuildException(e);
        } finally {
            if (sysProperties != null) {
                sysProperties.restoreSystem();
            }
            if (out != null) {
                System.setOut(sOut);
                System.setErr(sErr);
                out.close();
            }
        }
    }
}
