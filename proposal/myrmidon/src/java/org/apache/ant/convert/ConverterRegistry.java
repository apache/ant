/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.ant.convert;

import org.apache.avalon.camelot.Registry;

public interface ConverterRegistry
    extends Registry
{
    ConverterInfo getConverterInfo( String source, String destination );
}
