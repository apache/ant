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

package org.example.tasks;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

public class TaskdefTestSimpleTask extends Task {

    public class Echo {
        Echo() {
        }

        private String message = null;

        public void setMessage(String s) {
            message = s;
        }
    }

    public TaskdefTestSimpleTask() {
    }

    private Echo echo;

    public Echo createEcho() {
        echo = new Echo();
        return echo;
    }

    public void execute() {
        log("simpletask: " + echo.message, Project.MSG_INFO);
    }

}
