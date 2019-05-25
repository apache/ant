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

import java.util.ArrayList;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.MagicNames;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.PropertyHelper;
import org.apache.tools.ant.Task;

/**
 * This task is designed to allow the user to install a different
 * PropertyHelper on the current Project. This task also allows the
 * installation of PropertyHelper delegates on either the newly installed
 * or existing PropertyHelper.
 * @since Ant 1.8
 */
public class PropertyHelperTask extends Task {
    /**
     * Nested delegate for refid usage.
     */
    public final class DelegateElement {
        private String refid;

        private DelegateElement() {
        }

        /**
         * Get the refid.
         * @return String
         */
        public String getRefid() {
            return refid;
        }

        /**
         * Set the refid.
         * @param refid the String to set
         */
        public void setRefid(String refid) {
            this.refid = refid;
        }

        private PropertyHelper.Delegate resolve() {
            if (refid == null) {
                throw new BuildException("refid required for generic delegate");
            }
            return (PropertyHelper.Delegate) getProject().getReference(refid);
        }
    }

    private PropertyHelper propertyHelper;
    private List<Object> delegates;

    /**
     * Add a new PropertyHelper to be set on the Project.
     * @param propertyHelper the PropertyHelper to set.
     */
    public synchronized void addConfigured(PropertyHelper propertyHelper) {
        if (this.propertyHelper != null) {
            throw new BuildException("Only one PropertyHelper can be installed");
        }
        this.propertyHelper = propertyHelper;
    }

    /**
     * Add a PropertyHelper delegate to the existing or new PropertyHelper.
     * @param delegate the delegate to add.
     */
    public synchronized void addConfigured(PropertyHelper.Delegate delegate) {
        getAddDelegateList().add(delegate);
    }

    /**
     * Add a nested &lt;delegate refid="foo" /&gt; element.
     * @return DelegateElement
     */
    public DelegateElement createDelegate() {
        DelegateElement result = new DelegateElement();
        getAddDelegateList().add(result);
        return result;
    }

    /**
     * Execute the task.
     * @throws BuildException on error.
     */
    @Override
    public void execute() throws BuildException {
        if (getProject() == null) {
            throw new BuildException("Project instance not set");
        }
        if (propertyHelper == null && delegates == null) {
            throw new BuildException(
                "Either a new PropertyHelper or one or more PropertyHelper delegates are required");
        }
        PropertyHelper ph = propertyHelper;
        if (ph == null) {
            ph = PropertyHelper.getPropertyHelper(getProject());
        } else {
            ph = propertyHelper;
        }
        synchronized (ph) {
            if (delegates != null) {
                for (Object o : delegates) {
                    PropertyHelper.Delegate delegate = o instanceof DelegateElement
                            ? ((DelegateElement) o).resolve() : (PropertyHelper.Delegate) o;
                    log("Adding PropertyHelper delegate " + delegate, Project.MSG_DEBUG);
                    ph.add(delegate);
                }
            }
        }
        if (propertyHelper != null) {
            log("Installing PropertyHelper " + propertyHelper, Project.MSG_DEBUG);
            // TODO copy existing properties to new PH?
            getProject().addReference(MagicNames.REFID_PROPERTY_HELPER, propertyHelper);
        }
    }

    private synchronized List<Object> getAddDelegateList() {
        if (delegates == null) {
            delegates = new ArrayList<>();
        }
        return delegates;
    }
}
