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
 *  Class to resolve properties in a map.
 */
public class ResolvePropertyMap implements GetProperty {
    private final Set       seen = new HashSet();
    private final ParseProperties parseProperties;
    private final GetProperty    master;
    private Map map;

    /**
     * Constructor with a master getproperty and a collection of expanders.
     * @param project the current ant project.
     * @param master the master property holder (usually PropertyHelper)
     * @param expanders a collection of expanders (usually from PropertyHelper).
     */
    public ResolvePropertyMap(
        Project project, GetProperty master, Collection expanders) {
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
        // Note: the master overrides (even if the name is subsequently
        //       prefixed)
        Object masterProperty = master.getProperty(name);
        if (masterProperty != null) {
            return masterProperty;
        }
        try {
            seen.add(name);
            return parseProperties.parseProperties((String) map.get(name));
        } finally {
            seen.remove(name);
        }
    }

    /**
     * The action method - resolves all the properties in a map.
     * @param map the map to resolve properties in.
     */
    public void resolveAllProperties(Map map) {
        this.map = map; // The map gets used in the getProperty callback
        for (Iterator i = map.keySet().iterator(); i.hasNext();) {
            String key = (String) i.next();
            Object result = getProperty(key);
            String value = result == null ? "" : result.toString();
            map.put(key, value);
        }
    }
}
