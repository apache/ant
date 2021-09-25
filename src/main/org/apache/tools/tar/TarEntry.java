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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Locale;

import org.apache.tools.zip.ZipEncoding;

/**
 * This class represents an entry in a Tar archive. It consists
 * of the entry's header, as well as the entry's File. Entries
 * can be instantiated in one of three ways, depending on how
 * they are to be used.
 * <p>
 * TarEntries that are created from the header bytes read from
 * an archive are instantiated with the TarEntry(byte[])
 * constructor. These entries will be used when extracting from
 * or listing the contents of an archive. These entries have their
 * header filled in using the header bytes. They also set the File
 * to null, since they reference an archive entry not a file.
 * </p>
 * <p>
 * TarEntries that are created from Files that are to be written
 * into an archive are instantiated with the TarEntry(File)
 * constructor. These entries have their header filled in using
 * the File's information. They also keep a reference to the File
 * for convenience when writing entries.
 * </p>
 * <p>
 * Finally, TarEntries can be constructed from nothing but a name.
 * This allows the programmer to construct the entry by hand, for
 * instance when only an InputStream is available for writing to
 * the archive, and the header information is constructed from
 * other information. In this case the header fields are set to
 * defaults and the File is set to null.
 * </p>
 * The C structure for a Tar Entry's header is:
 * <pre>
 * struct header {
 * char name[NAMSIZ];
 * char mode[8];
 * char uid[8];
 * char gid[8];
 * char size[12];
 * char mtime[12];
 * char chksum[8];
 * char linkflag;
 * char linkname[NAMSIZ];
 * char magic[8];
 * char uname[TUNMLEN];
 * char gname[TGNMLEN];
 * char devmajor[8];
 * char devminor[8];
 * } header;
 * All unused bytes are set to null.
 * New-style GNU tar files are slightly different from the above.
 * For values of size larger than 077777777777L (11 7s)
 * or uid and gid larger than 07777777L (7 7s)
 * the sign bit of the first byte is set, and the rest of the
 * field is the binary representation of the number.
 * See TarUtils.parseOctalOrBinary.
 * </pre>
 * The C structure for a old GNU Tar Entry's header is:
 * <pre>
 * struct oldgnu_header {
 * char unused_pad1[345]; // TarConstants.PAD1LEN_GNU       - offset 0
 * char atime[12];        // TarConstants.ATIMELEN_GNU      - offset 345
 * char ctime[12];        // TarConstants.CTIMELEN_GNU      - offset 357
 * char offset[12];       // TarConstants.OFFSETLEN_GNU     - offset 369
 * char longnames[4];     // TarConstants.LONGNAMESLEN_GNU  - offset 381
 * char unused_pad2;      // TarConstants.PAD2LEN_GNU       - offset 385
 * struct sparse sp[4];   // TarConstants.SPARSELEN_GNU     - offset 386
 * char isextended;       // TarConstants.ISEXTENDEDLEN_GNU - offset 482
 * char realsize[12];     // TarConstants.REALSIZELEN_GNU   - offset 483
 * char unused_pad[17];   // TarConstants.PAD3LEN_GNU       - offset 495
 * };
 * </pre>
 * Whereas, "struct sparse" is:
 * <pre>
 * struct sparse {
 * char offset[12];   // offset 0
 * char numbytes[12]; // offset 12
 * };
 * </pre>
 */

public class TarEntry implements TarConstants {
    /** The entry's name. */
    private String name;

    /** The entry's permission mode. */
    private int mode;

    /** The entry's user id. */
    private long userId;

    /** The entry's group id. */
    private long groupId;

    /** The entry's size. */
    private long size;

    /** The entry's modification time. */
    private long modTime;

    /** The entry's link flag. */
    private byte linkFlag;

    /** The entry's link name. */
    private String linkName;

    /** The entry's magic tag. */
    private String magic;
    /** The version of the format */
    private String version;

    /** The entry's user name. */
    private String userName;

