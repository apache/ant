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

import java.io.File;

/**
 * How we represent libraries
 *
 * @since 20-Oct-2004
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

    private File libraryFile;

    public static final String ERROR_NO_ARCHIVE = "No archive defined";
    public static final String ERROR_NO_PROJECT = "No project defined";
    public static final String ERROR_NO_VERSION = "No version defined";
    public static final String ERROR_NO_SUFFIX = "No version defined";

    /**
     * suffix
     */
    private String suffix = "jar";


    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getArchive() {
        return archive;
    }

    public void setArchive(String archive) {
        this.archive = archive;
    }

    public String getDestinationName() {
        return destinationName;
    }

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

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

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

    public String toString() {
        return "Library " + getNormalFilename()
                + " from project " + project
                + " to " + getDestinationName();
    }

    /**
     * calculare the destination file of a library
     *
     * @param baseDir
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
     * a test that is only valid after binding
     *
     * @return
     */
    public boolean exists() {
        return libraryFile.exists();
    }

    /**
     * get the last modified date
     *
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

}
