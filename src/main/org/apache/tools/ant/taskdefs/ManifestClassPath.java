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
package org.apache.tools.ant.taskdefs;

import java.io.File;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.launch.Locator;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.util.FileUtils;

/**
 * Converts a Path into a property suitable as a Manifest classpath.
 *
 * @since Ant 1.7
 *
 * @ant.task category="property"
 */
public class ManifestClassPath extends Task {

    /** The property name to hold the classpath value. */
    private String name;

    /** The directory the classpath will be relative from. */
    private File dir;

    /** The maximum parent directory level to traverse. */
    private int maxParentLevels = 2;

    /** The classpath to convert. */
    private Path path;

    /**
     * Sets a property, which must not already exist, with a space
     * separated list of files and directories relative to the jar
     * file's parent directory.
     */
    @Override
    public void execute() {
        if (name == null) {
            throw new BuildException("Missing 'property' attribute!");
        }
        if (dir == null) {
            throw new BuildException("Missing 'jarfile' attribute!");
        }
        if (getProject().getProperty(name) != null) {
            throw new BuildException("Property '%s' already set!", name);
        }
        if (path == null) {
            throw new BuildException("Missing nested <classpath>!");
        }

        StringBuilder tooLongSb = new StringBuilder();
        for (int i = 0; i < maxParentLevels + 1; i++) {
            tooLongSb.append("../");
        }
        final String tooLongPrefix = tooLongSb.toString();

        // Normalize the reference directory (containing the jar)
        final FileUtils fileUtils = FileUtils.getFileUtils();
        dir = fileUtils.normalize(dir.getAbsolutePath());

        StringBuilder buffer = new StringBuilder();
        for (String element : path.list()) {
            // Normalize the current file
            File pathEntry = new File(element);
            String fullPath = pathEntry.getAbsolutePath();
            pathEntry = fileUtils.normalize(fullPath);

            String relPath = null;
            String canonicalPath = null;
            try {
                if (dir.equals(pathEntry)) {
                    relPath = ".";
                } else {
                    relPath = FileUtils.getRelativePath(dir, pathEntry);
                }

                canonicalPath = pathEntry.getCanonicalPath();
                // getRelativePath always uses '/' as separator, adapt
                if (File.separatorChar != '/') {
                    canonicalPath =
                        canonicalPath.replace(File.separatorChar, '/');
                }
            } catch (Exception e) {
                throw new BuildException("error trying to get the relative path"
                                         + " from " + dir + " to " + fullPath,
                                         e);
            }

            // No match, so bail out!
            if (relPath.equals(canonicalPath)
                || relPath.startsWith(tooLongPrefix)) {
                throw new BuildException(
                    "No suitable relative path from %s to %s", dir, fullPath);
            }

            if (pathEntry.isDirectory() && !relPath.endsWith("/")) {
                relPath += '/';
            }
            relPath = Locator.encodeURI(relPath);

            // Manifest's ClassPath: attribute always uses forward
            // slashes '/', and is space-separated. Ant will properly
            // format it on 72 columns with proper line continuation
            buffer.append(relPath);
            buffer.append(' ');
        }

        // Finally assign the property with the manifest classpath
        getProject().setNewProperty(name, buffer.toString().trim());
    }

    /**
     * Sets the property name to hold the classpath value.
     *
     * @param  name the property name
     */
    public void setProperty(String name) {
        this.name = name;
    }

    /**
     * The JAR file to contain the classpath attribute in its manifest.
     *
     * @param  jarfile the JAR file. Need not exist yet, but its parent
     *         directory must exist on the other hand.
     */
    public void setJarFile(File jarfile) {
        File parent = jarfile.getParentFile();
        if (!parent.isDirectory()) {
            throw new BuildException("Jar's directory not found: %s", parent);
        }
        this.dir = parent;
    }

    /**
     * Sets the maximum parent directory levels allowed when computing
     * a relative path.
     *
     * @param  levels the max level. Defaults to 2.
     */
    public void setMaxParentLevels(int levels) {
        if (levels < 0) {
            throw new BuildException(
                "maxParentLevels must not be a negative number");
        }
        this.maxParentLevels = levels;
    }

    /**
     * Adds the classpath to convert.
     *
     * @param  path the classpath to convert.
     */
    public void addClassPath(Path path) {
        this.path = path;
    }

}
