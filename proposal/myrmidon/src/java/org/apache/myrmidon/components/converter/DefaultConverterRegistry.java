/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.myrmidon.components.converter;

import java.util.HashMap;
import org.apache.myrmidon.interfaces.converter.ConverterRegistry;

/**
 * Default implementation of Converter registry.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 */
public class DefaultConverterRegistry
    implements ConverterRegistry
{
    private final HashMap m_mapping = new HashMap();

    public String getConverterName( final String source, final String destination )
    {
        final HashMap map = (HashMap)m_mapping.get( source );
        if( null == map ) return null;
        return (String)map.get( destination );
    }

    public void registerConverter( final String className,
                                   final String source,
                                   final String destination )
    {
        HashMap map = (HashMap)m_mapping.get( source );
        if( null == map )
        {
            map = new HashMap();
            m_mapping.put( source, map );
        }

        map.put( destination, className );
    }
}