    /** The entry's group name. */
    private String groupName;

    /** The entry's major device number. */
    private int devMajor;

    /** The entry's minor device number. */
    private int devMinor;

    /** If an extension sparse header follows. */
    private boolean isExtended;

    /** The entry's real size in case of a sparse file. */
    private long realSize;

    /** The entry's file reference */
    private File file;

    /** Maximum length of a user's name in the tar file */
    public static final int MAX_NAMELEN = 31;

    /** Default permissions bits for directories */
    public static final int DEFAULT_DIR_MODE = 040755;

    /** Default permissions bits for files */
    public static final int DEFAULT_FILE_MODE = 0100644;

    /** Convert millis to seconds */
    public static final int MILLIS_PER_SECOND = 1000;

    /**
     * Construct an empty entry and prepares the header values.
     */
    private TarEntry() {
        this.magic = MAGIC_POSIX;
        this.version = VERSION_POSIX;
        this.name = "";
        this.linkName = "";

        String user = System.getProperty("user.name", "");

        if (user.length() > MAX_NAMELEN) {
            user = user.substring(0, MAX_NAMELEN);
        }

        this.userId = 0;
        this.groupId = 0;
        this.userName = user;
        this.groupName = "";
        this.file = null;
    }

    /**
     * Construct an entry with only a name. This allows the programmer
     * to construct the entry's header "by hand". File is set to null.
     *
     * @param name the entry name
     */
    public TarEntry(String name) {
        this(name, false);
    }

    /**
     * Construct an entry with only a name. This allows the programmer
     * to construct the entry's header "by hand". File is set to null.
     *
     * @param name the entry name
     * @param preserveLeadingSlashes whether to allow leading slashes
     * in the name.
     */
    public TarEntry(String name, boolean preserveLeadingSlashes) {
        this();

        name = normalizeFileName(name, preserveLeadingSlashes);
        boolean isDir = name.endsWith("/");

        this.devMajor = 0;
        this.devMinor = 0;
        this.name = name;
        this.mode = isDir ? DEFAULT_DIR_MODE : DEFAULT_FILE_MODE;
        this.linkFlag = isDir ? LF_DIR : LF_NORMAL;
        this.userId = 0;
        this.groupId = 0;
        this.size = 0;
        this.modTime = (new Date()).getTime() / MILLIS_PER_SECOND;
        this.linkName = "";
        this.userName = "";
        this.groupName = "";
    }

    /**
     * Construct an entry with a name and a link flag.
     *
     * @param name the entry name
     * @param linkFlag the entry link flag.
     */
    public TarEntry(String name, byte linkFlag) {
        this(name);
        this.linkFlag = linkFlag;
        if (linkFlag == LF_GNUTYPE_LONGNAME) {
            magic = GNU_TMAGIC;
            version = VERSION_GNU_SPACE;
        }
    }

    /**
     * Construct an entry for a file. File is set to file, and the
     * header is constructed from information from the file.
     * The name is set from the normalized file path.
     *
     * @param file The file that the entry represents.
     */
    public TarEntry(File file) {
        this(file, file.getPath());
    }

    /**
     * Construct an entry for a file. File is set to file, and the
     * header is constructed from information from the file.
     *
     * @param file The file that the entry represents.
     * @param fileName the name to be used for the entry.
     */
    public TarEntry(File file, String fileName) {
        this();

        String normalizedName = normalizeFileName(fileName, false);
        this.file = file;

        this.linkName = "";

        if (file.isDirectory()) {
            this.mode = DEFAULT_DIR_MODE;
            this.linkFlag = LF_DIR;

            int nameLength = normalizedName.length();
            if (nameLength == 0 || normalizedName.charAt(nameLength - 1) != '/') {
                this.name = normalizedName + "/";
            } else {
                this.name = normalizedName;
            }
            this.size = 0;
        } else {
            this.mode = DEFAULT_FILE_MODE;
            this.linkFlag = LF_NORMAL;
            this.size = file.length();
            this.name = normalizedName;
        }

        this.modTime = file.lastModified() / MILLIS_PER_SECOND;
        this.devMajor = 0;
        this.devMinor = 0;
    }

