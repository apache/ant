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

package org.apache.tools.ant.taskdefs;

import java.lang.reflect.Method;

import org.apache.tools.ant.BuildFileTest;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.TypeAdapter;


/**
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
