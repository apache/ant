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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.Vector;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectComponent;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.filters.util.ChainReaderHelper;
import org.apache.tools.ant.types.FileList;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.FilterChain;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.resources.FileResource;
import org.apache.tools.ant.types.resources.Intersect;
import org.apache.tools.ant.types.resources.LogOutputResource;
import org.apache.tools.ant.types.resources.Resources;
import org.apache.tools.ant.types.resources.Restrict;
import org.apache.tools.ant.types.resources.StringResource;
import org.apache.tools.ant.types.resources.selectors.Exists;
import org.apache.tools.ant.types.resources.selectors.Not;
import org.apache.tools.ant.types.resources.selectors.ResourceSelector;
import org.apache.tools.ant.types.selectors.SelectorUtils;
import org.apache.tools.ant.util.ConcatResourceInputStream;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.ReaderInputStream;
import org.apache.tools.ant.util.ResourceUtils;

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
public class Concat extends Task implements ResourceCollection {

    // The size of buffers to be used
    private static final int BUFFER_SIZE = 8192;

    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();

    private static final ResourceSelector EXISTS = new Exists();
    private static final ResourceSelector NOT_EXISTS = new Not(EXISTS);

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
                throw new BuildException("File %s does not exist.", file);
            }

            BufferedReader reader = null;
            try {
                if (this.encoding == null) {
                    reader = new BufferedReader(new FileReader(file));
                } else {
                    reader = new BufferedReader(
                        new InputStreamReader(Files.newInputStream(file.toPath()),
                                              this.encoding));
                }
                value = FileUtils.safeReadFully(reader);
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
            if (value.trim().isEmpty()) {
                value = "";
            }
            if (trimLeading) {
                StringBuilder b = new StringBuilder();
                boolean startOfLine = true;
                for (final char ch : value.toCharArray()) {
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

    private interface ReaderFactory<S> {
        Reader getReader(S s) throws IOException;
    }

    /**
     * This class reads a Reader and ensures it ends with the desired linebreak
     * @since Ant 1.10.10
     */
    private final class LastLineFixingReader extends Reader {
        private final Reader reader;
        private int lastPos = 0;
        private final char[] lastChars = new char[eolString.length()];
        private boolean needAddSeparator = false;

        private LastLineFixingReader(Reader reader) {
            this.reader = reader;
        }

        /**
         * Read a character from the current reader object. Advance
         * to the next if the reader is finished.
         * @return the character read, -1 for EOF on the last reader.
         * @exception IOException - possibly thrown by the read for a reader
         *            object.
         */
        @Override
        public int read() throws IOException {
            if (needAddSeparator) {
                if (lastPos >= eolString.length()) {
                    return -1;
                } else {
                    return eolString.charAt(lastPos++);
                }
            }
            int ch = reader.read();
            if (ch == -1) {
                if (isMissingEndOfLine()) {
                    needAddSeparator = true;
                    lastPos = 1;
                    return eolString.charAt(0);
                }
            } else {
                addLastChar((char) ch);
                return ch;
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
        @Override
        public int read(char[] cbuf, int off, int len)
            throws IOException {

            int amountRead = 0;
            while (true) {
                if (needAddSeparator) {
                    if (lastPos >= eolString.length()) {
                        break;
                    }
                    cbuf[off] = eolString.charAt(lastPos++);
                    len--;
                    off++;
                    amountRead++;
                    if (len == 0) {
                        return amountRead;
                    }
                    continue;
                }
                int nRead = reader.read(cbuf, off, len);
                if (nRead == -1 || nRead == 0) {
                    if (isMissingEndOfLine()) {
                        needAddSeparator = true;
                        lastPos = 0;
                    } else {
                        break;
                    }
                } else {
                    for (int i = nRead;
                         i > (nRead - lastChars.length);
                         --i) {
                        if (i <= 0) {
                            break;
                        }
                        addLastChar(cbuf[off + i - 1]);
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
            }
            return amountRead;
        }

        /**
         * Close the current reader
         */
        @Override
        public void close() throws IOException {
            reader.close();
        }

        /**
         * if checking for end of line at end of file
         * add a character to the lastchars buffer
         */
        private void addLastChar(char ch) {
            System.arraycopy(lastChars, 1, lastChars, 0, lastChars.length - 2 + 1);
            lastChars[lastChars.length - 1] = ch;
        }

        /**
         * return true if the lastchars buffer does
         * not contain the line separator
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

    /**
     * This class reads from each of the source files in turn.
     * The concatenated result can then be filtered as
     * a single stream.
     */
    private final class MultiReader<S> extends Reader {
        private Reader reader = null;
        private Iterator<S> readerSources;
        private ReaderFactory<S> factory;
        private final boolean filterBeforeConcat;

        private MultiReader(Iterator<S> readerSources, ReaderFactory<S> factory,
                boolean filterBeforeConcat) {
            this.readerSources = readerSources;
            this.factory = factory;
            this.filterBeforeConcat = filterBeforeConcat;
        }

        private Reader getReader() throws IOException {
            if (reader == null && readerSources.hasNext()) {
                reader = factory.getReader(readerSources.next());
                if (isFixLastLine()) {
                    reader = new LastLineFixingReader(reader);
                }
                if (filterBeforeConcat) {
                    reader = getFilteredReader(reader);
                }
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
        @Override
        public int read() throws IOException {
            while (getReader() != null) {
                int ch = getReader().read();
                if (ch == -1) {
                    nextReader();
                } else {
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
        @Override
        public int read(char[] cbuf, int off, int len)
            throws IOException {

            int amountRead = 0;
            while (getReader() != null) {
                int nRead = getReader().read(cbuf, off, len);
                if (nRead == -1 || nRead == 0) {
                    nextReader();
                } else {
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
            }
            return amountRead;
        }

        /**
         * Close the current reader
         */
        @Override
        public void close() throws IOException {
            if (reader != null) {
                reader.close();
            }
        }

        private boolean isFixLastLine() {
            return fixLastLine && textBuffer == null;
        }
    }

    private final class ConcatResource extends Resource {
        private ResourceCollection c;

        private ConcatResource(ResourceCollection c) {
            this.c = c;
        }
        @Override
        public InputStream getInputStream() {
            if (binary) {
                ConcatResourceInputStream result = new ConcatResourceInputStream(c);
                result.setManagingComponent(this);
                return result;
            }
            Reader resourceReader;
            if (filterBeforeConcat) {
                resourceReader = new MultiReader<>(c.iterator(),
                        resourceReaderFactory, true);
            } else {
                resourceReader = getFilteredReader(
                        new MultiReader<>(c.iterator(), resourceReaderFactory, false));
            }
            Reader rdr;
            if (header == null && footer == null) {
                rdr = resourceReader;
            } else {
                int readerCount = 1;
                if (header != null) {
                    readerCount++;
                }
                if (footer != null) {
                    readerCount++;
                }
                Reader[] readers = new Reader[readerCount];
                int pos = 0;
                if (header != null) {
                    readers[pos] = new StringReader(header.getValue());
                    if (header.getFiltering()) {
                        readers[pos] = getFilteredReader(readers[pos]);
                    }
                    pos++;
                }
                readers[pos++] = resourceReader;
                if (footer != null) {
                    readers[pos] = new StringReader(footer.getValue());
                    if (footer.getFiltering()) {
                        readers[pos] = getFilteredReader(readers[pos]);
                    }
                }
                rdr = new MultiReader<>(Arrays.asList(readers).iterator(),
                        identityReaderFactory, false);
            }
            return outputEncoding == null ? new ReaderInputStream(rdr)
                    : new ReaderInputStream(rdr, outputEncoding);
        }
        @Override
        public String getName() {
            return resourceName == null
                    ? "concat (" + c + ")" : resourceName;
        }
    }

    // Attributes.

    /**
     * The destination of the stream. If <code>null</code>, the system
     * console is used.
     */
    private Resource dest;

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

    /** Stores the filterBeforeConcat attribute */
    private boolean filterBeforeConcat;

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
    private Vector<FilterChain> filterChains;
    /** ignore dates on input files */
    private boolean forceOverwrite = true;
    /** overwrite read-only files */
    private boolean force = false;
    /** String to place at the start of the concatenated stream */
    private TextElement footer;
    /** String to place at the end of the concatenated stream */
    private TextElement header;
    /** add missing line.separator to files **/
    private boolean fixLastLine = false;
    /** endofline for fixlast line */
    private String eolString;
    /** outputwriter */
    private Writer outputWriter = null;
    /** whether to not create dest if no source files are
     * available */
    private boolean ignoreEmpty = true;
    /** exposed resource name */
    private String resourceName;

    private ReaderFactory<Resource> resourceReaderFactory = new ReaderFactory<Resource>() {
        @Override
        public Reader getReader(Resource o) throws IOException {
            InputStream is = o.getInputStream();
            return new BufferedReader(encoding == null
                ? new InputStreamReader(is)
                : new InputStreamReader(is, encoding));
        }
    };

    private ReaderFactory<Reader> identityReaderFactory = o -> o;

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
        dest = null;
        encoding = null;
        outputEncoding = null;
        fixLastLine = false;
        filterChains = null;
        footer = null;
        header = null;
        binary = false;
        outputWriter = null;
        textBuffer = null;
        eolString = System.lineSeparator();
        rc = null;
        ignoreEmpty = true;
        force = false;
    }

    // Attribute setters.

    /**
     * Sets the destination file, or uses the console if not specified.
     * @param destinationFile the destination file
     */
    public void setDestfile(File destinationFile) {
        setDest(new FileResource(destinationFile));
    }

    /**
     * Set the resource to write to.
     * @param dest the Resource to write to.
     * @since Ant 1.8
     */
    public void setDest(Resource dest) {
        this.dest = dest;
    }

    /**
     * Sets the behavior when the destination exists. If set to
     * <code>true</code> the task will append the stream data an
     * {@link Appendable} resource; otherwise existing content will be
     * overwritten. Defaults to <code>false</code>.
     * @param append if true append output.
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
     * @param forceOverwrite if true always overwrite, otherwise only
     *              overwrite if the output file is older any of the
     *              input files.
     * @since Ant 1.6
     * @deprecated use #setOverwrite instead
     */
    @Deprecated
    public void setForce(boolean forceOverwrite) {
        this.forceOverwrite = forceOverwrite;
    }

    /**
     * Force overwrite existing destination file
     * @param forceOverwrite if true always overwrite, otherwise only
     *              overwrite if the output file is older any of the
     *              input files.
     * @since Ant 1.8.2
     */
    public void setOverwrite(boolean forceOverwrite) {
        setForce(forceOverwrite);
    }

    /**
     * Whether read-only destinations will be overwritten.
     *
     * <p>Defaults to false</p>
     *
     * @param f boolean
     * @since Ant 1.8.2
     */
    public void setForceReadOnly(boolean f) {
        force = f;
    }

    /**
     * Sets the behavior when no source resource files are available. If set to
     * <code>false</code> the destination file will always be created.
     * Defaults to <code>true</code>.
     * @param ignoreEmpty if false, honour destination file creation.
     * @since Ant 1.8.0
     */
    public void setIgnoreEmpty(boolean ignoreEmpty) {
        this.ignoreEmpty = ignoreEmpty;
    }

    /**
     * Set the name that will be reported by the exposed {@link Resource}.
     * @param resourceName to set
     * @since Ant 1.8.3
     */
    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
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
        synchronized (this) {
            if (rc == null) {
                rc = new Resources();
                rc.setProject(getProject());
                rc.setCache(true);
            }
        }
        rc.add(c);
    }

    /**
     * Adds a FilterChain.
     * @param filterChain a filterchain to filter the concatenated input
     * @since Ant 1.6
     */
    public void addFilterChain(FilterChain filterChain) {
        if (filterChains == null) {
            filterChains = new Vector<>();
        }
        filterChains.addElement(filterChain);
    }

    /**
     * This method adds text which appears in the 'concat' element.
     * @param text the text to be concatenated.
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
        if ("cr".equals(s) || "mac".equals(s)) {
            eolString = "\r";
        } else if ("lf".equals(s) || "unix".equals(s)) {
            eolString = "\n";
        } else if ("crlf".equals(s) || "dos".equals(s)) {
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
     * Set the filterBeforeConcat attribute. If true, concat will filter each
     * input through the filterchain before concatenating the results. This
     * allows to e.g. use the FileTokenizer to tokenize each input.
     * @param filterBeforeConcat if true, filter each input before concatenation
     * @since Ant 1.10.10
     */
    public void setFilterBeforeConcat(final boolean filterBeforeConcat) {
        this.filterBeforeConcat = filterBeforeConcat;
    }

    /**
     * Execute the concat task.
     */
    @Override
    public void execute() {
        validate();
        if (binary && dest == null) {
            throw new BuildException(
                "dest|destfile attribute is required for binary concatenation");
        }
        ResourceCollection c = getResources();
        if (isUpToDate(c)) {
            log(dest + " is up-to-date.", Project.MSG_VERBOSE);
            return;
        }
        if (c.isEmpty() && ignoreEmpty) {
            return;
        }
        try {
            //most of these are defaulted because the concat-as-a-resource code hijacks a lot:
            ResourceUtils.copyResource(new ConcatResource(c), dest == null
                                       ? new LogOutputResource(this, Project.MSG_WARN)
                                       : dest,
                                       null, null, true, false, append, null,
                                       null, getProject(), force);
        } catch (IOException e) {
            throw new BuildException("error concatenating content to " + dest, e);
        }
    }

    /**
     * Implement ResourceCollection.
     * @return Iterator&lt;Resource&gt;.
     */
    @Override
    public Iterator<Resource> iterator() {
        validate();
        return Collections
            .<Resource> singletonList(new ConcatResource(getResources()))
            .iterator();
    }

    /**
     * Implement ResourceCollection.
     * @return 1.
     */
    @Override
    public int size() {
        return 1;
    }

    /**
     * Implement ResourceCollection.
     * @return false.
     */
    @Override
    public boolean isFilesystemOnly() {
        return false;
    }

    /**
     * Validate configuration options.
     */
    private void validate() {

        // treat empty nested text as no text
        sanitizeText();

        // if binary check if incompatible attributes are used
        if (binary) {
            if (textBuffer != null) {
                throw new BuildException(
                    "Nested text is incompatible with binary concatenation");
            }
            if (encoding != null || outputEncoding != null) {
                throw new BuildException(
                    "Setting input or output encoding is incompatible with binary concatenation");
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
        if (dest != null && outputWriter != null) {
            throw new BuildException(
                "Cannot specify both a destination resource and an output writer");
        }
        // Sanity check our inputs.
        if (rc == null && textBuffer == null) {
            // Nothing to concatenate!
            throw new BuildException(
                "At least one resource must be provided, or some text.");
        }
        if (rc != null && textBuffer != null) {
            // If using resources, disallow inline text. This is similar to
            // using GNU 'cat' with file arguments--stdin is simply ignored.
            throw new BuildException(
                "Cannot include inline text when using resources.");
        }
    }

    /**
     * Get the resources to concatenate.
     */
    private ResourceCollection getResources() {
        if (rc == null) {
            return new StringResource(getProject(), textBuffer.toString());
        }
        if (dest != null) {
            Intersect checkDestNotInSources = new Intersect();
            checkDestNotInSources.setProject(getProject());
            checkDestNotInSources.add(rc);
            checkDestNotInSources.add(dest);
            if (checkDestNotInSources.size() > 0) {
                throw new BuildException(
                    "Destination resource %s was specified as an input resource.",
                    dest);
            }
        }
        Restrict noexistRc = new Restrict();
        noexistRc.add(NOT_EXISTS);
        noexistRc.add(rc);
        for (Resource r : noexistRc) {
            log(r + " does not exist.", Project.MSG_ERR);
        }
        Restrict result = new Restrict();
        result.add(EXISTS);
        result.add(rc);
        return result;
    }

    private boolean isUpToDate(ResourceCollection c) {
        return dest != null && !forceOverwrite
                && c.stream().noneMatch(r -> SelectorUtils.isOutOfDate(r, dest, FILE_UTILS.getFileTimestampGranularity()));
    }

    /**
     * Treat empty nested text as no text.
     *
     * <p>Depending on the XML parser, addText may have been called
     * for &quot;ignorable whitespace&quot; as well.</p>
     */
    private void sanitizeText() {
        if (textBuffer != null && textBuffer.toString().trim().isEmpty()) {
            textBuffer = null;
        }
    }

    private Reader getFilteredReader(Reader r) {
        if (filterChains == null) {
            return r;
        }
        ChainReaderHelper helper = new ChainReaderHelper();
        helper.setBufferSize(BUFFER_SIZE);
        helper.setPrimaryReader(r);
        helper.setFilterChains(filterChains);
        helper.setProject(getProject());
        //used to be a BufferedReader here, but we should be buffering lower:
        return helper.getAssembledReader();
    }

}
