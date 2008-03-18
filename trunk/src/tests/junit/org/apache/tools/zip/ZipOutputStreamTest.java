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

import java.util.Calendar;
import java.util.Date;

import junit.framework.TestCase;

public class ZipOutputStreamTest extends TestCase {
    
    private Date time;
    private ZipLong zl;
    
    /**
     * Constructor
     */	
    public ZipOutputStreamTest(String name) {
        super(name);
    }
	
    protected void setUp() throws Exception {
        time = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(time);
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1;
        long value =  ((year - 1980) << 25)
            |         (month << 21)
            |	      (cal.get(Calendar.DAY_OF_MONTH) << 16)
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

    protected void tearDown() throws Exception {
        super.tearDown();
    }
    
    public void testZipLong() throws Exception {
        ZipLong test = ZipOutputStream.toDosTime(time);
        assertEquals(test.getValue(), zl.getValue());
    }

    public void testAdjustToLong() {
        assertEquals((long) Integer.MAX_VALUE,
                     ZipOutputStream.adjustToLong(Integer.MAX_VALUE));
        assertEquals(((long) Integer.MAX_VALUE) + 1,
                     ZipOutputStream.adjustToLong(Integer.MAX_VALUE + 1));
        assertEquals(2 * ((long) Integer.MAX_VALUE),
                     ZipOutputStream.adjustToLong(2 * Integer.MAX_VALUE));
    }

}
