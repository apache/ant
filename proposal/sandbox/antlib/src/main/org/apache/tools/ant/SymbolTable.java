/*
 *  The Apache Software License, Version 1.1
 *
 *  Copyright (c) 1999 The Apache Software Foundation.  All rights
 *  reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions
 *  are met:
 *
 *  1. Redistributions of source code must retain the above copyright
 *  notice, this list of conditions and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright
 *  notice, this list of conditions and the following disclaimer in
 *  the documentation and/or other materials provided with the
 *  distribution.
 *
 *  3. The end-user documentation included with the redistribution, if
 *  any, must include the following acknowlegement:
 *  "This product includes software developed by the
 *  Apache Software Foundation (http://www.apache.org/)."
 *  Alternately, this acknowlegement may appear in the software itself,
 *  if and wherever such third-party acknowlegements normally appear.
 *
 *  4. The names "The Jakarta Project", "Ant", and "Apache Software
 *  Foundation" must not be used to endorse or promote products derived
 *  from this software without prior written permission. For written
 *  permission, please contact apache@apache.org.
 *
 *  5. Products derived from this software may not be called "Apache"
 *  nor may "Apache" appear in their names without prior written
 *  permission of the Apache Group.
 *
 *  THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 *  WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 *  OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 *  ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 *  USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 *  OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 *  OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 *  SUCH DAMAGE.
 *  ====================================================================
 *
 *  This software consists of voluntary contributions made by many
 *  individuals on behalf of the Apache Software Foundation.  For more
 *  information on the Apache Software Foundation, please see
 *  <http://www.apache.org/>.
 */
package org.apache.tools.ant;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

import org.apache.tools.ant.types.Path;

public class SymbolTable {

    /** Parent symbol table */
    private SymbolTable parentTable;

    /** Project associated with this symbol table */
    private Project project;

    /** The table of roles available to this Project */
    private Hashtable roles = new Hashtable();

    /** The table of loaders active on this Project */
    private Hashtable loaders = new Hashtable();

    /**
     * Table of per role definitions.
     */
    private Hashtable defs = new Hashtable();

    /**
     * Parameters for checking adapters.
     */
    private static final Class[] CHECK_ADAPTER_PARAMS =
        new Class[]{Class.class, Project.class};

    /**
     * Create a top level Symbol table.
     */
    public SymbolTable() {
    }

    /**
     * Create a symbol table inheriting the definitions
     * from that defined in the calling Project.
     * @param st the calling project
     */
    public SymbolTable(SymbolTable st) {
        parentTable = st;
    }

    /**
     * Set the project associated with this symbol table.
     * @param p the project for this symbol table
     */
    public void setProject(Project p) {
        this.project = p;
    }

    /**
     * Get the specified loader for the project.
     * @param name the name of the loader
     * @return the corresponding ANT classloader
     */
    private AntClassLoader getLoader(String name) {
        AntClassLoader cl = (AntClassLoader) loaders.get(name);
        if (cl == null && parentTable != null) {
            return parentTable.getLoader(name);
        }
        return cl;
    }

    /**
     * Add the specified class-path to a loader.
     * If the loader is defined in an ancestor project then a new
     * classloader inheritin from the one already existing
     * will be created, otherwise the path willbe added to the existing
     * ClassLoader.
     * @param name the name of the loader to use.
     * @param clspath the path to be added to the classloader
     */
    public ClassLoader addToLoader(String name, Path clspath) {
        // Find if the loader is already defined in the current project
        AntClassLoader cl = (AntClassLoader) loaders.get(name);
        if (cl == null) {
            // Is it inherited from the calling project
            if (parentTable != null) {
                cl = parentTable.getLoader(name);
            }
            cl = new AntClassLoader(cl, project, clspath, true);
            loaders.put(name, cl);
        }
        else {
            // Add additional path to the existing definition
            String[] pathElements = clspath.list();
            for (int i = 0; i < pathElements.length; ++i) {
                try {
                    cl.addPathElement(pathElements[i]);
                }
                catch (BuildException e) {
                    // ignore path elements invalid relative to the project
                }
            }
        }
        return cl;
    }

