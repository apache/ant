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
    
    /**
     * Method for generic Factory.
     *
     * @param info generic info
     * @return the created entry
     * @exception FactoryException if an error occurs
     */
    public Object create( final Info info )
        throws FactoryException
    {
        if( info.getClass().equals( ConverterInfo.class ) )
        {
            throw new IllegalArgumentException( "Passed incorrect Info type to factory" );
        }
        return create( (ConverterInfo)info );
    }
    
    /**
     * Non-generic factory method.
     *
     * @param info the info to create instance from
     * @return the created entry
     * @exception FactoryException if an error occurs
     */
    public Converter createConverter( final ConverterInfo info )
        throws FactoryException
    {
        final ConverterLoader loader = getLoader( info.getLocation() );
        
        try { return (Converter)loader.load( info.getClassname() ); }
        catch( final Exception e )
        {
            throw new FactoryException( "Failed loading converter from " + info.getLocation() +
                                        " due to " + e, e );
        }
    }
    
    /**
     * Get a loader for a particular location
     *
     * @param location the location 
     * @return the loader
     */
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
    
    /**
     * Create a new loader.
     * Put in another method so that it can be overridden.
     *
     * @param location the location the Loader will load from
     * @return the loader
     */
    protected ConverterLoader createLoader( final URL location )
    {
        if( null != location ) return new DefaultConverterLoader( location );
        else return new DefaultConverterLoader();
    }
}
