/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.todo.taskdefs.ccm;

import org.apache.myrmidon.api.TaskException;
import org.apache.tools.todo.types.Commandline;
import org.apache.tools.todo.types.ArgumentList;

/**
 * Task allows to reconfigure a project, recurcively or not
 *
 * @author Benoit Moussaud benoit.moussaud@criltelecom.com
 */
public class CCMReconfigure
    extends Continuus
{
    /**
     * /recurse --
     */
    public final static String FLAG_RECURSE = "/recurse";

    /**
     * /recurse --
     */
    public final static String FLAG_VERBOSE = "/verbose";

    /**
     * /project flag -- target project
     */
    public final static String FLAG_PROJECT = "/project";

    private String m_ccmProject;
    private boolean m_recurse;
    private boolean m_verbose;

    public CCMReconfigure()
    {
        super();
        setCcmAction( COMMAND_RECONFIGURE );
    }

    /**
     * Set the value of project.
     */
    public void setCcmProject( final String ccmProject )
    {
        m_ccmProject = ccmProject;
    }

    /**
     * Set the value of recurse.
     */
    public void setRecurse( final boolean recurse )
    {
        m_recurse = recurse;
    }

    /**
     * Set the value of verbose.
     */
    public void setVerbose( final boolean verbose )
    {
        m_verbose = verbose;
    }

    /**
     * Executes the task. <p>
     *
     * Builds a command line to execute ccm and then calls Exec's run method to
     * execute the command line. </p>
     */
    public void execute()
        throws TaskException
    {
        final Commandline cmd = new Commandline();

        // build the command line from what we got the format
        // as specified in the CCM.EXE help
        cmd.setExecutable( getCcmCommand() );
        cmd.addArgument( getCcmAction() );

        checkOptions( cmd );

        run( cmd, null );
    }

    /**
     * Build the command line options.
     */
    private void checkOptions( final ArgumentList cmd )
    {
        if( m_recurse == true )
        {
            cmd.addArgument( FLAG_RECURSE );
        }

        if( m_verbose == true )
        {
            cmd.addArgument( FLAG_VERBOSE );
        }

        if( m_ccmProject != null )
        {
            cmd.addArgument( FLAG_PROJECT );
            cmd.addArgument( m_ccmProject );
        }
    }
}

