/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
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

package org.apache.tools.ant.taskdefs;

import java.io.File;
import java.io.Reader;
import java.io.Writer;
import java.io.FileReader;
import java.io.InputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.Vector;
import java.util.Iterator;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.ProjectComponent;
import org.apache.tools.ant.filters.util.ChainReaderHelper;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.FileList;
import org.apache.tools.ant.types.FilterChain;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.resources.Restrict;
import org.apache.tools.ant.types.resources.Resources;
import org.apache.tools.ant.types.resources.FileResource;
import org.apache.tools.ant.types.resources.StringResource;
import org.apache.tools.ant.types.resources.selectors.Not;
import org.apache.tools.ant.types.resources.selectors.Exists;
import org.apache.tools.ant.types.resources.selectors.ResourceSelector;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.ConcatResourceInputStream;

/**
 * This class contains the 'concat' task, used to concatenate a series
 * of files into a single stream. The destination of this stream may
 * be the system console, or a file. The following is a sample
 * invocation:
 *
 * <pre>
 * &lt;concat destfile=&quot;${build.dir}/index.xml&quot;
 *   append=&quot;false&quot;&gt;
 *
 *   &lt;fileset dir=&quot;${xml.root.dir}&quot;
 *     includes=&quot;*.xml&quot; /&gt;
 *
 * &lt;/concat&gt;
 * </pre>
 *
 */
public class Concat extends Task {

    // The size of buffers to be used
    private static final int BUFFER_SIZE = 8192;

    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();

    private static final ResourceSelector EXISTS = new Exists();
    private static final ResourceSelector NOT_EXISTS = new Not(EXISTS);

    // Attributes.

    /**
     * The destination of the stream. If <code>null</code>, the system
     * console is used.
     */
    private File destinationFile;

    /**
     * Whether or not the stream should be appended if the destination file
     * exists.
     * Defaults to <code>false</code>.
     */
    private boolean append;

    /**
     * Stores the input file encoding.
     */
    private String encoding;

    /** Stores the output file encoding. */
    private String outputEncoding;

    /** Stores the binary attribute */
    private boolean binary;

    // Child elements.

    /**
     * This buffer stores the text within the 'concat' element.
     */
    private StringBuffer textBuffer;

    /**
     * Stores a collection of file sets and/or file lists, used to
     * select multiple files for concatenation.
     */
    private Resources rc;

    /** for filtering the concatenated */
    private Vector filterChains;
    /** ignore dates on input files */
    private boolean forceOverwrite = true;
    /** String to place at the start of the concatented stream */
    private TextElement footer;
    /** String to place at the end of the concatented stream */
    private TextElement header;
    /** add missing line.separator to files **/
    private boolean fixLastLine = false;
    /** endofline for fixlast line */
    private String eolString;
    /** outputwriter */
    private Writer outputWriter = null;

    /**
     * Construct a new Concat task.
     */
    public Concat() {
        reset();
    }

    /**
     * Reset state to default.
     */
    public void reset() {
        append = false;
        forceOverwrite = true;
        destinationFile = null;
        encoding = null;
        outputEncoding = null;
        fixLastLine = false;
        filterChains = null;
        footer = null;
        header = null;
        binary = false;
        outputWriter = null;
        textBuffer = null;
        eolString = System.getProperty("line.separator");
        rc = null;
    }

    // Attribute setters.

    /**
     * Sets the destination file, or uses the console if not specified.
     * @param destinationFile the destination file
     */
    public void setDestfile(File destinationFile) {
        this.destinationFile = destinationFile;
    }

    /**
     * Sets the behavior when the destination file exists. If set to
     * <code>true</code> the stream data will be appended to the
     * existing file, otherwise the existing file will be
     * overwritten. Defaults to <code>false</code>.
     * @param append if true append to the file.
     */
    public void setAppend(boolean append) {
        this.append = append;
    }

    /**
     * Sets the character encoding
     * @param encoding the encoding of the input stream and unless
     *        outputencoding is set, the outputstream.
     */
    public void setEncoding(String encoding) {
        this.encoding = encoding;
        if (outputEncoding == null) {
            outputEncoding = encoding;
        }
    }

    /**
     * Sets the character encoding for outputting
     * @param outputEncoding the encoding for the output file
     * @since Ant 1.6
     */
    public void setOutputEncoding(String outputEncoding) {
        this.outputEncoding = outputEncoding;
    }

