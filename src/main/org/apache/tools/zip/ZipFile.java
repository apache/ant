/*
 * Copyright  2003-2004 The Apache Software Foundation
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
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;
import java.util.zip.ZipException;

/**
 * Replacement for <code>java.util.ZipFile</code>.
 *
 * <p>This class adds support for file name encodings other than UTF-8
 * (which is required to work on ZIP files created by native zip tools
 * and is able to skip a preamble like the one found in self
 * extracting archives.  Furthermore it returns instances of
 * <code>org.apache.tools.zip.ZipEntry</code> instead of
 * <code>java.util.zip.ZipEntry</code>.</p>
 *
 * <p>It doesn't extend <code>java.util.zip.ZipFile</code> as it would
 * have to reimplement all methods anyway.  Like
 * <code>java.util.ZipFile</code>, it uses RandomAccessFile under the
 * covers and supports compressed and uncompressed entries.</p>
 *
 * <p>The method signatures mimic the ones of
 * <code>java.util.zip.ZipFile</code>, with a couple of exceptions:
 *
 * <ul>
 *   <li>There is no getName method.</li>
 *   <li>entries has been renamed to getEntries.</li>
 *   <li>getEntries and getEntry return
 *   <code>org.apache.tools.zip.ZipEntry</code> instances.</li>
 *   <li>close is allowed to throw IOException.</li>
 * </ul>
 *
 * @version $Revision$
 */
public class ZipFile {

    /**
     * Maps ZipEntrys to Longs, recording the offsets of the local
     * file headers.
     */
    private Hashtable entries = new Hashtable();

    /**
     * Maps String to ZipEntrys, name -> actual entry.
     */
    private Hashtable nameMap = new Hashtable();

    /**
     * Maps ZipEntrys to Longs, recording the offsets of the actual file data.
     */
    private Hashtable dataOffsets = new Hashtable();

    /**
     * The encoding to use for filenames and the file comment.
     *
     * <p>For a list of possible values see <a
     * href="http://java.sun.com/products/jdk/1.2/docs/guide/internat/encoding.doc.html">http://java.sun.com/products/jdk/1.2/docs/guide/internat/encoding.doc.html</a>.
     * Defaults to the platform's default character encoding.</p>
     */
    private String encoding = null;

    /**
     * The actual data source.
     */
    private RandomAccessFile archive;

    /**
     * Opens the given file for reading, assuming the platform's
     * native encoding for file names.
     *
     * @param f the archive.
     *
     * @throws IOException if an error occurs while reading the file.
     */
    public ZipFile(File f) throws IOException {
        this(f, null);
    }

    /**
     * Opens the given file for reading, assuming the platform's
     * native encoding for file names.
     *
     * @param name name of the archive.
     *
     * @throws IOException if an error occurs while reading the file.
     */
    public ZipFile(String name) throws IOException {
        this(new File(name), null);
    }

    /**
     * Opens the given file for reading, assuming the specified
     * encoding for file names.
     *
     * @param name name of the archive.
     * @param encoding the encoding to use for file names
     *
     * @throws IOException if an error occurs while reading the file.
     */
    public ZipFile(String name, String encoding) throws IOException {
        this(new File(name), encoding);
    }

    /**
     * Opens the given file for reading, assuming the specified
     * encoding for file names.
     *
     * @param f the archive.
     * @param encoding the encoding to use for file names
     *
     * @throws IOException if an error occurs while reading the file.
     */
    public ZipFile(File f, String encoding) throws IOException {
        this.encoding = encoding;
        archive = new RandomAccessFile(f, "r");
        populateFromCentralDirectory();
        resolveLocalFileHeaderData();
    }

    /**
     * The encoding to use for filenames and the file comment.
     *
     * @return null if using the platform's default character encoding.
     */
    public String getEncoding() {
        return encoding;
    }

    /**
     * Closes the archive.
     * @throws IOException if an error occurs closing the archive.
     */
    public void close() throws IOException {
        archive.close();
    }

    /**
     * close a zipfile quietly; throw no io fault, do nothing
     * on a null parameter
     * @param zipfile file to close, can be null
     */
    public static void closeQuietly(ZipFile zipfile) {
        if (zipfile != null) {
            try {
                zipfile.close();
            } catch (IOException e) {
                //ignore
            }
        }
    }

    /**
     * Returns all entries.
     * @return all entries as {@link ZipEntry} instances
     */
    public Enumeration getEntries() {
        return entries.keys();
    }

    /**
     * Returns a named entry - or <code>null</code> if no entry by
     * that name exists.
     * @param name name of the entry.
     * @return the ZipEntry corresponding to the given name - or
     * <code>null</code> if not present.
     */
    public ZipEntry getEntry(String name) {
        return (ZipEntry) nameMap.get(name);
    }

