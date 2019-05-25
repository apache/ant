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

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.util.DOMElementWriter;
import org.apache.tools.ant.util.DOMUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Class used to generate an XML changelog.
 *
 */
public class ChangeLogWriter {
    /** output format for dates written to xml file */
    private final SimpleDateFormat outputDate
        = new SimpleDateFormat("yyyy-MM-dd");

    /** output format for times written to xml file */
    private SimpleDateFormat outputTime
        = new SimpleDateFormat("HH:mm");

    /** stateless helper for writing the XML document */
    private static final DOMElementWriter DOM_WRITER = new DOMElementWriter();

    public ChangeLogWriter() {
        TimeZone utc = TimeZone.getTimeZone("UTC");
        outputDate.setTimeZone(utc);
        outputTime.setTimeZone(utc);
    }

    /**
     * Print out the specified entries.
     *
     * @param output writer to which to send output.
     * @param entries the entries to be written.
     */
    public void printChangeLog(final PrintWriter output,
                               final CVSEntry[] entries) {
        try {
            output.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            Document doc = DOMUtils.newDocument();
            Element root = doc.createElement("changelog");
            DOM_WRITER.openElement(root, output, 0, "\t");
            output.println();
            for (final CVSEntry entry : entries) {
                printEntry(doc, output, entry);
            }
            DOM_WRITER.closeElement(root, output, 0, "\t", true);
            output.flush();
            output.close();
        } catch (IOException e) {
            throw new BuildException(e);
        }
    }

    /**
     * Print out an individual entry in changelog.
     *
     * @param doc Document used to create elements.
     * @param entry the entry to print
     * @param output writer to which to send output.
     */
    private void printEntry(Document doc, final PrintWriter output,
                            final CVSEntry entry) throws IOException {
        Element ent = doc.createElement("entry");
        DOMUtils.appendTextElement(ent, "date",
                                   outputDate.format(entry.getDate()));
        DOMUtils.appendTextElement(ent, "time",
                                   outputTime.format(entry.getDate()));
        DOMUtils.appendCDATAElement(ent, "author", entry.getAuthor());

        for (RCSFile file : entry.getFiles()) {
            Element f = DOMUtils.createChildElement(ent, "file");
            DOMUtils.appendCDATAElement(f, "name", file.getName());
            DOMUtils.appendTextElement(f, "revision", file.getRevision());

            final String previousRevision = file.getPreviousRevision();
            if (previousRevision != null) {
                DOMUtils.appendTextElement(f, "prevrevision",
                                           previousRevision);
            }
        }
        DOMUtils.appendCDATAElement(ent, "msg", entry.getComment());
        DOM_WRITER.write(ent, output, 1, "\t");
    }
}
