/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.myrmidon.components.converter;

import org.apache.avalon.framework.component.Component;

/**
 * Interface for registry for ConverterInfos.
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public interface ConverterRegistry
    extends Component
{
    String ROLE = "org.apache.myrmidon.components.converter.ConverterRegistry";

    /**
     * Retrieve name of ConverterInfo that describes converter that converts
     * from source to destination.
     *
     * @param source the source classname
     * @param destination the destination classname
     * @return the className of converter or null if none available
     */
    String getConverterInfoName( String source, String destination );

    /**
     * Register a converter-info
     *
     * @param className the className of converter
     * @param info the ConverterInfo
     */
    void registerConverterInfo( String className, ConverterInfo info );
}
