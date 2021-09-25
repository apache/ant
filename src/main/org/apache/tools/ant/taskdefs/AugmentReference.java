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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.RuntimeConfigurable;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.TypeAdapter;

/**
 * Ant task to dynamically augment a previously declared reference.
 * @since Ant 1.8.1
 */
public class AugmentReference extends Task implements TypeAdapter {
    private String id;

    /**
     * {@inheritDoc}
     */
    public void checkProxyClass(Class<?> proxyClass) {
    }

    /**
     * {@inheritDoc}
     */
    public synchronized Object getProxy() {
        if (getProject() == null) {
            throw new IllegalStateException(getTaskName() + "Project owner unset");
        }
        hijackId();
        if (getProject().hasReference(id)) {
            Object result = getProject().getReference(id);
            log("project reference " + id + "=" + result, Project.MSG_DEBUG);
            return result;
        }
        throw new BuildException("Unknown reference \"" + id + "\"");
    }

    /**
     * {@inheritDoc}
     */
    public void setProxy(Object o) {
        throw new UnsupportedOperationException();
    }

    private synchronized void hijackId() {
        if (id == null) {
            RuntimeConfigurable wrapper = getWrapper();
            id = wrapper.getId();
            if (id == null) {
                throw new BuildException(getTaskName() + " attribute 'id' unset");
            }
            wrapper.setAttribute("id", null);
            wrapper.removeAttribute("id");
            wrapper.setElementTag("augmented reference \"" + id + "\"");
        }
    }

    /**
     * Overridden to restore the wrapper once it is no longer needed.
     * @since Ant 1.8.3
     */
    public void execute() {
        restoreWrapperId();
    }

    /**
     * Needed if two different targets reuse the same instance.
     * @see "https://issues.apache.org/bugzilla/show_bug.cgi?id=50894"
     */
    private synchronized void restoreWrapperId() {
        if (id != null) {
            log("restoring augment wrapper " + id, Project.MSG_DEBUG);
            RuntimeConfigurable wrapper = getWrapper();
            wrapper.setAttribute("id", id);
            wrapper.setElementTag(getTaskName());
            id = null;
        }
    }

}
