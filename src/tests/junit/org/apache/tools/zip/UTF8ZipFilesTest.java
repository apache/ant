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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Enumeration;
import junit.framework.TestCase;

public class UTF8ZipFilesTest extends TestCase {

    private static final String UTF_8 = "utf-8";
    private static final String CP437 = "cp437";
    private static final String US_ASCII = "US-ASCII";
    private static final String ASCII_TXT = "ascii.txt";
    private static final String EURO_FOR_DOLLAR_TXT = "\u20AC_for_Dollar.txt";
    private static final String OIL_BARREL_TXT = "\u00D6lf\u00E4sser.txt";

    public void testUtf8FileRoundtrip() throws IOException {
        testFileRoundtrip(UTF_8);
    }


    public void testCP437FileRoundtrip() throws IOException {
        testFileRoundtrip(CP437);
    }

    public void testASCIIFileRoundtrip() throws IOException {
        testFileRoundtrip(US_ASCII);
    }

    private static void testFileRoundtrip(String encoding)
        throws IOException {

        try {
            Charset.forName(encoding);
        } catch (UnsupportedCharsetException use) {
            System.err.println("Skipping testFileRoundtrip for unsupported "
                               + " encoding " + encoding);
            return;
        }

        File file = File.createTempFile(encoding + "-test", ".zip");
        try {
            createTestFile(file, encoding);
            testFile(file, encoding);
        } finally {
            if (file.exists()) {
                file.delete();
            }
        }
    }

    private static void createTestFile(File file, String encoding)
        throws UnsupportedEncodingException, IOException {

        ZipOutputStream zos = null;
        try {
            zos = new ZipOutputStream(file);
            zos.setEncoding(encoding);

            ZipEntry ze = new ZipEntry(OIL_BARREL_TXT);
            if (!ZipEncodingHelper.canEncodeName(ze.getName(),
                                                 zos.getEncoding())) {
                ze.addExtraField(new UnicodePathExtraField(ze.getName(),
                                                           zos.getEncoding()));
            }

            zos.putNextEntry(ze);
            zos.write("Hello, world!".getBytes("US-ASCII"));
            zos.closeEntry();

            ze = new ZipEntry(EURO_FOR_DOLLAR_TXT);
            if (!ZipEncodingHelper.canEncodeName(ze.getName(),
                                                 zos.getEncoding())) {
                ze.addExtraField(new UnicodePathExtraField(ze.getName(),
                                                           zos.getEncoding()));
            }

            zos.putNextEntry(ze);
            zos.write("Give me your money!".getBytes("US-ASCII"));
            zos.closeEntry();

            ze = new ZipEntry(ASCII_TXT);

            if (!ZipEncodingHelper.canEncodeName(ze.getName(),
                                                 zos.getEncoding())) {
                ze.addExtraField(new UnicodePathExtraField(ze.getName(),
                                                           zos.getEncoding()));
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
            zf = new ZipFile(file, encoding);

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

        ZipExtraField[] efs = ze.getExtraFields();
        for (int i = 0; i < efs.length; ++i) {
            if (efs[i].getHeaderId().equals(UnicodePathExtraField.UPATH_ID)) {
                return (UnicodePathExtraField) efs[i];
            }
        }
        return null;
    }

    private static void assertUnicodeName(ZipEntry ze,
                                          String expectedName,
                                          String encoding)
        throws IOException {
        if (!expectedName.equals(ze.getName())) {
            UnicodePathExtraField ucpf = findUniCodePath(ze);
            assertNotNull(ucpf);

            UnicodePathExtraField ucpe = new UnicodePathExtraField(expectedName,
                                                                   encoding);
            assertEquals(ucpe.getNameCRC32(), ucpf.getNameCRC32());
            assertEquals(expectedName, new String(ucpf.getUnicodeName(),
                                                  UTF_8));
        }
    }

    /*
    public void testUtf8Interoperability() throws IOException {
        File file1 = super.getFile("utf8-7zip-test.zip");
        File file2 = super.getFile("utf8-winzip-test.zip");

        testFile(file1,CP437);
        testFile(file2,CP437);

    }
    */
}

