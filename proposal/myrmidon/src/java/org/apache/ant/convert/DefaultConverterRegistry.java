/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.ant.convert;

import java.util.HashMap;
import org.apache.avalon.camelot.AbstractRegistry;
import org.apache.avalon.camelot.Info;
import org.apache.avalon.camelot.RegistryException;

/**
 * Default implementation of ConverterInfo registry.
 * 
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public class DefaultConverterRegistry
    extends AbstractRegistry
    implements ConverterRegistry
{
    protected final HashMap         m_mapping        = new HashMap();

    /**
     * Retrieve ConverterInfo that describes converter that converts from source to destination.
     *
     * @param source the source classname
     * @param destination the destination classname
     * @return the converter-info or null if none available
     */
    public String getConverterInfoName( final String source, final String destination )
    {
        final HashMap map = (HashMap)m_mapping.get( source );
        if( null == map ) return null;
        return (String)map.get( destination );
    }

    protected void checkInfo( final String name, final Info info )
        throws RegistryException
    {
        super.checkInfo( name, info );

        final ConverterInfo converterInfo = (ConverterInfo)info;
        final String source = converterInfo.getSource();
        final String destination = converterInfo.getDestination();

        HashMap map = (HashMap)m_mapping.get( source );
        if( null == map )
        {
            map = new HashMap();
            m_mapping.put( source, map );
        }
        
        map.put( destination, name );        
    }

    protected Class getInfoClass()
    {
        return ConverterInfo.class;
    }
}
