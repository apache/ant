/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.optional.jsp;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.types.DirectoryScanner;
import org.apache.tools.ant.taskdefs.MatchingTask;
import org.apache.tools.ant.taskdefs.optional.jsp.compilers.CompilerAdapter;
import org.apache.tools.ant.taskdefs.optional.jsp.compilers.CompilerAdapterFactory;
import org.apache.tools.ant.types.Path;

/**
 * Ant task to run the jsp compiler. <p>
 *
 * This task takes the given jsp files and compiles them into java files. It is
 * then up to the user to compile the java files into classes. <p>
 *
 * The task requires the srcdir and destdir attributes to be set. This Task is a
 * MatchingTask, so the files to be compiled can be specified using
 * includes/excludes attributes or nested include/exclude elements. Optional
 * attributes are verbose (set the verbosity level passed to jasper), package
 * (name of the destination package for generated java classes and classpath
 * (the classpath to use when running the jsp compiler). <p>
 *
 * This task supports the nested elements classpath (A Path) and classpathref (A
 * Reference) which can be used in preference to the attribute classpath, if the
 * jsp compiler is not already in the ant classpath. <p>
 *
 * <h4>Notes</h4> <p>
 *
 * At present, this task only supports the jasper compiler. In future, other
 * compilers will be supported by setting the jsp.compiler property. <p>
 *
 * <h4>Usage</h4> <pre>
 * &lt;jspc srcdir="${basedir}/src/war"
 *       destdir="${basedir}/gensrc"
 *       package="com.i3sp.jsp"
 *       verbose="9"&gt;
 *   &lt;include name="**\/*.jsp" /&gt;
 * &lt;/jspc&gt;
 * </pre>
 *
 * @author <a href="mailto:mattw@i3sp.com">Matthew Watson</a> <p>
 *
 *      Large Amount of cutting and pasting from the Javac task...
 * @author James Davidson <a href="mailto:duncan@x180.com">duncan@x180.com</a>
 * @author Robin Green <a href="mailto:greenrd@hotmail.com">greenrd@hotmail.com
 *      </a>
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 * @author <a href="mailto:jayglanville@home.com">J D Glanville</a>
 * @version $Revision$ $Date$
 */
public class JspC extends MatchingTask
{

    private final static String FAIL_MSG
        = "Compile failed, messages should have been provided.";
    private int verbose = 0;
    protected ArrayList compileList = new ArrayList();
    protected boolean failOnError = true;
    /*
     * ------------------------------------------------------------
     */
    private Path classpath;
    private File destDir;
    private String iepluginid;
    private boolean mapped;
    private String packageName;
    private Path src;

    /**
     * -uribase <dir>The uri directory compilations should be relative to
     * (Default is "/")
     */

    private File uribase;

    /**
     * -uriroot <dir>The root directory that uri files should be resolved
     * against,
     */
    private File uriroot;


    /*
     * ------------------------------------------------------------
     */
    /**
     * Set the classpath to be used for this compilation
     *
     * @param cp The new Classpath value
     */
    public void setClasspath( Path cp )
        throws TaskException
    {
        if( classpath == null ) {
            classpath = cp;
        } else {
            classpath.append( cp );
        }
    }

    /**
     * Set the destination directory into which the JSP source files should be
     * compiled.
     *
     * @param destDir The new Destdir value
     */
    public void setDestdir( File destDir )
    {
        this.destDir = destDir;
    }

    /*
     * ------------------------------------------------------------
     */
    /**
     * Throw a TaskException if compilation fails
     *
     * @param fail The new Failonerror value
     */
    public void setFailonerror( boolean fail )
    {
        failOnError = fail;
    }

    /**
     * Set the ieplugin id
     *
     * @param iepluginid_ The new Ieplugin value
     */
    public void setIeplugin( String iepluginid_ )
    {
        iepluginid = iepluginid_;
    }

    /**
     * set the mapped flag
     *
     * @param mapped_ The new Mapped value
     */
    public void setMapped( boolean mapped_ )
    {
        mapped = mapped_;
    }

    /*
     * ------------------------------------------------------------
     */
    /**
     * Set the name of the package the compiled jsp files should be in
     *
     * @param pkg The new Package value
     */
    public void setPackage( String pkg )
    {
        this.packageName = pkg;
    }

    /*
     * ------------------------------------------------------------
     */
    /**
     * Set the source dirs to find the source JSP files.
     *
     * @param srcDir The new Srcdir value
     */
    public void setSrcdir( Path srcDir )
        throws TaskException
    {
        if( src == null )
        {
            src = srcDir;
        }
        else
        {
            src.append( srcDir );
        }
    }

    /**
     * -uribase. the uri context of relative URI references in the JSP pages. If
     * it does not exist then it is derived from the location of the file
     * relative to the declared or derived value of -uriroot.
     *
     * @param uribase The new Uribase value
     */
    public void setUribase( File uribase )
    {
        this.uribase = uribase;
    }

    /**
     * -uriroot <dir>The root directory that uri files should be resolved
     * against, (Default is the directory jspc is invoked from)
     *
     * @param uriroot The new Uribase value
     */
    public void setUriroot( File uriroot )
    {
        this.uriroot = uriroot;
    }

    /*
     * ------------------------------------------------------------
     */
    /**
     * Set the verbose level of the compiler
     *
     * @param i The new Verbose value
     */
    public void setVerbose( int i )
    {
        verbose = i;
    }

    public Path getClasspath()
    {
        return classpath;
    }

