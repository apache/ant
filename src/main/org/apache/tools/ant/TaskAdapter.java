/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2002 The Apache Software Foundation.  All rights 
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

import java.lang.reflect.Method;

/**
 * Uses introspection to "adapt" an arbitrary Bean which doesn't
 * itself extend Task, but still contains an execute method and optionally 
 * a setProject method.
 *
 * @author costin@dnt.ro
 */
public class TaskAdapter extends Task {

    /** Object to act as a proxy for. */
    private Object proxy;
    
    /**
     * Checks whether or not a class is suitable to be adapted by TaskAdapter.
     *
     * This only checks conditions which are additionally required for 
     * tasks adapted by TaskAdapter. Thus, this method should be called by
     * Project.checkTaskClass.
     *
     * Throws a BuildException and logs as Project.MSG_ERR for
     * conditions that will cause the task execution to fail.
     * Logs other suspicious conditions with Project.MSG_WARN.
     * 
     * @param taskClass Class to test for suitability. 
     *                  Must not be <code>null</code>.
     * @param project   Project to log warnings/errors to. 
     *                  Must not be <code>null</code>.
     * 
     * @see Project#checkTaskClass(Class)
     */
    public static void checkTaskClass(final Class taskClass, 
                                      final Project project) {
        // don't have to check for interface, since then
        // taskClass would be abstract too.
        try {
            final Method executeM = taskClass.getMethod("execute", null);
            // don't have to check for public, since
            // getMethod finds public method only.
            // don't have to check for abstract, since then
            // taskClass would be abstract too.
            if (!Void.TYPE.equals(executeM.getReturnType())) {
                final String message = "return type of execute() should be " 
                    + "void but was \"" + executeM.getReturnType() + "\" in " 
                    + taskClass;
                project.log(message, Project.MSG_WARN);
            }
        } catch (NoSuchMethodException e) {
            final String message = "No public execute() in " + taskClass;
            project.log(message, Project.MSG_ERR);
            throw new BuildException(message);
        }
    }
    
    /**
     * Executes the proxied task.
     * 
     * @exception BuildException if the project could not be set
     * or the method could not be executed.
     */
    public void execute() throws BuildException {
        Method setProjectM = null;
        try {
            Class c = proxy.getClass();
            setProjectM = 
                c.getMethod("setProject", new Class[] {Project.class});
            if (setProjectM != null) {
                setProjectM.invoke(proxy, new Object[] {project});
            }
        } catch (NoSuchMethodException e) {
            // ignore this if the class being used as a task does not have
            // a set project method.
        } catch (Exception ex) {
            log("Error setting project in " + proxy.getClass(), 
                Project.MSG_ERR);
            throw new BuildException(ex);
        }


        Method executeM = null;
        try {
            Class c = proxy.getClass();
            executeM = c.getMethod("execute", new Class[0]);
            if (executeM == null) {
                log("No public execute() in " + proxy.getClass(), 
                    Project.MSG_ERR);
                throw new BuildException("No public execute() in " 
                    + proxy.getClass());
            }
            executeM.invoke(proxy, null);
            return; 
        } catch (java.lang.reflect.InvocationTargetException ie) {
            log("Error in " + proxy.getClass(), Project.MSG_ERR);
            Throwable t = ie.getTargetException();
            if (t instanceof BuildException) {
                throw ((BuildException) t);
            } else {
                throw new BuildException(t);
            }
        } catch (Exception ex) {
            log("Error in " + proxy.getClass(), Project.MSG_ERR);
            throw new BuildException(ex);
        }

    }
    
    /**
     * Sets the target object to proxy for.
     * 
     * @param o The target object. Must not be <code>null</code>.
     */
    public void setProxy(Object o) {
        this.proxy = o;
    }

    /**
     * Returns the target object being proxied.
     * 
     * @return the target proxy object
     */
    public Object getProxy() {
        return this.proxy ;
    }

}
