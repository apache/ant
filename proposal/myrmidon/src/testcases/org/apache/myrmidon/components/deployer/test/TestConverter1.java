/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.components.deployer.test;

import org.apache.aut.converter.Converter;
import org.apache.aut.converter.ConverterException;

/**
 * A test converter.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 */
public class TestConverter1
    implements Converter
{
    /**
     * Convert original to destination type.
     */
    public Object convert( Class destination, Object original, Object context )
        throws ConverterException
    {
        return new TestType1();
    }
}
