/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2002 The Apache Software Foundation.  All rights
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

/*
 * This package is based on the work done by Timothy Gerard Endres
 * (time@ice.com) to whom the Ant project is very grateful for his great code.
 */

package org.apache.tools.tar;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * The TarInputStream reads a UNIX tar archive as an InputStream.
 * methods are provided to position at each successive entry in
 * the archive, and the read each entry as a normal input stream
 * using read().
 *
 * @author Timothy Gerard Endres <a href="mailto:time@ice.com">time@ice.com</a>
 * @author Stefano Mazzocchi <a href="mailto:stefano@apache.org">stefano@apache.org</a>
 */
public class TarInputStream extends FilterInputStream {

    protected boolean debug;
    protected boolean hasHitEOF;
    protected int entrySize;
    protected int entryOffset;
    protected byte[] oneBuf;
    protected byte[] readBuf;
    protected TarBuffer buffer;
    protected TarEntry currEntry;
    private boolean v7Format;
    
    public TarInputStream(InputStream is) {
        this(is, TarBuffer.DEFAULT_BLKSIZE, TarBuffer.DEFAULT_RCDSIZE);
    }

    public TarInputStream(InputStream is, int blockSize) {
        this(is, blockSize, TarBuffer.DEFAULT_RCDSIZE);
    }

    public TarInputStream(InputStream is, int blockSize, int recordSize) {
        super(is);

        this.buffer = new TarBuffer(is, blockSize, recordSize);
        this.readBuf = null;
        this.oneBuf = new byte[1];
        this.debug = false;
        this.hasHitEOF = false;
        this.v7Format = false;
    }

    /**
     * Sets the debugging flag.
     *
     * @param debugF True to turn on debugging.
     */
    public void setDebug(boolean debug) {
        this.debug = debug;
        this.buffer.setDebug(debug);
    }

    /**
     * Closes this stream. Calls the TarBuffer's close() method.
     */
    public void close() throws IOException {
        this.buffer.close();
    }

    /**
     * Get the record size being used by this stream's TarBuffer.
     *
     * @return The TarBuffer record size.
     */
    public int getRecordSize() {
        return this.buffer.getRecordSize();
    }

    /**
     * Get the available data that can be read from the current
     * entry in the archive. This does not indicate how much data
     * is left in the entire archive, only in the current entry.
     * This value is determined from the entry's size header field
     * and the amount of data already read from the current entry.
     *
     *
     * @return The number of available bytes for the current entry.
     */
    public int available() throws IOException {
        return this.entrySize - this.entryOffset;
    }

