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
package org.apache.tools.ant.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

import org.apache.tools.ant.taskdefs.condition.Os;

/**
 * Contains methods related to Windows NTFS junctions.
 *
 * @since Ant 1.10.16
 */
public class NtfsJunctionUtils {

    private static final boolean ON_WINDOWS = Os.isFamily("windows");

    /**
     * Shared instance.
     */
    private static final NtfsJunctionUtils PRIMARY_INSTANCE = new NtfsJunctionUtils();

    /**
     * Method to retrieve The NtfsJunctionUtils, which is shared by
     * all users of this method.
     * @return an instance of NtfsJunctionUtils.
     */
    public static NtfsJunctionUtils getNtfsJunctionUtils() {
        return PRIMARY_INSTANCE;
    }

    /**
     * Empty constructor.
     */
    protected NtfsJunctionUtils() {
    }

    /**
     * Checks whether a given file is a directory junction.
     *
     * @return true if the file is a directory junction.
     * @throws IOException on error.
     */
    public boolean isDirectoryJunction(final File file) throws IOException {
        return isDirectoryJunction(file.toPath());
    }

    /**
     * Checks whether a given file is a directory junction.
     *
     * @return false if the given file is not a directory junction or
     * an exception occured while trying to check the file - most
     * likely because the file didn't exists.
     */
    public boolean isDirectoryJunctionSafe(final File file) {
        return isDirectoryJunctionSafe(file.toPath());
    }

    /**
     * Checks whether a given path is a directory junction.
     *
     * @return false if the given path is not a directory junction or
     * an exception occured while trying to check the path - most
     * likely because the path didn't exists.
     */
    public boolean isDirectoryJunctionSafe(final Path path) {
        try {
            return isDirectoryJunction(path);
        } catch (FileNotFoundException ex) {
            // ignore
        } catch (IOException ex) {
            System.err.println("Caught IOException " + ex.getMessage() + " while testing for junction.");
        }
        return false;
    }

    /**
     * Checks whether a given path is a directory junction.
     *
     * @return true if the path is a directory junction.
     * @throws IOException on error.
     */
    public boolean isDirectoryJunction(final Path path) throws IOException {
        if (!ON_WINDOWS) {
            return false;
        }
        BasicFileAttributes attrs =
            Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        return attrs.isDirectory() && attrs.isOther();
    }
}
