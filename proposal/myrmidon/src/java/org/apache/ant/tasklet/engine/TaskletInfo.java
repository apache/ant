/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.ant.tasklet.engine;

import java.net.URL;
import org.apache.avalon.camelot.Info;

/**
 * This is information about a task. 
 * A BeanInfo equivelent for a task. Eventually it will auto-magically
 * generate a schema via reflection for Validator/Editor tools.
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public interface TaskletInfo
    extends Info
{
    /**
     * Retrieve classname for task.
     *
     * @return the taskname
     */
    String getClassname();

    /**
     * Retrieve location of task library where task is contained.
     *
     * @return the location of task library
     */
    URL getLocation();
}