    /**
     * Skip bytes in the input buffer. This skips bytes in the
     * current entry's data, not the entire archive, and will
     * stop at the end of the current entry's data if the number
     * to skip extends beyond that point.
     *
     * @param numToSkip The number of bytes to skip.
     */
    public long skip(long numToSkip) throws IOException {
        // REVIEW
        // This is horribly inefficient, but it ensures that we
        // properly skip over bytes via the TarBuffer...
        //
        byte[] skipBuf = new byte[8 * 1024];
        long skip = numToSkip;
        while (skip > 0) {
            int realSkip = (int) (skip > skipBuf.length ? skipBuf.length : skip);
            int numRead = this.read(skipBuf, 0, realSkip);
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
    public boolean markSupported() {
        return false;
    }

    /**
     * Since we do not support marking just yet, we do nothing.
     *
     * @param markLimit The limit to mark.
     */
    public void mark(int markLimit) {
    }

    /**
     * Since we do not support marking just yet, we do nothing.
     */
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
     */
    public TarEntry getNextEntry() throws IOException {
        if (this.hasHitEOF) {
            return null;
        }

        if (this.currEntry != null) {
            int numToSkip = this.entrySize - this.entryOffset;

            if (this.debug) {
                System.err.println("TarInputStream: SKIP currENTRY '"
                        + this.currEntry.getName() + "' SZ "
                        + this.entrySize + " OFF "
                        + this.entryOffset + "  skipping "
                        + numToSkip + " bytes");
            }

            if (numToSkip > 0) {
                this.skip(numToSkip);
            }

            this.readBuf = null;
        }

        byte[] headerBuf = this.buffer.readRecord();

        if (headerBuf == null) {
            if (this.debug) {
                System.err.println("READ NULL RECORD");
            }
            this.hasHitEOF = true;
        } else if (this.buffer.isEOFRecord(headerBuf)) {
            if (this.debug) {
                System.err.println("READ EOF RECORD");
            }
            this.hasHitEOF = true;
        }

        if (this.hasHitEOF) {
            this.currEntry = null;
        } else {
            this.currEntry = new TarEntry(headerBuf);

            if (!(headerBuf[257] == 'u' && headerBuf[258] == 's'
                    && headerBuf[259] == 't' && headerBuf[260] == 'a'
                    && headerBuf[261] == 'r')) {
                this.v7Format = true;
            }

            if (this.debug) {
                System.err.println("TarInputStream: SET CURRENTRY '"
                        + this.currEntry.getName()
                        + "' size = "
                        + this.currEntry.getSize());
            }

            this.entryOffset = 0;

            // REVIEW How do we resolve this discrepancy?!
            this.entrySize = (int) this.currEntry.getSize();
        }

        if (this.currEntry != null && this.currEntry.isGNULongNameEntry()) {
            // read in the name
            StringBuffer longName = new StringBuffer();
            byte[] buffer = new byte[256];
            int length = 0;
            while ((length = read(buffer)) >= 0) {
                longName.append(new String(buffer, 0, length));
            }
            getNextEntry();
            this.currEntry.setName(longName.toString());
        }

        return this.currEntry;
    }

    /**
     * Reads a byte from the current tar archive entry.
     *
     * This method simply calls read( byte[], int, int ).
     *
     * @return The byte read, or -1 at EOF.
     */
    public int read() throws IOException {
        int num = this.read(this.oneBuf, 0, 1);

        if (num == -1) {
            return num;
        } else {
            return (int) this.oneBuf[0];
        }
    }

    /**
     * Reads bytes from the current tar archive entry.
     *
     * This method simply calls read( byte[], int, int ).
     *
     * @param buf The buffer into which to place bytes read.
     * @return The number of bytes read, or -1 at EOF.
     */
    public int read(byte[] buf) throws IOException {
        return this.read(buf, 0, buf.length);
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
     */
    public int read(byte[] buf, int offset, int numToRead) throws IOException {
        int totalRead = 0;

        if (this.entryOffset >= this.entrySize) {
            return -1;
        }

        if ((numToRead + this.entryOffset) > this.entrySize) {
            numToRead = (this.entrySize - this.entryOffset);
        }

        if (this.readBuf != null) {
            int sz = (numToRead > this.readBuf.length) ? this.readBuf.length
                    : numToRead;

            System.arraycopy(this.readBuf, 0, buf, offset, sz);

            if (sz >= this.readBuf.length) {
                this.readBuf = null;
            } else {
                int newLen = this.readBuf.length - sz;
                byte[] newBuf = new byte[newLen];

                System.arraycopy(this.readBuf, sz, newBuf, 0, newLen);

                this.readBuf = newBuf;
            }

            totalRead += sz;
            numToRead -= sz;
            offset += sz;
        }

        while (numToRead > 0) {
            byte[] rec = this.buffer.readRecord();

            if (rec == null) {
                // Unexpected EOF!
                throw new IOException("unexpected EOF with " + numToRead
                        + " bytes unread");
            }

            int sz = numToRead;
            int recLen = rec.length;

            if (recLen > sz) {
                System.arraycopy(rec, 0, buf, offset, sz);

                this.readBuf = new byte[recLen - sz];

                System.arraycopy(rec, sz, this.readBuf, 0, recLen - sz);
            } else {
                sz = recLen;

                System.arraycopy(rec, 0, buf, offset, recLen);
            }

            totalRead += sz;
            numToRead -= sz;
            offset += sz;
        }

        this.entryOffset += totalRead;

        return totalRead;
    }

    /**
     * Copies the contents of the current tar archive entry directly into
     * an output stream.
     *
     * @param out The OutputStream into which to write the entry's data.
     */
    public void copyEntryContents(OutputStream out) throws IOException {
        byte[] buf = new byte[32 * 1024];

        while (true) {
            int numRead = this.read(buf, 0, buf.length);

            if (numRead == -1) {
                break;
            }

            out.write(buf, 0, numRead);
        }
    }
}
