/*
 * Copyright 2005 The Apache Software Foundation
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
package org.apache.tools.ant.taskdefs.svn;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.LogOutputStream;
import org.apache.tools.ant.taskdefs.PumpStreamHandler;
import org.apache.tools.ant.taskdefs.cvslib.CvsUser;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.util.FileUtils;

/**
 * Examines the output of svn log and group related changes together.
 *
 * It produces an XML output representing the list of changes.
 * <pre>
 * <font color=#0000ff>&lt;!-- Root element --&gt;</font>
 * <font color=#6a5acd>&lt;!ELEMENT</font> changelog <font color=#ff00ff>(entry</font><font color=#ff00ff>+</font><font color=#ff00ff>)</font><font color=#6a5acd>&gt;</font>
 * <font color=#0000ff>&lt;!-- SVN Entry --&gt;</font>
 * <font color=#6a5acd>&lt;!ELEMENT</font> entry <font color=#ff00ff>(date,time,revision,author,file</font><font color=#ff00ff>+,msg</font><font color=#ff00ff>,msg)</font><font color=#6a5acd>&gt;</font>
 * <font color=#0000ff>&lt;!-- Date of svn entry --&gt;</font>
 * <font color=#6a5acd>&lt;!ELEMENT</font> date <font color=#ff00ff>(#PCDATA)</font><font color=#6a5acd>&gt;</font>
 * <font color=#0000ff>&lt;!-- Time of svn entry --&gt;</font>
 * <font color=#6a5acd>&lt;!ELEMENT</font> time <font color=#ff00ff>(#PCDATA)</font><font color=#6a5acd>&gt;</font>
 * <font color=#0000ff>&lt;!-- Author of change --&gt;</font>
 * <font color=#6a5acd>&lt;!ELEMENT</font> author <font color=#ff00ff>(#PCDATA)</font><font color=#6a5acd>&gt;</font>
 * <font color=#0000ff>&lt;!-- commit message --&gt;</font>
 * <font color=#6a5acd>&lt;!ELEMENT</font> msg <font color=#ff00ff>(#PCDATA)</font><font color=#6a5acd>&gt;</font>
 * <font color=#0000ff>&lt;!-- List of files affected --&gt;</font>
 * <font color=#6a5acd>&lt;!ELEMENT</font> file <font color=#ff00ff>(name</font><font color=#ff00ff>?</font><font color=#ff00ff>)</font><font color=#6a5acd>&gt;</font>
 * <font color=#0000ff>&lt;!-- Name of the file --&gt;</font>
 * <font color=#6a5acd>&lt;!ELEMENT</font> name <font color=#ff00ff>(#PCDATA)</font><font color=#6a5acd>&gt;</font>
 * <font color=#0000ff>&lt;!-- Revision number --&gt;</font>
 * <font color=#6a5acd>&lt;!ELEMENT</font> revision <font color=#ff00ff>(#PCDATA)</font><font color=#6a5acd>&gt;</font>
 * </pre>
 *
 * @ant.task name="svnchangelog" category="scm"
 */
public class SvnChangeLogTask extends AbstractSvnTask {
    /** User list */
    private File usersFile;

    /** User list */
    private Vector svnUsers = new Vector();

    /** Input dir */
    private File inputDir;

    /** Output file */
    private File destFile;

    /** The earliest revision at which to start processing entries.  */
    private String startRevision;

    /** The latest revision at which to stop processing entries.  */
    private String endRevision;

    /**
     * Filesets containing list of files against which the svn log will be
     * performed. If empty then all files in the working directory will
     * be checked.
     */
    private final Vector filesets = new Vector();


    /**
     * Set the base dir for svn.
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
        svnUsers.addElement(user);
    }


    /**
     * Set the revision at which the changelog should start.
     *
     * @param start The revision at which the changelog should start.
     */
    public void setStart(final String start) {
        this.startRevision = start;
    }


    /**
     * Set the revision at which the changelog should stop.
     *
     * @param endRevision The revision at which the changelog should stop.
     */
    public void setEnd(final String endRevision) {
        this.endRevision = endRevision;
    }


    /**
     * Set the number of days worth of log entries to process.
     *
     * @param days the number of days of log to process.
     */
    public void setDaysinpast(final int days) {
        final long time = System.currentTimeMillis()
            - (long) days * 24 * 60 * 60 * 1000;

        final SimpleDateFormat outputDate =
            new SimpleDateFormat("{yyyy-MM-dd}");
        setStart(outputDate.format(new Date(time)));
    }


