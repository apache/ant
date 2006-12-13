<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
    xmlns:lxslt="http://xml.apache.org/xslt"
    xmlns:redirect="http://xml.apache.org/xalan/redirect"
    xmlns:stringutils="xalan://org.apache.tools.ant.util.StringUtils"
    extension-element-prefixes="redirect">
<xsl:output method="html" indent="yes" encoding="US-ASCII"/>
<xsl:decimal-format decimal-separator="." grouping-separator=","/>
<!--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->

<!--

 Sample stylesheet to be used with Ant JUnitReport output.

 It creates a set of HTML files a la javadoc where you can browse easily
 through all directories and projects.

-->
<xsl:param name="output.dir" select="'.'"/>


<xsl:template match="testsuites">
    <!-- create the index.html -->
    <redirect:write file="{$output.dir}/index.html">
        <xsl:call-template name="index.html"/>
    </redirect:write>

    <!-- create the stylesheet.css -->
    <redirect:write file="{$output.dir}/stylesheet.css">
        <xsl:call-template name="stylesheet.css"/>
    </redirect:write>

    <!-- create the overview-directories.html at the root -->
    <redirect:write file="{$output.dir}/overview-summary.html">
        <xsl:apply-templates select="." mode="overview.directories"/>
    </redirect:write>

    <!-- create the all-directories.html at the root -->
    <redirect:write file="{$output.dir}/overview-frame.html">
        <xsl:apply-templates select="." mode="all.directories"/>
    </redirect:write>

    <!-- create the all-projects.html at the root -->
    <redirect:write file="{$output.dir}/allprojects-frame.html">
        <xsl:apply-templates select="." mode="all.projects"/>
    </redirect:write>

    <!-- create the all-tests.html at the root -->
    <redirect:write file="{$output.dir}/all-tests.html">
        <xsl:apply-templates select="." mode="all.tests"/>
    </redirect:write>

    <!-- create the alltests-fails.html at the root -->
    <redirect:write file="{$output.dir}/alltests-fails.html">
      <xsl:apply-templates select="." mode="all.tests">
        <xsl:with-param name="type" select="'fails'"/>
      </xsl:apply-templates>
    </redirect:write>

  <!-- create the alltests-errors.html at the root -->
    <redirect:write file="{$output.dir}/alltests-errors.html">
      <xsl:apply-templates select="." mode="all.tests">
        <xsl:with-param name="type" select="'errors'"/>
      </xsl:apply-templates>
    </redirect:write>

  <!-- process all directories -->
    <xsl:for-each select="./testsuite[not(./@package = preceding-sibling::testsuite/@package)]">
        <xsl:call-template name="directory">
            <xsl:with-param name="name" select="@package"/>
        </xsl:call-template>
    </xsl:for-each>
</xsl:template>


<xsl:template name="directory">
    <xsl:param name="name"/>
    <xsl:variable name="directory.dir">
        <xsl:if test="not($name = '')"><xsl:value-of select="translate($name,'.','/')"/></xsl:if>
        <xsl:if test="$name = ''">.</xsl:if>
    </xsl:variable>
    <!--Processing directory <xsl:value-of select="@name"/> in <xsl:value-of select="$output.dir"/> -->
    <!-- create a projects-list.html in the directory directory -->
    <redirect:write file="{$output.dir}/{$directory.dir}/directory-frame.html">
        <xsl:call-template name="projects.list">
            <xsl:with-param name="name" select="$name"/>
        </xsl:call-template>
    </redirect:write>

    <!-- create a directory-summary.html in the directory directory -->
    <redirect:write file="{$output.dir}/{$directory.dir}/directory-summary.html">
        <xsl:call-template name="directory.summary">
            <xsl:with-param name="name" select="$name"/>
        </xsl:call-template>
    </redirect:write>

    <!-- for each project, creates a @name.html -->
    <!-- @bug there will be a problem with inner projects having the same name, it will be overwritten -->
  <xsl:for-each select="/testsuites/testsuite[@package = $name]">
    <redirect:write file="{$output.dir}/{$directory.dir}/{@id}_{@name}.html">
      <xsl:apply-templates select="." mode="project.details"/>
    </redirect:write>
    <xsl:if test="string-length(./system-out)!=0">
      <redirect:write file="{$output.dir}/{$directory.dir}/{@id}_{@name}-out.txt">
        <xsl:value-of disable-output-escaping="yes" select="./system-out"/>
      </redirect:write>
    </xsl:if>
    <xsl:if test="string-length(./system-err)!=0">
      <redirect:write file="{$output.dir}/{$directory.dir}/{@id}_{@name}-err.txt">
        <xsl:value-of disable-output-escaping="yes" select="./system-err"/>
      </redirect:write>
    </xsl:if>
    <xsl:if test="failures/text() != 0">
      <redirect:write file="{$output.dir}/{$directory.dir}/{@id}_{@name}-fails.html">
        <xsl:apply-templates select="." mode="project.details">
          <xsl:with-param name="type" select="'fails'"/>
        </xsl:apply-templates>
      </redirect:write>
    </xsl:if>
    <xsl:if test="errors/text() != 0">
      <redirect:write file="{$output.dir}/{$directory.dir}/{@id}_{@name}-errors.html">
        <xsl:apply-templates select="." mode="project.details">
          <xsl:with-param name="type" select="'errors'"/>
        </xsl:apply-templates>
      </redirect:write>
    </xsl:if>
  </xsl:for-each>
