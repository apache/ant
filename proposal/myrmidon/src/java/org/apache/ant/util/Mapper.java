/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.ant.util;

import org.apache.ant.tasklet.DataType;

/**
 * Interface for Mappers.
 * Mappers are responsible for mapping source items to targets items.
 * Example mappers will map source files to destination files 
 * (ie A.java to A.class).
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 */
public interface Mapper
    extends DataType
{
    /**
     * Returns an array containing the target items(s) for the
     * given source file.
     *
     * <p>if the given rule doesn't apply to the input item,
     * implementation must return null. Scanner will then
     * omit the item in question.</p> 
     *
     * @param item the item to be mapped
     */
    Object[] mapItem( Object item );
}
