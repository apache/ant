/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.optional.perforce;

import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.api.AbstractTask;

/**
 * P4Counter - Obtain or set the value of a counter. P4Counter can be used to
 * either print the value of a counter to the output stream for the project (by
 * setting the "name" attribute only), to set a property based on the value of a
 * counter (by setting the "property" attribute) or to set the counter on the
 * perforce server (by setting the "value" attribute). Example Usage:<br>
 * &lt;p4counter name="${p4.counter}" property=${p4.change}"/&gt;
 *
 * @author <a href="mailto:kirk@radik.com">Kirk Wylie</a>
 */
public class P4Counter
    extends P4Base
{
    private String m_counter;
    private String m_property;
    private boolean m_shouldSetValue;
    private int m_value;

    public void setName( final String counter )
    {
        m_counter = counter;
    }

    public void setProperty( final String property )
    {
        m_property = property;
    }

    public void setValue( final int value )
    {
        m_value = value;
        m_shouldSetValue = true;
    }

    public void execute()
        throws TaskException
    {
        validate();

        String command = "counter " + m_p4CmdOpts + " " + m_counter;
        if( !shouldSetProperty() )
        {
            // NOTE kirk@radik.com 04-April-2001 -- If you put in the -s, you
            // have to start running through regular expressions here. Much easier
            // to just not include the scripting information than to try to add it
            // and strip it later.
            command = "-s " + command;
        }
        if( m_shouldSetValue )
        {
            command += " " + m_value;
        }

        execP4Command( command, null );
    }

    public void stdout( final String line )
    {
        if( shouldSetProperty() )
        {
            super.stdout( line );
        }
        else
        {
            getLogger().debug( "P4Counter retrieved line \"" + line + "\"" );
            try
            {
                m_value = Integer.parseInt( line );
                final String name = m_property;
                final Object value = "" + m_value;
                getContext().setProperty( name, value );
            }
            catch( final TaskException te )
            {
                registerError( te );
            }
            catch( NumberFormatException nfe )
            {
                final String message = "Perforce error. Could not retrieve counter value.";
                registerError( new TaskException( message ) );
            }
        }
    }

    private void validate()
        throws TaskException
    {
        if( ( m_counter == null ) || m_counter.length() == 0 )
        {
            throw new TaskException( "No counter specified to retrieve" );
        }

        if( m_shouldSetValue && shouldSetProperty() )
        {
            throw new TaskException( "Cannot both set the value of the property and retrieve the value of the property." );
        }
    }

    private boolean shouldSetProperty()
    {
        return ( null == m_property );
    }
}
