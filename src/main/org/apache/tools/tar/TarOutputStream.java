/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999 The Apache Software Foundation.  All rights
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
 * 4. The names "The Jakarta Project", "Ant", and "Apache Software
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

import java.io.*;

/**
 * The TarOutputStream writes a UNIX tar archive as an OutputStream.
 * Methods are provided to put entries, and then write their contents
 * by writing to this stream using write().
 * 
 * @author Timothy Gerard Endres <a href="mailto:time@ice.com">time@ice.com</a>
 */
public class TarOutputStream extends FilterOutputStream {
    static public final int LONGFILE_ERROR = 0;
    static public final int LONGFILE_TRUNCATE = 1;
    static public final int LONGFILE_GNU = 2;
    
    protected boolean   debug;
    protected int       currSize;
    protected int       currBytes;
    protected byte[]    oneBuf;
    protected byte[]    recordBuf;
    protected int       assemLen;
    protected byte[]    assemBuf;
    protected TarBuffer buffer;
    protected int       longFileMode = LONGFILE_ERROR;

    public TarOutputStream(OutputStream os) {
        this(os, TarBuffer.DEFAULT_BLKSIZE, TarBuffer.DEFAULT_RCDSIZE);
    }

    public TarOutputStream(OutputStream os, int blockSize) {
        this(os, blockSize, TarBuffer.DEFAULT_RCDSIZE);
    }

    public TarOutputStream(OutputStream os, int blockSize, int recordSize) {
        super(os);

        this.buffer = new TarBuffer(os, blockSize, recordSize);
        this.debug = false;
        this.assemLen = 0;
        this.assemBuf = new byte[recordSize];
        this.recordBuf = new byte[recordSize];
        this.oneBuf = new byte[1];
    }

    public void setLongFileMode(int longFileMode) {
        this.longFileMode = longFileMode;
    }
    

    /**
     * Sets the debugging flag.
     * 
     * @param debugF True to turn on debugging.
     */
    public void setDebug(boolean debugF) {
        this.debug = debugF;
    } 

    /**
     * Sets the debugging flag in this stream's TarBuffer.
     * 
     * @param debugF True to turn on debugging.
     */
    public void setBufferDebug(boolean debug) {
        this.buffer.setDebug(debug);
    } 

    /**
     * Ends the TAR archive without closing the underlying OutputStream.
     * The result is that the EOF record of nulls is written.
     */
    public void finish() throws IOException {
        this.writeEOFRecord();
    } 

