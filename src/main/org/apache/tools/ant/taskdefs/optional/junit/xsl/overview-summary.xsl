<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0" xmlns:html="http://www.w3.org/Profiles/XHTML-transitional">

<xsl:include href="toolkit.xsl"/>

<!--
	====================================================
		Create the page structure
    ====================================================
-->
<xsl:template match="testsuites">
	<HTML>
		<HEAD>
			<!--LINK REL ="stylesheet" TYPE="text/css" HREF="stylesheet.css" TITLE="Style"/-->
		<!-- put the style in the html so that we can mail it w/o problem -->
		<style type="text/css">
			BODY {
			font:normal 68% verdana,arial,helvetica;
			color:#000000;
			}
			TD {
			FONT-SIZE: 68%
			}
			P {
			line-height:1.5em;
			margin-top:0.5em; margin-bottom:1.0em;
			}
			H1 {
			MARGIN: 0px 0px 5px; FONT: 165% verdana,arial,helvetica
			}
			H2 {
			MARGIN-TOP: 1em; MARGIN-BOTTOM: 0.5em; FONT: bold 125% verdana,arial,helvetica
			}
			H3 {
			MARGIN-BOTTOM: 0.5em; FONT: bold 115% verdana,arial,helvetica
			}
			H4 {
			MARGIN-BOTTOM: 0.5em; FONT: bold 100% verdana,arial,helvetica
			}
			H5 {
			MARGIN-BOTTOM: 0.5em; FONT: bold 100% verdana,arial,helvetica
			}
			H6 {
			MARGIN-BOTTOM: 0.5em; FONT: bold 100% verdana,arial,helvetica
			}	
            .Error {
            	font-weight:bold; background:#EEEEE0; color:purple;
            }
            .Failure {
            	font-weight:bold; background:#EEEEE0; color:red;
            }
            .Pass {
            	background:#EEEEE0;
            }
			</style>			
		</HEAD>
		<body text="#000000" bgColor="#ffffff">
			<a name="#top"></a>
			<xsl:call-template name="header"/>
			
			<!-- Summary part -->
			<xsl:call-template name="summary"/>
			<hr size="1" width="95%" align="left"/>
			
			<!-- Package List part -->
			<xsl:call-template name="packagelist"/>
			<hr size="1" width="95%" align="left"/>
			
			<!-- For each package create its part -->
			<xsl:call-template name="packages"/>
			<hr size="1" width="95%" align="left"/>
			
			<!-- For each class create the  part -->
			<xsl:call-template name="classes"/>
			
		</body>
	</HTML>
</xsl:template>
	
	
	
	<!-- ================================================================== -->
	<!-- Write a list of all packages with an hyperlink to the anchor of    -->
	<!-- of the package name.                                               -->
	<!-- ================================================================== -->
	<xsl:template name="packagelist">	
		<h2>Packages</h2>
		Note: package statistics are not computed recursively, they only sum up all of its testsuites numbers.
		<table border="0" cellpadding="5" cellspacing="2" width="95%">
			<xsl:call-template name="packageSummaryHeader"/>
			<!-- list all packages recursively -->
			<xsl:for-each select="./testsuite[not(./@package = preceding-sibling::testsuite/@package)]">
				<xsl:sort select="@package"/>
				<xsl:variable name="testCount" select="sum(../testsuite[./@package = current()/@package]/@tests)"/>
				<xsl:variable name="errorCount" select="sum(../testsuite[./@package = current()/@package]/@errors)"/>
				<xsl:variable name="failureCount" select="sum(../testsuite[./@package = current()/@package]/@failures)"/>
				<xsl:variable name="timeCount" select="sum(../testsuite[./@package = current()/@package]/@time)"/>
				
				<!-- write a summary for the package -->
				<tr valign="top">
					<!-- set a nice color depending if there is an error/failure -->
					<xsl:attribute name="class">
						<xsl:choose>
						    <xsl:when test="$failureCount &gt; 0">Failure</xsl:when>
							<xsl:when test="$errorCount &gt; 0">Error</xsl:when>
							<xsl:otherwise>Pass</xsl:otherwise>
						</xsl:choose>
					</xsl:attribute>				
					<td><a href="#{@package}"><xsl:value-of select="@package"/></a></td>
					<td><xsl:value-of select="$testCount"/></td>
					<td><xsl:value-of select="$errorCount"/></td>
					<td><xsl:value-of select="$failureCount"/></td>
					<td>
                        <xsl:call-template name="display-time">
                        	<xsl:with-param name="value" select="$timeCount"/>
                        </xsl:call-template>					
					</td>					
				</tr>
			</xsl:for-each>
		</table>		
	</xsl:template>
	
	
	<!-- ================================================================== -->
	<!-- Write a package level report                                       -->
	<!-- It creates a table with values from the document:                  -->
	<!-- Name | Tests | Errors | Failures | Time                            -->
	<!-- ================================================================== -->
	<xsl:template name="packages">
		<!-- create an anchor to this package name -->
		<xsl:for-each select="./testsuite[not(./@package = preceding-sibling::testsuite/@package)]">
			<xsl:sort select="@package"/>
				<a name="#{@package}"></a>
				<h3>Package <xsl:value-of select="@package"/></h3>
				
				<table border="0" cellpadding="5" cellspacing="2" width="95%">
					<xsl:call-template name="packageSummaryHeader"/>
			
					<!-- match the testsuites of this package -->
					<xsl:apply-templates select="../testsuite[./@package = current()/@package]"/>					
				</table>
				<a href="#top">Back to top</a>
				<p/>
				<p/>
		</xsl:for-each>
	</xsl:template>

	<!-- ================================================================== -->
	<!-- Process a testsuite node                                           -->
	<!-- It creates a table with values from the document:                  -->
	<!-- Name | Tests | Errors | Failures | Time                            -->
	<!-- It must match the table definition at the package level            -->
	<!-- ================================================================== -->	
	<xsl:template match="testsuite">
		<tr valign="top">
			<!-- set a nice color depending if there is an error/failure -->
			<xsl:attribute name="class">
				<xsl:choose>
				    <xsl:when test="@failures[.&gt; 0]">Failure</xsl:when>
					<xsl:when test="@errors[.&gt; 0]">Error</xsl:when>
					<xsl:otherwise>Pass</xsl:otherwise>
				</xsl:choose>
			</xsl:attribute>
		
			<!-- print testsuite information -->
			<td><a href="#{@name}"><xsl:value-of select="@name"/></a></td>
			<td><xsl:value-of select="@tests"/></td>
			<td><xsl:value-of select="@errors"/></td>
			<td><xsl:value-of select="@failures"/></td>
			<td>
                <xsl:call-template name="display-time">
                	<xsl:with-param name="value" select="@time"/>
                </xsl:call-template>
			</td>
		</tr>
	</xsl:template>
	
	<xsl:template name="classes">
		<xsl:for-each select="./testsuite">
			<xsl:sort select="@name"/>
			<!-- create an anchor to this class name -->
			<a name="#{@name}"></a>
			<h3>TestCase <xsl:value-of select="@name"/></h3>
			
			<table border="0" cellpadding="5" cellspacing="2" width="95%">
				<!-- Header -->
				<xsl:call-template name="classesSummaryHeader"/>

				<!-- match the testcases of this package -->
				<xsl:apply-templates select="testcase"/>
			</table>
			<a href="#top">Back to top</a>
		</xsl:for-each>
	</xsl:template>
	
</xsl:stylesheet>
