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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Locale;
import java.util.TimeZone;

/**
 * A class used to parse the output of the CVS log command.
 *
 */
class ChangeLogParser {
    //private static final int GET_ENTRY = 0;
    private static final int GET_FILE = 1;
    private static final int GET_DATE = 2;
    private static final int GET_COMMENT = 3;
    private static final int GET_REVISION = 4;
    private static final int GET_PREVIOUS_REV = 5;

// FIXME formatters are not thread-safe

    /** input format for dates read in from cvs log */
    private static final SimpleDateFormat INPUT_DATE
        = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.US);
    /**
     * New formatter used to parse CVS date/timestamp.
     */
    private static final SimpleDateFormat CVS1129_INPUT_DATE =
        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.US);

    static {
        TimeZone utc = TimeZone.getTimeZone("UTC");
        INPUT_DATE.setTimeZone(utc);
        CVS1129_INPUT_DATE.setTimeZone(utc);
    }

    //The following is data used while processing stdout of CVS command
    private String file;
    private String date;
    private String author;
    private String comment;
    private String revision;
    private String previousRevision;

    private int status = GET_FILE;

    /** rcs entries */
    private final Hashtable entries = new Hashtable();

    /**
     * Get a list of rcs entries as an array.
     *
     * @return a list of rcs entries as an array
     */
    public CVSEntry[] getEntrySetAsArray() {
        final CVSEntry[] array = new CVSEntry[ entries.size() ];
        int i = 0;
        for (Enumeration e = entries.elements(); e.hasMoreElements();) {
            array[i++] = (CVSEntry) e.nextElement();
        }
        return array;
    }

    /**
     * Receive notification about the process writing
     * to standard output.
     * @param line the line to process
     */
    public void stdout(final String line) {
        switch(status) {
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

            default:
                // Do nothing
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
        if (line.equals(
                "=============================================================================")) {
            //We have ended changelog for that particular file
            //so we can save it
            final int end
                = comment.length() - lineSeparator.length(); //was -1
            comment = comment.substring(0, end);
            saveEntry();
            status = GET_FILE;
        } else if (line.equals("----------------------------")) {
            final int end
                = comment.length() - lineSeparator.length(); //was -1
            comment = comment.substring(0, end);
            status = GET_PREVIOUS_REV;
        } else {
            comment += line + lineSeparator;
        }
    }

    /**
     * Process a line while in "GET_FILE" state.
     *
     * @param line the line to process
     */
    private void processFile(final String line) {
        if (line.startsWith("Working file:")) {
            // CheckStyle:MagicNumber OFF
            file = line.substring(14, line.length());
            // CheckStyle:MagicNumber ON
            status = GET_REVISION;
        }
    }

    /**
     * Process a line while in "REVISION" state.
     *
     * @param line the line to process
     */
    private void processRevision(final String line) {
        if (line.startsWith("revision")) {
            // CheckStyle:MagicNumber OFF
            revision = line.substring(9);
            // CheckStyle:MagicNumber ON
            status = GET_DATE;
        } else if (line.startsWith("======")) {
            //There were no revisions in this changelog
            //entry so lets move onto next file
            status = GET_FILE;
        }
    }

    /**
     * Process a line while in "DATE" state.
     *
     * @param line the line to process
     */
    private void processDate(final String line) {
        if (line.startsWith("date:")) {
            // The date format is using a - format since 1.12.9 so we have:
            // 1.12.9-: 'date: YYYY/mm/dd HH:mm:ss;  author: name;'
            // 1.12.9+: 'date: YYYY-mm-dd HH:mm:ss Z;  author: name'
            int endOfDateIndex = line.indexOf(';');
            date = line.substring("date: ".length(), endOfDateIndex);

            int startOfAuthorIndex = line.indexOf("author: ", endOfDateIndex + 1);
            int endOfAuthorIndex = line.indexOf(';', startOfAuthorIndex + 1);
            author = line.substring("author: ".length() + startOfAuthorIndex, endOfAuthorIndex);

            status = GET_COMMENT;

            //Reset comment to empty here as we can accumulate multiple lines
            //in the processComment method
            comment = "";
        }
    }

    /**
     * Process a line while in "GET_PREVIOUS_REVISION" state.
     *
     * @param line the line to process
     */
    private void processGetPreviousRevision(final String line) {
        if (!line.startsWith("revision ")) {
            throw new IllegalStateException("Unexpected line from CVS: "
                + line);
        }
        previousRevision = line.substring("revision ".length());

        saveEntry();

        revision = previousRevision;
        status = GET_DATE;
    }

    /**
     * Utility method that saves the current entry.
     */
    private void saveEntry() {
        final String entryKey = date + author + comment;
        CVSEntry entry;
        if (!entries.containsKey(entryKey)) {
            Date dateObject = parseDate(date);
            entry = new CVSEntry(dateObject, author, comment);
            entries.put(entryKey, entry);
        } else {
            entry = (CVSEntry) entries.get(entryKey);
        }

        entry.addFile(file, revision, previousRevision);
    }

    /**
     * Parse date out from expected format.
     *
     * @param date the string holding date
     * @return the date object or null if unknown date format
     */
    private Date parseDate(final String date) {
        try {
            return INPUT_DATE.parse(date);
        } catch (ParseException e) {
            try {
                return CVS1129_INPUT_DATE.parse(date);
            } catch (ParseException e2) {
                throw new IllegalStateException("Invalid date format: " + date);
            }
        }
    }

    /**
     * Reset all internal attributes except status.
     */
    public void reset() {
        this.file = null;
        this.date = null;
        this.author = null;
        this.comment = null;
        this.revision = null;
        this.previousRevision = null;
    }
}
