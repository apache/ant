/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.myrmidon.components.converter;

import java.util.HashMap;

/**
 * Default implementation of ConverterInfo registry.
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public class DefaultConverterRegistry
    implements ConverterRegistry
{
    private final HashMap      m_mapping   = new HashMap();

    public String getConverterInfoName( final String source, final String destination )
    {
        final HashMap map = (HashMap)m_mapping.get( source );
        if( null == map ) return null;
        return (String)map.get( destination );
    }

    public void registerConverterInfo( final String className, final ConverterInfo info )
    {
        final String source = info.getSource();
        final String destination = info.getDestination();

        HashMap map = (HashMap)m_mapping.get( source );
        if( null == map )
        {
            map = new HashMap();
            m_mapping.put( source, map );
        }

        map.put( destination, className );
    }
}
