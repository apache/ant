/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.optional.ejb;

import java.io.File;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.types.DirectoryScanner;
import org.apache.tools.ant.taskdefs.Java;
import org.apache.tools.ant.taskdefs.MatchingTask;
import org.apache.tools.ant.types.Argument;
import org.apache.tools.ant.types.Path;

/**
 * Build EJB support classes using Weblogic's ejbc tool from a directory
 * containing a set of deployment descriptors.
 *
 * @author <a href="mailto:conor@cortexebusiness.com.au">Conor MacNeill</a> ,
 *      Cortex ebusiness Pty Limited
 */
public class Ejbc extends MatchingTask
{

    public boolean keepgenerated;

    /**
     * The classpath to be used in the weblogic ejbc calls. It must contain the
     * weblogic classes <b>and</b> the implementation classes of the home and
     * remote interfaces.
     */
    private String classpath;
    /**
     * The root directory of the tree containing the serialised deployment
     * desciptors. The actual deployment descriptor files are selected using
     * include and exclude constructs on the ejbc task provided by the
     * MatchingTask superclass.
     */
    private File descriptorDirectory;

    /**
     * The directory where generated files are placed.
     */
    private File generatedFilesDirectory;

    /**
     * The name of the manifest file generated for the EJB jar.
     */
    private File generatedManifestFile;

    /**
     * The source directory for the home and remote interfaces. This is used to
     * determine if the generated deployment classes are out of date.
     */
    private File sourceDirectory;

    /**
     * Set the classpath to be used for this compilation.
     *
     * @param s The new Classpath value
     */
    public void setClasspath( final Path s )
    {
        this.classpath = s.toString();
    }

    /**
     * Set the directory from where the serialised deployment descriptors are to
     * be read.
     *
     * @param dirName the name of the directory containing the serialised
     *      deployment descriptors.
     */
    public void setDescriptors( String dirName )
    {
        descriptorDirectory = new File( dirName );
    }

    /**
     * Set the directory into which the support classes, RMI stubs, etc are to
     * be written
     *
     * @param dirName the name of the directory into which code is generated
     */
    public void setDest( final File dirName )
    {
        generatedFilesDirectory = dirName;
    }

    public void setKeepgenerated( String newKeepgenerated )
    {
        keepgenerated = Boolean.valueOf( newKeepgenerated.trim() ).booleanValue();

    }

    /**
     * Set the generated manifest file. For each EJB that is processed an entry
     * is created in this file. This can then be used to create a jar file for
     * dploying the beans.
     *
     * @param manifestFilename The new Manifest value
     */
    public void setManifest( String manifestFilename )
    {
        generatedManifestFile = new File( manifestFilename );
    }

    /**
     * Set the directory containing the source code for the home interface,
     * remote interface and public key class definitions.
     *
     * @param dirName the directory containg the source tree for the EJB's
     *      interface classes.
     */
    public void setSrc( String dirName )
    {
        sourceDirectory = new File( dirName );
    }

    public boolean getKeepgenerated()
    {
        return keepgenerated;
    }

    /**
     * Do the work. The work is actually done by creating a separate JVM to run
     * a helper task. This approach allows the classpath of the helper task to
     * be set. Since the weblogic tools require the class files of the project's
     * home and remote interfaces to be available in the classpath, this also
     * avoids having to start ant with the class path of the project it is
     * building.
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

        if( sourceDirectory == null ||
            !sourceDirectory.isDirectory() )
        {
            throw new TaskException( "src directory " + sourceDirectory.getPath() +
                                     " is not valid" );
        }

        String systemClassPath = System.getProperty( "java.class.path" );
        String execClassPath =
            systemClassPath + File.separator + classpath + File.separator + generatedFilesDirectory;
        // get all the files in the descriptor directory
        DirectoryScanner ds = super.getDirectoryScanner( descriptorDirectory );

        String[] files = ds.getIncludedFiles();

        Java helperTask = (Java)getProject().createTask( "java" );
        helperTask.setFork( true );
        helperTask.setClassname( "org.apache.tools.ant.taskdefs.optional.ejb.EjbcHelper" );
        String args = "";
        args += " " + descriptorDirectory;
        args += " " + generatedFilesDirectory;
        args += " " + sourceDirectory;
        args += " " + generatedManifestFile;
        args += " " + keepgenerated;

        for( int i = 0; i < files.length; ++i )
        {
            args += " " + files[ i ];
        }

        Argument arguments = helperTask.createArg();
        arguments.setLine( args );
        helperTask.setClasspath( new Path( execClassPath ) );
        if( helperTask.executeJava() != 0 )
        {
            throw new TaskException( "Execution of ejbc helper failed" );
        }
    }
}
