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

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import javax.xml.parsers.DocumentBuilder;

import org.apache.tools.ant.util.DOMElementWriter;
import org.apache.tools.ant.util.JAXPUtils;

import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

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
    /** stateless helper for writing the XML document */
    private static final DOMElementWriter DOM_WRITER = new DOMElementWriter();

    /**
     * Print out the specified entries.
     *
     * @param output writer to which to send output.
     * @param entries the entries to be written.
     */
    public void printChangeLog(final PrintWriter output,
                               final SvnEntry[] entries) throws IOException {
        output.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        Document doc = JAXPUtils.getDocumentBuilder().newDocument();
        Element root = doc.createElement("changelog");
        DOM_WRITER.openElement(root, output, 0, "\t");
        output.println();
        for (int i = 0; i < entries.length; i++) {
            final SvnEntry entry = entries[i];

            printEntry(output, entry, root);
        }
        DOM_WRITER.closeElement(root, output, 0, "\t", entries.length > 0);
        output.flush();
        output.close();
    }


    /**
     * Print out an individual entry in changelog.
     *
     * @param entry the entry to print
     * @param output writer to which to send output.
     */
    private void printEntry(final PrintWriter output, final SvnEntry entry,
                            final Element element) throws IOException {
        Document doc = element.getOwnerDocument();

        Element ent = doc.createElement("entry");
        appendTextElement(ent, "date", OUTPUT_DATE.format(entry.getDate()));
        appendTextElement(ent, "time", OUTPUT_TIME.format(entry.getDate()));
        appendCDATAElement(ent, "author", entry.getAuthor());
        appendTextElement(ent, "revision", entry.getRevision());

        SvnEntry.Path[] paths = entry.getPaths();
        for (int i = 0; i < paths.length; i++) {
            Element path = doc.createElement("path");
            ent.appendChild(path);
            appendCDATAElement(path, "name", paths[i].getName());
            appendTextElement(path, "action", paths[i].getActionDescription());
        }
        appendCDATAElement(ent, "message", entry.getMessage());
        DOM_WRITER.write(ent, output, 1, "\t");
    }

    /**
     * Creates a named element with nested text as child of the given element.
     *
     * @param parent the parent element
     * @param name name of the child element
     * @param content the content of the nested text
     */
    private static void appendTextElement(Element parent, String name,
                                          String content) {
        Document doc = parent.getOwnerDocument();
        Element e = doc.createElement(name);
        parent.appendChild(e);
        Text t = doc.createTextNode(content);
        e.appendChild(t);
    }

    /**
     * Creates a named element with a nested CDATA section as child of
     * the given element.
     *
     * @param parent the parent element
     * @param name name of the child element
     * @param content the content of the nested text
     */
    private static void appendCDATAElement(Element parent, String name,
                                           String content) {
        Document doc = parent.getOwnerDocument();
        Element e = doc.createElement(name);
        parent.appendChild(e);
        CDATASection c  = doc.createCDATASection(content);
        e.appendChild(c);
    }
}

