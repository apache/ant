/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.aut.vfs.impl;

import java.io.File;
import java.util.ArrayList;
import org.apache.aut.vfs.FileConstants;
import org.apache.aut.vfs.FileObject;
import org.apache.aut.vfs.FileSelector;
import org.apache.aut.vfs.FileSystemException;
import org.apache.aut.vfs.provider.FileReplicator;
import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.avalon.framework.activity.Disposable;
import org.apache.avalon.framework.logger.AbstractLogEnabled;

/**
 * A simple file replicator.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 */
public class DefaultFileReplicator
    extends AbstractLogEnabled
    implements FileReplicator, Disposable
{
    private static final Resources REZ =
        ResourceManager.getPackageResources( DefaultFileReplicator.class );

    private final DefaultFileSystemManager m_manager;
    private final File m_tempDir;
    private final ArrayList m_copies = new ArrayList();
    private long m_filecount;

    public DefaultFileReplicator( final DefaultFileSystemManager manager )
    {
        m_manager = manager;
        m_tempDir = new File( "ant_vfs_cache" ).getAbsoluteFile();
    }

    /**
     * Deletes the temporary files.
     */
    public void dispose()
    {
        while( m_copies.size() > 0 )
        {
            final FileObject file = (FileObject)m_copies.remove( 0 );
            try
            {
                file.delete( FileConstants.SELECT_ALL );
            }
            catch( final FileSystemException e )
            {
                final String message = REZ.getString( "delete-temp.warn", file.getName() );
                getLogger().warn( message, e );
            }
        }
    }

    /**
     * Creates a local copy of the file, and all its descendents.
     */
    public File replicateFile( final FileObject srcFile,
                               final FileSelector selector )
        throws FileSystemException
    {
        // TODO - this is awful

        // Create a unique-ish file name
        final String basename = m_filecount + "_" + srcFile.getName().getBaseName();
        m_filecount++;
        final File file = new File( m_tempDir, basename );

        try
        {
            // Copy from the source file
            final FileObject destFile = m_manager.convert( file );
            destFile.copyFrom( srcFile, selector );

            // Keep track of the copy
            m_copies.add( destFile );
        }
        catch( final FileSystemException e )
        {
            final String message = REZ.getString( "replicate-file.error", srcFile.getName(), file );
            throw new FileSystemException( message, e );
        }

        return file;
    }

}
