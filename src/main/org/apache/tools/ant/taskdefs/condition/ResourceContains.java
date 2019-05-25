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
package org.apache.tools.ant.taskdefs.condition;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.resources.FileResource;
import org.apache.tools.ant.util.FileUtils;

/**
 * &lt;resourcecontains&gt;
 * Is a string contained in a resource (file currently)?
 * @since Ant 1.7.1
 */
public class ResourceContains implements Condition {

    private Project project;
    private String substring;
    private Resource resource;
    private String refid;
    private boolean casesensitive = true;

    /**
     * Set this condition's Project.
     * @param project Project
     */
    public void setProject(Project project) {
        this.project = project;
    }

    /**
     * Get this condition's Project.
     * @return Project
     */
    public Project getProject() {
        return project;
    }

    /**
     * Sets the resource to search
     * @param r the value to use.
     */
    public void setResource(String r) {
        this.resource = new FileResource(new File(r));
    }

    /**
     * Sets the refid to search; should indicate a resource directly
     * or by way of a single-element ResourceCollection.
     * @param refid the value to use.
     */
    public void setRefid(String refid) {
        this.refid = refid;
    }

    private void resolveRefid() {
        try {
            if (getProject() == null) {
                throw new BuildException("Cannot retrieve refid; project unset");
            }
            Object o = getProject().getReference(refid);
            if (!(o instanceof Resource)) {
                if (o instanceof ResourceCollection) {
                    ResourceCollection rc = (ResourceCollection) o;
                    if (rc.size() == 1) {
                        o = rc.iterator().next();
                    }
                } else {
                    throw new BuildException("Illegal value at '%s': %s", refid, o);
                }
            }
            this.resource = (Resource) o;
        } finally {
            refid = null;
        }
    }

    /**
     * Sets the substring to look for
     * @param substring the value to use.
     */
    public void setSubstring(String substring) {
        this.substring = substring;
    }

    /**
     * Sets case sensitivity attribute.
     * @param casesensitive the value to use.
     */
    public void setCasesensitive(boolean casesensitive) {
        this.casesensitive = casesensitive;
    }

    private void validate() {
        if (resource != null && refid != null) {
            throw new BuildException("Cannot set both resource and refid");
        }
        if (resource == null && refid != null) {
            resolveRefid();
        }
        if (resource == null || substring == null) {
            throw new BuildException(
                "both resource and substring are required in <resourcecontains>");
        }
    }

    /**
     * Evaluates the condition.
     * @return true if the substring is contained in the resource
     * @throws BuildException if there is a problem.
     */
    @Override
    public synchronized boolean eval() throws BuildException {
        validate();

        if (substring.isEmpty()) {
            if (getProject() != null) {
                getProject().log("Substring is empty; returning true",
                                 Project.MSG_VERBOSE);
            }
            return true;
        }
        if (resource.getSize() == 0) {
            return false;
        }

        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(resource.getInputStream()))) {
            String contents = FileUtils.safeReadFully(reader);
            String sub = substring;
            if (!casesensitive) {
                contents = contents.toLowerCase();
                sub = sub.toLowerCase();
            }
            return contents.contains(sub);
        } catch (IOException e) {
            throw new BuildException("There was a problem accessing resource : " + resource);
        }
    }
}
