/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.todo.taskdefs;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.StringTokenizer;
import org.apache.avalon.excalibur.util.StringUtil;
import org.apache.myrmidon.api.AbstractTask;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.todo.types.Commandline;
import org.apache.myrmidon.framework.file.Path;
import org.apache.tools.todo.types.PathUtil;

/**
 * Task to generate JNI header files using javah. This task can take the
 * following arguments:
 * <ul>
 *   <li> classname - the fully-qualified name of a class</li>
 *   <li> outputFile - Concatenates the resulting header or source files for all
 *   the classes listed into this file</li>
 *   <li> destdir - Sets the directory where javah saves the header files or the
 *   stub files</li>
 *   <li> classpath</li>
 *   <li> bootclasspath</li>
 *   <li> force - Specifies that output files should always be written (JDK1.2
 *   only)</li>
 *   <li> old - Specifies that old JDK1.0-style header files should be generated
 *   (otherwise output file contain JNI-style native method function prototypes)
 *   (JDK1.2 only)</li>
 *   <li> stubs - generate C declarations from the Java object file (used with
 *   old)</li>
 *   <li> verbose - causes javah to print a message to stdout concerning the
 *   status of the generated files</li>
 *   <li> extdirs - Override location of installed extensions</li>
 * </ul>
 * Of these arguments, either <b>outputFile</b> or <b>destdir</b> is required,
 * but not both. More than one classname may be specified, using a
 * comma-separated list or by using <code>&lt;class name="xxx"&gt;</code>
 * elements within the task. <p>
 *
 * When this task executes, it will generate C header and source files that are
 * needed to implement native methods.
 *
 * @author Rick Beton <a href="mailto:richard.beton@physics.org">
 *      richard.beton@physics.org</a>
 */

