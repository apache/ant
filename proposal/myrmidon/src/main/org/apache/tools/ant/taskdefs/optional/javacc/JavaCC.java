/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.optional.javacc;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.exec.Execute2;
import org.apache.tools.ant.types.Argument;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.CommandlineJava;
import org.apache.tools.ant.types.Path;
import org.apache.avalon.excalibur.util.StringUtil;

/**
 * Taskdef for the JavaCC compiler compiler.
 *
 * @author thomas.haas@softwired-inc.com
 * @author Michael Saunders <a href="mailto:michael@amtec.com">michael@amtec.com
 *      </a>
 */
public class JavaCC extends Task
{

    // keys to optional attributes
    private final static String LOOKAHEAD = "LOOKAHEAD";
    private final static String CHOICE_AMBIGUITY_CHECK = "CHOICE_AMBIGUITY_CHECK";
    private final static String OTHER_AMBIGUITY_CHECK = "OTHER_AMBIGUITY_CHECK";

    private final static String STATIC = "STATIC";
    private final static String DEBUG_PARSER = "DEBUG_PARSER";
    private final static String DEBUG_LOOKAHEAD = "DEBUG_LOOKAHEAD";
    private final static String DEBUG_TOKEN_MANAGER = "DEBUG_TOKEN_MANAGER";
    private final static String OPTIMIZE_TOKEN_MANAGER = "OPTIMIZE_TOKEN_MANAGER";
    private final static String ERROR_REPORTING = "ERROR_REPORTING";
    private final static String JAVA_UNICODE_ESCAPE = "JAVA_UNICODE_ESCAPE";
    private final static String UNICODE_INPUT = "UNICODE_INPUT";
    private final static String IGNORE_CASE = "IGNORE_CASE";
    private final static String COMMON_TOKEN_ACTION = "COMMON_TOKEN_ACTION";
    private final static String USER_TOKEN_MANAGER = "USER_TOKEN_MANAGER";
    private final static String USER_CHAR_STREAM = "USER_CHAR_STREAM";
    private final static String BUILD_PARSER = "BUILD_PARSER";
    private final static String BUILD_TOKEN_MANAGER = "BUILD_TOKEN_MANAGER";
    private final static String SANITY_CHECK = "SANITY_CHECK";
    private final static String FORCE_LA_CHECK = "FORCE_LA_CHECK";
    private final static String CACHE_TOKENS = "CACHE_TOKENS";

    private final Hashtable optionalAttrs = new Hashtable();

    // required attributes
    private File outputDirectory = null;
    private File target = null;
    private File javaccHome = null;

    private CommandlineJava cmdl = new CommandlineJava();

    public JavaCC()
    {
        cmdl.setVm( "java" );
        cmdl.setClassname( "COM.sun.labs.javacc.Main" );
    }

    public void setBuildparser( boolean buildParser )
    {
        optionalAttrs.put( BUILD_PARSER, new Boolean( buildParser ) );
    }

    public void setBuildtokenmanager( boolean buildTokenManager )
    {
        optionalAttrs.put( BUILD_TOKEN_MANAGER, new Boolean( buildTokenManager ) );
    }

    public void setCachetokens( boolean cacheTokens )
    {
        optionalAttrs.put( CACHE_TOKENS, new Boolean( cacheTokens ) );
    }

    public void setChoiceambiguitycheck( int choiceAmbiguityCheck )
    {
        optionalAttrs.put( CHOICE_AMBIGUITY_CHECK, new Integer( choiceAmbiguityCheck ) );
    }

    public void setCommontokenaction( boolean commonTokenAction )
    {
        optionalAttrs.put( COMMON_TOKEN_ACTION, new Boolean( commonTokenAction ) );
    }

    public void setDebuglookahead( boolean debugLookahead )
    {
        optionalAttrs.put( DEBUG_LOOKAHEAD, new Boolean( debugLookahead ) );
    }

    public void setDebugparser( boolean debugParser )
    {
        optionalAttrs.put( DEBUG_PARSER, new Boolean( debugParser ) );
    }

    public void setDebugtokenmanager( boolean debugTokenManager )
    {
        optionalAttrs.put( DEBUG_TOKEN_MANAGER, new Boolean( debugTokenManager ) );
    }

    public void setErrorreporting( boolean errorReporting )
    {
        optionalAttrs.put( ERROR_REPORTING, new Boolean( errorReporting ) );
    }

    public void setForcelacheck( boolean forceLACheck )
    {
        optionalAttrs.put( FORCE_LA_CHECK, new Boolean( forceLACheck ) );
    }

    public void setIgnorecase( boolean ignoreCase )
    {
        optionalAttrs.put( IGNORE_CASE, new Boolean( ignoreCase ) );
    }

    public void setJavacchome( File javaccHome )
    {
        this.javaccHome = javaccHome;
    }

