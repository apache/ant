/*
 * Copyright  2003-2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
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

package org.apache.tools.ant.types.selectors.modifiedselector;


import java.util.Comparator;


/**
 * Simple implementation of Comparator for use in CacheSelector.
 * compare() returns '0' (should not be selected) if both parameter
 * are equal otherwise '1' (should be selected).
 *
 * @version 2003-09-13
 * @since  Ant 1.6
 */
public class EqualComparator implements Comparator {

    /**
     * Implements Comparator.compare().
     * @param o1 the first object
     * @param o2 the second object
     * @return 0, if both are equal, otherwise 1
     */
    public int compare(Object o1, Object o2) {
        if (o1 == null) {
            if (o2 == null) {
                return 1;
            } else {
                return 0;
            }
        } else {
            return (o1.equals(o2)) ? 0 : 1;
        }
    }

    /**
     * Override Object.toString().
     * @return information about this comparator
     */
    public String toString() {
        return "EqualComparator";
    }
}
