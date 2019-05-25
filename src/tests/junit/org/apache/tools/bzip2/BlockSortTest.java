/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.tools.bzip2;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class BlockSortTest {

    private static final byte[] FIXTURE = {0, 1, (byte) 252, (byte) 253, (byte) 255,
                                           (byte) 254, 3, 2, (byte) 128};

    /*
      Burrows-Wheeler transform of fixture the manual way:

      * build the matrix

      0, 1, 252, 253, 255, 254, 3, 2, 128
      1, 252, 253, 255, 254, 3, 2, 128, 0
      252, 253, 255, 254, 3, 2, 128, 0, 1
      253, 255, 254, 3, 2, 128, 0, 1, 252
      255, 254, 3, 2, 128, 0, 1, 252, 253
      254, 3, 2, 128, 0, 1, 252, 253, 255
      3, 2, 128, 0, 1, 252, 253, 255, 254
      2, 128, 0, 1, 252, 253, 255, 254, 3
      128, 0, 1, 252, 253, 255, 254, 3, 2

      * sort it

      0, 1, 252, 253, 255, 254, 3, 2, 128
      1, 252, 253, 255, 254, 3, 2, 128, 0
      2, 128, 0, 1, 252, 253, 255, 254, 3
      3, 2, 128, 0, 1, 252, 253, 255, 254
      128, 0, 1, 252, 253, 255, 254, 3, 2
      252, 253, 255, 254, 3, 2, 128, 0, 1
      253, 255, 254, 3, 2, 128, 0, 1, 252
      254, 3, 2, 128, 0, 1, 252, 253, 255
      255, 254, 3, 2, 128, 0, 1, 252, 253

      * grab last column

      128, 0, 3, 254, 2, 1, 252, 255, 253

        and the original line has been 0
    */

    private static final byte[] FIXTURE_BWT = {(byte) 128, 0, 3, (byte) 254, 2, 1,
                                               (byte) 252, (byte) 255, (byte) 253};

    private static final int[] FIXTURE_SORTED = {
        0, 1, 7, 6, 8, 2, 3, 5, 4
    };

    private static final byte[] FIXTURE2 = {
        'C', 'o', 'm', 'm', 'o', 'n', 's', ' ', 'C', 'o', 'm', 'p', 'r', 'e', 's', 's',
    };

    private static final byte[] FIXTURE2_BWT = {
        's', 's', ' ', 'r', 'o', 'm', 'o', 'o', 'C', 'C', 'm', 'm', 'p', 'n', 's', 'e',
    };

    @Test
    public void testSortFixture() {
        DS ds = setUpFixture();
        ds.s.blockSort(ds.data, FIXTURE.length - 1);
        assertFixtureSorted(ds.data);
        assertEquals(0, ds.data.origPtr);
    }

    @Test
    public void testSortFixtureMainSort() {
        DS ds = setUpFixture();
        ds.s.mainSort(ds.data, FIXTURE.length - 1);
        assertFixtureSorted(ds.data);
    }

    @Test
    public void testSortFixtureFallbackSort() {
        DS ds = setUpFixture();
        ds.s.fallbackSort(ds.data, FIXTURE.length - 1);
        assertFixtureSorted(ds.data);
    }

    @Test
    public void testSortFixture2() {
        DS ds = setUpFixture2();
        ds.s.blockSort(ds.data, FIXTURE2.length - 1);
        assertFixture2Sorted(ds.data);
        assertEquals(1, ds.data.origPtr);
    }

    @Test
    public void testSortFixture2MainSort() {
        DS ds = setUpFixture2();
        ds.s.mainSort(ds.data, FIXTURE2.length - 1);
        assertFixture2Sorted(ds.data);
    }

    @Test
    public void testSortFixture2FallbackSort() {
        DS ds = setUpFixture2();
        ds.s.fallbackSort(ds.data, FIXTURE2.length - 1);
        assertFixture2Sorted(ds.data);
    }

    @Test
    public void testFallbackSort() {
        CBZip2OutputStream.Data data = new CBZip2OutputStream.Data(1);
        BlockSort s = new BlockSort(data);
        int[] fmap = new int[FIXTURE.length];
        s.fallbackSort(fmap, FIXTURE, FIXTURE.length);
        assertArrayEquals(FIXTURE_SORTED, fmap);
    }

    private DS setUpFixture() {
        return setUpFixture(FIXTURE);
    }

    private void assertFixtureSorted(CBZip2OutputStream.Data data) {
        assertFixtureSorted(data, FIXTURE, FIXTURE_BWT);
    }

    private DS setUpFixture2() {
        return setUpFixture(FIXTURE2);
    }

    private void assertFixture2Sorted(CBZip2OutputStream.Data data) {
        assertFixtureSorted(data, FIXTURE2, FIXTURE2_BWT);
    }

    private DS setUpFixture(byte[] fixture) {
        CBZip2OutputStream.Data data = new CBZip2OutputStream.Data(1);
        System.arraycopy(fixture, 0, data.block, 1, fixture.length);
        return new DS(data, new BlockSort(data));
    }

    private void assertFixtureSorted(CBZip2OutputStream.Data data,
                                     byte[] fixture, byte[] fixtureBwt) {
        assertEquals(fixture[fixture.length - 1], data.block[0]);
        for (int i = 0; i < fixture.length; i++) {
            assertEquals(fixtureBwt[i], data.block[data.fmap[i]]);
        }
    }

    private static class DS {
        private final CBZip2OutputStream.Data data;
        private final BlockSort s;
        DS(CBZip2OutputStream.Data data, BlockSort s) {
            this.data = data;
            this.s = s;
        }
    }
}
