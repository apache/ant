/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.core;

import java.util.Properties;

/**
 * This class is an UGLY HACK utility class to enable us to reuse
 * the property parsing and loading code from Properties object.
 */
class PropertyLoader
    extends Properties
{
    private LoadProperties m_loadProperties;

    public PropertyLoader( LoadProperties loadProperties )
    {
        m_loadProperties = loadProperties;
    }

    /**
     * Overidden put to add unresolved values.
     */
    public synchronized Object put( Object key, Object value )
    {
        m_loadProperties.addUnresolvedValue( key.toString(), value.toString() );
        return null;
    }
}
