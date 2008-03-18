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

import junit.framework.TestCase;

/**
 * JUnit 3 testcases for org.apache.tools.zip.ExtraFieldUtils.
 *
 */
public class ExtraFieldUtilsTest extends TestCase implements UnixStat {
    public ExtraFieldUtilsTest(String name) {
        super(name);
    }

    private AsiExtraField a;
    private UnrecognizedExtraField dummy;
    private byte[] data;
    private byte[] aLocal;

    public void setUp() {
        a = new AsiExtraField();
        a.setMode(0755);
        a.setDirectory(true);
        dummy = new UnrecognizedExtraField();
        dummy.setHeaderId(new ZipShort(1));
        dummy.setLocalFileDataData(new byte[0]);
        dummy.setCentralDirectoryData(new byte[] {0});

        aLocal = a.getLocalFileDataData();
        byte[] dummyLocal = dummy.getLocalFileDataData();
        data = new byte[4 + aLocal.length + 4 + dummyLocal.length];
        System.arraycopy(a.getHeaderId().getBytes(), 0, data, 0, 2);
        System.arraycopy(a.getLocalFileDataLength().getBytes(), 0, data, 2, 2);
        System.arraycopy(aLocal, 0, data, 4, aLocal.length);
        System.arraycopy(dummy.getHeaderId().getBytes(), 0, data,
                         4+aLocal.length, 2);
        System.arraycopy(dummy.getLocalFileDataLength().getBytes(), 0, data,
                         4+aLocal.length+2, 2);
        System.arraycopy(dummyLocal, 0, data,
                         4+aLocal.length+4, dummyLocal.length);

    }

    /**
     * test parser.
     */
    public void testParse() throws Exception {
        ZipExtraField[] ze = ExtraFieldUtils.parse(data);
        assertEquals("number of fields", 2, ze.length);
        assertTrue("type field 1", ze[0] instanceof AsiExtraField);
        assertEquals("mode field 1", 040755,
                     ((AsiExtraField) ze[0]).getMode());
        assertTrue("type field 2", ze[1] instanceof UnrecognizedExtraField);
        assertEquals("data length field 2", 0,
                     ze[1].getLocalFileDataLength().getValue());

        byte[] data2 = new byte[data.length-1];
        System.arraycopy(data, 0, data2, 0, data2.length);
        try {
            ExtraFieldUtils.parse(data2);
            fail("data should be invalid");
        } catch (Exception e) {
            assertEquals("message",
                         "data starting at "+(4+aLocal.length)+" is in unknown format",
                         e.getMessage());
        }
    }

    /**
     * Test merge methods
     */
    public void testMerge() {
        byte[] local =
            ExtraFieldUtils.mergeLocalFileDataData(new ZipExtraField[] {a, dummy});
        assertEquals("local length", data.length, local.length);
        for (int i=0; i<local.length; i++) {
            assertEquals("local byte "+i, data[i], local[i]);
        }

        byte[] dummyCentral = dummy.getCentralDirectoryData();
        byte[] data2 = new byte[4 + aLocal.length + 4 + dummyCentral.length];
        System.arraycopy(data, 0, data2, 0, 4 + aLocal.length + 2);
        System.arraycopy(dummy.getCentralDirectoryLength().getBytes(), 0,
                         data2, 4+aLocal.length+2, 2);
        System.arraycopy(dummyCentral, 0, data2,
                         4+aLocal.length+4, dummyCentral.length);


        byte[] central =
            ExtraFieldUtils.mergeCentralDirectoryData(new ZipExtraField[] {a, dummy});
        assertEquals("central length", data2.length, central.length);
        for (int i=0; i<central.length; i++) {
            assertEquals("central byte "+i, data2[i], central[i]);
        }

    }
}