</xsl:template>

<xsl:template name="index.html">
<html>
    <head>
        <title>AntUnit Test Results.</title>
    </head>
    <frameset cols="20%,80%">
        <frameset rows="30%,70%">
            <frame src="overview-frame.html" name="directoryListFrame"/>
            <frame src="allprojects-frame.html" name="projectListFrame"/>
        </frameset>
        <frame src="overview-summary.html" name="projectFrame"/>
        <noframes>
            <h2>Frame Alert</h2>
            <p>
                This document is designed to be viewed using the frames feature. If you see this message, you are using a non-frame-capable web client.
            </p>
        </noframes>
    </frameset>
</html>
</xsl:template>

<!-- this is the stylesheet css to use for nearly everything -->
<xsl:template name="stylesheet.css">
body {
    font:normal 68% verdana,arial,helvetica;
    color:#000000;
}
table tr td, table tr th {
    font-size: 68%;
}
table.details tr th{
    font-weight: bold;
    text-align:left;
    background:#a6caf0;
}
table.details tr td{
    background:#eeeee0;
}

p {
    line-height:1.5em;
    margin-top:0.5em; margin-bottom:1.0em;
}
h1 {
    margin: 0px 0px 5px; font: 165% verdana,arial,helvetica
}
h2 {
    margin-top: 1em; margin-bottom: 0.5em; font: bold 125% verdana,arial,helvetica
}
h3 {
    margin-bottom: 0.5em; font: bold 115% verdana,arial,helvetica
}
h4 {
    margin-bottom: 0.5em; font: bold 100% verdana,arial,helvetica
}
h5 {
    margin-bottom: 0.5em; font: bold 100% verdana,arial,helvetica
}
h6 {
    margin-bottom: 0.5em; font: bold 100% verdana,arial,helvetica
}
.Error {
    font-weight:bold; color:red;
}
.Failure {
    font-weight:bold; color:purple;
}
.Properties {
  text-align:right;
}
</xsl:template>

