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

package org.apache.tools.ant.types.selectors.modifiedselector;


import java.io.File;


/**
 * The <i>Algorithm</i> defines how a value for a file is computed.
 * It must be sure that multiple calls for the same file results in the
 * same value.
 * The implementing class should implement a useful toString() method.
 *
 * @version 2003-09-13
 * @since  Ant 1.6
 */
public interface Algorithm {

    /**
     * Checks its prerequisites.
     * @return <i>true</i> if all is ok, otherwise <i>false</i>.
     */
    boolean isValid();

    /**
     * Get the value for a file.
     * @param file    File object for which the value should be evaluated.
     * @return        The value for that file
     */
    String getValue(File file);
}
