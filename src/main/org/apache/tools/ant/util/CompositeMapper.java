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

import java.util.Iterator;
import java.util.LinkedHashSet;

/**
 * A <code>ContainerMapper</code> that unites the results of its constituent
 * <code>FileNameMapper</code>s into a single set of result filenames.
 */
public class CompositeMapper extends ContainerMapper {

    /** {@inheritDoc}. */
    public String[] mapFileName(String sourceFileName) {
        LinkedHashSet results = new LinkedHashSet();

        FileNameMapper mapper = null;
        for (Iterator mIter = getMappers().iterator(); mIter.hasNext();) {
            mapper = (FileNameMapper) (mIter.next());
            if (mapper != null) {
                String[] mapped = mapper.mapFileName(sourceFileName);
                if (mapped != null) {
                    for (int i = 0; i < mapped.length; i++) {
                        results.add(mapped[i]);
                    }
                }
            }
        }
        return (results.size() == 0) ? null
            : (String[]) results.toArray(new String[results.size()]);
    }

}

