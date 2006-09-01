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
package test;

import org.apache.tools.ant.Task;
import org.apache.tools.ant.TaskContainer;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.taskdefs.Echo;
import java.util.*;

public class SpecialSeq extends Task implements TaskContainer {
    /** Optional Vector holding the nested tasks */
    private Vector nestedTasks = new Vector();

    private FileSet fileset;
    
    private Echo nestedEcho;
    
    /**
     * Add a nested task.
     * <p>
     * @param nestedTask  Nested task to execute
     * <p>
     */
    public void addTask(Task nestedTask) {
        nestedTasks.addElement(nestedTask);
    }

    /**
     * Execute all nestedTasks.
     */
    public void execute() throws BuildException {
        if (fileset == null || fileset.getDir(getProject()) == null) {
            throw new BuildException("Fileset was not configured");
        }
        for (Enumeration e = nestedTasks.elements(); e.hasMoreElements();) {
            Task nestedTask = (Task) e.nextElement();
            nestedTask.perform();
        }
        nestedEcho.reconfigure();
        nestedEcho.perform();
    }

    public void addFileset(FileSet fileset) {
        this.fileset = fileset;
    }
    
    public void addNested(Echo nestedEcho) {
        this.nestedEcho = nestedEcho;
    }
}