    /**
     * Construct an entry from an archive's header bytes. File is set
     * to null.
     *
     * @param headerBuf The header bytes from a tar archive entry.
     * @throws IllegalArgumentException if any of the numeric fields have an invalid format
     */
    public TarEntry(byte[] headerBuf) {
        this();
        parseTarHeader(headerBuf);
    }

    /**
     * Construct an entry from an archive's header bytes. File is set
     * to null.
     *
     * @param headerBuf The header bytes from a tar archive entry.
     * @param encoding encoding to use for file names
     * @throws IllegalArgumentException if any of the numeric fields have an invalid format
     * @throws IOException if an error occurs during reading the archive
     */
    public TarEntry(byte[] headerBuf, ZipEncoding encoding)
        throws IOException {
        this();
        parseTarHeader(headerBuf, encoding);
    }

    /**
     * Determine if the two entries are equal. Equality is determined
     * by the header names being equal.
     *
     * @param it Entry to be checked for equality.
     * @return True if the entries are equal.
     */
    public boolean equals(TarEntry it) {
        return it != null && getName().equals(it.getName());
    }

    /**
     * Determine if the two entries are equal. Equality is determined
     * by the header names being equal.
     *
     * @param it Entry to be checked for equality.
     * @return True if the entries are equal.
     */
    @Override
    public boolean equals(Object it) {
        return it != null && getClass() == it.getClass() && equals((TarEntry) it);
    }

    /**
     * Hashcodes are based on entry names.
     *
     * @return the entry hashcode
     */
    @Override
    public int hashCode() {
        return getName().hashCode();
    }

    /**
     * Determine if the given entry is a descendant of this entry.
     * Descendancy is determined by the name of the descendant
     * starting with this entry's name.
     *
     * @param desc Entry to be checked as a descendant of this.
     * @return True if entry is a descendant of this.
     */
    public boolean isDescendent(TarEntry desc) {
        return desc.getName().startsWith(getName());
    }

    /**
     * Get this entry's name.
     *
     * @return This entry's name.
     */
    public String getName() {
        return name;
    }

    /**
     * Set this entry's name.
     *
     * @param name This entry's new name.
     */
    public void setName(String name) {
        this.name = normalizeFileName(name, false);
    }

    /**
     * Set the mode for this entry
     *
     * @param mode the mode for this entry
     */
    public void setMode(int mode) {
        this.mode = mode;
    }

    /**
     * Get this entry's link flag.
     *
     * @return This entry's link flag.
     * @since 1.10.12
     */
    public byte getLinkFlag() {
        return linkFlag;
    }

    /**
     * Set this entry's link flag.
     *
     * @param link the link flag to use.
     * @since 1.10.12
     */
    public void setLinkFlag(byte linkFlag) {
        this.linkFlag = linkFlag;
    }

    /**
     * Get this entry's link name.
     *
     * @return This entry's link name.
     */
    public String getLinkName() {
        return linkName;
    }

    /**
     * Set this entry's link name.
     *
     * @param link the link name to use.
     */
    public void setLinkName(String link) {
        this.linkName = link;
    }

    /**
     * Get this entry's user id.
     *
     * @return This entry's user id.
     * @deprecated use #getLongUserId instead as user ids can be
     * bigger than {@link Integer#MAX_VALUE}
     */
    @Deprecated
    public int getUserId() {
        return (int) userId;
    }

    /**
     * Set this entry's user id.
     *
     * @param userId This entry's new user id.
     */
    public void setUserId(int userId) {
        setUserId((long) userId);
    }

    /**
     * Get this entry's user id.
     *
     * @return This entry's user id.
     * @since 1.9.5
     */
    public long getLongUserId() {
        return userId;
    }

