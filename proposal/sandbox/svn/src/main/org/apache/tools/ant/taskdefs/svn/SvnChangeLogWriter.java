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

import java.io.PrintWriter;
import java.text.SimpleDateFormat;

/**
 * Class used to generate an XML changelog.
 */
public class SvnChangeLogWriter {
    /** output format for dates written to xml file */
    private static final SimpleDateFormat OUTPUT_DATE
        = new SimpleDateFormat("yyyy-MM-dd");
    /** output format for times written to xml file */
    private static final SimpleDateFormat OUTPUT_TIME
        = new SimpleDateFormat("HH:mm");

    /**
     * Print out the specified entries.
     *
     * @param output writer to which to send output.
     * @param entries the entries to be written.
     */
    public void printChangeLog(final PrintWriter output,
                               final SvnEntry[] entries) {
        output.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        output.println("<changelog>");
        for (int i = 0; i < entries.length; i++) {
            final SvnEntry entry = entries[i];

            printEntry(output, entry);
        }
        output.println("</changelog>");
        output.flush();
        output.close();
    }


    /**
     * Print out an individual entry in changelog.
     *
     * @param entry the entry to print
     * @param output writer to which to send output.
     */
    private void printEntry(final PrintWriter output, final SvnEntry entry) {
        output.println("\t<entry>");
        output.println("\t\t<date>" + OUTPUT_DATE.format(entry.getDate())
            + "</date>");
        output.println("\t\t<time>" + OUTPUT_TIME.format(entry.getDate())
            + "</time>");
        output.println("\t\t<author><![CDATA[" + entry.getAuthor()
            + "]]></author>");
        output.println("\t\t<revision><![CDATA[" + entry.getRevision()
            + "]]></revision>");

        String[] paths = entry.getPaths();
        for (int i = 0; i < paths.length; i++) {
            output.println("\t\t<file>");
            output.println("\t\t\t<name><![CDATA[" + paths[i] + "]]></name>");
            output.println("\t\t</file>");
        }
        output.println("\t\t<msg><![CDATA[" + entry.getMessage() + "]]></msg>");
        output.println("\t</entry>");
    }
}

