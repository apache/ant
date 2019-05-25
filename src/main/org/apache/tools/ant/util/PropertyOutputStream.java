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

package org.apache.tools.ant.util;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.resources.PropertyResource;

/**
 * {@link OutputStream} that writes an Ant property.
 * @since Ant 1.7
 * @see PropertyResource#getOutputStream()
 */
public class PropertyOutputStream extends ByteArrayOutputStream {
    private Project project;
    private String property;
    private boolean trim;

    /**
     * Construct a new PropertyOutputStream for the specified Project
     * and property name, trimming the property value.
     * @param p the associated Ant Project.
     * @param s the String property name.
     */
    public PropertyOutputStream(Project p, String s) {
        this(p, s, true);
    }

    /**
     * Construct a new PropertyOutputStream for
     * the specified Project, property name, and trim mode.
     * @param p the associated Ant Project.
     * @param s the String property name.
     * @param b the boolean trim mode.
     */
    public PropertyOutputStream(Project p, String s, boolean b) {
        project = p;
        property = s;
        trim = b;
    }

    /**
     * Close the PropertyOutputStream, storing the property.
     */
    @Override
    public void close() {
        if (project != null && property != null) {
            String s = new String(toByteArray());
            project.setNewProperty(property, trim ? s.trim() : s);
        }
    }

}

