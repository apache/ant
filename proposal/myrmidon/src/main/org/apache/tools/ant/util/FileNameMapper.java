/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.util;

/**
 * Interface to be used by SourceFileScanner. <p>
 *
 * Used to find the name of the target file(s) corresponding to a source file.
 * </p> <p>
 *
 * The rule by which the file names are transformed is specified via the setFrom
 * and setTo methods. The exact meaning of these is implementation dependent.
 * </p>
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 */
public interface FileNameMapper
{

    /**
     * Sets the from part of the transformation rule.
     *
     * @param from The new From value
     */
    void setFrom( String from );

    /**
     * Sets the to part of the transformation rule.
     *
     * @param to The new To value
     */
    void setTo( String to );

    /**
     * Returns an array containing the target filename(s) for the given source
     * file. <p>
     *
     * if the given rule doesn't apply to the source file, implementation must
     * return null. SourceFileScanner will then omit the source file in
     * question.</p>
     *
     * @param sourceFileName the name of the source file relative to some given
     *      basedirectory.
     * @return Description of the Returned Value
     */
    String[] mapFileName( String sourceFileName );
}
