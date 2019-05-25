/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * https://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.apache.tools.ant.types;

import java.util.stream.Stream;

/**
 * Interface describing a collection of Resources.
 *
 * @since Ant 1.7
 */
public interface ResourceCollection extends Iterable<Resource> {

    /**
     * Learn the number of contained Resources.
     *
     * @return number of elements as int.
     */
    int size();

    /**
     * Indicate whether this ResourceCollection is composed entirely of
     * Resources accessible via local filesystem conventions. If true, all
     * resources returned from this collection should respond with a
     * {@link org.apache.tools.ant.types.resources.FileProvider} when asked via
     * {@link Resource#as}.
     *
     * @return whether this is a filesystem-only resource collection.
     */
    boolean isFilesystemOnly();

    /**
     * Return a {@link Stream} over this {@link ResourceCollection}.
     * @return {@link Stream} of {@link Resource}
     * @since Ant 1.10.2
     */
    default Stream<? extends Resource> stream() {
        final Stream.Builder<Resource> b = Stream.builder();
        forEach(b);
        return b.build();
    }

    /**
     * Learn whether this {@link ResourceCollection} is empty.
     * @return boolean
     */
    default boolean isEmpty() {
        return size() == 0;
    }
}
