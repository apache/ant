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
import org.apache.tools.ant.util.FileUtils;

/**
 * Examines the output of svn diff between two revisions.
 *
 * It produces an XML output representing the list of changes.
 * <PRE>
 * &lt;!-- Root element --&gt;
 * &lt;!ELEMENT revisiondiff ( paths? ) &gt;
 * &lt;!-- Start revision of the report --&gt;
 * &lt;!ATTLIST revisiondiff startRevision NMTOKEN #IMPLIED &gt;
 * &lt;!-- End revision of the report --&gt;
 * &lt;!ATTLIST revisiondiff endRevision NMTOKEN #IMPLIED &gt;
 * &lt;!-- Start date of the report --&gt;
 *
 * &lt;!-- Path added, changed or removed --&gt;
 * &lt;!ELEMENT path ( name,action ) &gt;
 * &lt;!-- Name of the file --&gt;
 * &lt;!ELEMENT name ( #PCDATA ) &gt;
 * &lt;!ELEMENT action (added|modified|deleted)&gt;
 * </PRE>
 *
 * @ant.task name="svnrevisiondiff"
 */
public class SvnRevisionDiff extends AbstractSvnTask {

    /**
     * Used to create the temp file for svn log
     */
    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();

    /**
     * Token to identify the word file in the rdiff log
     */
    static final String INDEX = "Index: ";
    /**
     * Token to identify a deleted file based on the Index line.
     */
    static final String DELETED = " (deleted)";

    /**
     * Token to identify added files based on the diff line.
     */
    static final String IS_NEW = "\t(revision 0)";

    /**
     * Token that starts diff line of old revision.
     */
    static final String DASHES = "--- ";

    /**
     * The earliest revision from which diffs are to be included in the report.
     */
    private String mystartRevision;

    /**
     * The latest revision from which diffs are to be included in the report.
     */
    private String myendRevision;

    /**
     * The file in which to write the diff report.
     */
    private File mydestfile;

    /**
     * Set the start revision.
     *
     * @param s the start revision.
     */
    public void setStart(String s) {
        mystartRevision = s;
    }

    /**
     * Set the end revision.
     *
     * @param s the end revision.
     */
    public void setEnd(String s) {
        myendRevision = s;
    }

    /**
     * Set the output file for the diff.
     *
     * @param f the output file for the diff.
     */
    public void setDestFile(File f) {
        mydestfile = f;
    }

    /**
     * Execute task.
     *
     * @exception BuildException if an error occurs
     */
    public void execute() throws BuildException {
        // validate the input parameters
        validate();

        // build the rdiff command
        setSubCommand("diff");
        setRevision(mystartRevision + ":" + myendRevision);
        addSubCommandArgument("--no-diff-deleted");

        File tmpFile = null;
        try {
            tmpFile = 
                FILE_UTILS.createTempFile("svnrevisiondiff", ".log", null);
            tmpFile.deleteOnExit();
            setOutput(tmpFile);

            // run the svn command
            super.execute();

            // parse the rdiff
            SvnEntry.Path[] entries = parseDiff(tmpFile);

            // write the revision diff
            writeRevisionDiff(entries);

        } finally {
            if (tmpFile != null) {
                tmpFile.delete();
            }
        }
    }

    /**
     * Parse the tmpFile and return and array of SvnRevisionEntry to be
     * written in the output.
     *
     * @param tmpFile the File containing the output of the svn rdiff command
     * @return the entries in the output
     * @exception BuildException if an error occurs
     */
    private SvnEntry.Path[] parseDiff(File tmpFile) throws BuildException {
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
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    log(e.toString(), Project.MSG_ERR);
                }
            }
        }
    }

    /**
     * Write the rdiff log.
     *
     * @param entries a <code>SvnRevisionEntry[]</code> value
     * @exception BuildException if an error occurs
     */
    private void writeRevisionDiff(SvnEntry.Path[] entries) throws BuildException {
        FileOutputStream output = null;
        try {
            output = new FileOutputStream(mydestfile);
            PrintWriter writer = new PrintWriter(
                                     new OutputStreamWriter(output, "UTF-8"));
            writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            writer.print("<revisiondiff ");
            if (mystartRevision != null) {
                writer.print("start=\"" + mystartRevision + "\" ");
            }
            if (myendRevision != null) {
                writer.print("end=\"" + myendRevision + "\" ");
            }

            if (getSvnURL() != null) {
                writer.print("svnurl=\"" + getSvnURL() + "\" ");
            }

            writer.println(">");
            for (int i = 0, c = entries.length; i < c; i++) {
                writeRevisionEntry(writer, entries[i]);
            }
            writer.println("</revisiondiff>");
            writer.flush();
            writer.close();
        } catch (UnsupportedEncodingException uee) {
            log(uee.toString(), Project.MSG_ERR);
        } catch (IOException ioe) {
            throw new BuildException(ioe.toString(), ioe);
        } finally {
            if (null != output) {
                try {
                    output.close();
                } catch (IOException ioe) {
                    log(ioe.toString(), Project.MSG_ERR);
                }
            }
        }
    }

    /**
     * Write a single entry to the given writer.
     *
     * @param writer a <code>PrintWriter</code> value
     * @param entry a <code>SvnRevisionEntry</code> value
     */
    private void writeRevisionEntry(PrintWriter writer, SvnEntry.Path entry) {
        writer.println("\t<path>");
        writer.println("\t\t<name><![CDATA[" + entry.getName() + "]]></name>");
        writer.println("\t\t<action>" + entry.getActionDescription() 
                       + "</action>");
        writer.println("\t</path>");
    }

    /**
     * Validate the parameters specified for task.
     *
     * @exception BuildException if a parameter is not correctly set
     */
    private void validate() throws BuildException {
        if (null == mydestfile) {
            throw new BuildException("Destfile must be set.");
        }

        if (null == mystartRevision) {
            throw new BuildException("Start revision or start date must be set.");
        }

        if (null == myendRevision) {
            throw new BuildException("End revision or end date must be set.");
        }
    }
}
