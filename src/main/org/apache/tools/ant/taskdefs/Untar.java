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

package org.apache.tools.ant.taskdefs;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
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
 * <p>PatternSets are used to select files to extract
 * <I>from</I> the archive.  If no patternset is used, all files are extracted.
 * </p>
 * <p>FileSets may be used to select archived files
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

    public Untar() {
        super(null);
    }

    /**
     * Set decompression algorithm to use; default=none.
     *
     * Allowable values are
     * <ul>
     *   <li>none - no compression
     *   <li>gzip - Gzip compression
     *   <li>bzip2 - Bzip2 compression
     *   <li>xz - XZ compression, requires XZ for Java
     * </ul>
     *
     * @param method compression method
     */
    public void setCompression(UntarCompressionMethod method) {
        compression = method;
    }

    /**
     * No unicode extra fields in tar.
     *
     * @since Ant 1.8.0
     */
    @Override
    public void setScanForUnicodeExtraFields(boolean b) {
        throw new BuildException(
            "The " + getTaskName()
                + " task doesn't support the encoding attribute",
            getLocation());
    }

    /**
     * @see Expand#expandFile(FileUtils, File, File)
     * {@inheritDoc}
     */
    @Override
    protected void expandFile(FileUtils fileUtils, File srcF, File dir) {
        if (!srcF.exists()) {
            throw new BuildException("Unable to untar "
                    + srcF
                    + " as the file does not exist",
                    getLocation());
        }
        try (InputStream fis = Files.newInputStream(srcF.toPath())) {
            expandStream(srcF.getPath(), fis, dir);
        } catch (IOException ioe) {
            throw new BuildException("Error while expanding " + srcF.getPath()
                                     + "\n" + ioe.toString(),
                                     ioe, getLocation());
        }
    }

    /**
     * This method is to be overridden by extending unarchival tasks.
     *
     * @param srcR      the source resource
     * @param dir       the destination directory
     * @since Ant 1.7
     */
    @Override
    protected void expandResource(Resource srcR, File dir) {
        if (!srcR.isExists()) {
            throw new BuildException("Unable to untar "
                                     + srcR.getName()
                                     + " as the it does not exist",
                                     getLocation());
        }

        try (InputStream i = srcR.getInputStream()) {
            expandStream(srcR.getName(), i, dir);
        } catch (IOException ioe) {
            throw new BuildException("Error while expanding " + srcR.getName(),
                                     ioe, getLocation());
        }
    }

    /**
     * @since Ant 1.7
     */
    private void expandStream(String name, InputStream stream, File dir)
        throws IOException {
        try (TarInputStream tis = new TarInputStream(
            compression.decompress(name, new BufferedInputStream(stream)),
            getEncoding())) {
            log("Expanding: " + name + " into " + dir, Project.MSG_INFO);
            boolean empty = true;
            FileNameMapper mapper = getMapper();
            TarEntry te;
            while ((te = tis.getNextEntry()) != null) {
                empty = false;
                extractFile(FileUtils.getFileUtils(), null, dir, tis,
                            te.getName(), te.getModTime(),
                            te.isDirectory(), mapper);
            }
            if (empty && getFailOnEmptyArchive()) {
                throw new BuildException("archive '%s' is empty", name);
            }
            log("expand complete", Project.MSG_VERBOSE);
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
         *  XZ compression
         * @since 1.10.1
         */
        private static final String XZ = "xz";


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
        @Override
        public String[] getValues() {
            return new String[] {NONE, GZIP, BZIP2, XZ};
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
        public InputStream decompress(final String name, final InputStream istream)
                throws IOException, BuildException {
            final String v = getValue();
            if (GZIP.equals(v)) {
                return new GZIPInputStream(istream);
            }
            if (XZ.equals(v)) {
                return newXZInputStream(istream);
            }
            if (BZIP2.equals(v)) {
                final char[] magic = new char[] { 'B', 'Z' };
                for (char c : magic) {
                    if (istream.read() != c) {
                        throw new BuildException("Invalid bz2 file." + name);
                    }
                }
                return new CBZip2InputStream(istream);
            }
            return istream;
        }

        private static InputStream newXZInputStream(InputStream istream)
            throws BuildException {
            try {
                Class<? extends InputStream> clazz =
                    Class.forName("org.tukaani.xz.XZInputStream")
                    .asSubclass(InputStream.class);
                Constructor<? extends InputStream> c =
                    clazz.getConstructor(InputStream.class);
                return c.newInstance(istream);
            } catch (ClassNotFoundException ex) {
                throw new BuildException("xz decompression requires the XZ for Java library",
                                         ex);
            } catch (NoSuchMethodException
                     | InstantiationException
                     | IllegalAccessException
                     | InvocationTargetException
                     ex) {
                throw new BuildException("failed to create XZInputStream", ex);
            }
        }
    }
}
