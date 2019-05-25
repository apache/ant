/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.tools.ant.types;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * EnumeratedAttribute implementation for Charset to use with encoding/charset attributes.
 * @since Ant 1.10.6
 */
public class CharSet extends EnumeratedAttribute {

    private static final List<String> VALUES = new ArrayList<>();

    static {
        for (Map.Entry<String, Charset> entry :  Charset.availableCharsets().entrySet()) {
            VALUES.add(entry.getKey());
            VALUES.addAll(entry.getValue().aliases());
        }
    }

    /**
     * Default constructor.
     */
    public CharSet() {
    }

    /**
     * Construct a new CharSet with the specified value.
     * @param value the EnumeratedAttribute value.
     */
    public CharSet(String value) {
        setValue(value);
    }

    /**
     * Get the default value as provided by Charset.
     * @return the default value.
     */
    public static CharSet getDefault() {
        return new CharSet(Charset.defaultCharset().name());
    }

    /**
     * Convenience methood: get US-ASCII CharSet.
     * @return the default value.
     */
    public static CharSet getAscii() {
        return new CharSet(StandardCharsets.US_ASCII.name());
    }

    /**
     * Convenience method: get UTF-8 CharSet.
     * @return the default value.
     */
    public static CharSet getUtf8() {
        return new CharSet(StandardCharsets.UTF_8.name());
    }

    /**
     * Tell if CharSet values are aliases.
     * @param cs CharSet to compare the value to.
     * @return true if CharSet values are aliases.
     */
    public boolean equivalent(CharSet cs) {
        return getCharset().name().equals(cs.getCharset().name());
    }

    /**
     * Convert this enumerated type to a <code>Charset</code>.
     * @return a <code>Charset</code> object.
     */
    public Charset getCharset() {
        return Charset.forName(getValue());
    }

    /**
     * Return the possible values.
     * @return String[] of Charset names.
     */
    @Override
    public String[] getValues() {
        return VALUES.toArray(new String[0]);
    }

    /**
     * Accept additional values for backwards compatibility
     * (some java.io encoding names not available in java.nio)
     * @param value the <code>String</code> value of the attribute
     */
    @Override
    public final void setValue(final String value) {
        String realValue = value;
        if (value == null || value.isEmpty()) {
           realValue = Charset.defaultCharset().name();
       } else {
           for (String v : Arrays.asList(value, value.toLowerCase(), value.toUpperCase())) {
               if (VALUES.contains(v)) {
                   realValue = v;
                   break;
               }
           }
        }
        super.setValue(realValue);
    }
}
