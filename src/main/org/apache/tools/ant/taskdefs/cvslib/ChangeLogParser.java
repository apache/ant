/*
 * Copyright  2002-2004 Apache Software Foundation
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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.TimeZone;

/**
 * A class used to parse the output of the CVS log command.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
class ChangeLogParser {
    //private static final int GET_ENTRY = 0;
    private static final int GET_FILE = 1;
    private static final int GET_DATE = 2;
    private static final int GET_COMMENT = 3;
    private static final int GET_REVISION = 4;
    private static final int GET_PREVIOUS_REV = 5;

    /** input format for dates read in from cvs log */
    private static final SimpleDateFormat c_inputDate
        = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

    static {
        TimeZone utc = TimeZone.getTimeZone("UTC");
        c_inputDate.setTimeZone(utc);
    }

    //The following is data used while processing stdout of CVS command
    private String m_file;
    private String m_date;
    private String m_author;
    private String m_comment;
    private String m_revision;
    private String m_previousRevision;

    private int m_status = GET_FILE;

    /** rcs entries */
    private final Hashtable m_entries = new Hashtable();

    /**
     * Get a list of rcs entries as an array.
     *
     * @return a list of rcs entries as an array
     */
    CVSEntry[] getEntrySetAsArray() {
        final CVSEntry[] array = new CVSEntry[ m_entries.size() ];
        Enumeration e = m_entries.elements();
        int i = 0;
        while (e.hasMoreElements()) {
            array[i++] = (CVSEntry) e.nextElement();
        }
        return array;
    }

    /**
     * Receive notification about the process writing
     * to standard output.
     */
    public void stdout(final String line) {
        switch(m_status) {
            case GET_FILE:
                // make sure attributes are reset when
                // working on a 'new' file.
                reset();
                processFile(line);
                break;
            case GET_REVISION:
                processRevision(line);
                break;

            case GET_DATE:
                processDate(line);
                break;

            case GET_COMMENT:
                processComment(line);
                break;

            case GET_PREVIOUS_REV:
                processGetPreviousRevision(line);
                break;
        }
    }

    /**
     * Process a line while in "GET_COMMENT" state.
     *
     * @param line the line
     */
    private void processComment(final String line) {
        final String lineSeparator = System.getProperty("line.separator");
        if (line.startsWith("======")) {
            //We have ended changelog for that particular file
            //so we can save it
            final int end
                = m_comment.length() - lineSeparator.length(); //was -1
            m_comment = m_comment.substring(0, end);
            saveEntry();
            m_status = GET_FILE;
        } else if (line.startsWith("----------------------------")) {
            final int end
                = m_comment.length() - lineSeparator.length(); //was -1
            m_comment = m_comment.substring(0, end);
            m_status = GET_PREVIOUS_REV;
        } else {
            m_comment += line + lineSeparator;
        }
    }

    /**
     * Process a line while in "GET_FILE" state.
     *
     * @param line the line
     */
    private void processFile(final String line) {
        if (line.startsWith("Working file:")) {
            m_file = line.substring(14, line.length());
            m_status = GET_REVISION;
        }
    }

    /**
     * Process a line while in "REVISION" state.
     *
     * @param line the line
     */
    private void processRevision(final String line) {
        if (line.startsWith("revision")) {
            m_revision = line.substring(9);
            m_status = GET_DATE;
        } else if (line.startsWith("======")) {
            //There was no revisions in this changelog
            //entry so lets move unto next file
            m_status = GET_FILE;
        }
    }

    /**
     * Process a line while in "DATE" state.
     *
     * @param line the line
     */
    private void processDate(final String line) {
        if (line.startsWith("date:")) {
            m_date = line.substring(6, 25);
            String lineData = line.substring(line.indexOf(";") + 1);
            m_author = lineData.substring(10, lineData.indexOf(";"));

            m_status = GET_COMMENT;

            //Reset comment to empty here as we can accumulate multiple lines
            //in the processComment method
            m_comment = "";
        }
    }

    /**
     * Process a line while in "GET_PREVIOUS_REVISION" state.
     *
     * @param line the line
     */
    private void processGetPreviousRevision(final String line) {
        if (!line.startsWith("revision")) {
            throw new IllegalStateException("Unexpected line from CVS: "
                + line);
        }
        m_previousRevision = line.substring(9);

        saveEntry();

        m_revision = m_previousRevision;
        m_status = GET_DATE;
    }

    /**
     * Utility method that saves the current entry.
     */
    private void saveEntry() {
        final String entryKey = m_date + m_author + m_comment;
        CVSEntry entry;
        if (!m_entries.containsKey(entryKey)) {
            entry = new CVSEntry(parseDate(m_date), m_author, m_comment);
            m_entries.put(entryKey, entry);
        } else {
            entry = (CVSEntry) m_entries.get(entryKey);
        }

        entry.addFile(m_file, m_revision, m_previousRevision);
    }

    /**
     * Parse date out from expected format.
     *
     * @param date the string holding dat
     * @return the date object or null if unknown date format
     */
    private Date parseDate(final String date) {
        try {
            return c_inputDate.parse(date);
        } catch (ParseException e) {
            //final String message = REZ.getString( "changelog.bat-date.error", date );
            //getContext().error( message );
            return null;
        }
    }

    /**
     * reset all internal attributes except status.
     */
    private void reset() {
        m_file = null;
        m_date = null;
        m_author = null;
        m_comment = null;
        m_revision = null;
        m_previousRevision = null;
    }

}