public class Javah
    extends AbstractTask
{
    private final static String FAIL_MSG = "Compile failed, messages should have been provided.";

    private ArrayList m_classes = new ArrayList( 2 );
    private Path m_classpath;
    private File m_outputFile;
    private boolean m_verbose;
    private boolean m_force;
    private boolean m_old;
    private boolean m_stubs;
    private Path m_bootclasspath;
    private String m_cls;
    private File m_destDir;

    /**
     * Adds an element to the bootclasspath.
     */
    public void addBootclasspath( final Path bootclasspath )
    {
        if( m_bootclasspath == null )
        {
            m_bootclasspath = bootclasspath;
        }
        else
        {
            m_bootclasspath.add( bootclasspath );
        }
    }

    public void setClass( final String cls )
    {
        m_cls = cls;
    }

    /**
     * Adds an element to the classpath.
     */
    public void addClasspath( final Path classpath )
        throws TaskException
    {
        if( m_classpath == null )
        {
            m_classpath = classpath;
        }
        else
        {
            m_classpath.add( classpath );
        }
    }

    /**
     * Set the destination directory into which the Java source files should be
     * compiled.
     *
     * @param destDir The new Destdir value
     */
    public void setDestdir( final File destDir )
    {
        m_destDir = destDir;
    }

    /**
     * Set the force-write flag.
     */
    public void setForce( final boolean force )
    {
        m_force = force;
    }

    /**
     * Set the old flag.
     */
    public void setOld( final boolean old )
    {
        m_old = old;
    }

    /**
     * Set the output file name.
     */
    public void setOutputFile( final File outputFile )
    {
        m_outputFile = outputFile;
    }

    /**
     * Set the stubs flag.
     */
    public void setStubs( final boolean stubs )
    {
        m_stubs = stubs;
    }

    /**
     * Set the verbose flag.
     */
    public void setVerbose( final boolean verbose )
    {
        m_verbose = verbose;
    }

    public ClassArgument createClass()
    {
        final ClassArgument ga = new ClassArgument();
        m_classes.add( ga );
        return ga;
    }

    /**
     * Executes the task.
     */
    public void execute()
        throws TaskException
    {
        validate();
        doClassicCompile();
    }

    private void validate() throws TaskException
    {
        if( ( m_cls == null ) && ( m_classes.size() == 0 ) )
        {
            final String message = "class attribute must be set!";
            throw new TaskException( message );
        }

        if( ( m_cls != null ) && ( m_classes.size() > 0 ) )
        {
            final String message = "set class attribute or class element, not both.";
            throw new TaskException( message );
        }

        if( m_destDir != null )
        {
            if( !m_destDir.isDirectory() )
            {
                final String message = "destination directory \"" + m_destDir +
                    "\" does not exist or is not a directory";
                throw new TaskException( message );
            }
            if( m_outputFile != null )
            {
                final String message = "destdir and outputFile are mutually exclusive";
                throw new TaskException( message );
            }
        }
    }

    /**
     * Logs the compilation parameters, adds the files to compile and logs the
     * &qout;niceSourceList&quot;
     */
    private void logAndAddFilesToCompile( final Commandline cmd )
    {
        int n = 0;
        getContext().debug( "Compilation args: " + cmd.toString() );

        StringBuffer niceClassList = new StringBuffer();
        if( m_cls != null )
        {
            final StringTokenizer tok = new StringTokenizer( m_cls, ",", false );
            while( tok.hasMoreTokens() )
            {
                final String aClass = tok.nextToken().trim();
                cmd.addArgument( aClass );
                niceClassList.append( "    " + aClass + StringUtil.LINE_SEPARATOR );
                n++;
            }
        }

        final Iterator enum = m_classes.iterator();
        while( enum.hasNext() )
        {
            final ClassArgument arg = (ClassArgument)enum.next();
            final String aClass = arg.getName();
            cmd.addArgument( aClass );
            niceClassList.append( "    " + aClass + StringUtil.LINE_SEPARATOR );
            n++;
        }

        final StringBuffer prefix = new StringBuffer( "Class" );
        if( n > 1 )
        {
            prefix.append( "es" );
        }
        prefix.append( " to be compiled:" );
        prefix.append( StringUtil.LINE_SEPARATOR );

        getContext().debug( prefix.toString() + niceClassList.toString() );
    }

    /**
     * Does the command line argument processing common to classic and modern.
     */
    private Commandline setupJavahCommand()
        throws TaskException
    {
        final Commandline cmd = new Commandline();

        if( m_destDir != null )
        {
            cmd.addArgument( "-d" );
            cmd.addArgument( m_destDir );
        }

        if( m_outputFile != null )
        {
            cmd.addArgument( "-o" );
            cmd.addArgument( m_outputFile );
        }

        if( m_classpath != null )
        {
            cmd.addArgument( "-classpath" );
            cmd.addArgument( PathUtil.formatPath( m_classpath, getContext() ) );
        }

        if( m_verbose )
        {
            cmd.addArgument( "-verbose" );
        }
        if( m_old )
        {
            cmd.addArgument( "-old" );
        }
        if( m_force )
        {
            cmd.addArgument( "-force" );
        }

        if( m_stubs )
        {
            if( !m_old )
            {
                final String message = "stubs only available in old mode.";
                throw new TaskException( message );
            }
            cmd.addArgument( "-stubs" );
        }
        if( m_bootclasspath != null )
        {
            cmd.addArgument( "-bootclasspath" );
            cmd.addArgument( PathUtil.formatPath( m_bootclasspath, getContext() ) );
        }

        logAndAddFilesToCompile( cmd );
        return cmd;
    }

    /**
     * Peforms a compile using the classic compiler that shipped with JDK 1.1
     * and 1.2.
     *
     * @exception org.apache.myrmidon.api.TaskException Description of Exception
     */

    private void doClassicCompile()
        throws TaskException
    {
        Commandline cmd = setupJavahCommand();

        // Use reflection to be able to build on all JDKs
        /*
         * / provide the compiler a different message sink - namely our own
         * sun.tools.javac.Main compiler =
         * new sun.tools.javac.Main(new LogOutputStream(this, Project.MSG_WARN), "javac");
         * if (!compiler.compile(cmd.getArguments())) {
         * throw new TaskException("Compile failed");
         * }
         */
        try
        {
            // Javac uses logstr to change the output stream and calls
            // the constructor's invoke method to create a compiler instance
            // dynamically. However, javah has a different interface and this
            // makes it harder, so here's a simple alternative.
            //------------------------------------------------------------------
            com.sun.tools.javah.Main main = new com.sun.tools.javah.Main( cmd.getArguments() );
            main.run();
        }
            //catch (ClassNotFoundException ex) {
            //    throw new TaskException("Cannot use javah because it is not available"+
            //                             " A common solution is to set the environment variable"+
            //                             " JAVA_HOME to your jdk directory.", location);
            //}
        catch( Exception ex )
        {
            if( ex instanceof TaskException )
            {
                throw (TaskException)ex;
            }
            else
            {
                throw new TaskException( "Error starting javah: ", ex );
            }
        }
    }
}

