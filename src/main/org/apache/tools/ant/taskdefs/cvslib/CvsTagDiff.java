/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "Ant" and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */
package org.apache.tools.ant.taskdefs.cvslib;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Vector;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.AbstractCvsTask;
import org.apache.tools.ant.util.FileUtils;

/**
 * Examines the output of cvs rdiff between two tags.
 *
 * It produces an XML output representing the list of changes.
 * <PRE>
 * &lt;!-- Root element --&gt;
 * &lt;!ELEMENT tagdiff ( entry+ ) &gt;
 * &lt;!-- Start tag of the report --&gt;
 * &lt;!ATTLIST tagdiff startTag NMTOKEN #IMPLIED &gt;
 * &lt;!-- End tag of the report --&gt;
 * &lt;!ATTLIST tagdiff endTag NMTOKEN #IMPLIED &gt;
 * &lt;!-- Start date of the report --&gt;
 * &lt;!ATTLIST tagdiff startDate NMTOKEN #IMPLIED &gt;
 * &lt;!-- End date of the report --&gt;
 * &lt;!ATTLIST tagdiff endDate NMTOKEN #IMPLIED &gt;
 *
 * &lt;!-- CVS tag entry --&gt;
 * &lt;!ELEMENT entry ( file ) &gt;
 * &lt;!-- File added, changed or removed --&gt;
 * &lt;!ELEMENT file ( name, revision?, prevrevision? ) &gt;
 * &lt;!-- Name of the file --&gt;
 * &lt;!ELEMENT name ( #PCDATA ) &gt;
 * &lt;!-- Revision number --&gt;
 * &lt;!ELEMENT revision ( #PCDATA ) &gt;
 * &lt;!-- Previous revision number --&gt;
 * &lt;!ELEMENT prevrevision ( #PCDATA ) &gt;
 * </PRE>
 *
 * @author <a href="mailto:fred@castify.net">Frederic Lavigne</a>
 * @author <a href="mailto:rvanoo@xs4all.nl">Rob van Oostrum</a>
 * @version $Revision$ $Date$
 * @since Ant 1.5
 * @ant.task name="cvstagdiff"
 */
public class CvsTagDiff extends AbstractCvsTask {

    /**
     * Token to identify a new file in the rdiff log
     */
    static final String FILE_IS_NEW = " is new; current revision ";

    /**
     * Token to identify a modified file in the rdiff log
     */
    static final String FILE_HAS_CHANGED = " changed from revision ";

    /**
     * Token to identify a removed file in the rdiff log
     */
    static final String FILE_WAS_REMOVED = " is removed";

    /**
     * The cvs package/module to analyse
     */
    private String m_package;

    /**
     * The earliest tag from which diffs are to be included in the report.
     */
    private String m_startTag;

    /**
     * The latest tag from which diffs are to be included in the report.
     */
    private String m_endTag;

    /**
     * The earliest date from which diffs are to be included in the report.
     */
    private String m_startDate;

    /**
     * The latest date from which diffs are to be included in the report.
     */
    private String m_endDate;

    /**
     * The file in which to write the diff report.
     */
    private File m_destfile;

    /**
     * Used to create the temp file for cvs log
     */
    private FileUtils m_fileUtils = FileUtils.newFileUtils();

    /**
     * The package/module to analyze.
     */
    public void setPackage(String p) {
        m_package = p;
    }

    /**
     * Set the start tag.
     *
     * @param s the start tag.
     */
    public void setStartTag(String s) {
        m_startTag = s;
    }

    /**
     * Set the start date.
     *
     * @param s the start date.
     */
    public void setStartDate(String s) {
        m_startDate = s;
    }

    /**
     * Set the end tag.
     *
     * @param s the end tag.
     */
    public void setEndTag(String s) {
        m_endTag = s;
    }

    /**
     * Set the end date.
     *
     * @param s the end date.
     */
    public void setEndDate(String s) {
        m_endDate = s;
    }

    /**
     * Set the output file for the diff.
     *
     * @param f the output file for the diff.
     */
    public void setDestFile(File f) {
        m_destfile = f;
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
        String rdiff = "rdiff -s " +
            (m_startTag != null ? ("-r " + m_startTag) : ("-D " + m_startDate))
            + " "
            + (m_endTag != null ? ("-r " + m_endTag) : ("-D " + m_endDate))
            + " " + m_package;
        log("Cvs command is " + rdiff, Project.MSG_VERBOSE);
        setCommand(rdiff);

        File tmpFile = null;
        try {
            tmpFile = m_fileUtils.createTempFile("cvstagdiff", ".log", null);
            setOutput(tmpFile);

            // run the cvs command
            super.execute();

            // parse the rdiff
            CvsTagEntry[] entries = parseRDiff(tmpFile);

            // write the tag diff
            writeTagDiff(entries);

        } finally {
            if (tmpFile != null) {
                tmpFile.delete();
            }
        }
    }

