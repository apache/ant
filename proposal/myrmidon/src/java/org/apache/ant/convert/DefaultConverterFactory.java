/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.ant.convert;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import org.apache.ant.convert.Converter;
import org.apache.avalon.camelot.Entry;
import org.apache.avalon.camelot.Factory;
import org.apache.avalon.camelot.Loader;
import org.apache.avalon.camelot.FactoryException;
import org.apache.avalon.camelot.Info;

/**
 * Facility used to load Converters.
 * 
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public class DefaultConverterFactory
    implements ConverterFactory
{
    protected final HashMap         m_loaders        = new HashMap();
    
    public Entry create( final Info info )
        throws FactoryException
    {
        if( info.getClass().equals( ConverterInfo.class ) )
        {
            throw new IllegalArgumentException( "Passed incorrect Info type to factory" );
        }
        return create( (ConverterInfo)info );
    }
    
    public ConverterEntry create( final ConverterInfo info )
        throws FactoryException
    {
        final ConverterLoader loader = getLoader( info.getLocation() );
        
        Object object = null;
        
        try { object = loader.load( info.getClassname() ); }
        catch( final Exception e )
        {
            throw new FactoryException( "Failed loading converter from " + info.getLocation() +
                                        " due to " + e, e );
        }
        
        return new ConverterEntry( info, (Converter)object );        
    }
    
    protected ConverterLoader getLoader( final URL location )
    {
        ConverterLoader loader = (ConverterLoader)m_loaders.get( location );
        
        if( null == loader )
        {
            loader = createLoader( location );
            m_loaders.put( location, loader );
        }
        
        return loader;
    }
    
    protected ConverterLoader createLoader( final URL location )
    {
        if( null != location ) return new DefaultConverterLoader( location );
        else return new DefaultConverterLoader();
    }
}
