/*
 * Copyright  2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
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
package org.apache.tools.ant.dispatch;

import org.apache.tools.ant.Task;

/**
 * Tasks extending this class may contain multiple actions.
 * The method that is invoked for executoin depends upon the
 * value of the action attribute of the task.
 * <br/>
 * Example:<br/>
 * &lt;mytask action=&quot;list&quot;/&gt; will invoke the method
 * with the signature public void list() in mytask's class.
 * If the action attribute is not defined in the task or is empty,
 * the execute() method will be called.
 */
public abstract class DispatchTask extends Task implements Dispatchable {
    private String action;

    public String getActionParameterName() {
        return "action";
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getAction() {
        return action;
    }
}