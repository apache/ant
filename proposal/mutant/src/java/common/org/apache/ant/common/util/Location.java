/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "The Jakarta Project", "Ant", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */
package org.apache.ant.common.util;

/**
 * Stores the file name and line number in a file.
 *
 * @author <a href="mailto:conor@apache.org">Conor MacNeill</a>
 * @created 16 January 2002
 */
public class Location {

    /** Standard unknown location constant; */
    public final static Location UNKNOWN_LOCATION = new Location();
    /** The source URL to which this location relates. */
    private String source;

    /** The line number of this location within the source */
    private int lineNumber;

    /** The column number of this location within the source */
    private int columnNumber;

    /**
     * Creates a location consisting of a source location but no line
     * number.
     *
     * @param source the source (URL) to which this location is associated.
     */
    public Location(String source) {
        this(source, 1, 1);
    }

    /**
     * Creates a location consisting of a source location and co-ordinates
     * within that source
     *
     * @param source the source (URL) to which this location is associated.
     * @param lineNumber the line number of this location
     * @param columnNumber the column number of this location
     */
    public Location(String source, int lineNumber, int columnNumber) {
        this.source = source;
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
    }

    /** Creates an "unknown" location. */
    private Location() {
        this(null, 0, 0);
    }

    /**
     * Get the source URL for this location
     *
     * @return a URL string
     */
    public String getSourceURL() {
        return source;
    }

    /**
     * Get the line number of this location
     *
     * @return an integer line number
     */
    public int getLineNumber() {
        return lineNumber;
    }

    /**
     * Get the column number of this location
     *
     * @return an integer column number
     */
    public int getColumnNumber() {
        return columnNumber;
    }

    /**
     * Returns the source name, line number and a trailing space. An error
     * message can be appended easily. For unknown locations, returns an
     * empty string.
     *
     * @return a suitable string representation of the location
     */
    public String toString() {
        StringBuffer buf = new StringBuffer();

        if (source != null) {
            if (source.startsWith("file:")) {
                buf.append(source.substring(5));
            } else {
                buf.append(source);
            }

            if (lineNumber != 0) {
                buf.append(":");
                buf.append(lineNumber);
            }

            buf.append(": ");
        }

        return buf.toString();
    }
}

