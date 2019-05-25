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
package org.apache.tools.ant.taskdefs.optional.depend;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Iterator interface for iterating over a set of class files
 *
 */
public interface ClassFileIterator extends Iterable<ClassFile> {

    /**
     * Get the next class file in the iteration
     *
     * @return the next class file in the iteration
     */
    ClassFile getNextClassFile();

    @Override
    default Iterator<ClassFile> iterator() {

        return new Iterator<ClassFile>() {
            ClassFile next;
            {
                next = getNextClassFile();
            }

            @Override
            public boolean hasNext() {
                return next != null;
            }

            @Override
            public ClassFile next() {
                if (next == null) {
                    throw new NoSuchElementException();
                }
                try {
                    return next;
                } finally {
                    next = getNextClassFile();
                }
            }

        };
    }
}
