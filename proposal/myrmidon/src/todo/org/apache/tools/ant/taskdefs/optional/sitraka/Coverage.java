/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs.optional.sitraka;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Random;
import java.util.Vector;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Execute;
import org.apache.tools.ant.taskdefs.LogStreamHandler;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.CommandlineJava;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Path;

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
public class Coverage extends Task
{

    protected Commandline cmdl = new Commandline();

    protected CommandlineJava cmdlJava = new CommandlineJava();

    protected String function = "coverage";

    protected boolean applet = false;

    /**
     * this is a somewhat annoying thing, set it to never
     */
    protected String exitPrompt = "never";

    protected Filters filters = new Filters();

    protected String finalSnapshot = "coverage";

    protected String recordFromStart = "coverage";

    protected boolean trackNatives = false;

    protected int warnLevel = 0;

    protected Vector filesets = new Vector();

    protected File home;

    protected File inputFile;

    protected File javaExe;

    protected String seedName;

    protected File snapshotDir;

    protected Socket socket;

    protected Triggers triggers;

    protected String vm;

    protected File workingDir;


    //---------------- the tedious job begins here

    public Coverage()
    {
    }

    /**
     * default to false unless file is htm or html
     *
     * @param value The new Applet value
     */
    public void setApplet( boolean value )
    {
        applet = value;
    }

    /**
     * classname to run as standalone or runner for filesets
     *
     * @param value The new Classname value
     */
    public void setClassname( String value )
    {
        cmdlJava.setClassname( value );
    }

    /**
     * always, error, never
     *
     * @param value The new Exitprompt value
     */
    public void setExitprompt( String value )
    {
        exitPrompt = value;
    }

    /**
     * none, coverage, all. can be null, default to none
     *
     * @param value The new Finalsnapshot value
     */
    public void setFinalsnapshot( String value )
    {
        finalSnapshot = value;
    }

    //--------- setters used via reflection --

    /**
     * set the coverage home directory where are libraries, jars and jplauncher
     *
     * @param value The new Home value
     */
    public void setHome( File value )
    {
        home = value;
    }

    public void setInputfile( File value )
    {
        inputFile = value;
    }

    public void setJavaexe( File value )
    {
        javaExe = value;
    }

    /**
     * all, coverage, none
     *
     * @param value The new Recordfromstart value
     */
    public void setRecordfromstart( Recordfromstart value )
    {
        recordFromStart = value.getValue();
    }

    /**
     * seed name for snapshot file. can be null, default to snap
     *
     * @param value The new Seedname value
     */
    public void setSeedname( String value )
    {
        seedName = value;
    }

    public void setSnapshotdir( File value )
    {
        snapshotDir = value;
    }

    public void setTracknatives( boolean value )
    {
        trackNatives = value;
    }

    /**
     * jdk117, jdk118 or java2, can be null, default to java2
     *
     * @param value The new Vm value
     */
    public void setVm( Javavm value )
    {
        vm = value.getValue();
    }

    public void setWarnlevel( Integer value )
    {
        warnLevel = value.intValue();
    }

    public void setWorkingdir( File value )
    {
        workingDir = value;
    }

    /**
     * the classnames to execute
     *
     * @param fs The feature to be added to the Fileset attribute
     */
    public void addFileset( FileSet fs )
    {
        filesets.addElement( fs );
    }

    /**
     * the command arguments
     *
     * @return Description of the Returned Value
     */
    public Commandline.Argument createArg()
    {
        return cmdlJava.createArgument();
    }

    /**
     * classpath to run the files
     *
     * @return Description of the Returned Value
     */
    public Path createClasspath()
    {
        return cmdlJava.createClasspath( project ).createPath();
    }

    public Filters createFilters()
    {
        return filters;
    }

    //

    /**
     * the jvm arguments
     *
     * @return Description of the Returned Value
     */
    public Commandline.Argument createJvmarg()
    {
        return cmdlJava.createVmArgument();
    }

    public Socket createSocket()
    {
        if( socket == null )
        {
            socket = new Socket();
        }
        return socket;
    }

