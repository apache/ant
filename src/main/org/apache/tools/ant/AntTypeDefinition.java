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

package org.apache.tools.ant;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;


/**
 * This class contains all the information
 * on a particular ant type,
 * the classname, adapter and the class
 * it should be assignable from.
 * This type replaces the task/datatype split
 * of pre ant 1.6.
 *
 */
public class AntTypeDefinition {
    private String      name;
    private Class<?>       clazz;
    private Class<?>       adapterClass;
    private Class<?>       adaptToClass;
    private String      className;
    private ClassLoader classLoader;
    private boolean     restrict = false;

    /**
     * Set the restrict attribute.
     * @param restrict the value to set.
     */
    public void setRestrict(boolean restrict) {
         this.restrict = restrict;
    }

    /**
     * Get the restrict attribute.
     * @return the restrict attribute.
     */
    public boolean isRestrict() {
        return restrict;
    }

    /**
     * Set the definition's name.
     * @param name the name of the definition.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Return the definition's name.
     * @return the name of the definition.
     */
    public String getName() {
        return name;
    }

    /**
     * Set the class of the definition.
     * As a side-effect may set the classloader and classname.
     * @param clazz the class of this definition.
     */
    public void setClass(Class<?> clazz) {
        this.clazz = clazz;
        if (clazz == null) {
            return;
        }
        this.classLoader = (classLoader == null)
            ? clazz.getClassLoader() : classLoader;
        this.className = (className == null) ? clazz.getName() : className;
    }

    /**
     * Set the classname of the definition.
     * @param className the classname of this definition.
     */
    public void setClassName(String className) {
        this.className = className;
    }

    /**
     * Get the classname of the definition.
     * @return the name of the class of this definition.
     */
    public String getClassName() {
        return className;
    }

    /**
     * Set the adapter class for this definition.
     * This class is used to adapt the definitions class if
     * required.
     * @param adapterClass the adapterClass.
     */
    public void setAdapterClass(Class<?> adapterClass) {
        this.adapterClass = adapterClass;
    }

    /**
     * Set the assignable class for this definition.
     * @param adaptToClass the assignable class.
     */

    public void setAdaptToClass(Class<?> adaptToClass) {
        this.adaptToClass = adaptToClass;
    }

    /**
     * Set the classloader to use to create an instance
     * of the definition.
     * @param classLoader the ClassLoader.
     */
    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    /**
     * Get the classloader for this definition.
     * @return the classloader for this definition.
     */
    public ClassLoader getClassLoader() {
        return classLoader;
    }

    /**
     * Get the exposed class for this
     * definition. This will be a proxy class
     * (adapted class) if there is an adapter
     * class and the definition class is not
     * assignable from the assignable class.
     * @param project the current project.
     * @return the exposed class - may return null if unable to load the class
     */
    public Class<?> getExposedClass(Project project) {
        if (adaptToClass != null) {
            Class<?> z = getTypeClass(project);
            if (z == null || adaptToClass.isAssignableFrom(z)) {
                return z;
            }
        }
        return (adapterClass == null) ? getTypeClass(project) :  adapterClass;
    }

    /**
     * Get the definition class.
     * @param project the current project.
     * @return the type of the definition.
     */
    public Class<?> getTypeClass(Project project) {
        try {
            return innerGetTypeClass();
        } catch (NoClassDefFoundError ncdfe) {
            project.log("Could not load a dependent class ("
                        + ncdfe.getMessage() + ") for type "
                        + name, Project.MSG_DEBUG);
        } catch (ClassNotFoundException cnfe) {
            project.log("Could not load class (" + className
                        + ") for type " + name, Project.MSG_DEBUG);
        }
        return null;
    }

    /**
     * Try and load a class, with no attempt to catch any fault.
     * @return the class that implements this component
     * @throws ClassNotFoundException if the class cannot be found.
     * @throws NoClassDefFoundError   if the there is an error
     *                                finding the class.
     */
    public Class<?> innerGetTypeClass() throws ClassNotFoundException {
        if (clazz != null) {
            return clazz;
        }
        if (classLoader == null) {
            clazz = Class.forName(className);
        } else {
            clazz = classLoader.loadClass(className);
        }
        return clazz;
    }

    /**
     * Create an instance of the definition.
     * The instance may be wrapped in a proxy class.
     * @param project the current project.
     * @return the created object.
     */
    public Object create(Project project) {
        return icreate(project);
    }

    /**
     * Create a component object based on
     * its definition.
     * @return the component as an <code>Object</code>.
     */
    private Object icreate(Project project) {
        Class<?> c = getTypeClass(project);
        if (c == null) {
            return null;
        }
        Object o = createAndSet(project, c);
        if (adapterClass == null
            || (adaptToClass != null && adaptToClass.isAssignableFrom(o.getClass()))) {
            return o;
        }
        TypeAdapter adapterObject = (TypeAdapter) createAndSet(
            project, adapterClass);
        adapterObject.setProxy(o);
        return adapterObject;
    }

