/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.optional.jdepend;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.PathTokenizer;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.exec.Execute2;
import org.apache.tools.ant.types.Argument;
import org.apache.tools.ant.types.CommandlineJava;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;

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
public class JDependTask extends Task
{

    /**
     * No problems with this test.
     */
    private final static int SUCCESS = 0;
    /**
     * An error occured.
     */
    private final static int ERRORS = 1;
    private boolean _haltonerror = false;
    private boolean _fork = false;
    //private Integer _timeout = null;

    private String _jvm = null;
    private String format = "text";
    private Path _compileClasspath;
    private File _dir;

    // optional attributes
    private File _outputFile;
    //private CommandlineJava commandline = new CommandlineJava();

    // required attributes
    private Path _sourcesPath;

    public JDependTask()
    {
    }

    /**
     * Set the classpath to be used for this compilation.
     *
     * @param classpath The new Classpath value
     */
    public void setClasspath( Path classpath )
    {
        if( _compileClasspath == null )
        {
            _compileClasspath = classpath;
        }
        else
        {
            _compileClasspath.append( classpath );
        }
    }

    /**
     * Adds a reference to a CLASSPATH defined elsewhere.
     *
     * @param r The new ClasspathRef value
     */
    public void setClasspathRef( Reference r )
    {
        createClasspath().setRefid( r );
    }

    /**
     * The directory to invoke the VM in. Ignored if no JVM is forked.
     *
     * @param dir the directory to invoke the JVM from.
     * @see #setFork(boolean)
     */
    public void setDir( File dir )
    {
        _dir = dir;
    }

    /**
     * Tells whether a JVM should be forked for the task. Default: false.
     *
     * @param value <tt>true</tt> if a JVM should be forked, otherwise <tt>false
     *      <tt>
     */
    public void setFork( boolean value )
    {
        _fork = value;
    }

    public void setFormat( FormatAttribute ea )
    {
        format = ea.getValue();
    }

    /**
     * Halt on Failure? default: false.
     *
     * @param value The new Haltonerror value
     */
    public void setHaltonerror( boolean value )
    {
        _haltonerror = value;
    }

    /**
     * Set a new VM to execute the task. Default is <tt>java</tt> . Ignored if
     * no JVM is forked.
     *
     * @param value the new VM to use instead of <tt>java</tt>
     * @see #setFork(boolean)
     */
    public void setJvm( String value )
    {
        _jvm = value;

    }

    /*
     * public void setTimeout(Integer value) {
     * _timeout = value;
     * }
     * public Integer getTimeout() {
     * return _timeout;
     * }
     */
    public void setOutputFile( File outputFile )
    {
        _outputFile = outputFile;
    }

    /**
     * Gets the classpath to be used for this compilation.
     *
     * @return The Classpath value
     */
    public Path getClasspath()
    {
        return _compileClasspath;
    }

    public File getDir()
    {
        return _dir;
    }

    public boolean getFork()
    {
        return _fork;
    }

    public boolean getHaltonerror()
    {
        return _haltonerror;
    }

    public File getOutputFile()
    {
        return _outputFile;
    }

    /**
     * Gets the sourcepath.
     *
     * @return The Sourcespath value
     */
    public Path getSourcespath()
    {
        return _sourcesPath;
    }

    /**
     * Maybe creates a nested classpath element.
     *
     * @return Description of the Returned Value
     */
    public Path createClasspath()
    {
        if( _compileClasspath == null )
        {
            _compileClasspath = new Path();
        }
        return _compileClasspath.createPath();
    }

    /**
     * Create a new JVM argument. Ignored if no JVM is forked.
     *
     * @param commandline Description of Parameter
     * @return create a new JVM argument so that any argument can be passed to
     *      the JVM.
     * @see #setFork(boolean)
     */
    public Argument createJvmarg( CommandlineJava commandline )
    {
        return commandline.createVmArgument();
    }

    /**
     * Maybe creates a nested classpath element.
     *
     * @return Description of the Returned Value
     */
    public Path createSourcespath()
    {
        if( _sourcesPath == null )
        {
            _sourcesPath = new Path();
        }
        return _sourcesPath.createPath();
    }

    public void execute()
        throws TaskException
    {

        CommandlineJava commandline = new CommandlineJava();

        if( "text".equals( format ) )
            commandline.setClassname( "jdepend.textui.JDepend" );
        else if( "xml".equals( format ) )
            commandline.setClassname( "jdepend.xmlui.JDepend" );

        if( _jvm != null )
            commandline.setVm( _jvm );

        if( getSourcespath() == null )
            throw new TaskException( "Missing Sourcepath required argument" );

        // execute the test and get the return code
        int exitValue = JDependTask.ERRORS;
        boolean wasKilled = false;
        if( !getFork() )
        {
            exitValue = executeInVM( commandline );
        }
        else
        {
            exitValue = executeAsForked( commandline );
        }

        // if there is an error/failure and that it should halt, stop everything otherwise
        // just log a statement
        boolean errorOccurred = exitValue == JDependTask.ERRORS;

        if( errorOccurred )
        {
            if( getHaltonerror() )
                throw new TaskException( "JDepend failed" );
            else
                getLogger().error( "JDepend FAILED" );
        }
    }


