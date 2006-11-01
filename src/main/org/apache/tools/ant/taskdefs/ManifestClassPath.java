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
package org.apache.tools.ant.taskdefs;

import java.io.File;
import java.io.UnsupportedEncodingException;

import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.launch.Locator;
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
    public void execute() {
        if (name == null) {
          throw new BuildException("Missing 'property' attribute!");
        }
        if (dir == null) {
          throw new BuildException("Missing 'jarfile' attribute!");
        }
        if (getProject().getProperty(name) != null) {
          throw new BuildException("Property '" + name + "' already set!");
        }
        if (path == null) {
            throw new BuildException("Missing nested <classpath>!");
        }

        // Normalize the reference directory (containing the jar)
        final FileUtils fileUtils = FileUtils.getFileUtils();
        dir = fileUtils.normalize(dir.getAbsolutePath());

        // Create as many directory prefixes as parent levels to traverse,
        // in addition to the reference directory itself
        File currDir = dir;
        String[] dirs = new String[maxParentLevels + 1];
        for (int i = 0; i < maxParentLevels + 1; ++i) {
            dirs[i] = currDir.getAbsolutePath() + File.separatorChar;
            currDir = currDir.getParentFile();
            if (currDir == null) {
                maxParentLevels = i + 1;
                break;
            }
        }

        String[] elements = path.list();
        StringBuffer buffer = new StringBuffer();
        StringBuffer element = new StringBuffer();
        for (int i = 0; i < elements.length; ++i) {
            // Normalize the current file
            File pathEntry = new File(elements[i]);
            pathEntry = fileUtils.normalize(pathEntry.getAbsolutePath());
            String fullPath = pathEntry.getAbsolutePath();

            // Find the longest prefix shared by the current file
            // and the reference directory.
            String relPath = null;
            for (int j = 0; j <= maxParentLevels; ++j) {
                String dir = dirs[j];
                if (!fullPath.startsWith(dir)) {
                    continue;
                }

                // We have a match! Add as many ../ as parent
                // directory traversed to get the relative path
                element.setLength(0);
                for (int k = 0; k < j; ++k) {
                    element.append("..");
                    element.append(File.separatorChar);
                }
                element.append(fullPath.substring(dir.length()));
                relPath = element.toString();
                break;
            }

            // No match, so bail out!
            if (relPath == null) {
                throw new BuildException(
                    "No suitable relative path from "
                    + dir + " to " + fullPath);
            }

            // Manifest's ClassPath: attribute always uses forward
            // slashes '/', and is space-separated. Ant will properly
            // format it on 72 columns with proper line continuation
            if (File.separatorChar != '/') {
                relPath = relPath.replace(File.separatorChar, '/');
            }
            if (pathEntry.isDirectory()) {
                relPath = relPath + '/';
            }
            try {
                relPath = Locator.encodeURI(relPath);
            } catch (UnsupportedEncodingException exc) {
                throw new BuildException(exc);
            }
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
            throw new BuildException("Jar's directory not found: " + parent);
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
