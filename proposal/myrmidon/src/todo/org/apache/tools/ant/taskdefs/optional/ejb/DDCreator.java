/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs.optional.ejb;

import java.io.File;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.taskdefs.Java;
import org.apache.tools.ant.taskdefs.MatchingTask;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Argument;

/**
 * Build a serialised deployment descriptor given a text file description of the
 * descriptor in the format supported by WebLogic. This ant task is a front end
 * for the weblogic DDCreator tool.
 *
 * @author <a href="mailto:conor@cortexebusiness.com.au">Conor MacNeill</a> ,
 *      Cortex ebusiness Pty Limited
 */
public class DDCreator extends MatchingTask
{

    /**
     * The classpath to be used in the weblogic ejbc calls. It must contain the
     * weblogic classes necessary fro DDCreator <b>and</b> the implementation
     * classes of the home and remote interfaces.
     */
    private String classpath;
    /**
     * The root directory of the tree containing the textual deployment
     * desciptors. The actual deployment descriptor files are selected using
     * include and exclude constructs on the EJBC task, as supported by the
     * MatchingTask superclass.
     */
    private File descriptorDirectory;

    /**
     * The directory where generated serialised deployment descriptors are
     * placed.
     */
    private File generatedFilesDirectory;

    /**
     * Set the classpath to be used for this compilation.
     *
     * @param s the classpath to use for the ddcreator tool.
     */
    public void setClasspath( String s )
    {
        this.classpath = getProject().translatePath( s );
    }

    /**
     * Set the directory from where the text descriptions of the deployment
     * descriptors are to be read.
     *
     * @param dirName the name of the directory containing the text deployment
     *      descriptor files.
     */
    public void setDescriptors( String dirName )
    {
        descriptorDirectory = new File( dirName );
    }

    /**
     * Set the directory into which the serialised deployment descriptors are to
     * be written.
     *
     * @param dirName the name of the directory into which the serialised
     *      deployment descriptors are written.
     */
    public void setDest( String dirName )
    {
        generatedFilesDirectory = new File( dirName );
    }

    /**
     * Do the work. The work is actually done by creating a helper task. This
     * approach allows the classpath of the helper task to be set. Since the
     * weblogic tools require the class files of the project's home and remote
     * interfaces to be available in the classpath, this also avoids having to
     * start ant with the class path of the project it is building.
     *
     * @exception TaskException if someting goes wrong with the build
     */
    public void execute()
        throws TaskException
    {
        if( descriptorDirectory == null ||
            !descriptorDirectory.isDirectory() )
        {
            throw new TaskException( "descriptors directory " + descriptorDirectory.getPath() +
                                     " is not valid" );
        }
        if( generatedFilesDirectory == null ||
            !generatedFilesDirectory.isDirectory() )
        {
            throw new TaskException( "dest directory " + generatedFilesDirectory.getPath() +
                                     " is not valid" );
        }

        String args = descriptorDirectory + " " + generatedFilesDirectory;

        // get all the files in the descriptor directory
        DirectoryScanner ds = super.getDirectoryScanner( descriptorDirectory );

        String[] files = ds.getIncludedFiles();

        for( int i = 0; i < files.length; ++i )
        {
            args += " " + files[ i ];
        }

        String systemClassPath = System.getProperty( "java.class.path" );
        String execClassPath = getProject().translatePath( systemClassPath + ":" + classpath );
        Java ddCreatorTask = (Java)getProject().createTask( "java" );
        ddCreatorTask.setFork( true );
        ddCreatorTask.setClassname( "org.apache.tools.ant.taskdefs.optional.ejb.DDCreatorHelper" );
        Argument arguments = ddCreatorTask.createArg();
        arguments.setLine( args );
        ddCreatorTask.setClasspath( new Path( getProject(), execClassPath ) );
        if( ddCreatorTask.executeJava() != 0 )
        {
            throw new TaskException( "Execution of ddcreator helper failed" );
        }
    }
}
