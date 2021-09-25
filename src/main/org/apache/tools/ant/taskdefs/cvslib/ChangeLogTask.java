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
package org.apache.tools.ant.taskdefs.cvslib;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.AbstractCvsTask;
import org.apache.tools.ant.types.FileSet;

/**
 * Examines the output of cvs log and group related changes together.
 *
 * It produces an XML output representing the list of changes.
 * <pre>
 * &lt;!-- Root element --&gt;
 * &lt;!ELEMENT changelog (entry+)&gt;
 * &lt;!-- CVS Entry --&gt;
 * &lt;!ELEMENT entry (date,author,file+,msg)&gt;
 * &lt;!-- Date of cvs entry --&gt;
 * &lt;!ELEMENT date (#PCDATA)&gt;
 * &lt;!-- Author of change --&gt;
 * &lt;!ELEMENT author (#PCDATA)&gt;
 * &lt;!-- List of files affected --&gt;
 * &lt;!ELEMENT msg (#PCDATA)&gt;
 * &lt;!-- File changed --&gt;
 * &lt;!ELEMENT file (name,revision,prevrevision?)&gt;
 * &lt;!-- Name of the file --&gt;
 * &lt;!ELEMENT name (#PCDATA)&gt;
 * &lt;!-- Revision number --&gt;
 * &lt;!ELEMENT revision (#PCDATA)&gt;
 * &lt;!-- Previous revision number --&gt;
 * &lt;!ELEMENT prevrevision (#PCDATA)&gt;
 * </pre>
 *
 * @since Ant 1.5
 * @ant.task name="cvschangelog" category="scm"
 */
public class ChangeLogTask extends AbstractCvsTask {
    /** User list */
    private File usersFile;

    /** User list */
    private List<CvsUser> cvsUsers = new Vector<>();

    /** Input dir */
    private File inputDir;

    /** Output file */
    private File destFile;

    /** The earliest date at which to start processing entries.  */
    private Date startDate;

    /** The latest date at which to stop processing entries.  */
    private Date endDate;

    /** Determines whether log (false) or rlog (true) is used */
    private boolean remote = false;

    /** Start tag when doing tag ranges. */
    private String startTag;

    /** End tag when doing tag ranges. */
    private String endTag;

    /**
     * Filesets containing list of files against which the cvs log will be
     * performed. If empty then all files in the working directory will
     * be checked.
     */
    private final List<FileSet> filesets = new Vector<>();

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
     * Set a lookup list of user names &amp; addresses
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
        cvsUsers.add(user);
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
        // CheckStyle:MagicNumber OFF
        final long time = System.currentTimeMillis()
             - (long) days * 24 * 60 * 60 * 1000;
        // CheckStyle:MagicNumber ON

        setStart(new Date(time));
    }

    /**
     * Whether to use rlog against a remote repository instead of log
     * in a working copy's directory.
     *
     * @param remote boolean
     * @since Ant 1.8.0
     */
    public void setRemote(final boolean remote) {
        this.remote = remote;
    }

    /**
     * Set the tag at which the changelog should start.
     *
     * @param start The date at which the changelog should start.
     */
    public void setStartTag(final String start) {
        this.startTag = start;
    }

    /**
     * Set the tag at which the changelog should stop.
     *
     * @param end The date at which the changelog should stop.
     */
    public void setEndTag(final String end) {
        this.endTag = end;
    }

    /**
     * Adds a set of files about which cvs logs will be generated.
     *
     * @param fileSet a set of files about which cvs logs will be generated.
     */
    public void addFileset(final FileSet fileSet) {
        filesets.add(fileSet);
    }

    /**
     * Execute task
     *
     * @exception BuildException if something goes wrong executing the
     *            cvs command
     */
    @Override
    public void execute() throws BuildException {
        File savedDir = inputDir; // may be altered in validate

        try {
            validate();
            final Properties userList = new Properties();

            loadUserlist(userList);

            for (CvsUser user : cvsUsers) {
                user.validate();
                userList.put(user.getUserID(), user.getDisplayname());
            }

            if (!remote) {
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
            } else {
                // supply 'rlog' as argument instead of command
                setCommand("");
                addCommandArgument("rlog");
                // Do not print name/header if no revisions
                // selected. This is quicker: less output to parse.
                addCommandArgument("-S");
                // Do not list tags. This is quicker: less output to
                // parse.
                addCommandArgument("-N");
            }
            if (null != startTag || null != endTag) {
                // man, do I get spoiled by C#'s ?? operator
                String startValue = startTag == null ? "" : startTag;
                String endValue = endTag == null ? "" : endTag;
                addCommandArgument("-r" + startValue + "::" + endValue);
            } else if (null != startDate) {
                final SimpleDateFormat outputDate =
                    new SimpleDateFormat("yyyy-MM-dd");

                // We want something of the form: -d ">=YYYY-MM-dd"
                final String dateRange = ">=" + outputDate.format(startDate);

                // Supply '-d' as a separate argument - Bug# 14397
                addCommandArgument("-d");
                addCommandArgument(dateRange);
            }

            // Check if list of files to check has been specified
            for (FileSet fileSet : filesets) {
                final DirectoryScanner scanner =
                    fileSet.getDirectoryScanner(getProject());
                for (String file : scanner.getIncludedFiles()) {
                    addCommandArgument(file);
                }
            }

            final ChangeLogParser parser = new ChangeLogParser(remote,
                                                               getPackage(),
                                                               getModules());
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
            throw new BuildException("Destfile must be set.");
        }
        if (!inputDir.exists()) {
            throw new BuildException("Cannot find base dir %s",
                inputDir.getAbsolutePath());
        }
        if (null != usersFile && !usersFile.exists()) {
            throw new BuildException("Cannot find user lookup list %s",
                usersFile.getAbsolutePath());
        }
        if ((null != startTag || null != endTag)
            && (null != startDate || null != endDate)) {
            throw new BuildException(
                "Specify either a tag or date range, not both");
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
                userList.load(Files.newInputStream(usersFile.toPath()));
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
        final List<CVSEntry> results = new ArrayList<>();

        for (CVSEntry cvsEntry : entrySet) {
            final Date date = cvsEntry.getDate();

            //bug#30471
            //this is caused by Date.after throwing a NullPointerException
            //for some reason there's no date set in the CVSEntry
            //https://docs.oracle.com/javase/1.5.0/docs/api/java/util/Date.html#after(java.util.Date)
            //according to the docs as of 1.5 it does throw

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
            results.add(cvsEntry);
        }

        return results.toArray(new CVSEntry[0]);
    }

    /**
     * replace all known author's id's with their maven specified names
     */
    private void replaceAuthorIdWithName(final Properties userList,
                                         final CVSEntry[] entrySet) {
        for (final CVSEntry entry : entrySet) {
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

        try (final PrintWriter writer = new PrintWriter(
            new OutputStreamWriter(Files.newOutputStream(destFile.toPath()), StandardCharsets.UTF_8))) {

            new ChangeLogWriter().printChangeLog(writer, entrySet);

            if (writer.checkError()) {
                throw new IOException("Encountered an error writing changelog");
            }
        } catch (final UnsupportedEncodingException uee) {
            getProject().log(uee.toString(), Project.MSG_ERR);
        } catch (final IOException ioe) {
            throw new BuildException(ioe.toString(), ioe);
        }
    }
}