    /**
     * Returns an InputStream for reading the contents of the given entry.
     * @param ze the entry to get the stream for.
     * @return a stream to read the entry from.
     */
    public InputStream getInputStream(ZipEntry ze)
        throws IOException, ZipException {
        Long start = (Long) dataOffsets.get(ze);
        if (start == null) {
            return null;
        }
        BoundedInputStream bis =
            new BoundedInputStream(start.longValue(), ze.getCompressedSize());
        switch (ze.getMethod()) {
            case ZipEntry.STORED:
                return bis;
            case ZipEntry.DEFLATED:
                bis.addDummy();
                return new InflaterInputStream(bis, new Inflater(true));
            default:
                throw new ZipException("Found unsupported compression method "
                                       + ze.getMethod());
        }
    }

    private static final int CFH_LEN =
        /* version made by                 */ 2 +
        /* version needed to extract       */ 2 +
        /* general purpose bit flag        */ 2 +
        /* compression method              */ 2 +
        /* last mod file time              */ 2 +
        /* last mod file date              */ 2 +
        /* crc-32                          */ 4 +
        /* compressed size                 */ 4 +
        /* uncompressed size               */ 4 +
        /* filename length                 */ 2 +
        /* extra field length              */ 2 +
        /* file comment length             */ 2 +
        /* disk number start               */ 2 +
        /* internal file attributes        */ 2 +
        /* external file attributes        */ 4 +
        /* relative offset of local header */ 4;

    /**
     * Reads the central directory of the given archive and populates
     * the internal tables with ZipEntry instances.
     *
     * <p>The ZipEntrys will know all data that can be obtained from
     * the central directory alone, but not the data that requires the
     * local file header or additional data to be read.</p>
     */
    private void populateFromCentralDirectory()
        throws IOException {
        positionAtCentralDirectory();

        byte[] cfh = new byte[CFH_LEN];

        byte[] signatureBytes = new byte[4];
        archive.readFully(signatureBytes);
        ZipLong sig = new ZipLong(signatureBytes);
        while (sig.equals(ZipOutputStream.CFH_SIG)) {
            archive.readFully(cfh);
            int off = 0;
            ZipEntry ze = new ZipEntry();

            ZipShort versionMadeBy = new ZipShort(cfh, off);
            off += 2;
            ze.setPlatform((versionMadeBy.getValue() >> 8) & 0x0F);

            off += 4; // skip version info and general purpose byte

            ze.setMethod((new ZipShort(cfh, off)).getValue());
            off += 2;

            ze.setTime(fromDosTime(new ZipLong(cfh, off)).getTime());
            off += 4;

            ze.setCrc((new ZipLong(cfh, off)).getValue());
            off += 4;

            ze.setCompressedSize((new ZipLong(cfh, off)).getValue());
            off += 4;

            ze.setSize((new ZipLong(cfh, off)).getValue());
            off += 4;

            int fileNameLen = (new ZipShort(cfh, off)).getValue();
            off += 2;

            int extraLen = (new ZipShort(cfh, off)).getValue();
            off += 2;

            int commentLen = (new ZipShort(cfh, off)).getValue();
            off += 2;

            off += 2; // disk number

            ze.setInternalAttributes((new ZipShort(cfh, off)).getValue());
            off += 2;

            ze.setExternalAttributes((new ZipLong(cfh, off)).getValue());
            off += 4;

            // LFH offset
            entries.put(ze, new Long((new ZipLong(cfh, off)).getValue()));

            byte[] fileName = new byte[fileNameLen];
            archive.readFully(fileName);
            ze.setName(getString(fileName));

            nameMap.put(ze.getName(), ze);

            archive.skipBytes(extraLen);

            byte[] comment = new byte[commentLen];
            archive.readFully(comment);
            ze.setComment(getString(comment));

            archive.readFully(signatureBytes);
            sig = new ZipLong(signatureBytes);
        }
    }

    private static final int MIN_EOCD_SIZE =
        /* end of central dir signature    */ 4 +
        /* number of this disk             */ 2 +
        /* number of the disk with the     */   +
        /* start of the central directory  */ 2 +
        /* total number of entries in      */   +
        /* the central dir on this disk    */ 2 +
        /* total number of entries in      */   +
        /* the central dir                 */ 2 +
        /* size of the central directory   */ 4 +
        /* offset of start of central      */   +
        /* directory with respect to       */   +
        /* the starting disk number        */ 4 +
        /* zipfile comment length          */ 2;

    private static final int CFD_LOCATOR_OFFSET =
        /* end of central dir signature    */ 4 +
        /* number of this disk             */ 2 +
        /* number of the disk with the     */   +
        /* start of the central directory  */ 2 +
        /* total number of entries in      */   +
        /* the central dir on this disk    */ 2 +
        /* total number of entries in      */   +
        /* the central dir                 */ 2 +
        /* size of the central directory   */ 4;

