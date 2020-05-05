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
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.resources.FileProvider;
import org.apache.tools.ant.types.resources.FileResource;
import org.apache.tools.ant.types.resources.Union;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.StreamUtils;

/**
 * Replaces all occurrences of one or more string tokens with given
 * values in the indicated files. Each value can be either a string
 * or the value of a property available in a designated property file.
 * If you want to replace a text that crosses line boundaries, you
 * must use a nested <code>&lt;replacetoken&gt;</code> element.
 *
 * @since Ant 1.1
 *
 * @ant.task category="filesystem"
 */
public class Replace extends MatchingTask {

    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();

    private File sourceFile = null;
    private NestedString token = null;
    private NestedString value = new NestedString();

    private Resource propertyResource = null;
    private Resource replaceFilterResource = null;
    private Properties properties = null;
    private List<Replacefilter> replacefilters = new ArrayList<>();

    private File dir = null;

    private int fileCount;
    private int replaceCount;
    private boolean summary = false;

    /** The encoding used to read and write files - if null, uses default */
    private String encoding = null;

    private Union resources;

    private boolean preserveLastModified = false;
    private boolean failOnNoReplacements = false;

    /**
     * An inline string to use as the replacement text.
     */
    public class NestedString {

        private boolean expandProperties = false;
        private StringBuffer buf = new StringBuffer();

        /**
         * Whether properties should be expanded in nested test.
         *
         * <p>If you use this class via its Java interface the text
         * you add via {@link #addText addText} has most likely been
         * expanded already so you do <b>not</b> want to set this to
         * true.</p>
         *
         * @param b boolean
         * @since Ant 1.8.0
         */
        public void setExpandProperties(boolean b) {
            expandProperties = b;
        }

        /**
         * The text of the element.
         *
         * @param val the string to add
         */
        public void addText(String val) {
            buf.append(val);
        }

        /**
         * @return the text
         */
        public String getText() {
            String s = buf.toString();
            return expandProperties ? getProject().replaceProperties(s) : s;
        }
    }

    /**
     * A filter to apply.
     */
    public class Replacefilter {
        private NestedString token;
        private NestedString value;
        private String replaceValue;
        private String property;

        private StringBuffer inputBuffer;
        private StringBuffer outputBuffer = new StringBuffer();

        /**
         * Validate the filter's configuration.
         * @throws BuildException if any part is invalid.
         */
        public void validate() throws BuildException {
            //Validate mandatory attributes
            if (token == null) {
                throw new BuildException(
                    "token is a mandatory for replacefilter.");
            }

            if (token.getText().isEmpty()) {
                throw new BuildException(
                    "The token must not be an empty string.");
            }

            //value and property are mutually exclusive attributes
            if (value != null && property != null) {
                throw new BuildException(
                    "Either value or property can be specified, but a replacefilter element cannot have both.");
            }

            if (property != null) {
                //the property attribute must have access to a property file
                if (propertyResource == null) {
                    throw new BuildException(
                        "The replacefilter's property attribute can only be used with the replacetask's propertyFile/Resource attribute.");
                }

                //Make sure property exists in property file
                if (properties == null
                    || properties.getProperty(property) == null) {
                    throw new BuildException(
                        "property \"%s\" was not found in %s", property,
                        propertyResource.getName());
                }
            }

            replaceValue = getReplaceValue();
        }

        /**
         * Get the replacement value for this filter token.
         * @return the replacement value
         */
        public String getReplaceValue() {
            if (property != null) {
                return properties.getProperty(property);
            }
            if (value != null) {
                return value.getText();
            }
            if (Replace.this.value != null) {
                return Replace.this.value.getText();
            }
            //Default is empty string
            return "";
        }

        /**
         * Set the token to replace.
         * @param t <code>String</code> token.
         */
        public void setToken(String t) {
            createReplaceToken().addText(t);
        }

        /**
         * Get the string to search for.
         * @return current <code>String</code> token.
         */
        public String getToken() {
            return token.getText();
        }

