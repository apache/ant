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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.StringTokenizer;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.util.DOMElementWriter;
import org.apache.tools.ant.util.DOMUtils;
import org.apache.tools.ant.util.FileUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Parses the output of a svn diff command and/or writes an XML report
 * based on such a diff output.
 *
 * It produces an XML output representing the list of changes.
 */
final class SvnDiffHandler {

    /**
     * Token to identify the word file in the rdiff log
     */
    private static final String INDEX = "Index: ";
    /**
     * Token to identify a deleted file based on the Index line.
     */
    private static final String DELETED = " (deleted)";

    /**
     * Token to identify added files based on the diff line.
     */
    private static final String IS_NEW = "\t(revision 0)";

    /**
     * Token that starts diff line of old revision.
     */
    private static final String DASHES = "--- ";

    /** stateless helper for writing the XML document */
    private static final DOMElementWriter DOM_WRITER = new DOMElementWriter();

    /**
     * Parse the tmpFile and return and array of entries to be written
     * in the output.
     *
     * @param tmpFile the File containing the output of the svn rdiff command
     * @return the entries in the output
     * @exception BuildException if an error occurs
     */
    static SvnEntry.Path[] parseDiff(File tmpFile) throws BuildException {
        // parse the output of the command
        BufferedReader reader = null;

        try {
            reader = new BufferedReader(new FileReader(tmpFile));
            ArrayList entries = new ArrayList();

            String line = reader.readLine();
            String name = null;
            String currDiffLine = null;
            boolean deleted = false;
            boolean added = false;

            while (null != line) {
                if (line.length() > INDEX.length()) {
                    if (line.startsWith(INDEX)) {
                        if (name != null) {
                            SvnEntry.Path p =
                                new SvnEntry.Path(name, 
                                                  deleted 
                                                  ? SvnEntry.Path.DELETED 
                                                  : (added 
                                                     ? SvnEntry.Path.ADDED 
                                                     : SvnEntry.Path.MODIFIED)
                                                  );
                            entries.add(p);
                            deleted = added = false;
                        }

                        name = line.substring(INDEX.length());
                        if (line.endsWith(DELETED)) {
                            name = name.substring(0, name.length() 
                                                  - DELETED.length());
                            deleted = true;
                        }

                        currDiffLine = DASHES + name;
                    } else if (currDiffLine != null 
                               && line.startsWith(currDiffLine)
                               && line.endsWith(IS_NEW)) {
                        added = true;
                    }
                }
                line = reader.readLine();
            }
            if (name != null) {
                SvnEntry.Path p = new SvnEntry.Path(name, 
                                                    deleted 
                                                    ? SvnEntry.Path.DELETED 
                                                    : (added 
                                                       ? SvnEntry.Path.ADDED 
                                                       : SvnEntry.Path.MODIFIED)
                                                    );
                entries.add(p);
            }

            SvnEntry.Path[] array = (SvnEntry.Path[])
                entries.toArray(new SvnEntry.Path[entries.size()]);
            return array;
        } catch (IOException e) {
            throw new BuildException("Error in parsing", e);
        } finally {
            FileUtils.close(reader);
        }
    }

    /**
     * Write the diff log.
     *
     * @param entries a <code>SvnRevisionEntry[]</code> value
     * @exception BuildException if an error occurs
     */
    static void writeDiff(File destFile, SvnEntry.Path[] entries,
                          String rootElementName,
                          String tag1Name, String tag1Value,
                          String tag2Name, String tag2Value,
                          String svnURL) throws BuildException {
        FileOutputStream output = null;
        try {
            output = new FileOutputStream(destFile);
            PrintWriter writer = new PrintWriter(
                                     new OutputStreamWriter(output, "UTF-8"));
            writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            Document doc = DOMUtils.newDocument();
            Element root = doc.createElement(rootElementName);
            if (tag1Name != null && tag1Value != null) {
                root.setAttribute(tag1Name, tag1Value);
            }
            if (tag2Name != null && tag2Value != null) {
                root.setAttribute(tag2Name, tag2Value);
            }

            if (svnURL != null) {
                root.setAttribute("svnurl", svnURL);
            }
            DOM_WRITER.openElement(root, writer, 0, "\t");
            writer.println();
            for (int i = 0, c = entries.length; i < c; i++) {
                writeRevisionEntry(doc, writer, entries[i]);
            }
            DOM_WRITER.closeElement(root, writer, 0, "\t", true);
            writer.flush();
            writer.close();
        } catch (UnsupportedEncodingException uee) {
            throw new BuildException(uee);
        } catch (IOException ioe) {
            throw new BuildException(ioe.toString(), ioe);
        } finally {
            FileUtils.close(output);
        }
    }

    /**
     * Write a single entry to the given writer.
     *
     * @param doc Document used to create elements.
     * @param writer a <code>PrintWriter</code> value
     * @param entry a <code>SvnRevisionEntry</code> value
     */
    private static void writeRevisionEntry(Document doc,
                                           PrintWriter writer,
                                           SvnEntry.Path entry)
        throws IOException {
        Element e = doc.createElement("path");
        DOMUtils.appendCDATAElement(e, "name", entry.getName());
        DOMUtils.appendTextElement(e, "action", entry.getActionDescription());
        DOM_WRITER.write(e, writer, 1, "\t");
    }

}
