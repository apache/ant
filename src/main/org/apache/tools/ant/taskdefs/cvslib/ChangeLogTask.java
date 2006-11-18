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
package org.apache.tools.ant.taskdefs.cvslib;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.AbstractCvsTask;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.util.FileUtils;

/**
 * Examines the output of cvs log and group related changes together.
 *
 * It produces an XML output representing the list of changes.
 * <pre>
 * <font color=#0000ff>&lt;!-- Root element --&gt;</font>
 * <font color=#6a5acd>&lt;!ELEMENT</font> changelog <font color=#ff00ff>
 * (entry</font><font color=#ff00ff>+</font><font color=#ff00ff>)
 * </font><font color=#6a5acd>&gt;</font>
 * <font color=#0000ff>&lt;!-- CVS Entry --&gt;</font>
 * <font color=#6a5acd>&lt;!ELEMENT</font> entry <font color=#ff00ff>
 * (date,author,file</font><font color=#ff00ff>+</font><font color=#ff00ff>,msg)
 * </font><font color=#6a5acd>&gt;</font>
 * <font color=#0000ff>&lt;!-- Date of cvs entry --&gt;</font>
 * <font color=#6a5acd>&lt;!ELEMENT</font> date <font color=#ff00ff>(#PCDATA)
 * </font><font color=#6a5acd>&gt;</font>
 * <font color=#0000ff>&lt;!-- Author of change --&gt;</font>
 * <font color=#6a5acd>&lt;!ELEMENT</font> author <font color=#ff00ff>(#PCDATA)
 * </font><font color=#6a5acd>&gt;</font>
 * <font color=#0000ff>&lt;!-- List of files affected --&gt;</font>
 * <font color=#6a5acd>&lt;!ELEMENT</font> msg <font color=#ff00ff>(#PCDATA)
 * </font><font color=#6a5acd>&gt;</font>
 * <font color=#0000ff>&lt;!-- File changed --&gt;</font>
 * <font color=#6a5acd>&lt;!ELEMENT</font> file <font color=#ff00ff>
 * (name,revision,prevrevision</font><font color=#ff00ff>?</font>
 * <font color=#ff00ff>)</font><font color=#6a5acd>&gt;</font>
 * <font color=#0000ff>&lt;!-- Name of the file --&gt;</font>
 * <font color=#6a5acd>&lt;!ELEMENT</font> name <font color=#ff00ff>(#PCDATA)
 * </font><font color=#6a5acd>&gt;</font>
 * <font color=#0000ff>&lt;!-- Revision number --&gt;</font>
 * <font color=#6a5acd>&lt;!ELEMENT</font> revision <font color=#ff00ff>
 * (#PCDATA)</font><font color=#6a5acd>&gt;</font>
 * <font color=#0000ff>&lt;!-- Previous revision number --&gt;</font>
 * <font color=#6a5acd>&lt;!ELEMENT</font> prevrevision <font color=#ff00ff>
 * (#PCDATA)</font><font color=#6a5acd>&gt;</font>
 * </pre>
 *
 * @since Ant 1.5
 * @ant.task name="cvschangelog" category="scm"
 */
public class ChangeLogTask extends AbstractCvsTask {
    /** User list */
    private File usersFile;

    /** User list */
    private Vector cvsUsers = new Vector();

    /** Input dir */
    private File inputDir;

    /** Output file */
    private File destFile;

    /** The earliest date at which to start processing entries.  */
    private Date startDate;

    /** The latest date at which to stop processing entries.  */
    private Date endDate;

    /**
     * Filesets containing list of files against which the cvs log will be
     * performed. If empty then all files in the working directory will
     * be checked.
     */
    private final Vector filesets = new Vector();


    /**
     * Set the base dir for cvs.
     *
     * @param inputDir The new dir value
     */
    public void setDir(final File inputDir) {
        this.inputDir = inputDir;
    }


    /**
     * Set the output file for the log.
     *
     * @param destFile The new destfile value
     */
    public void setDestfile(final File destFile) {
        this.destFile = destFile;
    }


