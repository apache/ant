/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.ant.convert;

import org.apache.ant.convert.Converter;
import org.apache.avalon.camelot.AbstractEntry;

public class ConverterEntry
    extends AbstractEntry
{
    public ConverterEntry( final ConverterInfo info, final Converter converter )
    {
        super( info, converter );
    }
    
    /**
     * Retrieve instance of converter.
     *
     * @return the component instance
     */
    public Converter getConverter()
    {
        return (Converter)getInstance();
    }
}

