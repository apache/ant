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

package org.apache.tools.ant.taskdefs.optional;

import org.apache.tools.ant.*;
import org.apache.tools.ant.PropertyHelper;
import org.apache.tools.ant.types.*;

import java.io.*;
import java.util.*;

import org.apache.commons.jxpath.*;

// Experimental: need to add code to select the 'root', etc.

/**
 *  Enable JXPath dynamic properties
 *
 * @author Costin Manolache
 * @author Nicola Ken Barozzi
 */
public class JXPath extends Task {

    public static String PREFIX="jxpath:";
    JXPathContext jxpathCtx;

    public JXPath() {
    }

    public JXPathContext getJXPathContext() {
        return jxpathCtx;
    }

    // testing
    String foo;
    public void setFoo( String s ) {
        System.out.println("Set foo " + s );
        foo=s;
    }

    public String getFoo() {
        return foo;
    }

    public void execute() {
        JXPathIntrospector.registerDynamicClass(Hashtable.class, JXPathHashtableHandler.class);
        jxpathCtx=JXPathContext.newContext( project );
        jxpathCtx.setVariables(new AntVariables());

        PropertyHelper phelper=PropertyHelper.getPropertyHelper( project );
        JXPathPropertyHelper hook=new JXPathPropertyHelper(jxpathCtx);
        hook.setNext( phelper.getNext() );
        phelper.setNext( hook );

        project.addReference( "jxpathTask", this );

    }


    static class JXPathPropertyHelper extends PropertyHelper {
        JXPathContext jxpathCtx;

        public JXPathPropertyHelper( JXPathContext jxCtx ) {
            this.jxpathCtx=jxCtx;
        }

        public boolean setProperty( String ns, String name, Object v, boolean inh,
                                    boolean user, boolean isNew)
        {
            if( ! name.startsWith(PREFIX) )
                return false;
            name=name.substring( PREFIX.length() );

            jxpathCtx.setValue( name, v );
            return true;
        }

        public Object getPropertyHook( String ns, String name , boolean user) {
            if( ! name.startsWith(PREFIX) )
                return null;
            name=name.substring( PREFIX.length() );


            //Object o=jxpathCtx.getValue( name );
            //System.out.println("JXPath: getProperty " + ns + " " + name + "=" + o + o.getClass());

            String result = "";

            Iterator iter = jxpathCtx.iterate(name);

            if(iter==null){
                return "null";
            }

            result += iter.next();

            while (iter.hasNext()) {
                Object o = iter.next();
                //System.out.println("JXPath: getProperty " + ns + " " + name + "=" + o + o.getClass());
                result += ", "+o;
            }

            return result;
        }


    }


    public static class JXPathHashtableHandler implements DynamicPropertyHandler {

        private static final String[] STRING_ARRAY = new String[0];

        /**
         * Returns string representations of all keys in the map.
         */
        public String[] getPropertyNames(Object object){
            // System.out.println("getPropertyNames " + object );
            Hashtable map = (Hashtable) object;
            String names[] = new String[map.size()];
            Enumeration it = map.keys();
            for (int i = 0; i < names.length; i++){
                names[i] = String.valueOf(it.nextElement());
            }
            return names;
        }

        /**
         * Returns the value for the specified key.
         */
        public Object getProperty(Object object, String propertyName) {
            //  System.out.println("getProperty " + object + " " + propertyName);
            return ((Hashtable) object).get(propertyName);
        }

        /**
         * Sets the specified key value.
         */
        public void setProperty(Object object, String propertyName, Object value){
            ((Hashtable)object).put(propertyName, value);
        }
    }

    public class AntVariables implements Variables {

         protected AntVariables(){
         }

         public void declareVariable(String varName, Object value){
           project.setNewProperty(varName, value.toString());
         }

         public Object getVariable(String varName){
           return project.getProperty(varName);
         }

         public boolean isDeclaredVariable(String varName){
           return project.getProperty(varName) == null ? false : true ;
         }

         public void undeclareVariable(String varName){
           throw new UnsupportedOperationException("Cannot undeclare variables in Ant.");
         }

    }

}
