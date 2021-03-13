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

package org.apache.tools.zip;

import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipInputStream;

import static org.junit.Assert.assertEquals;

public class ZipOutputStreamTest {

    private Date time;
    private ZipLong zl;

    @Before
    public void setUp() {
        time = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(time);
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1;
        long value =  ((year - 1980) << 25)
            |         (month << 21)
            |         (cal.get(Calendar.DAY_OF_MONTH) << 16)
            |         (cal.get(Calendar.HOUR_OF_DAY) << 11)
            |         (cal.get(Calendar.MINUTE) << 5)
            |         (cal.get(Calendar.SECOND) >> 1);

        byte[] result = new byte[4];
        result[0] = (byte) ((value & 0xFF));
        result[1] = (byte) ((value & 0xFF00) >> 8);
        result[2] = (byte) ((value & 0xFF0000) >> 16);
        result[3] = (byte) ((value & 0xFF000000L) >> 24);
        zl = new ZipLong(result);
    }


    @Test
    public void testZipLong() {
        ZipLong test = ZipUtil.toDosTime(time);
        assertEquals(test.getValue(), zl.getValue());
    }

    @Test
    public void testAdjustToLong() {
        assertEquals((long) Integer.MAX_VALUE,
                     ZipUtil.adjustToLong(Integer.MAX_VALUE));
        assertEquals(((long) Integer.MAX_VALUE) + 1,
                     ZipUtil.adjustToLong(Integer.MAX_VALUE + 1));
        assertEquals(2 * ((long) Integer.MAX_VALUE),
                     ZipUtil.adjustToLong(2 * Integer.MAX_VALUE));
    }

    @Test
    public void testWriteAndReadManifest() throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            try (ZipOutputStream zos = new ZipOutputStream(baos)) {
                zos.putNextEntry(new ZipEntry(JarFile.MANIFEST_NAME));
                new Manifest().write(zos);
            }
            try (ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
                 ZipInputStream zis = new ZipInputStream(bais)) {
                zis.getNextEntry();
                zis.closeEntry();
            }
        }
    }
}
