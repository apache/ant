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
 * 4. The names "The Jakarta Project", "Tomcat", and "Apache Software
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
import java.util.*;

/**
 * This class represents an entry in a Tar archive. It consists
 * of the entry's header, as well as the entry's File. Entries
 * can be instantiated in one of three ways, depending on how
 * they are to be used.
 * <p>
 * TarEntries that are created from the header bytes read from
 * an archive are instantiated with the TarEntry( byte[] )
 * constructor. These entries will be used when extracting from
 * or listing the contents of an archive. These entries have their
 * header filled in using the header bytes. They also set the File
 * to null, since they reference an archive entry not a file.
 * <p>
 * TarEntries that are created from Files that are to be written
 * into an archive are instantiated with the TarEntry( File )
 * constructor. These entries have their header filled in using
 * the File's information. They also keep a reference to the File
 * for convenience when writing entries.
 * <p>
 * Finally, TarEntries can be constructed from nothing but a name.
 * This allows the programmer to construct the entry by hand, for
 * instance when only an InputStream is available for writing to
 * the archive, and the header information is constructed from
 * other information. In this case the header fields are set to
 * defaults and the File is set to null.
 * 
 * <p>
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
 * </pre>
 * 
 * @author Timothy Gerard Endres <a href="mailto:time@ice.com">time@ice.com</a>
 * @author Stefano Mazzocchi <a href="mailto:stefano@apache.org">stefano@apache.org</a>
 */
 
public class TarEntry implements TarConstants {

    private StringBuffer name;      /** The entry's name. */
    private int          mode;      /** The entry's permission mode. */
    private int          userId;    /** The entry's user id. */
    private int          groupId;   /** The entry's group id. */
    private long         size;      /** The entry's size. */
    private long         modTime;   /** The entry's modification time. */
    private int          checkSum;  /** The entry's checksum. */
    private byte         linkFlag;  /** The entry's link flag. */
    private StringBuffer linkName;  /** The entry's link name. */
    private StringBuffer magic;     /** The entry's magic tag. */
    private StringBuffer userName;  /** The entry's user name. */
    private StringBuffer groupName; /** The entry's group name. */
    private int          devMajor;  /** The entry's major device number. */
    private int          devMinor;  /** The entry's minor device number. */
    private File         file;      /** The entry's file reference */ 

    /** 
     * Construct an empty entry and prepares the header values.
     */ 
    private TarEntry () {
        this.magic = new StringBuffer(TMAGIC);
        this.name = new StringBuffer();
        this.linkName = new StringBuffer();
    
        String user = System.getProperty("user.name", "");
    
        if (user.length() > 31) {
            user = user.substring(0, 31);
        } 
    
        this.userId = 0;
        this.groupId = 0;
        this.userName = new StringBuffer(user);
        this.groupName = new StringBuffer("");
        this.file = null;
    }
        
    /** 
     * Construct an entry with only a name. This allows the programmer
     * to construct the entry's header "by hand". File is set to null.
     */ 
    public TarEntry(String name) {
        this();
        
        boolean isDir = name.endsWith("/");
        
        this.checkSum = 0;
        this.devMajor = 0;
        this.devMinor = 0;
        this.name = new StringBuffer(name);
        this.mode = isDir ? 040755 : 0100644;
        this.linkFlag = isDir ? LF_DIR : LF_NORMAL;
        this.userId = 0;
        this.groupId = 0;
        this.size = 0;
        this.checkSum = 0;
        this.modTime = (new Date()).getTime() / 1000;
        this.linkName = new StringBuffer("");
        this.userName = new StringBuffer("");
        this.groupName = new StringBuffer("");
        this.devMajor = 0;
        this.devMinor = 0;
    }   
        
    /** 
     * Construct an entry for a file. File is set to file, and the
     * header is constructed from information from the file.
     *  
     * @param file The file that the entry represents.
     */ 
    public TarEntry(File file) {
        this();
        
        this.file = file;
        
        String name = file.getPath();
        String osname = System.getProperty("os.name");
        
        if (osname != null) {
        
            // Strip off drive letters!
            // REVIEW Would a better check be "(File.separator == '\')"?
            String Win32Prefix = "Windows";
            String prefix = osname.substring(0, Win32Prefix.length());
        
            if (prefix.equalsIgnoreCase(Win32Prefix)) {
                if (name.length() > 2) {
                    char ch1 = name.charAt(0);
                    char ch2 = name.charAt(1);
        
                    if (ch2 == ':' 
                            && ((ch1 >= 'a' && ch1 <= 'z') 
                                || (ch1 >= 'A' && ch1 <= 'Z'))) {
                        name = name.substring(2);
                    } 
                } 
            } 
        } 
        
        name = name.replace(File.separatorChar, '/');
        
        // No absolute pathnames
        // Windows (and Posix?) paths can start with "\\NetworkDrive\",
        // so we loop on starting /'s.
        while (name.startsWith("/")) {
            name = name.substring(1);
        }
        
        this.linkName = new StringBuffer("");
        this.name = new StringBuffer(name);
        
        if (file.isDirectory()) {
            this.mode = 040755;
            this.linkFlag = LF_DIR;
        
            if (this.name.charAt(this.name.length() - 1) != '/') {
                this.name.append("/");
            } 
        } else {
            this.mode = 0100644;
            this.linkFlag = LF_NORMAL;
        } 
        
        if (this.name.length() > NAMELEN) {
            throw new RuntimeException("file name '" + this.name 
                                             + "' is too long ( > " 
                                             + NAMELEN + " bytes)");
        
            // UNDONE When File lets us get the userName, use it!
        } 
        
        this.size = file.length();
        this.modTime = file.lastModified() / 1000;
        this.checkSum = 0;
        this.devMajor = 0;
        this.devMinor = 0;
    }   
        
