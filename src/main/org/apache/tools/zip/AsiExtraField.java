/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2002 The Apache Software Foundation.  All rights
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

package org.apache.tools.zip;

import java.util.zip.CRC32;
import java.util.zip.ZipException;

/**
 * Adds Unix file permission and UID/GID fields as well as symbolic
 * link handling.
 *
 * <p>This class uses the ASi extra field in the format:
 * <pre>
 *         Value         Size            Description
 *         -----         ----            -----------
 * (Unix3) 0x756e        Short           tag for this extra block type
 *         TSize         Short           total data size for this block
 *         CRC           Long            CRC-32 of the remaining data
 *         Mode          Short           file permissions
 *         SizDev        Long            symlink'd size OR major/minor dev num
 *         UID           Short           user ID
 *         GID           Short           group ID
 *         (var.)        variable        symbolic link filename
 * </pre>
 * taken from appnote.iz (Info-ZIP note, 981119) found at <a
 * href="ftp://ftp.uu.net/pub/archiving/zip/doc/">ftp://ftp.uu.net/pub/archiving/zip/doc/</a></p>

 *
 * <p>Short is two bytes and Long is four bytes in big endian byte and
 * word order, device numbers are currently not supported.</p>
 *
 * @author <a href="stefan.bodewig@epost.de">Stefan Bodewig</a>
 * @version $Revision$
 */
public class AsiExtraField implements ZipExtraField, UnixStat, Cloneable {

    private static final ZipShort HEADER_ID = new ZipShort(0x756E);

    /**
     * Standard Unix stat(2) file mode.
     *
     * @since 1.1
     */
    private int mode = 0;
    /**
     * User ID.
     *
     * @since 1.1
     */
    private int uid = 0;
    /**
     * Group ID.
     *
     * @since 1.1
     */
    private int gid = 0;
    /**
     * File this entry points to, if it is a symbolic link.
     *
     * <p>empty string - if entry is not a symbolic link.</p>
     *
     * @since 1.1
     */
    private String link = "";
    /**
     * Is this an entry for a directory?
     *
     * @since 1.1
     */
    private boolean dirFlag = false;

    /**
     * Instance used to calculate checksums.
     *
     * @since 1.1
     */
    private CRC32 crc = new CRC32();

    public AsiExtraField() {
    }

    /**
     * The Header-ID.
     *
     * @since 1.1
     */
    public ZipShort getHeaderId() {
        return HEADER_ID;
    }

    /**
     * Length of the extra field in the local file data - without
     * Header-ID or length specifier.
     *
     * @since 1.1
     */
    public ZipShort getLocalFileDataLength() {
        return new ZipShort(4         // CRC
                          + 2         // Mode
                          + 4         // SizDev
                          + 2         // UID
                          + 2         // GID
                          + getLinkedFile().getBytes().length);
    }

    /**
     * Delegate to local file data.
     *
     * @since 1.1
     */
    public ZipShort getCentralDirectoryLength() {
        return getLocalFileDataLength();
    }

    /**
     * The actual data to put into local file data - without Header-ID
     * or length specifier.
     *
     * @since 1.1
     */
    public byte[] getLocalFileDataData() {
        // CRC will be added later
        byte[] data = new byte[getLocalFileDataLength().getValue() - 4];
        System.arraycopy((new ZipShort(getMode())).getBytes(), 0, data, 0, 2);

        byte[] linkArray = getLinkedFile().getBytes();
        System.arraycopy((new ZipLong(linkArray.length)).getBytes(),
                         0, data, 2, 4);

        System.arraycopy((new ZipShort(getUserId())).getBytes(), 
                         0, data, 6, 2);
        System.arraycopy((new ZipShort(getGroupId())).getBytes(),
                         0, data, 8, 2);

        System.arraycopy(linkArray, 0, data, 10, linkArray.length);

        crc.reset();
        crc.update(data);
        long checksum = crc.getValue();

        byte[] result = new byte[data.length + 4];
        System.arraycopy((new ZipLong(checksum)).getBytes(), 0, result, 0, 4);
        System.arraycopy(data, 0, result, 4, data.length);
        return result;
    }

