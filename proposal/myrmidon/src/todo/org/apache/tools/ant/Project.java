/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant;

import java.util.Enumeration;
import java.util.Hashtable;
import org.apache.tools.ant.types.FilterSet;

/**
 * Central representation of an Ant project. This class defines a Ant project
 * with all of it's targets and tasks. It also provides the mechanism to kick
 * off a build using a particular target name. <p>
 *
 * This class also encapsulates methods which allow Files to be refered to using
 * abstract path names which are translated to native system file paths at
 * runtime as well as defining various project properties.
 *
 * @author duncan@x180.com
 */
public class Project
{
    private Hashtable properties = new Hashtable();
    private FilterSet globalFilterSet = new FilterSet();

    /**
     * Records the latest task on a thread
     */
    private Hashtable threadTasks = new Hashtable();

    public FilterSet getGlobalFilterSet()
    {
        return globalFilterSet;
    }

    /**
     * get a copy of the property hashtable
     *
     * @return the hashtable containing all properties, user included
     */
    public Hashtable getProperties()
    {
        Hashtable propertiesCopy = new Hashtable();

        Enumeration e = properties.keys();
        while( e.hasMoreElements() )
        {
            Object name = e.nextElement();
            Object value = properties.get( name );
            propertiesCopy.put( name, value );
        }

        return propertiesCopy;
    }

    public void demuxOutput( String line, boolean isError )
    {
        Task task = (Task)threadTasks.get( Thread.currentThread() );
        if( task == null )
        {
            //fireMessageLogged( this, line, isError ? MSG_ERR : MSG_INFO );
        }
        else
        {
            if( isError )
            {
                task.handleErrorOutput( line );
            }
            else
            {
                task.handleOutput( line );
            }
        }
    }
}
