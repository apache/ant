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

package org.apache.tools.ant.types;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

/**
 * Class to hold a reference to another object in the project.
 *
 */
public class Reference {

    private String refid;
    private Project project;

    /**
     * Create a reference.
     * @deprecated since 1.7.
     *             Please use {@link Reference#Reference(Project,String)}
     *             instead.
     */
    @Deprecated
    public Reference() {
    }

    /**
     * Create a reference to a named ID.
     * @param id the name of this reference
     * @deprecated since 1.7.
     *             Please use {@link Reference#Reference(Project,String)}
     *             instead.
     */
    @Deprecated
    public Reference(String id) {
        setRefId(id);
    }

    /**
     * Create a reference to a named ID in a particular project.
     * @param p the project this reference is associated with
     * @param id the name of this reference
     * @since Ant 1.6.3
     */
    public Reference(Project p, String id) {
        setRefId(id);
        setProject(p);
    }

    /**
     * Set the reference id. Should not normally be necessary;
     * use {@link Reference#Reference(Project, String)}.
     * @param id the reference id to use
     */
    public void setRefId(String id) {
        refid = id;
    }

    /**
     * Get the reference id of this reference.
     * @return the reference id
     */
    public String getRefId() {
        return refid;
    }

    /**
     * Set the associated project. Should not normally be necessary;
     * use {@link Reference#Reference(Project,String)}.
     * @param p the project to use
     * @since Ant 1.6.3
     */
    public void setProject(Project p) {
        this.project = p;
    }

    /**
     * Get the associated project, if any; may be null.
     * @return the associated project
     * @since Ant 1.6.3
     */
    public Project getProject() {
        return project;
    }

    /**
     * Resolve the reference, using the associated project if
     * it set, otherwise use the passed in project.
     * @param <T> desired object type
     * @param fallback the fallback project to use if the project attribute of
     *                 reference is not set.
     * @return the dereferenced object.
     * @throws BuildException if the reference cannot be dereferenced.
     */
    public <T> T getReferencedObject(Project fallback) throws BuildException {
        if (refid == null) {
            throw new BuildException("No reference specified");
        }

        T o = project == null ? fallback.getReference(refid) : project.getReference(refid);
        if (o == null) {
            throw new BuildException("Reference " + refid + " not found.");
        }
        return o;
    }

    /**
     * Resolve the reference, looking in the associated project.
     * @see Project#getReference
     * @param <T> desired object type
     * @return the dereferenced object.
     * @throws BuildException if the project is null or the reference cannot be dereferenced
     * @since Ant 1.6.3
     */
    public <T> T getReferencedObject() throws BuildException {
        if (project == null) {
            throw new BuildException("No project set on reference to " + refid);
        }
        return getReferencedObject(project);
    }

}
