/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.optional;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Properties;
import java.util.StringTokenizer;
import netrexx.lang.Rexx;
import org.apache.avalon.excalibur.io.FileUtil;
import org.apache.avalon.excalibur.util.StringUtil;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.taskdefs.MatchingTask;

/**
 * Task to compile NetRexx source files. This task can take the following
 * arguments:
 * <ul>
 *   <li> binary</li>
 *   <li> classpath</li>
 *   <li> comments</li>
 *   <li> compile</li>
 *   <li> console</li>
 *   <li> crossref</li>
 *   <li> decimal</li>
 *   <li> destdir</li>
 *   <li> diag</li>
 *   <li> explicit</li>
 *   <li> format</li>
 *   <li> keep</li>
 *   <li> logo</li>
 *   <li> replace</li>
 *   <li> savelog</li>
 *   <li> srcdir</li>
 *   <li> sourcedir</li>
 *   <li> strictargs</li>
 *   <li> strictassign</li>
 *   <li> strictcase</li>
 *   <li> strictimport</li>
 *   <li> symbols</li>
 *   <li> time</li>
 *   <li> trace</li>
 *   <li> utf8</li>
 *   <li> verbose</li>
 * </ul>
 * Of these arguments, the <b>srcdir</b> argument is required. <p>
 *
 * When this task executes, it will recursively scan the srcdir looking for
 * NetRexx source files to compile. This task makes its compile decision based
 * on timestamp. <p>
 *
 * Before files are compiled they and any other file in the srcdir will be
 * copied to the destdir allowing support files to be located properly in the
 * classpath. The reason for copying the source files before the compile is that
 * NetRexxC has only two destinations for classfiles:
 * <ol>
 *   <li> The current directory, and,</li>
 *   <li> The directory the source is in (see sourcedir option)
 * </ol>
 *
 *
 * @author dIon Gillard <a href="mailto:dion@multitask.com.au">
 *      dion@multitask.com.au</a>
 */

public class NetRexxC extends MatchingTask
{
    private boolean compile = true;
    private boolean decimal = true;
    private boolean logo = true;
    private boolean sourcedir = true;
    private String trace = "trace2";
    private String verbose = "verbose3";

    // other implementation variables
    private ArrayList compileList = new ArrayList();
    private Hashtable filecopyList = new Hashtable();
    private String oldClasspath = System.getProperty( "java.class.path" );

    // variables to hold arguments
    private boolean binary;
    private String classpath;
    private boolean comments;
    private boolean compact;
    private boolean console;
    private boolean crossref;
    private File destDir;
    private boolean diag;
    private boolean explicit;
    private boolean format;
    private boolean java;
    private boolean keep;
    private boolean replace;
    private boolean savelog;
    private File srcDir;// ?? Should this be the default for ant?
    private boolean strictargs;
    private boolean strictassign;
    private boolean strictcase;
    private boolean strictimport;
    private boolean strictprops;
    private boolean strictsignal;
    private boolean symbols;
    private boolean time;
    private boolean utf8;

    /**
     * Set whether literals are treated as binary, rather than NetRexx types
     *
     * @param binary The new Binary value
     */
    public void setBinary( boolean binary )
    {
        this.binary = binary;
    }

    /**
     * Set the classpath used for NetRexx compilation
     *
     * @param classpath The new Classpath value
     */
    public void setClasspath( String classpath )
    {
        this.classpath = classpath;
    }

    /**
     * Set whether comments are passed through to the generated java source.
     * Valid true values are "on" or "true". Anything else sets the flag to
     * false. The default value is false
     *
     * @param comments The new Comments value
     */
    public void setComments( boolean comments )
    {
        this.comments = comments;
    }

    /**
     * Set whether error messages come out in compact or verbose format. Valid
     * true values are "on" or "true". Anything else sets the flag to false. The
     * default value is false
     *
     * @param compact The new Compact value
     */
    public void setCompact( boolean compact )
    {
        this.compact = compact;
    }

    /**
     * Set whether the NetRexx compiler should compile the generated java code
     * Valid true values are "on" or "true". Anything else sets the flag to
     * false. The default value is true. Setting this flag to false, will
     * automatically set the keep flag to true.
     *
     * @param compile The new Compile value
     */
    public void setCompile( boolean compile )
    {
        this.compile = compile;
        if( !this.compile && !this.keep )
            this.keep = true;
    }

