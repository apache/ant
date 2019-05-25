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

package org.apache.tools.ant.taskdefs.condition;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.util.StreamUtils;

/**
 * &lt;or&gt; condition container.
 *
 * <p>Iterates over all conditions and returns true as soon as one
 * evaluates to true.</p>
 *
 * @since Ant 1.4
 */
public class Or extends ConditionBase implements Condition {

    /**
     * @return true if any of the contained conditions evaluate to true
     * @exception BuildException if an error occurs
     */
    @Override
    public boolean eval() throws BuildException {
        return StreamUtils.enumerationAsStream(getConditions()).anyMatch(Condition::eval);
    }

}
