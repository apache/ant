/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.java;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.framework.FileSet;
import org.apache.myrmidon.framework.file.FileList;
import org.apache.myrmidon.framework.file.Path;
import org.apache.tools.todo.types.DirectoryScanner;
import org.apache.tools.todo.types.ScannerUtil;
import org.apache.tools.todo.types.SourceFileScanner;
import org.apache.tools.todo.util.mappers.GlobPatternMapper;

/**
 * An abstract Java compiler.
 *
 * @author James Davidson <a href="mailto:duncan@x180.com">duncan@x180.com</a>
 * @author Robin Green <a href="mailto:greenrd@hotmail.com">greenrd@hotmail.com
 *      </a>
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 * @author <a href="mailto:jayglanville@home.com">J D Glanville</a>
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 *
 * @ant.role shorthand="java-compiler"
 */
public abstract class JavaCompilerAdaptor
{
    private TaskContext m_context;
    private Path m_classPath = new Path();
    private ArrayList m_sourceFilesets = new ArrayList();
    private boolean m_debug;
    private boolean m_deprecation;
    private File m_destDir;

    /**
     * Sets the context for this adaptor.
     */
    public void setContext( final TaskContext context )
    {
        m_context = context;
    }

    /**
     * Returns the context for this adaptor.
     */
    protected TaskContext getContext()
    {
        return m_context;
    }

    /**
     * Enables debug in the compiled classes.
     */
    public void setDebug( final boolean debug )
    {
        m_debug = debug;
    }

    /**
     * Returns the 'debug' flag.
     */
    protected boolean isDebug()
    {
        return m_debug;
    }

    /**
     * Sets the destination directory.
     */
    public void setDestDir( final File destDir )
    {
        m_destDir = destDir;
    }

    /**
     * Returns the destination directory.
     */
    protected File getDestDir()
    {
        return m_destDir;
    }

    /**
     * Enables deprecation info.
     */
    public void setDeprecation( final boolean deprecation )
    {
        m_deprecation = deprecation;
    }

    /**
     * Returns the 'deprecation' flag.
     */
    protected boolean isDeprecation()
    {
        return m_deprecation;
    }

    /**
     * Adds a source fileset.
     */
    public void addSrc( final FileSet fileset )
    {
        m_sourceFilesets.add( fileset );
    }

    /**
     * Adds a class-path element.
     */
    public void addClasspath( final Path path )
    {
        m_classPath.add( path );
    }

    /**
     * Returns the classpath
     */
    protected FileList getClassPath()
    {
        return m_classPath;
    }

    /**
     * Invokes the compiler.
     */
    public void execute()
        throws TaskException
    {
        validate();

        // Build the list of files to compile
        final File[] compileList = getCompileList();
        logFiles( compileList );

        if( compileList.length == 0 )
        {
            return;
        }

        // Compile
        compile( compileList );
    }

    /**
     * Compiles a set of files.
     */
    protected abstract void compile( final File[] files )
        throws TaskException;

    /**
     * Logs the details of what is going to be compiled.
     */
    private void logFiles( final File[] compileList )
    {
        // Log
        final String message = "Compiling " + compileList.length + " source files to " + m_destDir;
        getContext().info( message );
        if( getContext().isVerboseEnabled() )
        {
            getContext().verbose( "Compiling the following files:" );
            for( int i = 0; i < compileList.length; i++ )
            {
                final File file = compileList[ i ];
                getContext().verbose( file.getAbsolutePath() );
            }
        }
    }

    /**
     * Builds the set of file to compile.
     */
    private File[] getCompileList()
        throws TaskException
    {
        final ArrayList allFiles = new ArrayList();
        for( int i = 0; i < m_sourceFilesets.size(); i++ )
        {
            final FileSet fileSet = (FileSet)m_sourceFilesets.get( i );
            final DirectoryScanner scanner = ScannerUtil.getDirectoryScanner( fileSet );
            final String[] files = scanner.getIncludedFiles();
            restrictFiles( fileSet.getDir(), files, allFiles );
        }
        return (File[])allFiles.toArray( new File[ allFiles.size() ] );
    }

    /**
     * Restricts a set of source files to those that are out-of-date WRT
     * their class file.
     */
    private void restrictFiles( final File srcDir,
                                final String files[],
                                final List acceptedFiles )
        throws TaskException
    {
        final GlobPatternMapper mapper = new GlobPatternMapper();
        mapper.setFrom( "*.java" );
        mapper.setTo( "*.class" );
        final SourceFileScanner sfs = new SourceFileScanner();
        final File[] newFiles = sfs.restrictAsFiles( files,
                                                     srcDir,
                                                     m_destDir,
                                                     mapper,
                                                     getContext() );

        for( int i = 0; i < newFiles.length; i++ )
        {
            final File file = newFiles[i ];
            acceptedFiles.add( file );
        }
    }

    /**
     * Validates the compiler settings.
     */
    private void validate() throws TaskException
    {
        // Validate the destination directory
        if( m_destDir == null )
        {
            throw new TaskException( "No destination directory specified." );
        }
        if( m_destDir.exists() )
        {
            if( !m_destDir.isDirectory() )
            {
                throw new TaskException( "Destination " + m_destDir + " is not a directory." );
            }
        }
        else
        {
            if( !m_destDir.mkdirs() )
            {
                throw new TaskException( "Cannot create destination directory " + m_destDir );
            }
        }
    }
}