    /**
     * Set whether or not messages should be displayed on the 'console' Valid
     * true values are "on" or "true". Anything else sets the flag to false. The
     * default value is true.
     *
     * @param console The new Console value
     */
    public void setConsole( boolean console )
    {
        this.console = console;
    }

    /**
     * Whether variable cross references are generated
     *
     * @param crossref The new Crossref value
     */
    public void setCrossref( boolean crossref )
    {
        this.crossref = crossref;
    }

    /**
     * Set whether decimal arithmetic should be used for the netrexx code.
     * Binary arithmetic is used when this flag is turned off. Valid true values
     * are "on" or "true". Anything else sets the flag to false. The default
     * value is true.
     *
     * @param decimal The new Decimal value
     */
    public void setDecimal( boolean decimal )
    {
        this.decimal = decimal;
    }

    /**
     * Set the destination directory into which the NetRexx source files should
     * be copied and then compiled.
     *
     * @param destDirName The new DestDir value
     */
    public void setDestDir( File destDirName )
    {
        destDir = destDirName;
    }

    /**
     * Whether diagnostic information about the compile is generated
     *
     * @param diag The new Diag value
     */
    public void setDiag( boolean diag )
    {
        this.diag = diag;
    }

    /**
     * Sets whether variables must be declared explicitly before use. Valid true
     * values are "on" or "true". Anything else sets the flag to false. The
     * default value is false.
     *
     * @param explicit The new Explicit value
     */
    public void setExplicit( boolean explicit )
    {
        this.explicit = explicit;
    }

    /**
     * Whether the generated java code is formatted nicely or left to match
     * NetRexx line numbers for call stack debugging
     *
     * @param format The new Format value
     */
    public void setFormat( boolean format )
    {
        this.format = format;
    }

    /**
     * Whether the generated java code is produced Valid true values are "on" or
     * "true". Anything else sets the flag to false. The default value is false.
     *
     * @param java The new Java value
     */
    public void setJava( boolean java )
    {
        this.java = java;
    }

    /**
     * Sets whether the generated java source file should be kept after
     * compilation. The generated files will have an extension of .java.keep,
     * <b>not</b> .java Valid true values are "on" or "true". Anything else sets
     * the flag to false. The default value is false.
     *
     * @param keep The new Keep value
     */
    public void setKeep( boolean keep )
    {
        this.keep = keep;
    }

    /**
     * Whether the compiler text logo is displayed when compiling
     *
     * @param logo The new Logo value
     */
    public void setLogo( boolean logo )
    {
        this.logo = logo;
    }

    /**
     * Whether the generated .java file should be replaced when compiling Valid
     * true values are "on" or "true". Anything else sets the flag to false. The
     * default value is false.
     *
     * @param replace The new Replace value
     */
    public void setReplace( boolean replace )
    {
        this.replace = replace;
    }

    /**
     * Sets whether the compiler messages will be written to NetRexxC.log as
     * well as to the console Valid true values are "on" or "true". Anything
     * else sets the flag to false. The default value is false.
     *
     * @param savelog The new Savelog value
     */
    public void setSavelog( boolean savelog )
    {
        this.savelog = savelog;
    }

    /**
     * Tells the NetRexx compiler to store the class files in the same directory
     * as the source files. The alternative is the working directory Valid true
     * values are "on" or "true". Anything else sets the flag to false. The
     * default value is true.
     *
     * @param sourcedir The new Sourcedir value
     */
    public void setSourcedir( boolean sourcedir )
    {
        this.sourcedir = sourcedir;
    }

    /**
     * Set the source dir to find the source Java files.
     *
     * @param srcDirName The new SrcDir value
     */
    public void setSrcDir( File srcDirName )
    {
        srcDir = srcDirName;
    }

    /**
     * Tells the NetRexx compiler that method calls always need parentheses,
     * even if no arguments are needed, e.g. <code>aStringVar.getBytes</code>
     * vs. <code>aStringVar.getBytes()</code> Valid true values are "on" or
     * "true". Anything else sets the flag to false. The default value is false.
     *
     * @param strictargs The new Strictargs value
     */
    public void setStrictargs( boolean strictargs )
    {
        this.strictargs = strictargs;
    }

