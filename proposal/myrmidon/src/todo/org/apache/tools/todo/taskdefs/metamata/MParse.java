/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.todo.taskdefs.metamata;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Random;
import org.apache.avalon.excalibur.io.IOUtil;
import org.apache.myrmidon.api.AbstractTask;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.framework.java.ExecuteJava;
import org.apache.tools.todo.types.Argument;
import org.apache.myrmidon.framework.file.Path;
import org.apache.tools.todo.types.PathUtil;

/**
 * Simple Metamata MParse task based on the original written by <a
 * href="mailto:thomas.haas@softwired-inc.com">Thomas Haas</a> This version was
 * written for Metamata 2.0 available at <a href="http://www.metamata.com">
 * http://www.metamata.com</a>
 *
 * @author <a href="mailto:sbailliez@imediation.com">Stephane Bailliez</a>
 */
public class MParse
    extends AbstractTask
{
    private Path m_classpath = new Path();
    private Path m_sourcepath = new Path();
    private File m_metahome;
    private File m_target;
    private boolean m_verbose;
    private boolean m_debugparser;
    private boolean m_debugscanner;
    private boolean m_cleanup;
    private ExecuteJava m_exe = new ExecuteJava();
    private File m_optionsFile;

    /**
     * create a temporary file in the current directory
     *
     * @return Description of the Returned Value
     */
    protected static final File createTmpFile()
    {
        // must be compatible with JDK 1.1 !!!!
        final long rand = ( new Random( System.currentTimeMillis() ) ).nextLong();
        File file = new File( "metamata" + rand + ".tmp" );
        return file;
    }

    /**
     * set the hack to cleanup the temp file
     *
     * @param value The new Cleanup value
     */
    public void setCleanup( boolean value )
    {
        m_cleanup = value;
    }

    /**
     * set parser debug mode
     *
     * @param flag The new Debugparser value
     */
    public void setDebugparser( boolean flag )
    {
        m_debugparser = flag;
    }

    /**
     * set scanner debug mode
     *
     * @param flag The new Debugscanner value
     */
    public void setDebugscanner( boolean flag )
    {
        m_debugscanner = flag;
    }

    /**
     * -mx or -Xmx depending on VM version
     *
     * @param max The new Maxmemory value
     */
    public void setMaxmemory( String max )
    {
        m_exe.setMaxMemory( max );
    }

    /**
     * location of metamata dev environment
     *
     * @param metamatahome The new Metamatahome value
     */
    public void setMetamatahome( File metamatahome )
    {
        m_metahome = metamatahome;
    }

    /**
     * the .jj file to process
     *
     * @param target The new Target value
     */
    public void setTarget( File target )
    {
        m_target = target;
    }

    /**
     * set verbose mode
     *
     * @param flag The new Verbose value
     */
    public void setVerbose( boolean flag )
    {
        m_verbose = flag;
    }

    /**
     * create a classpath entry
     *
     */
    public void addClasspath( final Path path )
    {
        m_classpath.addPath( path );
    }

    /**
     * Creates a nested jvmarg element.
     */
    public void addJvmarg( final Argument argument )
    {
        m_exe.getVmArguments().addArgument( argument );
    }

    /**
     * creates a sourcepath entry
     */
    public void addSourcepath( final Path path )
    {
        m_sourcepath.addPath( path );
    }

    /**
     * execute the command line
     *
     * @exception TaskException Description of Exception
     */
    public void execute()
        throws TaskException
    {
        try
        {
            setUp();
            doExecute();
        }
        finally
        {
            cleanUp();
        }
    }

    /**
     * check the options and build the command line
     *
     * @exception TaskException Description of Exception
     */
    protected void setUp()
        throws TaskException
    {
        checkOptions();

        // set the classpath as the jar files
        File[] jars = getMetamataLibs();
        final Path classPath = m_exe.getClassPath();
        for( int i = 0; i < jars.length; i++ )
        {
            classPath.addLocation( jars[ i ] );
        }

        // set the metamata.home property
        m_exe.getSysProperties().addVariable( "metamata.home",  m_metahome.getAbsolutePath() );

        // write all the options to a temp file and use it ro run the process
        String[] options = getOptions();
        m_optionsFile = createTmpFile();
        generateOptionsFile( m_optionsFile, options );
        m_exe.getArguments().addArgument( "-arguments" );
        m_exe.getArguments().addArgument( m_optionsFile );
    }

    /**
     * return an array of files containing the path to the needed libraries to
     * run metamata. The file are not checked for existence. You should do this
     * yourself if needed or simply let the forked process do it for you.
     *
     * @return array of jars/zips needed to run metamata.
     */
    protected File[] getMetamataLibs()
    {
        final ArrayList files = new ArrayList();
        files.add( new File( m_metahome, "lib/metamata.jar" ) );
        files.add( new File( m_metahome, "bin/lib/JavaCC.zip" ) );

        return (File[])files.toArray( new File[ files.size() ] );
    }

    /**
     * return all options of the command line as string elements
     *
     * @return The Options value
     */
    protected String[] getOptions() throws TaskException
    {
        ArrayList options = new ArrayList();
        if( m_verbose )
        {
            options.add( "-verbose" );
        }
        if( m_debugscanner )
        {
            options.add( "-ds" );
        }
        if( m_debugparser )
        {
            options.add( "-dp" );
        }
        final String[] classpath = m_classpath.listFiles( getContext() );
        if( classpath.length > 0 )
        {
            options.add( "-classpath" );
            options.add( PathUtil.formatPath( classpath ) );
        }
        final String[] sourcepath = m_sourcepath.listFiles( getContext() );
        if( sourcepath.length > 0 )
        {
            options.add( "-sourcepath" );
            options.add( PathUtil.formatPath( sourcepath ) );
        }
        options.add( m_target.getAbsolutePath() );

        return (String[])options.toArray( new String[ options.size() ] );
    }

    /**
     * execute the process with a specific handler
     */
    protected void doExecute()
        throws TaskException
    {
        // target has been checked as a .jj, see if there is a matching
        // java file and if it is needed to run to process the grammar
        String pathname = m_target.getAbsolutePath();
        int pos = pathname.length() - ".jj".length();
        pathname = pathname.substring( 0, pos ) + ".java";
        File javaFile = new File( pathname );
        if( javaFile.exists() && m_target.lastModified() < javaFile.lastModified() )
        {
            getContext().info( "Target is already build - skipping (" + m_target + ")" );
            return;
        }

        m_exe.setClassName( "com.metamata.jj.MParse" );
        m_exe.executeForked( getContext() );
    }

    /**
     * validate options set and resolve files and paths
     *
     * @throws TaskException thrown if an option has an incorrect state.
     */
    protected void checkOptions()
        throws TaskException
    {
        // check that the home is ok.
        if( m_metahome == null || !m_metahome.exists() )
        {
            throw new TaskException( "'metamatahome' must point to Metamata home directory." );
        }
        m_metahome = getContext().resolveFile( m_metahome.getPath() );

        // check that the needed jar exists.
        File[] jars = getMetamataLibs();
        for( int i = 0; i < jars.length; i++ )
        {
            if( !jars[ i ].exists() )
            {
                throw new TaskException( jars[ i ] + " does not exist. Check your metamata installation." );
            }
        }

        // check that the target is ok and resolve it.
        if( m_target == null || !m_target.isFile() || !m_target.getName().endsWith( ".jj" ) )
        {
            throw new TaskException( "Invalid target: " + m_target );
        }
        m_target = getContext().resolveFile( m_target.getPath() );
    }

    /**
     * clean up all the mess that we did with temporary objects
     */
    protected void cleanUp()
    {
        if( m_optionsFile != null )
        {
            m_optionsFile.delete();
            m_optionsFile = null;
        }
        if( m_cleanup )
        {
            String name = m_target.getName();
            int pos = name.length() - ".jj".length();
            name = "__jj" + name.substring( 0, pos ) + ".sunjj";
            final File sunjj = new File( m_target.getParent(), name );
            if( sunjj.exists() )
            {
                getContext().info( "Removing stale file: " + sunjj.getName() );
                sunjj.delete();
            }
        }
    }

    /**
     * write all options to a file with one option / line
     *
     * @param tofile the file to write the options to.
     * @param options the array of options element to write to the file.
     * @throws TaskException thrown if there is a problem while writing to the
     *      file.
     */
    protected void generateOptionsFile( File tofile, String[] options )
        throws TaskException
    {
        FileWriter fw = null;
        try
        {
            fw = new FileWriter( tofile );
            PrintWriter pw = new PrintWriter( fw );
            for( int i = 0; i < options.length; i++ )
            {
                pw.println( options[ i ] );
            }
            pw.flush();
        }
        catch( IOException e )
        {
            throw new TaskException( "Error while writing options file " + tofile, e );
        }
        finally
        {
            IOUtil.shutdownWriter( fw );
        }
    }
}
