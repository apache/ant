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

/*
 * This package is based on the work done by Timothy Gerard Endres
 * (time@ice.com) to whom the Ant project is very grateful for his great code.
 */

package org.apache.tools.tar;

import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.apache.tools.zip.ZipEncoding;
import org.apache.tools.zip.ZipEncodingHelper;

/**
 * The TarInputStream reads a UNIX tar archive as an InputStream.
 * methods are provided to position at each successive entry in
 * the archive, and the read each entry as a normal input stream
 * using read().
 *
 */
public class TarInputStream extends FilterInputStream {
    private static final int SMALL_BUFFER_SIZE = 256;
    private static final int BUFFER_SIZE = 8 * 1024;
    private static final int LARGE_BUFFER_SIZE = 32 * 1024;
    private static final int BYTE_MASK = 0xFF;

    private final byte[] SKIP_BUF = new byte[BUFFER_SIZE];
    private final byte[] SMALL_BUF = new byte[SMALL_BUFFER_SIZE];

    // CheckStyle:VisibilityModifier OFF - bc
    protected boolean debug;
    protected boolean hasHitEOF;
    protected long entrySize;
    protected long entryOffset;
    protected byte[] readBuf;
    protected TarBuffer buffer;
    protected TarEntry currEntry;

    /**
     * This contents of this array is not used at all in this class,
     * it is only here to avoid repeated object creation during calls
     * to the no-arg read method.
     */
    protected byte[] oneBuf;

    // CheckStyle:VisibilityModifier ON

    private final ZipEncoding encoding;

    /**
     * Constructor for TarInputStream.
     * @param is the input stream to use
     */
    public TarInputStream(InputStream is) {
        this(is, TarBuffer.DEFAULT_BLKSIZE, TarBuffer.DEFAULT_RCDSIZE);
    }

    /**
     * Constructor for TarInputStream.
     * @param is the input stream to use
     * @param encoding name of the encoding to use for file names
     */
    public TarInputStream(InputStream is, String encoding) {
        this(is, TarBuffer.DEFAULT_BLKSIZE, TarBuffer.DEFAULT_RCDSIZE, encoding);
    }

    /**
     * Constructor for TarInputStream.
     * @param is the input stream to use
     * @param blockSize the block size to use
     */
    public TarInputStream(InputStream is, int blockSize) {
        this(is, blockSize, TarBuffer.DEFAULT_RCDSIZE);
    }

    /**
     * Constructor for TarInputStream.
     * @param is the input stream to use
     * @param blockSize the block size to use
     * @param encoding name of the encoding to use for file names
     */
    public TarInputStream(InputStream is, int blockSize, String encoding) {
        this(is, blockSize, TarBuffer.DEFAULT_RCDSIZE, encoding);
    }

    /**
     * Constructor for TarInputStream.
     * @param is the input stream to use
     * @param blockSize the block size to use
     * @param recordSize the record size to use
     */
    public TarInputStream(InputStream is, int blockSize, int recordSize) {
        this(is, blockSize, recordSize, null);
    }

    /**
     * Constructor for TarInputStream.
     * @param is the input stream to use
     * @param blockSize the block size to use
     * @param recordSize the record size to use
     * @param encoding name of the encoding to use for file names
     */
    public TarInputStream(InputStream is, int blockSize, int recordSize,
                          String encoding) {
        super(is);
        this.buffer = new TarBuffer(is, blockSize, recordSize);
        this.readBuf = null;
        this.oneBuf = new byte[1];
        this.debug = false;
        this.hasHitEOF = false;
        this.encoding = ZipEncodingHelper.getZipEncoding(encoding);
    }

    /**
     * Sets the debugging flag.
     *
     * @param debug True to turn on debugging.
     */
    public void setDebug(boolean debug) {
        this.debug = debug;
        buffer.setDebug(debug);
    }

    /**
     * Closes this stream. Calls the TarBuffer's close() method.
     * @throws IOException on error
     */
    @Override
    public void close() throws IOException {
        buffer.close();
    }

    /**
     * Get the record size being used by this stream's TarBuffer.
     *
     * @return The TarBuffer record size.
     */
    public int getRecordSize() {
        return buffer.getRecordSize();
    }

