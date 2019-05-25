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
package org.apache.tools.ant.types;

/**
 * this interface should be implemented by classes (Scanners) needing
 * to deliver information about resources.
 *
 * @since Ant 1.5.2
 */
public interface ResourceFactory {

    /**
     * Query a resource (file, zipentry, ...) by name
     *
     * @param name relative path of the resource about which
     * information is sought.  Expects &quot;/&quot; to be used as the
     * directory separator.
     * @return instance of Resource; the exists attribute of Resource
     * will tell whether the sought resource exists
     */
    Resource getResource(String name);
}