    /**
     * Force overwrite existing destination file
     * @param force if true always overwrite, otherwise only overwrite
     *              if the output file is older any of the input files.
     * @since Ant 1.6
     */
    public void setForce(boolean force) {
        this.forceOverwrite = force;
    }

    // Nested element creators.

    /**
     * Path of files to concatenate.
     * @return the path used for concatenating
     * @since Ant 1.6
     */
     public Path createPath() {
        Path path = new Path(getProject());
        add(path);
        return path;
    }

    /**
     * Set of files to concatenate.
     * @param set the set of files
     */
    public void addFileset(FileSet set) {
        add(set);
    }

    /**
     * List of files to concatenate.
     * @param list the list of files
     */
    public void addFilelist(FileList list) {
        add(list);
    }

    /**
     * Add an arbitrary ResourceCollection.
     * @param c the ResourceCollection to add.
     * @since Ant 1.7
     */
    public void add(ResourceCollection c) {
        rc = rc == null ? new Resources() : rc;
        rc.add(c);
    }

    /**
     * Adds a FilterChain.
     * @param filterChain a filterchain to filter the concatenated input
     * @since Ant 1.6
     */
    public void addFilterChain(FilterChain filterChain) {
        if (filterChains == null) {
            filterChains = new Vector();
        }
        filterChains.addElement(filterChain);
    }

    /**
     * This method adds text which appears in the 'concat' element.
     * @param text the text to be concated.
     */
    public void addText(String text) {
        if (textBuffer == null) {
            // Initialize to the size of the first text fragment, with
            // the hopes that it's the only one.
            textBuffer = new StringBuffer(text.length());
        }

        // Append the fragment -- we defer property replacement until
        // later just in case we get a partial property in a fragment.
        textBuffer.append(text);
    }

    /**
     * Add a header to the concatenated output
     * @param headerToAdd the header
     * @since Ant 1.6
     */
    public void addHeader(TextElement headerToAdd) {
        this.header = headerToAdd;
    }

    /**
     * Add a footer to the concatenated output
     * @param footerToAdd the footer
     * @since Ant 1.6
     */
    public void addFooter(TextElement footerToAdd) {
        this.footer = footerToAdd;
    }

    /**
     * Append line.separator to files that do not end
     * with a line.separator, default false.
     * @param fixLastLine if true make sure each input file has
     *                    new line on the concatenated stream
     * @since Ant 1.6
     */
    public void setFixLastLine(boolean fixLastLine) {
        this.fixLastLine = fixLastLine;
    }

    /**
     * Specify the end of line to find and to add if
     * not present at end of each input file. This attribute
     * is used in conjunction with fixlastline.
     * @param crlf the type of new line to add -
     *              cr, mac, lf, unix, crlf, or dos
     * @since Ant 1.6
     */
    public void setEol(FixCRLF.CrLf crlf) {
        String s = crlf.getValue();
        if (s.equals("cr") || s.equals("mac")) {
            eolString = "\r";
        } else if (s.equals("lf") || s.equals("unix")) {
            eolString = "\n";
        } else if (s.equals("crlf") || s.equals("dos")) {
            eolString = "\r\n";
        }
    }

    /**
     * Set the output writer. This is to allow
     * concat to be used as a nested element.
     * @param outputWriter the output writer.
     * @since Ant 1.6
     */
    public void setWriter(Writer outputWriter) {
        this.outputWriter = outputWriter;
    }

    /**
     * Set the binary attribute. If true, concat will concatenate the files
     * byte for byte. This mode does not allow any filtering or other
     * modifications to the input streams. The default value is false.
     * @since Ant 1.6.2
     * @param binary if true, enable binary mode.
     */
    public void setBinary(boolean binary) {
        this.binary = binary;
    }

