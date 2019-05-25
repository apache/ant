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
package org.apache.tools.ant.filters;

import java.io.IOException;
import java.io.Reader;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.condition.Os;
import org.apache.tools.ant.types.EnumeratedAttribute;

/**
 * Converts text to local OS formatting conventions, as well as repair text
 * damaged by misconfigured or misguided editors or file transfer programs.
 * <p>
 * This filter can take the following arguments:
 * </p>
 * <ul>
 * <li>eof</li>
 * <li>eol</li>
 * <li>fixlast</li>
 * <li>javafiles</li>
 * <li>tab</li>
 * <li>tablength</li>
 * </ul>
 * None of which are required.
 * <p>
 * This version generalises the handling of EOL characters, and allows for
 * CR-only line endings (the standard on Mac systems prior to OS X). Tab
 * handling has also been generalised to accommodate any tabwidth from 2 to 80,
 * inclusive. Importantly, it can leave untouched any literal TAB characters
 * embedded within Java string or character constants.
 * </p>
 * <p>
 * <em>Caution:</em> run with care on carefully formatted files. This may
 * sound obvious, but if you don't specify asis, presume that your files are
 * going to be modified. If "tabs" is "add" or "remove", whitespace characters
 * may be added or removed as necessary. Similarly, for EOLs, eol="asis"
 * actually means convert to your native O/S EOL convention while eol="crlf" or
 * cr="add" can result in CR characters being removed in one special case
 * accommodated, i.e., CRCRLF is regarded as a single EOL to handle cases where
 * other programs have converted CRLF into CRCRLF.
 *</p>
 * <p>
 * Example:
 * </p>
 * <pre>
 * &lt;&lt;fixcrlf tab=&quot;add&quot; eol=&quot;crlf&quot; eof=&quot;asis&quot;/&gt;
 * </pre>
 * Or:
 * <pre>
 * &lt;filterreader classname=&quot;org.apache.tools.ant.filters.FixCrLfFilter&quot;&gt;
 *   &lt;param eol=&quot;crlf&quot; tab=&quot;asis&quot;/&gt;
 *  &lt;/filterreader&gt;
 * </pre>
 */
public final class FixCrLfFilter extends BaseParamFilterReader implements ChainableReader {
    private static final int DEFAULT_TAB_LENGTH = 8;
    private static final int MIN_TAB_LENGTH = 2;
    private static final int MAX_TAB_LENGTH = 80;
    private static final char CTRLZ = '\u001A';

    private int tabLength = DEFAULT_TAB_LENGTH;

    private CrLf eol;

    private AddAsisRemove ctrlz;

    private AddAsisRemove tabs;

    private boolean javafiles = false;

    private boolean fixlast = true;

    private boolean initialized = false;

    /**
     * Constructor for "dummy" instances.
     *
     * @see BaseFilterReader#BaseFilterReader()
     */
    public FixCrLfFilter() {
        super();
    }

    /**
     * Create a new filtered reader.
     *
     * @param in
     *            A Reader object providing the underlying stream. Must not be
     *            <code>null</code>.
     * @throws IOException on error.
     */
    public FixCrLfFilter(final Reader in) throws IOException {
        super(in);
    }

    // Instance initializer: Executes just after the super() call in this
    // class's constructor.
    {
        tabs = AddAsisRemove.ASIS;
        if (Os.isFamily("mac") && !Os.isFamily("unix")) {
            ctrlz = AddAsisRemove.REMOVE;
            setEol(CrLf.MAC);
        } else if (Os.isFamily("dos")) {
            ctrlz = AddAsisRemove.ASIS;
            setEol(CrLf.DOS);
        } else {
            ctrlz = AddAsisRemove.REMOVE;
            setEol(CrLf.UNIX);
        }
    }

    /**
     * Create a new FixCrLfFilter using the passed in Reader for instantiation.
     *
     * @param rdr
     *            A Reader object providing the underlying stream. Must not be
     *            <code>null</code>.
     *
     * @return a new filter based on this configuration, but filtering the
     *         specified reader.
     */
    public Reader chain(final Reader rdr) {
        try {
            FixCrLfFilter newFilter = new FixCrLfFilter(rdr);

            newFilter.setJavafiles(getJavafiles());
            newFilter.setEol(getEol());
            newFilter.setTab(getTab());
            newFilter.setTablength(getTablength());
            newFilter.setEof(getEof());
            newFilter.setFixlast(getFixlast());
            newFilter.initInternalFilters();

            return newFilter;
        } catch (IOException e) {
            throw new BuildException(e);
        }
    }

