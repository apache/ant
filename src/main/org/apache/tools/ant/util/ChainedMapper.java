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

package org.apache.tools.ant.util;

import java.util.stream.Stream;

/**
 * A <code>ContainerMapper</code> that chains the results of the first
 * nested <code>FileNameMapper</code>s into sourcefiles for the second,
 * the second to the third, and so on, returning the resulting mapped
 * filenames from the last nested <code>FileNameMapper</code>.
 */
public class ChainedMapper extends ContainerMapper {

    /** {@inheritDoc}. */
    @Override
    public String[] mapFileName(String sourceFileName) {
        String[] result = getMappers().stream()
            .reduce(new String[] { sourceFileName }, (i, m) -> Stream.of(i)
                .map(m::mapFileName).flatMap(Stream::of).toArray(String[]::new),
                (i, o) -> o);
        return result == null || result.length == 0 ? null : result;
    }
}

