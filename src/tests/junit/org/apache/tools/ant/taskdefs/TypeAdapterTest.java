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

package org.apache.tools.ant.taskdefs;

import java.lang.reflect.Method;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildFileRule;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.TypeAdapter;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 */
public class TypeAdapterTest {

    @Rule
    public final BuildFileRule buildRule = new BuildFileRule();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/taskdefs/typeadapter.xml");
    }

    @Test
    public void testTaskAdapter() {
        buildRule.executeTarget("taskadapter");
        assertThat(buildRule.getLog(), containsString("MyExec called"));
    }

    @Test
    public void testRunAdapter() {
        buildRule.executeTarget("runadapter");
        assertThat(buildRule.getLog(), containsString("MyRunnable called"));
    }

    @Test
    public void testRunAdapterError() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("No public run() method in");
        buildRule.executeTarget("runadaptererror");
    }

    @Test
    public void testDelay() {
        buildRule.executeTarget("delay");
        assertThat(buildRule.getLog(), containsString("MyTask called"));
    }

    @Test
    public void testOnErrorReport() {
        buildRule.executeTarget("onerror.report");
        assertThat(buildRule.getLog(), containsString("MyTaskNotPresent cannot be found"));
    }

    @Test
    public void testOnErrorIgnore() {
        buildRule.executeTarget("onerror.ignore");
        assertEquals("", buildRule.getLog());
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
        extends Task implements TypeAdapter {
        private String execMethodName = "run";
        private Object proxy;

        public Method getExecuteMethod(Class<?> proxyClass) {
            try {
                Method execMethod = proxyClass.getMethod(execMethodName);
                if (!Void.TYPE.equals(execMethod.getReturnType())) {
                    String message = "return type of " + execMethodName + "() should be "
                            + "void but was \"" + execMethod.getReturnType()
                            + "\" in " + proxyClass;
                    log(message, Project.MSG_WARN);
                }
                return execMethod;
            } catch (NoSuchMethodException e) {
                String message = "No public " + execMethodName + "() method in " + proxyClass;
                log(message, Project.MSG_ERR);
                throw new BuildException(message);
            }
        }
        public void checkProxyClass(Class<?> proxyClass) {
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
                executeMethod.invoke(proxy);
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