    /**
     * Execute the task by forking a new JVM. The command will block until it
     * finishes. To know if the process was destroyed or not, use the <tt>
     * killedProcess()</tt> method of the watchdog class.
     *
     * @param watchdog the watchdog in charge of cancelling the test if it
     *      exceeds a certain amount of time. Can be <tt>null</tt> , in this
     *      case the test could probably hang forever.
     * @param commandline Description of Parameter
     * @return Description of the Returned Value
     * @exception TaskException Description of Exception
     */
    // JL: comment extracted from JUnitTask (and slightly modified)
    public int executeAsForked( CommandlineJava commandline )
        throws TaskException
    {
        // if not set, auto-create the ClassPath from the project
        createClasspath();

        // not sure whether this test is needed but cost nothing to put.
        // hope it will be reviewed by anybody competent
        if( getClasspath().toString().length() > 0 )
        {
            createJvmarg( commandline ).setValue( "-classpath" );
            createJvmarg( commandline ).setValue( getClasspath().toString() );
        }

        if( getOutputFile() != null )
        {
            // having a space between the file and its path causes commandline to add quotes "
            // around the argument thus making JDepend not taking it into account. Thus we split it in two
            commandline.createArgument().setValue( "-file" );
            commandline.createArgument().setValue( _outputFile.getPath() );
            // we have to find a cleaner way to put this output
        }

        PathTokenizer sourcesPath = new PathTokenizer( getSourcespath().toString() );
        while( sourcesPath.hasMoreTokens() )
        {
            File f = new File( sourcesPath.nextToken() );

            // not necessary as JDepend would fail, but why loose some time?
            if( !f.exists() || !f.isDirectory() )
                throw new TaskException( "\"" + f.getPath() + "\" does not represent a valid directory. JDepend would fail." );
            commandline.createArgument().setValue( f.getPath() );
        }

        final Execute2 exe = new Execute2();
        setupLogger( exe );

        exe.setCommandline( commandline.getCommandline() );
        if( getDir() != null )
        {
            exe.setWorkingDirectory( getDir() );
        }

        if( getOutputFile() != null )
            getLogger().info( "Output to be stored in " + getOutputFile().getPath() );
        getLogger().debug( "Executing: " + commandline.toString() );
        try
        {
            return exe.execute();
        }
        catch( IOException e )
        {
            throw new TaskException( "Process fork failed.", e );
        }
    }


    // this comment extract from JUnit Task may also apply here
    // "in VM is not very nice since it could probably hang the
    // whole build. IMHO this method should be avoided and it would be best
    // to remove it in future versions. TBD. (SBa)"

    /**
     * Execute inside VM.
     *
     * @param commandline Description of Parameter
     * @return Description of the Returned Value
     * @exception TaskException Description of Exception
     */
    public int executeInVM( CommandlineJava commandline )
        throws TaskException
    {
        jdepend.textui.JDepend jdepend;

        if( "xml".equals( format ) )
            jdepend = new jdepend.xmlui.JDepend();
        else
            jdepend = new jdepend.textui.JDepend();

        if( getOutputFile() != null )
        {
            FileWriter fw;
            try
            {
                fw = new FileWriter( getOutputFile().getPath() );
            }
            catch( IOException e )
            {
                String msg = "JDepend Failed when creating the output file: " + e.getMessage();
                getLogger().info( msg );
                throw new TaskException( msg );
            }
            jdepend.setWriter( new PrintWriter( fw ) );
            getLogger().info( "Output to be stored in " + getOutputFile().getPath() );
        }

        PathTokenizer sourcesPath = new PathTokenizer( getSourcespath().toString() );
        while( sourcesPath.hasMoreTokens() )
        {
            File f = new File( sourcesPath.nextToken() );

            // not necessary as JDepend would fail, but why loose some time?
            if( !f.exists() || !f.isDirectory() )
            {
                String msg = "\"" + f.getPath() + "\" does not represent a valid directory. JDepend would fail.";
                getLogger().info( msg );
                throw new TaskException( msg );
            }
            try
            {
                jdepend.addDirectory( f.getPath() );
            }
            catch( IOException e )
            {
                String msg = "JDepend Failed when adding a source directory: " + e.getMessage();
                getLogger().info( msg );
                throw new TaskException( msg );
            }
        }
        jdepend.analyze();
        return SUCCESS;
    }

    public static class FormatAttribute extends EnumeratedAttribute
    {
        private String[] formats = new String[]{"xml", "text"};

        public String[] getValues()
        {
            return formats;
        }
    }
}
