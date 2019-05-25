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
package org.apache.tools.ant.filters;

import java.io.IOException;
import java.io.Reader;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.PropertyHelper;
import org.apache.tools.ant.property.GetProperty;
import org.apache.tools.ant.property.ParseProperties;
import org.apache.tools.ant.types.PropertySet;

/**
 * Expands Ant properties, if any, in the data.
 * <p>Example:</p>
 * <pre>&lt;expandproperties/&gt;</pre>
 * Or:
 * <pre>&lt;filterreader
 *    classname=&quot;org.apache.tools.ant.filters.ExpandProperties&quot;/&gt;</pre>
 *
 */
public final class ExpandProperties
    extends BaseFilterReader
    implements ChainableReader {

    private static final int EOF = -1;

    private char[] buffer;
    private int index;
    private PropertySet propertySet;

    /**
     * Constructor for "dummy" instances.
     *
     * @see BaseFilterReader#BaseFilterReader()
     */
    public ExpandProperties() {
        super();
    }

    /**
     * Creates a new filtered reader.
     *
     * @param in A Reader object providing the underlying stream.
     *           Must not be <code>null</code>.
     */
    public ExpandProperties(final Reader in) {
        super(in);
    }

    /**
     * Restrict the expanded properties using a PropertySet.
     * @param propertySet replacement lookup
     */
    public void add(PropertySet propertySet) {
        if (this.propertySet != null) {
            throw new BuildException("expandproperties filter accepts only one propertyset");
        }
        this.propertySet = propertySet;
    }

    /**
     * Returns the next character in the filtered stream. The original
     * stream is first read in fully, and the Ant properties are expanded.
     * The results of this expansion are then queued so they can be read
     * character-by-character.
     *
     * @return the next character in the resulting stream, or -1
     * if the end of the resulting stream has been reached
     *
     * @exception IOException if the underlying stream throws an IOException
     * during reading
     */
    public int read() throws IOException {
        if (index > EOF) {
            if (buffer == null) {
                String data = readFully();
                Project project = getProject();
                GetProperty getProperty;
                if (propertySet == null) {
                    getProperty = PropertyHelper.getPropertyHelper(project);
                } else {
                    getProperty = propertySet.getProperties()::getProperty;
                }
                Object expanded = new ParseProperties(project, PropertyHelper
                                                      .getPropertyHelper(project)
                                                      .getExpanders(),
                                                      getProperty)
                    .parseProperties(data);
                buffer = expanded == null ? new char[0]
                    : expanded.toString().toCharArray();
            }
            if (index < buffer.length) {
                return buffer[index++];
            }
            index = EOF;
        }
        return EOF;
    }

    /**
     * Creates a new ExpandProperties filter using the passed in
     * Reader for instantiation.
     *
     * @param rdr A Reader object providing the underlying stream.
     *            Must not be <code>null</code>.
     *
     * @return a new filter based on this configuration, but filtering
     *         the specified reader
     */
    public Reader chain(final Reader rdr) {
        ExpandProperties newFilter = new ExpandProperties(rdr);
        newFilter.setProject(getProject());
        newFilter.add(propertySet);
        return newFilter;
    }
}