        /**
         * The replacement string; required if <code>property</code>
         * is not set.
         * @param value <code>String</code> value to replace.
         */
        public void setValue(String value) {
            createReplaceValue().addText(value);
        }

        /**
         * Get replacement <code>String</code>.
         * @return replacement or null.
         */
        public String getValue() {
            return value.getText();
        }

        /**
         * Set the name of the property whose value is to serve as
         * the replacement value; required if <code>value</code> is not set.
         * @param property property name.
         */
        public void setProperty(String property) {
            this.property = property;
        }

        /**
         * Get the name of the property whose value is to serve as
         * the replacement value.
         * @return property or null.
         */
        public String getProperty() {
            return property;
        }

        /**
         * Create a token to filter as the text of a nested element.
         * @return nested token <code>NestedString</code> to configure.
         * @since Ant 1.8.0
         */
        public NestedString createReplaceToken() {
            if (token == null) {
                token = new NestedString();
            }
            return token;
        }

        /**
         * Create a string to replace the token as the text of a nested element.
         * @return replacement value <code>NestedString</code> to configure.
         * @since Ant 1.8.0
         */
        public NestedString createReplaceValue() {
            if (value == null) {
                value = new NestedString();
            }
            return value;
        }

        /**
         * Retrieves the output buffer of this filter. The filter guarantees
         * that data is only appended to the end of this StringBuffer.
         * @return The StringBuffer containing the output of this filter.
         */
        StringBuffer getOutputBuffer() {
            return outputBuffer;
        }

        /**
         * Sets the input buffer for this filter.
         * The filter expects from the component providing the input that data
         * is only added by that component to the end of this StringBuffer.
         * This StringBuffer will be modified by this filter, and expects that
         * another component will only append to this StringBuffer.
         * @param input The input for this filter.
         */
        void setInputBuffer(StringBuffer input) {
            inputBuffer = input;
        }

        /**
         * Processes the buffer as far as possible. Takes into account that
         * appended data may make it possible to replace the end of the already
         * received data, when the token is split over the "old" and the "new"
         * part.
         * @return true if some data has been made available in the
         *         output buffer.
         */
        boolean process() {
            String t = getToken();
            if (inputBuffer.length() > t.length()) {
                int pos = replace();
                pos = Math.max(inputBuffer.length() - t.length(), pos);
                outputBuffer.append(inputBuffer.substring(0, pos));
                inputBuffer.delete(0, pos);
                return true;
            }
            return false;
        }

        /**
         * Processes the buffer to the end. Does not take into account that
         * appended data may make it possible to replace the end of the already
         * received data.
         */
        void flush() {
            replace();
            outputBuffer.append(inputBuffer);
            inputBuffer.delete(0, inputBuffer.length());
        }

        /**
         * Performs the replace operation.
         * @return The position of the last character that was inserted as
         *         replacement.
         */
        private int replace() {
            String t = getToken();
            int found = inputBuffer.indexOf(t);
            int pos = -1;
            final int tokenLength = t.length();
            final int replaceValueLength = replaceValue.length();
            while (found >= 0) {
                inputBuffer.replace(found, found + tokenLength, replaceValue);
                pos = found + replaceValueLength;
                found = inputBuffer.indexOf(t, pos);
                ++replaceCount;
            }
            return pos;
        }
    }

    /**
     * Class reading a file in small chunks, and presenting these chunks in
     * a StringBuffer. Compatible with the Replacefilter.
     * @since 1.7
     */
    private class FileInput implements AutoCloseable {
        private static final int BUFF_SIZE = 4096;

        private StringBuffer outputBuffer;
        private final InputStream is;
        private Reader reader;
        private char[] buffer;

