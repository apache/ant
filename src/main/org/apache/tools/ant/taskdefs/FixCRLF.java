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

package org.apache.tools.ant.taskdefs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.util.Enumeration;
import java.util.NoSuchElementException;
import java.util.Vector;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.filters.ChainableReader;
import org.apache.tools.ant.filters.FixCrLfFilter;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.types.FilterChain;
import org.apache.tools.ant.util.FileUtils;

/**
 * Converts text source files to local OS formatting conventions, as
 * well as repair text files damaged by misconfigured or misguided editors or
 * file transfer programs.
 * <p>
 * This task can take the following arguments:
 * <ul>
 * <li>srcdir
 * <li>destdir
 * <li>include
 * <li>exclude
 * <li>cr
 * <li>eol
 * <li>tab
 * <li>eof
 * <li>encoding
 * <li>targetencoding
 * </ul>
 * Of these arguments, only <b>sourcedir</b> is required.
 * <p>
 * When this task executes, it will scan the srcdir based on the include
 * and exclude properties.
 * <p>
 * This version generalises the handling of EOL characters, and allows
 * for CR-only line endings (the standard on Mac systems prior to OS X).
 * Tab handling has also been generalised to accommodate any tabwidth
 * from 2 to 80, inclusive.  Importantly, it will leave untouched any
 * literal TAB characters embedded within string or character constants.
 * <p>
 * <em>Warning:</em> do not run on binary files.
 * <em>Caution:</em> run with care on carefully formatted files.
 * This may sound obvious, but if you don't specify asis, presume that
 * your files are going to be modified.  If "tabs" is "add" or "remove",
 * whitespace characters may be added or removed as necessary.  Similarly,
 * for CR's - in fact "eol"="crlf" or cr="add" can result in cr
 * characters being removed in one special case accommodated, i.e.,
 * CRCRLF is regarded as a single EOL to handle cases where other
 * programs have converted CRLF into CRCRLF.
 *
 * @since Ant 1.1
 *
 * @ant.task category="filesystem"
 */

public class FixCRLF extends MatchingTask implements ChainableReader {

    private static final String FIXCRLF_ERROR = "<fixcrlf> error: ";
    /** error string for using srcdir and file */
    public static final String ERROR_FILE_AND_SRCDIR
        = FIXCRLF_ERROR + "srcdir and file are mutually exclusive";

    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();

    private boolean preserveLastModified = false;
    private File srcDir;
    private File destDir = null;
    private File file;
    private FixCrLfFilter filter = new FixCrLfFilter();
    private Vector<FilterChain> fcv = null;

    /**
     * Encoding to assume for the files
     */
    private String encoding = null;

    /**
     * Encoding to use for output files
     */
    private String outputEncoding = null;


    /**
     * Chain this task as a reader.
     * @param rdr Reader to chain.
     * @return a Reader.
     * @since Ant 1.7?
     */
    @Override
    public final Reader chain(final Reader rdr) {
        return filter.chain(rdr);
    }

    /**
     * Set the source dir to find the source text files.
     * @param srcDir the source directory.
     */
    public void setSrcdir(File srcDir) {
        this.srcDir = srcDir;
    }

    /**
     * Set the destination where the fixed files should be placed.
     * Default is to replace the original file.
     * @param destDir the destination directory.
     */
    public void setDestdir(File destDir) {
        this.destDir = destDir;
    }

    /**
     * Set to true if modifying Java source files.
     * @param javafiles whether modifying Java files.
     */
    public void setJavafiles(boolean javafiles) {
        filter.setJavafiles(javafiles);
    }

    /**
     * Set a single file to convert.
     * @since Ant 1.6.3
     * @param file the file to convert.
     */
    public void setFile(File file) {
        this.file = file;
    }

    /**
     * Specify how EndOfLine characters are to be handled.
     *
     * @param attr valid values:
     * <ul>
     * <li>asis: leave line endings alone
     * <li>cr: convert line endings to CR
     * <li>lf: convert line endings to LF
     * <li>crlf: convert line endings to CRLF
     * </ul>
     */
    public void setEol(CrLf attr) {
        filter.setEol(FixCrLfFilter.CrLf.newInstance(attr.getValue()));
    }

