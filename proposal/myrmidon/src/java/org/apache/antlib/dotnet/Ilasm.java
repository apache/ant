/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.dotnet;

import java.io.File;
import org.apache.aut.nativelib.ExecManager;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.api.AbstractTask;
import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.framework.Execute;
import org.apache.tools.ant.taskdefs.MatchingTask;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.DirectoryScanner;

/**
 * Task to assemble .net 'Intermediate Language' files. The task will only work
 * on win2K until other platforms support csc.exe or an equivalent. ilasm.exe
 * must be on the execute path too. <p>
 *
 * <p>
 *
 * All parameters are optional: &lt;il/&gt; should suffice to produce a debug
 * build of all *.il files. The option set is roughly compatible with the CSharp
 * class; even though the command line options are only vaguely equivalent. [The
 * low level commands take things like /OUT=file, csc wants /out:file ...
 * /verbose is used some places; /quiet here in ildasm... etc.] It would be nice
 * if someone made all the command line tools consistent (and not as brittle as
 * the java cmdline tools) <p>
 *
 * The task is a directory based task, so attributes like <b>includes="*.il"</b>
 * and <b>excludes="broken.il"</b> can be used to control the files pulled in.
 * Each file is built on its own, producing an appropriately named output file
 * unless manually specified with <b>outfile</b>
 *
 * @author Steve Loughran steve_l@iseran.com
 * @version 0.2
 */
