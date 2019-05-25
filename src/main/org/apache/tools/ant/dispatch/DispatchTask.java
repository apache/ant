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
package org.apache.tools.ant.dispatch;

import org.apache.tools.ant.Task;

/**
 * Tasks extending this class may contain multiple actions.
 * The method that is invoked for execution depends upon the
 * value of the action attribute of the task.
 * <p>Example:</p>
 * &lt;mytask action=&quot;list&quot;/&gt; will invoke the method
 * with the signature public void list() in mytask's class.
 * If the action attribute is not defined in the task or is empty,
 * the execute() method will be called.
 */
public abstract class DispatchTask extends Task implements Dispatchable {
    private String action;

    /**
     * Get the action parameter name.
     * @return the <code>String</code> "action" by default (can be overridden).
     */
    @Override
    public String getActionParameterName() {
        return "action";
    }

    /**
     * Set the action.
     * @param action the method name.
     */
    public void setAction(String action) {
        this.action = action;
    }

    /**
     * Get the action.
     * @return the action.
     */
    public String getAction() {
        return action;
    }
}