<!-- Create list of all/failed/errored tests -->
<xsl:template match="testsuites" mode="all.tests">
    <xsl:param name="type" select="'all'"/>
    <html>
	<xsl:variable name="title">
	    <xsl:choose>
		<xsl:when test="$type = 'fails'">
		    <xsl:text>All Failures</xsl:text>
		</xsl:when>
		<xsl:when test="$type = 'errors'">
		    <xsl:text>All Errors</xsl:text>
		</xsl:when>
		<xsl:otherwise>
		    <xsl:text>All Tests</xsl:text>
		</xsl:otherwise>
	    </xsl:choose>
	</xsl:variable>
	<head>
	    <title>AntUnit Test Results: <xsl:value-of select="$title"/></title>
	    <xsl:call-template name="create.stylesheet.link">
                <xsl:with-param name="directory.name"/>
            </xsl:call-template>
	</head>
	<body>
	    <xsl:attribute name="onload">open('allprojects-frame.html','projectListFrame')</xsl:attribute>
            <xsl:call-template name="pageHeader"/>
            <h2><xsl:value-of select="$title"/></h2>

            <table class="details" border="0" cellpadding="5" cellspacing="2" width="95%">
		<xsl:call-template name="testcase.test.header">
		    <xsl:with-param name="show.project" select="'yes'"/>
		</xsl:call-template>
		<!--
                test can even not be started at all (failure to load the project)
		so report the error directly
		-->
              <xsl:if test="./error">
                <tr class="Error">
                  <td colspan="4">
                    <xsl:apply-templates select="./error"/>
                  </td>
                </tr>
              </xsl:if>
              <xsl:choose>
                <xsl:when test="$type = 'fails'">
                  <xsl:apply-templates select=".//testcase[failure]" mode="print.test">
                    <xsl:with-param name="show.project" select="'yes'"/>
                  </xsl:apply-templates>
                </xsl:when>
                <xsl:when test="$type = 'errors'">
                  <xsl:apply-templates select=".//testcase[error]" mode="print.test">
                    <xsl:with-param name="show.project" select="'yes'"/>
                  </xsl:apply-templates>
                </xsl:when>
                <xsl:otherwise>
                  <xsl:apply-templates select=".//testcase" mode="print.test">
                    <xsl:with-param name="show.project" select="'yes'"/>
                  </xsl:apply-templates>
                </xsl:otherwise>
              </xsl:choose>
            </table>
        </body>
    </html>
</xsl:template>


<!-- ======================================================================
    This page is created for every testsuite project.
    It prints a summary of the testsuite and detailed information about
    testcase methods.
     ====================================================================== -->
<xsl:template match="testsuite" mode="project.details">
    <xsl:param name="type" select="'all'"/>
    <xsl:variable name="directory.name" select="@package"/>
    <xsl:variable name="project.name"><xsl:if test="not($directory.name = '')"><xsl:value-of select="$directory.name"/>.</xsl:if><xsl:value-of select="@name"/></xsl:variable>
    <html>
        <head>
          <title>AntUnit Test Results: <xsl:value-of select="$project.name"/></title>
            <xsl:call-template name="create.stylesheet.link">
                <xsl:with-param name="directory.name" select="$directory.name"/>
            </xsl:call-template>
       <script type="text/javascript" language="JavaScript">
        var TestCases = new Array();
        var cur;
        <xsl:apply-templates select="properties"/>
       </script>
       <script type="text/javascript" language="JavaScript"><![CDATA[
        function displayProperties (name) {
          var win = window.open('','JUnitSystemProperties','scrollbars=1,resizable=1');
          var doc = win.document;
          doc.open();
          doc.write("<html><head><title>Properties of " + name + "</title>");
          doc.write("<style type=\"text/css\">");
          doc.write("body {font:normal 68% verdana,arial,helvetica; color:#000000; }");
          doc.write("table tr td, table tr th { font-size: 68%; }");
          doc.write("table.properties { border-collapse:collapse; border-left:solid 1 #cccccc; border-top:solid 1 #cccccc; padding:5px; }");
          doc.write("table.properties th { text-align:left; border-right:solid 1 #cccccc; border-bottom:solid 1 #cccccc; background-color:#eeeeee; }");
          doc.write("table.properties td { font:normal; text-align:left; border-right:solid 1 #cccccc; border-bottom:solid 1 #cccccc; background-color:#fffffff; }");
          doc.write("h3 { margin-bottom: 0.5em; font: bold 115% verdana,arial,helvetica }");
          doc.write("</style>");
          doc.write("</head><body>");
          doc.write("<h3>Properties of " + name + "</h3>");
          doc.write("<div align=\"right\"><a href=\"javascript:window.close();\">Close</a></div>");
          doc.write("<table class='properties'>");
          doc.write("<tr><th>Name</th><th>Value</th></tr>");
          for (prop in TestCases[name]) {
            doc.write("<tr><th>" + prop + "</th><td>" + TestCases[name][prop] + "</td></tr>");
          }
          doc.write("</table>");
          doc.write("</body></html>");
          doc.close();
          win.focus();
        }
      ]]>
      </script>
        </head>
        <body>
            <xsl:call-template name="pageHeader"/>
            <h3>Project <xsl:value-of select="$project.name"/></h3>


            <table class="details" border="0" cellpadding="5" cellspacing="2" width="95%">
                <xsl:call-template name="testsuite.test.header"/>
                <xsl:apply-templates select="." mode="print.test"/>
            </table>

	    <xsl:choose>
		<xsl:when test="$type = 'fails'">
		    <h2>Failures</h2>
		</xsl:when>
		<xsl:when test="$type = 'errors'">
		    <h2>Errors</h2>
		</xsl:when>
		<xsl:otherwise>
		    <h2>Tests</h2>
		</xsl:otherwise>
	    </xsl:choose>
            <table class="details" border="0" cellpadding="5" cellspacing="2" width="95%">
		<xsl:call-template name="testcase.test.header"/>
		<!--
                test can even not be started at all (failure to load the project)
		so report the error directly
		-->
                <xsl:if test="./error">
                    <tr class="Error">
                        <td colspan="4"><xsl:apply-templates select="./error"/></td>
                    </tr>
                </xsl:if>
		<xsl:choose>
		    <xsl:when test="$type = 'fails'">
			<xsl:apply-templates select="./testcase[failure]" mode="print.test"/>
		    </xsl:when>
		    <xsl:when test="$type = 'errors'">
			<xsl:apply-templates select="./testcase[error]" mode="print.test"/>
		    </xsl:when>
		    <xsl:otherwise>
			<xsl:apply-templates select="./testcase" mode="print.test"/>
		    </xsl:otherwise>
		</xsl:choose>
            </table>
            <!--div class="Properties">
                <a>
                    <xsl:attribute name="href">javascript:displayProperties('<xsl:value-of select="@package"/>.<xsl:value-of select="@name"/>');</xsl:attribute>
                    Properties &#187;
                </a>
            </div>
            <xsl:if test="string-length(./system-out)!=0">
                <div class="Properties">
                    <a>
                        <xsl:attribute name="href">./<xsl:value-of select="@id"/>_<xsl:value-of select="@name"/>-out.txt</xsl:attribute>
                        System.out &#187;
                    </a>
                </div>
            </xsl:if>
            <xsl:if test="string-length(./system-err)!=0">
                <div class="Properties">
                    <a>
                        <xsl:attribute name="href">./<xsl:value-of select="@id"/>_<xsl:value-of select="@name"/>-err.txt</xsl:attribute>
                        System.err &#187;
                    </a>
                </div>
            </xsl:if-->
        </body>
    </html>
