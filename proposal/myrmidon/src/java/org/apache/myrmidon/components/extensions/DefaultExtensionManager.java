/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.components.extensions;

import java.io.File;
import org.apache.avalon.excalibur.extension.DefaultPackageRepository;
import org.apache.avalon.excalibur.util.StringUtil;
import org.apache.avalon.framework.activity.Disposable;
import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.logger.LogEnabled;
import org.apache.avalon.framework.logger.Logger;
import org.apache.avalon.framework.parameters.ParameterException;
import org.apache.avalon.framework.parameters.Parameterizable;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.myrmidon.interfaces.extensions.ExtensionManager;

/**
 * PhoenixPackageRepository
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
public class DefaultExtensionManager
    extends DefaultPackageRepository
    implements LogEnabled, Parameterizable, Initializable, Disposable, ExtensionManager
{
    private Logger m_logger;

    private String m_path;

    public DefaultExtensionManager()
    {
        super( new File[ 0 ] );
    }

    public void enableLogging( final Logger logger )
    {
        m_logger = logger;
    }

    public void parameterize( final Parameters parameters )
        throws ParameterException
    {
        final String phoenixHome = parameters.getParameter( "myrmidon.home" );
        final String defaultExtPath = phoenixHome + File.separator + "ext";
        m_path = parameters.getParameter( "myrmidon.ext.path", defaultExtPath );
    }

    public void initialize()
        throws Exception
    {
        final String[] pathElements = StringUtil.split( m_path, "|" );

        final File[] dirs = new File[ pathElements.length ];
        for( int i = 0; i < dirs.length; i++ )
        {
            dirs[ i ] = new File( pathElements[ i ] );
        }

        setPath( dirs );

        scanPath();
    }

    public void dispose()
    {
        clearCache();
    }

    protected void debug( final String message )
    {
        m_logger.debug( message );
    }
}
