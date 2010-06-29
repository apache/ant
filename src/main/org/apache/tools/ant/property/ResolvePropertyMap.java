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
package org.apache.tools.ant.property;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.Collection;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.BuildException;

/**
 * Class to resolve properties in a map. This class is explicitly not threadsafe.
 * @since Ant 1.8.0
 */
public class ResolvePropertyMap implements GetProperty {
    private final Set seen = new HashSet();
    private final ParseProperties parseProperties;
    private final GetProperty master;
    private Map map;
    private String prefix;
    // whether properties of the value side of the map should be
    // expanded
    private boolean prefixValues = false;
    // whether the current getProperty call is expanding the key side
    // of the map
    private boolean expandingLHS = true;

    /**
     * Constructor with a master getproperty and a collection of expanders.
     * @param project the current ant project.
     * @param master the master property holder (usually PropertyHelper)
     * @param expanders a collection of expanders (usually from PropertyHelper).
     */
    public ResolvePropertyMap(Project project, GetProperty master, Collection expanders) {
        this.master = master;
        this.parseProperties = new ParseProperties(project, expanders, this);
    }

    /**
     * Returns the value of a property if it is set.
     * @param name name of the property.
     * @return the property value, or null for no match or for name being null.
     */
    public Object getProperty(String name) {
        if (seen.contains(name)) {
            throw new BuildException(
                "Property " + name + " was circularly " + "defined.");
        }

        try {
            String fullKey = name;
            if (prefix != null && prefixValues) {
                fullKey = prefix + name;
            }
            Object masterValue = expandingLHS
                ? null : master.getProperty(fullKey);
            // if the property is defined outside of this map don't
            // consult the map at all.
            if (masterValue != null) {
                return masterValue;
            }

            expandingLHS = false;
            seen.add(name);
            return parseProperties.parseProperties((String) map.get(name));
        } finally {
            seen.remove(name);
        }
    }

    /**
     * The action method - resolves all the properties in a map.
     * @param map the map to resolve properties in.
     * @deprecated since Ant 1.8.2, use the three-arg method instead.
     */
    public void resolveAllProperties(Map map) {
        resolveAllProperties(map, null, false);
    }

    /**
     * The action method - resolves all the properties in a map.
     * @param map the map to resolve properties in.
     * @param prefix the prefix the properties defined inside the map
     * will finally receive - may be null.
     * @deprecated since Ant 1.8.2, use the three-arg method instead.
     */
    public void resolveAllProperties(Map map, String prefix) {
        resolveAllProperties(map, null, false);
    }

    /**
     * The action method - resolves all the properties in a map.
     * @param map the map to resolve properties in.
     * @param prefix the prefix the properties defined inside the map
     * will finally receive - may be null.
     * @param prefixValues - whether the prefix will be applied
     * to properties on the value side of the map as well.
     */
    public void resolveAllProperties(Map map, String prefix,
                                     boolean prefixValues) {
        // The map, prefix and prefixValues flag get used in the
        // getProperty callback
        this.map = map;
        this.prefix = prefix;
        this.prefixValues = prefixValues;

        for (Iterator i = map.keySet().iterator(); i.hasNext();) {
            expandingLHS = true;
            String key = (String) i.next();

            // if the property has already been set to the name it
            // will have in the end, then return the existing value to
            // ensure properties remain immutable
            String fullKey = key;
            if (prefix != null) {
                fullKey = prefix + key;
            }
            Object result = master.getProperty(fullKey);

            if (result == null) {
                result = getProperty(key);
            }
            String value = result == null ? "" : result.toString();
            map.put(key, value);
        }
    }
}