</xsl:template>

  <!--
   Write properties into a JavaScript data structure.
   This is based on the original idea by Erik Hatcher (ehatcher@apache.org)
   -->
  <xsl:template match="properties">
    cur = TestCases['<xsl:value-of select="../@package"/>.<xsl:value-of select="../@name"/>'] = new Array();
    <xsl:for-each select="property">
    <xsl:sort select="@name"/>
        cur['<xsl:value-of select="@name"/>'] = '<xsl:call-template name="JS-escape"><xsl:with-param name="string" select="@value"/></xsl:call-template>';
    </xsl:for-each>
  </xsl:template>


<!-- ======================================================================
    This page is created for every directory.
    It prints the name of all projects that belongs to this directory.
    @param name the directory name to print projects.
     ====================================================================== -->
<!-- list of projects in a directory -->
<xsl:template name="projects.list">
    <xsl:param name="name"/>
    <html>
        <head>
            <title>AntUnit Test Projects: <xsl:value-of select="$name"/></title>
            <xsl:call-template name="create.stylesheet.link">
                <xsl:with-param name="directory.name" select="$name"/>
            </xsl:call-template>
        </head>
        <body>
            <table width="100%">
                <tr>
                    <td nowrap="nowrap">
                        <h2><a href="directory-summary.html" target="projectFrame">
                            <xsl:value-of select="$name"/>
                            <xsl:if test="$name = ''">&lt;none&gt;</xsl:if>
                        </a></h2>
                    </td>
                </tr>
            </table>

            <h2>Projects</h2>
            <table width="100%">
                <xsl:for-each select="/testsuites/testsuite[./@package = $name]">
                    <xsl:sort select="@name"/>
                    <tr>
                        <td nowrap="nowrap">
                            <a href="{@id}_{@name}.html" target="projectFrame"><xsl:value-of select="@name"/></a>
                        </td>
                    </tr>
                </xsl:for-each>
            </table>
        </body>
    </html>
