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

package org.apache.tools.ant.types.selectors;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Parameter;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;

/**
 * Selector that selects files by forwarding the request on to other classes.
 *
 * @since 1.5
 */
public class ExtendSelector extends BaseSelector {

    private String classname = null;
    private FileSelector dynselector = null;
    private List<Parameter> parameters =
        Collections.synchronizedList(new ArrayList<>());
    private Path classpath = null;

    /**
     * Sets the classname of the custom selector.
     *
     * @param classname is the class which implements this selector
     */
    public void setClassname(String classname) {
        this.classname = classname;
    }

    /**
     * Instantiates the identified custom selector class.
     */
    public void selectorCreate() {
        if (classname != null && !classname.isEmpty()) {
            try {
                Class<?> c;
                if (classpath == null) {
                    c = Class.forName(classname);
                } else {
                    // Memory-Leak in line below
                    AntClassLoader al
                            = getProject().createClassLoader(classpath);
                    c = Class.forName(classname, true, al);
                }
                dynselector = c.asSubclass(FileSelector.class).getDeclaredConstructor().newInstance();
                final Project p = getProject();
                if (p != null) {
                    p.setProjectReference(dynselector);
                }
            } catch (ClassNotFoundException cnfexcept) {
                setError("Selector " + classname
                    + " not initialized, no such class");
            } catch (InstantiationException | NoSuchMethodException
                    | InvocationTargetException iexcept) {
                setError("Selector " + classname
                    + " not initialized, could not create class");
            } catch (IllegalAccessException iaexcept) {
                setError("Selector " + classname
                    + " not initialized, class not accessible");
            }
        } else {
            setError("There is no classname specified");
        }
    }

    /**
     * Create new parameters to pass to custom selector.
     *
     * @param p The new Parameter object
     */
    public void addParam(Parameter p) {
        parameters.add(p);
    }

    /**
     * Set the classpath to load the classname specified using an attribute.
     * @param classpath the classpath to use
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
     * Specify the classpath to use to load the Selector (nested element).
     * @return a classpath to be configured
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
     * @return the classpath
     */
    public final Path getClasspath() {
        return classpath;
    }

    /**
     * Set the classpath to use for loading a custom selector by using
     * a reference.
     * @param r a reference to the classpath
     */
    public void setClasspathref(Reference r) {
        if (isReference()) {
            throw tooManyAttributes();
        }
        createClasspath().setRefid(r);
    }

    /**
     * These are errors specific to ExtendSelector only. If there are
     * errors in the custom selector, it should throw a BuildException
     * when isSelected() is called.
     */
    public void verifySettings() {
        // Creation is done here rather than in isSelected() because some
        // containers may do a validation pass before running isSelected(),
        // but we need to check for the existence of the created class.
        if (dynselector == null) {
            selectorCreate();
        }
        if (classname == null || classname.length() < 1) {
            setError("The classname attribute is required");
        } else if (dynselector == null) {
            setError("Internal Error: The custom selector was not created");
        } else if (!(dynselector instanceof ExtendFileSelector)
                    && !parameters.isEmpty()) {
            setError(
                "Cannot set parameters on custom selector that does not implement ExtendFileSelector");
        }
    }

    /**
     * Allows the custom selector to choose whether to select a file. This
     * is also where the Parameters are passed to the custom selector,
     * since we know we must have them all by now. And since we must know
     * both classpath and classname, creating the class is deferred to here
     * as well.
     * @param basedir The the base directory.
     * @param filename The name of the file to check.
     * @param file A File object for this filename.
     * @return whether the file should be selected or not.
     * @exception BuildException if an error occurs.
     */
    public boolean isSelected(File basedir, String filename, File file)
            throws BuildException {
        validate();
        if (!parameters.isEmpty() && dynselector instanceof ExtendFileSelector) {
            // We know that dynselector must be non-null if no error message
            ((ExtendFileSelector) dynselector).setParameters(
                parameters.toArray(new Parameter[0]));
        }
        return dynselector.isSelected(basedir, filename, file);
    }

}
