/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2003 The Apache Software Foundation.  All rights
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

package org.apache.tools.ant.taskdefs;

import java.lang.reflect.Method;

import org.apache.tools.ant.BuildFileTest;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.TypeAdapter;


/**
 * @author Peter Reilly
 */
public class TypeAdapterTest extends BuildFileTest { 
    
    public TypeAdapterTest(String name) { 
        super(name);
    }    
    
    public void setUp() { 
        configureProject("src/etc/testcases/taskdefs/typeadapter.xml");
    }
    
    public void testTaskAdapter() {
        expectLogContaining("taskadapter", "MyExec called");
    }

    public void testRunAdapter() {
        expectLogContaining("runadapter", "MyRunnable called");
    }

    public void testRunAdapterError() {
        expectBuildExceptionContaining(
            "runadaptererror", "xx", "No public run() method in");
    }

    public void testDelay() {
        expectLogContaining("delay", "MyTask called");
    }

    public void testOnErrorReport() {
        expectLogContaining("onerror.report",
                            "MyTaskNotPresent cannot be found");
    }

    public void testOnErrorIgnore() {
        expectLog("onerror.ignore","");
    }

    public static class MyTask extends Task {
        public void execute() {
            log("MyTask called");
        }
    }
    
    public static class MyExec {
        private Project project;
        public void setProject(Project project) {
            this.project = project;
        }
        
        public void execute() {
            project.log("MyExec called");
        }
    }

    public static class MyRunnable {
        private Project project;
        public void setProject(Project project) {
            this.project = project;
        }
        
        public void run() {
            project.log("MyRunnable called");
        }
    }

    public static class RunnableAdapter
        extends Task implements TypeAdapter
    {
        private String execMethodName = "run";
        private Object proxy;
    
        public Method getExecuteMethod(Class proxyClass) {
            try {
                Method execMethod = proxyClass.getMethod(
                    execMethodName, null);
                if (!Void.TYPE.equals(execMethod.getReturnType())) {
                    String message =
                        "return type of " + execMethodName + "() should be " 
                        + "void but was \"" + execMethod.getReturnType() +
                        "\" in " 
                        + proxyClass;
                    log(message, Project.MSG_WARN);
                }
                return execMethod;
            } catch (NoSuchMethodException e) {
                String message = "No public "+ execMethodName +
                    "() method in "
                    + proxyClass;
                log(message, Project.MSG_ERR);
                throw new BuildException(message);
            }
        }
        public void checkProxyClass(Class proxyClass) {
            getExecuteMethod(proxyClass);
        }
        
        public void setProxy(Object o) {
            getExecuteMethod(o.getClass());
            this.proxy = o;
        }

        public Object getProxy() {
            return proxy;
        }
    
        public void execute() {
            getProject().setProjectReference(proxy);
            Method executeMethod = getExecuteMethod(proxy.getClass());
            try {
                executeMethod.invoke(proxy, null);
            } catch (java.lang.reflect.InvocationTargetException ie) {
                log("Error in " + proxy.getClass(), Project.MSG_ERR);
                Throwable t = ie.getTargetException();
                if (t instanceof BuildException) {
                    throw ((BuildException) t);
                } else {
                    throw new BuildException(t);
                }
            } catch (Exception ex) {
                log("Error in " + proxy.getClass(), Project.MSG_ERR);
                throw new BuildException(ex);
            }
        }
    }
}
