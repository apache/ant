/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.todo.util;

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
class PathTokenizer
{
    /**
     * A String which stores any path components which have been read ahead.
     */
    private String m_lookahead;

    /**
     * Flag to indicate whether we are running on a platform with a DOS style
     * filesystem
     */
    private boolean m_dosStyleFilesystem;
    /**
     * A tokenizer to break the string up based on the ':' or ';' separators.
     */
    private StringTokenizer m_tokenizer;

    public PathTokenizer( String path )
    {
        m_tokenizer = new StringTokenizer( path, ":;", false );
        m_dosStyleFilesystem = File.pathSeparatorChar == ';';
    }

    public boolean hasNext()
    {
        if( m_lookahead != null )
        {
            return true;
        }

        return m_tokenizer.hasMoreTokens();
    }

    public String next()
        throws NoSuchElementException
    {
        String token = null;
        if( m_lookahead != null )
        {
            token = m_lookahead;
            m_lookahead = null;
        }
        else
        {
            token = m_tokenizer.nextToken().trim();
        }

        if( token.length() == 1 && Character.isLetter( token.charAt( 0 ) )
            && m_dosStyleFilesystem
            && m_tokenizer.hasMoreTokens() )
        {
            // we are on a dos style system so this path could be a drive
            // spec. We look at the next token
            String nextToken = m_tokenizer.nextToken().trim();
            if( nextToken.startsWith( "\\" ) || nextToken.startsWith( "/" ) )
            {
                // we know we are on a DOS style platform and the next path starts with a
                // slash or backslash, so we know this is a drive spec
                token += ":" + nextToken;
            }
            else
            {
                // store the token just read for next time
                m_lookahead = nextToken;
            }
        }

        return token;
    }
}
