/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.util.regexp;

import java.util.ArrayList;
import org.apache.myrmidon.api.TaskException;

/**
 * Interface describing a regular expression matcher.
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 * @author <a href="mailto:mattinger@mindless.com">Matthew Inger</a>
 */
public interface RegexpMatcher
{

    /**
     * Default Mask (case insensitive, neither multiline nor singleline
     * specified).
     */
    int MATCH_DEFAULT = 0x00000000;

    /**
     * Perform a case insenstive match
     */
    int MATCH_CASE_INSENSITIVE = 0x00000100;

    /**
     * Treat the input as a multiline input
     */
    int MATCH_MULTILINE = 0x00001000;

    /**
     * Treat the input as singleline input ('.' matches newline)
     */
    int MATCH_SINGLELINE = 0x00010000;

    /**
     * Set the regexp pattern from the String description.
     *
     * @param pattern The new Pattern value
     * @exception TaskException Description of Exception
     */
    void setPattern( String pattern )
        throws TaskException;

    /**
     * Get a String representation of the regexp pattern
     *
     * @return The Pattern value
     * @exception TaskException Description of Exception
     */
    String getPattern()
        throws TaskException;

    /**
     * Does the given argument match the pattern?
     *
     * @param argument Description of Parameter
     * @return Description of the Returned Value
     * @exception TaskException Description of Exception
     */
    boolean matches( String argument )
        throws TaskException;

    /**
     * Returns a ArrayList of matched groups found in the argument. <p>
     *
     * Group 0 will be the full match, the rest are the parenthesized
     * subexpressions</p> .
     *
     * @param argument Description of Parameter
     * @return The Groups value
     * @exception TaskException Description of Exception
     */
    ArrayList getGroups( String argument )
        throws TaskException;

    /**
     * Does this regular expression match the input, given certain options
     *
     * @param input The string to check for a match
     * @param options The list of options for the match. See the MATCH_
     *      constants above.
     * @return Description of the Returned Value
     * @exception TaskException Description of Exception
     */
    boolean matches( String input, int options )
        throws TaskException;

    /**
     * Get the match groups from this regular expression. The return type of the
     * elements is always String.
     *
     * @param input The string to check for a match
     * @param options The list of options for the match. See the MATCH_
     *      constants above.
     * @return The Groups value
     * @exception TaskException Description of Exception
     */
    ArrayList getGroups( String input, int options )
        throws TaskException;

}