    /**
     * Get the available data that can be read from the current
     * entry in the archive. This does not indicate how much data
     * is left in the entire archive, only in the current entry.
     * This value is determined from the entry's size header field
     * and the amount of data already read from the current entry.
     * Integer.MAX_VALUE is returned in case more than Integer.MAX_VALUE
     * bytes are left in the current entry in the archive.
     *
     * @return The number of available bytes for the current entry.
     * @throws IOException for signature
     */
    @Override
    public int available() throws IOException {
        if (isDirectory()) {
            return 0;
        }
        if (entrySize - entryOffset > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) (entrySize - entryOffset);
    }

    /**
     * Skip bytes in the input buffer. This skips bytes in the
     * current entry's data, not the entire archive, and will
     * stop at the end of the current entry's data if the number
     * to skip extends beyond that point.
     *
     * @param numToSkip The number of bytes to skip.
     * @return the number actually skipped
     * @throws IOException on error
     */
    @Override
    public long skip(long numToSkip) throws IOException {
        if (numToSkip <= 0 || isDirectory()) {
            return 0;
        }
        // REVIEW
        // This is horribly inefficient, but it ensures that we
        // properly skip over bytes via the TarBuffer...
        //
        long skip = numToSkip;
        while (skip > 0) {
            int realSkip = (int) (skip > SKIP_BUF.length
                                  ? SKIP_BUF.length : skip);
            int numRead = read(SKIP_BUF, 0, realSkip);
            if (numRead == -1) {
                break;
            }
            skip -= numRead;
        }
        return (numToSkip - skip);
    }

    /**
     * Since we do not support marking just yet, we return false.
     *
     * @return False.
     */
    @Override
    public boolean markSupported() {
        return false;
    }

    /**
     * Since we do not support marking just yet, we do nothing.
     *
     * @param markLimit The limit to mark.
     */
    @Override
    public void mark(int markLimit) {
    }

    /**
     * Since we do not support marking just yet, we do nothing.
     */
    @Override
    public void reset() {
    }

    /**
     * Get the next entry in this tar archive. This will skip
     * over any remaining data in the current entry, if there
     * is one, and place the input stream at the header of the
     * next entry, and read the header and instantiate a new
     * TarEntry from the header bytes and return that entry.
     * If there are no more entries in the archive, null will
     * be returned to indicate that the end of the archive has
     * been reached.
     *
     * @return The next TarEntry in the archive, or null.
     * @throws IOException on error
     */
    public TarEntry getNextEntry() throws IOException {
        if (hasHitEOF) {
            return null;
        }

        if (currEntry != null) {
            long numToSkip = entrySize - entryOffset;

            if (debug) {
                System.err.println("TarInputStream: SKIP currENTRY '"
                        + currEntry.getName() + "' SZ "
                        + entrySize + " OFF "
                        + entryOffset + "  skipping "
                        + numToSkip + " bytes");
            }

            while (numToSkip > 0) {
                long skipped = skip(numToSkip);
                if (skipped <= 0) {
                    throw new IOException("failed to skip current tar"
                                               + " entry");
                }
                numToSkip -= skipped;
            }

            readBuf = null;
        }

        byte[] headerBuf = getRecord();

        if (hasHitEOF) {
            currEntry = null;
            return null;
        }

        try {
            currEntry = new TarEntry(headerBuf, encoding);
        } catch (IllegalArgumentException e) {
            throw new IOException("Error detected parsing the header", e);
        }
        if (debug) {
            System.err.println("TarInputStream: SET CURRENTRY '"
                               + currEntry.getName()
                               + "' size = "
                               + currEntry.getSize());
        }

        entryOffset = 0;
        entrySize = currEntry.getSize();

        if (currEntry.isGNULongLinkEntry()) {
            byte[] longLinkData = getLongNameData();
            if (longLinkData == null) {
                // Bugzilla: 40334
                // Malformed tar file - long link entry name not followed by
                // entry
                return null;
            }
            currEntry.setLinkName(encoding.decode(longLinkData));
        }

        if (currEntry.isGNULongNameEntry()) {
            byte[] longNameData = getLongNameData();
            if (longNameData == null) {
                // Bugzilla: 40334
                // Malformed tar file - long entry name not followed by
                // entry
                return null;
            }
            currEntry.setName(encoding.decode(longNameData));
        }

        if (currEntry.isPaxHeader()) { // Process Pax headers
            paxHeaders();
        }

        if (currEntry.isGNUSparse()) { // Process sparse files
            readGNUSparse();
        }

        // If the size of the next element in the archive has changed
        // due to a new size being reported in the posix header
        // information, we update entrySize here so that it contains
        // the correct value.
        entrySize = currEntry.getSize();
        return currEntry;
    }

