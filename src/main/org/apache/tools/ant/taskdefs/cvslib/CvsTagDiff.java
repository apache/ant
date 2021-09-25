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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.AbstractCvsTask;
import org.apache.tools.ant.util.DOMElementWriter;
import org.apache.tools.ant.util.DOMUtils;
import org.apache.tools.ant.util.FileUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Examines the output of cvs rdiff between two tags.
 *
 * It produces an XML output representing the list of changes.
 * <pre>
 * &lt;!-- Root element --&gt;
 * &lt;!ELEMENT tagdiff (entry+) &gt;
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
 * &lt;!ELEMENT entry (file) &gt;
 * &lt;!-- File added, changed or removed --&gt;
 * &lt;!ELEMENT file (name, revision?, prevrevision?) &gt;
 * &lt;!-- Name of the file --&gt;
 * &lt;!ELEMENT name (#PCDATA) &gt;
 * &lt;!-- Revision number --&gt;
 * &lt;!ELEMENT revision (#PCDATA) &gt;
 * &lt;!-- Previous revision number --&gt;
 * &lt;!ELEMENT prevrevision (#PCDATA) &gt;
 * </pre>
 *
 * @since Ant 1.5
 * @ant.task name="cvstagdiff"
 */
public class CvsTagDiff extends AbstractCvsTask {

    /**
     * Used to create the temp file for cvs log
     */
    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();

    /** stateless helper for writing the XML document */
    private static final DOMElementWriter DOM_WRITER = new DOMElementWriter();

    /**
     * Token to identify the word file in the rdiff log
     */
    static final String FILE_STRING = "File ";
    /**
     * Length of token to identify the word file in the rdiff log
     */
    static final int FILE_STRING_LENGTH = FILE_STRING.length();
    /**
     * Token to identify the word file in the rdiff log
     */
    static final String TO_STRING = " to ";
    /**
     * Token to identify a new file in the rdiff log
     */
    static final String FILE_IS_NEW = " is new;";
    /**
     * Token to identify the revision
     */
    static final String REVISION = "revision ";

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
    private String mypackage;

    /**
     * The earliest tag from which diffs are to be included in the report.
     */
    private String mystartTag;

    /**
     * The latest tag from which diffs are to be included in the report.
     */
    private String myendTag;

    /**
     * The earliest date from which diffs are to be included in the report.
     */
    private String mystartDate;

    /**
     * The latest date from which diffs are to be included in the report.
     */
    private String myendDate;

    /**
     * The file in which to write the diff report.
     */
    private File mydestfile;

    /**
     * Used to skip over removed files
     */
    private boolean ignoreRemoved = false;

    /**
     * temporary list of package names.
     */
    private List<String> packageNames = new ArrayList<>();

    /**
     * temporary list of "File:" + package name + "/" for all packages.
     */
    private String[] packageNamePrefixes = null;

    /**
     * temporary list of length values for prefixes.
     */
    private int[] packageNamePrefixLengths = null;

    /**
     * The package/module to analyze.
     * @param p the name of the package to analyse
     */
    @Override
    public void setPackage(String p) {
        mypackage = p;
    }

    /**
     * Set the start tag.
     *
     * @param s the start tag.
     */
    public void setStartTag(String s) {
        mystartTag = s;
    }

    /**
     * Set the start date.
     *
     * @param s the start date.
     */
    public void setStartDate(String s) {
        mystartDate = s;
    }

    /**
     * Set the end tag.
     *
     * @param s the end tag.
     */
    public void setEndTag(String s) {
        myendTag = s;
    }

    /**
     * Set the end date.
     *
     * @param s the end date.
     */
    public void setEndDate(String s) {
        myendDate = s;
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
     * Set the ignore removed indicator.
     *
     * @param b the ignore removed indicator.
     *
     * @since Ant 1.8.0
     */
    public void setIgnoreRemoved(boolean b) {
        ignoreRemoved = b;
    }

    /**
     * Execute task.
     *
     * @exception BuildException if an error occurs
     */
    @Override
    public void execute() throws BuildException {
        // validate the input parameters
        validate();

        // build the rdiff command
        addCommandArgument("rdiff");
        addCommandArgument("-s");
        if (mystartTag != null) {
            addCommandArgument("-r");
            addCommandArgument(mystartTag);
        } else {
            addCommandArgument("-D");
            addCommandArgument(mystartDate);
        }
        if (myendTag != null) {
            addCommandArgument("-r");
            addCommandArgument(myendTag);
        } else {
            addCommandArgument("-D");
            addCommandArgument(myendDate);
        }

        // force command not to be null
        setCommand("");
        File tmpFile = null;
        try {
            handlePackageNames();

            tmpFile = FILE_UTILS.createTempFile(getProject(), "cvstagdiff", ".log", null,
                                                true, true);
            setOutput(tmpFile);

            // run the cvs command
            super.execute();

            // parse the rdiff
            CvsTagEntry[] entries = parseRDiff(tmpFile);

            // write the tag diff
            writeTagDiff(entries);

        } finally {
            packageNamePrefixes = null;
            packageNamePrefixLengths = null;
            packageNames.clear();
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

        try (BufferedReader reader =
            new BufferedReader(new FileReader(tmpFile))) {

            // entries are of the form:
            //CVS 1.11
            // File module/filename is new; current revision 1.1
            //CVS 1.11.9
            // File module/filename is new; cvstag_2003_11_03_2  revision 1.1
            // or
            // File module/filename changed from revision 1.4 to 1.6
            // or
            // File module/filename is removed; not included in
            // release tag SKINLF_12
            //CVS 1.11.9
            // File testantoine/antoine.bat is removed; TESTANTOINE_1 revision 1.1.1.1
            //
            // get rid of 'File module/"
            List<CvsTagEntry> entries = new ArrayList<>();

            String line = reader.readLine();

            while (null != line) {
                line = removePackageName(line, packageNamePrefixes,
                                         packageNamePrefixLengths);
                if (line != null) {
                    // use || in a perl like fashion
                    @SuppressWarnings("unused")
                    boolean processed
                        =  doFileIsNew(entries, line)
                        || doFileHasChanged(entries, line)
                        || doFileWasRemoved(entries, line);
                }
                line = reader.readLine();
            }

            return entries.toArray(new CvsTagEntry[0]);
        } catch (IOException e) {
            throw new BuildException("Error in parsing", e);
        }
    }

    private boolean doFileIsNew(List<CvsTagEntry> entries, String line) {
        int index = line.indexOf(FILE_IS_NEW);
        if (index == -1) {
            return false;
        }
        // it is a new file
        // set the revision but not the prevrevision
        String filename = line.substring(0, index);
        String rev = null;
        int indexrev = line.indexOf(REVISION, index);
        if (indexrev != -1) {
            rev = line.substring(indexrev + REVISION.length());
        }
        CvsTagEntry entry = new CvsTagEntry(filename, rev);
        entries.add(entry);
        log(entry.toString(), Project.MSG_VERBOSE);
        return true;
    }

    private boolean doFileHasChanged(List<CvsTagEntry> entries, String line) {
        int index = line.indexOf(FILE_HAS_CHANGED);
        if (index == -1) {
            return false;
        }
        // it is a modified file
        // set the revision and the prevrevision
        String filename = line.substring(0, index);
        int revSeparator = line.indexOf(" to ", index);
        String prevRevision =
            line.substring(index + FILE_HAS_CHANGED.length(),
                           revSeparator);
        String revision = line.substring(revSeparator + TO_STRING.length());
        CvsTagEntry entry = new CvsTagEntry(filename,
                                            revision,
                                            prevRevision);
        entries.add(entry);
        log(entry.toString(), Project.MSG_VERBOSE);
        return true;
    }

    private boolean doFileWasRemoved(List<CvsTagEntry> entries, String line) {
        if (ignoreRemoved) {
            return false;
        }
        int index = line.indexOf(FILE_WAS_REMOVED);
        if (index == -1) {
            return false;
        }
        // it is a removed file
        String filename = line.substring(0, index);
        String rev = null;
        int indexrev = line.indexOf(REVISION, index);
        if (indexrev != -1) {
            rev = line.substring(indexrev + REVISION.length());
        }
        CvsTagEntry entry = new CvsTagEntry(filename, null, rev);
        entries.add(entry);
        log(entry.toString(), Project.MSG_VERBOSE);
        return true;
    }

    /**
     * Write the rdiff log.
     *
     * @param entries a <code>CvsTagEntry[]</code> value
     * @exception BuildException if an error occurs
     */
    private void writeTagDiff(CvsTagEntry[] entries) throws BuildException {
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(
            Files.newOutputStream(mydestfile.toPath()), StandardCharsets.UTF_8))) {
            writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            Document doc = DOMUtils.newDocument();
            Element root = doc.createElement("tagdiff");
            if (mystartTag != null) {
                root.setAttribute("startTag", mystartTag);
            } else {
                root.setAttribute("startDate", mystartDate);
            }
            if (myendTag != null) {
                root.setAttribute("endTag", myendTag);
            } else {
                root.setAttribute("endDate", myendDate);
            }

            root.setAttribute("cvsroot", getCvsRoot());
            root.setAttribute("package",
                    String.join(",", packageNames));
            DOM_WRITER.openElement(root, writer, 0, "\t");
            writer.println();
            for (CvsTagEntry entry : entries) {
                writeTagEntry(doc, writer, entry);
            }
            DOM_WRITER.closeElement(root, writer, 0, "\t", true);
            writer.flush();
            if (writer.checkError()) {
                throw new IOException("Encountered an error writing tagdiff");
            }
        } catch (UnsupportedEncodingException uee) {
            log(uee.toString(), Project.MSG_ERR);
        } catch (IOException ioe) {
            throw new BuildException(ioe.toString(), ioe);
        }
    }

    /**
     * Write a single entry to the given writer.
     *
     * @param doc Document used to create elements.
     * @param writer a <code>PrintWriter</code> value
     * @param entry a <code>CvsTagEntry</code> value
     */
    private void writeTagEntry(Document doc, PrintWriter writer,
                               CvsTagEntry entry)
        throws IOException {
        Element ent = doc.createElement("entry");
        Element f = DOMUtils.createChildElement(ent, "file");
        DOMUtils.appendCDATAElement(f, "name", entry.getFile());
        if (entry.getRevision() != null) {
            DOMUtils.appendTextElement(f, "revision", entry.getRevision());
        }
        if (entry.getPreviousRevision() != null) {
            DOMUtils.appendTextElement(f, "prevrevision",
                                       entry.getPreviousRevision());
        }
        DOM_WRITER.write(ent, writer, 1, "\t");
    }

    /**
     * Validate the parameters specified for task.
     *
     * @exception BuildException if a parameter is not correctly set
     */
    private void validate() throws BuildException {
        if (null == mypackage && getModules().isEmpty()) {
            throw new BuildException("Package/module must be set.");
        }

        if (null == mydestfile) {
            throw new BuildException("Destfile must be set.");
        }

        if (null == mystartTag && null == mystartDate) {
            throw new BuildException("Start tag or start date must be set.");
        }

        if (null != mystartTag && null != mystartDate) {
            throw new BuildException(
                "Only one of start tag and start date must be set.");
        }

        if (null == myendTag && null == myendDate) {
            throw new BuildException("End tag or end date must be set.");
        }

        if (null != myendTag && null != myendDate) {
            throw new BuildException(
                "Only one of end tag and end date must be set.");
        }
    }

    /**
     * collects package names from the package attribute and nested
     * module elements.
     */
    private void handlePackageNames() {
        if (mypackage != null) {
            // support multiple packages
            StringTokenizer myTokenizer = new StringTokenizer(mypackage);
            while (myTokenizer.hasMoreTokens()) {
                String pack = myTokenizer.nextToken();
                packageNames.add(pack);
                addCommandArgument(pack);
            }
        }
        for (Module m : getModules()) {
            packageNames.add(m.getName());
            // will be added to command line in super.execute()
        }
        packageNamePrefixes = new String[packageNames.size()];
        packageNamePrefixLengths = new int[packageNames.size()];
        for (int i = 0; i < packageNamePrefixes.length; i++) {
            packageNamePrefixes[i] = FILE_STRING + packageNames.get(i) + "/";
            packageNamePrefixLengths[i] = packageNamePrefixes[i].length();
        }
    }

    /**
     * removes a "File: module/" prefix if present.
     *
     * @return null if the line was shorter than expected.
     */
    private static String removePackageName(String line,
                                            String[] packagePrefixes,
                                            int[] prefixLengths) {
        if (line.length() < FILE_STRING_LENGTH) {
            return null;
        }
        for (int i = 0; i < packagePrefixes.length; i++) {
            if (line.startsWith(packagePrefixes[i])) {
                return line.substring(prefixLengths[i]);
            }
        }
        return line.substring(FILE_STRING_LENGTH);
    }
}
