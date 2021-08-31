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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TimeZone;

import org.apache.tools.ant.taskdefs.AbstractCvsTask;
import org.apache.tools.ant.taskdefs.AbstractCvsTask.Module;

/**
 * A class used to parse the output of the CVS log command.
 *
 */
class ChangeLogParser {
    private static final int GET_FILE = 1;
    private static final int GET_DATE = 2;
    private static final int GET_COMMENT = 3;
    private static final int GET_REVISION = 4;
    private static final int GET_PREVIOUS_REV = 5;

    /** input format for dates read in from cvs log */
    private final SimpleDateFormat inputDate
        = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.US);
    /**
     * New formatter used to parse CVS date/timestamp.
     */
    private final SimpleDateFormat cvs1129InputDate =
        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.US);

    //The following is data used while processing stdout of CVS command
    private String file;
    private String date;
    private String author;
    private String comment;
    private String revision;
    private String previousRevision;

    private int status = GET_FILE;

    /** rcs entries */
    private final Map<String, CVSEntry> entries = new Hashtable<>();

    private final boolean remote;
    private final String[] moduleNames;
    private final int[] moduleNameLengths;

    public ChangeLogParser() {
        this(false, "", Collections.emptyList());
    }

    public ChangeLogParser(boolean remote, String packageName, List<AbstractCvsTask.Module> modules) {
        this.remote = remote;

        List<String> names = new ArrayList<>();
        if (packageName != null) {
            for (StringTokenizer tok = new StringTokenizer(packageName);
                 tok.hasMoreTokens();) {
                names.add(tok.nextToken());
            }
        }
        modules.stream().map(Module::getName).forEach(names::add);

        moduleNames = names.toArray(new String[0]);
        moduleNameLengths = new int[moduleNames.length];
        for (int i = 0; i < moduleNames.length; i++) {
            moduleNameLengths[i] = moduleNames[i].length();
        }

        TimeZone utc = TimeZone.getTimeZone("UTC");
        inputDate.setTimeZone(utc);
        cvs1129InputDate.setTimeZone(utc);
    }

    /**
     * Get a list of rcs entries as an array.
     *
     * @return a list of rcs entries as an array
     */
    public CVSEntry[] getEntrySetAsArray() {
        return entries.values().toArray(new CVSEntry[0]);
    }

    /**
     * Receive notification about the process writing
     * to standard output.
     * @param line the line to process
     */
    public void stdout(final String line) {
        switch (status) {
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
        if ("============================================================================="
            .equals(line)) {
            //We have ended changelog for that particular file
            //so we can save it
            final int end = comment.length() - System.lineSeparator().length(); //was -1
            comment = comment.substring(0, end);
            saveEntry();
            status = GET_FILE;
        } else if ("----------------------------".equals(line)) {
            final int end = comment.length() - System.lineSeparator().length(); //was -1
            comment = comment.substring(0, end);
            status = GET_PREVIOUS_REV;
        } else {
            comment += line + System.lineSeparator();
        }
    }

    /**
     * Process a line while in "GET_FILE" state.
     *
     * @param line the line to process
     */
    private void processFile(final String line) {
        if (!remote && line.startsWith("Working file:")) {
            // CheckStyle:MagicNumber OFF
            file = line.substring(14);
            // CheckStyle:MagicNumber ON
            status = GET_REVISION;
        } else if (remote && line.startsWith("RCS file:")) {
            // exclude the part of the RCS filename up to and
            // including the module name (and the path separator)
            int startOfFileName = 0;
            for (int i = 0; i < moduleNames.length; i++) {
                int index = line.indexOf(moduleNames[i]);
                if (index >= 0) {
                    startOfFileName = index + moduleNameLengths[i] + 1;
                    break;
                }
            }
            int endOfFileName = line.indexOf(",v");
            if (endOfFileName == -1) {
                file = line.substring(startOfFileName);
            } else {
                file = line.substring(startOfFileName, endOfFileName);
            }
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
        entries.computeIfAbsent(date + author + comment,
                k -> new CVSEntry(parseDate(date), author, comment)).addFile(file, revision, previousRevision);
    }

    /**
     * Parse date out from expected format.
     *
     * @param date the string holding date
     * @return the date object or null if unknown date format
     */
    private Date parseDate(final String date) {
        try {
            return inputDate.parse(date);
        } catch (ParseException e) {
            try {
                return cvs1129InputDate.parse(date);
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
