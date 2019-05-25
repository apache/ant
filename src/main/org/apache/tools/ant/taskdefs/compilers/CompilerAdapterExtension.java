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
package org.apache.tools.ant.taskdefs.compilers;

/**
 * Extension interface for compilers that support source extensions
 * other than .java.
 *
 * @since Ant 1.8.2
 */
public interface CompilerAdapterExtension {

    /**
     * Returns a list of source file extensions that are recognized by
     * this compiler adapter.
     *
     * <p>For example, most compiler adapters will return [ "java" ],
     * but a compiler adapter that can compile both Java and Groovy
     * source code would return [ "java", "groovy" ].</p>
     *
     * @return list of source file extensions recognized by this
     * compiler adapter.
     */
    String[] getSupportedFileExtensions();
}