    /**
     * Set this entry's user id.
     *
     * @param userId This entry's new user id.
     * @since 1.9.5
     */
    public void setUserId(long userId) {
        this.userId = userId;
    }

    /**
     * Get this entry's group id.
     *
     * @return This entry's group id.
     * @deprecated use #getLongGroupId instead as group ids can be
     * bigger than {@link Integer#MAX_VALUE}
     */
    @Deprecated
    public int getGroupId() {
        return (int) groupId;
    }

    /**
     * Set this entry's group id.
     *
     * @param groupId This entry's new group id.
     */
    public void setGroupId(int groupId) {
        setGroupId((long) groupId);
    }

    /**
     * Get this entry's group id.
     *
     * @return This entry's group id.
     * @since 1.9.5
     */
    public long getLongGroupId() {
        return groupId;
    }

    /**
     * Set this entry's group id.
     *
     * @param groupId This entry's new group id.
     * @since 1.9.5
     */
    public void setGroupId(long groupId) {
        this.groupId = groupId;
    }

    /**
     * Get this entry's user name.
     *
     * @return This entry's user name.
     */
    public String getUserName() {
        return userName;
    }

    /**
     * Set this entry's user name.
     *
     * @param userName This entry's new user name.
     */
    public void setUserName(String userName) {
        this.userName = userName;
    }

    /**
     * Get this entry's group name.
     *
     * @return This entry's group name.
     */
    public String getGroupName() {
        return groupName;
    }

    /**
     * Set this entry's group name.
     *
     * @param groupName This entry's new group name.
     */
    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    /**
     * Convenience method to set this entry's group and user ids.
     *
     * @param userId This entry's new user id.
     * @param groupId This entry's new group id.
     */
    public void setIds(int userId, int groupId) {
        setUserId(userId);
        setGroupId(groupId);
    }

    /**
     * Convenience method to set this entry's group and user names.
     *
     * @param userName This entry's new user name.
     * @param groupName This entry's new group name.
     */
    public void setNames(String userName, String groupName) {
        setUserName(userName);
        setGroupName(groupName);
    }

    /**
     * Set this entry's modification time. The parameter passed
     * to this method is in "Java time".
     *
     * @param time This entry's new modification time.
     */
    public void setModTime(long time) {
        modTime = time / MILLIS_PER_SECOND;
    }

    /**
     * Set this entry's modification time.
     *
     * @param time This entry's new modification time.
     */
    public void setModTime(Date time) {
        modTime = time.getTime() / MILLIS_PER_SECOND;
    }

    /**
     * Set this entry's modification time.
     *
     * @return time This entry's new modification time.
     */
    public Date getModTime() {
        return new Date(modTime * MILLIS_PER_SECOND);
    }

    /**
     * Get this entry's file.
     *
     * @return This entry's file.
     */
    public File getFile() {
        return file;
    }

    /**
     * Get this entry's mode.
     *
     * @return This entry's mode.
     */
    public int getMode() {
        return mode;
    }

    /**
     * Get this entry's file size.
     *
     * @return This entry's file size.
     */
    public long getSize() {
        return size;
    }

    /**
     * Set this entry's file size.
     *
     * @param size This entry's new file size.
     * @throws IllegalArgumentException if the size is &lt; 0.
     */
    public void setSize(long size) {
        if (size < 0) {
            throw new IllegalArgumentException("Size is out of range: " + size);
        }
        this.size = size;
    }

    /**
     * Get this entry's major device number.
     *
     * @return This entry's major device number.
     */
    public int getDevMajor() {
        return devMajor;
    }

    /**
     * Set this entry's major device number.
     *
     * @param devNo This entry's major device number.
     * @throws IllegalArgumentException if the devNo is &lt; 0.
     */
    public void setDevMajor(int devNo) {
        if (devNo < 0) {
            throw new IllegalArgumentException("Major device number is out of "
                                               + "range: " + devNo);
        }
        this.devMajor = devNo;
    }

