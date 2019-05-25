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
import org.apache.tools.ant.ProjectComponent;

/**
 * Condition that tests whether a given string evals to true
 *
 * @since Ant 1.5
 */
public class IsTrue extends ProjectComponent implements Condition {
    /**
     * what we eval
     */
    private Boolean value = null;

    /**
     * set the value to be tested; let ant eval it to true/false
     * @param value the value to test
     */
    public void setValue(boolean value) {
        this.value = value ? Boolean.TRUE : Boolean.FALSE;
    }

    /**
     * @return the value
     * @throws BuildException if someone forgot to spec a value
     */
    @Override
    public boolean eval() throws BuildException {
        if (value == null) {
            throw new BuildException("Nothing to test for truth");
        }
        return value;
    }

}
