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
package org.apache.tools.ant.dispatch;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.UnknownElement;

/**
 * Determines and Executes the action method for the task.
 */
// CheckStyle:HideUtilityClassConstructorCheck OFF - (bc)
public class DispatchUtils {
    /**
     * Determines and Executes the action method for the task.
     * @param task the task to execute.
     * @throws BuildException on error.
     */
    public static final void execute(Object task) throws BuildException {
        String methodName = "execute";
        Dispatchable dispatchable = null;
        try {
            if (task instanceof Dispatchable) {
                dispatchable = (Dispatchable) task;
            } else if (task instanceof UnknownElement) {
                UnknownElement ue = (UnknownElement) task;
                Object realThing = ue.getRealThing();
                if (realThing instanceof Dispatchable
                        && realThing instanceof Task) {
                    dispatchable = (Dispatchable) realThing;
                }
            }
            if (dispatchable != null) {
                String mName = null;
                try {
                    final String name = dispatchable.getActionParameterName();
                    if (name == null || name.trim().isEmpty()) {
                        throw new BuildException(
                            "Action Parameter Name must not be empty for Dispatchable Task.");
                    }
                    mName = "get" + name.trim().substring(0, 1).toUpperCase();
                    if (name.length() > 1) {
                        mName += name.substring(1);
                    }
                    final Class<? extends Dispatchable> c = dispatchable.getClass();
                    final Method actionM = c.getMethod(mName);
                    if (actionM != null) {
                        final Object o = actionM.invoke(dispatchable, (Object[]) null);
                        if (o == null) {
                                throw new BuildException(
                                    "Dispatchable Task attribute '" + name.trim()
                                    + "' not set or value is empty.");
                        }
                        methodName = o.toString().trim();
                        if (methodName.isEmpty()) {
                            throw new BuildException(
                                "Dispatchable Task attribute '" + name.trim()
                                + "' not set or value is empty.");
                        }
                        Method executeM = dispatchable.getClass().getMethod(methodName);
                        if (executeM == null) {
                            throw new BuildException(
                                "No public " + methodName + "() in "
                                + dispatchable.getClass());
                        }
                        executeM.invoke(dispatchable, (Object[]) null);
                        if (task instanceof UnknownElement) {
                            ((UnknownElement) task).setRealThing(null);
                        }
                    }
                } catch (NoSuchMethodException nsme) {
                    throw new BuildException("No public " + mName + "() in " + task.getClass());
                }
            } else {
                Method executeM = null;
                executeM = task.getClass().getMethod(methodName);
                if (executeM == null) {
                    throw new BuildException("No public " + methodName + "() in "
                        + task.getClass());
                }
                executeM.invoke(task);
                if (task instanceof UnknownElement) {
                    ((UnknownElement) task).setRealThing(null);
                }
            }
        } catch (InvocationTargetException ie) {
            Throwable t = ie.getTargetException();
            if (t instanceof BuildException) {
                throw ((BuildException) t);
            } else {
                throw new BuildException(t);
            }
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new BuildException(e);
        }
    }
}
