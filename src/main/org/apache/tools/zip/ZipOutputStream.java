/*
 * Copyright  2001-2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
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

package org.apache.tools.zip;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Hashtable;
import java.util.Vector;
import java.util.zip.CRC32;
import java.util.zip.Deflater;
import java.util.zip.ZipException;

/**
 * Reimplementation of {@link java.util.zip.ZipOutputStream
 * java.util.zip.ZipOutputStream} that does handle the extended
 * functionality of this package, especially internal/external file
 * attributes and extra fields with different layouts for local file
 * data and central directory entries.
 *
 * <p>This class will try to use {@link java.io.RandomAccessFile
 * RandomAccessFile} when you know that the output is going to go to a
 * file.</p>
 *
 * <p>If RandomAccessFile cannot be used, this implementation will use
 * a Data Descriptor to store size and CRC information for {@link
 * #DEFLATED DEFLATED} entries, this means, you don't need to
 * calculate them yourself.  Unfortunately this is not possible for
 * the {@link #STORED STORED} method, here setting the CRC and
 * uncompressed size information is required before {@link
 * #putNextEntry putNextEntry} can be called.</p>
 *
 * @version $Revision$
 */
public class ZipOutputStream extends FilterOutputStream {

    /**
     * Current entry.
     *
     * @since 1.1
     */
    private ZipEntry entry;

    /**
     * The file comment.
     *
     * @since 1.1
     */
    private String comment = "";

    /**
     * Compression level for next entry.
     *
     * @since 1.1
     */
    private int level = Deflater.DEFAULT_COMPRESSION;

    /**
     * Has the compression level changed when compared to the last
     * entry?
     *
     * @since 1.5
     */
    private boolean hasCompressionLevelChanged = false;

    /**
     * Default compression method for next entry.
     *
     * @since 1.1
     */
    private int method = DEFLATED;

    /**
     * List of ZipEntries written so far.
     *
     * @since 1.1
     */
    private Vector entries = new Vector();

    /**
     * CRC instance to avoid parsing DEFLATED data twice.
     *
     * @since 1.1
     */
    private CRC32 crc = new CRC32();

    /**
     * Count the bytes written to out.
     *
     * @since 1.1
     */
    private long written = 0;

    /**
     * Data for local header data
     *
     * @since 1.1
     */
    private long dataStart = 0;

    /**
     * Offset for CRC entry in the local file header data for the
     * current entry starts here.
     *
     * @since 1.15
     */
    private long localDataStart = 0;

    /**
     * Start of central directory.
     *
     * @since 1.1
     */
    private ZipLong cdOffset = new ZipLong(0);

    /**
     * Length of central directory.
     *
     * @since 1.1
     */
    private ZipLong cdLength = new ZipLong(0);

    /**
     * Helper, a 0 as ZipShort.
     *
     * @since 1.1
     */
    private static final byte[] ZERO = {0, 0};

    /**
     * Helper, a 0 as ZipLong.
     *
     * @since 1.1
     */
    private static final byte[] LZERO = {0, 0, 0, 0};

    /**
     * Holds the offsets of the LFH starts for each entry.
     *
     * @since 1.1
     */
    private Hashtable offsets = new Hashtable();

    /**
     * The encoding to use for filenames and the file comment.
     *
     * <p>For a list of possible values see <a
     * href="http://java.sun.com/products/jdk/1.2/docs/guide/internat/encoding.doc.html">http://java.sun.com/products/jdk/1.2/docs/guide/internat/encoding.doc.html</a>.
     * Defaults to the platform's default character encoding.</p>
     *
     * @since 1.3
     */
    private String encoding = null;

    /**
     * This Deflater object is used for output.
     *
     * <p>This attribute is only protected to provide a level of API
     * backwards compatibility.  This class used to extend {@link
     * java.util.zip.DeflaterOutputStream DeflaterOutputStream} up to
     * Revision 1.13.</p>
     *
     * @since 1.14
     */
    protected Deflater def = new Deflater(Deflater.DEFAULT_COMPRESSION, true);

