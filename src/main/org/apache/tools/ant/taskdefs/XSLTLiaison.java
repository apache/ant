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

import java.io.File;

/**
 * Proxy interface for XSLT processors.
 *
 * @see XSLTProcess
 * @since Ant 1.1
 */
public interface XSLTLiaison {

    /**
     * the file protocol prefix for systemid.
     * This file protocol must be appended to an absolute path.
     * Typically: <code>FILE_PROTOCOL_PREFIX + file.getAbsolutePath()</code>
     * Note that on Windows, an extra '/' must be appended to the
     * protocol prefix so that there is always 3 consecutive slashes.
     * @since Ant 1.4
     */
    String FILE_PROTOCOL_PREFIX = "file://";

    /**
     * set the stylesheet to use for the transformation.
     * @param stylesheet the stylesheet to be used for transformation.
     * @throws Exception thrown if any problems happens.
     * @since Ant 1.4
     */
    void setStylesheet(File stylesheet) throws Exception; //NOSONAR

    /**
     * Add a parameter to be set during the XSL transformation.
     * @param name the parameter name.
     * @param expression the parameter value as an expression string.
     * @throws Exception thrown if any problems happens.
     * @see XSLTLiaison4#addParam(java.lang.String, java.lang.Object)
     * @since Ant 1.3
     */
    void addParam(String name, String expression) throws Exception; //NOSONAR

    /**
     * Perform the transformation of a file into another.
     * @param infile the input file, probably an XML one. :-)
     * @param outfile the output file resulting from the transformation
     * @throws Exception thrown if any problems happens.
     * @see #setStylesheet(File)
     * @since Ant 1.4
     */
    void transform(File infile, File outfile) throws Exception; //NOSONAR

}
