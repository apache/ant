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

package org.apache.tools.ant.taskdefs;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.tar.TarInputStream;
import org.apache.tools.tar.TarEntry;
import org.apache.tools.ant.util.FileUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import org.apache.tools.bzip2.CBZip2InputStream;
import org.apache.tools.ant.types.EnumeratedAttribute;



/**
 * Untar a file.
 * <p>For JDK 1.1 &quot;last modified time&quot; field is set to current time instead of being
 * carried from the archive file.</p>
 * <p>PatternSets are used to select files to extract
 * <I>from</I> the archive.  If no patternset is used, all files are extracted.
 * </p>
 * <p>FileSet>s may be used used to select archived files
 * to perform unarchival upon.
 * </p>
 * <p>File permissions will not be restored on extracted files.</p>
 * <p>The untar task recognizes the long pathname entries used by GNU tar.<p>
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 * @author Magesh Umasankar
 *
 * @since Ant 1.1
 *
 * @ant.task category="packaging"
 */
public class Untar extends Expand {
    /**
     *   compression method
     */
    private UntarCompressionMethod compression = new UntarCompressionMethod();

    /**
     * Set decompression algorithm to use; default=none.
     *
     * Allowable values are
     * <ul>
     *   <li>none - no compression
     *   <li>gzip - Gzip compression
     *   <li>bzip2 - Bzip2 compression
     * </ul>
     *
     * @param method compression method
     */
    public void setCompression(UntarCompressionMethod method) {
        compression = method;
    }

    protected void expandFile(FileUtils fileUtils, File srcF, File dir) {
        TarInputStream tis = null;
        try {
            log("Expanding: " + srcF + " into " + dir, Project.MSG_INFO);
            tis = new TarInputStream(
                compression.decompress(srcF,
                    new BufferedInputStream(
                        new FileInputStream(srcF))));
            TarEntry te = null;

            while ((te = tis.getNextEntry()) != null) {
                extractFile(fileUtils, srcF, dir, tis,
                            te.getName(), te.getModTime(), te.isDirectory());
            }
            log("expand complete", Project.MSG_VERBOSE);

        } catch (IOException ioe) {
            throw new BuildException("Error while expanding " + srcF.getPath(),
                                     ioe, location);
        } finally {
            if (tis != null) {
                try {
                    tis.close();
                } catch (IOException e) {}
            }
        }
    }

    /**
     * Valid Modes for Compression attribute to Untar Task
     *
     */
    public static final class UntarCompressionMethod
        extends EnumeratedAttribute {

        // permissable values for compression attribute
        /**
         *  No compression
         */
        private static final String NONE = "none";
        /**
         *  GZIP compression
         */
        private static final String GZIP = "gzip";
        /**
         *  BZIP2 compression
         */
        private static final String BZIP2 = "bzip2";


        /**
         *  Constructor
         */
        public UntarCompressionMethod() {
            super();
            setValue(NONE);
        }

        /**
         * Get valid enumeration values
         *
         * @return valid values
         */
        public String[] getValues() {
            return new String[] { NONE, GZIP, BZIP2 };
        }

        /**
         *  This method wraps the input stream with the
         *     corresponding decompression method
         *
         *  @param file provides location information for BuildException
         *  @param istream input stream
         *  @return input stream with on-the-fly decompression
         *  @exception IOException thrown by GZIPInputStream constructor
         *  @exception BuildException thrown if bzip stream does not
         *     start with expected magic values
         */
        private InputStream decompress(final File file,
                                       final InputStream istream)
            throws IOException, BuildException {
            final String value = getValue();
            if (GZIP.equals(value)) {
                return new GZIPInputStream(istream);
            } else {
                if (BZIP2.equals(value)) {
                    final char[] magic = new char[] { 'B', 'Z' };
                    for (int i = 0; i < magic.length; i++) {
                        if (istream.read() != magic[i]) {
                            throw new BuildException(
                                "Invalid bz2 file." + file.toString());
                        }
                    }
                    return new CBZip2InputStream(istream);
                }
            }
            return istream;
        }
    }
}