    /**
     * Get how DOS EOF (control-z) characters are being handled.
     *
     * @return values:
     *         <ul>
     *         <li>add: ensure that there is an eof at the end of the file
     *         <li>asis: leave eof characters alone
     *         <li>remove: remove any eof character found at the end
     *         </ul>
     */
    public AddAsisRemove getEof() {
        // Return copy so that the call must call setEof() to change the state
        // of fixCRLF
        return ctrlz.newInstance();
    }

    /**
     * Get how EndOfLine characters are being handled.
     *
     * @return values:
     *         <ul>
     *         <li>asis: convert line endings to your O/S convention
     *         <li>cr: convert line endings to CR
     *         <li>lf: convert line endings to LF
     *         <li>crlf: convert line endings to CRLF
     *         </ul>
     */
    public CrLf getEol() {
        // Return copy so that the call must call setEol() to change the state
        // of fixCRLF
        return eol.newInstance();
    }

    /**
     * Get whether a missing EOL be added to the final line of the stream.
     *
     * @return true if a filtered file will always end with an EOL
     */
    public boolean getFixlast() {
        return fixlast;
    }

    /**
     * Get whether the stream is to be treated as though it contains Java
     * source.
     * <P>
     * This attribute is only used in association with the &quot;<i><b>tab</b></i>&quot;
     * attribute. Tabs found in Java literals are protected from changes by this
     * filter.
     *
     * @return true if whitespace in Java character and string literals is
     *         ignored.
     */
    public boolean getJavafiles() {
        return javafiles;
    }

    /**
     * Return how tab characters are being handled.
     *
     * @return values:
     *         <ul>
     *         <li>add: convert sequences of spaces which span a tab stop to
     *         tabs
     *         <li>asis: leave tab and space characters alone
     *         <li>remove: convert tabs to spaces
     *         </ul>
     */
    public AddAsisRemove getTab() {
        // Return copy so that the caller must call setTab() to change the state
        // of fixCRLF.
        return tabs.newInstance();
    }

    /**
     * Get the tab length to use.
     *
     * @return the length of tab in spaces
     */
    public int getTablength() {
        return tabLength;
    }

    private static String calculateEolString(CrLf eol) {
        // Calculate the EOL string per the current config
        if (eol == CrLf.CR || eol == CrLf.MAC) {
            return "\r";
        }
        if (eol == CrLf.CRLF || eol == CrLf.DOS) {
            return "\r\n";
        }
        // assume (eol == CrLf.LF || eol == CrLf.UNIX)
        return "\n";
    }

    /**
     * Wrap the input stream with the internal filters necessary to perform the
     * configuration settings.
     */
    private void initInternalFilters() {

        // If I'm removing an EOF character, do so first so that the other
        // filters don't see that character.
        in = (ctrlz == AddAsisRemove.REMOVE) ? new RemoveEofFilter(in) : in;

        // Change all EOL characters to match the calculated EOL string. If
        // configured to do so, append a trailing EOL so that the file ends on
        // a EOL.
        if (eol != CrLf.ASIS) {
            in = new NormalizeEolFilter(in, calculateEolString(eol), getFixlast());
        }

        if (tabs != AddAsisRemove.ASIS) {
            // If filtering Java source, prevent changes to whitespace in
            // character and string literals.
            if (getJavafiles()) {
                in = new MaskJavaTabLiteralsFilter(in);
            }
            // Add/Remove tabs
            in = (tabs == AddAsisRemove.ADD) ? new AddTabFilter(in, getTablength())
                    : new RemoveTabFilter(in, getTablength());
        }
        // Add missing EOF character
        in = (ctrlz == AddAsisRemove.ADD) ? new AddEofFilter(in) : in;
        initialized = true;
    }

    /**
     * Return the next character in the filtered stream.
     *
     * @return the next character in the resulting stream, or -1 if the end of
     *         the resulting stream has been reached.
     *
     * @exception IOException
     *                if the underlying stream throws an IOException during
     *                reading.
     */
    public synchronized int read() throws IOException {
        if (!initialized) {
            initInternalFilters();
        }
        return in.read();
    }

