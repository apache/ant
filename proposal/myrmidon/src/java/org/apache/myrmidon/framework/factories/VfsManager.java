/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.framework.factories;

import org.apache.aut.vfs.FileSystemException;
import org.apache.aut.vfs.impl.DefaultFileSystemManager;
import org.apache.aut.vfs.provider.FileSystemProvider;
import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.avalon.framework.activity.Disposable;
import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.myrmidon.interfaces.type.TypeFactory;
import org.apache.myrmidon.interfaces.type.TypeManager;

/**
 * The myrmidon FileSystemManager implementation.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 */
public class VfsManager
    extends DefaultFileSystemManager
    implements Serviceable, Initializable, Disposable
{
    private final static Resources REZ
        = ResourceManager.getPackageResources( VfsManager.class );

    private TypeManager m_typeManager;

    /**
     * Locate the services used by this service.
     */
    public void service( final ServiceManager serviceManager ) throws ServiceException
    {
        m_typeManager = (TypeManager)serviceManager.lookup( TypeManager.ROLE );
    }

    /**
     * Initialises this service.
     */
    public void initialize() throws Exception
    {
        final TypeFactory factory = m_typeManager.getFactory( FileSystemProvider.ROLE );

        // TODO - make this list configurable

        // Required providers
        addProvider( factory, new String[]{"zip", "jar"}, "zip", false );

        // Optional providers
        addProvider( factory, new String[]{"smb"}, "smb", true );
        addProvider( factory, new String[]{"ftp"}, "ftp", true );
    }

    /**
     * Disposes this service.
     */
    public void dispose()
    {
        // Clean-up
        close();
    }

    /**
     * Registers a file system provider.
     */
    private void addProvider( final TypeFactory factory,
                              final String[] urlSchemes,
                              final String providerName,
                              final boolean ignoreIfNotPresent )
        throws FileSystemException
    {
        // Create an instance
        if( ignoreIfNotPresent && !factory.canCreate( providerName ) )
        {
            return;
        }

        final FileSystemProvider provider;
        try
        {
            provider = (FileSystemProvider)factory.create( providerName );
        }
        catch( Exception e )
        {
            final String message = REZ.getString( "create-provider.error", providerName );
            throw new FileSystemException( message, e );
        }

        // Register the provider
        addProvider( urlSchemes, provider );
    }

}