    /**
     * Get this entry's minor device number.
     *
     * @return This entry's minor device number.
     */
    public int getDevMinor() {
        return devMinor;
    }

    /**
     * Set this entry's minor device number.
     *
     * @param devNo This entry's minor device number.
     * @throws IllegalArgumentException if the devNo is &lt; 0.
     */
    public void setDevMinor(int devNo) {
        if (devNo < 0) {
            throw new IllegalArgumentException("Minor device number is out of "
                                               + "range: " + devNo);
        }
        this.devMinor = devNo;
    }

    /**
     * Indicates in case of a sparse file if an extension sparse header
     * follows.
     *
     * @return true if an extension sparse header follows.
     */
    public boolean isExtended() {
        return isExtended;
    }

    /**
     * Get this entry's real file size in case of a sparse file.
     *
     * @return This entry's real file size.
     */
    public long getRealSize() {
        return realSize;
    }

    /**
     * Indicate if this entry is a GNU sparse block.
     *
     * @return true if this is a sparse extension provided by GNU tar
     */
    public boolean isGNUSparse() {
        return linkFlag == LF_GNUTYPE_SPARSE;
    }

    /**
     * Indicate if this entry is a GNU long linkname block
     *
     * @return true if this is a long name extension provided by GNU tar
     */
    public boolean isGNULongLinkEntry() {
        return linkFlag == LF_GNUTYPE_LONGLINK;
    }

    /**
     * Indicate if this entry is a GNU long name block
     *
     * @return true if this is a long name extension provided by GNU tar
     */
    public boolean isGNULongNameEntry() {
        return linkFlag == LF_GNUTYPE_LONGNAME;
    }

    /**
     * Check if this is a Pax header.
     *
     * @return {@code true} if this is a Pax header.
     */
    public boolean isPaxHeader() {
        return linkFlag == LF_PAX_EXTENDED_HEADER_LC
            || linkFlag == LF_PAX_EXTENDED_HEADER_UC;
    }

    /**
     * Check if this is a Pax header.
     *
     * @return {@code true} if this is a Pax header.
     */
    public boolean isGlobalPaxHeader() {
        return linkFlag == LF_PAX_GLOBAL_EXTENDED_HEADER;
    }

    /**
     * Return whether or not this entry represents a directory.
     *
     * @return True if this entry is a directory.
     */
    public boolean isDirectory() {
        if (file != null) {
            return file.isDirectory();
        }

        return linkFlag == LF_DIR || getName().endsWith("/");
    }

    /**
     * Check if this is a "normal file".
     * @return <i>true</i> if it is a 'normal' file
     */
    public boolean isFile() {
        return file != null ? file.isFile()
                : linkFlag == LF_OLDNORM || linkFlag == LF_NORMAL || !getName().endsWith("/");
    }

    /**
     * Check if this is a symbolic link entry.
     * @return <i>true</i> if it is a symlink
     */
    public boolean isSymbolicLink() {
        return linkFlag == LF_SYMLINK;
    }

    /**
     * Check if this is a link entry.
     * @return <i>true</i> if it is a link
     */
    public boolean isLink() {
        return linkFlag == LF_LINK;
    }

    /**
     * Check if this is a character device entry.
     * @return <i>true</i> if it is a character device entry
     */
    public boolean isCharacterDevice() {
        return linkFlag == LF_CHR;
    }

    /**
     * @return <i>true</i> if this is a block device entry.
     */
    public boolean isBlockDevice() {
        return linkFlag == LF_BLK;
    }

    /**
     * @return <i>true</i> if this is a FIFO (pipe) entry.
     */
    public boolean isFIFO() {
        return linkFlag == LF_FIFO;
    }

