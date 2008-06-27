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

import java.io.File;

import xdoclet.XDocletException;
import xjavadoc.XClass;

/**
 * Generates Ant task descriptors.
 *
 * @created              January 1, 2003
 * @ant.element          display-name="taskdescriptor" name="taskdescriptor"
 *      parent="xdoclet.modules.apache.ant.org.apache.ant.xdoclet.AntDocletTask"
 * @ant.task             ignore="true"
 * @xdoclet.merge-file   file="{0}.xml" relates-to="{0}.xml" description="Used for code examples. An example merge file
 *      may be found in Ant's proposal/xdocs/src directory."
 */
public class TaskDescriptorSubTask extends AntSubTask
{
    protected static String DEFAULT_TEMPLATE_FILE = "resources/task_xml.xdt";

    public TaskDescriptorSubTask()
    {
        setTemplateURL(getClass().getResource(DEFAULT_TEMPLATE_FILE));
        setDestinationFile("{0}.xml");
    }

    /**
     * Custom file naming. Use the task name for the file name rather than the default class name.
     *
     * @param clazz
     * @return
     * @exception XDocletException
     */
    protected String getGeneratedFileName(XClass clazz) throws XDocletException
    {
        String dir = TaskTagsHandler.getCategoryName(clazz);
        String taskName = TaskTagsHandler.getTaskName(clazz);

        return new File(dir, taskName + ".xml").toString();
    }

}
