/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2002 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "Ant" and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
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
 * @author Conor MacNeill
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
     * The length of the root directory. This is used to remove the root
     * directory from full paths.
     */
    private int rootLength;

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

        if (rootDirectory.isAbsolute() || changeInto) {
            rootLength = rootDirectory.getPath().length() + 1;
        } else {
            rootLength = 0;
        }

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
     * the topmost iterator on the statck is popped off to become the
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

