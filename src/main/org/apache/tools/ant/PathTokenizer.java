/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999 The Apache Software Foundation.  All rights
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
 * 4. The names "The Jakarta Project", "Tomcat", and "Apache Software
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

import java.util.*;
import java.io.*;

/**
 * A Path tokenizer takes a path and returns the components that make up
 * that path.
 *
 * The path can use path separators of either ':' or ';' and file separators
 * of either '/' or '\'
 *
 * @author Conor MacNeill (conor@ieee.org)
 *
 */ 
public class PathTokenizer {
    /**
     * A tokenizer to break the string up based on the ':' or ';' separators.
     */
    private StringTokenizer tokenizer;
    
    /**
     * A String which stores any path components which have been read ahead.
     */
    private String lookahead = null;

    /**
     * Flag to indicate whether we are running on a platform with a DOS style
     * filesystem
     */
    private boolean dosStyleFilesystem;

    public PathTokenizer(String path) {
       tokenizer = new StringTokenizer(path, ":;", false);
       dosStyleFilesystem = File.pathSeparatorChar == ';'; 
    }

    public boolean hasMoreTokens() {
        if (lookahead != null) {
            return true;
        }
        
        return tokenizer.hasMoreTokens();
    }
    
    public String nextToken() throws NoSuchElementException {
        String token = null;
        if (lookahead != null) {
            token = lookahead;
            lookahead = null;
        }
        else {
            token = tokenizer.nextToken().trim();
        }            
            
        if (token.length() == 1 && Character.isLetter(token.charAt(0))
                                && dosStyleFilesystem
                                && tokenizer.hasMoreTokens()) {
            // we are on a dos style system so this path could be a drive
            // spec. We look at the next token
            String nextToken = tokenizer.nextToken().trim();
            if (nextToken.startsWith("\\") || nextToken.startsWith("/")) {
                // we know we are on a DOS style platform and the next path starts with a
                // slash or backslash, so we know this is a drive spec
                token += ":" + nextToken;
            }
            else {
                // store the token just read for next time
                lookahead = nextToken;
            }
        }
           
        return token;
    }
}
          