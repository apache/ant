/*
 *  The Apache Software License, Version 1.1
 *
 *  Copyright (c) 2002 The Apache Software Foundation.  All rights
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
 *  4. The names "Ant" and "Apache Software
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

package org.apache.tools.ant.util;

import org.apache.tools.ant.BuildException;

import java.lang.reflect.Constructor;


/**
 * this is a weak reference on java1.2 and up, a hard
 * reference on java1.1
 * @since ant1.6
 */
public abstract class WeakishReference  {

    private static Constructor referenceConstructor;

    private final static String WEAK_REFERENCE_NAME= "org.apache.tools.ant.util.optional.WeakishReference12";

    /**
     * create the appropriate type of reference for the java version
     * @param object
     * @return
     */
    public static WeakishReference createReference(Object object) {
        if(referenceConstructor==null) {
            createReferenceConstructor();
        }
        try {
            return (WeakishReference)referenceConstructor
                        .newInstance(new Object[]{object});
        } catch (Exception e) {
            throw new BuildException("while creating a weakish reference",e);
        }
    }

    /**
     * create the appropriate constructor method for the
     */
    private static void createReferenceConstructor() {
        Class[] ctor=new Class[]{Object.class};
        try {
            referenceConstructor=HardReference.class.getConstructor(ctor);
        } catch (NoSuchMethodException e) {
            //deep trouble here
            throw new BuildException("when creating a Hard Reference constructor",e);
        }
        if (!JavaEnvUtils.isJavaVersion(JavaEnvUtils.JAVA_1_1)) {
            //create a weak ref constructor. If this fails we have that hard one anyway
            try {
                Class clazz=Class.forName(WEAK_REFERENCE_NAME);
                referenceConstructor=clazz.getConstructor(ctor);
            } catch (ClassNotFoundException e) {
            } catch (NoSuchMethodException e) {
            }
        }
    }


    /**
     * Returns this reference object's referent.  If this reference object has
     * been cleared, then this method returns <code>null</code>.
     *
     * @return	 The object to which this reference refers, or
     *		 <code>null</code> if this reference object has been cleared
     */
    public abstract Object get();

    /**
     * A hard reference for Java 1.1
     */
    public static class HardReference extends WeakishReference {
        private Object object;

        /**
         * construct
         * @param object
         */
        public HardReference(Object object) {
            this.object = object;
        }

        /**
         * Returns this reference object's referent.
         */
        public Object get() {
            return object;
        }
    }

}