    /**
     * Tells the NetRexx compile that assignments must match exactly on type
     *
     * @param strictassign The new Strictassign value
     */
    public void setStrictassign( boolean strictassign )
    {
        this.strictassign = strictassign;
    }

    /**
     * Specifies whether the NetRexx compiler should be case sensitive or not
     *
     * @param strictcase The new Strictcase value
     */
    public void setStrictcase( boolean strictcase )
    {
        this.strictcase = strictcase;
    }

    /**
     * Sets whether classes need to be imported explicitly using an <code>import</code>
     * statement. By default the NetRexx compiler will import certain packages
     * automatically Valid true values are "on" or "true". Anything else sets
     * the flag to false. The default value is false.
     *
     * @param strictimport The new Strictimport value
     */
    public void setStrictimport( boolean strictimport )
    {
        this.strictimport = strictimport;
    }

    /**
     * Sets whether local properties need to be qualified explicitly using
     * <code>this</code> Valid true values are "on" or "true". Anything else
     * sets the flag to false. The default value is false.
     *
     * @param strictprops The new Strictprops value
     */
    public void setStrictprops( boolean strictprops )
    {
        this.strictprops = strictprops;
    }

    /**
     * Whether the compiler should force catching of exceptions by explicitly
     * named types
     *
     * @param strictsignal The new Strictsignal value
     */
    public void setStrictsignal( boolean strictsignal )
    {
        this.strictsignal = strictsignal;
    }

    /**
     * Sets whether debug symbols should be generated into the class file Valid
     * true values are "on" or "true". Anything else sets the flag to false. The
     * default value is false.
     *
     * @param symbols The new Symbols value
     */
    public void setSymbols( boolean symbols )
    {
        this.symbols = symbols;
    }

    /**
     * Asks the NetRexx compiler to print compilation times to the console Valid
     * true values are "on" or "true". Anything else sets the flag to false. The
     * default value is false.
     *
     * @param time The new Time value
     */
    public void setTime( boolean time )
    {
        this.time = time;
    }

    /**
     * Turns on or off tracing and directs the resultant trace output Valid
     * values are: "trace", "trace1", "trace2" and "notrace". "trace" and
     * "trace2"
     *
     * @param trace The new Trace value
     */
    public void setTrace( String trace )
    {
        if( trace.equalsIgnoreCase( "trace" )
            || trace.equalsIgnoreCase( "trace1" )
            || trace.equalsIgnoreCase( "trace2" )
            || trace.equalsIgnoreCase( "notrace" ) )
        {
            this.trace = trace;
        }
        else
        {
            throw new TaskException( "Unknown trace value specified: '" + trace + "'" );
        }
    }

    /**
     * Tells the NetRexx compiler that the source is in UTF8 Valid true values
     * are "on" or "true". Anything else sets the flag to false. The default
     * value is false.
     *
     * @param utf8 The new Utf8 value
     */
    public void setUtf8( boolean utf8 )
    {
        this.utf8 = utf8;
    }

    /**
     * Whether lots of warnings and error messages should be generated
     *
     * @param verbose The new Verbose value
     */
    public void setVerbose( String verbose )
    {
        this.verbose = verbose;
    }

    /**
     * Executes the task, i.e. does the actual compiler call
     *
     * @exception TaskException Description of Exception
     */
    public void execute()
        throws TaskException
    {

        // first off, make sure that we've got a srcdir and destdir
        if( srcDir == null || destDir == null )
        {
            throw new TaskException( "srcDir and destDir attributes must be set!" );
        }

        // scan source and dest dirs to build up both copy lists and
        // compile lists
        //        scanDir(srcDir, destDir);
        DirectoryScanner ds = getDirectoryScanner( srcDir );

        String[] files = ds.getIncludedFiles();

        scanDir( srcDir, destDir, files );

        // copy the source and support files
        copyFilesToDestination();

        // compile the source files
        if( compileList.size() > 0 )
        {
            getLogger().info( "Compiling " + compileList.size() + " source file"
                              + ( compileList.size() == 1 ? "" : "s" )
                              + " to " + destDir );
            doNetRexxCompile();
        }
    }

