/*
 * Copyright  2002-2004 The Apache Software Foundation
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
import org.apache.tools.ant.taskdefs.Execute;
import org.apache.tools.ant.taskdefs.AbstractCvsTask;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.FileSet;

/**
 * Examines the output of cvs log and group related changes together.
 *
 * It produces an XML output representing the list of changes.
 * <PRE>
 * <FONT color=#0000ff>&lt;!-- Root element --&gt;</FONT>
 * <FONT color=#6a5acd>&lt;!ELEMENT</FONT> changelog <FONT color=#ff00ff>(entry</FONT><FONT color=#ff00ff>+</FONT><FONT color=#ff00ff>)</FONT><FONT color=#6a5acd>&gt;</FONT>
 * <FONT color=#0000ff>&lt;!-- CVS Entry --&gt;</FONT>
 * <FONT color=#6a5acd>&lt;!ELEMENT</FONT> entry <FONT color=#ff00ff>(date,author,file</FONT><FONT color=#ff00ff>+</FONT><FONT color=#ff00ff>,msg)</FONT><FONT color=#6a5acd>&gt;</FONT>
 * <FONT color=#0000ff>&lt;!-- Date of cvs entry --&gt;</FONT>
 * <FONT color=#6a5acd>&lt;!ELEMENT</FONT> date <FONT color=#ff00ff>(#PCDATA)</FONT><FONT color=#6a5acd>&gt;</FONT>
 * <FONT color=#0000ff>&lt;!-- Author of change --&gt;</FONT>
 * <FONT color=#6a5acd>&lt;!ELEMENT</FONT> author <FONT color=#ff00ff>(#PCDATA)</FONT><FONT color=#6a5acd>&gt;</FONT>
 * <FONT color=#0000ff>&lt;!-- List of files affected --&gt;</FONT>
 * <FONT color=#6a5acd>&lt;!ELEMENT</FONT> msg <FONT color=#ff00ff>(#PCDATA)</FONT><FONT color=#6a5acd>&gt;</FONT>
 * <FONT color=#0000ff>&lt;!-- File changed --&gt;</FONT>
 * <FONT color=#6a5acd>&lt;!ELEMENT</FONT> file <FONT color=#ff00ff>(name,revision,prevrevision</FONT><FONT color=#ff00ff>?</FONT><FONT color=#ff00ff>)</FONT><FONT color=#6a5acd>&gt;</FONT>
 * <FONT color=#0000ff>&lt;!-- Name of the file --&gt;</FONT>
 * <FONT color=#6a5acd>&lt;!ELEMENT</FONT> name <FONT color=#ff00ff>(#PCDATA)</FONT><FONT color=#6a5acd>&gt;</FONT>
 * <FONT color=#0000ff>&lt;!-- Revision number --&gt;</FONT>
 * <FONT color=#6a5acd>&lt;!ELEMENT</FONT> revision <FONT color=#ff00ff>(#PCDATA)</FONT><FONT color=#6a5acd>&gt;</FONT>
 * <FONT color=#0000ff>&lt;!-- Previous revision number --&gt;</FONT>
 * <FONT color=#6a5acd>&lt;!ELEMENT</FONT> prevrevision <FONT color=#ff00ff>(#PCDATA)</FONT><FONT color=#6a5acd>&gt;</FONT>
 * </PRE>
 *
 * @author <a href="mailto:jeff.martin@synamic.co.uk">Jeff Martin</a>
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 * @since Ant 1.5
 * @ant.task name="cvschangelog" category="scm"
 */
public class ChangeLogTask extends AbstractCvsTask {
    /** User list */
    private File m_usersFile;

    /** User list */
    private Vector m_cvsUsers = new Vector();

    /** Input dir */
    private File m_dir;

    /** Output file */
    private File m_destfile;

    /** The earliest date at which to start processing entries.  */
    private Date m_start;

    /** The latest date at which to stop processing entries.  */
    private Date m_stop;

    /**
     * Filesets containing list of files against which the cvs log will be
     * performed. If empty then all files will in the working directory will
     * be checked.
     */
    private final Vector m_filesets = new Vector();


    /**
     * Set the base dir for cvs.
     *
     * @param dir The new dir value
     */
    public void setDir(final File dir) {
        m_dir = dir;
    }


