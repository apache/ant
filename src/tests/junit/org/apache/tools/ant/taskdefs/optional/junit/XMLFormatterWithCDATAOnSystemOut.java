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
package org.apache.tools.ant.taskdefs.optional.junit;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.apache.tools.ant.BuildFileRule;
import org.apache.tools.ant.util.FileUtils;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;

public class XMLFormatterWithCDATAOnSystemOut {

    private static final String REPORT =
        "TEST-" + XMLFormatterWithCDATAOnSystemOut.class.getName() + ".xml";

    private static final String TESTDATA
        = "<ERROR>"
        + "<![CDATA[<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
        + "  <RESPONSE>"
        + "    <GDS/>"
        + "    <ERROR>"
        + "      <ID/>"
        + "      <MESSAGE/>"
        + "      <REQUEST_TYPE/>"
        + "      <RESEND/>"
        + "      <RAW_RESPONSE/>"
        + "    </ERROR>"
        + "  </RESPONSE>"
        + "]]>"
        + "</ERROR>";

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    @Test
    public void testOutput() {
        System.out.println(TESTDATA);
    }

    @Test
    public void testBuildfile() throws IOException {
        buildRule.configureProject("src/etc/testcases/taskdefs/optional/junit/cdataoutput.xml");
        if (buildRule.getProject().getProperty("cdata.inner") == null) {
            // avoid endless loop
            buildRule.executeTarget("run-junit");
            File f = buildRule.getProject().resolveFile(REPORT);
            try (FileReader reader = new FileReader(f)) {
                assertThat(FileUtils.readFully(reader),
                        containsString("</RESPONSE>&#x5d;&#x5d;&gt;</ERROR>"));
            } finally {
                f.delete();
            }
        }
    }

}
