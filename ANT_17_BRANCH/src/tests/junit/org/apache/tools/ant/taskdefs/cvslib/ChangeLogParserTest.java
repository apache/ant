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
package org.apache.tools.ant.taskdefs.cvslib;

import junit.framework.TestCase;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Date;

/**
 * Minimal test of the parser implementation
 */
public class ChangeLogParserTest extends TestCase {

    protected ChangeLogParser parser = new ChangeLogParser();

    public void testOldCvsFormat() throws Exception {
        parser.stdout("Working file: build.xml");
        parser.stdout("revision 1.475");
        parser.stdout("date: 2004/06/05 16:10:32;  author: somebody;  state: Exp;  lines: +2 -2");
        parser.stdout("I have done something. I swear.");
        parser.stdout("=============================================================================");
        CVSEntry[] entries = parser.getEntrySetAsArray();
        assertEquals("somebody", entries[0].getAuthor());
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.US);
        cal.set(Calendar.MILLISECOND, 0);
        cal.set(2004, Calendar.JUNE, 5, 16, 10, 32);
        Date date = cal.getTime();
        assertEquals(date, entries[0].getDate());
    }

    public void testCvs112Format() throws Exception {
        parser.stdout("Working file: build.xml");
        parser.stdout("revision 1.475");
        parser.stdout("date: 2004-06-05 16:10:32 +0000; author: somebody; state: Exp;  lines: +2 -2");
        parser.stdout("I have done something. I swear.");
        parser.stdout("=============================================================================");
        CVSEntry[] entries = parser.getEntrySetAsArray();
        assertEquals("somebody", entries[0].getAuthor());
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.US);
        cal.set(Calendar.MILLISECOND, 0);
        cal.set(2004, Calendar.JUNE, 5, 16, 10, 32);
        Date date = cal.getTime();
        assertEquals(date, entries[0].getDate());
    }
}
