/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2003 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "Ant" and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */

package org.apache.tools.ant;


/**
 * This class contains all the information
 * on a particular ant type,
 * the classname, adaptor and the class
 * it should be assignable from.
 * This type replaces the task/datatype split
 * of pre ant 1.6.
 *
 * @author Peter Reilly
 */
public class AntTypeDefinition {
    private String      name;
    private Class       clazz;
    private Class       adapterClass;
    private Class       adaptToClass;
    private String      className;
    private ClassLoader classLoader;

    /**
     * set the definition's name
     * @param name the name of the definition
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * return the definition's name
     * @return the name of the defintion
     */
    public String getName() {
        return name;
    }

    /**
     * set the class of the definition.
     * as a side-effect may set the classloader and classname
     * @param clazz the class of this definition
     */
    public void setClass(Class clazz) {
        this.clazz = clazz;
        if (clazz == null) {
            return;
        }
        if (classLoader == null) {
            this.classLoader = clazz.getClassLoader();
        }
        if (className == null) {
            this.className = clazz.getName();
        }
    }

    /**
     * set the classname of the definition
     * @param className the classname of this definition
     */
    public void setClassName(String className) {
        this.className = className;
    }

    /**
     * get the classname of the definition
     * @return the name of the class of this definition
     */
    public String getClassName() {
        return className;
    }

    /**
     * set the adapter class for this definition.
     * this class is used to adapt the definitions class if
     * required.
     * @param adapterClass the adapterClass
     */
    public void setAdapterClass(Class adapterClass) {
        this.adapterClass = adapterClass;
    }

    /**
     * set the assignable class for this definition.
     * @param adaptToClass the assignable class
     */

    public void setAdaptToClass(Class adaptToClass) {
        this.adaptToClass = adaptToClass;
    }

    /**
     * set the classloader to use to create an instance
     * of the definition
     * @param classLoader the classLoader
     */
    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    /**
     * get the classloader for this definition
     * @return the classloader for this definition
     */
    public ClassLoader getClassLoader() {
        return classLoader;
    }

    /**
     * get the exposed class for this
     * definition. This will be a proxy class
     * (adapted class) if there is an adpater
     * class and the definition class is not
     * assignable from the assignable class.
     * @param project the current project
     * @return the exposed class
     */
    public Class getExposedClass(Project project) {
        if (adaptToClass != null) {
            Class z = getTypeClass(project);
            if (z == null) {
                return null;
            }
            if (adaptToClass.isAssignableFrom(z)) {
                return z;
            }
        }
        if (adapterClass != null) {
            return adapterClass;
        }
        return getTypeClass(project);
    }

    /**
     * get the definition class
     * @param project the current project
     * @return the type of the definition
     */
    public Class getTypeClass(Project project) {
        if (clazz != null) {
            return clazz;
        }

        try {
            if (classLoader == null) {
                clazz = Class.forName(className);
            } else {
                clazz = classLoader.loadClass(className);
            }
        } catch (NoClassDefFoundError ncdfe) {
            project.log("Could not load a dependent class ("
                        + ncdfe.getMessage() + ") for type "
                        + name, Project.MSG_DEBUG);
        } catch (ClassNotFoundException cnfe) {
            project.log("Could not load class (" + className
                        + ") for type " + name, Project.MSG_DEBUG);
        }
        return clazz;
    }

    /**
     * create an instance of the definition.
     * The instance may be wrapped in a proxy class.
     * @param project the current project
     * @return the created object
     */
    public Object create(Project project) {
        return icreate(project);
    }

    /**
     * Create a component object based on
     * its definition
     */
    private Object icreate(Project project) {
        Class c = getTypeClass(project);
        if (c == null) {
            return null;
        }

        Object o = createAndSet(project, c);
        if (o == null || adapterClass == null) {
            return o;
        }

        if (adaptToClass != null) {
            if (adaptToClass.isAssignableFrom(o.getClass())) {
                return o;
            }
        }

        TypeAdapter adapterObject = (TypeAdapter) createAndSet(
            project, adapterClass);
        if (adapterObject == null) {
            return null;
        }

        adapterObject.setProxy(o);
        return adapterObject;
    }