    /**
     * Adds a set of files about which svn logs will be generated.
     *
     * @param fileSet a set of files about which svn logs will be generated.
     */
    public void addFileset(final FileSet fileSet) {
        filesets.addElement(fileSet);
    }


    /**
     * Execute task
     *
     * @exception BuildException if something goes wrong executing the
     *            svn command
     */
    public void execute() throws BuildException {
        File savedDir = inputDir; // may be altered in validate

        try {

            validate();
            final Properties userList = new Properties();

            loadUserlist(userList);

            for (int i = 0, size = svnUsers.size(); i < size; i++) {
                final CvsUser user = (CvsUser) svnUsers.get(i);
                user.validate();
                userList.put(user.getUserID(), user.getDisplayname());
            }

            setSubCommand("log");
            setVerbose(true);

            if (null != startRevision) {
                if (null != endRevision) {
                    setRevision(startRevision + ":" + endRevision);
                } else {
                    setRevision(startRevision + ":HEAD");
                }
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
                        addSubCommandArgument(files[i]);
                    }
                }
            }

            final SvnChangeLogParser parser = new SvnChangeLogParser();
            final PumpStreamHandler handler =
                new PumpStreamHandler(parser,
                                      new LogOutputStream(this,
                                                          Project.MSG_ERR));

            log(getSubCommand(), Project.MSG_VERBOSE);

            setDest(inputDir);
            setExecuteStreamHandler(handler);
            super.execute();

            final SvnEntry[] entrySet = parser.getEntrySetAsArray();
            final SvnEntry[] filteredEntrySet = filterEntrySet(entrySet);

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
            inputDir = getDest();
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
    private SvnEntry[] filterEntrySet(final SvnEntry[] entrySet) {
        final Vector results = new Vector();

        for (int i = 0; i < entrySet.length; i++) {
            final SvnEntry svnEntry = entrySet[i];

            if (null != endRevision && !isBeforeEndRevision(svnEntry)) {
                //Skip revisions that are too late
                continue;
            }
            results.addElement(svnEntry);
        }

        final SvnEntry[] resultArray = new SvnEntry[results.size()];

        results.copyInto(resultArray);
        return resultArray;
    }

    /**
     * replace all known author's id's with their maven specified names
     */
    private void replaceAuthorIdWithName(final Properties userList,
                                         final SvnEntry[] entrySet) {
        for (int i = 0; i < entrySet.length; i++) {

            final SvnEntry entry = entrySet[ i ];
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
    private void writeChangeLog(final SvnEntry[] entrySet)
        throws BuildException {
        FileOutputStream output = null;

        try {
            output = new FileOutputStream(destFile);

            final PrintWriter writer =
                new PrintWriter(new OutputStreamWriter(output, "UTF-8"));

            final SvnChangeLogWriter serializer = new SvnChangeLogWriter();

            serializer.printChangeLog(writer, entrySet);
        } catch (final UnsupportedEncodingException uee) {
            getProject().log(uee.toString(), Project.MSG_ERR);
        } catch (final IOException ioe) {
            throw new BuildException(ioe.toString(), ioe);
        } finally {
            FileUtils.close(output);
        }
    }

    private static final String PATTERN = "yyyy-MM-dd";
    private static final SimpleDateFormat INPUT_DATE
        = new SimpleDateFormat(PATTERN);

    /**
     * Checks whether a given entry is before the given end revision,
     * using revision numbers or date information as appropriate.
     */
    private boolean isBeforeEndRevision(SvnEntry entry) {
        if (endRevision.startsWith("{")
            && endRevision.length() >= 2 + PATTERN.length() ) {
            try {
                Date endDate = 
                    INPUT_DATE.parse(endRevision.substring(1, 
                                                           PATTERN.length() 
                                                           + 1));
                return entry.getDate().before(endDate);
            } catch (ParseException e) {
            }
        } else {
            try {
                int endRev = Integer.parseInt(endRevision);
                int entryRev = Integer.parseInt(entry.getRevision());
                return endRev >= entryRev;
            } catch (NumberFormatException e) {
            } // end of try-catch
        }
        // failed to parse revision, use a save fallback
        return true;
    }
}

