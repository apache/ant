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
import org.apache.avalon.camelot.AbstractLoader;

/**
 * Class used to load converters et al from a source.
 * 
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public class DefaultConverterLoader
    extends AbstractLoader
    implements ConverterLoader
{
    public DefaultConverterLoader()
    {
        super( new URLClassLoader( new URL[0], 
                                   Thread.currentThread().getContextClassLoader() ) );
    }
    
    public DefaultConverterLoader( final URL location )
    {
        super( new URLClassLoader( new URL[] { location } ) );
    }
    
    public Converter loadConverter( final String converter )
        throws Exception
    {
        return (Converter)load( converter );
    }
}
