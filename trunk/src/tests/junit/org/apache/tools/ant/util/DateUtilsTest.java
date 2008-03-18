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
package org.apache.tools.ant.util;

import java.util.Calendar;
import java.util.TimeZone;

import junit.framework.TestCase;

/**
 * TestCase for DateUtils.
 *
 */
public class DateUtilsTest extends TestCase {
    public DateUtilsTest(String s) {
        super(s);
    }

    public void testElapsedTime(){
        String text = DateUtils.formatElapsedTime(50*1000);
        assertEquals("50 seconds", text);
        text = DateUtils.formatElapsedTime(65*1000);
        assertEquals("1 minute 5 seconds", text);
        text = DateUtils.formatElapsedTime(120*1000);
        assertEquals("2 minutes 0 seconds", text);
        text = DateUtils.formatElapsedTime(121*1000);
        assertEquals("2 minutes 1 second", text);
    }

    public void testDateTimeISO(){
        TimeZone timeZone = TimeZone.getTimeZone("GMT+1");
        Calendar cal = Calendar.getInstance(timeZone);
        cal.set(2002,1,23,10,11,12);
        String text = DateUtils.format(cal.getTime(),
                DateUtils.ISO8601_DATETIME_PATTERN);
        assertEquals("2002-02-23T09:11:12", text);
    }

    public void testDateISO(){
        TimeZone timeZone = TimeZone.getTimeZone("GMT");
        Calendar cal = Calendar.getInstance(timeZone);
        cal.set(2002,1,23);
        String text = DateUtils.format(cal.getTime(),
                DateUtils.ISO8601_DATE_PATTERN);
        assertEquals("2002-02-23", text);
    }

    public void testTimeISODate(){
        // make sure that elapsed time in set via date works
        TimeZone timeZone = TimeZone.getTimeZone("GMT+1");
        Calendar cal = Calendar.getInstance(timeZone);
        cal.set(2002,1,23, 21, 11, 12);
        String text = DateUtils.format(cal.getTime(),
                DateUtils.ISO8601_TIME_PATTERN);
        assertEquals("20:11:12", text);
    }

    public void testTimeISO(){
        // make sure that elapsed time in ms works
        long ms = (20*3600 + 11*60 + 12)*1000;
        String text = DateUtils.format(ms,
                DateUtils.ISO8601_TIME_PATTERN);
        assertEquals("20:11:12", text);
    }

    public void testPhaseOfMoon() {
        TimeZone timeZone = TimeZone.getTimeZone("GMT");
        Calendar cal = Calendar.getInstance(timeZone);
        // should be full moon
        cal.set(2002, 2, 27);
        assertEquals(4, DateUtils.getPhaseOfMoon(cal));
        // should be new moon
        cal.set(2002, 2, 12);
        assertEquals(0, DateUtils.getPhaseOfMoon(cal));
    }
}
