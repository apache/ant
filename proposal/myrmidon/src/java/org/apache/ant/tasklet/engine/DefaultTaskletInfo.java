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
 * This is default container of information about a task. 
 * A BeanInfo equivelent for a task. Eventually it will auto-magically
 * generate a schema via reflection for Validator/Editor tools.
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public class DefaultTaskletInfo
    implements TaskletInfo
{
    protected final String        m_classname;
    protected final URL           m_location;

    /**
     * Constructor that takes classname and taskLibraryLocation.
     */
    public DefaultTaskletInfo( final String classname, final URL location )
    {
        m_location = location;
        m_classname = classname;
    }

    /**
     * Retrieve classname for task.
     *
     * @return the taskname
     */
    public String getClassname()
    {    
        return m_classname;
    }

    /**
     * Retrieve tasklib location from which task is loaded.
     *
     * @return the location
     */
    public URL getLocation()
    {
        return m_location;
    }
}
