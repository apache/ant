/*
 * Copyright  2002,2004 Apache Software Foundation
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

import java.util.Vector;
import org.apache.tools.ant.BuildException;

/**
 * An AntFileReader is a wrapper class that encloses the classname
 * and configuration of a Configurable FilterReader.
 *
 * @author Magesh Umasankar
 */
public final class AntFilterReader
    extends DataType
    implements Cloneable {

    private String className;

    private final Vector parameters = new Vector();

    private Path classpath;

    public final void setClassName(final String className) {
        this.className = className;
    }

    public final String getClassName() {
        return className;
    }

    public final void addParam(final Parameter param) {
        parameters.addElement(param);
    }

    /**
     * Set the classpath to load the FilterReader through (attribute).
     */
    public final void setClasspath(Path classpath) {
        if (isReference()) {
            throw tooManyAttributes();
        }
        if (this.classpath == null) {
            this.classpath = classpath;
        } else {
            this.classpath.append(classpath);
        }
    }

    /**
     * Set the classpath to load the FilterReader through (nested element).
     */
    public final Path createClasspath() {
        if (isReference()) {
            throw noChildrenAllowed();
        }
        if (this.classpath == null) {
            this.classpath = new Path(getProject());
        }
        return this.classpath.createPath();
    }

    /**
     * Get the classpath
     */
    public final Path getClasspath() {
        return classpath;
    }

    /**
     * Set the classpath to load the FilterReader through via
     * reference (attribute).
     */
    public void setClasspathRef(Reference r) {
        if (isReference()) {
            throw tooManyAttributes();
        }
        createClasspath().setRefid(r);
    }

    public final Parameter[] getParams() {
        Parameter[] params = new Parameter[parameters.size()];
        parameters.copyInto(params);
        return params;
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
        // change this to get the objects from the other reference
        Object o = r.getReferencedObject(getProject());
        if (o instanceof AntFilterReader) {
            AntFilterReader afr = (AntFilterReader) o;
            setClassName(afr.getClassName());
            setClasspath(afr.getClasspath());
            Parameter[] p = afr.getParams();
            if (p != null) {
                for (int i = 0; i < p.length; i++) {
                    addParam(p[i]);
                }
            }
        } else {
            String msg = r.getRefId() + " doesn\'t refer to a FilterReader";
            throw new BuildException(msg);
        }

        super.setRefid(r);
    }
}
