/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.components.configurer.test;

import org.apache.aut.converter.AbstractConverter;
import org.apache.aut.converter.ConverterException;
import org.apache.myrmidon.components.configurer.test.MyRole1;
import org.apache.myrmidon.components.configurer.test.MyRole1Adaptor;

/**
 * Converts from Object to MyRole1.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 */
public class ObjectToMyRole1Converter
    extends AbstractConverter
{
    public ObjectToMyRole1Converter()
    {
        super( Object.class, MyRole1.class );
    }

    protected Object convert( Object original, Object context )
        throws ConverterException
    {
        return new MyRole1Adaptor( original );
    }
}
