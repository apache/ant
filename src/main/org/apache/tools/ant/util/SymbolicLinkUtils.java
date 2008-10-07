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

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

/**
 * Contains methods related to symbolic links - or what Ant thinks is
 * a symbolic link based on the absent support for them in Java.
 *
 * @since Ant 1.8.0
 */
public class SymbolicLinkUtils {
    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();

    /**
     * Shared instance.
     */
    private static final SymbolicLinkUtils PRIMARY_INSTANCE =
        new SymbolicLinkUtils();

    /**
     * Method to retrieve The SymbolicLinkUtils, which is shared by
     * all users of this method.
     * @return an instance of SymbolicLinkUtils.
     */
    public static SymbolicLinkUtils getSymbolicLinkUtils() {
        // keep the door open for Java X.Y specific subclass if symbolic
        // links ever become supported in the classlib
        return PRIMARY_INSTANCE;
    }

    /**
     * Empty constructor.
     */
    protected SymbolicLinkUtils() {
    }

    /**
     * Checks whether a given file is a symbolic link.
     *
     * <p>It doesn't really test for symbolic links but whether the
     * canonical and absolute paths of the file are identical--this
     * may lead to false positives on some platforms.</p>
     *
     * @param file the file to test.
     *
     * @return true if the file is a symbolic link.
     * @throws IOException on error.
     */
    public boolean isSymbolicLink(File file) throws IOException {
        return isSymbolicLink(file.getParentFile(), file.getName());
    }

    /**
     * Checks whether a given file is a symbolic link.
     *
     * <p>It doesn't really test for symbolic links but whether the
     * canonical and absolute paths of the file are identical--this
     * may lead to false positives on some platforms.</p>
     *
     * @param name the name of the file to test.
     *
     * @return true if the file is a symbolic link.
     * @throws IOException on error.
     */
    public boolean isSymbolicLink(String name) throws IOException {
        return isSymbolicLink(new File(name));
    }

    /**
     * Checks whether a given file is a symbolic link.
     *
     * <p>It doesn't really test for symbolic links but whether the
     * canonical and absolute paths of the file are identical--this
     * may lead to false positives on some platforms.</p>
     *
     * @param parent the parent directory of the file to test
     * @param name the name of the file to test.
     *
     * @return true if the file is a symbolic link.
     * @throws IOException on error.
     */
    public boolean isSymbolicLink(File parent, String name)
        throws IOException {
        File toTest = new File(parent.getCanonicalPath(), name);
        return !toTest.getAbsolutePath().equals(toTest.getCanonicalPath());
    }

    /**
     * Checks whether a given file is a broken symbolic link.
     *
     * <p>It doesn't really test for symbolic links but whether Java
     * reports that the File doesn't exist but its parent's child list
     * contains it--this may lead to false positives on some
     * platforms.</p>
     *
     * <p>Note that #isSymbolicLink returns false if this method
     * returns true since Java won't produce a canonical name
     * different from the abolute one if the link is broken.</p>
     *
     * @param name the name of the file to test.
     *
     * @return true if the file is a broken symbolic link.
     * @throws IOException on error.
     */
    public boolean isDanglingSymbolicLink(String name) throws IOException {
        return isDanglingSymbolicLink(new File(name));
    }

    /**
     * Checks whether a given file is a broken symbolic link.
     *
     * <p>It doesn't really test for symbolic links but whether Java
     * reports that the File doesn't exist but its parent's child list
     * contains it--this may lead to false positives on some
     * platforms.</p>
     *
     * <p>Note that #isSymbolicLink returns false if this method
     * returns true since Java won't produce a canonical name
     * different from the abolute one if the link is broken.</p>
     *
     * @param file the file to test.
     *
     * @return true if the file is a broken symbolic link.
     * @throws IOException on error.
     */
    public boolean isDanglingSymbolicLink(File file) throws IOException {
        return isDanglingSymbolicLink(file.getParentFile(), file.getName());
    }

    /**
     * Checks whether a given file is a broken symbolic link.
     *
     * <p>It doesn't really test for symbolic links but whether Java
     * reports that the File doesn't exist but its parent's child list
     * contains it--this may lead to false positives on some
     * platforms.</p>
     *
     * <p>Note that #isSymbolicLink returns false if this method
     * returns true since Java won't produce a canonical name
     * different from the abolute one if the link is broken.</p>
     *
     * @param parent the parent directory of the file to test
     * @param name the name of the file to test.
     *
     * @return true if the file is a broken symbolic link.
     * @throws IOException on error.
     */
    public boolean isDanglingSymbolicLink(File parent, String name) 
        throws IOException {
        File f = new File(parent, name);
        if (!f.exists()) {
            final String localName = f.getName();
            String[] c = parent.list(new FilenameFilter() {
                    public boolean accept(File d, String n) {
                        return localName.equals(n);
                    }
                });
            return c != null && c.length > 0;
        }
        return false;
    }

}