    /**
     * Validate configuration options.
     */
    private ResourceCollection validate() {

        // treat empty nested text as no text
        sanitizeText();

        // if binary check if incompatible attributes are used
        if (binary) {
            if (destinationFile == null) {
                throw new BuildException(
                    "destfile attribute is required for binary concatenation");
            }
            if (textBuffer != null) {
                throw new BuildException(
                    "Nested text is incompatible with binary concatenation");
            }
            if (encoding != null || outputEncoding != null) {
                throw new BuildException(
                    "Seting input or output encoding is incompatible with binary"
                    + " concatenation");
            }
            if (filterChains != null) {
                throw new BuildException(
                    "Setting filters is incompatible with binary concatenation");
            }
            if (fixLastLine) {
                throw new BuildException(
                    "Setting fixlastline is incompatible with binary concatenation");
            }
            if (header != null || footer != null) {
                throw new BuildException(
                    "Nested header or footer is incompatible with binary concatenation");
            }
        }
        if (destinationFile != null && outputWriter != null) {
            throw new BuildException(
                "Cannot specify both a destination file and an output writer");
        }
        // Sanity check our inputs.
        if (rc == null && textBuffer == null) {
            // Nothing to concatenate!
            throw new BuildException(
                "At least one resource must be provided, or some text.");
        }
        if (rc != null) {
            // If using resources, disallow inline text. This is similar to
            // using GNU 'cat' with file arguments -- stdin is simply
            // ignored.
            if (textBuffer != null) {
                throw new BuildException(
                    "Cannot include inline text when using resources.");
            }
            Restrict noexistRc = new Restrict();
            noexistRc.add(NOT_EXISTS);
            noexistRc.add(rc);
            for (Iterator i = noexistRc.iterator(); i.hasNext();) {
                log(i.next() + " does not exist.", Project.MSG_ERR);
            }
            if (destinationFile != null) {
                for (Iterator i = rc.iterator(); i.hasNext();) {
                    Object o = i.next();
                    if (o instanceof FileResource) {
                        File f = ((FileResource) o).getFile();
                        if (FILE_UTILS.fileNameEquals(f, destinationFile)) {
                            throw new BuildException("Input file \""
                                + f + "\" is the same as the output file.");
                        }
                    }
                }
            }
            Restrict existRc = new Restrict();
            existRc.add(EXISTS);
            existRc.add(rc);
            boolean outofdate = destinationFile == null || forceOverwrite;
            if (!outofdate) {
                for (Iterator i = existRc.iterator(); !outofdate && i.hasNext();) {
                    Resource r = (Resource) i.next();
                    outofdate =
                        (r.getLastModified() == 0L
                         || r.getLastModified() > destinationFile.lastModified());
                }
            }
            if (!outofdate) {
                log(destinationFile + " is up-to-date.", Project.MSG_VERBOSE);
                return null; // no need to do anything
            }
            return existRc;
        } else {
            StringResource s = new StringResource();
            s.setProject(getProject());
            s.setValue(textBuffer.toString());
            return s;
        }
    }

    /**
     * Execute the concat task.
     */
    public void execute() {
        ResourceCollection c = validate();
        if (c == null) {
            return;
        }
        // Do nothing if no resources (including nested text)
        if (c.size() < 1 && header == null && footer == null) {
            log("No existing resources and no nested text, doing nothing",
                Project.MSG_INFO);
            return;
        }
        if (binary) {
            binaryCat(c);
        } else {
            cat(c);
        }
    }

    /** perform the binary concatenation */
    private void binaryCat(ResourceCollection c) {
        log("Binary concatenation of " + c.size()
            + " resources to " + destinationFile);
        FileOutputStream out = null;
        InputStream in = null;
        try {
            try {
                out = new FileOutputStream(destinationFile);
            } catch (Exception t) {
                throw new BuildException("Unable to open "
                    + destinationFile + " for writing", t);
            }
            in = new ConcatResourceInputStream(c);
            ((ConcatResourceInputStream) in).setManagingComponent(this);
            Thread t = new Thread(new StreamPumper(in, out));
            t.start();
            try {
                t.join();
            } catch (InterruptedException e) {
                try {
                    t.join();
                } catch (InterruptedException ee) {
                    // Empty
                }
            }
        } finally {
            FileUtils.close(in);
            if (out != null) {
                try {
                    out.close();
                } catch (Exception ex) {
                    throw new BuildException(
                        "Unable to close " + destinationFile, ex);
                }
            }
        }
    }

