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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;

/**
 * A class used to parse the output of the svn log command.
 *
 * @version $Revision$ $Date$
 */
class SvnChangeLogParser extends LineOrientedOutputStream {

    private final static int GET_ENTRY_LINE = 0;
    private final static int GET_REVISION_LINE = 1;
    private final static int GET_PATHS = 2;
    private final static int GET_MESSAGE = 3;

    private String message = "";
    private Date date = null;
    private String author = null;
    private String revision = null;
    private ArrayList paths = new ArrayList();

    /** input format for dates read in from cvs log */
    private static final String PATTERN = "yyyy-MM-dd HH:mm:ss";
    private static final SimpleDateFormat INPUT_DATE
        = new SimpleDateFormat(PATTERN);

    private final ArrayList entries = new ArrayList();
    private int status = GET_ENTRY_LINE;

    /**
     * Get a list of rcs entries as an array.
     *
     * @return a list of rcs entries as an array
     */
    public SvnEntry[] getEntrySetAsArray() {
        return (SvnEntry[]) entries.toArray(new SvnEntry[entries.size()]);
    }

    /**
     * Receive notification about the process writing
     * to standard output.
     * @param line the line to process
     */
    public void processLine(final String line) {
        switch(status) {
            case GET_ENTRY_LINE:
                // make sure attributes are reset when
                // working on a 'new' file.
                reset();
                processEntryStart(line);
                break;
            case GET_REVISION_LINE:
                processRevision(line);
                break;

            case GET_MESSAGE:
                processMessage(line);
                break;

            case GET_PATHS:
                processPath(line);
                break;

            default:
                // Do nothing
                break;
        }
    }

    /**
     * Process a line while in "GET_MESSAGE" state.
     *
     * @param line the line
     */
    private void processMessage(final String line) {
        final String lineSeparator = System.getProperty("line.separator");
        if (line.equals("------------------------------------------------------------------------")) {
            //We have ended changelog for that particular revision
            //so we can save it
            final int end
                = message.length() - lineSeparator.length();
            message = message.substring(0, end);
            saveEntry();
            status = GET_REVISION_LINE;
        } else {
            message += line + lineSeparator;
        }
    }

    /**
     * Process a line while in "GET_ENTRY_LINE" state.
     *
     * @param line the line to process
     */
    private void processEntryStart(final String line) {
        if (line.equals("------------------------------------------------------------------------")) {
            status = GET_REVISION_LINE;
        }
    }

    /**
     * Process a line while in "REVISION" state.
     *
     * @param line the line to process
     */
    private void processRevision(final String line) {
        int index = line.indexOf(" |");
        if (line.startsWith("r") 
            && (line.endsWith("lines") || line.endsWith("line"))
            && index > -1) {
            revision = line.substring(1, index);
            int end = line.indexOf(" |", index + 1);
            author = line.substring(index + 3, end);
            String d = line.substring(end + 3, end + 3 + PATTERN.length());
            date = parseDate(d);
            status = GET_PATHS;
        }
    }

    /**
     * Process a line while in "GET_PATHS" state.
     *
     * @param line the line to process
     */
    private void processPath(final String line) {
        if (line.startsWith("Changed paths:")) {
            // ignore
        } else if (line.equals("")) {
            status = GET_MESSAGE;
        } else if (line.length() > 5) {
            paths.add(new SvnEntry.Path(line.substring(5), line.charAt(3)));
        }
    }

    /**
     * Utility method that saves the current entry.
     */
    private void saveEntry() {
        SvnEntry entry = new SvnEntry(date, revision, author, message,
                                      paths);
        entries.add(entry);
        reset();
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
            return null;
        }
    }

    /**
     * Reset all internal attributes except status.
     */
    public void reset() {
        this.date = null;
        this.author = null;
        this.message = "";
        this.revision = null;
        this.paths.clear();
    }
}
