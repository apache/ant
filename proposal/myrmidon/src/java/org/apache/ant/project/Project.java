/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.ant.project;

import java.util.Iterator;
import org.apache.ant.AntException;
import org.apache.ant.tasklet.TaskletContext;
import org.apache.avalon.Component;

public interface Project
    extends Component
{
    // the name of currently executing project
    String PROJECT          = "ant.project.name"; 

    // the name of currently executing project
    String PROJECT_FILE     = "ant.project.file"; 

    // the name of currently executing target
    String TARGET           = "ant.target.name"; 

    String getDefaultTargetName();
    Target getImplicitTarget();
    Target getTarget( String name );
    Iterator getTargetNames();
    TaskletContext getContext();
}
