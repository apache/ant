/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant;

import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.ArrayList;
import org.apache.myrmidon.api.TaskException;

/**
 * This class implements a target object with required parameters.
 *
 * @author James Davidson <a href="mailto:duncan@x180.com">duncan@x180.com</a>
 */
public class Target
{
    private ArrayList dependencies = new ArrayList( 2 );
    private ArrayList children = new ArrayList( 5 );
    private String description = null;

    private String name;
    private Project project;

    public void setDepends( String depS )
        throws TaskException
    {
        if( depS.length() > 0 )
        {
            StringTokenizer tok =
                new StringTokenizer( depS, ",", true );
            while( tok.hasMoreTokens() )
            {
                String token = tok.nextToken().trim();

                //Make sure the dependency is not empty string
                if( token.equals( "" ) || token.equals( "," ) )
                {
                    throw new TaskException( "Syntax Error: Depend attribute " +
                                             "for target \"" + getName() +
                                             "\" has an empty string for dependency." );
                }

                addDependency( token );

                //Make sure that depends attribute does not
                //end in a ,
                if( tok.hasMoreTokens() )
                {
                    token = tok.nextToken();
                    if( !tok.hasMoreTokens() || !token.equals( "," ) )
                    {
                        throw new TaskException( "Syntax Error: Depend attribute " +
                                                 "for target \"" + getName() +
                                                 "\" ends with a , character" );
                    }
                }
            }
        }
    }

    public void setDescription( String description )
    {
        this.description = description;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    public void setProject( Project project )
    {
        this.project = project;
    }

    public Iterator getDependencies()
    {
        return dependencies.iterator();
    }

    public String getDescription()
    {
        return description;
    }

    public String getName()
    {
        return name;
    }

    public Project getProject()
    {
        return project;
    }

    /**
     * Get the current set of tasks to be executed by this target.
     *
     * @return The current set of tasks.
     */
    public Task[] getTasks()
    {
        ArrayList tasks = new ArrayList( children.size() );
        Iterator enum = children.iterator();
        while( enum.hasNext() )
        {
            Object o = enum.next();
            if( o instanceof Task )
            {
                tasks.add( o );
            }
        }

        final Task[] retval = new Task[ tasks.size() ];
        return (Task[])tasks.toArray( retval );
    }

    public void addDependency( String dependency )
    {
        dependencies.add( dependency );
    }

    public void addTask( Task task )
    {
        children.add( task );
    }

    void replaceChild( Task el, Object o )
    {
        int index = -1;
        while( ( index = children.indexOf( el ) ) >= 0 )
        {
            children.set( index, o );
        }
    }
}
