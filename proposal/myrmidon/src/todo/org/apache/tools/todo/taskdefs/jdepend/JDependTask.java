/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.todo.taskdefs.jdepend;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import org.apache.myrmidon.api.AbstractTask;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.framework.java.ExecuteJava;
import org.apache.myrmidon.framework.file.Path;

/**
 * Ant task to run JDepend tests. <p>
 *
 * JDepend is a tool to generate design quality metrics for each Java package.
 * It has been initially created by Mike Clark. JDepend can be found at <a
 * href="http://www.clarkware.com/software/JDepend.html">
 * http://www.clarkware.com/software/JDepend.html</a> . The current
 * implementation spawn a new Java VM.
 *
 * @author <a href="mailto:Jerome@jeromelacoste.com">Jerome Lacoste</a>
 * @author <a href="mailto:roxspring@yahoo.com">Rob Oxspring</a>
 */
public class JDependTask
    extends AbstractTask
{
    private boolean m_fork;
    private String m_jvm;
    private String m_format = "text";
    private Path m_compileClasspath = new Path();
    private File m_dir;
    private File m_outputFile;
    private Path m_sourcesPath;

    /**
     * Set the classpath to be used for this compilation.
     */
    public void setClasspath( final Path classpath )
        throws TaskException
    {
        addClasspath( classpath );
    }

    /**
     * The directory to invoke the VM in. Ignored if no JVM is forked.
     *
     * @param dir the directory to invoke the JVM from.
     * @see #setFork(boolean)
     */
    public void setDir( final File dir )
    {
        m_dir = dir;
    }

    /**
     * Tells whether a JVM should be forked for the task. Default: false.
     *
     * @param fork <tt>true</tt> if a JVM should be forked, otherwise <tt>false
     *      <tt>
     */
    public void setFork( final boolean fork )
    {
        m_fork = fork;
    }

    public void setFormat( final FormatAttribute format )
    {
        m_format = format.getValue();
    }

    /**
     * Set a new VM to execute the task. Default is <tt>java</tt> . Ignored if
     * no JVM is forked.
     *
     * @param jvm the new VM to use instead of <tt>java</tt>
     * @see #setFork(boolean)
     */
    public void setJvm( final String jvm )
    {
        m_jvm = jvm;
    }

    /*
     * public void setTimeout(Integer value) {
     * _timeout = value;
     * }
     * public Integer getTimeout() {
     * return _timeout;
     * }
     */
    public void setOutputFile( final File outputFile )
    {
        m_outputFile = outputFile;
    }

    /**
     * Adds a nested classpath element.
     */
    public void addClasspath( final Path path )
    {
        m_compileClasspath.add( path );
    }

    /**
     * Maybe creates a nested classpath element.
     */
    public Path createSourcespath()
    {
        if( m_sourcesPath == null )
        {
            m_sourcesPath = new Path();
        }
        Path path1 = m_sourcesPath;
        final Path path = new Path();
        path1.add( path );
        return path;
    }

    public void execute()
        throws TaskException
    {
        if( m_sourcesPath == null )
        {
            throw new TaskException( "Missing Sourcepath required argument" );
        }

        // execute the test and get the return code
        if( !m_fork )
        {
            executeInVM();
        }
        else
        {
            executeAsForked();
        }
    }


    /**
     * Execute the task by forking a new JVM. The command will block until it
     * finishes. To know if the process was destroyed or not, use the <tt>
     * killedProcess()</tt> method of the watchdog class.
     */
    // JL: comment extracted from JUnitTask (and slightly modified)
    private void executeAsForked()
        throws TaskException
    {
        final ExecuteJava exe = new ExecuteJava();
        exe.setWorkingDirectory( m_dir );

        if( "text".equals( m_format ) )
        {
            exe.setClassName( "jdepend.textui.JDepend" );
        }
        else
        {
            exe.setClassName( "jdepend.xmlui.JDepend" );
        }

        if( m_jvm != null )
        {
            exe.setJvm( m_jvm );
        }

        exe.getClassPath().add( m_compileClasspath );

        if( m_outputFile != null )
        {
            // having a space between the file and its path causes commandline to add quotes "
            // around the argument thus making JDepend not taking it into account. Thus we split it in two
            exe.getArguments().addArgument( "-file" );
            exe.getArguments().addArgument( m_outputFile );
            getContext().info( "Output to be stored in " + m_outputFile.getPath() );
        }

        final String[] elements = m_sourcesPath.listFiles( getContext() );
        for( int i = 0; i < elements.length; i++ )
        {
            File f = new File( elements[ i ] );

            // not necessary as JDepend would fail, but why loose some time?
            if( !f.exists() || !f.isDirectory() )
            {
                throw new TaskException( "\"" + f.getPath() + "\" does not represent a valid directory. JDepend would fail." );
            }
            exe.getArguments().addArgument( f );
        }

        exe.executeForked( getContext() );
    }


    // this comment extract from JUnit Task may also apply here
    // "in VM is not very nice since it could probably hang the
    // whole build. IMHO this method should be avoided and it would be best
    // to remove it in future versions. TBD. (SBa)"

    /**
     * Execute inside VM.
     */
    private void executeInVM()
        throws TaskException
    {
        jdepend.textui.JDepend jdepend;

        if( "xml".equals( m_format ) )
        {
            jdepend = new jdepend.xmlui.JDepend();
        }
        else
        {
            jdepend = new jdepend.textui.JDepend();
        }

        if( m_outputFile != null )
        {
            FileWriter fw;
            try
            {
                fw = new FileWriter( m_outputFile.getPath() );
            }
            catch( IOException e )
            {
                String msg = "JDepend Failed when creating the output file: " + e.getMessage();
                throw new TaskException( msg );
            }
            jdepend.setWriter( new PrintWriter( fw ) );
            getContext().info( "Output to be stored in " + m_outputFile.getPath() );
        }

        final String[] elements = m_sourcesPath.listFiles( getContext() );
        for( int i = 0; i < elements.length; i++ )
        {
            File f = new File( elements[ i ] );

            // not necessary as JDepend would fail, but why loose some time?
            if( !f.exists() || !f.isDirectory() )
            {
                String msg = "\"" + f.getPath() + "\" does not represent a valid directory. JDepend would fail.";
                throw new TaskException( msg );
            }
            try
            {
                jdepend.addDirectory( f.getPath() );
            }
            catch( IOException e )
            {
                String msg = "JDepend Failed when adding a source directory: " + e.getMessage();
                throw new TaskException( msg );
            }
        }
        jdepend.analyze();
    }
}
