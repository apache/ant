/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.tools.ant.types.resources.comparators;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.Resource;

/**
 * Reverses another ResourceComparator.  If no nested ResourceComparator
 * is supplied, the compared Resources' natural order will be reversed.
 * @since Ant 1.7
 */
public class Reverse extends ResourceComparator {
    private static final String ONE_NESTED
        = "You must not nest more than one ResourceComparator for reversal.";

    private ResourceComparator nested;

    /**
     * Default constructor.
     */
    public Reverse() {
    }

    /**
     * Construct a new Reverse, supplying the ResourceComparator to be reversed.
     * @param c the ResourceComparator to reverse.
     */
    public Reverse(ResourceComparator c) {
        add(c);
    }

    /**
     * Add the ResourceComparator to reverse.
     * @param c the ResourceComparator to add.
     */
    public void add(ResourceComparator c) {
        if (nested != null) {
            throw new BuildException(ONE_NESTED);
        }
        nested = c;
    }

    /**
     * Compare two Resources.
     * @param foo the first Resource.
     * @param bar the second Resource.
     * @return a negative integer, zero, or a positive integer as the first
     *         argument is greater than, equal to, or less than the second.
     */
    protected int resourceCompare(Resource foo, Resource bar) {
        return -1 * (nested == null
            ? foo.compareTo(bar) : nested.compare(foo, bar));
    }

}