        /**
         * Constructs the input component. Opens the file for reading.
         * @param source The file to read from.
         * @throws IOException When the file cannot be read from.
         */
        FileInput(File source) throws IOException {
            outputBuffer = new StringBuffer();
            buffer = new char[BUFF_SIZE];
            is = Files.newInputStream(source.toPath());
            try {
                reader = new BufferedReader(
                    encoding != null ? new InputStreamReader(is, encoding)
                        : new InputStreamReader(is));
            } finally {
                if (reader == null) {
                    is.close();
                }
            }
        }

        /**
         * Retrieves the output buffer of this filter. The component guarantees
         * that data is only appended to the end of this StringBuffer.
         * @return The StringBuffer containing the output of this filter.
         */
        StringBuffer getOutputBuffer() {
            return outputBuffer;
        }

        /**
         * Reads some data from the file.
         * @return true when the end of the file has not been reached.
         * @throws IOException When the file cannot be read from.
         */
        boolean readChunk() throws IOException {
            int bufferLength = reader.read(buffer);
            if (bufferLength < 0) {
                return false;
            }
            outputBuffer.append(new String(buffer, 0, bufferLength));
            return true;
        }

        /**
         * Closes the file.
         * @throws IOException When the file cannot be closed.
         */
        @Override
        public void close() throws IOException {
            is.close();
        }

    }

    /**
     * Component writing a file in chunks, taking the chunks from the
     * Replacefilter.
     * @since 1.7
     */
    private class FileOutput implements AutoCloseable {
        private StringBuffer inputBuffer;
        private final OutputStream os;
        private Writer writer;

        /**
         * Constructs the output component. Opens the file for writing.
         * @param out The file to read to.
         * @throws IOException When the file cannot be read from.
         */
        FileOutput(File out) throws IOException {
            os = Files.newOutputStream(out.toPath());
            try {
                writer = new BufferedWriter(
                    encoding != null ? new OutputStreamWriter(os, encoding)
                        : new OutputStreamWriter(os));
            } finally {
                if (writer == null) {
                    os.close();
                }
            }
        }

        /**
         * Sets the input buffer for this component.
         * The filter expects from the component providing the input that data
         * is only added by that component to the end of this StringBuffer.
         * This StringBuffer will be modified by this filter, and expects that
         * another component will only append to this StringBuffer.
         * @param input The input for this filter.
         */
        void setInputBuffer(StringBuffer input) {
            inputBuffer = input;
        }

        /**
         * Writes the buffer as far as possible.
         * @return false to be inline with the Replacefilter.
         * (Yes defining an interface crossed my mind, but would publish the
         * internal behavior.)
         * @throws IOException when the output cannot be written.
         */
        boolean process() throws IOException {
            writer.write(inputBuffer.toString());
            inputBuffer.delete(0, inputBuffer.length());
            return false;
        }

        /**
         * Processes the buffer to the end.
         * @throws IOException when the output cannot be flushed.
         */
        void flush() throws IOException {
            process();
            writer.flush();
        }

        /**
         * Closes the file.
         * @throws IOException When the file cannot be closed.
         */
        @Override
        public void close() throws IOException {
            os.close();
        }

    }