    /**
     * Specify how DOS EOF (control-z) characters are to be handled.
     *
     * @param attr
     *            valid values:
     *            <ul>
     *            <li>add: ensure that there is an eof at the end of the file
     *            <li>asis: leave eof characters alone
     *            <li>remove: remove any eof character found at the end
     *            </ul>
     */
    public void setEof(AddAsisRemove attr) {
        ctrlz = attr.resolve();
    }

    /**
     * Specify how end of line (EOL) characters are to be handled.
     *
     * @param attr
     *            valid values:
     *            <ul>
     *            <li>asis: convert line endings to your O/S convention
     *            <li>cr: convert line endings to CR
     *            <li>lf: convert line endings to LF
     *            <li>crlf: convert line endings to CRLF
     *            </ul>
     */
    public void setEol(CrLf attr) {
        eol = attr.resolve();
    }

    /**
     * Specify whether a missing EOL will be added to the final line of input.
     *
     * @param fixlast
     *            if true a missing EOL will be appended.
     */
    public void setFixlast(boolean fixlast) {
        this.fixlast = fixlast;
    }

    /**
     * Indicate whether this stream contains Java source.
     *
     * This attribute is only used in association with the &quot;<i><b>tab</b></i>&quot;
     * attribute.
     *
     * @param javafiles
     *            set to true to prevent this filter from changing tabs found in
     *            Java literals.
     */
    public void setJavafiles(boolean javafiles) {
        this.javafiles = javafiles;
    }

    /**
     * Specify how tab characters are to be handled.
     *
     * @param attr
     *            valid values:
     *            <ul>
     *            <li>add: convert sequences of spaces which span a tab stop to
     *            tabs
     *            <li>asis: leave tab and space characters alone
     *            <li>remove: convert tabs to spaces
     *            </ul>
     */
    public void setTab(AddAsisRemove attr) {
        tabs = attr.resolve();
    }

    /**
     * Specify tab length in characters.
     *
     * @param tabLength
     *            specify the length of tab in spaces. Valid values are between
     *            2 and 80 inclusive. The default for this parameter is 8.
     * @throws IOException on error.
     */
    public void setTablength(int tabLength) throws IOException {
        if (tabLength < MIN_TAB_LENGTH
            || tabLength > MAX_TAB_LENGTH) {
            throw new IOException(
                "tablength must be between " + MIN_TAB_LENGTH
                + " and " + MAX_TAB_LENGTH);
        }
        this.tabLength = tabLength;
    }

    /**
     * This filter reader redirects all read I/O methods through its own read()
     * method.
     *
     * <P>
     * The input stream is already buffered by the copy task so this doesn't
     * significantly impact performance while it makes writing the individual
     * fix filters much easier.
     * </P>
     */
    private static class SimpleFilterReader extends Reader {
        private static final int PREEMPT_BUFFER_LENGTH = 16;
        private Reader in;

        private int[] preempt = new int[PREEMPT_BUFFER_LENGTH];

        private int preemptIndex = 0;

        public SimpleFilterReader(Reader in) {
            this.in = in;
        }

        public void push(char c) {
            push((int) c);
        }

        public void push(int c) {
            try {
                preempt[preemptIndex++] = c;
            } catch (ArrayIndexOutOfBoundsException e) {
                int[] p2 = new int[preempt.length * 2];
                System.arraycopy(preempt, 0, p2, 0, preempt.length);
                preempt = p2;
                push(c);
            }
        }

        public void push(char[] cs, int start, int length) {
            for (int i = start + length - 1; i >= start;) {
                push(cs[i--]);
            }
        }

        public void push(char[] cs) {
            push(cs, 0, cs.length);
        }

        /**
         * Does this filter want to block edits on the last character returned
         * by read()?
         */
        public boolean editsBlocked() {
            return in instanceof SimpleFilterReader && ((SimpleFilterReader) in).editsBlocked();
        }

        public int read() throws IOException {
            return preemptIndex > 0 ? preempt[--preemptIndex] : in.read();
        }

        public void close() throws IOException {
            in.close();
        }

        public void reset() throws IOException {
            in.reset();
        }

        public boolean markSupported() {
            return in.markSupported();
        }