    /**
     * Checks if the attributes are correct.
     * <ul>
     *   <li>if the class can be created.</li>
     *   <li>if an adapter class can be created</li>
     *   <li>if the type is assignable from adapter</li>
     *   <li>if the type can be used with the adapter class</li>
     * </ul>
     * @param project the current project.
     */
    public void checkClass(Project project) {
        if (clazz == null) {
            clazz = getTypeClass(project);
            if (clazz == null) {
                throw new BuildException(
                    "Unable to create class for " + getName());
            }
        }
        // check adapter
        if (adapterClass != null && (adaptToClass == null
            || !adaptToClass.isAssignableFrom(clazz))) {
            TypeAdapter adapter = (TypeAdapter) createAndSet(
                project, adapterClass);
            adapter.checkProxyClass(clazz);
        }
    }

    /**
     * Get the constructor of the definition
     * and invoke it.
     * @return the instantiated <code>Object</code>, will never be null.
     */
    private Object createAndSet(Project project, Class<?> c) {
        try {
            return innerCreateAndSet(c, project);
        } catch (InvocationTargetException ex) {
            Throwable t = ex.getTargetException();
            throw new BuildException(
                "Could not create type " + name + " due to " + t, t);
        } catch (NoClassDefFoundError ncdfe) {
            String msg = "Type " + name + ": A class needed by class "
                + c + " cannot be found: " + ncdfe.getMessage();
            throw new BuildException(msg, ncdfe);
        } catch (NoSuchMethodException nsme) {
            throw new BuildException("Could not create type " + name
                    + " as the class " + c + " has no compatible constructor");
        } catch (InstantiationException nsme) {
            throw new BuildException("Could not create type "
                    + name + " as the class " + c + " is abstract");
        } catch (IllegalAccessException e) {
            throw new BuildException("Could not create type "
                    + name + " as the constructor " + c + " is not accessible");
        } catch (Throwable t) {
            throw new BuildException(
                "Could not create type " + name + " due to " + t, t);
        }
    }

    /**
     * Inner implementation of the {@link #createAndSet(Project, Class)} logic, with no
     * exception catching.
     * @param <T> return type of the method
     * @param newclass class to create
     * @param project the project to use
     * @return a newly constructed and bound instance.
     * @throws NoSuchMethodException  no good constructor.
     * @throws InstantiationException cannot initialize the object.
     * @throws IllegalAccessException cannot access the object.
     * @throws InvocationTargetException error in invocation.
     */
    public <T> T innerCreateAndSet(Class<T> newclass, Project project)
            throws NoSuchMethodException,
            InstantiationException,
            IllegalAccessException,
            InvocationTargetException {
        Constructor<T> ctor;
        boolean noArg = false;
        // DataType can have a "no arg" constructor or take a single
        // Project argument.
        try {
            ctor = newclass.getConstructor();
            noArg = true;
        } catch (NoSuchMethodException nse) {
            //can throw the same exception, if there is no this(Project) ctor.
            ctor = newclass.getConstructor(Project.class);
            noArg = false;
        }
        //now we instantiate
        T o = ctor.newInstance(
            ((noArg) ? new Object[0] : new Object[] {project}));

        //set up project references.
        project.setProjectReference(o);
        return o;
    }

    /**
     * Equality method for this definition (assumes the names are the same).
     *
     * @param other another definition.
     * @param project the project the definition.
     * @return true if the definitions are the same.
     */
    public boolean sameDefinition(AntTypeDefinition other, Project project) {
        return (other != null && other.getClass() == getClass()
            && other.getTypeClass(project).equals(getTypeClass(project))
            && other.getExposedClass(project).equals(getExposedClass(project))
            && other.restrict == restrict
            && other.adapterClass == adapterClass
            && other.adaptToClass == adaptToClass);
    }

    /**
     * Similar definition;
     * used to compare two definitions defined twice with the same
     * name and the same types.
     * The classloader may be different but have the same
     * path so #sameDefinition cannot
     * be used.
     * @param other the definition to compare to.
     * @param project the current project.
     * @return true if the definitions are the same.
     */
    public boolean similarDefinition(AntTypeDefinition other, Project project) {
        if (other == null
            || getClass() != other.getClass()
            || !getClassName().equals(other.getClassName())
            || !extractClassname(adapterClass).equals(
            extractClassname(other.adapterClass))
            || !extractClassname(adaptToClass).equals(
            extractClassname(other.adaptToClass))
            || restrict != other.restrict) {
            return false;
        }
        // all the names are the same: check if the class path of the loader
        // is the same
        ClassLoader oldLoader = other.getClassLoader();
        ClassLoader newLoader = getClassLoader();
        return oldLoader == newLoader
            || (oldLoader instanceof AntClassLoader
            && newLoader instanceof AntClassLoader
            && ((AntClassLoader) oldLoader).getClasspath()
            .equals(((AntClassLoader) newLoader).getClasspath()));
    }

    private String extractClassname(Class<?> c) {
        return (c == null) ? "<null>" : c.getName();
    }
}
