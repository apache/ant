/*
 * Copyright  2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
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

package org.apache.tools.ant.taskdefs.optional.repository;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Path;

import java.io.File;

/**
 * How we represent libraries
 *
 * @since Ant1.7
 */
public class Library {

    //project "ant"
    private String project;

    //version "1.5"
    private String version;

    //archive prefix "ant-optional"
    private String archive;

    /**
     * very optional attribute; name of the destination. Autocalculated if not
     * set.
     */

    private String destinationName;

    /**
     * file mapped to this one
     */
    private File libraryFile;

    /**
     * if clause
     */
    private String ifClause;

    /**
     * unless clause
     */
    private String unlessClause;

    public static final String ERROR_NO_ARCHIVE = "No archive defined";
    public static final String ERROR_NO_PROJECT = "No project defined";
    public static final String ERROR_NO_VERSION = "No version defined";
    public static final String ERROR_NO_SUFFIX = "No version defined";

    /**
     * suffix
     */
    private String suffix = "jar";


    /**
     * the project that provides this library
     * @return the project or null
     */
    public String getProject() {
        return project;
    }

    /**
     * the project that provides this library
     * @param project
     */
    public void setProject(String project) {
        this.project = project;
    }

    /**
     * Get the version string of this library
     * @return
     */
    public String getVersion() {
        return version;
    }

    /**
     * set the version string of this library
     * @param version
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * get the base name of this library
     * @return
     */
    public String getArchive() {
        return archive;
    }

    /**
     * set the base name of this library
     * @param archive
     */
    public void setArchive(String archive) {
        this.archive = archive;
    }

    /**
     * get the destination name attribute.
     * @return
     */
    public String getDestinationName() {
        return destinationName;
    }

    /**
     * set the name of the library when downloaded,
     * relative to the base directory
     * @param destinationName
     */
    public void setDestinationName(String destinationName) {
        this.destinationName = destinationName;
    }

    /**
      * get the suffix for this file.
     *
     * @return
     */
    public String getSuffix() {
        return suffix;
    }

    /**
     * set the suffix for this file; default is "jar"
     * @param suffix
     */
    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    /**
     * a property that must be set for the library to be considered a dependency
     * @return
     */
    public String getIf() {
        return ifClause;
    }

    /**
     * a property that must be set for the library to be considered a dependency
     * @param ifClause
     */
    public void setIf(String ifClause) {
        this.ifClause = ifClause;
    }

    /**
     * a property that must be unset for the library to be considered a dependency
     * @return
     */
    public String getUnless() {
        return unlessClause;
    }

    /**
     * a property that must be unset for the library to be considered a dependency
     * @param unlessClause
     */
    public void setUnless(String unlessClause) {
        this.unlessClause = unlessClause;
    }

    /**
     * get the library file
     * (only non-null after binding)
     * @return library file or null
     */
    public File getLibraryFile() {
        return libraryFile;
    }

    /**
     * fault if the field is null or empty
     *
     * @param field
     * @param message text for fault
     *
     * @throws BuildException if the field is not set up
     */
    private void faultIfEmpty(String field, String message) {
        if (field == null || field.length() == 0) {
            throw new BuildException(message);
        }
    }

    /**
     * validate;
     *
     * @throws BuildException if invalid
     */
    public void validate() {
        faultIfEmpty(archive, ERROR_NO_ARCHIVE);
        faultIfEmpty(project, ERROR_NO_PROJECT);
        faultIfEmpty(version, ERROR_NO_VERSION);
        faultIfEmpty(version, ERROR_NO_SUFFIX);
    }

    /**
     * string is for debug
     * @return
     */
    public String toString() {
        return "Library " + getNormalFilename()
                + " from project " + project
                + " to " + getDestinationName();
    }

    /**
     * calculate the destination file of a library; set {@link #libraryFile}
     * to the File thereof.
     *
     * @param baseDir dir that
     *
     * @throws BuildException if invalid
     */
    public void bind(File baseDir) {
        validate();
        if (destinationName == null) {
            destinationName = getNormalFilename();
        }
        libraryFile = new File(baseDir, destinationName);
    }

    /**
     * Test for a library
     * only valid after binding
     *
     * @return
     */
    public boolean exists() {
        return libraryFile.exists();
    }

    /**
     * get the last modified date
     * only valid after binding
     * @return
     */
    public long getLastModified() {
        return libraryFile.lastModified();
    }

    /**
     * get the filename from the rule of archive+version+'.'+suffix. Clearly
     * only valid if all fields are defined.
     *
     * @return a string representing the expected name of the file at the
     *         source
     */
    public String getNormalFilename() {
        return archive + "-" + version + "." + suffix;
    }

    /**
     * get the filename of the destination; no path.
     *
     * @return
     */
    public String getDestFilename() {
        if (destinationName == null) {
            return getNormalFilename();
        } else {
            return destinationName;
        }
    }

    /**
     * get a maven path (project/filename)
     *
     * @param separator directory separator
     *
     * @return
     */
    public String getMavenPath(char separator) {
        return project + separator + "jars" + separator + getNormalFilename();
    }

    /**
     * get the absolute path of this library
     *
     * @return
     */
    public String getAbsolutePath() {
        return libraryFile.getAbsolutePath();
    }

    /**
     * test for being enabled
     * @param project
     * @return
     */
    public boolean isEnabled(Project project) {
        if (unlessClause != null && project.getProperty(unlessClause) != null) {
            return false;
        }
        if (ifClause == null) {
            return true;
        }
        return project.getProperty(ifClause) != null;
    }


    /**
     * add our location to a filepath
     * @param path
     */
    public void appendToPath(Path path) {
        Path.PathElement pathElement = path.createPathElement();
        pathElement.setLocation(getLibraryFile());
    }


}
