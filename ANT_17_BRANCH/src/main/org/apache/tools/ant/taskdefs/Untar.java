/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
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

package org.apache.tools.ant.taskdefs;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.util.FileNameMapper;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.bzip2.CBZip2InputStream;
import org.apache.tools.tar.TarEntry;
import org.apache.tools.tar.TarInputStream;



/**
 * Untar a file.
 * <p>For JDK 1.1 &quot;last modified time&quot; field is set to current time instead of being
 * carried from the archive file.</p>
 * <p>PatternSets are used to select files to extract
 * <I>from</I> the archive.  If no patternset is used, all files are extracted.
 * </p>
 * <p>FileSet>s may be used to select archived files
 * to perform unarchival upon.
 * </p>
 * <p>File permissions will not be restored on extracted files.</p>
 * <p>The untar task recognizes the long pathname entries used by GNU tar.<p>
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

    /**
     * No encoding support in Untar.
     * @param encoding not used
     * @throws BuildException always
     * @since Ant 1.6
     */
    public void setEncoding(String encoding) {
        throw new BuildException("The " + getTaskName()
                                 + " task doesn't support the encoding"
                                 + " attribute", getLocation());
    }

    /**
     * @see Expand#expandFile(FileUtils, File, File)
     */
    /** {@inheritDoc} */
    protected void expandFile(FileUtils fileUtils, File srcF, File dir) {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(srcF);
            expandStream(srcF.getPath(), fis, dir);
        } catch (IOException ioe) {
            throw new BuildException("Error while expanding " + srcF.getPath(),
                                     ioe, getLocation());
        } finally {
            FileUtils.close(fis);
        }
    }

    /**
     * This method is to be overridden by extending unarchival tasks.
     *
     * @param srcR      the source resource
     * @param dir       the destination directory
     * @since Ant 1.7
     */
    protected void expandResource(Resource srcR, File dir) {
        InputStream i = null;
        try {
            i = srcR.getInputStream();
            expandStream(srcR.getName(), i, dir);
        } catch (IOException ioe) {
            throw new BuildException("Error while expanding " + srcR.getName(),
                                     ioe, getLocation());
        } finally {
            FileUtils.close(i);
        }
    }

    /**
     * @since Ant 1.7
     */
    private void expandStream(String name, InputStream stream, File dir)
        throws IOException {
        TarInputStream tis = null;
        try {
            tis =
                new TarInputStream(compression.decompress(name,
                                                          new BufferedInputStream(stream)));
            log("Expanding: " + name + " into " + dir, Project.MSG_INFO);
            TarEntry te = null;
            FileNameMapper mapper = getMapper();
            while ((te = tis.getNextEntry()) != null) {
                extractFile(FileUtils.getFileUtils(), null, dir, tis,
                            te.getName(), te.getModTime(),
                            te.isDirectory(), mapper);
            }
            log("expand complete", Project.MSG_VERBOSE);
        } finally {
            FileUtils.close(tis);
        }
    }

    /**
     * Valid Modes for Compression attribute to Untar Task
     *
     */
    public static final class UntarCompressionMethod
        extends EnumeratedAttribute {

        // permissible values for compression attribute
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
            return new String[] {NONE, GZIP, BZIP2};
        }

        /**
         *  This method wraps the input stream with the
         *     corresponding decompression method
         *
         *  @param name provides location information for BuildException
         *  @param istream input stream
         *  @return input stream with on-the-fly decompression
         *  @exception IOException thrown by GZIPInputStream constructor
         *  @exception BuildException thrown if bzip stream does not
         *     start with expected magic values
         */
        public InputStream decompress(final String name,
                                       final InputStream istream)
            throws IOException, BuildException {
            final String v = getValue();
            if (GZIP.equals(v)) {
                return new GZIPInputStream(istream);
            } else {
                if (BZIP2.equals(v)) {
                    final char[] magic = new char[] {'B', 'Z'};
                    for (int i = 0; i < magic.length; i++) {
                        if (istream.read() != magic[i]) {
                            throw new BuildException(
                                                     "Invalid bz2 file." + name);
                        }
                    }
                    return new CBZip2InputStream(istream);
                }
            }
            return istream;
        }
    }
}