    /**
     * If this entry represents a file, and the file is a directory, return
     * an array of TarEntries for this entry's children.
     *
     * @return An array of TarEntry's for this entry's children.
     */
    public TarEntry[] getDirectoryEntries() {
        if (file == null || !file.isDirectory()) {
            return new TarEntry[0];
        }

        String[]   list = file.list();
        TarEntry[] result = new TarEntry[list.length];

        for (int i = 0; i < list.length; ++i) {
            result[i] = new TarEntry(new File(file, list[i]));
        }

        return result;
    }

    /**
     * Write an entry's header information to a header buffer.
     *
     * <p>This method does not use the star/GNU tar/BSD tar extensions.</p>
     *
     * @param outbuf The tar entry header buffer to fill in.
     */
    public void writeEntryHeader(byte[] outbuf) {
        try {
            writeEntryHeader(outbuf, TarUtils.DEFAULT_ENCODING, false);
        } catch (IOException ex) {
            try {
                writeEntryHeader(outbuf, TarUtils.FALLBACK_ENCODING, false);
            } catch (IOException ex2) {
                // impossible
                throw new RuntimeException(ex2); //NOSONAR
            }
        }
    }

    /**
     * Write an entry's header information to a header buffer.
     *
     * @param outbuf The tar entry header buffer to fill in.
     * @param encoding encoding to use when writing the file name.
     * @param starMode whether to use the star/GNU tar/BSD tar
     * extension for numeric fields if their value doesn't fit in the
     * maximum size of standard tar archives
     * @throws IOException if an error occurs while writing the archive
     */
    public void writeEntryHeader(byte[] outbuf, ZipEncoding encoding,
                                 boolean starMode) throws IOException {
        int offset = 0;

        offset = TarUtils.formatNameBytes(name, outbuf, offset, NAMELEN,
                                          encoding);
        offset = writeEntryHeaderField(mode, outbuf, offset, MODELEN, starMode);
        offset = writeEntryHeaderField(userId, outbuf, offset, UIDLEN,
                                       starMode);
        offset = writeEntryHeaderField(groupId, outbuf, offset, GIDLEN,
                                       starMode);
        offset = writeEntryHeaderField(size, outbuf, offset, SIZELEN, starMode);
        offset = writeEntryHeaderField(modTime, outbuf, offset, MODTIMELEN,
                                       starMode);

        int csOffset = offset;

        for (int c = 0; c < CHKSUMLEN; ++c) {
            outbuf[offset++] = (byte) ' ';
        }

        outbuf[offset++] = linkFlag;
        offset = TarUtils.formatNameBytes(linkName, outbuf, offset, NAMELEN,
                                          encoding);
        offset = TarUtils.formatNameBytes(magic, outbuf, offset, PURE_MAGICLEN);
        offset = TarUtils.formatNameBytes(version, outbuf, offset, VERSIONLEN);
        offset = TarUtils.formatNameBytes(userName, outbuf, offset, UNAMELEN,
                                          encoding);
        offset = TarUtils.formatNameBytes(groupName, outbuf, offset, GNAMELEN,
                                          encoding);
        offset = writeEntryHeaderField(devMajor, outbuf, offset, DEVLEN,
                                       starMode);
        offset = writeEntryHeaderField(devMinor, outbuf, offset, DEVLEN,
                                       starMode);

        while (offset < outbuf.length) {
            outbuf[offset++] = 0;
        }

        long chk = TarUtils.computeCheckSum(outbuf);

        TarUtils.formatCheckSumOctalBytes(chk, outbuf, csOffset, CHKSUMLEN);
    }

    private int writeEntryHeaderField(long value, byte[] outbuf, int offset,
                                      int length, boolean starMode) {
        if (!starMode && (value < 0
                          || value >= (1L << (3 * (length - 1))))) {
            // value doesn't fit into field when written as octal
            // number, will be written to PAX header or causes an
            // error
            return TarUtils.formatLongOctalBytes(0, outbuf, offset, length);
        }
        return TarUtils.formatLongOctalOrBinaryBytes(value, outbuf, offset,
                                                     length);
    }