    /**
     * This buffer servers as a Deflater.
     *
     * <p>This attribute is only protected to provide a level of API
     * backwards compatibility.  This class used to extend {@link
     * java.util.zip.DeflaterOutputStream DeflaterOutputStream} up to
     * Revision 1.13.</p>
     *
     * @since 1.14
     */
    protected byte[] buf = new byte[512];

    /**
     * Optional random access output.
     *
     * @since 1.14
     */
    private RandomAccessFile raf = null;

    /**
     * Compression method for deflated entries.
     *
     * @since 1.1
     */
    public static final int DEFLATED = ZipEntry.DEFLATED;

    /**
     * Compression method for deflated entries.
     *
     * @since 1.1
     */
    public static final int STORED = ZipEntry.STORED;

    /**
     * Creates a new ZIP OutputStream filtering the underlying stream.
     *
     * @since 1.1
     */
    public ZipOutputStream(OutputStream out) {
        super(out);
    }

    /**
     * Creates a new ZIP OutputStream writing to a File.  Will use
     * random access if possible.
     *
     * @since 1.14
     */
    public ZipOutputStream(File file) throws IOException {
        super(null);

        try {
            raf = new RandomAccessFile(file, "rw");
            raf.setLength(0);
        } catch (IOException e) {
            if (raf != null) {
                try {
                    raf.close();
                } catch (IOException inner) {
                    // ignore
                }
                raf = null;
            }
            out = new FileOutputStream(file);
        }
    }

    /**
     * This method indicates whether this archive is writing to a seekable stream (i.e., to a random
     * access file).
     *
     * <p>For seekable streams, you don't need to calculate the CRC or
     * uncompressed size for {@link #STORED} entries before
     * invoking {@link #putNextEntry}.
     *
     * @since 1.17
     */
    public boolean isSeekable() {
        return raf != null;
    }

    /**
     * The encoding to use for filenames and the file comment.
     *
     * <p>For a list of possible values see <a
     * href="http://java.sun.com/products/jdk/1.2/docs/guide/internat/encoding.doc.html">http://java.sun.com/products/jdk/1.2/docs/guide/internat/encoding.doc.html</a>.
     * Defaults to the platform's default character encoding.</p>
     *
     * @since 1.3
     */
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    /**
     * The encoding to use for filenames and the file comment.
     *
     * @return null if using the platform's default character encoding.
     *
     * @since 1.3
     */
    public String getEncoding() {
        return encoding;
    }

    /**
     * Finishs writing the contents and closes this as well as the
     * underlying stream.
     *
     * @since 1.1
     */
    public void finish() throws IOException {
        closeEntry();
        cdOffset = new ZipLong(written);
        for (int i = 0; i < entries.size(); i++) {
            writeCentralFileHeader((ZipEntry) entries.elementAt(i));
        }
        cdLength = new ZipLong(written - cdOffset.getValue());
        writeCentralDirectoryEnd();
        offsets.clear();
        entries.removeAllElements();
    }

    /**
     * Writes all necessary data for this entry.
     *
     * @since 1.1
     */
    public void closeEntry() throws IOException {
        if (entry == null) {
            return;
        }

        long realCrc = crc.getValue();
        crc.reset();

        if (entry.getMethod() == DEFLATED) {
            def.finish();
            while (!def.finished()) {
                deflate();
            }

            entry.setSize(def.getTotalIn());
            entry.setComprSize(def.getTotalOut());
            entry.setCrc(realCrc);

            def.reset();

            written += entry.getCompressedSize();
        } else if (raf == null) {
            if (entry.getCrc() != realCrc) {
                throw new ZipException("bad CRC checksum for entry "
                                       + entry.getName() + ": "
                                       + Long.toHexString(entry.getCrc())
                                       + " instead of "
                                       + Long.toHexString(realCrc));
            }

            if (entry.getSize() != written - dataStart) {
                throw new ZipException("bad size for entry "
                                       + entry.getName() + ": "
                                       + entry.getSize()
                                       + " instead of "
                                       + (written - dataStart));
            }
        } else { /* method is STORED and we used RandomAccessFile */
            long size = written - dataStart;

            entry.setSize(size);
            entry.setComprSize(size);
            entry.setCrc(realCrc);
        }

        // If random access output, write the local file header containing
        // the correct CRC and compressed/uncompressed sizes
        if (raf != null) {
            long save = raf.getFilePointer();

            raf.seek(localDataStart);
            writeOut((new ZipLong(entry.getCrc())).getBytes());
            writeOut((new ZipLong(entry.getCompressedSize())).getBytes());
            writeOut((new ZipLong(entry.getSize())).getBytes());
            raf.seek(save);
        }

        writeDataDescriptor(entry);
        entry = null;
    }

