/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.ant.convert.engine;

import org.apache.avalon.framework.camelot.Registry;

/**
 * Interface for registry for ConverterInfos.
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public interface ConverterRegistry
    extends Registry
{
    /**
     * Retrieve name of ConverterInfo that describes converter that converts 
     * from source to destination.
     *
     * @param source the source classname
     * @param destination the destination classname
     * @return the converter-info or null if none available
     */
    String getConverterInfoName( String source, String destination );
}