    public void setJavaunicodeescape( boolean javaUnicodeEscape )
    {
        optionalAttrs.put( JAVA_UNICODE_ESCAPE, new Boolean( javaUnicodeEscape ) );
    }

    public void setLookahead( int lookahead )
    {
        optionalAttrs.put( LOOKAHEAD, new Integer( lookahead ) );
    }

    public void setOptimizetokenmanager( boolean optimizeTokenManager )
    {
        optionalAttrs.put( OPTIMIZE_TOKEN_MANAGER, new Boolean( optimizeTokenManager ) );
    }

    public void setOtherambiguityCheck( int otherAmbiguityCheck )
    {
        optionalAttrs.put( OTHER_AMBIGUITY_CHECK, new Integer( otherAmbiguityCheck ) );
    }

    public void setOutputdirectory( File outputDirectory )
    {
        this.outputDirectory = outputDirectory;
    }

    public void setSanitycheck( boolean sanityCheck )
    {
        optionalAttrs.put( SANITY_CHECK, new Boolean( sanityCheck ) );
    }

    public void setStatic( boolean staticParser )
    {
        optionalAttrs.put( STATIC, new Boolean( staticParser ) );
    }

    public void setTarget( File target )
    {
        this.target = target;
    }

    public void setUnicodeinput( boolean unicodeInput )
    {
        optionalAttrs.put( UNICODE_INPUT, new Boolean( unicodeInput ) );
    }

    public void setUsercharstream( boolean userCharStream )
    {
        optionalAttrs.put( USER_CHAR_STREAM, new Boolean( userCharStream ) );
    }

    public void setUsertokenmanager( boolean userTokenManager )
    {
        optionalAttrs.put( USER_TOKEN_MANAGER, new Boolean( userTokenManager ) );
    }

    public void execute()
        throws TaskException
    {

        // load command line with optional attributes
        Enumeration iter = optionalAttrs.keys();
        while( iter.hasMoreElements() )
        {
            String name = (String)iter.nextElement();
            Object value = optionalAttrs.get( name );
            cmdl.createArgument().setValue( "-" + name + ":" + value.toString() );
        }

        // check the target is a file
        if( target == null || !target.isFile() )
        {
            throw new TaskException( "Invalid target: " + target );
        }

        // use the directory containing the target as the output directory
        if( outputDirectory == null )
        {
            outputDirectory = new File( target.getParent() );
        }
        else if( !outputDirectory.isDirectory() )
        {
            throw new TaskException( "Outputdir not a directory." );
        }
        cmdl.createArgument().setValue(
            "-OUTPUT_DIRECTORY:" + outputDirectory.getAbsolutePath() );

        // determine if the generated java file is up-to-date
        final File javaFile = getOutputJavaFile( outputDirectory, target );
        if( javaFile.exists() && target.lastModified() < javaFile.lastModified() )
        {
            getLogger().debug( "Target is already built - skipping (" + target + ")" );
            return;
        }
        cmdl.createArgument().setValue( target.getAbsolutePath() );

        if( javaccHome == null || !javaccHome.isDirectory() )
        {
            throw new TaskException( "Javacchome not set." );
        }
        final Path classpath = cmdl.createClasspath();
        classpath.addLocation( new File( javaccHome, "JavaCC.zip" ) );
        classpath.addJavaRuntime();

        final Argument arg = cmdl.createVmArgument();
        arg.setValue( "-mx140M" );
        arg.setValue( "-Dinstall.root=" + javaccHome.getAbsolutePath() );

        runCommand( cmdl.getCommandline() );
    }

    private void runCommand( final String[] cmdline )
        throws TaskException
    {
        try
        {
            getLogger().debug( StringUtil.join( cmdline, " " ) );
            final Execute2 exe = new Execute2();
            setupLogger( exe );
            exe.setCommandline( cmdline );
            int retval = exe.execute();
            if( retval != 0 )
            {
                throw new TaskException( cmdline[ 0 ] + " failed with return code " + retval );
            }
        }
        catch( final IOException ioe )
        {
            throw new TaskException( "Could not launch " + cmdline[ 0 ] + ": " + ioe );
        }
    }

    /**
     * Determines the output Java file to be generated by the given grammar
     * file.
     *
     * @param outputdir Description of Parameter
     * @param srcfile Description of Parameter
     * @return The OutputJavaFile value
     */
    private File getOutputJavaFile( File outputdir, File srcfile )
    {
        String path = srcfile.getPath();

        // Extract file's base-name
        int startBasename = path.lastIndexOf( File.separator );
        if( startBasename != -1 )
        {
            path = path.substring( startBasename + 1 );
        }

        // Replace the file's extension with '.java'
        int startExtn = path.lastIndexOf( '.' );
        if( startExtn != -1 )
        {
            path = path.substring( 0, startExtn ) + ".java";
        }
        else
        {
            path += ".java";
        }

        // Change the directory
        if( outputdir != null )
        {
            path = outputdir + File.separator + path;
        }

        return new File( path );
    }
}
