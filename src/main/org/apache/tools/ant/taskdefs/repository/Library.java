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

package org.apache.tools.ant.taskdefs.repository;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.util.FileUtils;

import java.io.File;

/**
 * How we represent libraries
 *
 * @since Ant1.7
 */
public class Library implements EnabledLibraryElement {

    /**
     * enabled flag
     */
    private boolean enabled = true;

    private static FileUtils FILE_UTILS = FileUtils.newFileUtils();

    /**
     * turn policy on/off
     *
     * @param enabled
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * are we enabled
     *
     * @return true if {@link #enabled} is set
     */
    public boolean getEnabled() {
        return enabled;
    }

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
     * we fetch every library by default; note the enabled/disabled
     * flag has precedence, and this flag is not visible in the XML
     */
    private boolean toFetch = true;

    /**
     * flag set after fetching
     */
    private boolean fetched = false;

    public static final String ERROR_NO_ARCHIVE = "No archive defined";
    public static final String ERROR_NO_PROJECT = "No project defined";
    public static final String ERROR_NO_VERSION = "No version defined";
    public static final String ERROR_FILE_IS_A_DIR = "Library file is a directory:";

    /**
     * suffix
     */
    private String suffix = ".jar";


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
     * set the suffix for this file; default is ".jar"
     * @param suffix
     */
    public void setSuffix(String suffix) {
        this.suffix = suffix;
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
     * set the library file.
     * @param libraryFile
     */
    public void setLibraryFile(File libraryFile) {
        this.libraryFile = libraryFile;
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
        faultIfEmpty(project, ERROR_NO_PROJECT);
        if(archive==null) {
            //adopt the name of the project if no archive is specced
            archive=project;
        }
        faultIfEmpty(archive, ERROR_NO_ARCHIVE);
        faultIfEmpty(version, ERROR_NO_VERSION);
        if(suffix==null) {
            suffix="";
        }
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
     * @param baseDir dir that is used as the base for the operations
     *
     * @param flatten flag to indicate whether the directory path is 'flat' or not.
     * @throws BuildException if invalid
     */
    public void bind(File baseDir, boolean flatten) {
        validate();
        if (destinationName == null) {
            if(flatten) {
                destinationName = getNormalFilename();
            } else {
                destinationName = getMavenPath('/');
            }
        }
        libraryFile = FILE_UTILS.resolveFile(baseDir, destinationName);
        if (libraryFile.isDirectory()) {
            throw new BuildException(ERROR_FILE_IS_A_DIR
                + libraryFile);
        }
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
        return archive + "-" + version + suffix;
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
     * prefixed to avoid ant picking up on it, this sets
     * the fetch/no-fetch flag.
     * @param toFetch
     */
    public void _setToFetch(boolean toFetch) {
        this.toFetch = toFetch;
    }

    /**
     * get the fetch flag.
     * @return
     */
    public boolean isToFetch() {
        return toFetch;
    }

    /**
     * get a flag that marks if a file is fetched
     * @return
     */
    public boolean wasFetched() {
        return fetched;
    }

    /**
     * another not-for-end-users attribute; a flag set to true if the
     * library has been fetched.
     * @param fetched
     */
    public void _setFetched(boolean fetched) {
        this.fetched = fetched;
    }

    /**
     * add our location to a filepath
     * @param path
     */
    public void appendToPath(Path path) {
        Path.PathElement pathElement = path.createPathElement();
        pathElement.setLocation(getLibraryFile());
    }

    /**
     * equality test uses archive, destinationName, project, suffix and version
     * fields (any of which can be null)
     * @param o
     * @return
     */
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Library)) {
            return false;
        }

        final Library library = (Library) o;

        if (archive != null ? !archive.equals(library.archive) : library.archive != null) {
            return false;
        }
        if (destinationName != null ? !destinationName.equals(
                library.destinationName) : library.destinationName != null) {
            return false;
        }
        if (project != null ? !project.equals(library.project) : library.project != null) {
            return false;
        }
        if (suffix != null ? !suffix.equals(library.suffix) : library.suffix != null) {
            return false;
        }
        if (version != null ? !version.equals(library.version) : library.version != null) {
            return false;
        }

        return true;
    }

    /**
     * Hash code uses the name fields as {@link #equals(Object)}
     * This sequence
     * @return
     */
    public int hashCode() {
        int result;
        result = (project != null ? project.hashCode() : 0);
        result = 29 * result + (version != null ? version.hashCode() : 0);
        result = 29 * result + (archive != null ? archive.hashCode() : 0);
        result = 29 * result + (destinationName != null ? destinationName.hashCode() : 0);
        result = 29 * result + (suffix != null ? suffix.hashCode() : 0);
        return result;
    }

}
