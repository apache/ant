/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.extensions;

import java.io.File;
import java.util.Iterator;
import java.util.Vector;
import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.myrmidon.api.AbstractTask;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.framework.FileSet;
import org.apache.tools.todo.types.DirectoryScanner;
import org.apache.tools.todo.types.ScannerUtil;

/**
 * Display the "Optional Package" and "Package Specification" information
 * contained within the specified jars.
 *
 * <p>Prior to JDK1.3, an "Optional Package" was known as an Extension.
 * The specification for this mechanism is available in the JDK1.3
 * documentation in the directory
 * $JDK_HOME/docs/guide/extensions/versioning.html. Alternatively it is
 * available online at <a href="http://java.sun.com/j2se/1.3/docs/guide/extensions/versioning.html">
 * http://java.sun.com/j2se/1.3/docs/guide/extensions/versioning.html</a>.</p>
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @ant.task name="extension-display"
 */
public class ExtensionDisplayTask
    extends AbstractTask
{
    private final static Resources REZ =
        ResourceManager.getPackageResources( ExtensionDisplayTask.class );

    /**
     * The library to display information about.
     */
    private File m_file;

    /**
     * Filesets specifying all the librarys
     * to display information about.
     */
    private final Vector m_filesets = new Vector();

    /**
     * The jar library to display information for.
     *
     * @param file The jar library to display information for.
     */
    public void setFile( final File file )
    {
        m_file = file;
    }

    /**
     * Adds a set of files about which library data will be displayed.
     *
     * @param fileSet a set of files about which library data will be displayed.
     */
    public void addFileset( final FileSet fileSet )
    {
        m_filesets.addElement( fileSet );
    }

    public void execute()
        throws TaskException
    {
        validate();

        final LibraryDisplay displayer = new LibraryDisplay();
        // Check if list of files to check has been specified
        if( !m_filesets.isEmpty() )
        {
            final Iterator iterator = m_filesets.iterator();
            while( iterator.hasNext() )
            {
                final FileSet fileSet = (FileSet)iterator.next();
                final DirectoryScanner scanner = ScannerUtil.getDirectoryScanner( fileSet );
                final File basedir = scanner.getBasedir();
                final String[] files = scanner.getIncludedFiles();
                for( int i = 0; i < files.length; i++ )
                {
                    final File file = new File( basedir, files[ i ] );
                    displayer.displayLibrary( file );
                }
            }
        }
        else
        {
            displayer.displayLibrary( m_file );
        }
    }

    /**
     * Validate the tasks parameters.
     *
     * @throws TaskException if invalid parameters found
     */
    private void validate()
        throws TaskException
    {
        if( null == m_file && m_filesets.isEmpty() )
        {
            final String message = REZ.getString( "extension.missing-file.error" );
            throw new TaskException( message );
        }
        if( null != m_file && !m_file.exists() )
        {
            final String message = REZ.getString( "extension.file-noexist.error", m_file );
            throw new TaskException( message );
        }
        if( null != m_file && !m_file.isFile() )
        {
            final String message = REZ.getString( "extension.bad-file.error", m_file );
            throw new TaskException( message );
        }
    }
}
