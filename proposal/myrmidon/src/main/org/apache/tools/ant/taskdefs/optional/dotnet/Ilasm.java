/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs.optional.dotnet;

import java.io.File;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;

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
    extends org.apache.tools.ant.taskdefs.MatchingTask
{

    /**
     * name of the executable. the .exe suffix is deliberately not included in
     * anticipation of the unix version
     */
    protected final static String exe_name = "ilasm";

    /**
     * what is the file extension we search on?
     */
    protected final static String file_ext = "il";

    /**
     * and now derive the search pattern from the extension
     */
    protected final static String file_pattern = "**/*." + file_ext;

    /**
     * title of task for external presentation
     */
    protected final static String exe_title = "ilasm";

    /**
     * debug flag. Controls generation of debug information.
     */
    protected boolean _debug;

    /**
     * any extra command options?
     */
    protected String _extraOptions;

    /**
     * flag to control action on execution trouble
     */
    protected boolean _failOnError;

    /**
     * listing flag
     */

    protected boolean _listing;

    /**
     * output file. If not supplied this is derived from the source file
     */
    protected File _outputFile;

    /**
     * resource file (.res format) to include in the app.
     */
    protected File _resourceFile;

    /**
     * type of target. Should be one of exe|library|module|winexe|(null) default
     * is exe; the actual value (if not null) is fed to the command line. <br>
     * See /target
     */
    protected String _targetType;

    /**
     * verbose flag
     */
    protected boolean _verbose;

    /**
     * file containing private key
     */

    private File _keyfile;

    /**
     * source directory upon which the search pattern is applied
     */
    private File _srcDir;

    /**
     * constructor inits everything and set up the search pattern
     */
    public Ilasm()
        throws TaskException
    {
        Clear();
        setIncludes( file_pattern );
    }

    /**
     * set the debug flag on or off
     *
     * @param f on/off flag
     */
    public void setDebug( boolean f )
    {
        _debug = f;
    }

    /**
     * Sets the ExtraOptions attribute
     *
     * @param extraOptions The new ExtraOptions value
     */
    public void setExtraOptions( String extraOptions )
    {
        this._extraOptions = extraOptions;
    }

    /**
     * set fail on error flag
     *
     * @param b The new FailOnError value
     */
    public void setFailOnError( boolean b )
    {
        _failOnError = b;
    }

    public void setKeyfile( File keyfile )
    {
        this._keyfile = keyfile;
    }

    /**
     * enable/disable listing
     *
     * @param b flag set to true for listing on
     */
    public void setListing( boolean b )
    {
        _listing = b;
    }

    /**
     * Set the definitions
     *
     * @param params The new OutputFile value
     */
    public void setOutputFile( File params )
    {
        _outputFile = params;
    }

    /**
     * Sets the Owner attribute
     *
     * @param s The new Owner value
     */

    public void setOwner( String s )
    {
        log( "This option is not supported by ILASM as of Beta-2, and will be ignored", Project.MSG_WARN );
    }

    /**
     * Set the resource file
     *
     * @param fileName path to the file. Can be relative, absolute, whatever.
     */
    public void setResourceFile( File fileName )
    {
        _resourceFile = fileName;
    }

    /**
     * Set the source dir to find the files to be compiled
     *
     * @param srcDirName The new SrcDir value
     */
    public void setSrcDir( File srcDirName )
    {
        _srcDir = srcDirName;
    }

    /**
     * define the target
     *
     * @param targetType one of exe|library|
     * @exception TaskException if target is not one of
     *      exe|library|module|winexe
     */

    public void setTargetType( String targetType )
        throws TaskException
    {
        targetType = targetType.toLowerCase();
        if( targetType.equals( "exe" ) || targetType.equals( "library" ) )
        {
            _targetType = targetType;
        }
        else
            throw new TaskException( "targetType " + targetType + " is not a valid type" );
    }

    /**
     * enable/disable verbose ILASM output
     *
     * @param b flag set to true for verbose on
     */
    public void setVerbose( boolean b )
    {
        _verbose = b;
    }

    /**
     * query the debug flag
     *
     * @return true if debug is turned on
     */
    public boolean getDebug()
    {
        return _debug;
    }

    /**
     * Gets the ExtraOptions attribute
     *
     * @return The ExtraOptions value
     */
    public String getExtraOptions()
    {
        return this._extraOptions;
    }

    /**
     * query fail on error flag
     *
     * @return The FailFailOnError value
     */
    public boolean getFailFailOnError()
    {
        return _failOnError;
    }

    /**
     * accessor method for target type
     *
     * @return the current target option
     */
    public String getTargetType()
    {
        return _targetType;
    }

    /**
     * reset all contents.
     */
    public void Clear()
    {
        _targetType = null;
        _srcDir = null;
        _listing = false;
        _verbose = false;
        _debug = true;
        _outputFile = null;
        _failOnError = true;
        _resourceFile = null;
        _extraOptions = null;
    }

    /**
     * This is the execution entry point. Build a list of files and call ilasm
     * on each of them.
     *
     * @throws TaskException if the assembly failed and FailOnError is true
     */
    public void execute()
        throws TaskException
    {
        if( _srcDir == null )
            _srcDir = resolveFile( "." );

        //get dependencies list.
        DirectoryScanner scanner = super.getDirectoryScanner( _srcDir );
        String[] dependencies = scanner.getIncludedFiles();
        log( "assembling " + dependencies.length + " file" + ( ( dependencies.length == 1 ) ? "" : "s" ) );
        String baseDir = scanner.getBasedir().toString();
        //add to the command
        for( int i = 0; i < dependencies.length; i++ )
        {
            String targetFile = dependencies[ i ];
            targetFile = baseDir + File.separator + targetFile;
            executeOneFile( targetFile );
        }

    }// end execute

    /**
     * do the work for one file by building the command line then calling it
     *
     * @param targetFile name of the the file to assemble
     * @throws TaskException if the assembly failed and FailOnError is true
     */
    public void executeOneFile( String targetFile )
        throws TaskException
    {
        NetCommand command = new NetCommand( this, exe_title, exe_name );
        command.setFailOnError( getFailFailOnError() );
        //DEBUG helper
        command.setTraceCommandLine( true );
        //fill in args
        command.addArgument( getDebugParameter() );
        command.addArgument( getTargetTypeParameter() );
        command.addArgument( getListingParameter() );
        command.addArgument( getOutputFileParameter() );
        command.addArgument( getResourceFileParameter() );
        command.addArgument( getVerboseParameter() );
        command.addArgument( getKeyfileParameter() );
        command.addArgument( getExtraOptionsParameter() );

        /*
         * space for more argumentativeness
         * command.addArgument();
         * command.addArgument();
         */
        command.addArgument( targetFile );
        //now run the command of exe + settings + file
        command.runCommand();
    }

    /**
     * get the argument or null for no argument needed
     *
     * @return The DebugParameter value
     */
    protected String getDebugParameter()
    {
        return _debug ? "/debug" : null;
    }

    /**
     * get any extra options or null for no argument needed
     *
     * @return The ExtraOptions Parameter to CSC
     */
    protected String getExtraOptionsParameter()
    {
        if( _extraOptions != null && _extraOptions.length() != 0 )
            return _extraOptions;
        else
            return null;
    }

    /**
     * get the argument or null for no argument needed
     *
     * @return The KeyfileParameter value
     */
    protected String getKeyfileParameter()
    {
        if( _keyfile != null )
            return "/keyfile:" + _keyfile.toString();
        else
            return null;
    }

    /**
     * turn the listing flag into a parameter for ILASM
     *
     * @return the appropriate string from the state of the listing flag
     */
    protected String getListingParameter()
    {
        return _listing ? "/listing" : "/nolisting";
    }

    /**
     * get the output file
     *
     * @return the argument string or null for no argument
     */
    protected String getOutputFileParameter()
    {
        if( _outputFile == null || _outputFile.length() == 0 )
            return null;
        File f = _outputFile;
        return "/output=" + f.toString();
    }

    protected String getResourceFileParameter()
    {
        if( _resourceFile != null )
        {
            return "/resource=" + _resourceFile.toString();
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

    protected String getTargetTypeParameter()
    {
        if( !notEmpty( _targetType ) )
            return null;
        if( _targetType.equals( "exe" ) )
            return "/exe";
        else if( _targetType.equals( "library" ) )
            return "/dll";
        else
            return null;
    }

    /**
     * turn the verbose flag into a parameter for ILASM
     *
     * @return null or the appropriate command line string
     */
    protected String getVerboseParameter()
    {
        return _verbose ? null : "/quiet";
    }

    /**
     * test for a string containing something useful
     *
     * @param s Description of Parameter
     * @return Description of the Returned Value
     * @returns true if the argument is not null or empty
     */
    protected boolean notEmpty( String s )
    {
        return s != null && s.length() != 0;
    }// end executeOneFile
}//class