    /** 
     * Construct an entry from an archive's header bytes. File is set
     * to null.
     *  
     * @param headerBuf The header bytes from a tar archive entry.
     */ 
    public TarEntry(byte[] headerBuf) {
        this();
        this.parseTarHeader(headerBuf);
    }   
        
    /** 
     * Determine if the two entries are equal. Equality is determined
     * by the header names being equal.
     *  
     * @return it Entry to be checked for equality.
     * @return True if the entries are equal.
     */ 
    public boolean equals(TarEntry it) {
        return this.getName().equals(it.getName());
    }   
        
    /** 
     * Determine if the given entry is a descendant of this entry.
     * Descendancy is determined by the name of the descendant
     * starting with this entry's name.
     *  
     * @param desc Entry to be checked as a descendent of this.
     * @return True if entry is a descendant of this.
     */ 
    public boolean isDescendent(TarEntry desc) {
        return desc.getName().startsWith(this.getName());
    }   
        
    /** 
     * Get this entry's name.
     *  
     * @return This entry's name.
     */ 
    public String getName() {
        return this.name.toString();
    }   
        
    /** 
     * Set this entry's name.
     *  
     * @param name This entry's new name.
     */ 
    public void setName(String name) {
        this.name = new StringBuffer(name);
    }   
        
    /** 
     * Get this entry's user id.
     *  
     * @return This entry's user id.
     */ 
    public int getUserId() {
        return this.userId;
    }   
        
    /** 
     * Set this entry's user id.
     *  
     * @param userId This entry's new user id.
     */ 
    public void setUserId(int userId) {
        this.userId = userId;
    }   
        
    /** 
     * Get this entry's group id.
     *  
     * @return This entry's group id.
     */ 
    public int getGroupId() {
        return this.groupId;
    }   
        