    /**
     * Set a lookup list of user names & addresses
     *
     * @param usersFile The file containing the users info.
     */
    public void setUsersfile(final File usersFile) {
        this.usersFile = usersFile;
    }


    /**
     * Add a user to list changelog knows about.
     *
     * @param user the user
     */
    public void addUser(final CvsUser user) {
        cvsUsers.addElement(user);
    }


    /**
     * Set the date at which the changelog should start.
     *
     * @param start The date at which the changelog should start.
     */
    public void setStart(final Date start) {
        this.startDate = start;
    }


    /**
     * Set the date at which the changelog should stop.
     *
     * @param endDate The date at which the changelog should stop.
     */
    public void setEnd(final Date endDate) {
        this.endDate = endDate;
    }


    /**
     * Set the number of days worth of log entries to process.
     *
     * @param days the number of days of log to process.
     */
    public void setDaysinpast(final int days) {
        final long time = System.currentTimeMillis()
             - (long) days * 24 * 60 * 60 * 1000;

        setStart(new Date(time));
    }


    /**
     * Adds a set of files about which cvs logs will be generated.
     *
     * @param fileSet a set of files about which cvs logs will be generated.
     */
    public void addFileset(final FileSet fileSet) {
        filesets.addElement(fileSet);
    }


    /**
     * Execute task
     *
     * @exception BuildException if something goes wrong executing the
     *            cvs command
     */
    public void execute() throws BuildException {
        File savedDir = inputDir; // may be altered in validate

        try {

            validate();
            final Properties userList = new Properties();

            loadUserlist(userList);

            for (int i = 0, size = cvsUsers.size(); i < size; i++) {
                final CvsUser user = (CvsUser) cvsUsers.get(i);
                user.validate();
                userList.put(user.getUserID(), user.getDisplayname());
            }

            setCommand("log");

            if (getTag() != null) {
                CvsVersion myCvsVersion = new CvsVersion();
                myCvsVersion.setProject(getProject());
                myCvsVersion.setTaskName("cvsversion");
                myCvsVersion.setCvsRoot(getCvsRoot());
                myCvsVersion.setCvsRsh(getCvsRsh());
                myCvsVersion.setPassfile(getPassFile());
                myCvsVersion.setDest(inputDir);
                myCvsVersion.execute();
                if (myCvsVersion.supportsCvsLogWithSOption()) {
                    addCommandArgument("-S");
                }
            }
            if (null != startDate) {
                final SimpleDateFormat outputDate =
                    new SimpleDateFormat("yyyy-MM-dd");

                // We want something of the form: -d ">=YYYY-MM-dd"
                final String dateRange = ">=" + outputDate.format(startDate);

                // Supply '-d' as a separate argument - Bug# 14397
                addCommandArgument("-d");
                addCommandArgument(dateRange);
            }

            // Check if list of files to check has been specified
            if (!filesets.isEmpty()) {
                final Enumeration e = filesets.elements();

                while (e.hasMoreElements()) {
                    final FileSet fileSet = (FileSet) e.nextElement();
                    final DirectoryScanner scanner =
                        fileSet.getDirectoryScanner(getProject());
                    final String[] files = scanner.getIncludedFiles();

                    for (int i = 0; i < files.length; i++) {
                        addCommandArgument(files[i]);
                    }
                }
            }

            final ChangeLogParser parser = new ChangeLogParser();
            final RedirectingStreamHandler handler =
                new RedirectingStreamHandler(parser);

            log(getCommand(), Project.MSG_VERBOSE);

            setDest(inputDir);
            setExecuteStreamHandler(handler);
            try {
                super.execute();
            } finally {
                final String errors = handler.getErrors();

                if (null != errors) {
                    log(errors, Project.MSG_ERR);
                }
            }
            final CVSEntry[] entrySet = parser.getEntrySetAsArray();
            final CVSEntry[] filteredEntrySet = filterEntrySet(entrySet);

            replaceAuthorIdWithName(userList, filteredEntrySet);

            writeChangeLog(filteredEntrySet);

        } finally {
            inputDir = savedDir;
        }
    }

