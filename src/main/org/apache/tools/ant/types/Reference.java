/*
 * Copyright  2000-2002,2004 The Apache Software Foundation
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

package org.apache.tools.ant.types;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

/**
 * Class to hold a reference to another object in the project.
 *
 * @author Stefan Bodewig
 */
public class Reference {

    private String refid;

    public Reference() {
        super();
    }

    public Reference(String id) {
        this();
        setRefId(id);
    }

    public void setRefId(String id) {
        refid = id;
    }

    public String getRefId() {
        return refid;
    }

    public Object getReferencedObject(Project project) throws BuildException {
        if (refid == null) {
            throw new BuildException("No reference specified");
        }

        Object o = project.getReference(refid);
        if (o == null) {
            throw new BuildException("Reference " + refid + " not found.");
        }
        return o;
    }
}