    /**
     * Set the output file for the log.
     *
     * @param destfile The new destfile value
     */
    public void setDestfile(final File destfile) {
        m_destfile = destfile;
    }


    /**
     * Set a lookup list of user names & addresses
     *
     * @param usersFile The file containing the users info.
     */
    public void setUsersfile(final File usersFile) {
        m_usersFile = usersFile;
    }


    /**
     * Add a user to list changelog knows about.
     *
     * @param user the user
     */
    public void addUser(final CvsUser user) {
        m_cvsUsers.addElement(user);
    }


    /**
     * Set the date at which the changelog should start.
     *
     * @param start The date at which the changelog should start.
     */
    public void setStart(final Date start) {
        m_start = start;
    }


    /**
     * Set the date at which the changelog should stop.
     *
     * @param stop The date at which the changelog should stop.
     */
    public void setEnd(final Date stop) {
        m_stop = stop;
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
        m_filesets.addElement(fileSet);
    }


    /**
     * Execute task
     *
     * @exception BuildException if something goes wrong executing the
     *            cvs command
     */
    public void execute() throws BuildException {
        File savedDir = m_dir; // may be altered in validate

        try {

            validate();
            final Properties userList = new Properties();

            loadUserlist(userList);

            for (Enumeration e = m_cvsUsers.elements();
                e.hasMoreElements();) {
                final CvsUser user = (CvsUser) e.nextElement();

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
                myCvsVersion.setDest(m_dir);
                myCvsVersion.execute();
                if (myCvsVersion.supportsCvsLogWithSOption()) {
                    addCommandArgument("-S");
                }
            }
            if (null != m_start) {
                final SimpleDateFormat outputDate =
                    new SimpleDateFormat("yyyy-MM-dd");

                // We want something of the form: -d ">=YYYY-MM-dd"
                final String dateRange = ">=" + outputDate.format(m_start);

        // Supply '-d' as a separate argument - Bug# 14397
                addCommandArgument("-d");
                addCommandArgument(dateRange);
            }

            // Check if list of files to check has been specified
            if (!m_filesets.isEmpty()) {
                final Enumeration e = m_filesets.elements();

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

            setDest(m_dir);
            setExecuteStreamHandler(handler);
            super.execute();
            final String errors = handler.getErrors();

            if (null != errors) {
                log(errors, Project.MSG_ERR);
            }

            final CVSEntry[] entrySet = parser.getEntrySetAsArray();
            final CVSEntry[] filteredEntrySet = filterEntrySet(entrySet);

            replaceAuthorIdWithName(userList, filteredEntrySet);

            writeChangeLog(filteredEntrySet);

        } finally {
            m_dir = savedDir;
        }
    }

    /**
     * Validate the parameters specified for task.
     *
     * @throws BuildException if fails validation checks
     */
    private void validate()
         throws BuildException {
        if (null == m_dir) {
            m_dir = getProject().getBaseDir();
        }
        if (null == m_destfile) {
            final String message = "Destfile must be set.";

            throw new BuildException(message);
        }
        if (!m_dir.exists()) {
            final String message = "Cannot find base dir "
                 + m_dir.getAbsolutePath();

            throw new BuildException(message);
        }
        if (null != m_usersFile && !m_usersFile.exists()) {
            final String message = "Cannot find user lookup list "
                 + m_usersFile.getAbsolutePath();

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
        if (null != m_usersFile) {
            try {
                userList.load(new FileInputStream(m_usersFile));
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

            if (null != m_start && m_start.after(date)) {
                //Skip dates that are too early
                continue;
            }
            if (null != m_stop && m_stop.before(date)) {
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
            output = new FileOutputStream(m_destfile);

            final PrintWriter writer =
                new PrintWriter(new OutputStreamWriter(output, "UTF-8"));

            final ChangeLogWriter serializer = new ChangeLogWriter();

            serializer.printChangeLog(writer, entrySet);
        } catch (final UnsupportedEncodingException uee) {
            getProject().log(uee.toString(), Project.MSG_ERR);
        } catch (final IOException ioe) {
            throw new BuildException(ioe.toString(), ioe);
        } finally {
            if (null != output) {
                try {
                    output.close();
                } catch (final IOException ioe) {
                }
            }
        }
    }
}

