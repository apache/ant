/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.ant.configuration;

import org.xml.sax.SAXException;

/**
 * Hostile fork till Avalon gets equivelent functionality ;)
 */
public class ConfigurationBuilder
    extends org.apache.avalon.DefaultConfigurationBuilder
{
    public ConfigurationBuilder()
        throws SAXException
    {
        super();
    }

    protected org.apache.avalon.SAXConfigurationHandler getHandler()
    {
        return new SAXConfigurationHandler();
    }
}