    /**
     * check if the attributes are correct
     * <dl>
     *   <li>if the class can be created.</li>
     *   <li>if an adapter class can be created</li>
     *   <li>if the type is assignable from adapto</li>
     *   <li>if the type can be used with the adapter class</li>
     * </dl>
     * @param project the current project
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
        if (adapterClass != null) {
            boolean needToCheck = true;
            if (adaptToClass != null
                && adaptToClass.isAssignableFrom(clazz)) {
                needToCheck = false;
            }
            if (needToCheck) {
                TypeAdapter adapter = (TypeAdapter) createAndSet(
                    project, adapterClass);
                if (adapter == null) {
                    throw new BuildException("Unable to create adapter object");
                }
                adapter.checkProxyClass(clazz);
            }
        }
    }

    /**
     * get the constructor of the defintion
     * and invoke it.
     */
    private Object createAndSet(Project project, Class c) {
        try {
            java.lang.reflect.Constructor ctor = null;
            boolean noArg = false;
            // DataType can have a "no arg" constructor or take a single
            // Project argument.
            try {
                ctor = c.getConstructor(new Class[0]);
                noArg = true;
            } catch (NoSuchMethodException nse) {
                ctor = c.getConstructor(new Class[] {Project.class});
                noArg = false;
            }

            Object o = null;
            if (noArg) {
                o = ctor.newInstance(new Object[0]);
            } else {
                o = ctor.newInstance(new Object[] {project});
            }
            project.setProjectReference(o);
            return o;
        } catch (java.lang.reflect.InvocationTargetException ex) {
            Throwable t = ex.getTargetException();
            throw new BuildException(
                "Could not create type " + name + " due to " + t, t);
        } catch (NoClassDefFoundError ncdfe) {
            String msg = "Type " + name + ": A class needed by class "
                + c + " cannot be found: " + ncdfe.getMessage();
            throw new BuildException(msg, ncdfe);
       } catch (Throwable t) {
            throw new BuildException(
                "Could not create type " + name + " due to " + t, t);
        }
    }

    /**
     * Equality method for this definition (assumes the names are the same)
     *
     * @param other another definition
     * @param project the project the definition
     * @return true if the definitions are the same
     */
    public boolean sameDefinition(AntTypeDefinition other, Project project) {
        if (other == null) {
            return false;
        }
        if (other.getClass() != this.getClass()) {
            return false;
        }
        if (!(other.getTypeClass(project).equals(getTypeClass(project)))) {
            return false;
        }
        if (!other.getExposedClass(project).equals(getExposedClass(project))) {
            return false;
        }
        if (other.adapterClass != adapterClass) {
            return false;
        }
        if (other.adaptToClass != adaptToClass) {
            return false;
        }
        return true;
    }

    /**
     * Similar definition
     * used to compare two definitions defined twice with the same
     * name and the same types.
     * the classloader may be different but have the same
     * path so #sameDefinition cannot
     * be used.
     * @param other the definition to compare to
     * @param project the current project
     * @return true if the definitions are the same
     */
    public boolean similarDefinition(AntTypeDefinition other, Project project) {
        if (other == null) {
            return false;
        }
        if (getClass() != other.getClass()) {
            return false;
        }
        if (!getClassName().equals(other.getClassName())) {
            return false;
        }
        if (!extractClassname(adapterClass).equals(
                extractClassname(other.adapterClass))) {
            return false;
        }
        if (!extractClassname(adaptToClass).equals(
                extractClassname(other.adaptToClass))) {
            return false;
        }
        // all the names are the same: check if the class path of the loader
        // is the same
        ClassLoader oldLoader = other.getClassLoader();
        ClassLoader newLoader = this.getClassLoader();
        if (oldLoader != null
            && newLoader != null
            && oldLoader instanceof AntClassLoader
            && newLoader instanceof AntClassLoader
            && ((AntClassLoader) oldLoader).getClasspath()
            .equals(((AntClassLoader) newLoader).getClasspath())
            ) {
            return true;
        } else {
            return false;
        }
    }

    private String extractClassname(Class c) {
        if (c == null) {
            return "<null>";
        } else {
            return c.getClass().getName();
        }
    }
}