    /**
     * Get the next entry in this tar archive as longname data.
     *
     * @return The next entry in the archive as longname data, or null.
     * @throws IOException on error
     */
    protected byte[] getLongNameData() throws IOException {
        // read in the name
        ByteArrayOutputStream longName = new ByteArrayOutputStream();
        int length = 0;
        while ((length = read(SMALL_BUF)) >= 0) {
            longName.write(SMALL_BUF, 0, length);
        }
        getNextEntry();
        if (currEntry == null) {
            // Bugzilla: 40334
            // Malformed tar file - long entry name not followed by entry
            return null;
        }
        byte[] longNameData = longName.toByteArray();
        // remove trailing null terminator(s)
        length = longNameData.length;
        while (length > 0 && longNameData[length - 1] == 0) {
            --length;
        }
        if (length != longNameData.length) {
            byte[] l = new byte[length];
            System.arraycopy(longNameData, 0, l, 0, length);
            longNameData = l;
        }
        return longNameData;
    }

    /**
     * Get the next record in this tar archive. This will skip
     * over any remaining data in the current entry, if there
     * is one, and place the input stream at the header of the
     * next entry.
     * If there are no more entries in the archive, null will
     * be returned to indicate that the end of the archive has
     * been reached.
     *
     * @return The next header in the archive, or null.
     * @throws IOException on error
     */
    private byte[] getRecord() throws IOException {
        if (hasHitEOF) {
            return null;
        }

        byte[] headerBuf = buffer.readRecord();

        if (headerBuf == null) {
            if (debug) {
                System.err.println("READ NULL RECORD");
            }
            hasHitEOF = true;
        } else if (buffer.isEOFRecord(headerBuf)) {
            if (debug) {
                System.err.println("READ EOF RECORD");
            }
            hasHitEOF = true;
        }

        return hasHitEOF ? null : headerBuf;
    }

    private void paxHeaders() throws IOException {
        Map<String, String> headers = parsePaxHeaders(this);
        getNextEntry(); // Get the actual file entry
        applyPaxHeadersToCurrentEntry(headers);
    }

    Map<String, String> parsePaxHeaders(InputStream i) throws IOException {
        Map<String, String> headers = new HashMap<>();
        // Format is "length keyword=value\n";
        while (true) { // get length
            int ch;
            int len = 0;
            int read = 0;
            while ((ch = i.read()) != -1) {
                read++;
                if (ch == ' ') { // End of length string
                    // Get keyword
                    ByteArrayOutputStream coll = new ByteArrayOutputStream();
                    while ((ch = i.read()) != -1) {
                        read++;
                        if (ch == '=') { // end of keyword
                            String keyword = coll.toString("UTF-8");
                            // Get rest of entry
                            final int restLen = len - read;
                            ByteArrayOutputStream bos = new ByteArrayOutputStream();
                            int got = 0;
                            while (got < restLen && (ch = i.read()) != -1) {
                                bos.write((byte) ch);
                                got++;
                            }
                            bos.close();
                            if (got != restLen) {
                                throw new IOException("Failed to read "
                                                      + "Paxheader. Expected "
                                                      + restLen
                                                      + " bytes, read "
                                                      + got);
                            }
                            byte[] rest = bos.toByteArray();
                            // Drop trailing NL
                            String value = new String(rest, 0,
                                                      restLen - 1, StandardCharsets.UTF_8);
                            headers.put(keyword, value);
                            break;
                        }
                        coll.write((byte) ch);
                    }
                    break; // Processed single header
                }
                len *= 10;
                len += ch - '0';
            }
            if (ch == -1) { // EOF
                break;
            }
        }
        return headers;
    }

    private void applyPaxHeadersToCurrentEntry(Map<String, String> headers) {
        /*
         * The following headers are defined for Pax.
         * atime, ctime, charset: cannot use these without changing TarEntry fields
         * mtime
         * comment
         * gid, gname
         * linkpath
         * size
         * uid,uname
         * SCHILY.devminor, SCHILY.devmajor: don't have setters/getters for those
         */
        headers.forEach((key, val) -> {
            switch (key) {
                case "path":
                    currEntry.setName(val);
                    break;
                case "linkpath":
                    currEntry.setLinkName(val);
                    break;
                case "gid":
                    currEntry.setGroupId(Long.parseLong(val));
                    break;
                case "gname":
                    currEntry.setGroupName(val);
                    break;
                case "uid":
                    currEntry.setUserId(Long.parseLong(val));
                    break;
                case "uname":
                    currEntry.setUserName(val);
                    break;
                case "size":
                    currEntry.setSize(Long.parseLong(val));
                    break;
                case "mtime":
                    currEntry.setModTime((long) (Double.parseDouble(val) * 1000));
                    break;
                case "SCHILY.devminor":
                    currEntry.setDevMinor(Integer.parseInt(val));
                    break;
                case "SCHILY.devmajor":
                    currEntry.setDevMajor(Integer.parseInt(val));
                    break;
            }
        });
    }