    /** perform the concatenation */
    private void cat(ResourceCollection c) {
        OutputStream os = null;
        char[] buffer = new char[BUFFER_SIZE];

        try {
            PrintWriter writer = null;

            if (outputWriter != null) {
                writer = new PrintWriter(outputWriter);
            } else {
                if (destinationFile == null) {
                    // Log using WARN so it displays in 'quiet' mode.
                    os = new LogOutputStream(this, Project.MSG_WARN);
                } else {
                    // ensure that the parent dir of dest file exists
                    File parent = destinationFile.getParentFile();
                    if (!parent.exists()) {
                        parent.mkdirs();
                    }
                    os = new FileOutputStream(destinationFile.getAbsolutePath(),
                                              append);
                }
                if (outputEncoding == null) {
                    writer = new PrintWriter(
                        new BufferedWriter(
                            new OutputStreamWriter(os)));
                } else {
                    writer = new PrintWriter(
                        new BufferedWriter(
                            new OutputStreamWriter(os, outputEncoding)));
                }
            }
            if (header != null) {
                if (header.getFiltering()) {
                    concatenate(
                        buffer, writer, new StringReader(header.getValue()));
                } else {
                    writer.print(header.getValue());
                }
            }
            if (c.size() > 0) {
                concatenate(buffer, writer, new MultiReader(c));
            }
            if (footer != null) {
                if (footer.getFiltering()) {
                    concatenate(
                        buffer, writer, new StringReader(footer.getValue()));
                } else {
                    writer.print(footer.getValue());
                }
            }
            writer.flush();
            if (os != null) {
                os.flush();
            }
        } catch (IOException ioex) {
            throw new BuildException("Error while concatenating: "
                                     + ioex.getMessage(), ioex);
        } finally {
            FileUtils.close(os);
        }
    }

    /** Concatenate a single reader to the writer using buffer */
    private void concatenate(char[] buffer, Writer writer, Reader in)
        throws IOException {
        if (filterChains != null) {
            ChainReaderHelper helper = new ChainReaderHelper();
            helper.setBufferSize(BUFFER_SIZE);
            helper.setPrimaryReader(in);
            helper.setFilterChains(filterChains);
            helper.setProject(getProject());
            in = new BufferedReader(helper.getAssembledReader());
        }
        while (true) {
            int nRead = in.read(buffer, 0, buffer.length);
            if (nRead == -1) {
                break;
            }
            writer.write(buffer, 0, nRead);
        }
        writer.flush();
    }

    /**
     * Treat empty nested text as no text.
     *
     * <p>Depending on the XML parser, addText may have been called
     * for &quot;ignorable whitespace&quot; as well.</p>
     */
    private void sanitizeText() {
        if (textBuffer != null) {
            if (textBuffer.substring(0).trim().length() == 0) {
                textBuffer = null;
            }
        }
    }

    /**
     * sub element points to a file or contains text
     */
    public static class TextElement extends ProjectComponent {
        private String   value = "";
        private boolean  trimLeading = false;
        private boolean  trim = false;
        private boolean  filtering = true;
        private String   encoding = null;

        /**
         * whether to filter the text in this element
         * or not.
         *
         * @param filtering true if the text should be filtered.
         *                  the default value is true.
         */
        public void setFiltering(boolean filtering) {
            this.filtering = filtering;
        }

        /** return the filtering attribute */
        private boolean getFiltering() {
            return filtering;
        }

        /**
         * The encoding of the text element
         *
         * @param encoding the name of the charset used to encode
         */
        public void setEncoding(String encoding) {
            this.encoding = encoding;
        }

        /**
         * set the text using a file
         * @param file the file to use
         * @throws BuildException if the file does not exist, or cannot be
         *                        read
         */
        public void setFile(File file) throws BuildException {
            // non-existing files are not allowed
            if (!file.exists()) {
                throw new BuildException("File " + file + " does not exist.");
            }

            BufferedReader reader = null;
            try {
                if (this.encoding == null) {
                    reader = new BufferedReader(new FileReader(file));
                } else {
                    reader = new BufferedReader(
                        new InputStreamReader(new FileInputStream(file),
                                              this.encoding));
                }
                value = FileUtils.readFully(reader);
            } catch (IOException ex) {
                throw new BuildException(ex);
            } finally {
                FileUtils.close(reader);
            }
        }

        /**
         * set the text using inline
         * @param value the text to place inline
         */
        public void addText(String value) {
            this.value += getProject().replaceProperties(value);
        }

        /**
         * s:^\s*:: on each line of input
         * @param strip if true do the trim
         */
        public void setTrimLeading(boolean strip) {
            this.trimLeading = strip;
        }

        /**
         * whether to call text.trim()
         * @param trim if true trim the text
         */
        public void setTrim(boolean trim) {
            this.trim = trim;
        }