        public boolean ready() throws IOException {
            return in.ready();
        }

        public void mark(int i) throws IOException {
            in.mark(i);
        }

        public long skip(long i) throws IOException {
            return in.skip(i);
        }

        public int read(char[] buf) throws IOException {
            return read(buf, 0, buf.length);
        }

        public int read(char[] buf, int start, int length) throws IOException {
            int count = 0;
            int c = 0;

            // CheckStyle:InnerAssignment OFF - leave alone
            while (length-- > 0 && (c = this.read()) != -1) {
                buf[start++] = (char) c;
                count++;
            }
            // if at EOF with no characters in the buffer, return EOF
            return (count == 0 && c == -1) ? -1 : count;
        }
    }

    private static class MaskJavaTabLiteralsFilter extends SimpleFilterReader {
        private boolean editsBlocked = false;

        private static final int JAVA = 1;

        private static final int IN_CHAR_CONST = 2;

        private static final int IN_STR_CONST = 3;

        private static final int IN_SINGLE_COMMENT = 4;

        private static final int IN_MULTI_COMMENT = 5;

        private static final int TRANS_TO_COMMENT = 6;

        private static final int TRANS_FROM_MULTI = 8;

        private int state;

        public MaskJavaTabLiteralsFilter(Reader in) {
            super(in);
            state = JAVA;
        }

        public boolean editsBlocked() {
            return editsBlocked || super.editsBlocked();
        }

        public int read() throws IOException {
            int thisChar = super.read();
            // Mask, block from being edited, all characters in constants.
            editsBlocked = (state == IN_CHAR_CONST || state == IN_STR_CONST);

            switch (state) {
            case JAVA:
                // The current character is always emitted.
                switch (thisChar) {
                case '\'':
                    state = IN_CHAR_CONST;
                    break;
                case '"':
                    state = IN_STR_CONST;
                    break;
                case '/':
                    state = TRANS_TO_COMMENT;
                    break;
                default:
                    // Fall tru
                }
                break;
            case IN_CHAR_CONST:
                switch (thisChar) {
                case '\'':
                    state = JAVA;
                    break;
                default:
                    // Fall tru
                }
                break;
            case IN_STR_CONST:
                switch (thisChar) {
                case '"':
                    state = JAVA;
                    break;
                default:
                    // Fall tru
                }
                break;
            case IN_SINGLE_COMMENT:
                // The current character is always emitted.
                switch (thisChar) {
                case '\n':
                case '\r': // EOL
                    state = JAVA;
                    break;
                default:
                    // Fall tru
                }
                break;
            case IN_MULTI_COMMENT:
                // The current character is always emitted.
                switch (thisChar) {
                case '*':
                    state = TRANS_FROM_MULTI;
                    break;
                default:
                    // Fall tru
                }
                break;
            case TRANS_TO_COMMENT:
                // The current character is always emitted.
                switch (thisChar) {
                case '*':
                    state = IN_MULTI_COMMENT;
                    break;
                case '/':
                    state = IN_SINGLE_COMMENT;
                    break;
                case '\'':
                    state = IN_CHAR_CONST;
                    break;
                case '"':
                    state = IN_STR_CONST;
                    break;
                default:
                    state = JAVA;
                }
                break;
            case TRANS_FROM_MULTI:
                // The current character is always emitted.
                switch (thisChar) {
                case '/':
                    state = JAVA;
                    break;
                default:
                    // Fall tru
                }
                break;
            default:
                // Fall tru
            }
            return thisChar;
        }
    }

    private static class NormalizeEolFilter extends SimpleFilterReader {
        private boolean previousWasEOL;

        private boolean fixLast;

        private int normalizedEOL = 0;

        private char[] eol = null;

        public NormalizeEolFilter(Reader in, String eolString, boolean fixLast) {
            super(in);
            eol = eolString.toCharArray();
            this.fixLast = fixLast;
        }

