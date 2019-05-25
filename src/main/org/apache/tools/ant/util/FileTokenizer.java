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
package org.apache.tools.ant.util;

import java.io.IOException;
import java.io.Reader;

import org.apache.tools.ant.ProjectComponent;

/**
 * Class to read the complete input into a string.
 * @since Ant 1.7
 */
public class FileTokenizer extends ProjectComponent implements Tokenizer {

    /**
     * Get the complete input as a string
     * @param in the reader object
     * @return the complete input
     * @throws IOException if error reading
     */
    @Override
    public String getToken(Reader in) throws IOException {
        return FileUtils.readFully(in);
    }

    /**
     * Return the intra-token string
     * @return an empty string always
     */
    @Override
    public String getPostToken() {
        return "";
    }
}
