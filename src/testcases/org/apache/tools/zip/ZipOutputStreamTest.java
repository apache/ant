/*
 * Copyright 2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
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

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.Date;

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
        byte[] result = new byte[4];
        int year = time.getYear() + 1900;
        int month = time.getMonth() + 1;
        long value =  ((year - 1980) << 25)
            |         (month << 21)
            |         (time.getDate() << 16)
            |         (time.getHours() << 11)
            |         (time.getMinutes() << 5)
            |         (time.getSeconds() >> 1);

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

}
