/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.Task;

/**
 * Just exit the active build, giving an additional message if available.
 *
 * @author <a href="mailto:nico@seessle.de">Nico Seessle</a>
 */
public class Exit extends Task
{
    private String ifCondition, unlessCondition;
    private String message;

    public void setIf( String c )
    {
        ifCondition = c;
    }

    public void setMessage( String value )
    {
        this.message = value;
    }

    public void setUnless( String c )
    {
        unlessCondition = c;
    }

    /**
     * Set a multiline message.
     *
     * @param msg The feature to be added to the Text attribute
     */
    public void addText( String msg )
    {
        message += project.replaceProperties( msg );
    }

    public void execute()
        throws BuildException
    {
        if( testIfCondition() && testUnlessCondition() )
        {
            if( message != null && message.length() > 0 )
            {
                throw new BuildException( message );
            }
            else
            {
                throw new BuildException( "No message" );
            }
        }
    }

    private boolean testIfCondition()
    {
        if( ifCondition == null || "".equals( ifCondition ) )
        {
            return true;
        }

        return project.getProperty( ifCondition ) != null;
    }

    private boolean testUnlessCondition()
    {
        if( unlessCondition == null || "".equals( unlessCondition ) )
        {
            return true;
        }
        return project.getProperty( unlessCondition ) == null;
    }

}