    /**
     * Do the execution.
     * @throws BuildException if we can't build
     */
    @Override
    public void execute() throws BuildException {
        List<Replacefilter> savedFilters = new ArrayList<>(replacefilters);
        Properties savedProperties =
            properties == null ? null : (Properties) properties.clone();

        if (token != null) {
            // line separators in values and tokens are "\n"
            // in order to compare with the file contents, replace them
            // as needed
            StringBuilder val = new StringBuilder(value.getText());
            stringReplace(val, "\r\n", "\n");
            stringReplace(val, "\n", System.lineSeparator());
            StringBuilder tok = new StringBuilder(token.getText());
            stringReplace(tok, "\r\n", "\n");
            stringReplace(tok, "\n", System.lineSeparator());
            Replacefilter firstFilter = createPrimaryfilter();
            firstFilter.setToken(tok.toString());
            firstFilter.setValue(val.toString());
        }

        try {
            if (replaceFilterResource != null) {
                final Properties properties = getProperties(replaceFilterResource);
                StreamUtils.iteratorAsStream(getOrderedIterator(properties)).forEach(tok -> {
                    Replacefilter replaceFilter = createReplacefilter();
                    replaceFilter.setToken(tok);
                    replaceFilter.setValue(properties.getProperty(tok));
                });
            }

            validateAttributes();

            if (propertyResource != null) {
                properties = getProperties(propertyResource);
            }

            validateReplacefilters();
            fileCount = 0;
            replaceCount = 0;

            if (sourceFile != null) {
                processFile(sourceFile);
            }

            if (dir != null) {
                DirectoryScanner ds = super.getDirectoryScanner(dir);
                for (String src : ds.getIncludedFiles()) {
                    File file = new File(dir, src);
                    processFile(file);
                }
            }

            if (resources != null) {
                for (Resource r : resources) {
                    processFile(r.as(FileProvider.class).getFile());
                }
            }

            if (summary) {
                log("Replaced " + replaceCount + " occurrences in "
                    + fileCount + " files.", Project.MSG_INFO);
            }
            if (failOnNoReplacements && replaceCount == 0) {
                throw new BuildException("didn't replace anything");
            }
        } finally {
            replacefilters = savedFilters;
            properties = savedProperties;
        } // end of finally

    }

    /**
     * Validate attributes provided for this task in .xml build file.
     *
     * @exception BuildException if any supplied attribute is invalid or any
     * mandatory attribute is missing.
     */
    public void validateAttributes() throws BuildException {
        if (sourceFile == null && dir == null && resources == null) {
            throw new BuildException(
                "Either the file or the dir attribute or nested resources must be specified",
                getLocation());
        }
        if (propertyResource != null && !propertyResource.isExists()) {
            throw new BuildException("Property file "
                + propertyResource.getName() + " does not exist.",
                getLocation());
        }
        if (token == null && replacefilters.isEmpty()) {
            throw new BuildException(
                "Either token or a nested replacefilter must be specified",
                getLocation());
        }
        if (token != null && token.getText().isEmpty()) {
            throw new BuildException(
                "The token attribute must not be an empty string.",
                getLocation());
        }
    }

    /**
     * Validate nested elements.
     *
     * @exception BuildException if any supplied attribute is invalid or any
     * mandatory attribute is missing.
     */
    public void validateReplacefilters()
            throws BuildException {
        replacefilters.forEach(Replacefilter::validate);
    }

    /**
     * Load a properties file.
     * @param propertyFile the file to load the properties from.
     * @return loaded <code>Properties</code> object.
     * @throws BuildException if the file could not be found or read.
     */
    public Properties getProperties(File propertyFile) throws BuildException {
        return getProperties(new FileResource(getProject(), propertyFile));
    }

    /**
     * Load a properties resource.
     * @param propertyResource the resource to load the properties from.
     * @return loaded <code>Properties</code> object.
     * @throws BuildException if the resource could not be found or read.
     * @since Ant 1.8.0
     */
    public Properties getProperties(Resource propertyResource)
        throws BuildException {
        Properties props = new Properties();

        try (InputStream in = propertyResource.getInputStream()) {
            props.load(in);
        } catch (IOException e) {
            throw new BuildException("Property resource (%s) cannot be loaded.",
                propertyResource.getName());
        }
        return props;
    }