    /**
     * Specify how carriage return (CR) characters are to be handled.
     *
     * @param attr valid values:
     * <ul>
     * <li>add: ensure that there is a CR before every LF
     * <li>asis: leave CR characters alone
     * <li>remove: remove all CR characters
     * </ul>
     *
     * @deprecated since 1.4.x.
     *             Use {@link #setEol setEol} instead.
     */
    @Deprecated
    public void setCr(AddAsisRemove attr) {
        log("DEPRECATED: The cr attribute has been deprecated,",
            Project.MSG_WARN);
        log("Please use the eol attribute instead", Project.MSG_WARN);
        String option = attr.getValue();
        CrLf c = new CrLf();
        if ("remove".equals(option)) {
            c.setValue("lf");
        } else if ("asis".equals(option)) {
            c.setValue("asis");
        } else {
            // must be "add"
            c.setValue("crlf");
        }
        setEol(c);
    }

    /**
     * Specify how tab characters are to be handled.
     *
     * @param attr valid values:
     * <ul>
     * <li>add: convert sequences of spaces which span a tab stop to tabs
     * <li>asis: leave tab and space characters alone
     * <li>remove: convert tabs to spaces
     * </ul>
     */
    public void setTab(AddAsisRemove attr) {
        filter.setTab(FixCrLfFilter.AddAsisRemove.newInstance(attr.getValue()));
    }

    /**
     * Specify tab length in characters.
     *
     * @param tlength specify the length of tab in spaces.
     * @throws BuildException on error.
     */
    public void setTablength(int tlength) throws BuildException {
        try {
            filter.setTablength(tlength);
        } catch (IOException e) {
            // filter.setTablength throws IOException that would better be
            // a BuildException
            throw new BuildException(e.getMessage(), e);
        }
    }

    /**
     * Specify how DOS EOF (control-z) characters are to be handled.
     *
     * @param attr valid values:
     * <ul>
     * <li>add: ensure that there is an eof at the end of the file
     * <li>asis: leave eof characters alone
     * <li>remove: remove any eof character found at the end
     * </ul>
     */
    public void setEof(AddAsisRemove attr) {
        filter.setEof(FixCrLfFilter.AddAsisRemove.newInstance(attr.getValue()));
    }

    /**
     * Specifies the encoding Ant expects the files to be
     * in--defaults to the platforms default encoding.
     * @param encoding String encoding name.
     */
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    /**
     * Specifies the encoding that the files are
     * to be written in--same as input encoding by default.
     * @param outputEncoding String outputEncoding name.
     */
    public void setOutputEncoding(String outputEncoding) {
        this.outputEncoding = outputEncoding;
    }

    /**
     * Specify whether a missing EOL will be added
     * to the final line of a file.
     * @param fixlast whether to fix the last line.
     */
    public void setFixlast(boolean fixlast) {
        filter.setFixlast(fixlast);
    }

    /**
     * Set whether to preserve the last modified time as the original files.
     * @param preserve true if timestamps should be preserved.
     * @since Ant 1.6.3
     */
    public void setPreserveLastModified(boolean preserve) {
        preserveLastModified = preserve;
    }

    /**
     * Executes the task.
     * @throws BuildException on error.
     */
    @Override
    public void execute() throws BuildException {
        // first off, make sure that we've got a srcdir and destdir
        validate();

        // log options used
        String enc = encoding == null ? "default" : encoding;
        log("options:"
            + " eol=" + filter.getEol().getValue()
            + " tab=" + filter.getTab().getValue()
            + " eof=" + filter.getEof().getValue()
            + " tablength=" + filter.getTablength()
            + " encoding=" + enc
            + " outputencoding="
            + (outputEncoding == null ? enc : outputEncoding),
            Project.MSG_VERBOSE);

        DirectoryScanner ds = super.getDirectoryScanner(srcDir);

        for (String filename : ds.getIncludedFiles()) {
            processFile(filename);
        }
    }

    private void validate() throws BuildException {
        if (file != null) {
            if (srcDir != null) {
                throw new BuildException(ERROR_FILE_AND_SRCDIR);
            }
            //patch file into the fileset
            fileset.setFile(file);
            //set our parent dir
            srcDir = file.getParentFile();
        }
        if (srcDir == null) {
            throw new BuildException(
                FIXCRLF_ERROR + "srcdir attribute must be set!");
        }
        if (!srcDir.exists()) {
            throw new BuildException(
                FIXCRLF_ERROR + "srcdir does not exist: '%s'", srcDir);
        }
        if (!srcDir.isDirectory()) {
            throw new BuildException(
                FIXCRLF_ERROR + "srcdir is not a directory: '%s'", srcDir);
        }
        if (destDir != null) {
            if (!destDir.exists()) {
                throw new BuildException(
                    FIXCRLF_ERROR + "destdir does not exist: '%s'", destDir);
            }
            if (!destDir.isDirectory()) {
                throw new BuildException(
                    FIXCRLF_ERROR + "destdir is not a directory: '%s'",
                    destDir);
            }
        }
    }

