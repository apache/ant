/*
 * Copyright 2005 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
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

import org.apache.tools.ant.Task;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.UnknownElement;
import org.apache.tools.ant.RuntimeConfigurable;

/**
 * Clone an Object from a reference.
 * @since Ant 1.7
 */
public class Clone extends UnknownElement {
    /** Task name. */
    public static final String TASK_NAME = "clone";

    /** Clone reference attribute ID. */
    public static final String CLONE_REF = "cloneref";

    private static final Class[] NO_ARGS = new Class[] {};

    /**
     * Create a new instance of the Clone task.
     */
    public Clone() {
        super(TASK_NAME);
    }

    /**
     * Creates a named task or data type. If the real object is a task,
     * it is configured up to the init() stage.
     *
     * @param ue The UnknownElement to create the real object for.
     *           Not used in this implementation.
     * @param w  The RuntimeConfigurable containing the configuration
     *           information to pass to the cloned Object.
     *
     * @return the task or data type represented by the given unknown element.
     */
    protected Object makeObject(UnknownElement ue, RuntimeConfigurable w) {
        String cloneref = (String) (w.getAttributeMap().get(CLONE_REF));
        if (cloneref == null) {
            throw new BuildException("cloneref attribute not set");
        }
        Object ob = getProject().getReference(cloneref);
        if (ob == null) {
            throw new BuildException(
                "reference \"" + cloneref + "\" not found");
        }
        try {
            log("Attempting to clone " + ob.toString() + " \""
                + cloneref + "\"", Project.MSG_VERBOSE);
            Method m = ob.getClass().getMethod("clone", NO_ARGS);
            try {
                Object bo = m.invoke(ob, NO_ARGS);
                if (bo == null) {
                    throw new BuildException(m.toString() + " returned null");
                }
                w.removeAttribute(CLONE_REF);
                w.setProxy(bo);
                w.setElementTag(null);
                setRuntimeConfigurableWrapper(w);
                if (bo instanceof Task) {
                    ((Task) bo).setOwningTarget(getOwningTarget());
                    ((Task) bo).init();
                }
                return bo;
            } catch (Exception e) {
                throw new BuildException(e);
            }
        } catch (NoSuchMethodException e) {
            throw new BuildException(
                "Unable to locate public clone method for object \""
                + cloneref + "\"");
        }
    }

}
