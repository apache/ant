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
package org.apache.tools.ant.taskdefs.optional.junit;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import org.apache.tools.ant.BuildFileTest;
import org.apache.tools.ant.util.FileUtils;

public class XMLFormatterWithCDATAOnSystemOut extends BuildFileTest {

    private static String DIR = "src/etc/testcases/taskdefs/optional/junit";
    private static String REPORT =
        "TEST-" + XMLFormatterWithCDATAOnSystemOut.class.getName() + ".xml";

    private static String TESTDATA =
        "<ERROR>" +
        "<![CDATA[<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
        "  <RESPONSE>" +
        "    <GDS/>" +
        "    <ERROR>" +
        "      <ID/>" +
        "      <MESSAGE/>" +
        "      <REQUEST_TYPE/>" +
        "      <RESEND/>" +
        "      <RAW_RESPONSE/>" +
        "    </ERROR>" +
        "  </RESPONSE>" +
        "]]>" +
        "</ERROR>";

    public XMLFormatterWithCDATAOnSystemOut(String name) {
        super(name);
    }

    public void testOutput() {
        System.out.println(TESTDATA);
    }

    public void testBuildfile() throws IOException {
        configureProject(DIR + "/cdataoutput.xml");
        if (getProject().getProperty("cdata.inner") == null) {
            // avoid endless loop
            executeTarget("run-junit");
            File f = getProject().resolveFile(REPORT);
            FileReader reader = null;
            try {
                reader = new FileReader(f);
                String content = FileUtils.readFully(reader);
                assertTrue(content.indexOf("</RESPONSE>&#x5d;&#x5d;&gt;"
                                           + "</ERROR>") > 0);
            } finally {
                if (reader != null) {
                    reader.close();
                }
                f.delete();
            }
        }
    }

}
