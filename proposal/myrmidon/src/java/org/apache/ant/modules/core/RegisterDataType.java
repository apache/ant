/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.ant.modules.core;

import java.net.URL;
import org.apache.avalon.framework.camelot.DefaultLocator;
import org.apache.avalon.framework.camelot.DeploymentException;
import org.apache.avalon.framework.camelot.RegistryException;
import org.apache.myrmidon.api.TaskException;

/**
 * Method to register a single datatype.
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public class RegisterDataType
    extends AbstractResourceRegisterer
{
    protected void registerResource( final String name,
                                     final String classname,
                                     final URL url )
        throws TaskException, RegistryException
    {
        if( null == classname )
        {
            try { m_tskDeployer.deployDataType( name, url.toString(), url ); }
            catch( final DeploymentException de )
            {
                throw new TaskException( "Failed deploying " + name + " from " + url, de );
            }
        }
        else
        {
            final DefaultLocator locator = new DefaultLocator( classname, url );
            m_dataTypeEngine.getRegistry().register( name, locator );
        }
    }
}
