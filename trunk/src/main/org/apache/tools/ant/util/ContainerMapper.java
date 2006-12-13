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

import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Collections;
import org.apache.tools.ant.types.Mapper;

/**
 * A <code>FileNameMapper</code> that contains
 * other <code>FileNameMapper</code>s.
 * @see FileNameMapper
 */
public abstract class ContainerMapper implements FileNameMapper {

    private List mappers = new ArrayList();

    /**
     * Add a <code>Mapper</code>.
     * @param mapper the <code>Mapper</code> to add.
     */
    public void addConfiguredMapper(Mapper mapper) {
        add(mapper.getImplementation());
    }

    /**
     * An add configured version of the add method.
     * This class used to contain an add method and an
     * addConfiguredMapper method. Dur to ordering,
     * the add method was always called first. This
     * addConfigued method has been added to allow
     * chaining to work correctly.
     * @param fileNameMapper a <code>FileNameMapper</code>.
     */
    public void addConfigured(FileNameMapper fileNameMapper) {
        add(fileNameMapper);
    }

    /**
     * Add a <code>FileNameMapper</code>.
     * @param fileNameMapper a <code>FileNameMapper</code>.
     * @throws IllegalArgumentException if attempting to add this
     *         <code>ContainerMapper</code> to itself, or if the specified
     *         <code>FileNameMapper</code> is itself a <code>ContainerMapper</code>
     *         that contains this <code>ContainerMapper</code>.
     */
    public synchronized void add(FileNameMapper fileNameMapper) {
        if (this == fileNameMapper
            || (fileNameMapper instanceof ContainerMapper
            && ((ContainerMapper) fileNameMapper).contains(this))) {
            throw new IllegalArgumentException(
                "Circular mapper containment condition detected");
        } else {
            mappers.add(fileNameMapper);
        }
    }

    /**
     * Return <code>true</code> if this <code>ContainerMapper</code> or any of
     * its sub-elements contains the specified <code>FileNameMapper</code>.
     * @param fileNameMapper   the <code>FileNameMapper</code> to search for.
     * @return <code>boolean</code>.
     */
    protected synchronized boolean contains(FileNameMapper fileNameMapper) {
        boolean foundit = false;
        for (Iterator iter = mappers.iterator(); iter.hasNext() && !foundit;) {
            FileNameMapper next = (FileNameMapper) (iter.next());
            foundit |= (next == fileNameMapper
                || (next instanceof ContainerMapper
                && ((ContainerMapper) next).contains(fileNameMapper)));
        }
        return foundit;
    }

    /**
     * Get the <code>List</code> of <code>FileNameMapper</code>s.
     * @return <code>List</code>.
     */
    public synchronized List getMappers() {
        return Collections.unmodifiableList(mappers);
    }

    /**
     * Empty implementation.
     * @param ignore ignored.
     */
    public void setFrom(String ignore) {
        //Empty
    }

    /**
     * Empty implementation.
     * @param ignore ignored.
     */
    public void setTo(String ignore) {
        //Empty
    }

}

