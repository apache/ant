/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.aut.converter;

/**
 * Instances of this interface are used to convert between different types.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 * @ant:role shorthand="converter"
 */
public interface Converter
{
    String ROLE = Converter.class.getName();

    /**
     * Convert original to destination type.
     * Destination is passed so that one converter can potentiall
     * convert to multiple different types.
     *
     * @param destination the destinaiton type
     * @param original the original type
     * @param context the context in which to convert
     * @return the converted object
     * @exception ConverterException if an error occurs
     */
    Object convert( Class destination, Object original, ConverterContext context )
        throws ConverterException;
}
