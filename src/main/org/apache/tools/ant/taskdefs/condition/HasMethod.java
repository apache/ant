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

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.ProjectComponent;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;

/**
 * test for a method
 */
public class HasMethod extends ProjectComponent implements Condition {
    private String classname;
    private String method;
    private String field;
    private Path classpath;
    private AntClassLoader loader;
    private boolean ignoreSystemClasses = false;

    /**
     * Set the classpath to be used when searching for classes and resources.
     *
     * @param classpath an Ant Path object containing the search path.
     */
    public void setClasspath(Path classpath) {
        createClasspath().append(classpath);
    }

    /**
     * Classpath to be used when searching for classes and resources.
     *
     * @return an empty Path instance to be configured by Ant.
     */
    public Path createClasspath() {
        if (this.classpath == null) {
            this.classpath = new Path(getProject());
        }
        return this.classpath.createPath();
    }

    /**
     * Set the classpath by reference.
     *
     * @param r a Reference to a Path instance to be used as the classpath
     *          value.
     */
    public void setClasspathRef(Reference r) {
        createClasspath().setRefid(r);
    }

    /**
     * Set the classname attribute.
     * @param classname the name of the class to check.
     */
    public void setClassname(String classname) {
        this.classname = classname;
    }

    /**
     * Set the name of the method.
     * @param method the name of the method to check.
     */
    public void setMethod(String method) {
        this.method = method;
    }

    /**
     * Set the name of the field.
     * @param field the name of the field to check.
     */
    public void setField(String field) {
        this.field = field;
    }

    /**
     * Set whether to ignore system classes when looking for the class.
     * @param ignoreSystemClasses a <code>boolean</code> value.
     */
    public void setIgnoreSystemClasses(boolean ignoreSystemClasses) {
        this.ignoreSystemClasses = ignoreSystemClasses;
    }

    /**
     * Check if a given class can be loaded.
     */
    private Class<?> loadClass(String classname) {
        try {
            if (ignoreSystemClasses) {
                loader = getProject().createClassLoader(classpath);
                loader.setParentFirst(false);
                loader.addJavaLibraries();
                try {
                    return loader.findClass(classname);
                } catch (SecurityException se) {
                    // class found but restricted name
                    throw new BuildException(
                        "class \"" + classname
                            + "\" was found but a SecurityException has been raised while loading it",
                        se);
                }
            }
            if (loader != null) {
                // How do we ever get here?
                return loader.loadClass(classname);
            }
            ClassLoader l = this.getClass().getClassLoader();
            // Can return null to represent the bootstrap class loader.
            // see API docs of Class.getClassLoader.
            if (l != null) {
                return Class.forName(classname, true, l);
            }
            return Class.forName(classname);
        } catch (ClassNotFoundException e) {
            throw new BuildException("class \"" + classname
                                     + "\" was not found");
        } catch (NoClassDefFoundError e) {
            throw new BuildException("Could not load dependent class \""
                                     + e.getMessage()
                                     + "\" for class \"" + classname + "\"");
        }
    }

    /** {@inheritDoc}. */
    @Override
    public boolean eval() throws BuildException {
        if (classname == null) {
            throw new BuildException("No classname defined");
        }
        ClassLoader preLoadClass = loader;
        try {
            Class<?> clazz = loadClass(classname);
            if (method != null) {
                return isMethodFound(clazz);
            }
            if (field != null) {
                return isFieldFound(clazz);
            }
            throw new BuildException("Neither method nor field defined");
        } finally {
            if (preLoadClass != loader && loader != null) {
                loader.cleanup();
                loader = null;
            }
        }
    }

    private boolean isFieldFound(Class<?> clazz) {
        for (Field fieldEntry : clazz.getDeclaredFields()) {
            if (fieldEntry.getName().equals(field)) {
                return true;
            }
        }
        return false;
    }

    private boolean isMethodFound(Class<?> clazz) {
        for (Method methodEntry : clazz.getDeclaredMethods()) {
            if (methodEntry.getName().equals(method)) {
                return true;
            }
        }
        return false;
    }

}
