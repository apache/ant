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

package org.apache.tools.ant.helper;

import java.lang.reflect.Method;
import org.apache.tools.ant.*;

/**
 * Uses introspection to "adapt" an arbitrary Bean which doesn't
 * itself extend Task, but still contains an execute method and optionally 
 * a setProject method.
 *
 *  The adapter can also be used to wrap tasks that are loaded in a different class loader
 *  by ant, when used in programatic mode.
 *
 * @author Costin Manolache
 */
public class TaskAdapter2 extends Task { // implements DynamicConfigurator {
    /* Need to support DynamicConfigurator so that adapted tasks can
       support that too.
    */
    
    /** Object to act as a proxy for. */
    private Object proxy;
    private String methodName="execute";
    
    private IntrospectionHelper ih;

    void setIntrospectionHelper( IntrospectionHelper ih ) {
        this.ih=ih;
    }

    IntrospectionHelper getIntrospectionHelper()
    {
        if( ih==null ) {
            ih = IntrospectionHelper.getHelper(target.getClass());
        }
        return ih;
    }

    public void setDynamicAttribute(String name, String value)
            throws BuildException
    {
        setAttribute( name, value );
    }

    public Object createDynamicElement(String name) throws BuildException
    {
        return null;
    }

    
    /** Experimental, non-public method for better 'adaptation'
     *
     */
    void setAttribute( String name, String value )
        throws BuildException
    {
        try {
            ih.setAttribute( project, proxy, name, value );
        } catch( BuildException ex ) {
            if( "do".equals( name ) ) {
                setDo( value );
            } else {
                throw ex;
            }
        }
    }
        
    /** Set the 'action' method. This allow beans implementing multiple
     * actions or using methods other than 'execute()' to be used in ant
     * without any modification.
     * 
     *  @ant:experimental 
     */
    public void setDo(String methodName ) {
        this.methodName=methodName;
    }
    
    /**
     * Executes the proxied task.
     */
    public void execute() throws BuildException {
        Method setProjectM = null;
        try {
            Class c = proxy.getClass();
            setProjectM = 
                c.getMethod( "setProject", new Class[] {Project.class});
            if(setProjectM != null) {
                setProjectM.invoke(proxy, new Object[] {project});
            }
        } catch (NoSuchMethodException e) {
             // ignore this if the class being used as a task does not have
            // a set project method.
        } catch( Exception ex ) {
            log("Error setting project in " + proxy.getClass(), 
                Project.MSG_ERR);
            throw new BuildException( ex );
        }


        Method executeM=null;
        try {
            Class c=proxy.getClass();
            executeM=c.getMethod( methodName, new Class[0] );
            if( executeM == null ) {
                log("No public " + methodName + "() in " + proxy.getClass(), Project.MSG_ERR);
                throw new BuildException("No public " + methodName +"() in " + proxy.getClass());
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
        } catch( Exception ex ) {
            log("Error in " + proxy.getClass(), Project.MSG_ERR);
            throw new BuildException( ex );
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
