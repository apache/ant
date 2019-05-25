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
package org.apache.tools.ant.property;

import java.text.ParsePosition;
import java.util.Collection;
import java.util.Objects;

import org.apache.tools.ant.Project;

/**
 * Parse properties using a collection of expanders.
 *
 * @since Ant 1.8.0
 */
public class ParseProperties implements ParseNextProperty {

    private final Project project;
    private final GetProperty getProperty;
    private final Collection<PropertyExpander> expanders;

    /**
     * Constructor with a getProperty.
     * @param project the current Ant project.
     * @param expanders a sequence of expanders
     * @param getProperty property resolver.
     */
    public ParseProperties(Project project, Collection<PropertyExpander> expanders, GetProperty getProperty) {
        this.project = project;
        this.expanders = expanders;
        this.getProperty = getProperty;
    }

    /**
     * Get the project.
     * @return the current Ant project.
     */
    @Override
    public Project getProject() {
        return project;
    }

    /**
     * Decode properties from a String representation.
     *
     * <ul>
     *
     *  <li>This implementation starts parsing the <code>value</code>
     *  parameter (unsurprisingly) at the beginning and asks each
     *  {@link PropertyExpander PropertyExpander} whether there is a
     *  property reference at that point.  PropertyExpanders return
     *  the name of a property they may find and may advance the parse
     *  position.</li>
     *
     *  <li>If the PropertyExpander returns <code>null</code> the
     *  method continues with the next PropertyExpander, otherwise it
     *  tries to look up the property's value using the configured
     *  {@link GetProperty GetProperty} instance.</li>
     *
     *  <li>Once all PropertyExpanders have been consulted, the parse
     *  position is advanced by one character and the process repeated
     *  until <code>value</code> is exhausted.</li>
     *
     * </ul>
     *
     * <p>If the entire contents of <code>value</code> resolves to a
     * single property, the looked up property value is returned.
     * Otherwise a String is returned that concatenates the
     * non-property parts of <code>value</code> and the expanded
     * values of the properties that have been found.</p>
     *
     * @param value The string to be scanned for property references.
     *              May be <code>null</code>, in which case this
     *              method returns immediately with no effect.
     *
     * @return the original string with the properties replaced, or
     *         <code>null</code> if the original string is <code>null</code>.
     */
    public Object parseProperties(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        final int len = value.length();
        ParsePosition pos = new ParsePosition(0);
        Object o = parseNextProperty(value, pos);
        if (o != null && pos.getIndex() >= len) {
            return o;
        }
        StringBuilder sb = new StringBuilder(len * 2);
        if (o == null) {
            sb.append(value.charAt(pos.getIndex()));
            pos.setIndex(pos.getIndex() + 1);
        } else {
            sb.append(o);
        }
        while (pos.getIndex() < len) {
            o = parseNextProperty(value, pos);
            if (o == null) {
                sb.append(value.charAt(pos.getIndex()));
                pos.setIndex(pos.getIndex() + 1);
            } else {
                sb.append(o);
            }
        }
        return sb.toString();
    }

    /**
     * Learn whether a String contains replaceable properties.
     *
     * <p>Uses the configured {@link PropertyExpander
     *  PropertyExpanders} and scans through the string.  Returns true
     *  as soon as any expander finds a property.</p>
     *
     * @param value the String to check.
     * @return <code>true</code> if <code>value</code> contains property notation.
     */
    public boolean containsProperties(String value) {
        if (value == null) {
            return false;
        }
        final int len = value.length();
        for (ParsePosition pos = new ParsePosition(0); pos.getIndex() < len;) {
            if (parsePropertyName(value, pos) != null) {
                return true;
            }
            pos.setIndex(pos.getIndex() + 1);
        }
        return false;
    }

    /**
     * Return any property that can be parsed from the specified position
     * in the specified String.
     *
     * <p>Uses the configured {@link PropertyExpander
     *  PropertyExpanders} and {@link GetProperty GetProperty}
     *  instance .</p>
     *
     * @param value String to parse
     * @param pos ParsePosition
     * @return Object or null if no property is at the current
     * location.  If a property reference has been found but the
     * property doesn't expand to a value, the property's name is
     * returned.
     */
    @Override
    public Object parseNextProperty(String value, ParsePosition pos) {
        final int start = pos.getIndex();

        if (start > value.length()) {
            // early exit, can't find any property here, no need to
            // consult all the delegates.
            return null;
        }

        String propertyName = parsePropertyName(value, pos);
        if (propertyName != null) {
            Object result = getProperty(propertyName);
            if (result != null) {
                return result;
            }
            if (project != null) {
                project.log(
                    "Property \"" + propertyName
                    + "\" has not been set", Project.MSG_VERBOSE);
            }
            return value.substring(start, pos.getIndex());
        }
        return null;
    }

    private String parsePropertyName(String value, ParsePosition pos) {
        return expanders.stream()
            .map(xp -> xp.parsePropertyName(value, pos, this))
            .filter(Objects::nonNull).findFirst().orElse(null);
    }

    private Object getProperty(String propertyName) {
        return getProperty.getProperty(propertyName);
    }
}
