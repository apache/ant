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
import java.util.LinkedList;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.util.FileUtils;

/**
 * <p>This task takes file and turns them into a URL, which it then assigns
 * to a property. Use when for setting up RMI codebases.</p>
 *
 * <p>nested filesets are supported; if present, these are turned into the
 * url with the given separator between them (default = " ").</p>
 *
 * @ant.task category="core" name="makeurl"
 */

public class MakeUrl extends Task {
    // error message strings
    /** Missing file */
    public static final String ERROR_MISSING_FILE = "A source file is missing: ";
    /** No property defined */
    public static final String ERROR_NO_PROPERTY = "No property defined";
    /** No files defined */
    public static final String ERROR_NO_FILES = "No files defined";

    /**
     * name of the property to set
     */
    private String property;

    /**
     * name of a file to turn into a URL
     */
    private File file;

    /**
     * separator char
     */
    private String separator = " ";

    /**
     * filesets of nested files to add to this url
     */
    private List<FileSet> filesets = new LinkedList<>();

    /**
     * paths to add
     */
    private List<Path> paths = new LinkedList<>();

    /**
     * validation flag
     */
    private boolean validate = true;

    /**
     * set the name of a property to fill with the URL
     *
     * @param property the name of the property.
     */
    public void setProperty(String property) {
        this.property = property;
    }

    /**
     * the name of a file to be converted into a URL
     *
     * @param file the file to be converted.
     */
    public void setFile(File file) {
        this.file = file;
    }

    /**
     * a fileset of jar files to include in the URL, each
     * separated by the separator
     *
     * @param fileset the fileset to be added.
     */
    public void addFileSet(FileSet fileset) {
        filesets.add(fileset);
    }

    /**
     * set the separator for the multi-url option.
     *
     * @param separator the separator to use.
     */
    public void setSeparator(String separator) {
        this.separator = separator;
    }

    /**
     * set this flag to trigger validation that every named file exists.
     * Optional: default=true
     *
     * @param validate a <code>boolean</code> value.
     */
    public void setValidate(boolean validate) {
        this.validate = validate;
    }

    /**
     * add a path to the URL. All elements in the path
     * will be converted to individual URL entries
     *
     * @param path a path value.
     */
    public void addPath(Path path) {
        paths.add(path);
    }

    /**
     * convert the filesets to urls.
     *
     * @return null for no files
     */
    private String filesetsToURL() {
        if (filesets.isEmpty()) {
            return "";
        }
        int count = 0;
        StringBuilder urls = new StringBuilder();
        for (FileSet fs : filesets) {
            DirectoryScanner scanner = fs.getDirectoryScanner(getProject());
            for (String file : scanner.getIncludedFiles()) {
                File f = new File(scanner.getBasedir(), file);
                validateFile(f);
                String asUrl = toURL(f);
                urls.append(asUrl);
                log(asUrl, Project.MSG_DEBUG);
                urls.append(separator);
                count++;
            }
        }
        //at this point there is one trailing space to remove, if the list is not empty.
        return stripTrailingSeparator(urls, count);
    }

    /**
     * convert the string buffer to a string, potentially stripping
     * out any trailing separator
     *
     * @param urls  URL buffer
     * @param count number of URL entries
     * @return trimmed string, or empty string
     */
    private String stripTrailingSeparator(StringBuilder urls,
                                          int count) {
        if (count > 0) {
            urls.delete(urls.length() - separator.length(), urls.length());
            return new String(urls);
        }
        return "";
    }


    /**
     * convert all paths to URLs
     *
     * @return the paths as a separated list of URLs
     */
    private String pathsToURL() {
        if (paths.isEmpty()) {
            return "";
        }
        int count = 0;
        StringBuilder urls = new StringBuilder();
        for (Path path : paths) {
            for (String element : path.list()) {
                File f = new File(element);
                validateFile(f);
                String asUrl = toURL(f);
                urls.append(asUrl);
                log(asUrl, Project.MSG_DEBUG);
                urls.append(separator);
                count++;
            }
        }
        //at this point there is one trailing space to remove, if the list is not empty.
        return stripTrailingSeparator(urls, count);
    }

    /**
     * verify that the file exists, if {@link #validate} is set
     *
     * @param fileToCheck file that may need to exist
     * @throws BuildException with text beginning {@link #ERROR_MISSING_FILE}
     */
    private void validateFile(File fileToCheck) {
        if (validate && !fileToCheck.exists()) {
            throw new BuildException(ERROR_MISSING_FILE + fileToCheck);
        }
    }

    /**
     * Create the url
     *
     * @throws BuildException
     *          if something goes wrong with the build
     */
    @Override
    public void execute() throws BuildException {
        validate();
        //now exit here if the property is already set
        if (getProject().getProperty(property) != null) {
            return;
        }
        String url;
        String filesetURL = filesetsToURL();
        if (file == null) {
            url = filesetURL;
        } else {
            validateFile(file);
            url = toURL(file);
            //and add any files if also defined
            if (!filesetURL.isEmpty()) {
                url += separator + filesetURL;
            }
        }
        //add path URLs
        String pathURL = pathsToURL();
        if (!pathURL.isEmpty()) {
            if (url.isEmpty()) {
                url = pathURL;
            } else {
                url += separator + pathURL;
            }
        }
        log("Setting " + property + " to URL " + url, Project.MSG_VERBOSE);
        getProject().setNewProperty(property, url);
    }

    /**
     * check for errors
     * @throws BuildException if we are not configured right
     */
    private void validate() {
        //validation
        if (property == null) {
            throw new BuildException(ERROR_NO_PROPERTY);
        }
        if (file == null && filesets.isEmpty() && paths.isEmpty()) {
            throw new BuildException(ERROR_NO_FILES);
        }
    }

    /**
     * convert a file to a URL;
     *
     * @param fileToConvert File
     * @return the file converted to a URL
     */
    private String toURL(File fileToConvert) {
        //create the URL
        //ant equivalent of  fileToConvert.toURI().toURL().toExternalForm();
        return FileUtils.getFileUtils().toURI(fileToConvert.getAbsolutePath());
    }

}