    /**
     * Delegate to local file data.
     *
     * @since 1.1
     */
    public byte[] getCentralDirectoryData() {
        return getLocalFileDataData();
    }

    /**
     * Set the user id.
     *
     * @since 1.1
     */
    public void setUserId(int uid) {
        this.uid = uid;
    }

    /**
     * Get the user id.
     *
     * @since 1.1
     */
    public int getUserId() {
        return uid;
    }

    /**
     * Set the group id.
     *
     * @since 1.1
     */
    public void setGroupId(int gid) {
        this.gid = gid;
    }

    /**
     * Get the group id.
     *
     * @since 1.1
     */
    public int getGroupId() {
        return gid;
    }

    /**
     * Indicate that this entry is a symbolic link to the given filename.
     *
     * @param name Name of the file this entry links to, empty String
     *             if it is not a symbolic link.
     *
     * @since 1.1
     */
    public void setLinkedFile(String name) {
        link = name;
        mode = getMode(mode);
    }

    /**
     * Name of linked file
     *
     * @return name of the file this entry links to if it is a
     *         symbolic link, the empty string otherwise.
     *
     * @since 1.1
     */
    public String getLinkedFile() {
        return link;
    }

    /**
     * Is this entry a symbolic link?
     *
     * @since 1.1
     */
    public boolean isLink() {
        return getLinkedFile().length() != 0;
    }

    /**
     * File mode of this file.
     *
     * @since 1.1
     */
    public void setMode(int mode) {
        this.mode = getMode(mode);
    }

    /**
     * File mode of this file.
     *
     * @since 1.1
     */
    public int getMode() {
        return mode;
    }

    /**
     * Indicate whether this entry is a directory.
     *
     * @since 1.1
     */
    public void setDirectory(boolean dirFlag) {
        this.dirFlag = dirFlag;
        mode = getMode(mode);
    }

    /**
     * Is this entry a directory?
     *
     * @since 1.1
     */
    public boolean isDirectory() {
        return dirFlag && !isLink();
    }

    /**
     * Populate data from this array as if it was in local file data.
     *
     * @since 1.1
     */
    public void parseFromLocalFileData(byte[] data, int offset, int length)
        throws ZipException {

        long givenChecksum = (new ZipLong(data, offset)).getValue();
        byte[] tmp = new byte[length - 4];
        System.arraycopy(data, offset + 4, tmp, 0, length - 4);
        crc.reset();
        crc.update(tmp);
        long realChecksum = crc.getValue();
        if (givenChecksum != realChecksum) {
            throw new ZipException("bad CRC checksum " 
                                   + Long.toHexString(givenChecksum)
                                   + " instead of " 
                                   + Long.toHexString(realChecksum));
        }
        
        int newMode = (new ZipShort(tmp, 0)).getValue();
        byte[] linkArray = new byte[(int) (new ZipLong(tmp, 2)).getValue()];
        uid = (new ZipShort(tmp, 6)).getValue();
        gid = (new ZipShort(tmp, 8)).getValue();

        if (linkArray.length == 0) {
            link = "";
        } else {
            System.arraycopy(tmp, 10, linkArray, 0, linkArray.length);
            link = new String(linkArray);
        }
        setDirectory((newMode & DIR_FLAG) != 0);
        setMode(newMode);
    }

    /**
     * Get the file mode for given permissions with the correct file type.
     *
     * @since 1.1
     */
    protected int getMode(int mode) {
        int type = FILE_FLAG;
        if (isLink()) {
            type = LINK_FLAG;
        } else if (isDirectory()) {
            type = DIR_FLAG;
        }
        return type | (mode & PERM_MASK);
    }
    
}
