/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.ant.convert.core;

import org.apache.ant.convert.AbstractConverter;

/**
 * String to long converter
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public class StringToLongConverter
    extends AbstractConverter
{
    public StringToLongConverter()
    {
        super( String.class, Long.class );
    }

    public Object convert( final Object original )
        throws Exception
    {
        return new Long( (String)original );
    }
}