    public Triggers createTriggers()
    {
        if( triggers == null )
        {
            triggers = new Triggers();
        }
        return triggers;
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
        if( inputFile == null )
        {
            checkOptions();
            paramfile = createParamFile();
        }
        else
        {
            paramfile = inputFile;
        }
        try
        {
            // we need to run Coverage from his directory due to dll/jar issues
            cmdl.setExecutable( new File( home, "jplauncher" ).getAbsolutePath() );
            cmdl.createArgument().setValue( "-jp_input=" + paramfile.getAbsolutePath() );

            // use the custom handler for stdin issues
            LogStreamHandler handler = new CoverageStreamHandler( this );
            Execute exec = new Execute( handler );
            log( cmdl.toString(), Project.MSG_VERBOSE );
            exec.setCommandline( cmdl.getCommandline() );
            int exitValue = exec.execute();
            if( exitValue != 0 )
            {
                throw new TaskException( "JProbe Coverage failed (" + exitValue + ")" );
            }
        }
        catch( IOException e )
        {
            throw new TaskException( "Failed to execute JProbe Coverage.", e );
        }
        finally
        {
            //@todo should be removed once switched to JDK1.2
            if( inputFile == null && paramfile != null )
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
        Vector params = new Vector();
        params.addElement( "-jp_function=" + function );
        if( vm != null )
        {
            params.addElement( "-jp_vm=" + vm );
        }
        if( javaExe != null )
        {
            params.addElement( "-jp_java_exe=" + resolveFile( javaExe.getPath() ) );
        }
        params.addElement( "-jp_working_dir=" + workingDir.getPath() );
        params.addElement( "-jp_snapshot_dir=" + snapshotDir.getPath() );
        params.addElement( "-jp_record_from_start=" + recordFromStart );
        params.addElement( "-jp_warn=" + warnLevel );
        if( seedName != null )
        {
            params.addElement( "-jp_output_file=" + seedName );
        }
        params.addElement( "-jp_filter=" + filters.toString() );
        if( triggers != null )
        {
            params.addElement( "-jp_trigger=" + triggers.toString() );
        }
        if( finalSnapshot != null )
        {
            params.addElement( "-jp_final_snapshot=" + finalSnapshot );
        }
        params.addElement( "-jp_exit_prompt=" + exitPrompt );
        //params.addElement("-jp_append=" + append);
        params.addElement( "-jp_track_natives=" + trackNatives );
        //.... now the jvm
        // arguments
        String[] vmargs = cmdlJava.getVmCommand().getArguments();
        for( int i = 0; i < vmargs.length; i++ )
        {
            params.addElement( vmargs[ i ] );
        }
        // classpath
        Path classpath = cmdlJava.getClasspath();
        if( classpath != null && classpath.size() > 0 )
        {
            params.addElement( "-classpath " + classpath.toString() );
        }
        // classname (runner or standalone)
        if( cmdlJava.getClassname() != null )
        {
            params.addElement( cmdlJava.getClassname() );
        }
        // arguments for classname
        String[] args = cmdlJava.getJavaCommand().getArguments();
        for( int i = 0; i < args.length; i++ )
        {
            params.addElement( args[ i ] );
        }

        String[] array = new String[ params.size() ];
        params.copyInto( array );
        return array;
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
        if( home == null || !home.isDirectory() )
        {
            throw new TaskException( "Invalid home directory. Must point to JProbe home directory" );
        }
        home = new File( home, "coverage" );
        File jar = new File( home, "coverage.jar" );
        if( !jar.exists() )
        {
            throw new TaskException( "Cannot find Coverage directory: " + home );
        }

        // make sure snapshot dir exists and is resolved
        if( snapshotDir == null )
        {
            snapshotDir = new File( "." );
        }
        snapshotDir = resolveFile( snapshotDir.getPath() );
        if( !snapshotDir.isDirectory() || !snapshotDir.exists() )
        {
            throw new TaskException( "Snapshot directory does not exists :" + snapshotDir );
        }
        if( workingDir == null )
        {
            workingDir = new File( "." );
        }
        workingDir = resolveFile( workingDir.getPath() );

        // check for info, do your best to select the java executable.
        // JProbe 3.0 fails if there is no javaexe option. So
        if( javaExe == null && ( vm == null || "java2".equals( vm ) ) )
        {
            String version = System.getProperty( "java.version" );
            // make we are using 1.2+, if it is, then do your best to
            // get a javaexe
            if( !version.startsWith( "1.1" ) )
            {
                if( vm == null )
                {
                    vm = "java2";
                }
                // if we are here obviously it is java2
                String home = System.getProperty( "java.home" );
                boolean isUnix = File.separatorChar == '/';
                javaExe = isUnix ? new File( home, "bin/java" ) : new File( home, "/bin/java.exe" );
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
        File file = createTmpFile();
        log( "Creating parameter file: " + file, Project.MSG_VERBOSE );

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
        log( "JProbe Coverage parameters:\n" + sw.toString(), Project.MSG_VERBOSE );

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

    /**
     * create a temporary file in the current dir (For JDK1.1 support)
     *
     * @return Description of the Returned Value
     */
    protected File createTmpFile()
    {
        final long rand = ( new Random( System.currentTimeMillis() ) ).nextLong();
        File file = new File( "jpcoverage" + rand + ".tmp" );
        return file;
    }

    public static class Finalsnapshot extends EnumeratedAttribute
    {
        public String[] getValues()
        {
            return new String[]{"coverage", "none", "all"};
        }
    }

    public static class Javavm extends EnumeratedAttribute
    {
        public String[] getValues()
        {
            return new String[]{"java2", "jdk118", "jdk117"};
        }
    }

    public static class Recordfromstart extends EnumeratedAttribute
    {
        public String[] getValues()
        {
            return new String[]{"coverage", "none", "all"};
        }
    }

    /**
     * specific pumper to avoid those nasty stdin issues
     *
     * @author RT
     */
    static class CoverageStreamHandler extends LogStreamHandler
    {
        CoverageStreamHandler( Task task )
        {
            super( task, Project.MSG_INFO, Project.MSG_WARN );
        }

        /**
         * there are some issues concerning all JProbe executable In our case a
         * 'Press ENTER to close this window..." will be displayed in the
         * current window waiting for enter. So I'm closing the stream right
         * away to avoid problems.
         *
         * @param os The new ProcessInputStream value
         */
        public void setProcessInputStream( OutputStream os )
        {
            try
            {
                os.close();
            }
            catch( IOException ignored )
            {
            }
        }
    }

}
