/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.ant.configuration;

/**
 * Hostile fork till Avalon gets equivelent functionality ;)
 */
public class SAXConfigurationHandler
    extends org.apache.avalon.SAXConfigurationHandler
{
    protected org.apache.avalon.DefaultConfiguration 
        createConfiguration( final String localName, final String location )
    {
        return new DefaultConfiguration( localName, location );
    }
}
