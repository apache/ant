/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000,2002 The Apache Software Foundation.  All rights
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
 * 4. The names "Ant" and "Apache Software
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

package org.apache.tools.ant;

/**
 * Stores the location of a piece of text within a file (file name,
 * line number and column number). Note that the column number is
 * currently ignored.
 */
public class Location {
    
    /** Name of the file. */
    private String fileName;
    /** Line number within the file. */
    private int lineNumber;
    /** Column number within the file. */
    private int columnNumber;

    /** Location to use when one is needed but no information is available */
    public static final Location UNKNOWN_LOCATION = new Location();

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
        this.fileName = fileName;
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
    }

    /**
     * Returns the file name, line number, a colon and a trailing space. 
     * An error message can be appended easily. For unknown locations, an 
     * empty string is returned.
     * 
     * @return a String of the form <code>"fileName: lineNumber: "</code>
     *         if both file name and line number are known,
     *         <code>"fileName: "</code> if only the file name is known,
     *         and the empty string for unknown locations.
     */
    public String toString() {
        StringBuffer buf = new StringBuffer();

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
}