</xsl:template>


<!--
    Creates an all-projects.html file that contains a link to all directory-summary.html
    on each project.
-->
<xsl:template match="testsuites" mode="all.projects">
    <html>
        <head>
            <title>All AntUnit Test Projects</title>
            <xsl:call-template name="create.stylesheet.link">
                <xsl:with-param name="directory.name"/>
            </xsl:call-template>
        </head>
        <body>
            <h2>Projects</h2>
            <table width="100%">
                <xsl:apply-templates select="testsuite" mode="all.projects">
                    <xsl:sort select="@name"/>
                </xsl:apply-templates>
            </table>
        </body>
    </html>
</xsl:template>

<xsl:template match="testsuite" mode="all.projects">
    <xsl:variable name="directory.name" select="@package"/>
    <tr>
        <td nowrap="nowrap">
            <a target="projectFrame">
                <xsl:attribute name="href">
                    <xsl:if test="not($directory.name='')">
                        <xsl:value-of select="translate($directory.name,'.','/')"/><xsl:text>/</xsl:text>
                    </xsl:if><xsl:value-of select="@id"/>_<xsl:value-of select="@name"/><xsl:text>.html</xsl:text>
                </xsl:attribute>
                <xsl:value-of select="@name"/>
            </a>
        </td>
    </tr>
</xsl:template>


<!--
    Creates an html file that contains a link to all directory-summary.html files on
    each directory existing on testsuites.
    @bug there will be a problem here, I don't know yet how to handle unnamed directory :(
-->
<xsl:template match="testsuites" mode="all.directories">
    <html>
        <head>
            <title>All AntUnit Test Directories</title>
            <xsl:call-template name="create.stylesheet.link">
                <xsl:with-param name="directory.name"/>
            </xsl:call-template>
        </head>
        <body>
            <h2><a href="overview-summary.html" target="projectFrame">Home</a></h2>
            <h2>Directories</h2>
            <table width="100%">
                <xsl:apply-templates select="testsuite[not(./@package = preceding-sibling::testsuite/@package)]" mode="all.directories">
                    <xsl:sort select="@package"/>
                </xsl:apply-templates>
            </table>
        </body>
    </html>
</xsl:template>

<xsl:template match="testsuite" mode="all.directories">
    <tr>
        <td nowrap="nowrap">
            <a href="./{translate(@package,'.','/')}/directory-summary.html" target="projectFrame">
                <xsl:value-of select="@package"/>
                <xsl:if test="@package = ''">&lt;none&gt;</xsl:if>
            </a>
        </td>
    </tr>
</xsl:template>