        public int read() throws IOException {
            int thisChar = super.read();

            if (normalizedEOL == 0) {
                int numEOL = 0;
                boolean atEnd = false;
                switch (thisChar) {
                case CTRLZ:
                    int c = super.read();
                    if (c == -1) {
                        atEnd = true;
                        if (fixLast && !previousWasEOL) {
                            numEOL = 1;
                            push(thisChar);
                        }
                    } else {
                        push(c);
                    }
                    break;
                case -1:
                    atEnd = true;
                    if (fixLast && !previousWasEOL) {
                        numEOL = 1;
                    }
                    break;
                case '\n':
                    // EOL was "\n"
                    numEOL = 1;
                    break;
                case '\r':
                    numEOL = 1;
                    int c1 = super.read();
                    int c2 = super.read();

                    if (c1 == '\r' && c2 == '\n') {
                        // EOL was "\r\r\n"
                    } else if (c1 == '\r') {
                        // EOL was "\r\r" - handle as two consecutive "\r" and
                        // "\r"
                        numEOL = 2;
                        push(c2);
                    } else if (c1 == '\n') {
                        // EOL was "\r\n"
                        push(c2);
                    } else {
                        // EOL was "\r"
                        push(c2);
                        push(c1);
                    }
                default:
                    // Fall tru
                }
                if (numEOL > 0) {
                    while (numEOL-- > 0) {
                        push(eol);
                        normalizedEOL += eol.length;
                    }
                    previousWasEOL = true;
                    thisChar = read();
                } else if (!atEnd) {
                    previousWasEOL = false;
                }
            } else {
                normalizedEOL--;
            }
            return thisChar;
        }
    }

    private static class AddEofFilter extends SimpleFilterReader {
        private int lastChar = -1;

        public AddEofFilter(Reader in) {
            super(in);
        }

        public int read() throws IOException {
            int thisChar = super.read();

            // if source is EOF but last character was NOT ctrl-z, return ctrl-z
            if (thisChar == -1) {
                if (lastChar != CTRLZ) {
                    lastChar = CTRLZ;
                    return lastChar;
                }
            } else {
                lastChar = thisChar;
            }
            return thisChar;
        }
    }

    private static class RemoveEofFilter extends SimpleFilterReader {
        private int lookAhead = -1;

        public RemoveEofFilter(Reader in) {
            super(in);

            try {
                lookAhead = in.read();
            } catch (IOException e) {
                lookAhead = -1;
            }
        }

        public int read() throws IOException {
            int lookAhead2 = super.read();

            // If source at EOF and lookAhead is ctrl-z, return EOF (NOT ctrl-z)
            if (lookAhead2 == -1 && lookAhead == CTRLZ) {
                return -1;
            }
            // Return current look-ahead
            int i = lookAhead;
            lookAhead = lookAhead2;
            return i;
        }
    }

    private static class AddTabFilter extends SimpleFilterReader {
        private int columnNumber = 0;

        private int tabLength = 0;

        public AddTabFilter(Reader in, int tabLength) {
            super(in);
            this.tabLength = tabLength;
        }

        public int read() throws IOException {
            int c = super.read();

            switch (c) {
            case '\r':
            case '\n':
                columnNumber = 0;
                break;
            case ' ':
                columnNumber++;
                if (!editsBlocked()) {
                    int colNextTab = ((columnNumber + tabLength - 1) / tabLength) * tabLength;
                    int countSpaces = 1;
                    int numTabs = 0;

                    scanWhitespace: while ((c = super.read()) != -1) {
                        switch (c) {
                        case ' ':
                            if (++columnNumber == colNextTab) {
                                numTabs++;
                                countSpaces = 0;
                                colNextTab += tabLength;
                            } else {
                                countSpaces++;
                            }
                            break;
                        case '\t':
                            columnNumber = colNextTab;
                            numTabs++;
                            countSpaces = 0;
                            colNextTab += tabLength;
                            break;
                        default:
                            push(c);
                            break scanWhitespace;
                        }
                    }
                    while (countSpaces-- > 0) {
                        push(' ');
                        columnNumber--;
                    }
                    while (numTabs-- > 0) {
                        push('\t');
                        columnNumber -= tabLength;
                    }
                    c = super.read();
                    switch (c) {
                    case ' ':
                        columnNumber++;
                        break;
                    case '\t':
                        columnNumber += tabLength;
                        break;
                    default:
                        // Fall tru
                    }
                }
                break;
            case '\t':
                columnNumber = ((columnNumber + tabLength - 1) / tabLength) * tabLength;
                break;
            default:
                columnNumber++;
            }
            return c;
        }
    }

