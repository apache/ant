/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.ant.convert;

/**
 * Instances of this interface are used to convert between different types.
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public interface Converter
{
    /**
     * Convert original to destination type.
     * Destination is passed so that one converter can potentiall 
     * convert to multiple different types.
     *
     * @param destination the destinaiton type
     * @param original the original type
     * @return the converted object
     * @exception Exception if an error occurs
     */
    Object convert( Class destination, Object original )
        throws Exception;
}