    /**
     * Adds the sparse chunks from the current entry to the sparse chunks,
     * including any additional sparse entries following the current entry.
     *
     * @throws IOException on error
     *
     * @todo Sparse files get not yet really processed.
     */
    private void readGNUSparse() throws IOException {
        /* we do not really process sparse files yet
        sparses = new ArrayList();
        sparses.addAll(currEntry.getSparses());
        */
        if (currEntry.isExtended()) {
            TarArchiveSparseEntry entry;
            do {
                byte[] headerBuf = getRecord();
                if (hasHitEOF) {
                    currEntry = null;
                    break;
                }
                entry = new TarArchiveSparseEntry(headerBuf);
                /* we do not really process sparse files yet
                sparses.addAll(entry.getSparses());
                */
            } while (entry.isExtended());
        }
    }

    /**
     * Reads a byte from the current tar archive entry.
     *
     * This method simply calls read(byte[], int, int).
     *
     * @return The byte read, or -1 at EOF.
     * @throws IOException on error
     */
    @Override
    public int read() throws IOException {
        int num = read(oneBuf, 0, 1);
        return num == -1 ? -1 : (oneBuf[0]) & BYTE_MASK;
    }

    /**
     * Reads bytes from the current tar archive entry.
     *
     * This method is aware of the boundaries of the current
     * entry in the archive and will deal with them as if they
     * were this stream's start and EOF.
     *
     * @param buf The buffer into which to place bytes read.
     * @param offset The offset at which to place bytes read.
     * @param numToRead The number of bytes to read.
     * @return The number of bytes read, or -1 at EOF.
     * @throws IOException on error
     */
    @Override
    public int read(byte[] buf, int offset, int numToRead) throws IOException {
        int totalRead = 0;

        if (entryOffset >= entrySize || isDirectory()) {
            return -1;
        }

        if ((numToRead + entryOffset) > entrySize) {
            numToRead = (int) (entrySize - entryOffset);
        }

        if (readBuf != null) {
            int sz = (numToRead > readBuf.length) ? readBuf.length
                    : numToRead;

            System.arraycopy(readBuf, 0, buf, offset, sz);

            if (sz >= readBuf.length) {
                readBuf = null;
            } else {
                int newLen = readBuf.length - sz;
                byte[] newBuf = new byte[newLen];

                System.arraycopy(readBuf, sz, newBuf, 0, newLen);

                readBuf = newBuf;
            }

            totalRead += sz;
            numToRead -= sz;
            offset += sz;
        }

        while (numToRead > 0) {
            byte[] rec = buffer.readRecord();

            if (rec == null) {
                // Unexpected EOF!
                throw new IOException("unexpected EOF with " + numToRead
                        + " bytes unread");
            }

            int sz = numToRead;
            int recLen = rec.length;

            if (recLen > sz) {
                System.arraycopy(rec, 0, buf, offset, sz);

                readBuf = new byte[recLen - sz];

                System.arraycopy(rec, sz, readBuf, 0, recLen - sz);
            } else {
                sz = recLen;

                System.arraycopy(rec, 0, buf, offset, recLen);
            }

            totalRead += sz;
            numToRead -= sz;
            offset += sz;
        }

        entryOffset += totalRead;

        return totalRead;
    }

    /**
     * Copies the contents of the current tar archive entry directly into
     * an output stream.
     *
     * @param out The OutputStream into which to write the entry's data.
     * @throws IOException on error
     */
    public void copyEntryContents(OutputStream out) throws IOException {
        byte[] buf = new byte[LARGE_BUFFER_SIZE];

        while (true) {
            int numRead = read(buf, 0, buf.length);

            if (numRead == -1) {
                break;
            }

            out.write(buf, 0, numRead);
        }
    }

    /**
     * Whether this class is able to read the given entry.
     *
     * <p>May return false if the current entry is a sparse file.</p>
     *
     * @param te TarEntry
     * @return boolean
     */
    public boolean canReadEntryData(TarEntry te) {
        return !te.isGNUSparse();
    }

    private boolean isDirectory() {
        return currEntry != null && currEntry.isDirectory();
    }
}