public class Ilasm
    extends MatchingTask
{
    /**
     * name of the executable. the .exe suffix is deliberately not included in
     * anticipation of the unix version
     */
    private final static String EXE_NAME = "ilasm";

    /**
     * what is the file extension we search on?
     */
    private final static String FILE_EXT = "il";

    /**
     * and now derive the search pattern from the extension
     */
    private final static String FILE_PATTERN = "**/*." + FILE_EXT;

    /**
     * debug flag. Controls generation of debug information.
     */
    private boolean m_debug;

    /**
     * any extra command options?
     */
    private String m_extraOptions;

    /**
     * listing flag
     */
    private boolean m_listing;

    /**
     * output file. If not supplied this is derived from the source file
     */
    private File m_outputFile;

    /**
     * resource file (.res format) to include in the app.
     */
    private File m_resourceFile;

    /**
     * type of target. Should be one of exe|library|module|winexe|(null) default
     * is exe; the actual value (if not null) is fed to the command line. <br>
     * See /target
     */
    private String m_targetType;

    /**
     * verbose flag
     */
    private boolean m_verbose;

    /**
     * file containing private key
     */
    private File m_keyfile;

    /**
     * source directory upon which the search pattern is applied
     */
    private File m_srcDir;

    /**
     * constructor inits everything and set up the search pattern
     */
    public Ilasm()
        throws TaskException
    {
        setIncludes( FILE_PATTERN );
        m_debug = true;
    }

    /**
     * set the debug flag on or off
     *
     * @param debug on/off flag
     */
    public void setDebug( final boolean debug )
    {
        m_debug = debug;
    }

    /**
     * Sets the ExtraOptions attribute
     *
     * @param extraOptions The new ExtraOptions value
     */
    public void setExtraOptions( final String extraOptions )
    {
        m_extraOptions = extraOptions;
    }

    public void setKeyfile( final File keyfile )
    {
        m_keyfile = keyfile;
    }

    /**
     * enable/disable listing
     *
     * @param listing flag set to true for listing on
     */
    public void setListing( final boolean listing )
    {
        m_listing = listing;
    }

    /**
     * Set the definitions
     */
    public void setOutputFile( final File outputFile )
    {
        m_outputFile = outputFile;
    }

    /**
     * Set the resource file
     *
     * @param resourceFile path to the file. Can be relative, absolute, whatever.
     */
    public void setResourceFile( final File resourceFile )
    {
        m_resourceFile = resourceFile;
    }

    /**
     * Set the source dir to find the files to be compiled
     */
    public void setSrcDir( final File srcDir )
    {
        m_srcDir = srcDir;
    }

    /**
     * define the target
     *
     * @param targetType one of exe|library|
     * @exception TaskException if target is not one of
     *      exe|library|module|winexe
     */

    public void setTargetType( final String targetType )
        throws TaskException
    {
        final String type = targetType.toLowerCase();
        if( type.equals( "exe" ) || type.equals( "library" ) )
        {
            m_targetType = type;
        }
        else
        {
            final String message = "targetType " + targetType + " is not a valid type";
            throw new TaskException( message );
        }
    }

    /**
     * enable/disable verbose ILASM output
     *
     * @param verbose flag set to true for verbose on
     */
    public void setVerbose( final boolean verbose )
    {
        m_verbose = verbose;
    }

    /**
     * This is the execution entry point. Build a list of files and call ilasm
     * on each of them.
     *
     * @throws TaskException if the assembly failed
     */
    public void execute()
        throws TaskException
    {
        if( null == m_srcDir )
        {
            m_srcDir = getBaseDirectory();
        }

        //get dependencies list.
        final DirectoryScanner scanner = super.getDirectoryScanner( m_srcDir );
        final String[] dependencies = scanner.getIncludedFiles();
        final String baseDir = scanner.getBasedir().toString();

        final String message = "assembling " + dependencies.length + " file" +
            ( ( dependencies.length == 1 ) ? "" : "s" );
        getContext().info( message );

        //add to the command
        for( int i = 0; i < dependencies.length; i++ )
        {
            final String targetFile = baseDir + File.separator + dependencies[ i ];
            executeOneFile( targetFile );
        }
    }

    /**
     * do the work for one file by building the command line then calling it
     *
     * @param targetFile name of the the file to assemble
     * @throws TaskException if the assembly failed and FailOnError is true
     */
    public void executeOneFile( final String targetFile )
        throws TaskException
    {
        final ExecManager execManager = (ExecManager)getService( ExecManager.class );
        final Execute exe = new Execute( execManager );
        exe.setReturnCode( 0 );

        final Commandline cmd = exe.getCommandline();
        cmd.setExecutable( EXE_NAME );
        addArgument( cmd, getDebugParameter() );
        addArgument( cmd, getTargetTypeParameter() );
        addArgument( cmd, getListingParameter() );
        addArgument( cmd, getOutputFileParameter() );
        addArgument( cmd, getResourceFileParameter() );
        addArgument( cmd, getVerboseParameter() );
        addArgument( cmd, getKeyfileParameter() );
        addArgument( cmd, getExtraOptionsParameter() );
        addArgument( cmd, targetFile );
        exe.execute();
    }

    private void addArgument( final Commandline cmd, final String argument )
    {
        if( null != argument && 0 != argument.length() )
        {
            cmd.addArgument( argument );
        }
    }

    /**
     * get the argument or null for no argument needed
     *
     * @return The DebugParameter value
     */
    private String getDebugParameter()
    {
        return m_debug ? "/debug" : null;
    }

    /**
     * get any extra options or null for no argument needed
     *
     * @return The ExtraOptions Parameter to CSC
     */
    private String getExtraOptionsParameter()
    {
        if( m_extraOptions != null && m_extraOptions.length() != 0 )
        {
            return m_extraOptions;
        }
        else
        {
            return null;
        }
    }

    /**
     * get the argument or null for no argument needed
     */
    private String getKeyfileParameter()
    {
        if( m_keyfile != null )
        {
            return "/keyfile:" + m_keyfile.toString();
        }
        else
        {
            return null;
        }
    }

    /**
     * turn the listing flag into a parameter for ILASM
     *
     * @return the appropriate string from the state of the listing flag
     */
    private String getListingParameter()
    {
        return m_listing ? "/listing" : "/nolisting";
    }

    /**
     * get the output file
     *
     * @return the argument string or null for no argument
     */
    private String getOutputFileParameter()
    {
        if( null == m_outputFile || 0 == m_outputFile.length() )
        {
            return null;
        }
        return "/output=" + m_outputFile.toString();
    }

    private String getResourceFileParameter()
    {
        if( null != m_resourceFile )
        {
            return "/resource=" + m_resourceFile.toString();
        }
        else
        {
            return null;
        }
    }

    /**
     * g get the target type or null for no argument needed
     *
     * @return The TargetTypeParameter value
     */

    private String getTargetTypeParameter()
    {
        if( !notEmpty( m_targetType ) )
        {
            return null;
        }
        if( m_targetType.equals( "exe" ) )
        {
            return "/exe";
        }
        else if( m_targetType.equals( "library" ) )
        {
            return "/dll";
        }
        else
        {
            return null;
        }
    }

    /**
     * turn the verbose flag into a parameter for ILASM
     *
     * @return null or the appropriate command line string
     */
    private String getVerboseParameter()
    {
        return m_verbose ? null : "/quiet";
    }

    /**
     * test for a string containing something useful
     *
     * @returns true if the argument is not null or empty
     */
    private boolean notEmpty( final String string )
    {
        return string != null && string.length() != 0;
    }
}
