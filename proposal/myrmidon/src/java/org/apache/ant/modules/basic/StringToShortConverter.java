/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.ant.modules.basic;

import org.apache.ant.convert.AbstractConverter;
import org.apache.avalon.Context;

/**
 * String to short converter
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public class StringToShortConverter
    extends AbstractConverter
{
    public StringToShortConverter()
    {
        super( String.class, Short.class );
    }

    public Object convert( final Object original, final Context context )
        throws Exception
    {
        return new Short( (String)original );
    }
}

