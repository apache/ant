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
package org.apache.tools.ant.types;

// java io classes




// java util classes
import java.util.Enumeration;
import java.util.Vector;

// ant classes




/**
 * A FilterSetCollection is a collection of filtersets each of which may have
 * a different start/end token settings.
 *
 */
public class FilterSetCollection {

    private Vector filterSets = new Vector();

    /**
     * Constructor for a FilterSetCollection.
     */
    public FilterSetCollection() {
    }

    /**
     * Constructor for a FilterSetCollection.
     * @param filterSet a filterset to start the collection with
     */
    public FilterSetCollection(FilterSet filterSet) {
        addFilterSet(filterSet);
    }


    /**
     * Add a filterset to the collection.
     *
     * @param filterSet a <code>FilterSet</code> value
     */
    public void addFilterSet(FilterSet filterSet) {
        filterSets.addElement(filterSet);
    }

    /**
     * Does replacement on the given string with token matching.
     * This uses the defined begintoken and endtoken values which default to @ for both.
     *
     * @param line  The line to process the tokens in.
     * @return      The string with the tokens replaced.
     */
    public String replaceTokens(String line) {
        String replacedLine = line;
        for (Enumeration e = filterSets.elements(); e.hasMoreElements();) {
            FilterSet filterSet = (FilterSet) e.nextElement();
            replacedLine = filterSet.replaceTokens(replacedLine);
        }
        return replacedLine;
    }

    /**
    * Test to see if this filter set it empty.
    *
    * @return   Return true if there are filter in this set otherwise false.
    */
    public boolean hasFilters() {
        for (Enumeration e = filterSets.elements(); e.hasMoreElements();) {
            FilterSet filterSet = (FilterSet) e.nextElement();
            if (filterSet.hasFilters()) {
                return true;
            }
        }
        return false;
    }
}



