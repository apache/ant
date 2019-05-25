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
package org.apache.tools.ant.taskdefs;


/**
 * Extends Proxy interface for XSLT processors: adds support for XSLT parameters
 * of various types (not only String)
 *
 *
 * @see XSLTProcess
 * @author Frantisek Kucera (xkucf03)
 * @since Ant 1.9.3
 */
public interface XSLTLiaison4 extends XSLTLiaison3 {

    /**
     * Add a parameter to be set during the XSL transformation.
     *
     * @param name the parameter name.
     * @param value the parameter value as String, Boolean, int, etc.
     * @throws Exception thrown if any problems happens.
     * @since Ant 1.9.3
     * @see javax.xml.transform.Transformer#setParameter(java.lang.String, java.lang.Object)
     */
    void addParam(String name, Object value) throws Exception;
}
