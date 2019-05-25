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
package org.apache.tools.ant.filters;

import java.io.Reader;

/**
 * Interface indicating that a reader may be chained to another one.
 *
 */
public interface ChainableReader {
    /**
     * Returns a reader with the same configuration as this one,
     * but filtering input from the specified reader.
     *
     * @param rdr the reader which the returned reader should be filtering
     *
     * @return a reader with the same configuration as this one, but
     *         filtering input from the specified reader
     */
    Reader chain(Reader rdr);
}