    /**
     * Find all the roles supported by a Class
     * on this symbol table.
     * @param clz the class to analyze
     * @return an array of roles supported by the class
     */
    public String[] findRoles(final Class clz) {
        Vector list = new Vector();
        findRoles(clz, list);
        return (String[])list.toArray(new String[list.size()]);
    }

    /**
     * Collect the roles for the class
     * @param clz the class being inspected
     * @param list the roles collected up to this point
     */
    private void findRoles(final Class clz, Vector list) {
        for (Enumeration e = roles.keys(); e.hasMoreElements();) {
            String role = (String) e.nextElement();

            if (((Role) roles.get(role)).isImplementedBy(clz)) {
                list.addElement(role);
            }
        }
        if (parentTable != null) parentTable.findRoles(clz, list);
    }

    /**
     * Get the Role definition
     * @param role the name of the role
     * @return the Role description
     */
    public Role getRole(String role) {
        Role r = (Role) roles.get(role);
        if (r == null && parentTable != null) {
            return parentTable.getRole(role);
        }
        return r;
    }

    /**
     * Add a new role definition to this project.
     * @param role the name of the role
     * @param rclz the interface used to specify support for the role.
     * @param aclz the optional adapter class
     * @return whether the role replaced a different definition
     */
    public boolean addRole(String role, Class rclz, Class aclz) {
        // Check if role already declared
        Role old = getRole(role);
        if (old != null && old.isSameAsFor(rclz, aclz)) {
            project.log("Ignoring override for role " + role
                        + ", it is already defined by the same definition.",
                        project.MSG_VERBOSE);
            return false;
        }
        // Role interfaces should only contain one method
        roles.put(role, new Role(rclz, aclz));
        return (old != null);
    }

    /**
     * Add a new type of element to a role.
     * @param role the role for this Class.
     * @param name the name of the element for this Class
     * @param clz the Class being declared
     * @return the old definition
     */
    public Class add(String role, String name, Class clz) {
        // Find the role definition
        Role r = getRole(role);
        if (r == null) {
            throw new BuildException("Unknown role: " + role);
        }
        // Check if it is already defined
        Factory old = get(role, name);
        if (old != null) {
            if (old.getOriginalClass().equals(clz)) {
                project.log("Ignoring override for "+ role + " " + name
                            + ", it is already defined by the same class.",
                            project.MSG_VERBOSE);
                return old.getOriginalClass();
            }
            else {
                project.log("Trying to override old definition of " +
                            role + " " + name,
                            project.MSG_WARN);
            }
        }
        Factory f = checkClass(clz);
        // Check that the Class is compatible with the role definition
        f = r.verifyAdaptability(role, f);
        // Record the new type
        Hashtable defTable = (Hashtable)defs.get(role);
        if (defTable == null) {
            defTable = new Hashtable();
            defs.put(role, defTable);
        }
        defTable.put(name, f);

        String msg =
            " +User " + role + ": " + name + "     " + clz.getName();
        project.log(msg, project.MSG_DEBUG);
        return (old != null ? old.getOriginalClass() : null);
    }

