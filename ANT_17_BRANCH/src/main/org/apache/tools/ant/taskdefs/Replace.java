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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.StringUtils;

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

    private File src = null;
    private NestedString token = null;
    private NestedString value = new NestedString();

    private File propertyFile = null;
    private File replaceFilterFile = null;
    private Properties properties = null;
    private Vector replacefilters = new Vector();

    private File dir = null;

    private int fileCount;
    private int replaceCount;
    private boolean summary = false;

    /** The encoding used to read and write files - if null, uses default */
    private String encoding = null;

    /**
     * An inline string to use as the replacement text.
     */
    public class NestedString {

        private StringBuffer buf = new StringBuffer();

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
            return buf.toString();
        }
    }

    /**
     * A filter to apply.
     */
    public class Replacefilter {
        private String token;
        private String value;
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
                String message = "token is a mandatory attribute "
                    + "of replacefilter.";
                throw new BuildException(message);
            }

            if ("".equals(token)) {
                String message = "The token attribute must not be an empty "
                    + "string.";
                throw new BuildException(message);
            }

            //value and property are mutually exclusive attributes
            if ((value != null) && (property != null)) {
                String message = "Either value or property "
                    + "can be specified, but a replacefilter "
                    + "element cannot have both.";
                throw new BuildException(message);
            }

            if ((property != null)) {
                //the property attribute must have access to a property file
                if (propertyFile == null) {
                    String message = "The replacefilter's property attribute "
                        + "can only be used with the replacetask's "
                        + "propertyFile attribute.";
                    throw new BuildException(message);
                }

                //Make sure property exists in property file
                if (properties == null
                    || properties.getProperty(property) == null) {
                    String message = "property \"" + property
                        + "\" was not found in " + propertyFile.getPath();
                    throw new BuildException(message);
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
            } else if (value != null) {
                return value;
            } else if (Replace.this.value != null) {
                return Replace.this.value.getText();
            } else {
                //Default is empty string
                return "";
            }
        }

        /**
         * Set the token to replace.
         * @param token <code>String</code> token.
         */
        public void setToken(String token) {
            this.token = token;
        }

        /**
         * Get the string to search for.
         * @return current <code>String</code> token.
         */
        public String getToken() {
            return token;
        }

        /**
         * The replacement string; required if <code>property<code>
         * is not set.
         * @param value <code>String</code> value to replace.
         */
        public void setValue(String value) {
            this.value = value;
        }

        /**
         * Get replacement <code>String</code>.
         * @return replacement or null.
         */
        public String getValue() {
            return value;
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
         * another component will only apped to this StringBuffer.
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
            if (inputBuffer.length() > token.length()) {
                int pos = replace();
                pos = Math.max((inputBuffer.length() - token.length()), pos);
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
            // Avoid runtime problem on pre 1.4 when compiling post 1.4
            outputBuffer.append(inputBuffer.toString());
            inputBuffer.delete(0, inputBuffer.length());
        }

        /**
         * Performs the replace operation.
         * @return The position of the last character that was inserted as
         *         replacement.
         */
        private int replace() {
            int found = inputBuffer.toString().indexOf(token);
            int pos = -1;
            while (found >= 0) {
                inputBuffer.replace(found, found + token.length(),
                        replaceValue);
                pos = found + replaceValue.length();
                found = inputBuffer.toString().indexOf(token, pos);
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
    private class FileInput {
        private StringBuffer outputBuffer;
        private Reader reader;
        private char[] buffer;
        private static final int BUFF_SIZE = 4096;

        /**
         * Constructs the input component. Opens the file for reading.
         * @param source The file to read from.
         * @throws IOException When the file cannot be read from.
         */
        FileInput(File source) throws IOException {
            outputBuffer = new StringBuffer();
            buffer = new char[BUFF_SIZE];
            if (encoding == null) {
                reader = new BufferedReader(new FileReader(source));
            } else {
                reader = new BufferedReader(new InputStreamReader(
                        new FileInputStream(source), encoding));
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
            int bufferLength = 0;
            bufferLength = reader.read(buffer);
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
        void close() throws IOException {
            reader.close();
        }

        /**
         * Closes file but doesn't throw exception
         */
        void closeQuietly() {
            FileUtils.close(reader);
        }

    }

    /**
     * Component writing a file in chunks, taking the chunks from the
     * Replacefilter.
     * @since 1.7
     */
    private class FileOutput {
        private StringBuffer inputBuffer;
        private Writer writer;

        /**
         * Constructs the output component. Opens the file for writing.
         * @param out The file to read to.
         * @throws IOException When the file cannot be read from.
         */
        FileOutput(File out) throws IOException {
                if (encoding == null) {
                    writer = new BufferedWriter(new FileWriter(out));
                } else {
                    writer = new BufferedWriter(new OutputStreamWriter
                            (new FileOutputStream(out), encoding));
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
        void close() throws IOException {
            writer.close();
        }

        /**
         * Closes file but doesn't throw exception
         */
        void closeQuietly() {
            FileUtils.close(writer);
        }
    }

    /**
     * Do the execution.
     * @throws BuildException if we cant build
     */
    public void execute() throws BuildException {

        Vector savedFilters = (Vector) replacefilters.clone();
        Properties savedProperties =
            properties == null ? null : (Properties) properties.clone();

        if (token != null) {
            // line separators in values and tokens are "\n"
            // in order to compare with the file contents, replace them
            // as needed
            StringBuffer val = new StringBuffer(value.getText());
            stringReplace(val, "\r\n", "\n");
            stringReplace(val, "\n", StringUtils.LINE_SEP);
            StringBuffer tok = new StringBuffer(token.getText());
            stringReplace(tok, "\r\n", "\n");
            stringReplace(tok, "\n", StringUtils.LINE_SEP);
            Replacefilter firstFilter = createPrimaryfilter();
            firstFilter.setToken(tok.toString());
            firstFilter.setValue(val.toString());
        }

        try {
            if (replaceFilterFile != null) {
                Properties props = getProperties(replaceFilterFile);
                Enumeration e = props.keys();
                while (e.hasMoreElements()) {
                    String tok =  e.nextElement().toString();
                    Replacefilter replaceFilter = createReplacefilter();
                    replaceFilter.setToken(tok);
                    replaceFilter.setValue(props.getProperty(tok));
                }
            }

            validateAttributes();

            if (propertyFile != null) {
                properties = getProperties(propertyFile);
            }

            validateReplacefilters();
            fileCount = 0;
            replaceCount = 0;

            if (src != null) {
                processFile(src);
            }

            if (dir != null) {
                DirectoryScanner ds = super.getDirectoryScanner(dir);
                String[] srcs = ds.getIncludedFiles();

                for (int i = 0; i < srcs.length; i++) {
                    File file = new File(dir, srcs[i]);
                    processFile(file);
                }
            }

            if (summary) {
                log("Replaced " + replaceCount + " occurrences in "
                    + fileCount + " files.", Project.MSG_INFO);
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
        if (src == null && dir == null) {
            String message = "Either the file or the dir attribute "
                + "must be specified";
            throw new BuildException(message, getLocation());
        }
        if (propertyFile != null && !propertyFile.exists()) {
            String message = "Property file " + propertyFile.getPath()
                + " does not exist.";
            throw new BuildException(message, getLocation());
        }
        if (token == null && replacefilters.size() == 0) {
            String message = "Either token or a nested replacefilter "
                + "must be specified";
            throw new BuildException(message, getLocation());
        }
        if (token != null && "".equals(token.getText())) {
            String message = "The token attribute must not be an empty string.";
            throw new BuildException(message, getLocation());
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
        for (int i = 0; i < replacefilters.size(); i++) {
            Replacefilter element =
                (Replacefilter) replacefilters.elementAt(i);
            element.validate();
        }
    }

    /**
     * Load a properties file.
     * @param propertyFile the file to load the properties from.
     * @return loaded <code>Properties</code> object.
     * @throws BuildException if the file could not be found or read.
     */
    public Properties getProperties(File propertyFile) throws BuildException {
        Properties props = new Properties();

        FileInputStream in = null;
        try {
            in = new FileInputStream(propertyFile);
            props.load(in);
        } catch (FileNotFoundException e) {
            String message = "Property file (" + propertyFile.getPath()
                + ") not found.";
            throw new BuildException(message);
        } catch (IOException e) {
            String message = "Property file (" + propertyFile.getPath()
                + ") cannot be loaded.";
            throw new BuildException(message);
        } finally {
            FileUtils.close(in);
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

        File temp = null;
        FileInput in = null;
        FileOutput out = null;
        try {
            in = new FileInput(src);

            temp = FILE_UTILS.createTempFile("rep", ".tmp",
                    src.getParentFile(), false, true);
            out = new FileOutput(temp);

            int repCountStart = replaceCount;

            logFilterChain(src.getPath());

            out.setInputBuffer(buildFilterChain(in.getOutputBuffer()));

            while (in.readChunk()) {
                if (processFilterChain()) {
                    out.process();
                }
            }

            flushFilterChain();

            out.flush();
            in.close();
            in = null;
            out.close();
            out = null;

            boolean changes = (replaceCount != repCountStart);
            if (changes) {
                fileCount++;
                FILE_UTILS.rename(temp, src);
                temp = null;
            }
        } catch (IOException ioe) {
            throw new BuildException("IOException in " + src + " - "
                    + ioe.getClass().getName() + ":"
                    + ioe.getMessage(), ioe, getLocation());
        } finally {
            if (null != in) {
                in.closeQuietly();
            }
            if (null != out) {
                out.closeQuietly();
            }
            if (temp != null) {
                if (!temp.delete()) {
                    temp.deleteOnExit();
                }
            }
        }
    }

    /**
     * Flushes all filters.
     */
    private void flushFilterChain() {
        for (int i = 0; i < replacefilters.size(); i++) {
            Replacefilter filter = (Replacefilter) replacefilters.elementAt(i);
            filter.flush();
        }
    }

    /**
     * Performs the normal processing of the filters.
     * @return true if the filter chain produced new output.
     */
    private boolean processFilterChain() {
        for (int i = 0; i < replacefilters.size(); i++) {
            Replacefilter filter = (Replacefilter) replacefilters.elementAt(i);
            if (!filter.process()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Creates the chain of filters to operate.
     * @param inputBuffer <code>StringBuffer</code> containing the input for the
     *                    first filter.
     * @return <code>StringBuffer</code> containing the output of the last filter.
     */
    private StringBuffer buildFilterChain(StringBuffer inputBuffer) {
        StringBuffer buf = inputBuffer;
        for (int i = 0; i < replacefilters.size(); i++) {
            Replacefilter filter = (Replacefilter) replacefilters.elementAt(i);
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
        for (int i = 0; i < replacefilters.size(); i++) {
            Replacefilter filter = (Replacefilter) replacefilters.elementAt(i);
            log("Replacing in " + filename + ": " + filter.getToken()
                    + " --> " + filter.getReplaceValue(), Project.MSG_VERBOSE);
        }
    }
    /**
     * Set the source file; required unless <code>dir</code> is set.
     * @param file source <code>File</code>.
     */
    public void setFile(File file) {
        this.src = file;
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
        this.replaceFilterFile = replaceFilterFile;
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
     * <code>replacetoken</code> element or the <code>replacefilterfile</code>
     * attribute is used.
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
        this.propertyFile = propertyFile;
    }

    /**
     * Add a nested &lt;replacefilter&gt; element.
     * @return a nested <code>Replacefilter</code> object to be configured.
     */
    public Replacefilter createReplacefilter() {
        Replacefilter filter = new Replacefilter();
        replacefilters.addElement(filter);
        return filter;
    }

    /**
     * Adds the token and value as first &lt;replacefilter&gt; element.
     * The token and value are always processed first.
     * @return a nested <code>Replacefilter</code> object to be configured.
     */
    private Replacefilter createPrimaryfilter() {
        Replacefilter filter = new Replacefilter();
        replacefilters.insertElementAt(filter, 0);
        return filter;
    }

    /**
     * Replace occurrences of str1 in StringBuffer str with str2.
     */
    private void stringReplace(StringBuffer str, String str1, String str2) {
        int found = str.toString().indexOf(str1);
        while (found >= 0) {
            str.replace(found, found + str1.length(), str2);
            found = str.toString().indexOf(str1, found + str2.length());
        }
    }

}
