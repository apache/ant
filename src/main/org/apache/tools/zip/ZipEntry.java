/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2003 The Apache Software Foundation.  All rights
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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Vector;
import java.util.zip.ZipException;

/**
 * Extension that adds better handling of extra fields and provides
 * access to the internal and external file attributes.
 *
 * @author Stefan Bodewig
 * @version $Revision$
 */
public class ZipEntry extends java.util.zip.ZipEntry {

    private static final int PLATFORM_UNIX = 3;
    private static final int PLATFORM_FAT  = 0;

    private int internalAttributes = 0;
    private int platform = PLATFORM_FAT;
    private long externalAttributes = 0;
    private Vector extraFields = new Vector();

    /**
     * Creates a new zip entry with the specified name.
     *
     * @since 1.1
     */
    public ZipEntry(String name) {
        super(name);
    }

    /**
     * Creates a new zip entry with fields taken from the specified zip entry.
     *
     * @since 1.1
     */
    public ZipEntry(java.util.zip.ZipEntry entry) throws ZipException {
        /*
         * REVISIT: call super(entry) instead of this stuff in Ant2,
         *          "copy constructor" has not been available in JDK 1.1
         */
        super(entry.getName());

        setComment(entry.getComment());
        setMethod(entry.getMethod());
        setTime(entry.getTime());

        long size = entry.getSize();
        if (size > 0) {
            setSize(size);
        }
        long cSize = entry.getCompressedSize();
        if (cSize > 0) {
            setComprSize(cSize);
        }
        long crc = entry.getCrc();
        if (crc > 0) {
            setCrc(crc);
        }
        
        byte[] extra = entry.getExtra();
        if (extra != null) {
            setExtraFields(ExtraFieldUtils.parse(extra));
        } else {
            // initializes extra data to an empty byte array
            setExtra();
        }
    }

    /**
     * Creates a new zip entry with fields taken from the specified zip entry.
     *
     * @since 1.1
     */
    public ZipEntry(ZipEntry entry) throws ZipException {
        this((java.util.zip.ZipEntry) entry);
        setInternalAttributes(entry.getInternalAttributes());
        setExternalAttributes(entry.getExternalAttributes());
        setExtraFields(entry.getExtraFields());
    }

    /**
     * Overwrite clone
     *
     * @since 1.1
     */
    public Object clone() {
        ZipEntry e = null;
        try {
            e = new ZipEntry((java.util.zip.ZipEntry) super.clone());
        } catch (Exception ex) {
            // impossible as extra data is in correct format
            ex.printStackTrace();
        }
        e.setInternalAttributes(getInternalAttributes());
        e.setExternalAttributes(getExternalAttributes());
        e.setExtraFields(getExtraFields());
        return e;
    }

    /**
     * Retrieves the internal file attributes.
     *
     * @since 1.1
     */
    public int getInternalAttributes() {
        return internalAttributes;
    }

    /**
     * Sets the internal file attributes.
     *
     * @since 1.1
     */
    public void setInternalAttributes(int value) {
        internalAttributes = value;
    }

    /**
     * Retrieves the external file attributes.
     *
     * @since 1.1
     */
    public long getExternalAttributes() {
        return externalAttributes;
    }

    /**
     * Sets the external file attributes.
     *
     * @since 1.1
     */
    public void setExternalAttributes(long value) {
        externalAttributes = value;
    }

    /**
     * Sets Unix permissions in a way that is understood by Info-Zip's
     * unzip command.
     *
     * @since Ant 1.5.2
     */
    public void setUnixMode(int mode) {
        setExternalAttributes((mode << 16)
                              // MS-DOS read-only attribute
                              | ((mode & 0200) == 0 ? 1 : 0)
                              // MS-DOS directory flag
                              | (isDirectory() ? 0x10 : 0));
        platform = PLATFORM_UNIX;
    }

    /**
     * Platform specification to put into the &quot;version made
     * by&quot; part of the central file header.
     *
     * @return 0 (MS-DOS FAT) unless {@link #setUnixMode setUnixMode}
     * has been called, in which case 3 (Unix) will be returned.
     *
     * @since Ant 1.5.2
     */
    public int getPlatform() {
        return platform;
    }

    /**
     * Replaces all currently attached extra fields with the new array.
     *
     * @since 1.1
     */
    public void setExtraFields(ZipExtraField[] fields) {
        extraFields.removeAllElements();
        for (int i = 0; i < fields.length; i++) {
            extraFields.addElement(fields[i]);
        }
        setExtra();
    }

    /**
     * Retrieves extra fields.
     *
     * @since 1.1
     */
    public ZipExtraField[] getExtraFields() {
        ZipExtraField[] result = new ZipExtraField[extraFields.size()];
        extraFields.copyInto(result);
        return result;
    }

