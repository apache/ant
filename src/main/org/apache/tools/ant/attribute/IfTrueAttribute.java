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

package org.apache.tools.ant.attribute;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.UnknownElement;

/**
 * Check if an attribute value is true or not.
 * @since Ant 1.9.1
 */
public class IfTrueAttribute extends BaseIfAttribute {
    /** The unless version */
    public static class Unless extends IfTrueAttribute {
        { setPositive(false); }
    }

    /**
     * check if the attribute value is true or not
     * {@inheritDoc}
     */
    public boolean isEnabled(UnknownElement el, String value) {
        return convertResult(Project.toBoolean(value));
    }
}