    /**
     * Begin writing next entry.
     *
     * @since 1.1
     */
    public void putNextEntry(ZipEntry ze) throws IOException {
        closeEntry();

        entry = ze;
        entries.addElement(entry);

        if (entry.getMethod() == -1) { // not specified
            entry.setMethod(method);
        }

        if (entry.getTime() == -1) { // not specified
            entry.setTime(System.currentTimeMillis());
        }

        // Size/CRC not required if RandomAccessFile is used
        if (entry.getMethod() == STORED && raf == null) {
            if (entry.getSize() == -1) {
                throw new ZipException("uncompressed size is required for"
                                       + " STORED method when not writing to a"
                                       + " file");
            }
            if (entry.getCrc() == -1) {
                throw new ZipException("crc checksum is required for STORED"
                                       + " method when not writing to a file");
            }
            entry.setComprSize(entry.getSize());
        }

        if (entry.getMethod() == DEFLATED && hasCompressionLevelChanged) {
            def.setLevel(level);
            hasCompressionLevelChanged = false;
        }
        writeLocalFileHeader(entry);
    }

    /**
     * Set the file comment.
     *
     * @since 1.1
     */
    public void setComment(String comment) {
        this.comment = comment;
    }

    /**
     * Sets the compression level for subsequent entries.
     *
     * <p>Default is Deflater.DEFAULT_COMPRESSION.</p>
     *
     * @since 1.1
     */
    public void setLevel(int level) {
        hasCompressionLevelChanged = (this.level != level);
        this.level = level;
    }

    /**
     * Sets the default compression method for subsequent entries.
     *
     * <p>Default is DEFLATED.</p>
     *
     * @since 1.1
     */
    public void setMethod(int method) {
        this.method = method;
    }

    /**
     * Writes bytes to ZIP entry.
     */
    public void write(byte[] b, int offset, int length) throws IOException {
        if (entry.getMethod() == DEFLATED) {
            if (length > 0) {
                if (!def.finished()) {
                    def.setInput(b, offset, length);
                    while (!def.needsInput()) {
                        deflate();
                    }
                }
            }
        } else {
            writeOut(b, offset, length);
            written += length;
        }
        crc.update(b, offset, length);
    }

    /**
     * Writes a single byte to ZIP entry.
     *
     * <p>Delegates to the three arg method.</p>
     *
     * @since 1.14
     */
    public void write(int b) throws IOException {
        byte[] buff = new byte[1];
        buff[0] = (byte) (b & 0xff);
        write(buff, 0, 1);
    }

    /**
     * Closes this output stream and releases any system resources
     * associated with the stream.
     *
     * @exception  IOException  if an I/O error occurs.
     * @since 1.14
     */
    public void close() throws IOException {
        finish();

        if (raf != null) {
            raf.close();
        }
        if (out != null) {
            out.close();
        }
    }

    /**
     * Flushes this output stream and forces any buffered output bytes
     * to be written out to the stream.
     *
     * @exception  IOException  if an I/O error occurs.
     * @since 1.14
     */
    public void flush() throws IOException {
        if (out != null) {
            out.flush();
        }
    }

    /*
     * Various ZIP constants
     */
    /**
     * local file header signature
     *
     * @since 1.1
     */
    protected static final ZipLong LFH_SIG = new ZipLong(0X04034B50L);
    /**
     * data descriptor signature
     *
     * @since 1.1
     */
    protected static final ZipLong DD_SIG = new ZipLong(0X08074B50L);
    /**
     * central file header signature
     *
     * @since 1.1
     */
    protected static final ZipLong CFH_SIG = new ZipLong(0X02014B50L);
    /**
     * end of central dir signature
     *
     * @since 1.1
     */
    protected static final ZipLong EOCD_SIG = new ZipLong(0X06054B50L);