    /**
     * Parse an entry's header information from a header buffer.
     *
     * @param header The tar entry header buffer to get information from.
     * @throws IllegalArgumentException if any of the numeric fields have an invalid format
     */
    public void parseTarHeader(byte[] header) {
        try {
            parseTarHeader(header, TarUtils.DEFAULT_ENCODING);
        } catch (IOException ex) {
            try {
                parseTarHeader(header, TarUtils.DEFAULT_ENCODING, true);
            } catch (IOException ex2) {
                // not really possible
                throw new RuntimeException(ex2); //NOSONAR
            }
        }
    }

    /**
     * Parse an entry's header information from a header buffer.
     *
     * @param header The tar entry header buffer to get information from.
     * @param encoding encoding to use for file names
     * @throws IllegalArgumentException if any of the numeric fields
     * have an invalid format
     * @throws IOException if an error occurs while reading the archive
     */
    public void parseTarHeader(byte[] header, ZipEncoding encoding)
        throws IOException {
        parseTarHeader(header, encoding, false);
    }

    private void parseTarHeader(byte[] header, ZipEncoding encoding,
                                final boolean oldStyle)
        throws IOException {
        int offset = 0;

        name = oldStyle ? TarUtils.parseName(header, offset, NAMELEN)
            : TarUtils.parseName(header, offset, NAMELEN, encoding);
        offset += NAMELEN;
        mode = (int) TarUtils.parseOctalOrBinary(header, offset, MODELEN);
        offset += MODELEN;
        userId = (int) TarUtils.parseOctalOrBinary(header, offset, UIDLEN);
        offset += UIDLEN;
        groupId = (int) TarUtils.parseOctalOrBinary(header, offset, GIDLEN);
        offset += GIDLEN;
        size = TarUtils.parseOctalOrBinary(header, offset, SIZELEN);
        offset += SIZELEN;
        modTime = TarUtils.parseOctalOrBinary(header, offset, MODTIMELEN);
        offset += MODTIMELEN;
        offset += CHKSUMLEN;
        linkFlag = header[offset++];
        linkName = oldStyle ? TarUtils.parseName(header, offset, NAMELEN)
            : TarUtils.parseName(header, offset, NAMELEN, encoding);
        offset += NAMELEN;
        magic = TarUtils.parseName(header, offset, PURE_MAGICLEN);
        offset += PURE_MAGICLEN;
        version = TarUtils.parseName(header, offset, VERSIONLEN);
        offset += VERSIONLEN;
        userName = oldStyle ? TarUtils.parseName(header, offset, UNAMELEN)
            : TarUtils.parseName(header, offset, UNAMELEN, encoding);
        offset += UNAMELEN;
        groupName = oldStyle ? TarUtils.parseName(header, offset, GNAMELEN)
            : TarUtils.parseName(header, offset, GNAMELEN, encoding);
        offset += GNAMELEN;
        devMajor = (int) TarUtils.parseOctalOrBinary(header, offset, DEVLEN);
        offset += DEVLEN;
        devMinor = (int) TarUtils.parseOctalOrBinary(header, offset, DEVLEN);
        offset += DEVLEN;

        int type = evaluateType(header);
        switch (type) {
            case FORMAT_OLDGNU: {
                offset += ATIMELEN_GNU;
                offset += CTIMELEN_GNU;
                offset += OFFSETLEN_GNU;
                offset += LONGNAMESLEN_GNU;
                offset += PAD2LEN_GNU;
                offset += SPARSELEN_GNU;
                isExtended = TarUtils.parseBoolean(header, offset);
                offset += ISEXTENDEDLEN_GNU;
                realSize = TarUtils.parseOctal(header, offset, REALSIZELEN_GNU);
                offset += REALSIZELEN_GNU;
                break;
            }
            case FORMAT_POSIX:
            default: {
                String prefix = oldStyle ? TarUtils.parseName(header, offset, PREFIXLEN)
                        : TarUtils.parseName(header, offset, PREFIXLEN, encoding);
                // SunOS tar -E does not add / to directory names, so fix
                // up to be consistent
                if (isDirectory() && !name.endsWith("/")) {
                    name += "/";
                }
                if (!prefix.isEmpty()) {
                    name = prefix + "/" + name;
                }
            }
        }
    }

