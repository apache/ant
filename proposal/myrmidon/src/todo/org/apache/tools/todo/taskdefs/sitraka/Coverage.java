/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.todo.taskdefs.sitraka;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import org.apache.myrmidon.api.AbstractTask;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.framework.Execute;
import org.apache.tools.todo.types.Argument;
import org.apache.tools.todo.types.Commandline;
import org.apache.tools.todo.types.FileSet;
import org.apache.myrmidon.framework.file.Path;
import org.apache.tools.todo.types.PathUtil;

/**
 * Convenient task to run Sitraka JProbe Coverage from Ant. Options are pretty
 * numerous, you'd better check the manual for a full descriptions of options.
 * (not that simple since they differ from the online help, from the usage
 * command line and from the examples...) <p>
 *
 * For additional information, visit <a href="http://www.sitraka.com">
 * www.sitraka.com</a>
 *
 * @author <a href="sbailliez@imediation.com">Stephane Bailliez</a>
 */
public class Coverage
    extends AbstractTask
{
    /**
     * this is a somewhat annoying thing, set it to never
     */
    private String m_exitPrompt = "never";

    private Filters m_filters = new Filters();
    private String m_finalSnapshot = "coverage";
    private String m_recordFromStart = "coverage";
    private boolean m_trackNatives;
    private int m_warnLevel = 0;
    private ArrayList m_filesets = new ArrayList();
    private File m_home;
    private File m_inputFile;
    private File m_javaExe;
    private String m_seedName;
    private File m_snapshotDir;
    private Socket m_socket;
    private Triggers m_triggers;
    private String m_vm;
    private File m_workingDir;
    private String m_className;
    private Commandline m_args = new Commandline();
    private Path m_classpath = new Path();
    private Commandline m_vmArgs = new Commandline();

    /**
     * classname to run as standalone or runner for filesets
     *
     * @param value The new Classname value
     */
    public void setClassname( String value )
    {
        m_className = value;
    }

    /**
     * always, error, never
     *
     * @param value The new Exitprompt value
     */
    public void setExitprompt( String value )
    {
        m_exitPrompt = value;
    }

    /**
     * none, coverage, all. can be null, default to none
     *
     * @param value The new Finalsnapshot value
     */
    public void setFinalsnapshot( String value )
    {
        m_finalSnapshot = value;
    }

    /**
     * set the coverage home directory where are libraries, jars and jplauncher
     *
     * @param value The new Home value
     */
    public void setHome( File value )
    {
        m_home = value;
    }

    public void setInputfile( File value )
    {
        m_inputFile = value;
    }

    public void setJavaexe( File value )
    {
        m_javaExe = value;
    }

    /**
     * all, coverage, none
     *
     * @param value The new Recordfromstart value
     */
    public void setRecordfromstart( Recordfromstart value )
    {
        m_recordFromStart = value.getValue();
    }

    /**
     * seed name for snapshot file. can be null, default to snap
     *
     * @param value The new Seedname value
     */
    public void setSeedname( String value )
    {
        m_seedName = value;
    }

    public void setSnapshotdir( File value )
    {
        m_snapshotDir = value;
    }

    public void setTracknatives( boolean value )
    {
        m_trackNatives = value;
    }

    /**
     * jdk117, jdk118 or java2, can be null, default to java2
     *
     * @param value The new Vm value
     */
    public void setVm( Javavm value )
    {
        m_vm = value.getValue();
    }

    public void setWarnlevel( Integer value )
    {
        m_warnLevel = value.intValue();
    }

    public void setWorkingdir( File value )
    {
        m_workingDir = value;
    }

    /**
     * the classnames to execute
     *
     * @param fs The feature to be added to the Fileset attribute
     */
    public void addFileset( FileSet fs )
    {
        m_filesets.add( fs );
    }

    /**
     * the command arguments
     */
    public void addArg( final Argument argument )
    {
        m_args.addArgument( argument );
    }

    /**
     * classpath to run the files
     */
    public void setClasspath( final Path path )
    {
        m_classpath.add( path );
    }

    public Filters createFilters()
    {
        return m_filters;
    }

    /**
     * the jvm arguments
     */
    public void addJvmarg( final Argument argument )
    {
        m_vmArgs.addArgument( argument );
    }

    public Socket createSocket()
    {
        if( m_socket == null )
        {
            m_socket = new Socket();
        }
        return m_socket;
    }

    public Triggers createTriggers()
    {
        if( m_triggers == null )
        {
            m_triggers = new Triggers();
        }
        return m_triggers;
    }

    /**
     * execute the jplauncher by providing a parameter file
     *
     * @exception TaskException Description of Exception
     */
    public void execute()
        throws TaskException
    {
        File paramfile = null;
        // if an input file is used, all other options are ignored...
        if( m_inputFile == null )
        {
            checkOptions();
            paramfile = createParamFile();
        }
        else
        {
            paramfile = m_inputFile;
        }
        try
        {
            // we need to run Coverage from his directory due to dll/jar issues
            final Execute exe = new Execute();
            final Commandline cmdl = exe.getCommandline();
            cmdl.setExecutable( new File( m_home, "jplauncher" ).getAbsolutePath() );
            cmdl.addArgument( "-jp_input=" + paramfile.getAbsolutePath() );

            // use the custom handler for stdin issues
            exe.setCommandline( cmdl );
            exe.execute( getContext() );
        }
        finally
        {
            //@todo should be removed once switched to JDK1.2
            if( m_inputFile == null && paramfile != null )
            {
                paramfile.delete();
            }
        }
    }

    /**
     * return the command line parameters. Parameters can either be passed to
     * the command line and stored to a file (then use the
     * -jp_input=&lt;filename&gt;) if they are too numerous.
     *
     * @return The Parameters value
     */
    protected String[] getParameters()
        throws TaskException
    {
        Commandline params = new Commandline();
        params.addArgument( "-jp_function=coverage" );
        if( m_vm != null )
        {
            params.addArgument( "-jp_vm=" + m_vm );
        }
        if( m_javaExe != null )
        {
            params.addArgument( "-jp_java_exe=" + m_javaExe.getPath() );
        }
        params.addArgument( "-jp_working_dir=" + m_workingDir.getPath() );
        params.addArgument( "-jp_snapshot_dir=" + m_snapshotDir.getPath() );
        params.addArgument( "-jp_record_from_start=" + m_recordFromStart );
        params.addArgument( "-jp_warn=" + m_warnLevel );
        if( m_seedName != null )
        {
            params.addArgument( "-jp_output_file=" + m_seedName );
        }
        params.addArgument( "-jp_filter=" + m_filters.toString() );
        if( m_triggers != null )
        {
            params.addArgument( "-jp_trigger=" + m_triggers.toString() );
        }
        if( m_finalSnapshot != null )
        {
            params.addArgument( "-jp_final_snapshot=" + m_finalSnapshot );
        }
        params.addArgument( "-jp_exit_prompt=" + m_exitPrompt );
        //params.add("-jp_append=" + append);
        params.addArgument( "-jp_track_natives=" + m_trackNatives );
        //.... now the jvm
        // arguments
        params.addArguments( m_vmArgs );

        // classpath
        final String[] classpath = m_classpath.listFiles();
        if( classpath.length > 0 )
        {
            params.addArgument( "-classpath" );
            params.addArgument( PathUtil.formatPath( classpath ) );
        }
        // classname (runner or standalone)
        if( m_className != null )
        {
            params.addArgument( m_className );
        }
        // arguments for classname
        params.addArguments( m_args );

        return params.getArguments();
    }

    /**
     * wheck what is necessary to check, Coverage will do the job for us
     *
     * @exception TaskException Description of Exception
     */
    protected void checkOptions()
        throws TaskException
    {
        // check coverage home
        if( m_home == null || !m_home.isDirectory() )
        {
            throw new TaskException( "Invalid home directory. Must point to JProbe home directory" );
        }
        m_home = new File( m_home, "coverage" );
        File jar = new File( m_home, "coverage.jar" );
        if( !jar.exists() )
        {
            throw new TaskException( "Cannot find Coverage directory: " + m_home );
        }

        // make sure snapshot dir exists and is resolved
        if( m_snapshotDir == null )
        {
            m_snapshotDir = new File( "." );
        }
        m_snapshotDir = getContext().resolveFile( m_snapshotDir.getPath() );
        if( !m_snapshotDir.isDirectory() || !m_snapshotDir.exists() )
        {
            throw new TaskException( "Snapshot directory does not exists :" + m_snapshotDir );
        }
        if( m_workingDir == null )
        {
            m_workingDir = new File( "." );
        }
        m_workingDir = getContext().resolveFile( m_workingDir.getPath() );

        // check for info, do your best to select the java executable.
        // JProbe 3.0 fails if there is no javaexe option. So
        if( m_javaExe == null && ( m_vm == null || "java2".equals( m_vm ) ) )
        {
            String version = System.getProperty( "java.version" );
            // make we are using 1.2+, if it is, then do your best to
            // get a javaexe
            if( !version.startsWith( "1.1" ) )
            {
                if( m_vm == null )
                {
                    m_vm = "java2";
                }
                // if we are here obviously it is java2
                String home = System.getProperty( "java.home" );
                boolean isUnix = File.separatorChar == '/';
                m_javaExe = isUnix ? new File( home, "bin/java" ) : new File( home, "/bin/java.exe" );
            }
        }
    }

    /**
     * create the parameter file from the given options. The file is created
     * with a random name in the current directory.
     *
     * @return the file object where are written the configuration to run JProbe
     *      Coverage
     * @throws TaskException thrown if something bad happens while writing the
     *      arguments to the file.
     */
    protected File createParamFile()
        throws TaskException
    {
        //@todo change this when switching to JDK 1.2 and use File.createTmpFile()
        File file = File.createTempFile( "jpcoverage", "tmp" );
        getContext().debug( "Creating parameter file: " + file );

        // options need to be one per line in the parameter file
        // so write them all in a single string
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter( sw );
        String[] params = getParameters();
        for( int i = 0; i < params.length; i++ )
        {
            pw.println( params[ i ] );
        }
        pw.flush();
        getContext().debug( "JProbe Coverage parameters:\n" + sw.toString() );

        // now write them to the file
        FileWriter fw = null;
        try
        {
            fw = new FileWriter( file );
            fw.write( sw.toString() );
            fw.flush();
        }
        catch( IOException e )
        {
            throw new TaskException( "Could not write parameter file " + file, e );
        }
        finally
        {
            if( fw != null )
            {
                try
                {
                    fw.close();
                }
                catch( IOException ignored )
                {
                }
            }
        }
        return file;
    }
}
