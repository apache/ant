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
package org.apache.tools.ant.types.resources;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;

/**
 * ResourceCollection that contains the last <code>count</code> elements of
 * another ResourceCollection, a la the UNIX tail command.
 * @since Ant 1.7.1
 */
public class Last extends SizeLimitCollection {

    /**
     * Take the last <code>count</code> elements.
     * @return a Collection of Resources.
     */
    @Override
    protected Collection<Resource> getCollection() {
        int count = getValidCount();
        ResourceCollection rc = getResourceCollection();
        int size = rc.size();
        int skip = Math.max(0, size - count);

        List<Resource> result =
            rc.stream().skip(skip).collect(Collectors.toList());

        int found = result.size();
        if (found == count || (size < count && found == size)) {
            return result;
        }
        //mismatch:
        String msg = String.format(
            "Resource collection %s reports size %d but returns %d elements.",
            rc, size, found + skip);

        //size was understated -> too many results; warn and continue:
        if (found > count) {
            log(msg, Project.MSG_WARN);
            return result.subList(found - count, found);
        }
        //size was overstated; we missed some and are now in error-land:
        throw new BuildException(msg);
    }

}
