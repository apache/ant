/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.ant.convert;

import org.apache.avalon.Component;
import org.apache.avalon.camelot.LocatorRegistry;

/**
 * Converter engine to handle converting between types.
 * 
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public interface ConverterEngine
    extends Component, Converter
{
    /**
     * Get registry used to locate converters.
     *
     * @return the LocatorRegistry
     */
    LocatorRegistry getRegistry();

    /**
     * Get registry for converterInfo objects.
     *
     * @return the ConverterRegistry
     */
    ConverterRegistry getInfoRegistry();
}
