/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs.optional;
import java.io.File;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.MatchingTask;
import org.apache.tools.ant.taskdefs.Move;
import org.apache.tools.ant.types.Mapper;

/**
 * @author dIon Gillard <a href="mailto:dion@multitask.com.au">
 *      dion@multitask.com.au</a>
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 * @version 1.2
 */
public class RenameExtensions extends MatchingTask
{

    private String fromExtension = "";
    private String toExtension = "";
    private boolean replace = false;

    private Mapper.MapperType globType;
    private File srcDir;


    /**
     * Creates new RenameExtensions
     */
    public RenameExtensions()
    {
        super();
        globType = new Mapper.MapperType();
        globType.setValue( "glob" );
    }

    /**
     * store fromExtension *
     *
     * @param from The new FromExtension value
     */
    public void setFromExtension( String from )
    {
        fromExtension = from;
    }

    /**
     * store replace attribute - this determines whether the target file should
     * be overwritten if present
     *
     * @param replace The new Replace value
     */
    public void setReplace( boolean replace )
    {
        this.replace = replace;
    }

    /**
     * Set the source dir to find the files to be renamed.
     *
     * @param srcDir The new SrcDir value
     */
    public void setSrcDir( File srcDir )
    {
        this.srcDir = srcDir;
    }

    /**
     * store toExtension *
     *
     * @param to The new ToExtension value
     */
    public void setToExtension( String to )
    {
        toExtension = to;
    }

    /**
     * Executes the task, i.e. does the actual compiler call
     *
     * @exception BuildException Description of Exception
     */
    public void execute()
        throws BuildException
    {

        // first off, make sure that we've got a from and to extension
        if( fromExtension == null || toExtension == null || srcDir == null )
        {
            throw new BuildException( "srcDir, fromExtension and toExtension " +
                "attributes must be set!" );
        }

        log( "DEPRECATED - The renameext task is deprecated.  Use move instead.",
            Project.MSG_WARN );
        log( "Replace this with:", Project.MSG_INFO );
        log( "<move todir=\"" + srcDir + "\" overwrite=\"" + replace + "\">",
            Project.MSG_INFO );
        log( "  <fileset dir=\"" + srcDir + "\" />", Project.MSG_INFO );
        log( "  <mapper type=\"glob\"", Project.MSG_INFO );
        log( "          from=\"*" + fromExtension + "\"", Project.MSG_INFO );
        log( "          to=\"*" + toExtension + "\" />", Project.MSG_INFO );
        log( "</move>", Project.MSG_INFO );
        log( "using the same patterns on <fileset> as you\'ve used here",
            Project.MSG_INFO );

        Move move = ( Move )project.createTask( "move" );
        move.setOwningTarget( target );
        move.setTaskName( getTaskName() );
        move.setLocation( getLocation() );
        move.setTodir( srcDir );
        move.setOverwrite( replace );

        fileset.setDir( srcDir );
        move.addFileset( fileset );

        Mapper me = move.createMapper();
        me.setType( globType );
        me.setFrom( "*" + fromExtension );
        me.setTo( "*" + toExtension );

        move.execute();
    }

}