<xsl:template match="testsuites" mode="overview.directories">
    <html>
        <head>
            <title>AntUnit Test Results: Summary</title>
            <xsl:call-template name="create.stylesheet.link">
                <xsl:with-param name="directory.name"/>
            </xsl:call-template>
        </head>
        <body>
        <xsl:attribute name="onload">open('allprojects-frame.html','projectListFrame')</xsl:attribute>
        <xsl:call-template name="pageHeader"/>
        <h2>Summary</h2>
        <xsl:variable name="testCount" select="sum(testsuite/tests/text())"/>
        <xsl:variable name="errorCount" select="sum(testsuite/errors/text())"/>
        <xsl:variable name="failureCount" select="sum(testsuite/failures/text())"/>
        <xsl:variable name="timeCount" select="sum(testsuite/time/text())"/>
        <xsl:variable name="successRate" select="($testCount - $failureCount - $errorCount) div $testCount"/>
        <table class="details" border="0" cellpadding="5" cellspacing="2" width="95%">
        <tr valign="top">
            <th>Tests</th>
            <th>Failures</th>
            <th>Errors</th>
            <th>Success rate</th>
            <th>Time</th>
        </tr>
        <tr valign="top">
            <xsl:attribute name="class">
                <xsl:choose>
                    <xsl:when test="$errorCount &gt; 0">Error</xsl:when>
                    <xsl:when test="$failureCount &gt; 0">Failure</xsl:when>
                    <xsl:otherwise>Pass</xsl:otherwise>
                </xsl:choose>
            </xsl:attribute>
            <td><a title="Display all tests" href="all-tests.html"><xsl:value-of select="$testCount"/></a></td>
            <td><a title="Display all failures" href="alltests-fails.html"><xsl:value-of select="$failureCount"/></a></td>
            <td><a title="Display all errors" href="alltests-errors.html"><xsl:value-of select="$errorCount"/></a></td>
            <td>
                <xsl:call-template name="display-percent">
                    <xsl:with-param name="value" select="$successRate"/>
                </xsl:call-template>
            </td>
            <td>
                <xsl:call-template name="display-time">
                    <xsl:with-param name="value" select="$timeCount"/>
                </xsl:call-template>
            </td>
        </tr>
        </table>
        <table border="0" width="95%">
        <tr>
        <td style="text-align: justify;">
        Note: <em>failures</em> are anticipated and checked for with assertions while <em>errors</em> are unanticipated.
        </td>
        </tr>
        </table>

        <h2>Directories</h2>
        <table class="details" border="0" cellpadding="5" cellspacing="2" width="95%">
            <xsl:call-template name="testsuite.test.header"/>
            <xsl:for-each select="testsuite[not(./@package = preceding-sibling::testsuite/@package)]">
                <xsl:sort select="@package" order="ascending"/>
                <!-- get the node set containing all testsuites that have the same directory -->
                <xsl:variable name="insamedirectory" select="/testsuites/testsuite[./@package = current()/@package]"/>
                <tr valign="top">
                    <!-- display a failure if there is any failure/error in the directory -->
                    <xsl:attribute name="class">
                        <xsl:choose>
                            <xsl:when test="sum($insamedirectory/errors/text()) &gt; 0">Error</xsl:when>
                            <xsl:when test="sum($insamedirectory/failures/text()) &gt; 0">Failure</xsl:when>
                            <xsl:otherwise>Pass</xsl:otherwise>
                        </xsl:choose>
                    </xsl:attribute>
                    <td><a href="./{translate(@package,'.','/')}/directory-summary.html">
                        <xsl:value-of select="@package"/>
                        <xsl:if test="@package = ''">&lt;none&gt;</xsl:if>
                    </a></td>
                    <td><xsl:value-of select="sum($insamedirectory/tests/text())"/></td>
                    <td><xsl:value-of select="sum($insamedirectory/errors/text())"/></td>
                    <td><xsl:value-of select="sum($insamedirectory/failures/text())"/></td>
                    <td>
                    <xsl:call-template name="display-time">
                        <xsl:with-param name="value" select="sum($insamedirectory/time/text())"/>
                    </xsl:call-template>
                    </td>
                    <td><xsl:value-of select="$insamedirectory/@timestamp"/></td>
                    <td><xsl:value-of select="$insamedirectory/@hostname"/></td>
                </tr>
            </xsl:for-each>
        </table>
        </body>
        </html>
</xsl:template>


<xsl:template name="directory.summary">
    <xsl:param name="name"/>
    <html>
        <head>
            <xsl:call-template name="create.stylesheet.link">
                <xsl:with-param name="directory.name" select="$name"/>
            </xsl:call-template>
        </head>
        <body>
            <xsl:attribute name="onload">open('directory-frame.html','projectListFrame')</xsl:attribute>
            <xsl:call-template name="pageHeader"/>
            <h3>Directory <xsl:value-of select="$name"/></h3>

            <!--table border="0" cellpadding="5" cellspacing="2" width="95%">
                <xsl:call-template name="project.metrics.header"/>
                <xsl:apply-templates select="." mode="print.metrics"/>
            </table-->

            <xsl:variable name="insamedirectory" select="/testsuites/testsuite[./@package = $name]"/>
            <xsl:if test="count($insamedirectory) &gt; 0">
                <h2>Projects</h2>
                <p>
                <table class="details" border="0" cellpadding="5" cellspacing="2" width="95%">
                    <xsl:call-template name="testsuite.test.header"/>
                    <xsl:apply-templates select="$insamedirectory" mode="print.test">
                        <xsl:sort select="@name"/>
                    </xsl:apply-templates>
                </table>
                </p>
            </xsl:if>
        </body>
    </html>
