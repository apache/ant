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
import org.apache.ant.common.antlib.StandardLibFactory;
import org.apache.ant.common.service.EventService;
import org.apache.ant.common.util.ExecutionException;
import org.apache.ant.init.LoaderUtils;

/**
 * The factory object for the Ant1 compatability Ant library
 *
 * @author Conor MacNeill
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
        if (project != null) {
            return;
        }

        this.context = context;
        // set the system classpath. In Ant2, the system classpath will not
        // in general, have any useful information. For Ant1 compatability
        // we set it now to include the Ant1 facade classes
        System.setProperty("java.class.path", getAnt1Classpath());

        project = new Project(this);
        project.init(context);

        EventService eventService =
            (EventService) context.getCoreService(EventService.class);
        eventService.addBuildListener(project);
    }


    /**
     * Create an instance of the given component class
     *
     * @param componentClass the class for which an instance is required
     * @param localName the name within the library under which the task is
     *      defined
     * @return an instance of the required class
     * @exception InstantiationException if the class cannot be instantiated
     * @exception IllegalAccessException if the instance cannot be accessed
     * @exception ExecutionException if there is a problem creating the task
     */
    public Object createComponent(Class componentClass, String localName)
         throws InstantiationException, IllegalAccessException,
        ExecutionException {
        try {
            java.lang.reflect.Constructor constructor = null;
            // DataType can have a "no arg" constructor or take a single
            // Project argument.
            Object component = null;
            try {
                constructor = componentClass.getConstructor(new Class[0]);
                component = constructor.newInstance(new Object[0]);
            } catch (NoSuchMethodException nse) {
                constructor 
                    = componentClass.getConstructor(new Class[]{Project.class});
                component = constructor.newInstance(new Object[]{project});
            }

            if (component instanceof ProjectComponent) {
                ((ProjectComponent) component).setProject(project);
            }
            return component;
        } catch (java.lang.reflect.InvocationTargetException ite) {
            Throwable t = ite.getTargetException();
            String msg = "Could not create component of type: "
                 + componentClass.getName() + " due to " + t;
            throw new ExecutionException(msg, t);
        } catch (NoSuchMethodException e) {
            throw new ExecutionException("Unable to find an appropriate "
                 + "constructor for component " + componentClass.getName(), e);
        }
    }

    /**
     * Create an instance of the given class
     *
     * @param requiredClass the class for which an instance is
     *      required
     * @return a instance of the required class
     * @exception InstantiationException if the class cannot be instantiated
     * @exception IllegalAccessException if the instance cannot be accessed
     * @exception ExecutionException if there is a problem creating the
     *      converter
     */
    public Object createInstance(Class requiredClass)
         throws InstantiationException, IllegalAccessException,
        ExecutionException {

        java.lang.reflect.Constructor c = null;

        Object instance = null;
        try {
            try {
                c = requiredClass.getConstructor(new Class[0]);
                instance = c.newInstance(new Object[0]);
            } catch (NoSuchMethodException nse) {
                c = requiredClass.getConstructor(new Class[]{Project.class});
                instance = c.newInstance(new Object[]{project});
            }

            return instance;
        } catch (java.lang.reflect.InvocationTargetException ite) {
            Throwable t = ite.getTargetException();
            String msg = "Could not create instance of type: "
                 + requiredClass.getName() + " due to " + t;
            throw new ExecutionException(msg, t);
        } catch (NoSuchMethodException e) {
            throw new ExecutionException("Unable to find an appropriate "
                 + "constructor for class " + requiredClass.getName(), e);
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
            ProjectComponent component = (ProjectComponent) createdElement;
            component.setProject(project);
        }
    }

    /**
     * Get an Ant1 equivalent classpath
     *
     * @return an Ant1 suitable classpath
     */
    String getAnt1Classpath() {
        ClassLoader classLoader = getClass().getClassLoader();
        String path = LoaderUtils.getClasspath(classLoader);
        return path;
    }
}