    /**
     * Searches for the &quot;End of central dir record&quot;, parses
     * it and positions the stream at the first central directory
     * record.
     */
    private void positionAtCentralDirectory()
        throws IOException {
        long off = archive.length() - MIN_EOCD_SIZE;
        archive.seek(off);
        byte[] sig = ZipOutputStream.EOCD_SIG.getBytes();
        int curr = archive.read();
        boolean found = false;
        while (curr != -1) {
            if (curr == sig[0]) {
                curr = archive.read();
                if (curr == sig[1]) {
                    curr = archive.read();
                    if (curr == sig[2]) {
                        curr = archive.read();
                        if (curr == sig[3]) {
                            found = true;
                            break;
                        }
                    }
                }
            }
            archive.seek(--off);
            curr = archive.read();
        }
        if (!found) {
            throw new ZipException("archive is not a ZIP archive");
        }
        archive.seek(off + CFD_LOCATOR_OFFSET);
        byte[] cfdOffset = new byte[4];
        archive.readFully(cfdOffset);
        archive.seek((new ZipLong(cfdOffset)).getValue());
    }

    /**
     * Number of bytes in local file header up to the &quot;length of
     * filename&quot; entry.
     */
    private static final long LFH_OFFSET_FOR_FILENAME_LENGTH =
        /* local file header signature     */ 4 +
        /* version needed to extract       */ 2 +
        /* general purpose bit flag        */ 2 +
        /* compression method              */ 2 +
        /* last mod file time              */ 2 +
        /* last mod file date              */ 2 +
        /* crc-32                          */ 4 +
        /* compressed size                 */ 4 +
        /* uncompressed size               */ 4;

    /**
     * Walks through all recorded entries and adds the data available
     * from the local file header.
     *
     * <p>Also records the offsets for the data to read from the
     * entries.</p>
     */
    private void resolveLocalFileHeaderData()
        throws IOException {
        Enumeration e = getEntries();
        while (e.hasMoreElements()) {
            ZipEntry ze = (ZipEntry) e.nextElement();
            long offset = ((Long) entries.get(ze)).longValue();
            archive.seek(offset + LFH_OFFSET_FOR_FILENAME_LENGTH);
            byte[] b = new byte[2];
            archive.readFully(b);
            int fileNameLen = (new ZipShort(b)).getValue();
            archive.readFully(b);
            int extraFieldLen = (new ZipShort(b)).getValue();
            archive.skipBytes(fileNameLen);
            byte[] localExtraData = new byte[extraFieldLen];
            archive.readFully(localExtraData);
            ze.setExtra(localExtraData);
            dataOffsets.put(ze,
                            new Long(offset + LFH_OFFSET_FOR_FILENAME_LENGTH
                                     + 2 + 2 + fileNameLen + extraFieldLen));
        }
    }

    /**
     * Convert a DOS date/time field to a Date object.
     *
     * @param l contains the stored DOS time.
     * @return a Date instance corresponding to the given time.
     */
    protected static Date fromDosTime(ZipLong l) {
        long dosTime = l.getValue();
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, (int) ((dosTime >> 25) & 0x7f) + 1980);
        cal.set(Calendar.MONTH, (int) ((dosTime >> 21) & 0x0f) - 1);
        cal.set(Calendar.DATE, (int) (dosTime >> 16) & 0x1f);
        cal.set(Calendar.HOUR_OF_DAY, (int) (dosTime >> 11) & 0x1f);
        cal.set(Calendar.MINUTE, (int) (dosTime >> 5) & 0x3f);
        cal.set(Calendar.SECOND, (int) (dosTime << 1) & 0x3e);
        return cal.getTime();
    }

    /**
     * Retrieve a String from the given bytes using the encoding set
     * for this ZipFile.
     *
     * @param bytes the byte array to transform
     * @return String obtained by using the given encoding
     * @throws ZipException if the encoding cannot be recognized.
     */
    protected String getString(byte[] bytes) throws ZipException {
        if (encoding == null) {
            return new String(bytes);
        } else {
            try {
                return new String(bytes, encoding);
            } catch (UnsupportedEncodingException uee) {
                throw new ZipException(uee.getMessage());
            }
        }
    }

    /**
     * InputStream that delegates requests to the underlying
     * RandomAccessFile, making sure that only bytes from a certain
     * range can be read.
     */
    private class BoundedInputStream extends InputStream {
        private long remaining;
        private long loc;
        private boolean addDummyByte = false;

        BoundedInputStream(long start, long remaining) {
            this.remaining = remaining;
            loc = start;
        }

        public int read() throws IOException {
            if (remaining-- <= 0) {
                if (addDummyByte) {
                    addDummyByte = false;
                    return 0;
                }
                return -1;
            }
            synchronized (archive) {
                archive.seek(loc++);
                return archive.read();
            }
        }

        public int read(byte[] b, int off, int len) throws IOException {
            if (remaining <= 0) {
                if (addDummyByte) {
                    addDummyByte = false;
                    b[off] = 0;
                    return 1;
                }
                return -1;
            }

            if (len <= 0) {
                return 0;
            }

            if (len > remaining) {
                len = (int) remaining;
            }
            int ret = -1;
            synchronized (archive) {
                archive.seek(loc);
                ret = archive.read(b, off, len);
            }
            if (ret > 0) {
                loc += ret;
                remaining -= ret;
            }
            return ret;
        }

        /**
         * Inflater needs an extra dummy byte for nowrap - see
         * Inflater's javadocs.
         */
        void addDummy() {
            addDummyByte = true;
        }
    }

}
