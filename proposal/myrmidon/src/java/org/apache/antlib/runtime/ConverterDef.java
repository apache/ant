/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.runtime;

import java.io.File;
import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.myrmidon.api.AbstractTask;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.interfaces.deployer.Deployer;
import org.apache.myrmidon.interfaces.deployer.TypeDeployer;

/**
 * Task to define a converter.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 */
public class ConverterDef
    extends AbstractTask
{
    private final static Resources REZ =
        ResourceManager.getPackageResources( ConverterDef.class );

    private String m_sourceType;
    private String m_destinationType;
    private File m_lib;
    private String m_classname;

    public void setLib( final File lib )
    {
        m_lib = lib;
    }

    public void setClassname( final String classname )
    {
        m_classname = classname;
    }

    public void setSourceType( final String sourceType )
    {
        m_sourceType = sourceType;
    }

    public void setDestinationType( final String destinationType )
    {
        m_destinationType = destinationType;
    }

    public void execute()
        throws TaskException
    {
        if( null == m_classname )
        {
            final String message = REZ.getString( "converterdef.no-classname.error" );
            throw new TaskException( message );
        }
        else if( null == m_sourceType )
        {
            final String message = REZ.getString( "converterdef.no-source.error" );
            throw new TaskException( message );
        }
        else if( null == m_destinationType )
        {
            final String message = REZ.getString( "converterdef.no-destination.error" );
            throw new TaskException( message );
        }
        else if( null == m_lib )
        {
            final String message = REZ.getString( "converterdef.no-lib.error" );
            throw new TaskException( message );
        }

        try
        {
            // Locate the deployer, then deploy the converter
            final Deployer deployer = (Deployer)getService( Deployer.class );
            final TypeDeployer typeDeployer = deployer.createDeployer( m_lib );
            typeDeployer.deployConverter( m_classname, m_sourceType, m_destinationType );
        }
        catch( final Exception e )
        {
            final String message = REZ.getString( "converterdef.no-register.error", m_classname );
            throw new TaskException( message, e );
        }
    }
}
