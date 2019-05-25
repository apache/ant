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

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.zip.ZipException;

/**
 * JUnit 4 testcases for org.apache.tools.zip.ExtraFieldUtils.
 */
public class ExtraFieldUtilsTest implements UnixStat {

    /**
     * Header-ID of a ZipExtraField not supported by Ant.
     *
     * <p>Used to be ZipShort(1) but this is the ID of the Zip64 extra
     * field.</p>
     */
    static final ZipShort UNRECOGNIZED_HEADER = new ZipShort(0x5555);

    private AsiExtraField a;
    private UnrecognizedExtraField dummy;
    private byte[] data;
    private byte[] aLocal;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() {
        a = new AsiExtraField();
        a.setMode(0755);
        a.setDirectory(true);
        dummy = new UnrecognizedExtraField();
        dummy.setHeaderId(UNRECOGNIZED_HEADER);
        dummy.setLocalFileDataData(new byte[] {0});
        dummy.setCentralDirectoryData(new byte[] {0});

        aLocal = a.getLocalFileDataData();
        byte[] dummyLocal = dummy.getLocalFileDataData();
        data = new byte[4 + aLocal.length + 4 + dummyLocal.length];
        System.arraycopy(a.getHeaderId().getBytes(), 0, data, 0, 2);
        System.arraycopy(a.getLocalFileDataLength().getBytes(), 0, data, 2, 2);
        System.arraycopy(aLocal, 0, data, 4, aLocal.length);
        System.arraycopy(dummy.getHeaderId().getBytes(), 0, data,
                         4 + aLocal.length, 2);
        System.arraycopy(dummy.getLocalFileDataLength().getBytes(), 0, data,
                         4 + aLocal.length + 2, 2);
        System.arraycopy(dummyLocal, 0, data,
                         4 + aLocal.length + 4, dummyLocal.length);

    }

    /**
     * test parser.
     */
    @Test
    public void testParse() throws Exception {
        thrown.expect(ZipException.class);
        thrown.expectMessage("bad extra field starting at " + (4 + aLocal.length)
                        + ".  Block length of 1 bytes exceeds remaining data of 0 bytes.");

        ZipExtraField[] ze = ExtraFieldUtils.parse(data);
        assertEquals("number of fields", 2, ze.length);
        assertThat("type field 1", ze[0], instanceOf(AsiExtraField.class));
        assertEquals("mode field 1", 040755,
                     ((AsiExtraField) ze[0]).getMode());
        assertThat("type field 2", ze[1], instanceOf(UnrecognizedExtraField.class));
        assertEquals("data length field 2", 1,
                     ze[1].getLocalFileDataLength().getValue());

        byte[] data2 = new byte[data.length - 1];
        System.arraycopy(data, 0, data2, 0, data2.length);
        ExtraFieldUtils.parse(data2);
    }

    @Test
    public void testParseWithRead() throws Exception {
        ZipExtraField[] ze =
            ExtraFieldUtils.parse(data, true,
                                  ExtraFieldUtils.UnparseableExtraField.READ);
        assertEquals("number of fields", 2, ze.length);
        assertThat("type field 1", ze[0], instanceOf(AsiExtraField.class));
        assertEquals("mode field 1", 040755,
                     ((AsiExtraField) ze[0]).getMode());
        assertThat("type field 2", ze[1], instanceOf(UnrecognizedExtraField.class));
        assertEquals("data length field 2", 1,
                     ze[1].getLocalFileDataLength().getValue());

        byte[] data2 = new byte[data.length - 1];
        System.arraycopy(data, 0, data2, 0, data2.length);
        ze = ExtraFieldUtils.parse(data2, true,
                                   ExtraFieldUtils.UnparseableExtraField.READ);
        assertEquals("number of fields", 2, ze.length);
        assertThat("type field 1", ze[0], instanceOf(AsiExtraField.class));
        assertEquals("mode field 1", 040755,
                     ((AsiExtraField) ze[0]).getMode());
        assertThat("type field 2", ze[1], instanceOf(UnparseableExtraFieldData.class));
        assertEquals("data length field 2", 4,
                     ze[1].getLocalFileDataLength().getValue());
        for (int i = 0; i < 4; i++) {
            assertEquals("byte number " + i,
                         data2[data.length - 5 + i],
                         ze[1].getLocalFileDataData()[i]);
        }
    }