    /**
     * Perform the replacement on the given file.
     *
     * The replacement is performed on a temporary file which then
     * replaces the original file.
     *
     * @param src the source <code>File</code>.
     */
    private void processFile(File src) throws BuildException {
        if (!src.exists()) {
            throw new BuildException("Replace: source file " + src.getPath()
                                     + " doesn't exist", getLocation());
        }

        int repCountStart = replaceCount;
        logFilterChain(src.getPath());

        try {
            File temp = FILE_UTILS.createTempFile(getProject(), "rep", ".tmp",
                    src.getParentFile(), false, true);
            try {
                try (FileInput in = new FileInput(src);
                     FileOutput out = new FileOutput(temp)) {
                    out.setInputBuffer(buildFilterChain(in.getOutputBuffer()));

                    while (in.readChunk()) {
                        if (processFilterChain()) {
                            out.process();
                        }
                    }

                    flushFilterChain();

                    out.flush();
                }
                boolean changes = (replaceCount != repCountStart);
                if (changes) {
                    fileCount++;
                    long origLastModified = src.lastModified();
                    FILE_UTILS.rename(temp, src);
                    if (preserveLastModified) {
                        FILE_UTILS.setFileLastModified(src, origLastModified);
                    }
                }
            } finally {
                if (temp.isFile() && !temp.delete()) {
                    temp.deleteOnExit();
                }
            }
        } catch (IOException ioe) {
            throw new BuildException("IOException in " + src + " - "
                    + ioe.getClass().getName() + ":"
                    + ioe.getMessage(), ioe, getLocation());
        }
    }

    /**
     * Flushes all filters.
     */
    private void flushFilterChain() {
        replacefilters.forEach(Replacefilter::flush);
    }

    /**
     * Performs the normal processing of the filters.
     * @return true if the filter chain produced new output.
     */
    private boolean processFilterChain() {
        return replacefilters.stream().allMatch(Replacefilter::process);
    }

    /**
     * Creates the chain of filters to operate.
     * @param inputBuffer <code>StringBuffer</code> containing the input for the
     *                    first filter.
     * @return <code>StringBuffer</code> containing the output of the last filter.
     */
    private StringBuffer buildFilterChain(StringBuffer inputBuffer) {
        StringBuffer buf = inputBuffer;
        for (Replacefilter filter : replacefilters) {
            filter.setInputBuffer(buf);
            buf = filter.getOutputBuffer();
        }
        return buf;
    }

    /**
     * Logs the chain of filters to operate on the file.
     * @param filename <code>String</code>.
     */
    private void logFilterChain(String filename) {
        replacefilters
            .forEach(
                filter -> log(
                    "Replacing in " + filename + ": " + filter.getToken()
                        + " --> " + filter.getReplaceValue(),
                    Project.MSG_VERBOSE));
    }

    /**
     * Set the source file; required unless <code>dir</code> is set.
     * @param file source <code>File</code>.
     */
    public void setFile(File file) {
        this.sourceFile = file;
    }

    /**
     * Indicates whether a summary of the replace operation should be
     * produced, detailing how many token occurrences and files were
     * processed; optional, default=<code>false</code>.
     *
     * @param summary <code>boolean</code> whether a summary of the
     *                replace operation should be logged.
     */
    public void setSummary(boolean summary) {
        this.summary = summary;
    }


    /**
     * Sets the name of a property file containing filters; optional.
     * Each property will be treated as a replacefilter where token is the name
     * of the property and value is the value of the property.
     * @param replaceFilterFile <code>File</code> to load.
     */
    public void setReplaceFilterFile(File replaceFilterFile) {
        setReplaceFilterResource(new FileResource(getProject(),
                                                  replaceFilterFile));
    }

    /**
     * Sets the name of a resource containing filters; optional.
     * Each property will be treated as a replacefilter where token is the name
     * of the property and value is the value of the property.
     * @param replaceFilter <code>Resource</code> to load.
     * @since Ant 1.8.0
     */
    public void setReplaceFilterResource(Resource replaceFilter) {
        this.replaceFilterResource = replaceFilter;
    }

    /**
     * The base directory to use when replacing a token in multiple files;
     * required if <code>file</code> is not defined.
     * @param dir <code>File</code> representing the base directory.
     */
    public void setDir(File dir) {
        this.dir = dir;
    }

    /**
     * Set the string token to replace; required unless a nested
     * <code>replacetoken</code> element or the
     * <code>replacefilterresource</code> attribute is used.
     * @param token token <code>String</code>.
     */
    public void setToken(String token) {
        createReplaceToken().addText(token);
    }