    /**
     * Builds the compilation classpath.
     *
     * @return The CompileClasspath value
     */
    private String getCompileClasspath()
        throws TaskException
    {
        StringBuffer classpath = new StringBuffer();

        // add dest dir to classpath so that previously compiled and
        // untouched classes are on classpath
        classpath.append( destDir.getAbsolutePath() );

        // add our classpath to the mix
        if( this.classpath != null )
        {
            addExistingToClasspath( classpath, this.classpath );
        }

        // add the system classpath
        // addExistingToClasspath(classpath,System.getProperty("java.class.path"));
        return classpath.toString();
    }

    /**
     * This
     *
     * @return The CompileOptionsAsArray value
     */
    private String[] getCompileOptionsAsArray()
    {
        ArrayList options = new ArrayList();
        options.add( binary ? "-binary" : "-nobinary" );
        options.add( comments ? "-comments" : "-nocomments" );
        options.add( compile ? "-compile" : "-nocompile" );
        options.add( compact ? "-compact" : "-nocompact" );
        options.add( console ? "-console" : "-noconsole" );
        options.add( crossref ? "-crossref" : "-nocrossref" );
        options.add( decimal ? "-decimal" : "-nodecimal" );
        options.add( diag ? "-diag" : "-nodiag" );
        options.add( explicit ? "-explicit" : "-noexplicit" );
        options.add( format ? "-format" : "-noformat" );
        options.add( keep ? "-keep" : "-nokeep" );
        options.add( logo ? "-logo" : "-nologo" );
        options.add( replace ? "-replace" : "-noreplace" );
        options.add( savelog ? "-savelog" : "-nosavelog" );
        options.add( sourcedir ? "-sourcedir" : "-nosourcedir" );
        options.add( strictargs ? "-strictargs" : "-nostrictargs" );
        options.add( strictassign ? "-strictassign" : "-nostrictassign" );
        options.add( strictcase ? "-strictcase" : "-nostrictcase" );
        options.add( strictimport ? "-strictimport" : "-nostrictimport" );
        options.add( strictprops ? "-strictprops" : "-nostrictprops" );
        options.add( strictsignal ? "-strictsignal" : "-nostrictsignal" );
        options.add( symbols ? "-symbols" : "-nosymbols" );
        options.add( time ? "-time" : "-notime" );
        options.add( "-" + trace );
        options.add( utf8 ? "-utf8" : "-noutf8" );
        options.add( "-" + verbose );
        String[] results = new String[ options.size() ];
        options.copyInto( results );
        return results;
    }

    /**
     * Takes a classpath-like string, and adds each element of this string to a
     * new classpath, if the components exist. Components that don't exist,
     * aren't added. We do this, because jikes issues warnings for non-existant
     * files/dirs in his classpath, and these warnings are pretty annoying.
     *
     * @param target - target classpath
     * @param source - source classpath to get file objects.
     */
    private void addExistingToClasspath( StringBuffer target, String source )
        throws TaskException
    {
        StringTokenizer tok = new StringTokenizer( source,
                                                   System.getProperty( "path.separator" ), false );
        while( tok.hasMoreTokens() )
        {
            File f = resolveFile( tok.nextToken() );

            if( f.exists() )
            {
                target.append( File.pathSeparator );
                target.append( f.getAbsolutePath() );
            }
            else
            {
                getLogger().debug( "Dropping from classpath: " + f.getAbsolutePath() );
            }
        }

    }

    /**
     * Copy eligible files from the srcDir to destDir
     */
    private void copyFilesToDestination()
    {
        //FIXME: This should be zapped no ?
        if( filecopyList.size() > 0 )
        {
            getLogger().info( "Copying " + filecopyList.size() + " file"
                              + ( filecopyList.size() == 1 ? "" : "s" )
                              + " to " + destDir.getAbsolutePath() );
            Iterator enum = filecopyList.keySet().iterator();
            while( enum.hasNext() )
            {
                String fromFile = (String)enum.next();
                String toFile = (String)filecopyList.get( fromFile );
                try
                {
                    FileUtil.copyFile( new File( fromFile ), new File( toFile ) );
                }
                catch( IOException ioe )
                {
                    String msg = "Failed to copy " + fromFile + " to " + toFile
                        + " due to " + ioe.getMessage();
                    throw new TaskException( msg, ioe );
                }
            }
        }
    }