    /** 
     * Set this entry's group id.
     *  
     * @param groupId This entry's new group id.
     */ 
    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }   
        
    /** 
     * Get this entry's user name.
     *  
     * @return This entry's user name.
     */ 
    public String getUserName() {
        return this.userName.toString();
    }   
        
    /** 
     * Set this entry's user name.
     *  
     * @param userName This entry's new user name.
     */ 
    public void setUserName(String userName) {
        this.userName = new StringBuffer(userName);
    }   
        
    /** 
     * Get this entry's group name.
     *  
     * @return This entry's group name.
     */ 
    public String getGroupName() {
        return this.groupName.toString();
    }   
        
    /** 
     * Set this entry's group name.
     *  
     * @param groupName This entry's new group name.
     */ 
    public void setGroupName(String groupName) {
        this.groupName = new StringBuffer(groupName);
    }   
        
    /** 
     * Convenience method to set this entry's group and user ids.
     *  
     * @param userId This entry's new user id.
     * @param groupId This entry's new group id.
     */ 
    public void setIds(int userId, int groupId) {
        this.setUserId(userId);
        this.setGroupId(groupId);
    }   
        
    /** 
     * Convenience method to set this entry's group and user names.
     *  
     * @param userName This entry's new user name.
     * @param groupName This entry's new group name.
     */ 
    public void setNames(String userName, String groupName) {
        this.setUserName(userName);
        this.setGroupName(groupName);
    }   
        
    /** 
     * Set this entry's modification time. The parameter passed
     * to this method is in "Java time".
     *  
     * @param time This entry's new modification time.
     */ 
    public void setModTime(long time) {
        this.modTime = time / 1000;
    }   
        
    /** 
     * Set this entry's modification time.
     *  
     * @param time This entry's new modification time.
     */ 
    public void setModTime(Date time) {
        this.modTime = time.getTime() / 1000;
    }   
        
    /** 
     * Set this entry's modification time.
     *  
     * @param time This entry's new modification time.
     */ 
    public Date getModTime() {
        return new Date(this.modTime * 1000);
    }   
        
    /** 
     * Get this entry's file.
     *  
     * @return This entry's file.
     */ 
    public File getFile() {
        return this.file;
    }   
        
    /** 
     * Get this entry's file size.
     *  
     * @return This entry's file size.
     */ 
    public long getSize() {
        return this.size;
    }   
        
    /** 
     * Set this entry's file size.
     *  
     * @param size This entry's new file size.
     */ 
    public void setSize(long size) {
        this.size = size;
    }   
        
    /** 
     * Return whether or not this entry represents a directory.
     *  
     * @return True if this entry is a directory.
     */ 
    public boolean isDirectory() {
        if (this.file != null) {
            return this.file.isDirectory();
        } 
        
        if (this.linkFlag == LF_DIR) {
            return true;
        } 
        
        if (this.getName().endsWith("/")) {
            return true;
        } 
        
        return false;
    }   
        
    /** 
     * If this entry represents a file, and the file is a directory, return
     * an array of TarEntries for this entry's children.
     *  
     * @return An array of TarEntry's for this entry's children.
     */ 
    public TarEntry[] getDirectoryEntries() {
        if (this.file == null ||!this.file.isDirectory()) {
            return new TarEntry[0];
        } 
        
        String[]   list = this.file.list();
        TarEntry[] result = new TarEntry[list.length];
        
        for (int i = 0; i < list.length; ++i) {
            result[i] = new TarEntry(new File(this.file, list[i]));
        } 
        
        return result;
    }   
        
    /** 
     * Write an entry's header information to a header buffer.
     *  
     * @param outbuf The tar entry header buffer to fill in.
     */ 
    public void writeEntryHeader(byte[] outbuf) {
        int offset = 0;
        
        offset = TarUtils.getNameBytes(this.name, outbuf, offset, NAMELEN);
        offset = TarUtils.getOctalBytes(this.mode, outbuf, offset, MODELEN);
        offset = TarUtils.getOctalBytes(this.userId, outbuf, offset, UIDLEN);
        offset = TarUtils.getOctalBytes(this.groupId, outbuf, offset, GIDLEN);
        offset = TarUtils.getLongOctalBytes(this.size, outbuf, offset, SIZELEN);
        offset = TarUtils.getLongOctalBytes(this.modTime, outbuf, offset, MODTIMELEN);
        
        int csOffset = offset;
        
        for (int c = 0; c < CHKSUMLEN; ++c) {
            outbuf[offset++] = (byte) ' ';
        }
        
        outbuf[offset++] = this.linkFlag;
        offset = TarUtils.getNameBytes(this.linkName, outbuf, offset, NAMELEN);
        offset = TarUtils.getNameBytes(this.magic, outbuf, offset, MAGICLEN);
        offset = TarUtils.getNameBytes(this.userName, outbuf, offset, UNAMELEN);
        offset = TarUtils.getNameBytes(this.groupName, outbuf, offset, GNAMELEN);
        offset = TarUtils.getOctalBytes(this.devMajor, outbuf, offset, DEVLEN);
        offset = TarUtils.getOctalBytes(this.devMinor, outbuf, offset, DEVLEN);
        
        while (offset < outbuf.length) {
            outbuf[offset++] = 0;
        }
        
        long checkSum = TarUtils.computeCheckSum(outbuf);
        
        TarUtils.getCheckSumOctalBytes(checkSum, outbuf, csOffset, CHKSUMLEN);
    }   
        
    /** 
     * Parse an entry's header information from a header buffer.
     *  
     * @param header The tar entry header buffer to get information from.
     */ 
    public void parseTarHeader(byte[] header) {
        int offset = 0;
        
        this.name = TarUtils.parseName(header, offset, NAMELEN);  
        offset += NAMELEN;
        this.mode = (int) TarUtils.parseOctal(header, offset, MODELEN); 
        offset += MODELEN;
        this.userId = (int) TarUtils.parseOctal(header, offset, UIDLEN);
        offset += UIDLEN;
        this.groupId = (int) TarUtils.parseOctal(header, offset, GIDLEN);
        offset += GIDLEN;
        this.size = TarUtils.parseOctal(header, offset, SIZELEN);
        offset += SIZELEN;
        this.modTime = TarUtils.parseOctal(header, offset, MODTIMELEN);
        offset += MODTIMELEN;
        this.checkSum = (int) TarUtils.parseOctal(header, offset, CHKSUMLEN);
        offset += CHKSUMLEN;
        this.linkFlag = header[offset++];
        this.linkName = TarUtils.parseName(header, offset, NAMELEN);
        offset += NAMELEN;
        this.magic = TarUtils.parseName(header, offset, MAGICLEN);
        offset += MAGICLEN;
        this.userName = TarUtils.parseName(header, offset, UNAMELEN);
        offset += UNAMELEN;
        this.groupName = TarUtils.parseName(header, offset, GNAMELEN);
        offset += GNAMELEN;
        this.devMajor = (int) TarUtils.parseOctal(header, offset, DEVLEN);
        offset += DEVLEN;
        this.devMinor = (int) TarUtils.parseOctal(header, offset, DEVLEN);
    }        
}       