    /**
     * Validate the parameters specified for task.
     *
     * @throws BuildException if fails validation checks
     */
    private void validate()
         throws BuildException {
        if (null == inputDir) {
            inputDir = getProject().getBaseDir();
        }
        if (null == destFile) {
            final String message = "Destfile must be set.";

            throw new BuildException(message);
        }
        if (!inputDir.exists()) {
            final String message = "Cannot find base dir "
                 + inputDir.getAbsolutePath();

            throw new BuildException(message);
        }
        if (null != usersFile && !usersFile.exists()) {
            final String message = "Cannot find user lookup list "
                 + usersFile.getAbsolutePath();

            throw new BuildException(message);
        }
    }

    /**
     * Load the userlist from the userList file (if specified) and add to
     * list of users.
     *
     * @param userList the file of users
     * @throws BuildException if file can not be loaded for some reason
     */
    private void loadUserlist(final Properties userList)
         throws BuildException {
        if (null != usersFile) {
            try {
                userList.load(new FileInputStream(usersFile));
            } catch (final IOException ioe) {
                throw new BuildException(ioe.toString(), ioe);
            }
        }
    }

    /**
     * Filter the specified entries according to an appropriate rule.
     *
     * @param entrySet the entry set to filter
     * @return the filtered entry set
     */
    private CVSEntry[] filterEntrySet(final CVSEntry[] entrySet) {
        final Vector results = new Vector();

        for (int i = 0; i < entrySet.length; i++) {
            final CVSEntry cvsEntry = entrySet[i];
            final Date date = cvsEntry.getDate();

            //bug#30471
            //this is caused by Date.after throwing a NullPointerException
            //for some reason there's no date set in the CVSEntry
            //Java 1.3.1 API
            //http://java.sun.com/j2se/1.3/docs/api/java/util/Date.html#after(java.util.Date)
            //doesn't throw NullPointerException
            //Java 1.4.2 + 1.5 API
            //http://java.sun.com/j2se/1.4.2/docs/api/java/util/Date.html#after(java.util.Date)
            //according to the docs it doesn't throw, according to the bug report it does
            //http://java.sun.com/j2se/1.5.0/docs/api/java/util/Date.html#after(java.util.Date)
            //according to the docs it does throw

            //for now skip entries which are missing a date
            if (null == date) {
                continue;
            }

            if (null != startDate && startDate.after(date)) {
                //Skip dates that are too early
                continue;
            }
            if (null != endDate && endDate.before(date)) {
                //Skip dates that are too late
                continue;
            }
            results.addElement(cvsEntry);
        }

        final CVSEntry[] resultArray = new CVSEntry[results.size()];

        results.copyInto(resultArray);
        return resultArray;
    }

    /**
     * replace all known author's id's with their maven specified names
     */
    private void replaceAuthorIdWithName(final Properties userList,
                                         final CVSEntry[] entrySet) {
        for (int i = 0; i < entrySet.length; i++) {

            final CVSEntry entry = entrySet[ i ];
            if (userList.containsKey(entry.getAuthor())) {
                entry.setAuthor(userList.getProperty(entry.getAuthor()));
            }
        }
    }

    /**
     * Print changelog to file specified in task.
     *
     * @param entrySet the entry set to write.
     * @throws BuildException if there is an error writing changelog.
     */
    private void writeChangeLog(final CVSEntry[] entrySet)
         throws BuildException {
        FileOutputStream output = null;

        try {
            output = new FileOutputStream(destFile);

            final PrintWriter writer =
                new PrintWriter(new OutputStreamWriter(output, "UTF-8"));

            final ChangeLogWriter serializer = new ChangeLogWriter();

            serializer.printChangeLog(writer, entrySet);
        } catch (final UnsupportedEncodingException uee) {
            getProject().log(uee.toString(), Project.MSG_ERR);
        } catch (final IOException ioe) {
            throw new BuildException(ioe.toString(), ioe);
        } finally {
            FileUtils.close(output);
        }
    }
}

