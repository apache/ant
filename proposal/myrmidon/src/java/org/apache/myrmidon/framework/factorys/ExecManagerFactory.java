/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.framework.factorys;

import java.io.File;
import org.apache.aut.nativelib.ExecException;
import org.apache.aut.nativelib.impl.DefaultExecManager;
import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.myrmidon.interfaces.service.ServiceException;
import org.apache.myrmidon.interfaces.service.ServiceFactory;

/**
 * A Factory responsible for creating the ExecManager service.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
public class ExecManagerFactory
    implements ServiceFactory
{
    private final static Resources REZ =
        ResourceManager.getPackageResources( ExecManagerFactory.class );

    /**
     * Create the ExecManager Service.
     */
    public Object createService()
        throws ServiceException
    {
        final File home = getHomeDirectory();
        try
        {
            return new DefaultExecManager( home );
        }
        catch( final ExecException ee )
        {
            throw new ServiceException( ee.getMessage(), ee );
        }
    }

    /**
     * Utility method to retrieve home directory.
     */
    private static File getHomeDirectory()
        throws ServiceException
    {
        final String home = System.getProperty( "myrmidon.home" );
        if( null == home )
        {
            final String message = REZ.getString( "missing-home-dir.error" );
            throw new ServiceException( message );
        }

        return new File( home );
    }
}
