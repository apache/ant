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
import org.apache.tools.ant.Project;
import org.apache.tools.ant.util.FileUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Properties;

/**
 * This {@link org.apache.tools.ant.taskdefs.repository.LibraryPolicy} updates the files only
 * when the schedule indicates that it should.
 * <p/>
 * The default interval is eleven hours; it's prime, it encourages
 * regular but not excessive days.
 * <p/>
 * It requires a marker file which is used to save a list of all files
 * that were saved. If anything in the list of files changes then the
 * update is triggered again.
 */
public class ScheduledUpdatePolicy extends BaseLibraryPolicy  {
    private File markerFile;
    private int hours = 17;
    private int days = 0;

    /**
     * if not null, this means that we have a marker file to save
     */
    private Properties markerFileToSave;

    public static final String ERROR_NO_MARKER_FILE
        = "No marker file";
    public static final String MARKER_MISMATCH
        = "No match between last update and current one";
    public static final String INTERVAL_TRIGGERS_UPDATE
        = "Interval between updates is long; updating";
    public static final String INTERVAL_SHORT_NO_UPDATE
        = "Interval between updates is short; no update";


    public File getMarkerFile() {
        return markerFile;
    }

    /**
     * set a file that stores the history of the operation
     * @param markerFile
     */
    public void setMarkerFile(File markerFile) {
        this.markerFile = markerFile;
    }

    public int getHours() {
        return hours;
    }

    /**
     * set the interval between updates in hours
     * @param hours
     */
    public void setHours(int hours) {
        this.hours = hours;
    }

    public int getDays() {
        return days;
    }

    /**
     * set the interval between updates in days.
     * @param days
     */
    public void setDays(int days) {
        this.days = days;
    }

    /**
     * get the refresh interval in milliseconds
     * @return
     */
    public long getInterval() {
        return ((days * 24) + hours) * 60 * 60000;
    }


    /**
     * Method called before we connect
     *
     * @param owner
     *
     * @param libraries
     * @return true if the connection is to go ahead
     *
     * @throws org.apache.tools.ant.BuildException
     *          if needed
     */
    public boolean beforeConnect(Libraries owner, ListIterator libraries) {

        Repository repository = owner.getRepository();
        if (markerFile == null) {
            throw new BuildException(ERROR_NO_MARKER_FILE);
        }
        Properties now = makeProperties(owner.enabledLibrariesIterator(), repository);
        try {
            if (markerFile.exists()) {
                long timestamp = markerFile.lastModified();
                Properties then = loadMarkerFile();
                long currentTime = System.currentTimeMillis();
                long diff = currentTime - timestamp;
                if (now.equals(then)) {
                    if (diff < getInterval()) {
                        owner.log(INTERVAL_SHORT_NO_UPDATE,
                                Project.MSG_VERBOSE);
                        return false;
                    } else {
                        owner.log(INTERVAL_TRIGGERS_UPDATE,
                                Project.MSG_VERBOSE);
                        return true;
                    }
                } else {
                    owner.log(MARKER_MISMATCH,
                            Project.MSG_VERBOSE);
                }
            } else {
                owner.log("Marker file not found", Project.MSG_VERBOSE);
            }
            markerFileToSave = now;
            return true;
        } catch (IOException e) {
            throw new BuildException(
                "Marker file " + markerFile.getAbsolutePath() + " access failed", e);
        }
    }

    /**
     * method called after a (nominally successful fetch)
     *
     * @param owner
     * @param libraries
     */
    public void afterFetched(Libraries owner, ListIterator libraries) {

        if (markerFileToSave != null) {
            //if we get here, we need to save the file
            try {
                saveMarkerFile(markerFileToSave);
            } catch (IOException e) {
                throw new BuildException("Failed to save marker file "
                        + markerFile,
                        e);
            }
        } else {
            //touch the file anyway
            markerFile.setLastModified(System.currentTimeMillis());
        }

    }

    /**
     * make a properties file from the library list
     * @param libraries iterator of type Library.
     * @return a new properties file
     */
    protected Properties makeProperties(Iterator libraries, Repository repository) {
        Properties props = new Properties();
        int counter = 1;
        while (libraries.hasNext()) {
            Library library = (Library) libraries.next();
            String name = makeEntry(library);
            props.put(Integer.toString(counter), name);
        }
        props.put("repository", repository.getRepositoryURI());
        return props;
    }

    /**
     * save a property file to disk.
     * @param props
     * @throws IOException
     */
    protected void saveMarkerFile(Properties props)
            throws IOException {
        markerFile.getParentFile().mkdirs();
        OutputStream out = new BufferedOutputStream(new FileOutputStream(markerFile));
        try {
            props.store(out, null);
        } finally {
            FileUtils.close(out);
        }
    }

    /**
     * Load an input stream
     * @return
     * @throws IOException
     */
    protected Properties loadMarkerFile() throws IOException {
        Properties props = new Properties();
        InputStream in = new BufferedInputStream(new FileInputStream(markerFile));
        try {
            props.load(in);
            return props;
        } finally {
            FileUtils.close(in);
        }
    }




    /**
     * make an entry for the properties file
     * @param lib
     * @return
     */
    protected String makeEntry(Library lib) {
        return lib.getMavenPath('/') + "//" + lib.getNormalFilename();
    }
}