</xsl:template>


<!--
    transform string like a.b.c to ../../../
    @param path the path to transform into a descending directory path
-->
<xsl:template name="path">
    <xsl:param name="path"/>
    <xsl:if test="contains($path,'.')">
        <xsl:text>../</xsl:text>
        <xsl:call-template name="path">
            <xsl:with-param name="path"><xsl:value-of select="substring-after($path,'.')"/></xsl:with-param>
        </xsl:call-template>
    </xsl:if>
    <xsl:if test="not(contains($path,'.')) and not($path = '')">
        <xsl:text>../</xsl:text>
    </xsl:if>
</xsl:template>


<!-- create the link to the stylesheet based on the directory name -->
<xsl:template name="create.stylesheet.link">
    <xsl:param name="directory.name"/>
    <link rel="stylesheet" type="text/css" title="Style"><xsl:attribute name="href"><xsl:if test="not($directory.name = 'unnamed directory')"><xsl:call-template name="path"><xsl:with-param name="path" select="$directory.name"/></xsl:call-template></xsl:if>stylesheet.css</xsl:attribute></link>
</xsl:template>


<!-- Page HEADER -->
<xsl:template name="pageHeader">
    <h1>AntUnit Test Results</h1>
    <table width="100%">
    <tr>
        <td align="left"></td>
        <td align="right">Designed for use with <a href="http://ant.apache.org/antlibs/antunit/">AntUnit</a> and <a href="http://ant.apache.org/">Ant</a>.</td>
    </tr>
    </table>
    <hr size="1"/>
</xsl:template>

<!-- project header -->
<xsl:template name="testsuite.test.header">
    <tr valign="top">
        <th width="80%">Name</th>
        <th>Tests</th>
        <th>Errors</th>
        <th>Failures</th>
        <th nowrap="nowrap">Time(s)</th>
        <th nowrap="nowrap">Time Stamp</th>
        <th>Host</th>
    </tr>
</xsl:template>

<!-- method header -->
<xsl:template name="testcase.test.header">
    <xsl:param name="show.project" select="''"/>
    <tr valign="top">
	<xsl:if test="boolean($show.project)">
	    <th>Project</th>
	</xsl:if>
        <th>Name</th>
        <th>Status</th>
        <th width="80%">Type</th>
        <th nowrap="nowrap">Time(s)</th>
    </tr>
</xsl:template>


<!-- project information -->
<xsl:template match="testsuite" mode="print.test">
    <tr valign="top">
        <xsl:attribute name="class">
            <xsl:choose>
                <xsl:when test="errors/text()[.&gt; 0]">Error</xsl:when>
                <xsl:when test="failures/text()[.&gt; 0]">Failure</xsl:when>
                <xsl:otherwise>Pass</xsl:otherwise>
            </xsl:choose>
        </xsl:attribute>
        <td><a title="Display all tests" href="{@id}_{@name}.html"><xsl:value-of select="@name"/></a></td>
        <td><a title="Display all tests" href="{@id}_{@name}.html"><xsl:apply-templates select="tests/text()"/></a></td>
        <td>
	    <xsl:choose>
		<xsl:when test="errors/text() != 0">
		    <a title="Display only errors" href="{@id}_{@name}-errors.html"><xsl:apply-templates select="errors/text()"/></a>
		</xsl:when>
		<xsl:otherwise>
		    <xsl:apply-templates select="errors/text()"/>
		</xsl:otherwise>
	    </xsl:choose>
	</td>
        <td>
	    <xsl:choose>
		<xsl:when test="failures/text() != 0">
		    <a title="Display only failures" href="{@id}_{@name}-fails.html"><xsl:apply-templates select="failures/text()"/></a>
		</xsl:when>
		<xsl:otherwise>
		    <xsl:apply-templates select="failures/text()"/>
		</xsl:otherwise>
	    </xsl:choose>
	</td>
        <td><xsl:call-template name="display-time">
                <xsl:with-param name="value" select="time/text()"/>
            </xsl:call-template>
        </td>
        <td><xsl:apply-templates select="@timestamp"/></td>
        <td><xsl:apply-templates select="@hostname"/></td>
    </tr>
