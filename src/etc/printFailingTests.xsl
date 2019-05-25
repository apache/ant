<?xml version="1.0" encoding="ISO-8859-1"?>
<!--
   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

       https://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
-->

<!--
  This stylesheet is for work in build.xml:printFailingTests.
  It extracts all failing tests from a given testsuite (JUnit+AntUnit XML format)
  and writes that into a text file.
  All text files are written to STDOUT via <concat>.
-->
<xsl:stylesheet
  version="1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <!-- Output format so no XML header would be written -->
    <xsl:output indent="no" method="text" encoding="ISO-8859-1"/>
    <!-- What is the name of the current testsuite (JUnit class or AntUnit buildfile) -->
    <xsl:variable name="testsuite" select="/testsuite/@name"/>


<!-- failing tests: suitename.testname : message; Leading pipe for line break -->
<xsl:template match="testcase[failure|error]">
| <xsl:value-of select="$testsuite"/>.<xsl:value-of select="@name"/>() : <xsl:value-of select="failure/@message"/><xsl:value-of select="error/@message"/>
</xsl:template>

<!-- Suppress log output from the tests like stacktraces -->
<xsl:template match="text()"/>


</xsl:stylesheet>