    /**
     * Set the string value to use as token replacement;
     * optional, default is the empty string "".
     * @param value replacement value.
     */
    public void setValue(String value) {
        createReplaceValue().addText(value);
    }

    /**
     * Set the file encoding to use on the files read and written by the task;
     * optional, defaults to default JVM encoding.
     *
     * @param encoding the encoding to use on the files.
     */
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    /**
     * Create a token to filter as the text of a nested element.
     * @return nested token <code>NestedString</code> to configure.
     */
    public NestedString createReplaceToken() {
        if (token == null) {
            token = new NestedString();
        }
        return token;
    }

    /**
     * Create a string to replace the token as the text of a nested element.
     * @return replacement value <code>NestedString</code> to configure.
     */
    public NestedString createReplaceValue() {
        return value;
    }

    /**
     * The name of a property file from which properties specified using nested
     * <code>&lt;replacefilter&gt;</code> elements are drawn; required only if
     * the <i>property</i> attribute of <code>&lt;replacefilter&gt;</code> is used.
     * @param propertyFile <code>File</code> to load.
     */
    public void setPropertyFile(File propertyFile) {
        setPropertyResource(new FileResource(propertyFile));
    }

    /**
     * A resource from which properties specified using nested
     * <code>&lt;replacefilter&gt;</code> elements are drawn; required
     * only if the <i>property</i> attribute of
     * <code>&lt;replacefilter&gt;</code> is used.
     * @param propertyResource <code>Resource</code> to load.
     *
     * @since Ant 1.8.0
     */
    public void setPropertyResource(Resource propertyResource) {
        this.propertyResource = propertyResource;
    }

    /**
     * Add a nested &lt;replacefilter&gt; element.
     * @return a nested <code>Replacefilter</code> object to be configured.
     */
    public Replacefilter createReplacefilter() {
        Replacefilter filter = new Replacefilter();
        replacefilters.add(filter);
        return filter;
    }

    /**
     * Support arbitrary file system based resource collections.
     *
     * @param rc ResourceCollection
     * @since Ant 1.8.0
     */
    public void addConfigured(ResourceCollection rc) {
        if (!rc.isFilesystemOnly()) {
            throw new BuildException("only filesystem resources are supported");
        }
        if (resources == null) {
            resources = new Union();
        }
        resources.add(rc);
    }

    /**
     * Whether the file timestamp shall be preserved even if the file
     * is modified.
     *
     * @param b boolean
     * @since Ant 1.8.0
     */
    public void setPreserveLastModified(boolean b) {
        preserveLastModified = b;
    }

    /**
     * Whether the build should fail if nothing has been replaced.
     *
     * @param b boolean
     * @since Ant 1.8.0
     */
    public void setFailOnNoReplacements(boolean b) {
        failOnNoReplacements = b;
    }

    /**
     * Adds the token and value as first &lt;replacefilter&gt; element.
     * The token and value are always processed first.
     * @return a nested <code>Replacefilter</code> object to be configured.
     */
    private Replacefilter createPrimaryfilter() {
        Replacefilter filter = new Replacefilter();
        replacefilters.add(0, filter);
        return filter;
    }

    /**
     * Replace occurrences of str1 in StringBuffer str with str2.
     *
     * @param str StringBuilder
     * @param str1 String
     * @param str2 String
     */
    private void stringReplace(StringBuilder str, String str1, String str2) {
        int found = str.indexOf(str1);
        final int str1Length = str1.length();
        final int str2Length = str2.length();
        while (found >= 0) {
            str.replace(found, found + str1Length, str2);
            found = str.indexOf(str1, found + str2Length);
        }
    }

    /**
     * Sort keys by size so that tokens that are substrings of other
     * strings are tried later.
     *
     * @param props Properties
     */
    private Iterator<String> getOrderedIterator(Properties props) {
        List<String> keys = new ArrayList<>(props.stringPropertyNames());
        keys.sort(Comparator.comparingInt(String::length).reversed());
        return keys.iterator();
    }
}
