/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant;

import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.Vector;
import org.apache.myrmidon.api.TaskException;

/**
 * This class implements a target object with required parameters.
 *
 * @author James Davidson <a href="mailto:duncan@x180.com">duncan@x180.com</a>
 */
public class Target
{
    private Vector dependencies = new Vector( 2 );
    private Vector children = new Vector( 5 );
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

    public Enumeration getDependencies()
    {
        return dependencies.elements();
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
        Vector tasks = new Vector( children.size() );
        Enumeration enum = children.elements();
        while( enum.hasMoreElements() )
        {
            Object o = enum.nextElement();
            if( o instanceof Task )
            {
                tasks.addElement( o );
            }
        }

        Task[] retval = new Task[ tasks.size() ];
        tasks.copyInto( retval );
        return retval;
    }

    public void addDependency( String dependency )
    {
        dependencies.addElement( dependency );
    }

    public void addTask( Task task )
    {
        children.addElement( task );
    }

    void replaceChild( Task el, Object o )
    {
        int index = -1;
        while( ( index = children.indexOf( el ) ) >= 0 )
        {
            children.setElementAt( o, index );
        }
    }
}
