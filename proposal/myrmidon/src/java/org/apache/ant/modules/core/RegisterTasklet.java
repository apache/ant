/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.ant.modules.core;

import java.net.URL;
import org.apache.ant.AntException;
import org.apache.avalon.camelot.DefaultLocator;
import org.apache.avalon.camelot.DeploymentException;
import org.apache.avalon.camelot.RegistryException;

/**
 * Method to register a single tasklet.
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public class RegisterTasklet 
    extends AbstractResourceRegisterer
{
    protected void registerResource( final String name, 
                                     final String classname, 
                                     final URL url )
        throws AntException, RegistryException 
    {
        if( null == classname )
        {
            try { m_engine.getTskDeployer().deployTasklet( name, url.toString(), url ); }
            catch( final DeploymentException de )
            {
                throw new AntException( "Failed deploying " + name + " from " + url, de );
            }
        }
        else
        {
            final DefaultLocator locator = new DefaultLocator( classname, url );
            m_engine.getRegistry().register( name, locator ); 
        }
    }
}