    private void processFile(String file) throws BuildException {
        File srcFile = new File(srcDir, file);
        long lastModified = srcFile.lastModified();
        File destD = destDir == null ? srcDir : destDir;

        if (fcv == null) {
            FilterChain fc = new FilterChain();
            fc.add(filter);
            fcv = new Vector<>(1);
            fcv.add(fc);
        }
        File tmpFile = FILE_UTILS.createTempFile(getProject(), "fixcrlf", "", null, true, true);
        try {
            FILE_UTILS.copyFile(srcFile, tmpFile, null, fcv, true, false,
                encoding, outputEncoding == null ? encoding : outputEncoding,
                getProject());

            File destFile = new File(destD, file);

            boolean destIsWrong = true;
            if (destFile.exists()) {
                // Compare the destination with the temp file
                log("destFile " + destFile + " exists", Project.MSG_DEBUG);
                destIsWrong = !FILE_UTILS.contentEquals(destFile, tmpFile);
                log(destFile + (destIsWrong ? " is being written"
                    : " is not written, as the contents are identical"),
                    Project.MSG_DEBUG);
            }
            if (destIsWrong) {
                FILE_UTILS.rename(tmpFile, destFile);
                if (preserveLastModified) {
                    log("preserved lastModified for " + destFile,
                        Project.MSG_DEBUG);
                    FILE_UTILS.setFileLastModified(destFile, lastModified);
                }
            }
        } catch (IOException e) {
            throw new BuildException("error running fixcrlf on file " + srcFile, e);
        } finally {
            if (tmpFile != null && tmpFile.exists()) {
                FILE_UTILS.tryHardToDelete(tmpFile);
            }
        }
    }

    /**
     * Deprecated, the functionality has been moved to filters.FixCrLfFilter.
     * @deprecated since 1.7.0.
     */
    @Deprecated
    protected class OneLiner implements Enumeration<Object> {
        private static final int UNDEF = -1;
        private static final int NOTJAVA = 0;
        private static final int LOOKING = 1;
        private static final int INBUFLEN = 8192;
        private static final int LINEBUFLEN = 200;
        private static final char CTRLZ = '\u001A';

        private int state = filter.getJavafiles() ? LOOKING : NOTJAVA;

        private StringBuffer eolStr = new StringBuffer(LINEBUFLEN);
        private StringBuffer eofStr = new StringBuffer();

        private BufferedReader reader;
        private StringBuffer line = new StringBuffer();
        private boolean reachedEof = false;
        private File srcFile;

        /**
         * Constructor.
         * @param srcFile the file to read.
         * @throws BuildException if there is an error.
         */
        public OneLiner(File srcFile)
            throws BuildException {
            this.srcFile = srcFile;
            try {
                reader = new BufferedReader(
                    ((encoding == null) ? new FileReader(srcFile)
                    : new InputStreamReader(
                    Files.newInputStream(srcFile.toPath()), encoding)), INBUFLEN);

                nextLine();
            } catch (IOException e) {
                throw new BuildException(srcFile + ": " + e.getMessage(),
                                         e, getLocation());
            }
        }