    /**
     * Checks a class, whether it is suitable for serving in ANT.
     * @return the factory to use when instantiating the class
     * @throws BuildException and logs as Project.MSG_ERR for
     * conditions, that will cause execution to fail.
     */
    Factory checkClass(final Class clz) // Package on purpose
        throws BuildException {
        if (clz == null) return null;

        if(!Modifier.isPublic(clz.getModifiers())) {
            final String message = clz + " is not public";
            project.log(message, Project.MSG_ERR);
            throw new BuildException(message);
        }
        if(Modifier.isAbstract(clz.getModifiers())) {
            final String message = clz + " is abstract";
            project.log(message, Project.MSG_ERR);
            throw new BuildException(message);
        }
        try {
            // Class can have a "no arg" constructor or take a single
            // Project argument.
            // don't have to check for public, since
            // getConstructor finds public constructors only.
            try {
                clz.getConstructor(new Class[0]);
                return new Factory(){
                        public Object create(Project p) {
                            try {
                                return clz.newInstance();
                            }
                            catch(Exception e) {
                                throw new BuildException(e);
                            }
                        }

                        public Class getOriginalClass() {
                            return clz;
                        }
                    };
            } catch (NoSuchMethodException nse) {
                final Constructor c =
                    clz.getConstructor(new Class[] {Project.class});
                return new Factory(){
                        public Object create(Project p) {
                            try {
                                return c.newInstance(new Object[]{p});
                            }
                            catch(Exception e) {
                                throw new BuildException(e);
                            }
                        }

                        public Class getOriginalClass() {
                            return clz;
                        }
                    };
            }
        } catch(NoSuchMethodException e) {
            final String message = "No valid public constructor in " + clz;
            project.log(message, Project.MSG_ERR);
            throw new BuildException(message);
        }
        catch (NoClassDefFoundError ncdfe) {
            final String msg = "Class cannot be loaded: " + ncdfe.getMessage();
            throw new BuildException(msg, ncdfe);
        }
    }

    /**
     * Get the class in the role identified with the element name.
     * @param role the role to look into.
     * @param name the name of the element to sea
     * @return the Class implementation
     */
    public Factory get(String role, String name) {
        Hashtable defTable = (Hashtable)defs.get(role);
        if (defTable != null) {
            Factory f = (Factory)defTable.get(name);
            if (f != null) return f;
        }
        if (parentTable != null) {
            return parentTable.get(role, name);
        }
        return null;
    }

    /**
     * Get a Hashtable that is usable for manipulating elements on Role.
     * @param role the role of the elements in the table
     * @return a Hashtable that delegates to the Symbol table.
     */
    Hashtable getDefinitions(String role) { // package scope on purpose
        return new SymbolHashtable(role);
    }

    /**
     * Hashtable implementation that delegates
     * the search operations to the Symbol table
     */
    private class SymbolHashtable extends Hashtable {
        final String role;
        SymbolHashtable(String role) {
            this.role = role;
        }

        public synchronized Object put(Object key, Object value) {
            return SymbolTable.this.add(role, (String) key, (Class) value);
        }

        public synchronized Object get(Object key) {
            Factory f = SymbolTable.this.get(role, (String)key);
            return (f == null? null : f.getOriginalClass());
        }
    }

    /**
     * Factory for creating ANT objects.
     * Class objects are not instanciated directly but through a Factory
     * which is able to resolve issues such as proxys and such.
     */
    public static interface Factory {
        /**
         * Creates an object for the Role
         * @param p the project in which it is created
         * @return the instantiated object with a proxy if necessary
         */
        public Object create(Project p);

        /**
         * Creates an object for the Role, adapted if necessary
         * for a particular interface.
         */
//  	public Object adaptFor(Class clz, Project p, Object o);

        /**
         * The original class of the object without proxy.
         */
        public Class getOriginalClass();
    }

    /**
     * The definition of a role
     */
    public class Role {
        private Method interfaceMethod;
        private Method adapterVerifier;
        private Factory adapterFactory;

        /**
         * Creates a new Role object
         * @param roleClz the class that defines the role
         * @param adapterClz the class for the adapter, or null if none
         */
        Role(Class roleClz, Class adapterClz) {
            interfaceMethod = validInterface(roleClz);
            adapterFactory = checkClass(adapterClz);
            adapterVerifier = validAdapter(adapterClz, interfaceMethod);
        }

