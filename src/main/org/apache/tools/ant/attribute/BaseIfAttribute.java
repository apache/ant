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

package org.apache.tools.ant.attribute;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.tools.ant.ProjectComponent;
import org.apache.tools.ant.RuntimeConfigurable;
import org.apache.tools.ant.UnknownElement;


/**
 * An abstract class for if/unless attributes.
 * This contains a boolean flag to specify whether this is an
 * if or unless attribute.
 * @since Ant 1.9.1
 */
public abstract class BaseIfAttribute
    extends ProjectComponent implements EnableAttribute {
    private boolean positive = true;
    /**
     * Set the positive flag.
     * @param positive the value to use.
     */
    protected void setPositive(boolean positive) {
        this.positive = positive;
    }

    /**
     * Get the positive flag.
     * @return the flag.
     */
    protected boolean isPositive() {
        return positive;
    }

    /**
     * convert the result.
     * @param val the result to convert
     * @return val if positive or !val if not.
     */
    protected boolean convertResult(boolean val) {
        return positive ? val : !val;
    }

    /**
     * Get all the attributes in the ant-attribute:param
     * namespace and place them in a map.
     * @param el the element this attribute is in.
     * @return a map of attributes.
     */
    protected Map getParams(UnknownElement el) {
        Map ret = new HashMap();
        RuntimeConfigurable rc = el.getWrapper();
        Map attributes = rc.getAttributeMap(); // This does a copy!
        for (Iterator i = attributes.entrySet().iterator(); i.hasNext();) {
            Map.Entry entry = (Map.Entry) i.next();
            String key = (String) entry.getKey();
            String value = (String) entry.getValue();
            if (key.startsWith("ant-attribute:param")) {
                int pos = key.lastIndexOf(':');
                ret.put(key.substring(pos + 1),
                        el.getProject().replaceProperties(value));
            }
        }
        return ret;
    }
}