    /**
     * Parse the tmpFile and return and array of CvsTagEntry to be
     * written in the output.
     *
     * @param tmpFile the File containing the output of the cvs rdiff command
     * @return the entries in the output
     * @exception BuildException if an error occurs
     */
    private CvsTagEntry[] parseRDiff(File tmpFile) throws BuildException {
        // parse the output of the command
        BufferedReader reader = null;

        try {
            reader = new BufferedReader(new FileReader(tmpFile));

            // entries are of the form:
            // File module/filename is new; current revision 1.1
            // or
            // File module/filename changed from revision 1.4 to 1.6
            // or
            // File module/filename is removed; not included in
            // release tag SKINLF_12

            // get rid of 'File module/"
            int headerLength = 5 + m_package.length() + 1;
            Vector entries = new Vector();

            String line = reader.readLine();
            int index;
            CvsTagEntry entry = null;

            while (null != line) {
                line = line.substring(headerLength);

                if ((index = line.indexOf(FILE_IS_NEW)) != -1) {
                    // it is a new file
                    // set the revision but not the prevrevision
                    String filename = line.substring(0, index);
                    String rev = line.substring(index + FILE_IS_NEW.length());

                    entries.addElement(entry = new CvsTagEntry(filename, rev));
                    log(entry.toString(), Project.MSG_VERBOSE);
                } else if ((index = line.indexOf(FILE_HAS_CHANGED)) != -1) {
                    // it is a modified file
                    // set the revision and the prevrevision
                    String filename = line.substring(0, index);
                    int revSeparator = line.indexOf(" to ", index);
                    String prevRevision =
                        line.substring(index + FILE_HAS_CHANGED.length(),
                                       revSeparator);
                     // 4 is " to " length
                    String revision = line.substring(revSeparator + 4);

                    entries.addElement(entry = new CvsTagEntry(filename,
                                                               revision,
                                                               prevRevision));
                    log(entry.toString(), Project.MSG_VERBOSE);
                } else if ((index = line.indexOf(FILE_WAS_REMOVED)) != -1) {
                    // it is a removed file
                    String filename = line.substring(0, index);

                    entries.addElement(entry = new CvsTagEntry(filename));
                    log(entry.toString(), Project.MSG_VERBOSE);
                }
                line = reader.readLine();
            }

            CvsTagEntry[] array = new CvsTagEntry[entries.size()];
            entries.copyInto(array);

            return array;
        } catch (IOException e) {
            throw new BuildException("Error in parsing", e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                }
            }
        }
    }

    /**
     * Write the rdiff log.
     *
     * @param entries a <code>CvsTagEntry[]</code> value
     * @exception BuildException if an error occurs
     */
    private void writeTagDiff(CvsTagEntry[] entries) throws BuildException {
        FileOutputStream output = null;
        try {
            output = new FileOutputStream(m_destfile);
            PrintWriter writer = new PrintWriter(
                                     new OutputStreamWriter(output, "UTF-8"));
            writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            writer.print("<tagdiff ");
            if (m_startTag != null) {
                writer.print("startTag=\"" + m_startTag + "\" ");
            } else {
                writer.print("startDate=\"" + m_startDate + "\" ");
            }
            if (m_endTag != null) {
                writer.print("endTag=\"" + m_endTag + "\" ");
            } else {
                writer.print("endDate=\"" + m_endDate + "\" ");
            }
            writer.println(">");
            for (int i = 0, c = entries.length; i < c; i++) {
                writeTagEntry(writer, entries[i]);
            }
            writer.println("</tagdiff>");
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
                } catch (IOException ioe) { }
            }
        }
    }

    /**
     * Write a single entry to the given writer.
     *
     * @param writer a <code>PrintWriter</code> value
     * @param entry a <code>CvsTagEntry</code> value
     */
    private void writeTagEntry(PrintWriter writer, CvsTagEntry entry) {
        writer.println("\t<entry>");
        writer.println("\t\t<file>");
        writer.println("\t\t\t<name>" + entry.getFile() + "</name>");
        if (entry.getRevision() != null) {
            writer.println("\t\t\t<revision>" + entry.getRevision()
                           + "</revision>");
        }
        if (entry.getPreviousRevision() != null) {
            writer.println("\t\t\t<prevrevision>"
                           + entry.getPreviousRevision() + "</prevrevision>");
        }
        writer.println("\t\t</file>");
        writer.println("\t</entry>");
    }

    /**
     * Validate the parameters specified for task.
     *
     * @exception BuildException if a parameter is not correctly set
     */
    private void validate() throws BuildException {
        if (null == m_package) {
            throw new BuildException("Package/module must be set.");
        }

        if (null == m_destfile) {
            throw new BuildException("Destfile must be set.");
        }

        if (null == m_startTag && null == m_startDate) {
            throw new BuildException("Start tag or start date must be set.");
        }

        if (null != m_startTag && null != m_startDate) {
            throw new BuildException("Only one of start tag and start date "
                                     + "must be set.");
        }

        if (null == m_endTag && null == m_endDate) {
            throw new BuildException("End tag or end date must be set.");
        }

        if (null != m_endTag && null != m_endDate) {
            throw new BuildException("Only one of end tag and end date must "
                                     + "be set.");
        }
    }
}
