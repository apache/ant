/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant;

import java.io.File;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

/**
 * A Path tokenizer takes a path and returns the components that make up that
 * path. The path can use path separators of either ':' or ';' and file
 * separators of either '/' or '\'
 *
 * @author Conor MacNeill (conor@ieee.org)
 */
public class PathTokenizer
{
    /**
     * A String which stores any path components which have been read ahead.
     */
    private String lookahead;

    /**
     * Flag to indicate whether we are running on a platform with a DOS style
     * filesystem
     */
    private boolean dosStyleFilesystem;
    /**
     * A tokenizer to break the string up based on the ':' or ';' separators.
     */
    private StringTokenizer tokenizer;

    public PathTokenizer( String path )
    {
        tokenizer = new StringTokenizer( path, ":;", false );
        dosStyleFilesystem = File.pathSeparatorChar == ';';
    }

    public boolean hasMoreTokens()
    {
        if( lookahead != null )
        {
            return true;
        }

        return tokenizer.hasMoreTokens();
    }

    public String nextToken()
        throws NoSuchElementException
    {
        String token = null;
        if( lookahead != null )
        {
            token = lookahead;
            lookahead = null;
        }
        else
        {
            token = tokenizer.nextToken().trim();
        }

        if( token.length() == 1 && Character.isLetter( token.charAt( 0 ) )
            && dosStyleFilesystem
            && tokenizer.hasMoreTokens() )
        {
            // we are on a dos style system so this path could be a drive
            // spec. We look at the next token
            String nextToken = tokenizer.nextToken().trim();
            if( nextToken.startsWith( "\\" ) || nextToken.startsWith( "/" ) )
            {
                // we know we are on a DOS style platform and the next path starts with a
                // slash or backslash, so we know this is a drive spec
                token += ":" + nextToken;
            }
            else
            {
                // store the token just read for next time
                lookahead = nextToken;
            }
        }

        return token;
    }
}