    /**
     * Adds an extra fields - replacing an already present extra field
     * of the same type.
     *
     * @since 1.1
     */
    public void addExtraField(ZipExtraField ze) {
        ZipShort type = ze.getHeaderId();
        boolean done = false;
        for (int i = 0; !done && i < extraFields.size(); i++) {
            if (((ZipExtraField) extraFields.elementAt(i)).getHeaderId().equals(type)) {
                extraFields.setElementAt(ze, i);
                done = true;
            }
        }
        if (!done) {
            extraFields.addElement(ze);
        }
        setExtra();
    }

    /**
     * Remove an extra fields.
     *
     * @since 1.1
     */
    public void removeExtraField(ZipShort type) {
        boolean done = false;
        for (int i = 0; !done && i < extraFields.size(); i++) {
            if (((ZipExtraField) extraFields.elementAt(i)).getHeaderId().equals(type)) {
                extraFields.removeElementAt(i);
                done = true;
            }
        }
        if (!done) {
            throw new java.util.NoSuchElementException();
        }
        setExtra();
    }

    /**
     * Throws an Exception if extra data cannot be parsed into extra fields.
     *
     * @since 1.1
     */
    public void setExtra(byte[] extra) throws RuntimeException {
        try {
            setExtraFields(ExtraFieldUtils.parse(extra));
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * Unfortunately {@link java.util.zip.ZipOutputStream
     * java.util.zip.ZipOutputStream} seems to access the extra data
     * directly, so overriding getExtra doesn't help - we need to
     * modify super's data directly.
     *
     * @since 1.1
     */
    protected void setExtra() {
        super.setExtra(ExtraFieldUtils.mergeLocalFileDataData(getExtraFields()));
    }

    /**
     * Retrieves the extra data for the local file data.
     *
     * @since 1.1
     */
    public byte[] getLocalFileDataExtra() {
        byte[] extra = getExtra();
        return extra != null ? extra : new byte[0];
    }

    /**
     * Retrieves the extra data for the central directory.
     *
     * @since 1.1
     */
    public byte[] getCentralDirectoryExtra() {
        return ExtraFieldUtils.mergeCentralDirectoryData(getExtraFields());
    }

    /**
     * Helper for JDK 1.1 <-> 1.2 incompatibility.
     *
     * @since 1.2
     */
    private Long compressedSize = null;

    /**
     * Make this class work in JDK 1.1 like a 1.2 class.
     *
     * <p>This either stores the size for later usage or invokes
     * setCompressedSize via reflection.</p>
     *
     * @since 1.2
     */
    public void setComprSize(long size) {
        if (haveSetCompressedSize()) {
            performSetCompressedSize(this, size);
        } else {
            compressedSize = new Long(size);
        }
    }

    /**
     * Override to make this class work in JDK 1.1 like a 1.2 class.
     *
     * @since 1.2
     */
    public long getCompressedSize() {
        if (compressedSize != null) {
            // has been set explicitly and we are running in a 1.1 VM
            return compressedSize.longValue();
        }
        return super.getCompressedSize();
    }

    /**
     * Helper for JDK 1.1
     *
     * @since 1.2
     */
    private static Method setCompressedSizeMethod = null;
    /**
     * Helper for JDK 1.1
     *
     * @since 1.2
     */
    private static Object lockReflection = new Object();
    /**
     * Helper for JDK 1.1
     *
     * @since 1.2
     */
    private static boolean triedToGetMethod = false;

    /**
     * Are we running JDK 1.2 or higher?
     *
     * @since 1.2
     */
    private static boolean haveSetCompressedSize() {
        checkSCS();
        return setCompressedSizeMethod != null;
    }

    /**
     * Invoke setCompressedSize via reflection.
     *
     * @since 1.2
     */
    private static void performSetCompressedSize(ZipEntry ze, long size) {
        Long[] s = {new Long(size)};
        try {
            setCompressedSizeMethod.invoke(ze, s);
        } catch (InvocationTargetException ite) {
            Throwable nested = ite.getTargetException();
            throw new RuntimeException("Exception setting the compressed size "
                                       + "of " + ze + ": "
                                       + nested.getMessage());
        } catch (Throwable other) {
            throw new RuntimeException("Exception setting the compressed size "
                                       + "of " + ze + ": "
                                       + other.getMessage());
        }
    }

    /**
     * Try to get a handle to the setCompressedSize method.
     *
     * @since 1.2
     */
    private static void checkSCS() {
        if (!triedToGetMethod) {
            synchronized (lockReflection) {
                triedToGetMethod = true;
                try {
                    setCompressedSizeMethod = 
                        java.util.zip.ZipEntry.class.getMethod("setCompressedSize", 
                                                               new Class[] {Long.TYPE});
                } catch (NoSuchMethodException nse) {
                }
            }
        }
    }

}
