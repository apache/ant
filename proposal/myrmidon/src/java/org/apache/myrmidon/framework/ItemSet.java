/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.myrmidon.framework;

import org.apache.myrmidon.api.DataType;

/**
 * Interface for ItemSet.
 * An item set contains a number of items. Example item sets include
 * PatternSets, FileSets, FilterSets etc.
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public interface ItemSet
    extends DataType
{
    /**
     * Returns an array containing the items(s) contained within set.
     *
     * Question: should ItemSet be context sensitive????
     */
    Object[] getItems( /* Context context??? */ );
}