    /*
     * ------------------------------------------------------------
     */
    public ArrayList getCompileList()
    {
        return compileList;
    }

    public File getDestdir()
    {
        return destDir;
    }

    /**
     * Gets the failonerror flag.
     *
     * @return The Failonerror value
     */
    public boolean getFailonerror()
    {
        return failOnError;
    }

    /*
     * ------------------------------------------------------------
     */
    public String getIeplugin()
    {
        return iepluginid;
    }

    public String getPackage()
    {
        return packageName;
    }

    public Path getSrcDir()
    {
        return src;
    }

    public File getUribase()
    {
        return uriroot;
    }

    public File getUriroot()
    {
        return uriroot;
    }

    public int getVerbose()
    {
        return verbose;
    }

    /*
     * ------------------------------------------------------------
     */
    public boolean isMapped()
    {
        return mapped;
    }

    /**
     * Maybe creates a nested classpath element.
     *
     * @return Description of the Returned Value
     */
    public Path createClasspath()
        throws TaskException
    {
        if( classpath == null ) {
            classpath = new Path();
        }
        Path path1 = classpath;
        final Path path = new Path();
        path1.addPath( path );
        return path;
    }

    /*
     * ------------------------------------------------------------
     */
    public void execute()
        throws TaskException
    {
        // first off, make sure that we've got a srcdir
        if( src == null )
        {
            throw new TaskException( "srcdir attribute must be set!" );
        }
        String[] list = src.list();
        if( list.length == 0 )
        {
            throw new TaskException( "srcdir attribute must be set!" );
        }

        if( destDir != null && !destDir.isDirectory() )
        {
            throw new
                TaskException( "destination directory \"" + destDir +
                               "\" does not exist or is not a directory" );
        }

        // calculate where the files will end up:
        File dest = null;
        if( packageName == null ) {
            dest = destDir;
        } else
        {
            String path = destDir.getPath() + File.separatorChar +
                packageName.replace( '.', File.separatorChar );
            dest = new File( path );
        }

        // scan source directories and dest directory to build up both copy
        // lists and compile lists
        resetFileLists();
        int filecount = 0;
        for( int i = 0; i < list.length; i++ )
        {
            File srcDir = (File)resolveFile( list[ i ] );
            if( !srcDir.exists() )
            {
                throw new TaskException( "srcdir \"" + srcDir.getPath() +
                                         "\" does not exist!" );
            }

            DirectoryScanner ds = this.getDirectoryScanner( srcDir );

            String[] files = ds.getIncludedFiles();
            filecount = files.length;
            scanDir( srcDir, dest, files );
        }

        // compile the source files

        Object compiler = getProperty( "jsp.compiler" );
        if( compiler == null )
        {
            compiler = "jasper";
        }
        getLogger().debug( "compiling " + compileList.size() + " files" );

        if( compileList.size() > 0 )
        {
            CompilerAdapter adapter =
                CompilerAdapterFactory.getCompiler( compiler.toString(), this );
            getLogger().info( "Compiling " + compileList.size() +
                              " source file"
                              + ( compileList.size() == 1 ? "" : "s" )
                              + ( destDir != null ? " to " + destDir : "" ) );

            // now we need to populate the compiler adapter
            adapter.setJspc( this );

            // finally, lets execute the compiler!!
            if( !adapter.execute() )
            {
                if( failOnError )
                {
                    throw new TaskException( FAIL_MSG );
                }
                else
                {
                    getLogger().error( FAIL_MSG );
                }
            }
        }
        else
        {
            if( filecount == 0 )
            {
                getLogger().info( "there were no files to compile" );
            }
            else
            {
                getLogger().debug( "all files are up to date" );
            }
        }
    }

    /*
     * ------------------------------------------------------------
     */
    /**
     * Clear the list of files to be compiled and copied..
     */
    protected void resetFileLists()
    {
        compileList.clear();
    }

    /*
     * ------------------------------------------------------------
     */
    /**
     * Scans the directory looking for source files to be compiled. The results
     * are returned in the class variable compileList
     *
     * @param srcDir Description of Parameter
     * @param destDir Description of Parameter
     * @param files Description of Parameter
     */
    protected void scanDir( File srcDir, File destDir, String files[] )
    {

        long now = ( new Date() ).getTime();

        for( int i = 0; i < files.length; i++ )
        {
            File srcFile = new File( srcDir, files[ i ] );
            if( files[ i ].endsWith( ".jsp" ) )
            {
                // drop leading path (if any)
                int fileStart =
                    files[ i ].lastIndexOf( File.separatorChar ) + 1;
                File javaFile = new File( destDir, files[ i ].substring( fileStart,
                                                                         files[ i ].indexOf( ".jsp" ) ) + ".java" );

                if( srcFile.lastModified() > now )
                {
                    final String message =
                        "Warning: file modified in the future: " + files[ i ];
                    getLogger().warn( message );
                }

                if( !javaFile.exists() ||
                    srcFile.lastModified() > javaFile.lastModified() )
                {
                    if( !javaFile.exists() )
                    {
                        getLogger().debug( "Compiling " + srcFile.getPath() + " because java file " + javaFile.getPath() + " does not exist" );
                    }
                    else
                    {
                        getLogger().debug( "Compiling " + srcFile.getPath() + " because it is out of date with respect to " + javaFile.getPath() );
                    }
                    compileList.add( srcFile.getAbsolutePath() );
                }
            }
        }
    }
    /*
     * ------------------------------------------------------------
     */
}
