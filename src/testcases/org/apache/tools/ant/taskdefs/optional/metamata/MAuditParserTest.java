/*
 * Copyright  2002,2004 Apache Software Foundation
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
package org.apache.tools.ant.taskdefs.optional.metamata;

import java.io.File;

import junit.framework.TestCase;

import org.apache.tools.ant.util.StringUtils;

/**
 * Test for the Audit parser.
 *
 * @author <a href="mailto:sbailliez@apache.org">Stephane Bailliez</a>
 */
public class MAuditParserTest extends TestCase {

    private MAuditParser parser;

    public MAuditParserTest(String s) {
        super(s);
    }

    protected void setUp() {
        parser = new MAuditParser();
    }

    public void testViolation() {
        String line = "file:\\WebGain\\QA\\examples\\auditexamples\\Vector.java:55: Array declarators (\"[]\") should be placed with their component types and not after field/method declarations (5.27).";
        // the replace is done to simulate a platform dependant separator since
        // the parser may do some magic with the file separator
        line = StringUtils.replace(line, "\\", File.separator);
        MAuditParser.Violation violation = parser.parseLine(line);
        assertEquals("\\WebGain\\QA\\examples\\auditexamples\\Vector.java",
                StringUtils.replace(violation.file, File.separator, "\\"));
        assertEquals("55", violation.line);
        assertEquals("Array declarators (\"[]\") should be placed with their component types and not after field/method declarations (5.27).", violation.error);
    }

    public void testNonViolation(){
        String line = "Audit completed with 36 violations.";
        Object violation = parser.parseLine(line);
        assertNull(violation);
    }

    public void testFilePathInViolation(){
        String line = "file:\\WebGain\\QA\\examples\\auditexamples\\Hashtable.java:302: Loop variable defined at file:\\WebGain\\QA\\examples\\auditexamples\\Hashtable.java:300 is being modified (5.16).";
        line = StringUtils.replace(line, "\\", File.separator);
        MAuditParser.Violation violation = parser.parseLine(line);
        assertEquals("\\WebGain\\QA\\examples\\auditexamples\\Hashtable.java",
                StringUtils.replace(violation.file, File.separator, "\\"));
        assertEquals("302", violation.line);
        assertEquals("Loop variable defined at Hashtable.java:300 is being modified (5.16).", violation.error);
    }

}
