/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002 The Apache Software Foundation.  All rights
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
 * 4. The names "The Jakarta Project", "Ant", and "Apache Software
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
import org.apache.ant.common.antlib.AntContext;
import org.apache.ant.common.antlib.Converter;
import org.apache.ant.common.antlib.StandardLibFactory;
import org.apache.ant.common.util.ExecutionException;

/**
 * The factory object for the Ant1 compatability Ant library
 *
 * @author <a href="mailto:conor@apache.org">Conor MacNeill</a>
 * @created 31 January 2002
 */
public class Ant1Factory extends StandardLibFactory {
    /**
     * A Project instance associated with the factory - used in the creation
     * of tasks and types
     */
    private Project project;
    /** The Ant context for this factory */
    private AntContext context;

    /**
     * Initialise the factory
     *
     * @param context the context for this factory to use to access core
     *      services.
     * @exception ExecutionException if the factory cannot be initialised.
     */
    public void init(AntContext context) throws ExecutionException {
        this.context = context;
        project = new Project();
        project.init(context);
    }

    /**
     * Create an instance of the requested type class
     *
     * @param typeClass the class from which an instance is required
     * @return an instance of the requested class
     * @exception ExecutionException the instance could not be created.
     * @exception InstantiationException if the type cannot be instantiated
     * @exception IllegalAccessException if the type cannot be accessed
     */
    public Object createTypeInstance(Class typeClass)
         throws InstantiationException, IllegalAccessException,
        ExecutionException {
        try {
            java.lang.reflect.Constructor ctor = null;
            // DataType can have a "no arg" constructor or take a single
            // Project argument.
            Object o = null;
            try {
                ctor = typeClass.getConstructor(new Class[0]);
                o = ctor.newInstance(new Object[0]);
            } catch (NoSuchMethodException nse) {
                ctor = typeClass.getConstructor(new Class[]{Project.class});
                o = ctor.newInstance(new Object[]{project});
            }

            if (o instanceof ProjectComponent) {
                ((ProjectComponent)o).setProject(project);
            }
            return o;
        } catch (java.lang.reflect.InvocationTargetException ite) {
            Throwable t = ite.getTargetException();
            String msg = "Could not create datatype of type: "
                 + typeClass.getName() + " due to " + t;
            throw new ExecutionException(msg, t);
        } catch (NoSuchMethodException e) {
            throw new ExecutionException("Unable to find an appropriate "
                 + "constructor for type " + typeClass.getName(), e);
        }
    }

    /**
     * Create an instance of the requested task class
     *
     * @param taskClass the class from which an instance is required
     * @return an instance of the requested class
     * @exception InstantiationException if the task cannot be instantiated
     * @exception IllegalAccessException if the task cannot be accessed
     */
    public Object createTaskInstance(Class taskClass)
         throws InstantiationException, IllegalAccessException {
        Object instance = taskClass.newInstance();
        if (instance instanceof ProjectComponent) {
            ((ProjectComponent)instance).setProject(project);
        }

        return instance;
    }

    /**
     * Create a converter.
     *
     * @param converterClass the class of the converter.
     * @return an instance of the requested converter class
     * @exception InstantiationException if the converter cannot be
     *      instantiated
     * @exception IllegalAccessException if the converter cannot be accessed
     * @exception ExecutionException if the converter cannot be created
     */
    public Converter createConverter(Class converterClass)
         throws InstantiationException, IllegalAccessException,
        ExecutionException {

        java.lang.reflect.Constructor c = null;

        Converter converter = null;
        try {
            try {
                c = converterClass.getConstructor(new Class[0]);
                converter = (Converter)c.newInstance(new Object[0]);
            } catch (NoSuchMethodException nse) {
                c = converterClass.getConstructor(new Class[]{Project.class});
                converter = (Converter)c.newInstance(new Object[]{project});
            }

            return converter;
        } catch (java.lang.reflect.InvocationTargetException ite) {
            Throwable t = ite.getTargetException();
            String msg = "Could not create converter of type: "
                 + converterClass.getName() + " due to " + t;
            throw new ExecutionException(msg, t);
        } catch (NoSuchMethodException e) {
            throw new ExecutionException("Unable to find an appropriate "
                 + "constructor for converter " + converterClass.getName(), e);
        }
    }

    /**
     * Register an element which has been created as the result of calling a
     * create method.
     *
     * @param createdElement the element that the component created
     * @exception ExecutionException if there is a problem registering the
     *      element
     */
    public void registerCreatedElement(Object createdElement)
         throws ExecutionException {
        if (createdElement instanceof ProjectComponent) {
            ProjectComponent component = (ProjectComponent)createdElement;
            component.setProject(project);
        }
    }
}

