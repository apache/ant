/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs.optional.ide;

import java.util.Vector;

/**
 * Load specific project versions into the Visual Age for Java workspace. Each
 * project and version name has to be specified completely. Example:
 * <blockquote> &lt;vajload> &nbsp;&lt;project name="MyVAProject"
 * version="2.1"/> &nbsp;&lt;project name="Apache Xerces" version="1.2.0"/>
 * &lt;/vajload> </blockquote>
 *
 * @author Wolf Siberski, TUI Infotec GmbH
 */

public class VAJLoad extends VAJTask
{
    Vector projectDescriptions = new Vector();

    /**
     * Add a project description entry on the project list.
     *
     * @return Description of the Returned Value
     */
    public VAJProjectDescription createVAJProject()
    {
        VAJProjectDescription d = new VAJProjectDescription();
        projectDescriptions.addElement( d );
        return d;
    }

    /**
     * Load specified projects.
     */
    public void execute()
    {
        getUtil().loadProjects( projectDescriptions );
    }
}
