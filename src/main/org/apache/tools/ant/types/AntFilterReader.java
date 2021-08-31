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

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

/**
 * An AntFilterReader is a wrapper class that encloses the classname
 * and configuration of a Configurable FilterReader.
 */
public final class AntFilterReader extends DataType {

    private String className;

    private final List<Parameter> parameters = new ArrayList<>();

    private Path classpath;

    /**
     * Set the className attribute.
     *
     * @param className a <code>String</code> value
     */
    public void setClassName(final String className) {
        if (isReference()) {
            throw tooManyAttributes();
        }
        this.className = className;
    }

    /**
     * Get the className attribute.
     *
     * @return a <code>String</code> value
     */
    public String getClassName() {
        if (isReference()) {
            return getRef().getClassName();
        }
        dieOnCircularReference();
        return className;
    }

    /**
     * Add a Parameter.
     *
     * @param param a <code>Parameter</code> value
     */
    public void addParam(final Parameter param) {
        if (isReference()) {
            throw noChildrenAllowed();
        }
        parameters.add(param);
    }

    /**
     * Set the classpath to load the FilterReader through (attribute).
     * @param classpath a classpath
     */
    public void setClasspath(Path classpath) {
        if (isReference()) {
            throw tooManyAttributes();
        }
        if (this.classpath == null) {
            this.classpath = classpath;
        } else {
            this.classpath.append(classpath);
        }
        setChecked(false);
    }

    /**
     * Set the classpath to load the FilterReader through (nested element).
     * @return a classpath to be configured
     */
    public Path createClasspath() {
        if (isReference()) {
            throw noChildrenAllowed();
        }
        if (this.classpath == null) {
            this.classpath = new Path(getProject());
        }
        setChecked(false);
        return this.classpath.createPath();
    }

    /**
     * Get the classpath.
     * @return the classpath
     */
    public Path getClasspath() {
        if (isReference()) {
            getRef().getClasspath();
        }
        dieOnCircularReference();
        return classpath;
    }

    /**
     * Set the classpath to load the FilterReader through via
     * reference (attribute).
     * @param r a reference to a classpath
     */
    public void setClasspathRef(Reference r) {
        if (isReference()) {
            throw tooManyAttributes();
        }
        createClasspath().setRefid(r);
    }

    /**
     * The parameters for this filter.
     *
     * @return a <code>Parameter[]</code> value
     */
    public Parameter[] getParams() {
        if (isReference()) {
            getRef().getParams();
        }
        dieOnCircularReference();
        return parameters.toArray(new Parameter[0]);
    }

    /**
     * Makes this instance in effect a reference to another AntFilterReader
     * instance.
     *
     * <p>You must not set another attribute or nest elements inside
     * this element if you make it a reference.</p>
     *
     * @param r the reference to which this instance is associated
     * @exception BuildException if this instance already has been configured.
     */
    public void setRefid(Reference r) throws BuildException {
        if (!parameters.isEmpty() || className != null
                || classpath != null) {
            throw tooManyAttributes();
        }
        super.setRefid(r);
    }

    protected synchronized void dieOnCircularReference(Stack<Object> stk, Project p)
        throws BuildException {
        if (isChecked()) {
            return;
        }
        if (isReference()) {
            super.dieOnCircularReference(stk, p);
        } else {
            if (classpath != null) {
                pushAndInvokeCircularReferenceCheck(classpath, stk, p);
            }
            setChecked(true);
        }
    }

    private AntFilterReader getRef() {
        return getCheckedRef(AntFilterReader.class);
    }
}
