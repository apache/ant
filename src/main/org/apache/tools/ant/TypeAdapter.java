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

/**
 * Used to wrap types.
 *
 */
public interface TypeAdapter {

    /**
     * Sets the project
     *
     * @param p the project instance.
     */
    void setProject(Project p);

    /**
     * Gets the project
     *
     * @return the project instance.
     */
    Project getProject();

    /**
     * Sets the proxy object, whose methods are going to be
     * invoked by ant.
     * A proxy object is normally the object defined by
     * a &lt;typedef/&gt; task that is adapted by the "adapter"
     * attribute.
     *
     * @param o The target object. Must not be <code>null</code>.
     */
    void setProxy(Object o);

    /**
     * Returns the proxy object.
     *
     * @return the target proxy object
     */
    Object getProxy();

    /**
     * Check if the proxy class is compatible with this adapter - i.e.
     * the adapter will be able to adapt instances of the give class.
     *
     * @param proxyClass the class to be checked.
     */
    void checkProxyClass(Class<?> proxyClass);
}
