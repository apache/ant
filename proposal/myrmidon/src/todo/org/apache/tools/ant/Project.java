/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant;

import java.io.File;
import java.util.Enumeration;
import java.util.Hashtable;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.listeners.ProjectListener;
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
    public final static int MSG_ERR = 0;
    public final static int MSG_WARN = 1;
    public final static int MSG_INFO = 2;
    public final static int MSG_VERBOSE = 3;
    public final static int MSG_DEBUG = 4;

    private Hashtable properties = new Hashtable();
    private FilterSet globalFilterSet = new FilterSet();

    /**
     * Records the latest task on a thread
     */
    private Hashtable threadTasks = new Hashtable();
    private File baseDir;

    /**
     * get the base directory of the project as a file object
     *
     * @return the base directory. If this is null, then the base dir is not
     *      valid
     */
    public File getBaseDir()
    {
        return baseDir;
    }

    /**
     * get the current task definition hashtable
     *
     * @return The DataTypeDefinitions value
     */
    public Hashtable getDataTypeDefinitions()
    {
        return null;
    }

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

    /**
     * query a property.
     *
     * @param name the name of the property
     * @return the property value, or null for no match
     */
    public String getProperty( String name )
    {
        if( name == null )
        {
            return null;
        }
        String property = (String)properties.get( name );
        return property;
    }

    /**
     * @param key Description of Parameter
     * @return The object with the "id" key.
     */
    public Object getReference( String key )
    {
        return null;
    }

    /**
     * get the current task definition hashtable
     *
     * @return The TaskDefinitions value
     */
    public Hashtable getTaskDefinitions()
    {
        return null;
    }

    public void addProjectListener( final ProjectListener listener )
    {
    }

    /**
     * create a new task instance
     *
     * @param taskType name of the task
     * @return null if the task name is unknown
     * @throws TaskException when task creation goes bad
     */
    public Task createTask( String taskType )
        throws TaskException
    {
        throw new TaskException( "Task needs reimplementing" );
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

    /**
     * Output a message to the log with the given log level and an event scope
     * of project
     *
     * @param msg text to log
     * @param msgLevel level to log at
     */
    public void log( String msg, int msgLevel )
    {
    }

    /**
     * Replace ${} style constructions in the given value with the string value
     * of the corresponding data types.
     *
     * @param value the string to be scanned for property references.
     * @return Description of the Returned Value
     * @exception TaskException Description of Exception
     */
    public String replaceProperties( String value )
        throws TaskException
    {
        return null;
    }
}
