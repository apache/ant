/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.framework.factories;

import java.io.File;
import org.apache.aut.nativelib.impl.DefaultExecManager;
import org.apache.avalon.framework.context.Context;
import org.apache.avalon.framework.context.ContextException;
import org.apache.avalon.framework.context.Contextualizable;
import org.apache.myrmidon.interfaces.service.AntServiceException;
import org.apache.myrmidon.interfaces.service.ServiceFactory;

/**
 * A Factory responsible for creating the ExecManager service.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
public class ExecManagerFactory
    implements ServiceFactory, Contextualizable
{
    private File m_homeDir;

    public void contextualize( final Context context ) throws ContextException
    {
        m_homeDir = (File)context.get( "myrmidon.home" );
    }

    /**
     * Create the ExecManager Service.
     */
    public Object createService()
        throws AntServiceException
    {
        try
        {
            return new DefaultExecManager( m_homeDir );
        }
        catch( final Exception ee )
        {
            throw new AntServiceException( ee.getMessage(), ee );
        }
    }
}