        /**
         * Move to the next line.
         * @throws BuildException if there is an error.
         */
        protected void nextLine()
            throws BuildException {
            int ch = -1;
            int eolcount = 0;

            eolStr = new StringBuffer();
            line = new StringBuffer();

            try {
                ch = reader.read();
                while (ch != -1 && ch != '\r' && ch != '\n') {
                    line.append((char) ch);
                    ch = reader.read();
                }

                if (ch == -1 && line.length() == 0) {
                    // Eof has been reached
                    reachedEof = true;
                    return;
                }

                switch ((char) ch) {
                case '\r':
                    // Check for \r, \r\n and \r\r\n
                    // Regard \r\r not followed by \n as two lines
                    ++eolcount;
                    eolStr.append('\r');
                    reader.mark(2);
                    ch = reader.read();
                    switch (ch) {
                    case '\r':
                        ch = reader.read();
                        if ((char) ch == '\n') {
                            eolcount += 2;
                            eolStr.append("\r\n");
                        } else {
                            reader.reset();
                        }
                        break;
                    case '\n':
                        ++eolcount;
                        eolStr.append('\n');
                        break;
                    case -1:
                        // don't reposition when we've reached the end
                        // of the stream
                        break;
                    default:
                        reader.reset();
                        break;
                    } // end of switch ((char)(ch = reader.read()))
                    break;

                case '\n':
                    ++eolcount;
                    eolStr.append('\n');
                    break;
                default:
                    // Fall tru
                } // end of switch ((char) ch)

                // if at eolcount == 0 and trailing characters of string
                // are CTRL-Zs, set eofStr
                if (eolcount == 0) {
                    int i = line.length();
                    while (--i >= 0 && line.charAt(i) == CTRLZ) {
                        // keep searching for the first ^Z
                    }
                    if (i < line.length() - 1) {
                        // Trailing characters are ^Zs
                        // Construct new line and eofStr
                        eofStr.append(line.toString().substring(i + 1));
                        if (i < 0) {
                            line.setLength(0);
                            reachedEof = true;
                        } else {
                            line.setLength(i + 1);
                        }
                    }

                } // end of if (eolcount == 0)

            } catch (IOException e) {
                throw new BuildException(srcFile + ": " + e.getMessage(),
                                         e, getLocation());
            }
        }

        /**
         * get the eof string.
         * @return the eof string.
         */
        public String getEofStr() {
            return eofStr.substring(0);
        }

        /**
         * get the state.
         * @return the state.
         */
        public int getState() {
            return state;
        }

        /**
         * Set the state.
         * @param state the value to use.
         */
        public void setState(int state) {
            this.state = state;
        }

        /**
         * @return true if there is more elements.
         */
        @Override
        public boolean hasMoreElements() {
            return !reachedEof;
        }

        /**
         * get the next element.
         * @return the next element.
         * @throws NoSuchElementException if there is no more.
         */
        @Override
        public Object nextElement()
            throws NoSuchElementException {
            if (!hasMoreElements()) {
                throw new NoSuchElementException("OneLiner");
            }
            BufferLine tmpLine =
                    new BufferLine(line.toString(), eolStr.substring(0));
            nextLine();
            return tmpLine;
        }

        /**
         * Close the reader.
         * @throws IOException if there is an error.
         */
        public void close() throws IOException {
            if (reader != null) {
                reader.close();
            }
        }

        class BufferLine {
            private int next = 0;
            private int column = 0;
            private int lookahead = UNDEF;
            private String line;
            private String eolStr;

            public BufferLine(String line, String eolStr)
                throws BuildException {
                next = 0;
                column = 0;
                this.line = line;
                this.eolStr = eolStr;
            }

            public int getNext() {
                return next;
            }

            public void setNext(int next) {
                this.next = next;
            }

            public int getLookahead() {
                return lookahead;
            }

            public void setLookahead(int lookahead) {
                this.lookahead = lookahead;
            }

            public char getChar(int i) {
                return line.charAt(i);
            }

            public char getNextChar() {
                return getChar(next);
            }

            public char getNextCharInc() {
                return getChar(next++);
            }

            public int getColumn() {
                return column;
            }

            public void setColumn(int col) {
                column = col;
            }

            public int incColumn() {
                return column++;
            }

            public int length() {
                return line.length();
            }

            public int getEolLength() {
                return eolStr.length();
            }

            public String getLineString() {
                return line;
            }

            public String getEol() {
                return eolStr;
            }

            public String substring(int begin) {
                return line.substring(begin);
            }

            public String substring(int begin, int end) {
                return line.substring(begin, end);
            }

            public void setState(int state) {
                OneLiner.this.setState(state);
            }

            public int getState() {
                return OneLiner.this.getState();
            }
        }
    }

    /**
     * Enumerated attribute with the values "asis", "add" and "remove".
     */
    public static class AddAsisRemove extends EnumeratedAttribute {
        /** {@inheritDoc}. */
        @Override
        public String[] getValues() {
            return new String[] {"add", "asis", "remove"};
        }
    }

    /**
     * Enumerated attribute with the values "asis", "cr", "lf", "crlf", "mac", "unix" and "dos.
     */
    public static class CrLf extends EnumeratedAttribute {
        /**
         * @see EnumeratedAttribute#getValues
         * {@inheritDoc}.
         */
        @Override
        public String[] getValues() {
            return new String[] {"asis", "cr", "lf", "crlf", "mac", "unix",
                "dos"};
        }
    }

}

