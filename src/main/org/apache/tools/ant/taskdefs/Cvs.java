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
 * Performs operations on a CVS repository.
 *
 * original 1.20
 *
 *  NOTE: This implementation has been moved to AbstractCvsTask with
 *  the addition of some accessors for extensibility.
 *
 *
 * @since Ant 1.1
 *
 * @ant.task category="scm"
 */
public class Cvs extends AbstractCvsTask {

    /**
     * CVS Task - now implemented by the Abstract CVS Task base class
     */
    public Cvs() {
    }
}