</xsl:template>

<xsl:template match="testcase" mode="print.test">
    <xsl:param name="show.project" select="''"/>
    <tr valign="top">
        <xsl:attribute name="class">
            <xsl:choose>
                <xsl:when test="error">Error</xsl:when>
                <xsl:when test="failure">Failure</xsl:when>
                <xsl:otherwise>TableRowColor</xsl:otherwise>
            </xsl:choose>
        </xsl:attribute>
	<xsl:variable name="project.href">
	    <xsl:value-of select="concat(translate(../@package,'.','/'), '/', ../@id, '_', ../@name, '.html')"/>
	</xsl:variable>
	<xsl:if test="boolean($show.project)">
	    <td><a href="{$project.href}"><xsl:value-of select="../@name"/></a></td>
	</xsl:if>
        <td>
	    <a name="{@name}"/>
	    <xsl:choose>
		<xsl:when test="boolean($show.project)">
		    <a href="{concat($project.href, '#', @name)}"><xsl:value-of select="@name"/></a>
		</xsl:when>
		<xsl:otherwise>
		    <xsl:value-of select="@name"/>
		</xsl:otherwise>
	    </xsl:choose>
	</td>
        <xsl:choose>
            <xsl:when test="failure">
                <td>Failure</td>
                <td><xsl:apply-templates select="failure"/></td>
            </xsl:when>
            <xsl:when test="error">
                <td>Error</td>
                <td><xsl:apply-templates select="error"/></td>
            </xsl:when>
            <xsl:otherwise>
                <td>Success</td>
                <td></td>
            </xsl:otherwise>
        </xsl:choose>
        <td>
            <xsl:call-template name="display-time">
                <xsl:with-param name="value" select="time/text()"/>
            </xsl:call-template>
        </td>
    </tr>
</xsl:template>


<!-- Note : the below template error and failure are the same style
            so just call the same style store in the toolkit template -->
<xsl:template match="failure">
    <xsl:call-template name="display-failures"/>
</xsl:template>

<xsl:template match="error">
    <xsl:call-template name="display-failures"/>
    <!-- display the stacktrace -->
    <br/><br/>
    <code>
        <xsl:call-template name="br-replace">
            <xsl:with-param name="word" select="."/>
        </xsl:call-template>
    </code>
    <!-- the latter is better but might be problematic for non-21" monitors... -->
    <!--pre><xsl:value-of select="."/></pre-->
</xsl:template>

<!-- Style for the error and failure in the testcase template -->
<xsl:template name="display-failures">
    <xsl:choose>
        <xsl:when test="not(@message)">N/A</xsl:when>
        <xsl:otherwise>
            <xsl:value-of select="@message"/>
        </xsl:otherwise>
    </xsl:choose>
    <xsl:choose>
        <xsl:when test="@linenumber">
            <br></br>
            at line <xsl:value-of select="@linenumber"/>
            <xsl:choose>
                <xsl:when test="@columnnumber">
                    , column <xsl:value-of select="@columnnumber"/>
                </xsl:when>
            </xsl:choose>
        </xsl:when>
    </xsl:choose>
</xsl:template>

<xsl:template name="JS-escape">
    <xsl:param name="string"/>
    <xsl:param name="tmp1" select="stringutils:replace(string($string),'\','\\')"/>
    <xsl:param name="tmp2" select="stringutils:replace(string($tmp1),&quot;'&quot;,&quot;\&apos;&quot;)"/>
    <xsl:value-of select="$tmp2"/>
</xsl:template>


<!--
    template that will convert a carriage return into a br tag
    @param word the text from which to convert CR to BR tag
-->
<xsl:template name="br-replace">
    <xsl:param name="word"/>
    <xsl:value-of disable-output-escaping="yes" select='stringutils:replace(string($word),"&#xA;","&lt;br/>")'/>
</xsl:template>

<xsl:template name="display-time">
    <xsl:param name="value"/>
    <xsl:value-of select="format-number($value,'0.000')"/>
</xsl:template>

<xsl:template name="display-percent">
    <xsl:param name="value"/>
    <xsl:value-of select="format-number($value,'0.00%')"/>
</xsl:template>
</xsl:stylesheet>