    /**
     * Writes next block of compressed data to the output stream.
     *
     * @since 1.14
     */
    protected final void deflate() throws IOException {
        int len = def.deflate(buf, 0, buf.length);
        if (len > 0) {
            writeOut(buf, 0, len);
        }
    }

    /**
     * Writes the local file header entry
     *
     * @since 1.1
     */
    protected void writeLocalFileHeader(ZipEntry ze) throws IOException {
        offsets.put(ze, new ZipLong(written));

        writeOut(LFH_SIG.getBytes());
        written += 4;

        // version needed to extract
        // general purpose bit flag
        if (ze.getMethod() == DEFLATED && raf == null) {
            // requires version 2 as we are going to store length info
            // in the data descriptor
            writeOut((new ZipShort(20)).getBytes());

            // bit3 set to signal, we use a data descriptor
            writeOut((new ZipShort(8)).getBytes());
        } else {
            writeOut((new ZipShort(10)).getBytes());
            writeOut(ZERO);
        }
        written += 4;

        // compression method
        writeOut((new ZipShort(ze.getMethod())).getBytes());
        written += 2;

        // last mod. time and date
        writeOut(toDosTime(ze.getTime()));
        written += 4;

        // CRC
        // compressed length
        // uncompressed length
        localDataStart = written;
        if (ze.getMethod() == DEFLATED || raf != null) {
            writeOut(LZERO);
            writeOut(LZERO);
            writeOut(LZERO);
        } else {
            writeOut((new ZipLong(ze.getCrc())).getBytes());
            writeOut((new ZipLong(ze.getSize())).getBytes());
            writeOut((new ZipLong(ze.getSize())).getBytes());
        }
        written += 12;

        // file name length
        byte[] name = getBytes(ze.getName());
        writeOut((new ZipShort(name.length)).getBytes());
        written += 2;

        // extra field length
        byte[] extra = ze.getLocalFileDataExtra();
        writeOut((new ZipShort(extra.length)).getBytes());
        written += 2;

        // file name
        writeOut(name);
        written += name.length;

        // extra field
        writeOut(extra);
        written += extra.length;

        dataStart = written;
    }

    /**
     * Writes the data descriptor entry
     *
     * @since 1.1
     */
    protected void writeDataDescriptor(ZipEntry ze) throws IOException {
        if (ze.getMethod() != DEFLATED || raf != null) {
            return;
        }
        writeOut(DD_SIG.getBytes());
        writeOut((new ZipLong(entry.getCrc())).getBytes());
        writeOut((new ZipLong(entry.getCompressedSize())).getBytes());
        writeOut((new ZipLong(entry.getSize())).getBytes());
        written += 16;
    }

    /**
     * Writes the central file header entry
     *
     * @since 1.1
     */
    protected void writeCentralFileHeader(ZipEntry ze) throws IOException {
        writeOut(CFH_SIG.getBytes());
        written += 4;

        // version made by
        writeOut((new ZipShort((ze.getPlatform() << 8) | 20)).getBytes());
        written += 2;

        // version needed to extract
        // general purpose bit flag
        if (ze.getMethod() == DEFLATED && raf == null) {
            // requires version 2 as we are going to store length info
            // in the data descriptor
            writeOut((new ZipShort(20)).getBytes());

            // bit3 set to signal, we use a data descriptor
            writeOut((new ZipShort(8)).getBytes());
        } else {
            writeOut((new ZipShort(10)).getBytes());
            writeOut(ZERO);
        }
        written += 4;

        // compression method
        writeOut((new ZipShort(ze.getMethod())).getBytes());
        written += 2;

        // last mod. time and date
        writeOut(toDosTime(ze.getTime()));
        written += 4;

        // CRC
        // compressed length
        // uncompressed length
        writeOut((new ZipLong(ze.getCrc())).getBytes());
        writeOut((new ZipLong(ze.getCompressedSize())).getBytes());
        writeOut((new ZipLong(ze.getSize())).getBytes());
        written += 12;

        // file name length
        byte[] name = getBytes(ze.getName());
        writeOut((new ZipShort(name.length)).getBytes());
        written += 2;

        // extra field length
        byte[] extra = ze.getCentralDirectoryExtra();
        writeOut((new ZipShort(extra.length)).getBytes());
        written += 2;

        // file comment length
        String comm = ze.getComment();
        if (comm == null) {
            comm = "";
        }
        byte[] commentB = getBytes(comm);
        writeOut((new ZipShort(commentB.length)).getBytes());
        written += 2;

        // disk number start
        writeOut(ZERO);
        written += 2;

        // internal file attributes
        writeOut((new ZipShort(ze.getInternalAttributes())).getBytes());
        written += 2;

        // external file attributes
        writeOut((new ZipLong(ze.getExternalAttributes())).getBytes());
        written += 4;

        // relative offset of LFH
        writeOut(((ZipLong) offsets.get(ze)).getBytes());
        written += 4;

        // file name
        writeOut(name);
        written += name.length;

        // extra field
        writeOut(extra);
        written += extra.length;

        // file comment
        writeOut(commentB);
        written += commentB.length;
    }