        /**
         * Get the method used to set on interface
         */
        public Method getInterfaceMethod() {
            return interfaceMethod;
        }

        /**
         * Instantiate a new adapter for this role.
         */
        public RoleAdapter createAdapter(Project p) {
            if (adapterFactory == null) return null;

            try {
                return (RoleAdapter) adapterFactory.create(p);
            }
            catch(BuildException be) {
                throw be;
            }
            catch(Exception e) {
                throw new BuildException(e);
            }
        }

        /**
         * Verify if the class can be adapted to use by the role
         * @param role the name of the role to verify
         * @param f the factory for the class to verify
         */
        public Factory verifyAdaptability(String role, final Factory f) {
            final Class clz = f.getOriginalClass();
            if (interfaceMethod.getParameterTypes()[0].isAssignableFrom(clz)) {
                return f;
            }
            if (adapterVerifier == null) {
                String msg = "Class " + clz.getName() +
                    " incompatible with role: " + role;
                throw new BuildException(msg);
            }
            try {
                try {
                    adapterVerifier.invoke(null, new Object[]{clz, project});
                    return new Factory(){
                            public Object create(Project p) {
                                RoleAdapter ra = createAdapter(p);
                                ra.setProxy(f.create(p));
                                return ra;
                            }

                            public Class getOriginalClass() {
                                return clz;
                            }
                        };
                }
                catch (InvocationTargetException ite) {
                    throw ite.getTargetException();
                }
            }
            catch(BuildException be) { throw be; }
            catch(Error err) {throw err; }
            catch(Throwable t) {
                throw new BuildException(t);
            }
        }

        public boolean isSameAsFor(Class clz, Class pclz) {
            return interfaceMethod.getDeclaringClass().equals(clz) &&
                ((adapterVerifier == null && pclz == null) ||
                 adapterVerifier.getDeclaringClass().equals(pclz));
        }

        public boolean isImplementedBy(Class clz) {
            return interfaceMethod.getDeclaringClass().isAssignableFrom(clz);
        }

        /**
         * Verify if the interface is valid.
         * @param clz the interface to validate
         * @return the method defined by the interface
         */
        private Method validInterface(Class clz) {
            Method m[] = clz.getDeclaredMethods();
            if (m.length == 1
                && java.lang.Void.TYPE.equals(m[0].getReturnType())) {
                Class args[] = m[0].getParameterTypes();
                if (args.length == 1
                    && !java.lang.String.class.equals(args[0])
                    && !args[0].isArray()
                    && !args[0].isPrimitive()) {
                    return m[0];
                }
                else {
                    throw new BuildException("Invalid role interface method in: "
                                             + clz.getName());
                }
            }
            else {
                throw new BuildException("More than one method on role interface");
            }
        }

        /**
         * Verify if the adapter is valid with respect to the interface.
         * @param clz the class adapter to validate
         * @param mtd the method whose only argument must match
         * @return the static method to use for validating adaptees
         */
        private Method validAdapter(Class clz, Method mtd) {
            if (clz == null) return null;

            if (!mtd.getParameterTypes()[0].isAssignableFrom(clz)) {
                String msg = "Adapter " + clz.getName() +
                    " is incompatible with role interface " +
                    mtd.getDeclaringClass().getName();
                throw new BuildException(msg);
            }
            String msg = "Class " + clz.getName() + " is not an adapter: ";
            if (!RoleAdapter.class.isAssignableFrom(clz)) {
                throw new BuildException(msg + "does not implement RoleAdapter");
            }
            try {
                Method chk = clz.getMethod("checkClass", CHECK_ADAPTER_PARAMS);
                if (!Modifier.isStatic(chk.getModifiers())) {
                    throw new BuildException(msg + "checkClass() is not static");
                }
                return chk;
            }
            catch(NoSuchMethodException nme){
                throw new BuildException(msg + "checkClass() not found", nme);
            }
        }

    }
}
