/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.optional.perforce;

import java.text.SimpleDateFormat;
import java.util.Date;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.api.AbstractTask;
import org.apache.myrmidon.api.TaskContext;

/**
 * P4Label - create a Perforce Label. P4Label inserts a label into perforce
 * reflecting the current client contents. Label name defaults to AntLabel if
 * none set. Example Usage: <pre>
 *   &lt;P4Label name="MyLabel-${TSTAMP}-${DSTAMP}" desc="Auto Build Label" /&gt;
 * </pre>
 *
 * @author <A HREF="mailto:leslie.hughes@rubus.com">Les Hughes</A>
 */
public class P4Label
    extends P4Base
{
    private String m_description;
    private String m_lock;
    private String m_name;
    private boolean m_getLabelSpec;
    private StringBuffer m_labelSpec;

    public void setDesc( final String description )
    {
        m_description = description;
    }

    public void setLock( final String lock )
    {
        m_lock = lock;
    }

    public void setName( final String name )
    {
        m_name = name;
    }

    public void stdout( String line )
    {
        getContext().debug( line );

        if( null != m_labelSpec )
        {
            if( util.match( "/^Options:/", line ) )
            {
                line = "Options: " + m_lock;
            }

            m_labelSpec.append( line + "\n" );
        }
    }

    public void execute()
        throws TaskException
    {
        getContext().info( "P4Label exec:" );

        validate();

        //We have to create a unlocked label first
        String newLabel =
            "Label: " + m_name + "\n" +
            "Description: " + m_description + "\n" +
            "Options: unlocked\n" +
            "View: " + m_p4View + "\n";

        //handler.setOutput( newLabel );
        execP4Command( "label -i", null );
        execP4Command( "labelsync -l " + m_name, null );

        getContext().info( "Created Label " + m_name + " (" + m_description + ")" );

        //Now lock if required
        if( m_lock != null && m_lock.equalsIgnoreCase( "locked" ) )
        {

            getContext().info( "Modifying lock status to 'locked'" );

            //Read back the label spec from perforce,
            //Replace Options
            //Submit back to Perforce

            m_labelSpec = new StringBuffer();
            execP4Command( "label -o " + m_name, null );
            final String labelSpec = m_labelSpec.toString();
            getContext().debug( labelSpec );

            //reset labelSpec to null so output is not written to it anymore
            m_labelSpec = null;

            getContext().debug( "Now locking label..." );
            //handler.setOutput( labelSpec );
            execP4Command( "label -i", null );
        }
    }

    private void validate()
    {
        if( m_p4View == null || m_p4View.length() < 1 )
        {
            getContext().warn( "View not set, assuming //depot/..." );
            m_p4View = "//depot/...";
        }

        if( m_description == null || m_description.length() < 1 )
        {
            getContext().warn( "Label Description not set, assuming 'AntLabel'" );
            m_description = "AntLabel";
        }

        if( m_lock != null && !m_lock.equalsIgnoreCase( "locked" ) )
        {
            getContext().warn( "lock attribute invalid - ignoring" );
        }

        if( m_name == null || m_name.length() < 1 )
        {
            SimpleDateFormat formatter = new SimpleDateFormat( "yyyy.MM.dd-hh:mm" );
            Date now = new Date();
            m_name = "AntLabel-" + formatter.format( now );
            getContext().warn( "name not set, assuming '" + m_name + "'" );
        }
    }
}