    /**
     * Writes the &quot;End of central dir record&quot;
     *
     * @since 1.1
     */
    protected void writeCentralDirectoryEnd() throws IOException {
        writeOut(EOCD_SIG.getBytes());

        // disk numbers
        writeOut(ZERO);
        writeOut(ZERO);

        // number of entries
        byte[] num = (new ZipShort(entries.size())).getBytes();
        writeOut(num);
        writeOut(num);

        // length and location of CD
        writeOut(cdLength.getBytes());
        writeOut(cdOffset.getBytes());

        // ZIP file comment
        byte[] data = getBytes(comment);
        writeOut((new ZipShort(data.length)).getBytes());
        writeOut(data);
    }

    /**
     * Smallest date/time ZIP can handle.
     *
     * @since 1.1
     */
    private static final ZipLong DOS_TIME_MIN = new ZipLong(0x00002100L);

    /**
     * Convert a Date object to a DOS date/time field.
     *
     * @since 1.1
     */
    protected static ZipLong toDosTime(Date time) {
        return new ZipLong(toDosTime(time.getTime()));
    }

    /**
     * Convert a Date object to a DOS date/time field.
     *
     * <p>Stolen from InfoZip's <code>fileio.c</code></p>
     *
     * @since 1.26
     */
    protected static byte[] toDosTime(long t) {
        Date time = new Date(t);
        int year = time.getYear() + 1900;
        if (year < 1980) {
            return DOS_TIME_MIN.getBytes();
        }
        int month = time.getMonth() + 1;
        long value =  ((year - 1980) << 25)
            |         (month << 21)
            |         (time.getDate() << 16)
            |         (time.getHours() << 11)
            |         (time.getMinutes() << 5)
            |         (time.getSeconds() >> 1);
        byte[] result = new byte[4];
        result[0] = (byte) ((value & 0xFF));
        result[1] = (byte) ((value & 0xFF00) >> 8);
        result[2] = (byte) ((value & 0xFF0000) >> 16);
        result[3] = (byte) ((value & 0xFF000000L) >> 24);
        return result;
    }

    /**
     * Retrieve the bytes for the given String in the encoding set for
     * this Stream.
     *
     * @since 1.3
     */
    protected byte[] getBytes(String name) throws ZipException {
        if (encoding == null) {
            return name.getBytes();
        } else {
            try {
                return name.getBytes(encoding);
            } catch (UnsupportedEncodingException uee) {
                throw new ZipException(uee.getMessage());
            }
        }
    }

    /**
     * Write bytes to output or random access file
     *
     * @since 1.14
     */
    protected final void writeOut(byte [] data) throws IOException {
        writeOut(data, 0, data.length);
    }

    /**
     * Write bytes to output or random access file
     *
     * @since 1.14
     */
    protected final void writeOut(byte [] data, int offset, int length)
        throws IOException {
        if (raf != null) {
            raf.write(data, offset, length);
        } else {
            out.write(data, offset, length);
        }
    }
}
