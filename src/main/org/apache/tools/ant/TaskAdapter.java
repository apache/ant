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

package org.apache.tools.ant;

import java.lang.reflect.Method;

import org.apache.tools.ant.dispatch.DispatchUtils;
import org.apache.tools.ant.dispatch.Dispatchable;

/**
 * Uses introspection to "adapt" an arbitrary Bean which doesn't
 * itself extend Task, but still contains an execute method and optionally
 * a setProject method.
 *
 */
public class TaskAdapter extends Task implements TypeAdapter {

    /** Object to act as a proxy for. */
    private Object proxy;

    /**
     * No-arg constructor for reflection.
     */
    public TaskAdapter() {
    }

    /**
     * Constructor for given proxy.
     * So you could write easier code
     * <pre>
     * myTaskContainer.addTask(new TaskAdapter(myProxy));
     * </pre>
     *
     * @param proxy The object which Ant should use as task.
     */
    public TaskAdapter(Object proxy) {
        this();
        setProxy(proxy);
    }

    /**
     * Checks whether or not a class is suitable to be adapted by TaskAdapter.
     * If the class is of type Dispatchable, the check is not performed because
     * the method that will be executed will be determined only at runtime of
     * the actual task and not during parse time.
     *
     * This only checks conditions which are additionally required for
     * tasks adapted by TaskAdapter. Thus, this method should be called by
     * Project.checkTaskClass.
     *
     * Throws a BuildException and logs as Project.MSG_ERR for
     * conditions that will cause the task execution to fail.
     * Logs other suspicious conditions with Project.MSG_WARN.
     *
     * @param taskClass Class to test for suitability.
     *                  Must not be <code>null</code>.
     * @param project   Project to log warnings/errors to.
     *                  Must not be <code>null</code>.
     *
     * @see Project#checkTaskClass(Class)
     */
    public static void checkTaskClass(final Class<?> taskClass,
                                      final Project project) {
        if (!Dispatchable.class.isAssignableFrom(taskClass)) {
            // don't have to check for interface, since then
            // taskClass would be abstract too.
            try {
                final Method executeM = taskClass.getMethod("execute");
                // don't have to check for public, since
                // getMethod finds public method only.
                // don't have to check for abstract, since then
                // taskClass would be abstract too.
                if (!Void.TYPE.equals(executeM.getReturnType())) {
                    final String message = "return type of execute() should be "
                        + "void but was \"" + executeM.getReturnType() + "\" in "
                        + taskClass;
                    project.log(message, Project.MSG_WARN);
                }
            } catch (NoSuchMethodException e) {
                final String message = "No public execute() in " + taskClass;
                project.log(message, Project.MSG_ERR);
                throw new BuildException(message);
            } catch (LinkageError e) {
                String message = "Could not load " + taskClass + ": " + e;
                project.log(message, Project.MSG_ERR);
                throw new BuildException(message, e);
            }
        }
    }

    /**
     * Check if the proxy class is a valid class to use
     * with this adapter.
     * The class must have a public no-arg "execute()" method.
     * @param proxyClass the class to check.
     */
    public void checkProxyClass(Class<?> proxyClass) {
        checkTaskClass(proxyClass, getProject());
    }

    /**
     * Executes the proxied task.
     *
     * @exception BuildException if the project could not be set
     * or the method could not be executed.
     */
    public void execute() throws BuildException {
        try {
            Method setLocationM =
                proxy.getClass().getMethod("setLocation", Location.class);
            if (setLocationM != null) {
                setLocationM.invoke(proxy, getLocation());
            }
        } catch (NoSuchMethodException e) {
            // ignore this if the class being used as a task does not have
            // a set location method.
        } catch (Exception ex) {
            log("Error setting location in " + proxy.getClass(),
                Project.MSG_ERR);
            throw new BuildException(ex);
        }

        try {
            Method setProjectM =
                proxy.getClass().getMethod("setProject", Project.class);
            if (setProjectM != null) {
                setProjectM.invoke(proxy, getProject());
            }
        } catch (NoSuchMethodException e) {
            // ignore this if the class being used as a task does not have
            // a set project method.
        } catch (Exception ex) {
            log("Error setting project in " + proxy.getClass(),
                Project.MSG_ERR);
            throw new BuildException(ex);
        }

        try {
            DispatchUtils.execute(proxy);
        } catch (BuildException be) {
            throw be;
        } catch (Exception ex) {
            log("Error in " + proxy.getClass(), Project.MSG_VERBOSE);
            throw new BuildException(ex);
        }
    }

    /**
     * Sets the target object to proxy for.
     *
     * @param o The target object. Must not be <code>null</code>.
     */
    public void setProxy(Object o) {
        this.proxy = o;
    }

    /**
     * Returns the target object being proxied.
     *
     * @return the target proxy object.
     */
    public Object getProxy() {
        return proxy;
    }

}
