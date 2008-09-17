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

package org.apache.tools.ant.types.selectors;

/**
 * Container for a path that has been split into its components.
 * @since 1.8.0
 */
public class TokenizedPath {

    private final String path;
    private final String tokenizedPath[];

    /**
    * Initialize the TokenizedPath by parsing it. 
    * @param path The path to tokenize. Must not be
    *                <code>null</code>.
    */
    public TokenizedPath(String path) {
        this.path = path;    
        this.tokenizedPath = SelectorUtils.tokenizePathAsArray(path);
    }
    
    /**
     * @return The original path String
     */
    public String toString() {
        return path;
    }
    
    /**
     * The depth (or length) of a path.
     */
    public int depth() {
        return tokenizedPath.length;
    }

    /* package */ String[] getTokens() {
        return tokenizedPath;
    }
}