    private static class RemoveTabFilter extends SimpleFilterReader {
        private int columnNumber = 0;

        private int tabLength = 0;

        public RemoveTabFilter(Reader in, int tabLength) {
            super(in);

            this.tabLength = tabLength;
        }

        public int read() throws IOException {
            int c = super.read();

            switch (c) {
            case '\r':
            case '\n':
                columnNumber = 0;
                break;
            case '\t':
                int width = tabLength - columnNumber % tabLength;

                if (!editsBlocked()) {
                    for (; width > 1; width--) {
                        push(' ');
                    }
                    c = ' ';
                }
                columnNumber += width;
                break;
            default:
                columnNumber++;
            }
            return c;
        }
    }

    /**
     * Enumerated attribute with the values "asis", "add" and "remove".
     */
    public static class AddAsisRemove extends EnumeratedAttribute {
        private static final AddAsisRemove ASIS = newInstance("asis");

        private static final AddAsisRemove ADD = newInstance("add");

        private static final AddAsisRemove REMOVE = newInstance("remove");

        /** {@inheritDoc}. */
        public String[] getValues() {
            return new String[] {"add", "asis", "remove"};
        }

        /**
         * Equality depending in the index.
         * @param other the object to test equality against.
         * @return true if the object has the same index as this.
         */
        public boolean equals(Object other) {
            return other instanceof AddAsisRemove
                    && getIndex() == ((AddAsisRemove) other).getIndex();
        }

        /**
         * Hashcode depending on the index.
         * @return the index as the hashcode.
         */
        public int hashCode() {
            return getIndex();
        }

        AddAsisRemove resolve() throws IllegalStateException {
            if (this.equals(ASIS)) {
                return ASIS;
            }
            if (this.equals(ADD)) {
                return ADD;
            }
            if (this.equals(REMOVE)) {
                return REMOVE;
            }
            throw new IllegalStateException("No replacement for " + this);
        }

        // Works like clone() but doesn't show up in the Javadocs
        private AddAsisRemove newInstance() {
            return newInstance(getValue());
        }

        /**
         * Create an instance of this enumerated value based on the string value.
         * @param value the value to use.
         * @return an enumerated instance.
         */
        public static AddAsisRemove newInstance(String value) {
            AddAsisRemove a = new AddAsisRemove();
            a.setValue(value);
            return a;
        }
    }

    /**
     * Enumerated attribute with the values "asis", "cr", "lf" and "crlf".
     */
    public static class CrLf extends EnumeratedAttribute {
        private static final CrLf ASIS = newInstance("asis");

        private static final CrLf CR = newInstance("cr");

        private static final CrLf CRLF = newInstance("crlf");

        private static final CrLf DOS = newInstance("dos");

        private static final CrLf LF = newInstance("lf");

        private static final CrLf MAC = newInstance("mac");

        private static final CrLf UNIX = newInstance("unix");

        /**
         * @see EnumeratedAttribute#getValues
         * {@inheritDoc}.
         */
        public String[] getValues() {
            return new String[] {"asis", "cr", "lf", "crlf", "mac", "unix", "dos"};
        }

        /**
         * Equality depending in the index.
         * @param other the object to test equality against.
         * @return true if the object has the same index as this.
         */
        public boolean equals(Object other) {
            return other instanceof CrLf && getIndex() == ((CrLf) other).getIndex();
        }

        /**
         * Hashcode depending on the index.
         * @return the index as the hashcode.
         */
        public int hashCode() {
            return getIndex();
        }

        CrLf resolve() {
            if (this.equals(ASIS)) {
                return ASIS;
            }
            if (this.equals(CR) || this.equals(MAC)) {
                return CR;
            }
            if (this.equals(CRLF) || this.equals(DOS)) {
                return CRLF;
            }
            if (this.equals(LF) || this.equals(UNIX)) {
                return LF;
            }
            throw new IllegalStateException("No replacement for " + this);
        }

        // Works like clone() but doesn't show up in the Javadocs
        private CrLf newInstance() {
            return newInstance(getValue());
        }

        /**
         * Create an instance of this enumerated value based on the string value.
         * @param value the value to use.
         * @return an enumerated instance.
         */
        public static CrLf newInstance(String value) {
            CrLf c = new CrLf();
            c.setValue(value);
            return c;
        }
    }
}
