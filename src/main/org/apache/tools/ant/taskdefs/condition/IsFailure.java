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

import org.apache.tools.ant.taskdefs.Execute;

/**
 * Condition to test a return-code for failure.
 * @since Ant 1.7
 */
public class IsFailure implements Condition {
    private int code;

    /**
     * Set the return code to check.
     * @param c the return code.
     */
    public void setCode(int c) {
        code = c;
    }

    /**
     * Get the return code that will be checked by this IsFailure condition.
     * @return return code as int.
     */
    public int getCode() {
        return code;
    }

    /**
     * Fulfill the condition interface.
     * @return the result of evaluating the specified return code.
     */
    public boolean eval() {
        return Execute.isFailure(code);
    }

}
