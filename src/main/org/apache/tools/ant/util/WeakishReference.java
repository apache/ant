/* 
 * Copyright  2000-2004 Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * 
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

    private final static String WEAK_REFERENCE_NAME
        = "org.apache.tools.ant.util.optional.WeakishReference12";

    /**
     * create the appropriate type of reference for the java version
     * @param object
     * @return reference to the Object.
     */
    public static WeakishReference createReference(Object object) {
        if (referenceConstructor == null) {
            createReferenceConstructor();
        }
        try {
            return (WeakishReference) referenceConstructor
                        .newInstance(new Object[]{object});
        } catch (Exception e) {
            throw new BuildException("while creating a weakish reference", e);
        }
    }

    /**
     * create the appropriate constructor method for the
     */
    private static void createReferenceConstructor() {
        Class[] ctor = new Class[]{Object.class};
        try {
            referenceConstructor = HardReference.class.getConstructor(ctor);
        } catch (NoSuchMethodException e) {
            //deep trouble here
            throw new BuildException("when creating a Hard Reference constructor", e);
        }
        if (!JavaEnvUtils.isJavaVersion(JavaEnvUtils.JAVA_1_1)) {
            //create a weak ref constructor. If this fails we have that hard one anyway
            try {
                Class clazz = Class.forName(WEAK_REFERENCE_NAME);
                referenceConstructor = clazz.getConstructor(ctor);
            } catch (ClassNotFoundException e) {
                // ignore
            } catch (NoSuchMethodException e) {
                // ignore
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
