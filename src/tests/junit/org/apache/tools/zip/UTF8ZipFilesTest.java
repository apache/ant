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

package org.apache.tools.zip;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Enumeration;
import java.util.zip.CRC32;
import junit.framework.TestCase;

public class UTF8ZipFilesTest extends TestCase {

    private static final String UTF_8 = "utf-8";
    private static final String CP437 = "cp437";
    private static final String US_ASCII = "US-ASCII";
    private static final String ASCII_TXT = "ascii.txt";
    private static final String EURO_FOR_DOLLAR_TXT = "\u20AC_for_Dollar.txt";
    private static final String OIL_BARREL_TXT = "\u00D6lf\u00E4sser.txt";

    public void testUtf8FileRoundtripExplicitUnicodeExtra()
        throws IOException {
        testFileRoundtrip(UTF_8, true, true);
    }

    public void testUtf8FileRoundtripNoEFSExplicitUnicodeExtra()
        throws IOException {
        testFileRoundtrip(UTF_8, false, true);
    }

    public void testCP437FileRoundtripExplicitUnicodeExtra()
        throws IOException {
        testFileRoundtrip(CP437, false, true);
    }

    public void testASCIIFileRoundtripExplicitUnicodeExtra()
        throws IOException {
        testFileRoundtrip(US_ASCII, false, true);
    }

    public void testUtf8FileRoundtripImplicitUnicodeExtra()
        throws IOException {
        testFileRoundtrip(UTF_8, true, false);
    }

    public void testUtf8FileRoundtripNoEFSImplicitUnicodeExtra()
        throws IOException {
        testFileRoundtrip(UTF_8, false, false);
    }

    public void testCP437FileRoundtripImplicitUnicodeExtra()
        throws IOException {
        testFileRoundtrip(CP437, false, false);
    }

    public void testASCIIFileRoundtripImplicitUnicodeExtra()
        throws IOException {
        testFileRoundtrip(US_ASCII, false, false);
    }

    public void testZipFileReadsUnicodeFields() throws IOException {
        File file = File.createTempFile("unicode-test", ".zip");
        ZipFile zf = null;
        try {
            createTestFile(file, US_ASCII, false, true);
            zf = new ZipFile(file, US_ASCII, true);
            assertNotNull(zf.getEntry(ASCII_TXT));
            assertNotNull(zf.getEntry(EURO_FOR_DOLLAR_TXT));
            assertNotNull(zf.getEntry(OIL_BARREL_TXT));
        } finally {
            ZipFile.closeQuietly(zf);
            if (file.exists()) {
                file.delete();
            }
        }
    }

    private static void testFileRoundtrip(String encoding, boolean withEFS,
                                          boolean withExplicitUnicodeExtra)
        throws IOException {

        File file = File.createTempFile(encoding + "-test", ".zip");
        try {
            createTestFile(file, encoding, withEFS, withExplicitUnicodeExtra);
            testFile(file, encoding);
        } finally {
            if (file.exists()) {
                file.delete();
            }
        }
    }

    private static void createTestFile(File file, String encoding,
                                       boolean withEFS,
                                       boolean withExplicitUnicodeExtra)
        throws UnsupportedEncodingException, IOException {

        ZipEncoding zipEncoding = ZipEncodingHelper.getZipEncoding(encoding);

        ZipOutputStream zos = null;
        try {
            zos = new ZipOutputStream(file);
            zos.setEncoding(encoding);
            zos.setUseLanguageEncodingFlag(withEFS);
            zos.setCreateUnicodeExtraFields(withExplicitUnicodeExtra ? 
                                            ZipOutputStream
                                            .UnicodeExtraFieldPolicy.NEVER
                                            : ZipOutputStream
                                            .UnicodeExtraFieldPolicy.ALWAYS);

            ZipEntry ze = new ZipEntry(OIL_BARREL_TXT);
            if (withExplicitUnicodeExtra
                && !zipEncoding.canEncode(ze.getName())) {

                ByteBuffer en = zipEncoding.encode(ze.getName());

                ze.addExtraField(new UnicodePathExtraField(ze.getName(),
                                                           en.array(),
                                                           en.arrayOffset(),
                                                           en.limit()));
            }

            zos.putNextEntry(ze);
            zos.write("Hello, world!".getBytes("US-ASCII"));
            zos.closeEntry();

            ze = new ZipEntry(EURO_FOR_DOLLAR_TXT);
            if (withExplicitUnicodeExtra
                && !zipEncoding.canEncode(ze.getName())) {

                ByteBuffer en = zipEncoding.encode(ze.getName());

                ze.addExtraField(new UnicodePathExtraField(ze.getName(),
                                                           en.array(),
                                                           en.arrayOffset(),
                                                           en.limit()));
            }

            zos.putNextEntry(ze);
            zos.write("Give me your money!".getBytes("US-ASCII"));
            zos.closeEntry();

            ze = new ZipEntry(ASCII_TXT);

            if (withExplicitUnicodeExtra
                && !zipEncoding.canEncode(ze.getName())) {

                ByteBuffer en = zipEncoding.encode(ze.getName());

                ze.addExtraField(new UnicodePathExtraField(ze.getName(),
                                                           en.array(),
                                                           en.arrayOffset(),
                                                           en.limit()));
            }

            zos.putNextEntry(ze);
            zos.write("ascii".getBytes("US-ASCII"));
            zos.closeEntry();
        } finally {
            if (zos != null) {
                try {
                    zos.close();
                } catch (IOException e) { /* swallow */ }
            }
        }
    }

    private static void testFile(File file, String encoding)
        throws IOException {
        ZipFile zf = null;
        try {
            zf = new ZipFile(file, encoding, false);

            Enumeration e = zf.getEntries();
            while (e.hasMoreElements()) {
                ZipEntry ze = (ZipEntry) e.nextElement();

                if (ze.getName().endsWith("sser.txt")) {
                    assertUnicodeName(ze, OIL_BARREL_TXT, encoding);

                } else if (ze.getName().endsWith("_for_Dollar.txt")) {
                    assertUnicodeName(ze, EURO_FOR_DOLLAR_TXT, encoding);
                } else if (!ze.getName().equals(ASCII_TXT)) {
                    throw new AssertionError("Urecognized ZIP entry with name ["
                                             + ze.getName() + "] found.");
                }
            }
        } finally {
            ZipFile.closeQuietly(zf);
        }
    }

    private static UnicodePathExtraField findUniCodePath(ZipEntry ze) {
        return (UnicodePathExtraField)
            ze.getExtraField(UnicodePathExtraField.UPATH_ID);
    }

    private static void assertUnicodeName(ZipEntry ze,
                                          String expectedName,
                                          String encoding)
        throws IOException {
        if (!expectedName.equals(ze.getName())) {
            UnicodePathExtraField ucpf = findUniCodePath(ze);
            assertNotNull(ucpf);

            ZipEncoding enc = ZipEncodingHelper.getZipEncoding(encoding);
            ByteBuffer ne = enc.encode(ze.getName());

            CRC32 crc = new CRC32();
            crc.update(ne.array(),ne.arrayOffset(),ne.limit());

            assertEquals(crc.getValue(), ucpf.getNameCRC32());
            assertEquals(expectedName, new String(ucpf.getUnicodeName(),
                                                  UTF_8));
        }
    }

}

