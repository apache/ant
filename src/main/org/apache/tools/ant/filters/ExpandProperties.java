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
package org.apache.tools.ant.filters;

import java.io.IOException;
import java.io.Reader;
import org.apache.tools.ant.Project;

/**
 * Expands Ant properties, if any, in the data.
 * <p>
 * Example:<br>
 * <pre>&lt;expandproperties/&gt;</pre>
 * Or:
 * <pre>&lt;filterreader
 *    classname=&quot;org.apache.tools.ant.filters.ExpandProperties&quot;/&gt;</pre>
 *
 */
public final class ExpandProperties
    extends BaseFilterReader
    implements ChainableReader {
    /** Data that must be read from, if not null. */
    private String queuedData = null;

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

        int ch = -1;

        if (queuedData != null && queuedData.length() == 0) {
            queuedData = null;
        }

        if (queuedData != null) {
            ch = queuedData.charAt(0);
            queuedData = queuedData.substring(1);
            if (queuedData.length() == 0) {
                queuedData = null;
            }
        } else {
            queuedData = readFully();
            if (queuedData == null) {
                ch = -1;
            } else {
                Project project = getProject();
                queuedData = project.replaceProperties(queuedData);
                return read();
            }
        }
        return ch;
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
        return newFilter;
    }
}
