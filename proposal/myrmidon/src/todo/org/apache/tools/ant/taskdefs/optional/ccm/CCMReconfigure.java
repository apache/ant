/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs.optional.ccm;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Commandline;


/**
 * Task allows to reconfigure a project, recurcively or not
 *
 * @author Benoit Moussaud benoit.moussaud@criltelecom.com
 */
public class CCMReconfigure extends Continuus
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

    private String project = null;
    private boolean recurse = false;
    private boolean verbose = false;

    public CCMReconfigure()
    {
        super();
        setCcmAction( COMMAND_RECONFIGURE );
    }

    /**
     * Set the value of project.
     *
     * @param v Value to assign to project.
     */
    public void setCcmProject( String v )
    {
        this.project = v;
    }

    /**
     * Set the value of recurse.
     *
     * @param v Value to assign to recurse.
     */
    public void setRecurse( boolean v )
    {
        this.recurse = v;
    }

    /**
     * Set the value of verbose.
     *
     * @param v Value to assign to verbose.
     */
    public void setVerbose( boolean v )
    {
        this.verbose = v;
    }

    /**
     * Get the value of project.
     *
     * @return value of project.
     */
    public String getCcmProject()
    {
        return project;
    }


    /**
     * Get the value of recurse.
     *
     * @return value of recurse.
     */
    public boolean isRecurse()
    {
        return recurse;
    }


    /**
     * Get the value of verbose.
     *
     * @return value of verbose.
     */
    public boolean isVerbose()
    {
        return verbose;
    }


    /**
     * Executes the task. <p>
     *
     * Builds a command line to execute ccm and then calls Exec's run method to
     * execute the command line. </p>
     *
     * @exception BuildException Description of Exception
     */
    public void execute()
        throws BuildException
    {
        Commandline commandLine = new Commandline();
        Project aProj = getProject();
        int result = 0;

        // build the command line from what we got the format
        // as specified in the CCM.EXE help
        commandLine.setExecutable( getCcmCommand() );
        commandLine.createArgument().setValue( getCcmAction() );

        checkOptions( commandLine );

        result = run( commandLine );
        if( result != 0 )
        {
            String msg = "Failed executing: " + commandLine.toString();
            throw new BuildException( msg, location );
        }
    }


    /**
     * Check the command line options.
     *
     * @param cmd Description of Parameter
     */
    private void checkOptions( Commandline cmd )
    {

        if( isRecurse() == true )
        {
            cmd.createArgument().setValue( FLAG_RECURSE );
        }// end of if ()

        if( isVerbose() == true )
        {
            cmd.createArgument().setValue( FLAG_VERBOSE );
        }// end of if ()

        if( getCcmProject() != null )
        {
            cmd.createArgument().setValue( FLAG_PROJECT );
            cmd.createArgument().setValue( getCcmProject() );
        }

    }

}

