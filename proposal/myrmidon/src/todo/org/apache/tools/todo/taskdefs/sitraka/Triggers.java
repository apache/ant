/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.todo.taskdefs.sitraka;

import java.util.ArrayList;
import java.util.Hashtable;
import org.apache.myrmidon.api.TaskException;

/**
 * Trigger information. It will return as a command line argument by calling the
 * <tt>toString()</tt> method.
 *
 * @author <a href="mailto:sbailliez@imediation.com">Stephane Bailliez</a>
 */
public class Triggers
{

    /**
     * mapping of actions to cryptic command line mnemonics
     */
    private final static Hashtable actionMap = new Hashtable( 3 );

    /**
     * mapping of events to cryptic command line mnemonics
     */
    private final static Hashtable eventMap = new Hashtable( 3 );

    protected ArrayList triggers = new ArrayList();

    static
    {
        actionMap.put( "enter", "E" );
        actionMap.put( "exit", "X" );
        // clear|pause|resume|snapshot|suspend|exit
        eventMap.put( "clear", "C" );
        eventMap.put( "pause", "P" );
        eventMap.put( "resume", "R" );
        eventMap.put( "snapshot", "S" );
        eventMap.put( "suspend", "A" );
        eventMap.put( "exit", "X" );
    }

    public Triggers()
    {
    }

    public void addMethod( Method method )
    {
        triggers.add( method );
    }

    // -jp_trigger=ClassName.*():E:S,ClassName.MethodName():X:X
    public String toString()
    {
        StringBuffer buf = new StringBuffer();
        final int size = triggers.size();
        for( int i = 0; i < size; i++ )
        {
            buf.append( triggers.get( i ).toString() );
            if( i < size - 1 )
            {
                buf.append( ',' );
            }
        }
        return buf.toString();
    }

    public static class Method
    {
        protected String action;
        protected String event;
        protected String name;
        protected String param;

        public void setAction( String value )
            throws TaskException
        {
            if( actionMap.get( value ) == null )
            {
                throw new TaskException( "Invalid action, must be one of " + actionMap );
            }
            action = value;
        }

        public void setEvent( String value )
        {
            if( eventMap.get( value ) == null )
            {
                throw new TaskException( "Invalid event, must be one of " + eventMap );
            }
            event = value;
        }

        public void setName( String value )
        {
            name = value;
        }

        public void setParam( String value )
        {
            param = value;
        }

        // return <name>:<event>:<action>[:param]
        public String toString()
        {
            StringBuffer buf = new StringBuffer();
            buf.append( name ).append( ":" );//@todo name must not be null, check for it
            buf.append( eventMap.get( event ) ).append( ":" );
            buf.append( actionMap.get( action ) );
            if( param != null )
            {
                buf.append( ":" ).append( param );
            }
            return buf.toString();
        }
    }

}
