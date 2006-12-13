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
package org.apache.tools.ant.taskdefs.optional.depend;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Stack;
import java.util.Vector;

/**
 * An iterator which iterates through the contents of a java directory. The
 * iterator should be created with the directory at the root of the Java
 * namespace.
 *
 */
public class DirectoryIterator implements ClassFileIterator {

    /**
     * This is a stack of current iterators supporting the depth first
     * traversal of the directory tree.
     */
    private Stack enumStack;

    /**
     * The current directory iterator. As directories encounter lower level
     * directories, the current iterator is pushed onto the iterator stack
     * and a new iterator over the sub directory becomes the current
     * directory. This implements a depth first traversal of the directory
     * namespace.
     */
    private Enumeration currentEnum;

    /**
     * Creates a directory iterator. The directory iterator is created to
     * scan the root directory. If the changeInto flag is given, then the
     * entries returned will be relative to this directory and not the
     * current directory.
     *
     * @param rootDirectory the root if the directory namespace which is to
     *      be iterated over
     * @param changeInto if true then the returned entries will be relative
     *      to the rootDirectory and not the current directory.
     * @exception IOException if there is a problem reading the directory
     *      information.
     */
    public DirectoryIterator(File rootDirectory, boolean changeInto)
         throws IOException {
        super();

        enumStack = new Stack();

        Vector filesInRoot = getDirectoryEntries(rootDirectory);

        currentEnum = filesInRoot.elements();
    }

    /**
     * Get a vector covering all the entries (files and subdirectories in a
     * directory).
     *
     * @param directory the directory to be scanned.
     * @return a vector containing File objects for each entry in the
     *      directory.
     */
    private Vector getDirectoryEntries(File directory) {
        Vector files = new Vector();

        // File[] filesInDir = directory.listFiles();
        String[] filesInDir = directory.list();

        if (filesInDir != null) {
            int length = filesInDir.length;

            for (int i = 0; i < length; ++i) {
                files.addElement(new File(directory, filesInDir[i]));
            }
        }

        return files;
    }

    /**
     * Template method to allow subclasses to supply elements for the
     * iteration. The directory iterator maintains a stack of iterators
     * covering each level in the directory hierarchy. The current iterator
     * covers the current directory being scanned. If the next entry in that
     * directory is a subdirectory, the current iterator is pushed onto the
     * stack and a new iterator is created for the subdirectory. If the
     * entry is a file, it is returned as the next element and the iterator
     * remains valid. If there are no more entries in the current directory,
     * the topmost iterator on the stack is popped off to become the
     * current iterator.
     *
     * @return the next ClassFile in the iteration.
     */
    public ClassFile getNextClassFile() {
        ClassFile nextElement = null;

        try {
            while (nextElement == null) {
                if (currentEnum.hasMoreElements()) {
                    File element = (File) currentEnum.nextElement();

                    if (element.isDirectory()) {

                        // push the current iterator onto the stack and then
                        // iterate through this directory.
                        enumStack.push(currentEnum);

                        Vector files = getDirectoryEntries(element);

                        currentEnum = files.elements();
                    } else {

                        // we have a file. create a stream for it
                        FileInputStream inFileStream
                            = new FileInputStream(element);

                        if (element.getName().endsWith(".class")) {

                            // create a data input stream from the jar
                            // input stream
                            ClassFile javaClass = new ClassFile();

                            javaClass.read(inFileStream);

                            nextElement = javaClass;
                        }
                    }
                } else {
                    // this iterator is exhausted. Can we pop one off the stack
                    if (enumStack.empty()) {
                        break;
                    } else {
                        currentEnum = (Enumeration) enumStack.pop();
                    }
                }
            }
        } catch (IOException e) {
            nextElement = null;
        }

        return nextElement;
    }

}

