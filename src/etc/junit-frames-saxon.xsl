<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    exclude-result-prefixes="xs"
    version="2.0">
    <xsl:character-map name="no-control-characters">
        <xsl:output-character character="&#127;" string="&amp;#127;"/>
        <xsl:output-character character="&#128;" string="&amp;#128;"/>
        <xsl:output-character character="&#129;" string="&amp;#129;"/>
        <xsl:output-character character="&#130;" string="&amp;#130;"/>
        <xsl:output-character character="&#131;" string="&amp;#131;"/>
        <xsl:output-character character="&#132;" string="&amp;#132;"/>
        <xsl:output-character character="&#133;" string="&amp;#133;"/>
        <xsl:output-character character="&#134;" string="&amp;#134;"/>
        <xsl:output-character character="&#135;" string="&amp;#135;"/>
        <xsl:output-character character="&#136;" string="&amp;#136;"/>
        <xsl:output-character character="&#137;" string="&amp;#137;"/>
        <xsl:output-character character="&#138;" string="&amp;#138;"/>
        <xsl:output-character character="&#139;" string="&amp;#139;"/>
        <xsl:output-character character="&#140;" string="&amp;#140;"/>
        <xsl:output-character character="&#141;" string="&amp;#141;"/>
        <xsl:output-character character="&#142;" string="&amp;#142;"/>
        <xsl:output-character character="&#143;" string="&amp;#143;"/>
        <xsl:output-character character="&#144;" string="&amp;#144;"/>
        <xsl:output-character character="&#145;" string="&amp;#145;"/>
        <xsl:output-character character="&#146;" string="&amp;#146;"/>
        <xsl:output-character character="&#147;" string="&amp;#147;"/>
        <xsl:output-character character="&#148;" string="&amp;#148;"/>
        <xsl:output-character character="&#149;" string="&amp;#149;"/>
        <xsl:output-character character="&#150;" string="&amp;#150;"/>
        <xsl:output-character character="&#151;" string="&amp;#151;"/>
        <xsl:output-character character="&#152;" string="&amp;#152;"/>
        <xsl:output-character character="&#153;" string="&amp;#153;"/>
        <xsl:output-character character="&#154;" string="&amp;#154;"/>
        <xsl:output-character character="&#155;" string="&amp;#155;"/>
        <xsl:output-character character="&#156;" string="&amp;#156;"/>
        <xsl:output-character character="&#157;" string="&amp;#157;"/>
        <xsl:output-character character="&#158;" string="&amp;#158;"/>
        <xsl:output-character character="&#159;" string="&amp;#159;"/>
    </xsl:character-map>
    <xsl:output method="html" indent="yes" encoding="UTF-8" use-character-maps="no-control-characters"/>
    <xsl:decimal-format decimal-separator="." grouping-separator=","/>
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

     Sample stylesheet to be used with Ant JUnitReport output.

     It creates a set of HTML files a la javadoc where you can browse easily
     through all packages and classes.

    -->
    <xsl:param name="output.dir" select="'.'"/>
    <xsl:param name="TITLE">Unit Test Results</xsl:param>


    <xsl:template match="testsuites">
        <!-- create the index.html -->
        <xsl:result-document href="file:///{$output.dir}/index.html">
            <xsl:call-template name="index.html"/>
        </xsl:result-document>

        <!-- create the stylesheet.css -->
        <xsl:result-document href="file:///{$output.dir}/stylesheet.css">
            <xsl:call-template name="stylesheet.css"/>
        </xsl:result-document>

        <!-- create the overview-packages.html at the root -->
        <xsl:result-document href="file:///{$output.dir}/overview-summary.html">
            <xsl:apply-templates select="." mode="overview.packages"/>
        </xsl:result-document>

        <!-- create the all-packages.html at the root -->
        <xsl:result-document href="file:///{$output.dir}/overview-frame.html">
            <xsl:apply-templates select="." mode="all.packages"/>
        </xsl:result-document>

        <!-- create the all-classes.html at the root -->
        <xsl:result-document href="file:///{$output.dir}/allclasses-frame.html">
            <xsl:apply-templates select="." mode="all.classes"/>
        </xsl:result-document>

        <!-- create the all-tests.html at the root -->
        <xsl:result-document href="file:///{$output.dir}/all-tests.html">
            <xsl:apply-templates select="." mode="all.tests"/>
        </xsl:result-document>

        <!-- create the alltests-fails.html at the root -->
        <xsl:result-document href="file:///{$output.dir}/alltests-fails.html">
            <xsl:apply-templates select="." mode="all.tests">
                <xsl:with-param name="type" select="'fails'"/>
            </xsl:apply-templates>
        </xsl:result-document>

        <!-- create the alltests-errors.html at the root -->
        <xsl:result-document href="file:///{$output.dir}/alltests-errors.html">
            <xsl:apply-templates select="." mode="all.tests">
                <xsl:with-param name="type" select="'errors'"/>
            </xsl:apply-templates>
        </xsl:result-document>

        <!-- create the alltests-skipped.html at the root -->
        <xsl:result-document href="file:///{$output.dir}/alltests-skipped.html">
            <xsl:apply-templates select="." mode="all.tests">
                <xsl:with-param name="type" select="'skipped'"/>
            </xsl:apply-templates>
        </xsl:result-document>

        <!-- process all packages -->
        <xsl:for-each select="./testsuite[not(./@package = preceding-sibling::testsuite/@package)]">
            <xsl:call-template name="package">
                <xsl:with-param name="name" select="@package"/>
            </xsl:call-template>
        </xsl:for-each>
    </xsl:template>


    <xsl:template name="package">
        <xsl:param name="name"/>
        <xsl:variable name="package.dir">
            <xsl:if test="not($name = '')"><xsl:value-of select="translate($name,'.','/')"/></xsl:if>
            <xsl:if test="$name = ''">.</xsl:if>
        </xsl:variable>
        <!--Processing package <xsl:value-of select="@name"/> in <xsl:value-of select="$output.dir"/> -->
        <!-- create a classes-list.html in the package directory -->
        <xsl:result-document href="file:///{$output.dir}/{$package.dir}/package-frame.html">
            <xsl:call-template name="classes.list">
                <xsl:with-param name="name" select="$name"/>
            </xsl:call-template>
        </xsl:result-document>

        <!-- create a package-summary.html in the package directory -->
        <xsl:result-document href="file:///{$output.dir}/{$package.dir}/package-summary.html">
            <xsl:call-template name="package.summary">
                <xsl:with-param name="name" select="$name"/>
            </xsl:call-template>
        </xsl:result-document>

        <!-- for each class, creates a @name.html -->
        <!-- @bug there will be a problem with inner classes having the same name, it will be overwritten -->
        <xsl:for-each select="/testsuites/testsuite[@package = $name]">
            <xsl:result-document href="file:///{$output.dir}/{$package.dir}/{@id}_{@name}.html">
                <xsl:apply-templates select="." mode="class.details"/>
            </xsl:result-document>
            <xsl:if test="string-length(./system-out)!=0">
                <xsl:result-document href="file:///{$output.dir}/{$package.dir}/{@id}_{@name}-out.html">
                    <html>
                        <head>
                            <title>Standard Output from <xsl:value-of select="@name"/></title>
                        </head>
                        <body>
                            <pre><xsl:value-of select="./system-out"/></pre>
                        </body>
                    </html>
                </xsl:result-document>
            </xsl:if>
            <xsl:if test="string-length(./system-err)!=0">
                <xsl:result-document href="file:///{$output.dir}/{$package.dir}/{@id}_{@name}-err.html">
                    <html>
                        <head>
                            <title>Standard Error from <xsl:value-of select="@name"/></title>
                        </head>
                        <body>
                            <pre><xsl:value-of select="./system-err"/></pre>
                        </body>
                    </html>
                </xsl:result-document>
            </xsl:if>
            <xsl:if test="@failures != 0">
                <xsl:result-document href="file:///{$output.dir}/{$package.dir}/{@id}_{@name}-fails.html">
                    <xsl:apply-templates select="." mode="class.details">
                        <xsl:with-param name="type" select="'fails'"/>
                    </xsl:apply-templates>
                </xsl:result-document>
            </xsl:if>
            <xsl:if test="@errors != 0">
                <xsl:result-document href="file:///{$output.dir}/{$package.dir}/{@id}_{@name}-errors.html">
                    <xsl:apply-templates select="." mode="class.details">
                        <xsl:with-param name="type" select="'errors'"/>
                    </xsl:apply-templates>
                </xsl:result-document>
            </xsl:if>
            <xsl:if test="@skipped != 0">
                <xsl:result-document href="file:///{$output.dir}/{$package.dir}/{@id}_{@name}-skipped.html">
                    <xsl:apply-templates select="." mode="class.details">
                        <xsl:with-param name="type" select="'skipped'"/>
                    </xsl:apply-templates>
                </xsl:result-document>
            </xsl:if>
        </xsl:for-each>
    </xsl:template>

    <xsl:template name="index.html">
        <html>
            <head>
                <title><xsl:value-of select="$TITLE"/></title>
            </head>
            <frameset cols="20%,80%">
                <frameset rows="30%,70%">
                    <frame src="overview-frame.html" name="packageListFrame"/>
                    <frame src="allclasses-frame.html" name="classListFrame"/>
                </frameset>
                <frame src="overview-summary.html" name="classFrame"/>
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

    <!-- Create list of all/failed/errored/skipped tests -->
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
                    <xsl:when test="$type = 'skipped'">
                        <xsl:text>All Skipped</xsl:text>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:text>All Tests</xsl:text>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:variable>
            <head>
                <title><xsl:value-of select="$TITLE"/>: <xsl:value-of select="$title"/></title>
                <xsl:call-template name="create.stylesheet.link">
                    <xsl:with-param name="package.name"/>
                </xsl:call-template>
            </head>
            <body>
                <xsl:attribute name="onload">open('allclasses-frame.html','classListFrame')</xsl:attribute>
                <xsl:call-template name="pageHeader"/>
                <h2><xsl:value-of select="$title"/></h2>

                <table class="details" border="0" cellpadding="5" cellspacing="2" width="95%">
                    <xsl:call-template name="testcase.test.header">
                        <xsl:with-param name="show.class" select="'yes'"/>
                    </xsl:call-template>
                    <!--
                            test can even not be started at all (failure to load the class)
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
                                <xsl:with-param name="show.class" select="'yes'"/>
                            </xsl:apply-templates>
                        </xsl:when>
                        <xsl:when test="$type = 'errors'">
                            <xsl:apply-templates select=".//testsuite[error]" mode="alltests.error.row"/>
                            <xsl:apply-templates select=".//testcase[error]" mode="print.test">
                                <xsl:with-param name="show.class" select="'yes'"/>
                            </xsl:apply-templates>
                        </xsl:when>
                        <xsl:when test="$type = 'skipped'">
                            <xsl:apply-templates select=".//testcase[skipped]" mode="print.test">
                                <xsl:with-param name="show.class" select="'yes'"/>
                            </xsl:apply-templates>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:apply-templates select=".//testsuite[error]" mode="alltests.error.row"/>
                            <xsl:apply-templates select=".//testcase" mode="print.test">
                                <xsl:with-param name="show.class" select="'yes'"/>
                            </xsl:apply-templates>
                        </xsl:otherwise>
                    </xsl:choose>
                </table>
            </body>
        </html>
    </xsl:template>


    <!-- ======================================================================
        This page is created for every testsuite class.
        It prints a summary of the testsuite and detailed information about
        testcase methods.
         ====================================================================== -->
    <xsl:template match="testsuite" mode="class.details">
        <xsl:param name="type" select="'all'"/>
        <xsl:variable name="package.name" select="@package"/>
        <xsl:variable name="class.name"><xsl:if test="not($package.name = '')"><xsl:value-of select="$package.name"/>.</xsl:if><xsl:value-of select="@name"/></xsl:variable>
        <html>
            <head>
                <title><xsl:value-of select="$TITLE"/>: <xsl:value-of select="$class.name"/></title>
                <xsl:call-template name="create.stylesheet.link">
                    <xsl:with-param name="package.name" select="$package.name"/>
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
                <h3>Class <xsl:value-of select="$class.name"/></h3>


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
                    <xsl:when test="$type = 'skipped'">
                        <h2>Skipped</h2>
                    </xsl:when>
                    <xsl:otherwise>
                        <h2>Tests</h2>
                    </xsl:otherwise>
                </xsl:choose>
                <table class="details" border="0" cellpadding="5" cellspacing="2" width="95%">
                    <xsl:call-template name="testcase.test.header"/>
                    <!--
                            test can even not be started at all (failure to load the class)
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
                        <xsl:when test="$type = 'skipped'">
                            <xsl:apply-templates select="./testcase[skipped]" mode="print.test"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:apply-templates select="./testcase" mode="print.test"/>
                        </xsl:otherwise>
                    </xsl:choose>
                </table>
                <div class="Properties">
                    <a>
                        <xsl:attribute name="href">javascript:displayProperties('<xsl:value-of select="@package"/>.<xsl:value-of select="@name"/>');</xsl:attribute>
                        Properties &#187;
                    </a>
                </div>
                <xsl:if test="string-length(./system-out)!=0">
                    <div class="Properties">
                        <a>
                            <xsl:attribute name="href">./<xsl:value-of select="@id"/>_<xsl:value-of select="@name"/>-out.html</xsl:attribute>
                            System.out &#187;
                        </a>
                    </div>
                </xsl:if>
                <xsl:if test="string-length(./system-err)!=0">
                    <div class="Properties">
                        <a>
                            <xsl:attribute name="href">./<xsl:value-of select="@id"/>_<xsl:value-of select="@name"/>-err.html</xsl:attribute>
                            System.err &#187;
                        </a>
                    </div>
                </xsl:if>
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
        This page is created for every package.
        It prints the name of all classes that belongs to this package.
        @param name the package name to print classes.
         ====================================================================== -->
    <!-- list of classes in a package -->
    <xsl:template name="classes.list">
        <xsl:param name="name"/>
        <html>
            <head>
                <title>Unit Test Classes: <xsl:value-of select="$name"/></title>
                <xsl:call-template name="create.stylesheet.link">
                    <xsl:with-param name="package.name" select="$name"/>
                </xsl:call-template>
            </head>
            <body>
                <table width="100%">
                    <tr>
                        <td nowrap="nowrap">
                            <h2><a href="package-summary.html" target="classFrame">
                                <xsl:value-of select="$name"/>
                                <xsl:if test="$name = ''">&lt;none&gt;</xsl:if>
                            </a></h2>
                        </td>
                    </tr>
                </table>

                <h2>Classes</h2>
                <table width="100%">
                    <xsl:for-each select="/testsuites/testsuite[./@package = $name]">
                        <xsl:sort select="@name"/>
                        <tr>
                            <td nowrap="nowrap">
                                <a href="{@id}_{@name}.html" target="classFrame"><xsl:value-of select="@name"/></a>
                            </td>
                        </tr>
                    </xsl:for-each>
                </table>
            </body>
        </html>
    </xsl:template>


    <!--
        Creates an all-classes.html file that contains a link to all package-summary.html
        on each class.
    -->
    <xsl:template match="testsuites" mode="all.classes">
        <html>
            <head>
                <title>All Unit Test Classes</title>
                <xsl:call-template name="create.stylesheet.link">
                    <xsl:with-param name="package.name"/>
                </xsl:call-template>
            </head>
            <body>
                <h2>Classes</h2>
                <table width="100%">
                    <xsl:apply-templates select="testsuite" mode="all.classes">
                        <xsl:sort select="@name"/>
                    </xsl:apply-templates>
                </table>
            </body>
        </html>
    </xsl:template>

    <xsl:template match="testsuite" mode="all.classes">
        <xsl:variable name="package.name" select="@package"/>
        <tr>
            <td nowrap="nowrap">
                <a target="classFrame">
                    <xsl:attribute name="href">
                        <xsl:if test="not($package.name='')">
                            <xsl:value-of select="translate($package.name,'.','/')"/><xsl:text>/</xsl:text>
                        </xsl:if><xsl:value-of select="@id"/>_<xsl:value-of select="@name"/><xsl:text>.html</xsl:text>
                    </xsl:attribute>
                    <xsl:value-of select="@name"/>
                </a>
            </td>
        </tr>
    </xsl:template>


    <!--
        Creates an html file that contains a link to all package-summary.html files on
        each package existing on testsuites.
        @bug there will be a problem here, I don't know yet how to handle unnamed package :(
    -->
    <xsl:template match="testsuites" mode="all.packages">
        <html>
            <head>
                <title>All Unit Test Packages</title>
                <xsl:call-template name="create.stylesheet.link">
                    <xsl:with-param name="package.name"/>
                </xsl:call-template>
            </head>
            <body>
                <h2><a href="overview-summary.html" target="classFrame">Home</a></h2>
                <h2>Packages</h2>
                <table width="100%">
                    <xsl:apply-templates select="testsuite[not(./@package = preceding-sibling::testsuite/@package)]" mode="all.packages">
                        <xsl:sort select="@package"/>
                    </xsl:apply-templates>
                </table>
            </body>
        </html>
    </xsl:template>

    <xsl:template match="testsuite" mode="all.packages">
        <tr>
            <td nowrap="nowrap">
                <a href="./{translate(@package,'.','/')}/package-summary.html" target="classFrame">
                    <xsl:value-of select="@package"/>
                    <xsl:if test="@package = ''">&lt;none&gt;</xsl:if>
                </a>
            </td>
        </tr>
    </xsl:template>


    <xsl:template match="testsuites" mode="overview.packages">
        <html>
            <head>
                <title><xsl:value-of select="$TITLE"/>: Summary</title>
                <xsl:call-template name="create.stylesheet.link">
                    <xsl:with-param name="package.name"/>
                </xsl:call-template>
            </head>
            <body>
                <xsl:attribute name="onload">open('allclasses-frame.html','classListFrame')</xsl:attribute>
                <xsl:call-template name="pageHeader"/>
                <h2>Summary</h2>
                <xsl:variable name="testCount" select="sum(testsuite/@tests)"/>
                <xsl:variable name="errorCount" select="sum(testsuite/@errors)"/>
                <xsl:variable name="failureCount" select="sum(testsuite/@failures)"/>
                <xsl:variable name="skippedCount" select="sum(testsuite/@skipped)" />
                <xsl:variable name="timeCount" select="sum(testsuite/@time)"/>
                <xsl:variable name="successRate" select="($testCount - $failureCount - $errorCount) div $testCount"/>
                <table class="details" border="0" cellpadding="5" cellspacing="2" width="95%">
                    <tr valign="top">
                        <th>Tests</th>
                        <th>Failures</th>
                        <th>Errors</th>
                        <th>Skipped</th>
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
                        <td><a title="Display all skipped test" href="alltests-skipped.html"><xsl:value-of select="$skippedCount" /></a></td>
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

                <h2>Packages</h2>
                <table class="details" border="0" cellpadding="5" cellspacing="2" width="95%">
                    <xsl:call-template name="testsuite.test.header"/>
                    <xsl:for-each select="testsuite[not(./@package = preceding-sibling::testsuite/@package)]">
                        <xsl:sort select="@package" order="ascending"/>
                        <!-- get the node set containing all testsuites that have the same package -->
                        <xsl:variable name="insamepackage" select="/testsuites/testsuite[./@package = current()/@package]"/>
                        <tr valign="top">
                            <!-- display a failure if there is any failure/error in the package -->
                            <xsl:attribute name="class">
                                <xsl:choose>
                                    <xsl:when test="sum($insamepackage/@errors) &gt; 0">Error</xsl:when>
                                    <xsl:when test="sum($insamepackage/@failures) &gt; 0">Failure</xsl:when>
                                    <xsl:otherwise>Pass</xsl:otherwise>
                                </xsl:choose>
                            </xsl:attribute>
                            <td><a href="./{translate(@package,'.','/')}/package-summary.html">
                                <xsl:value-of select="@package"/>
                                <xsl:if test="@package = ''">&lt;none&gt;</xsl:if>
                            </a></td>
                            <td><xsl:value-of select="sum($insamepackage/@tests)"/></td>
                            <td><xsl:value-of select="sum($insamepackage/@errors)"/></td>
                            <td><xsl:value-of select="sum($insamepackage/@failures)"/></td>
                            <td><xsl:value-of select="sum($insamepackage/@skipped)" /></td>
                            <td>
                                <xsl:call-template name="display-time">
                                    <xsl:with-param name="value" select="sum($insamepackage/@time)"/>
                                </xsl:call-template>
                            </td>
                            <td><xsl:value-of select="$insamepackage/@timestamp"/></td>
                            <td><xsl:value-of select="$insamepackage/@hostname"/></td>
                        </tr>
                    </xsl:for-each>
                </table>
            </body>
        </html>
    </xsl:template>


    <xsl:template name="package.summary">
        <xsl:param name="name"/>
        <html>
            <head>
                <xsl:call-template name="create.stylesheet.link">
                    <xsl:with-param name="package.name" select="$name"/>
                </xsl:call-template>
            </head>
            <body>
                <xsl:attribute name="onload">open('package-frame.html','classListFrame')</xsl:attribute>
                <xsl:call-template name="pageHeader"/>
                <h3>Package <xsl:value-of select="$name"/></h3>

                <!--table border="0" cellpadding="5" cellspacing="2" width="95%">
                    <xsl:call-template name="class.metrics.header"/>
                    <xsl:apply-templates select="." mode="print.metrics"/>
                </table-->

                <xsl:variable name="insamepackage" select="/testsuites/testsuite[./@package = $name]"/>
                <xsl:if test="count($insamepackage) &gt; 0">
                    <h2>Classes</h2>
                    <p>
                        <table class="details" border="0" cellpadding="5" cellspacing="2" width="95%">
                            <xsl:call-template name="testsuite.test.header"/>
                            <xsl:apply-templates select="$insamepackage" mode="print.test">
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


    <!-- create the link to the stylesheet based on the package name -->
    <xsl:template name="create.stylesheet.link">
        <xsl:param name="package.name"/>
        <link rel="stylesheet" type="text/css" title="Style"><xsl:attribute name="href"><xsl:if test="not($package.name = 'unnamed package')"><xsl:call-template name="path"><xsl:with-param name="path" select="$package.name"/></xsl:call-template></xsl:if>stylesheet.css</xsl:attribute></link>
    </xsl:template>


    <!-- Page HEADER -->
    <xsl:template name="pageHeader">
        <h1><xsl:value-of select="$TITLE"/></h1>
        <table width="100%">
            <tr>
                <td align="left"></td>
                <td align="right">Designed for use with <a href="https://www.junit.org/">JUnit</a> and <a href="https://ant.apache.org/">Ant</a>.</td>
            </tr>
        </table>
        <hr size="1"/>
    </xsl:template>

    <!-- class header -->
    <xsl:template name="testsuite.test.header">
        <tr valign="top">
            <th width="80%">Name</th>
            <th>Tests</th>
            <th>Errors</th>
            <th>Failures</th>
            <th>Skipped</th>
            <th nowrap="nowrap">Time(s)</th>
            <th nowrap="nowrap">Time Stamp</th>
            <th>Host</th>
        </tr>
    </xsl:template>

    <!-- method header -->
    <xsl:template name="testcase.test.header">
        <xsl:param name="show.class" select="''"/>
        <tr valign="top">
            <xsl:if test="boolean($show.class)">
                <th>Class</th>
            </xsl:if>
            <th>Name</th>
            <th>Status</th>
            <th width="80%">Type</th>
            <th nowrap="nowrap">Time(s)</th>
        </tr>
    </xsl:template>


    <!-- class information -->
    <xsl:template match="testsuite" mode="print.test">
        <tr valign="top">
            <xsl:attribute name="class">
                <xsl:choose>
                    <xsl:when test="@errors[.&gt; 0]">Error</xsl:when>
                    <xsl:when test="@failures[.&gt; 0]">Failure</xsl:when>
                    <xsl:otherwise>Pass</xsl:otherwise>
                </xsl:choose>
            </xsl:attribute>
            <td><a title="Display all tests" href="{@id}_{@name}.html"><xsl:value-of select="@name"/></a></td>
            <td><a title="Display all tests" href="{@id}_{@name}.html"><xsl:apply-templates select="@tests"/></a></td>
            <td>
                <xsl:choose>
                    <xsl:when test="@errors != 0">
                        <a title="Display only errors" href="{@id}_{@name}-errors.html"><xsl:apply-templates select="@errors"/></a>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:apply-templates select="@errors"/>
                    </xsl:otherwise>
                </xsl:choose>
            </td>
            <td>
                <xsl:choose>
                    <xsl:when test="@failures != 0">
                        <a title="Display only failures" href="{@id}_{@name}-fails.html"><xsl:apply-templates select="@failures"/></a>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:apply-templates select="@failures"/>
                    </xsl:otherwise>
                </xsl:choose>
            </td>
            <td>
                <xsl:choose>
                    <xsl:when test="@skipped != 0">
                        <a title="Display only skipped tests" href="{@id}_{@name}-skipped.html"><xsl:apply-templates select="@skipped"/></a>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:apply-templates select="@skipped"/>
                    </xsl:otherwise>
                </xsl:choose>
            </td>
            <td><xsl:call-template name="display-time">
                <xsl:with-param name="value" select="@time"/>
            </xsl:call-template>
            </td>
            <td><xsl:apply-templates select="@timestamp"/></td>
            <td><xsl:apply-templates select="@hostname"/></td>
        </tr>
    </xsl:template>

    <xsl:template match="testcase" mode="print.test">
        <xsl:param name="show.class" select="''"/>
        <tr valign="top">
            <xsl:attribute name="class">
                <xsl:choose>
                    <xsl:when test="error">Error</xsl:when>
                    <xsl:when test="failure">Failure</xsl:when>
                    <xsl:otherwise>TableRowColor</xsl:otherwise>
                </xsl:choose>
            </xsl:attribute>
            <xsl:variable name="class.href">
                <xsl:value-of select="concat(translate(../@package,'.','/'), '/', ../@id, '_', ../@name, '.html')"/>
            </xsl:variable>
            <xsl:if test="boolean($show.class)">
                <td><a href="{$class.href}"><xsl:value-of select="../@name"/></a></td>
            </xsl:if>
            <td>
                <a name="{@name}"/>
                <xsl:choose>
                    <xsl:when test="boolean($show.class)">
                        <a href="{concat($class.href, '#', @name)}"><xsl:value-of select="@name"/></a>
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
                <xsl:when test="skipped">
                    <td>Skipped</td>
                    <td><xsl:apply-templates select="skipped"/></td>
                </xsl:when>
                <xsl:otherwise>
                    <td>Success</td>
                    <td></td>
                </xsl:otherwise>
            </xsl:choose>
            <td>
                <xsl:call-template name="display-time">
                    <xsl:with-param name="value" select="@time"/>
                </xsl:call-template>
            </td>
        </tr>
    </xsl:template>


    <!-- Note : the below template skipped, error and failure are the same style
                so just call the same style store in the toolkit template -->
    <xsl:template match="failure">
        <xsl:call-template name="display-failures"/>
    </xsl:template>

    <xsl:template match="error">
        <xsl:call-template name="display-failures"/>
    </xsl:template>

    <xsl:template match="skipped">
        <xsl:call-template name="display-failures"/>
    </xsl:template>

    <!-- Style for the error and failure in the testcase template -->
    <xsl:template name="display-failures">
        <xsl:choose>
            <xsl:when test="not(@message)">N/A</xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="@message"/>
            </xsl:otherwise>
        </xsl:choose>
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

    <xsl:template name="JS-escape">
        <xsl:param name="string"/>
        <xsl:variable name="tmp1" select="replace($string,'\\','\\\\')"/>
        <xsl:variable name="tmp2" select="replace($tmp1,&quot;'&quot;,&quot;\\&apos;&quot;)"/>
        <xsl:variable name="tmp3" select="replace($tmp2,&quot;&#10;&quot;,'\\n')"/>
        <xsl:variable name="tmp4" select="replace($tmp3,&quot;&#13;&quot;,'\\r')"/>
        <xsl:value-of select="$tmp4"/>
    </xsl:template>


    <!--
        template that will convert a carriage return into a br tag
        @param word the text from which to convert CR to BR tag
    -->
    <xsl:template name="br-replace">
        <xsl:param name="word"/>
        <xsl:param name="splitlimit">32</xsl:param>
        <xsl:variable name="secondhalfstartindex" select="(string-length($word)+(string-length($word) mod 2)) div 2"/>
        <xsl:variable name="secondhalfword" select="substring($word, $secondhalfstartindex)"/>
        <!-- When word is very big, a recursive replace is very heap/stack expensive, so subdivide on line break after middle of string -->
        <xsl:choose>
            <xsl:when test="(string-length($word) > $splitlimit) and (contains($secondhalfword, '&#xa;'))">
                <xsl:variable name="secondhalfend" select="substring-after($secondhalfword, '&#xa;')"/>
                <xsl:variable name="firsthalflen" select="string-length($word) - string-length($secondhalfword)"/>
                <xsl:variable name="firsthalfword" select="substring($word, 1, $firsthalflen)"/>
                <xsl:variable name="firsthalfend" select="substring-before($secondhalfword, '&#xa;')"/>
                <xsl:call-template name="br-replace">
                    <xsl:with-param name="word" select="concat($firsthalfword,$firsthalfend)"/>
                </xsl:call-template>
                <br/>
                <xsl:call-template name="br-replace">
                    <xsl:with-param name="word" select="$secondhalfend"/>
                </xsl:call-template>
            </xsl:when>
            <xsl:when test="contains($word, '&#xa;')">
                <xsl:value-of select="substring-before($word, '&#xa;')"/>
                <br/>
                <xsl:call-template name="br-replace">
                    <xsl:with-param name="word" select="substring-after($word, '&#xa;')"/>
                </xsl:call-template>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$word"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="display-time">
        <xsl:param name="value"/>
        <xsl:value-of select="format-number($value,'0.000')"/>
    </xsl:template>

    <xsl:template name="display-percent">
        <xsl:param name="value"/>
        <xsl:value-of select="format-number($value,'0.00%')"/>
    </xsl:template>

    <xsl:template match="testsuite" mode="alltests.error.row">
      <xsl:variable name="package.dir">
        <xsl:if test="not(@package = '')"><xsl:value-of select="translate(@package,'.','/')"/><xsl:text>/</xsl:text></xsl:if>
      </xsl:variable>
      <xsl:variable name="class.href">
        <xsl:value-of select="concat($package.dir, @id, '_', @name, '.html')"/>
      </xsl:variable>
      <tr class="Error">
        <td><a href="{$class.href}"><xsl:value-of select="@name"/></a></td>
        <td colspan="3"><xsl:apply-templates select="./error"/></td>
      </tr>
    </xsl:template>

</xsl:stylesheet>