    /**
     * Peforms a copmile using the NetRexx 1.1.x compiler
     *
     * @exception TaskException Description of Exception
     */
    private void doNetRexxCompile()
        throws TaskException
    {
        getLogger().debug( "Using NetRexx compiler" );
        String classpath = getCompileClasspath();
        StringBuffer compileOptions = new StringBuffer();
        StringBuffer fileList = new StringBuffer();

        // create an array of strings for input to the compiler: one array
        // comes from the compile options, the other from the compileList
        String[] compileOptionsArray = getCompileOptionsAsArray();
        String[] fileListArray = new String[ compileList.size() ];
        Iterator e = compileList.iterator();
        int j = 0;
        while( e.hasNext() )
        {
            fileListArray[ j ] = (String)e.next();
            j++;
        }
        // create a single array of arguments for the compiler
        String compileArgs[] = new String[ compileOptionsArray.length + fileListArray.length ];
        for( int i = 0; i < compileOptionsArray.length; i++ )
        {
            compileArgs[ i ] = compileOptionsArray[ i ];
        }
        for( int i = 0; i < fileListArray.length; i++ )
        {
            compileArgs[ i + compileOptionsArray.length ] = fileListArray[ i ];
        }

        // print nice output about what we are doing for the log
        compileOptions.append( "Compilation args: " );
        for( int i = 0; i < compileOptionsArray.length; i++ )
        {
            compileOptions.append( compileOptionsArray[ i ] );
            compileOptions.append( " " );
        }
        getLogger().debug( compileOptions.toString() );

        StringBuffer niceSourceList = new StringBuffer( "Files to be compiled:" + StringUtil.LINE_SEPARATOR );

        for( int i = 0; i < compileList.size(); i++ )
        {
            niceSourceList.append( "    " );
            niceSourceList.append( compileList.get( i ).toString() );
            niceSourceList.append( StringUtil.LINE_SEPARATOR );
        }

        getLogger().debug( niceSourceList.toString() );

        // need to set java.class.path property and restore it later
        // since the NetRexx compiler has no option for the classpath
        String currentClassPath = System.getProperty( "java.class.path" );
        Properties currentProperties = System.getProperties();
        currentProperties.put( "java.class.path", classpath );

        try
        {
            StringWriter out = new StringWriter();
            int rc =
                COM.ibm.netrexx.process.NetRexxC.main( new Rexx( compileArgs ), new PrintWriter( out ) );

            if( rc > 1 )
            {// 1 is warnings from real NetRexxC
                getLogger().error( out.toString() );
                String msg = "Compile failed, messages should have been provided.";
                throw new TaskException( msg );
            }
            else if( rc == 1 )
            {
                getLogger().warn( out.toString() );
            }
            else
            {
                getLogger().info( out.toString() );
            }
        }
        finally
        {
            // need to reset java.class.path property
            // since the NetRexx compiler has no option for the classpath
            currentProperties = System.getProperties();
            currentProperties.put( "java.class.path", currentClassPath );
        }
    }

    /**
     * Scans the directory looking for source files to be compiled and support
     * files to be copied.
     *
     * @param srcDir Description of Parameter
     * @param destDir Description of Parameter
     * @param files Description of Parameter
     */
    private void scanDir( File srcDir, File destDir, String[] files )
    {
        for( int i = 0; i < files.length; i++ )
        {
            File srcFile = new File( srcDir, files[ i ] );
            File destFile = new File( destDir, files[ i ] );
            String filename = files[ i ];
            // if it's a non source file, copy it if a later date than the
            // dest
            // if it's a source file, see if the destination class file
            // needs to be recreated via compilation
            if( filename.toLowerCase().endsWith( ".nrx" ) )
            {
                File classFile =
                    new File( destDir,
                              filename.substring( 0, filename.lastIndexOf( '.' ) ) + ".class" );

                if( !compile || srcFile.lastModified() > classFile.lastModified() )
                {
                    filecopyList.put( srcFile.getAbsolutePath(), destFile.getAbsolutePath() );
                    compileList.add( destFile.getAbsolutePath() );
                }
            }
            else
            {
                if( srcFile.lastModified() > destFile.lastModified() )
                {
                    filecopyList.put( srcFile.getAbsolutePath(), destFile.getAbsolutePath() );
                }
            }
        }
    }
}