    /**
     * Ends the TAR archive and closes the underlying OutputStream.
     * This means that finish() is called followed by calling the
     * TarBuffer's close().
     */
    public void close() throws IOException {
        this.finish();
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
     * Put an entry on the output stream. This writes the entry's
     * header record and positions the output stream for writing
     * the contents of the entry. Once this method is called, the
     * stream is ready for calls to write() to write the entry's
     * contents. Once the contents are written, closeEntry()
     * <B>MUST</B> be called to ensure that all buffered data
     * is completely written to the output stream.
     * 
     * @param entry The TarEntry to be written to the archive.
     */
    public void putNextEntry(TarEntry entry) throws IOException {
        if (entry.getName().length() >= TarConstants.NAMELEN) {

            if (longFileMode == LONGFILE_GNU) {
                // create a TarEntry for the LongLink, the contents
                // of which are the entry's name 
                TarEntry longLinkEntry = new TarEntry(TarConstants.GNU_LONGLINK,
                                                      TarConstants.LF_GNUTYPE_LONGNAME);
                 
                longLinkEntry.setSize(entry.getName().length() + 1);
                putNextEntry(longLinkEntry);                                                    
                write(entry.getName().getBytes());
                write(0);
                closeEntry();
            }
            else if (longFileMode != LONGFILE_TRUNCATE) {
                throw new RuntimeException("file name '" + entry.getName() 
                                             + "' is too long ( > " 
                                             + TarConstants.NAMELEN + " bytes)");
            }
        } 

        entry.writeEntryHeader(this.recordBuf);
        this.buffer.writeRecord(this.recordBuf);

        this.currBytes = 0;

        if (entry.isDirectory()) {
            this.currSize = 0;
        } else {
            this.currSize = (int) entry.getSize();
        }
    } 

    /**
     * Close an entry. This method MUST be called for all file
     * entries that contain data. The reason is that we must
     * buffer data written to the stream in order to satisfy
     * the buffer's record based writes. Thus, there may be
     * data fragments still being assembled that must be written
     * to the output stream before this entry is closed and the
     * next entry written.
     */
    public void closeEntry() throws IOException {
        if (this.assemLen > 0) {
            for (int i = this.assemLen; i < this.assemBuf.length; ++i) {
                this.assemBuf[i] = 0;
            }

            this.buffer.writeRecord(this.assemBuf);

            this.currBytes += this.assemLen;
            this.assemLen = 0;
        } 

        if (this.currBytes < this.currSize) {
            throw new IOException("entry closed at '" + this.currBytes 
                                  + "' before the '" + this.currSize 
                                  + "' bytes specified in the header were written");
        } 
    } 

    /**
     * Writes a byte to the current tar archive entry.
     * 
     * This method simply calls read( byte[], int, int ).
     * 
     * @param b The byte written.
     */
    public void write(int b) throws IOException {
        this.oneBuf[0] = (byte) b;

        this.write(this.oneBuf, 0, 1);
    } 

    /**
     * Writes bytes to the current tar archive entry.
     * 
     * This method simply calls write( byte[], int, int ).
     * 
     * @param wBuf The buffer to write to the archive.
     * @return The number of bytes read, or -1 at EOF.
     */
    public void write(byte[] wBuf) throws IOException {
        this.write(wBuf, 0, wBuf.length);
    } 

    /**
     * Writes bytes to the current tar archive entry. This method
     * is aware of the current entry and will throw an exception if
     * you attempt to write bytes past the length specified for the
     * current entry. The method is also (painfully) aware of the
     * record buffering required by TarBuffer, and manages buffers
     * that are not a multiple of recordsize in length, including
     * assembling records from small buffers.
     * 
     * @param wBuf The buffer to write to the archive.
     * @param wOffset The offset in the buffer from which to get bytes.
     * @param numToWrite The number of bytes to write.
     */
    public void write(byte[] wBuf, int wOffset, int numToWrite) throws IOException {
        if ((this.currBytes + numToWrite) > this.currSize) {
            throw new IOException("request to write '" + numToWrite 
                                  + "' bytes exceeds size in header of '" 
                                  + this.currSize + "' bytes");

            // 
            // We have to deal with assembly!!!
            // The programmer can be writing little 32 byte chunks for all
            // we know, and we must assemble complete records for writing.
            // REVIEW Maybe this should be in TarBuffer? Could that help to
            // eliminate some of the buffer copying.
            // 
        } 

        if (this.assemLen > 0) {
            if ((this.assemLen + numToWrite) >= this.recordBuf.length) {
                int aLen = this.recordBuf.length - this.assemLen;

                System.arraycopy(this.assemBuf, 0, this.recordBuf, 0, 
                                 this.assemLen);
                System.arraycopy(wBuf, wOffset, this.recordBuf, 
                                 this.assemLen, aLen);
                this.buffer.writeRecord(this.recordBuf);

                this.currBytes += this.recordBuf.length;
                wOffset += aLen;
                numToWrite -= aLen;
                this.assemLen = 0;
            } else {
                System.arraycopy(wBuf, wOffset, this.assemBuf, this.assemLen, 
                                 numToWrite);

                wOffset += numToWrite;
                this.assemLen += numToWrite;
                numToWrite -= numToWrite;
            } 
        } 

        // 
        // When we get here we have EITHER:
        // o An empty "assemble" buffer.
        // o No bytes to write (numToWrite == 0)
        // 
        while (numToWrite > 0) {
            if (numToWrite < this.recordBuf.length) {
                System.arraycopy(wBuf, wOffset, this.assemBuf, this.assemLen, 
                                 numToWrite);

                this.assemLen += numToWrite;

                break;
            } 

            this.buffer.writeRecord(wBuf, wOffset);

            int num = this.recordBuf.length;

            this.currBytes += num;
            numToWrite -= num;
            wOffset += num;
        } 
    } 

    /**
     * Write an EOF (end of archive) record to the tar archive.
     * An EOF record consists of a record of all zeros.
     */
    private void writeEOFRecord() throws IOException {
        for (int i = 0; i < this.recordBuf.length; ++i) {
            this.recordBuf[i] = 0;
        }

        this.buffer.writeRecord(this.recordBuf);
    } 
}


