 /*
 * Copyright  2003, 2005 The Apache Software Foundation
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

package org.apache.ant.xdoclet;

import xdoclet.TemplateSubTask;

/**
 * Generates Ant taskdef properties files, suitable for bulk defining tasks with Ant's &lt;taskdef&gt; task.
 *
 * @created       January 5, 2003
 * @ant.element   display-name="taskdefproperties" name="taskdefproperties"
 *      parent="org.apache.ant.xdoclet.AntDocletTask"
 * @ant.task      ignore="true"
 */
public class TaskDefPropertiesSubTask extends AntSubTask
{
    protected static String DEFAULT_TEMPLATE_FILE = "resources/taskdef_properties.xdt";

    public TaskDefPropertiesSubTask()
    {
        setTemplateURL(getClass().getResource(DEFAULT_TEMPLATE_FILE));
        setDestinationFile("taskdef.properties");
    }
}
