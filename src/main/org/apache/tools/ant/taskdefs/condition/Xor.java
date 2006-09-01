/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
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
package org.apache.tools.ant.taskdefs.condition;

import org.apache.tools.ant.BuildException;

import java.util.Enumeration;

/**
 * The <tt>Xor</tt> condition type to exclusive or operations.
 * This does not shortcut stuff.
 */
public class Xor extends ConditionBase implements Condition {

    /**
     * Evaluate the contained conditions.
     * @return the result of xoring the conditions together.
     * @throws org.apache.tools.ant.BuildException
     *          if an error occurs.
     */
    public boolean eval() throws BuildException {
        Enumeration e = getConditions();
        //initial state is false.
        boolean state = false;
        while (e.hasMoreElements()) {
            Condition c = (Condition) e.nextElement();
            //every condition is xored against the previous one
            state ^= c.eval();
        }
        return state;
    }

}