    @Test
    public void testParseWithSkip() throws Exception {
        ZipExtraField[] ze =
            ExtraFieldUtils.parse(data, true,
                                  ExtraFieldUtils.UnparseableExtraField.SKIP);
        assertEquals("number of fields", 2, ze.length);
        assertThat("type field 1", ze[0], instanceOf(AsiExtraField.class));
        assertEquals("mode field 1", 040755,
                     ((AsiExtraField) ze[0]).getMode());
        assertThat("type field 2", ze[1], instanceOf(UnrecognizedExtraField.class));
        assertEquals("data length field 2", 1,
                     ze[1].getLocalFileDataLength().getValue());

        byte[] data2 = new byte[data.length - 1];
        System.arraycopy(data, 0, data2, 0, data2.length);
        ze = ExtraFieldUtils.parse(data2, true,
                                   ExtraFieldUtils.UnparseableExtraField.SKIP);
        assertEquals("number of fields", 1, ze.length);
        assertThat("type field 1", ze[0], instanceOf(AsiExtraField.class));
        assertEquals("mode field 1", 040755,
                     ((AsiExtraField) ze[0]).getMode());
    }

    /**
     * Test merge methods
     */
    @Test
    public void testMerge() {
        byte[] local =
            ExtraFieldUtils.mergeLocalFileDataData(new ZipExtraField[] {a, dummy});
        assertEquals("local length", data.length, local.length);
        for (int i = 0; i < local.length; i++) {
            assertEquals("local byte " + i, data[i], local[i]);
        }

        byte[] dummyCentral = dummy.getCentralDirectoryData();
        byte[] data2 = new byte[4 + aLocal.length + 4 + dummyCentral.length];
        System.arraycopy(data, 0, data2, 0, 4 + aLocal.length + 2);
        System.arraycopy(dummy.getCentralDirectoryLength().getBytes(), 0,
                         data2, 4 + aLocal.length + 2, 2);
        System.arraycopy(dummyCentral, 0, data2,
                         4 + aLocal.length + 4, dummyCentral.length);


        byte[] central =
            ExtraFieldUtils.mergeCentralDirectoryData(new ZipExtraField[] {a, dummy});
        assertEquals("central length", data2.length, central.length);
        for (int i = 0; i < central.length; i++) {
            assertEquals("central byte " + i, data2[i], central[i]);
        }

    }

    @Test
    public void testMergeWithUnparseableData() throws Exception {
        ZipExtraField d = new UnparseableExtraFieldData();
        byte[] b = UNRECOGNIZED_HEADER.getBytes();
        d.parseFromLocalFileData(new byte[] {b[0], b[1], 1, 0}, 0, 4);
        byte[] local =
            ExtraFieldUtils.mergeLocalFileDataData(new ZipExtraField[] {a, d});
        assertEquals("local length", data.length - 1, local.length);
        for (int i = 0; i < local.length; i++) {
            assertEquals("local byte " + i, data[i], local[i]);
        }

        byte[] dCentral = d.getCentralDirectoryData();
        byte[] data2 = new byte[4 + aLocal.length + dCentral.length];
        System.arraycopy(data, 0, data2, 0, 4 + aLocal.length + 2);
        System.arraycopy(dCentral, 0, data2,
                         4 + aLocal.length, dCentral.length);


        byte[] central =
            ExtraFieldUtils.mergeCentralDirectoryData(new ZipExtraField[] {a, d});
        assertEquals("central length", data2.length, central.length);
        for (int i = 0; i < central.length; i++) {
            assertEquals("central byte " + i, data2[i], central[i]);
        }

    }
}