        /**
         * @return the text, after possible trimming
         */
        public String getValue() {
            if (value == null) {
                value = "";
            }
            if (value.trim().length() == 0) {
                value = "";
            }
            if (trimLeading) {
                char[] current = value.toCharArray();
                StringBuffer b = new StringBuffer(current.length);
                boolean startOfLine = true;
                int pos = 0;
                while (pos < current.length) {
                    char ch = current[pos++];
                    if (startOfLine) {
                        if (ch == ' ' || ch == '\t') {
                            continue;
                        }
                        startOfLine = false;
                    }
                    b.append(ch);
                    if (ch == '\n' || ch == '\r') {
                        startOfLine = true;
                    }
                }
                value = b.toString();
            }
            if (trim) {
                value = value.trim();
            }
            return value;
        }
    }

    /**
     * This class reads from each of the source files in turn.
     * The concatentated result can then be filtered as
     * a single stream.
     */
    private class MultiReader extends Reader {
        private Reader reader = null;
        private int    lastPos = 0;
        private char[] lastChars = new char[eolString.length()];
        private boolean needAddSeparator = false;
        private Iterator i;

        private MultiReader(ResourceCollection c) {
            i = c.iterator();
        }

        private Reader getReader() throws IOException {
            if (reader == null && i.hasNext()) {
                Resource r = (Resource) i.next();
                log("Concating " + r.toLongString(), Project.MSG_VERBOSE);
                InputStream is = r.getInputStream();
                reader = new BufferedReader(encoding == null
                    ? new InputStreamReader(is)
                    : new InputStreamReader(is, encoding));
                Arrays.fill(lastChars, (char) 0);
            }
            return reader;
        }

        private void nextReader() throws IOException {
            close();
            reader = null;
        }

        /**
         * Read a character from the current reader object. Advance
         * to the next if the reader is finished.
         * @return the character read, -1 for EOF on the last reader.
         * @exception IOException - possibly thrown by the read for a reader
         *            object.
         */
        public int read() throws IOException {
            if (needAddSeparator) {
                int ret = eolString.charAt(lastPos++);
                if (lastPos >= eolString.length()) {
                    lastPos = 0;
                    needAddSeparator = false;
                }
                return ret;
            }
            while (getReader() != null) {
                int ch = getReader().read();
                if (ch == -1) {
                    nextReader();
                    if (fixLastLine && isMissingEndOfLine()) {
                        needAddSeparator = true;
                        lastPos = 0;
                    }
                } else {
                    addLastChar((char) ch);
                    return ch;
                }
            }
            return -1;
        }

        /**
         * Read into the buffer <code>cbuf</code>.
         * @param cbuf The array to be read into.
         * @param off The offset.
         * @param len The length to read.
         * @exception IOException - possibly thrown by the reads to the
         *            reader objects.
         */
        public int read(char[] cbuf, int off, int len)
            throws IOException {

            int amountRead = 0;
            while (getReader() != null || needAddSeparator) {
                if (needAddSeparator) {
                    cbuf[off] = eolString.charAt(lastPos++);
                    if (lastPos >= eolString.length()) {
                        lastPos = 0;
                        needAddSeparator = false;
                    }
                    len--;
                    off++;
                    amountRead++;
                    if (len == 0) {
                        return amountRead;
                    }
                    continue;
                }
                int nRead = getReader().read(cbuf, off, len);
                if (nRead == -1 || nRead == 0) {
                    nextReader();
                    if (fixLastLine && isMissingEndOfLine()) {
                        needAddSeparator = true;
                        lastPos = 0;
                    }
                } else {
                    if (fixLastLine) {
                        for (int i = nRead;
                                 i > (nRead - lastChars.length);
                                 --i) {
                            if (i <= 0) {
                                break;
                            }
                            addLastChar(cbuf[off + i - 1]);
                        }
                    }
                    len -= nRead;
                    off += nRead;
                    amountRead += nRead;
                    if (len == 0) {
                        return amountRead;
                    }
                }
            }
            if (amountRead == 0) {
                return -1;
            } else {
                return amountRead;
            }
        }

        /**
         * Close the current reader
         */
        public void close() throws IOException {
            if (reader != null) {
                reader.close();
            }
        }

        /**
         * if checking for end of line at end of file
         * add a character to the lastchars buffer
         */
        private void addLastChar(char ch) {
            for (int i = lastChars.length - 2; i >= 0; --i) {
                lastChars[i] = lastChars[i + 1];
            }
            lastChars[lastChars.length - 1] = ch;
        }

        /**
         * return true if the lastchars buffer does
         * not contain the lineseparator
         */
        private boolean isMissingEndOfLine() {
            for (int i = 0; i < lastChars.length; ++i) {
                if (lastChars[i] != eolString.charAt(i)) {
                    return true;
                }
            }
            return false;
        }
    }

}

