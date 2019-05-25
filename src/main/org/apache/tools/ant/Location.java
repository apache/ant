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

package org.apache.tools.ant;

import java.io.Serializable;
import java.util.Objects;

import org.apache.tools.ant.util.FileUtils;
import org.xml.sax.Locator;

/**
 * Stores the location of a piece of text within a file (file name,
 * line number and column number). Note that the column number is
 * currently ignored.
 *
 */
public class Location implements Serializable {
    private static final long serialVersionUID = 1L;

    /** Name of the file. */
    private final String fileName;
    /** Line number within the file. */
    private final int lineNumber;
    /** Column number within the file. */
    private final int columnNumber;

    /** Location to use when one is needed but no information is available */
    public static final Location UNKNOWN_LOCATION = new Location();

    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();

    /**
     * Creates an "unknown" location.
     */
    private Location() {
        this(null, 0, 0);
    }

    /**
     * Creates a location consisting of a file name but no line number or
     * column number.
     *
     * @param fileName The name of the file. May be <code>null</code>,
     *                 in which case the location is equivalent to
     *                 {@link #UNKNOWN_LOCATION UNKNOWN_LOCATION}.
     */
    public Location(String fileName) {
        this(fileName, 0, 0);
    }

    /**
     * Creates a location from the SAX locator using the system ID as
     * the filename.
     *
     * @param loc Must not be <code>null</code>.
     *
     * @since Ant 1.6
     */
    public Location(Locator loc) {
        this(loc.getSystemId(), loc.getLineNumber(), loc.getColumnNumber());
    }

    /**
     * Creates a location consisting of a file name, line number and
     * column number.
     *
     * @param fileName The name of the file. May be <code>null</code>,
     *                 in which case the location is equivalent to
     *                 {@link #UNKNOWN_LOCATION UNKNOWN_LOCATION}.
     *
     * @param lineNumber Line number within the file. Use 0 for unknown
     *                   positions within a file.
     * @param columnNumber Column number within the line.
     */
    public Location(String fileName, int lineNumber, int columnNumber) {
        if (fileName != null && fileName.startsWith("file:")) {
            this.fileName = FILE_UTILS.fromURI(fileName);
        } else {
            this.fileName = fileName;
        }
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
    }

    /**
     * @return the filename portion of the location
     * @since Ant 1.6
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * @return the line number
     * @since Ant 1.6
     */
    public int getLineNumber() {
        return lineNumber;
    }

    /**
     * @return the column number
     * @since Ant 1.7
     */
    public int getColumnNumber() {
        return columnNumber;
    }

    /**
     * Returns the file name, line number, a colon and a trailing space.
     * An error message can be appended easily. For unknown locations, an
     * empty string is returned.
     *
     * @return a String of the form <code>"fileName:lineNumber: "</code>
     *         if both file name and line number are known,
     *         <code>"fileName: "</code> if only the file name is known,
     *         and the empty string for unknown locations.
     */
    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();

        if (fileName != null) {
            buf.append(fileName);

            if (lineNumber != 0) {
                buf.append(":");
                buf.append(lineNumber);
            }

            buf.append(": ");
        }

        return buf.toString();
    }

    /**
     * Equality operation.
     * @param other the object to compare to.
     * @return true if the other object contains the same information
     *              as this object.
     * @since Ant 1.6.3
     */
    @Override
    public boolean equals(Object other) {
        return this == other || other != null && other.getClass() == getClass()
                && toString().equals(other.toString());
    }

    /**
     * Hash operation.
     * @return a hash code value for this location.
     * @since Ant 1.6.3
     */
    @Override
    public int hashCode() {
        return Objects.hash(fileName, lineNumber);
    }
}