    /**
     * Strips Windows' drive letter as well as any leading slashes,
     * turns path separators into forward slashes.
     */
    private static String normalizeFileName(String fileName,
                                            boolean preserveLeadingSlashes) {
        String osname = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);

        if (osname != null) {

            // Strip off drive letters!
            // REVIEW Would a better check be "(File.separator == '\')"?

            if (osname.startsWith("windows")) {
                if (fileName.length() > 2) {
                    char ch1 = fileName.charAt(0);
                    char ch2 = fileName.charAt(1);

                    if (ch2 == ':'
                        && ((ch1 >= 'a' && ch1 <= 'z')
                            || (ch1 >= 'A' && ch1 <= 'Z'))) {
                        fileName = fileName.substring(2);
                    }
                }
            } else if (osname.contains("netware")) {
                int colon = fileName.indexOf(':');
                if (colon != -1) {
                    fileName = fileName.substring(colon + 1);
                }
            }
        }

        fileName = fileName.replace(File.separatorChar, '/');

        // No absolute pathnames
        // Windows (and Posix?) paths can start with "\\NetworkDrive\",
        // so we loop on starting /'s.
        while (!preserveLeadingSlashes && fileName.startsWith("/")) {
            fileName = fileName.substring(1);
        }
        return fileName;
    }

    /**
     * Evaluate an entry's header format from a header buffer.
     *
     * @param header The tar entry header buffer to evaluate the format for.
     * @return format type
     */
    private int evaluateType(byte[] header) {
        if (matchAsciiBuffer(GNU_TMAGIC, header, MAGIC_OFFSET, PURE_MAGICLEN)) {
            return FORMAT_OLDGNU;
        }
        if (matchAsciiBuffer(MAGIC_POSIX, header, MAGIC_OFFSET, PURE_MAGICLEN)) {
            return FORMAT_POSIX;
        }
        return 0;
    }

    /**
     * Check if buffer contents matches Ascii String.
     *
     * @param expected String
     * @param buffer byte[]
     * @param offset int
     * @param length int
     * @return {@code true} if buffer is the same as the expected string
     */
    private static boolean matchAsciiBuffer(String expected, byte[] buffer,
                                            int offset, int length) {
        byte[] buffer1 = expected.getBytes(StandardCharsets.US_ASCII);
        return isEqual(buffer1, 0, buffer1.length, buffer, offset, length,
                       false);
    }

    /**
     * Compare byte buffers, optionally ignoring trailing nulls
     *
     * @param buffer1 byte[]
     * @param offset1 int
     * @param length1 int
     * @param buffer2 byte[]
     * @param offset2 int
     * @param length2 int
     * @param ignoreTrailingNulls boolean
     * @return {@code true} if buffer1 and buffer2 have same contents, having regard to trailing nulls
     */
    private static boolean isEqual(
            final byte[] buffer1, final int offset1, final int length1,
            final byte[] buffer2, final int offset2, final int length2,
            boolean ignoreTrailingNulls) {
        int minLen = (length1 < length2) ? length1 : length2;
        for (int i = 0; i < minLen; i++) {
            if (buffer1[offset1 + i] != buffer2[offset2 + i]) {
                return false;
            }
        }
        if (length1 == length2) {
            return true;
        }
        if (ignoreTrailingNulls) {
            if (length1 > length2) {
                for (int i = length2; i < length1; i++) {
                    if (buffer1[offset1 + i] != 0) {
                        return false;
                    }
                }
            } else {
                for (int i = length1; i < length2; i++) {
                    if (buffer2[offset2 + i] != 0) {
                        return false;
                    }
                }
            }
            return true;
        }
        return false;
    }
}
