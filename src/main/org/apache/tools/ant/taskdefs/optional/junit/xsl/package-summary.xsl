<?xml version="1.0" encoding="ISO-8859-1"?>

<!-- This style sheet should contain just a named templates that used in the other specific templates -->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<!-- import the commun templates -->
<xsl:include href="toolkit.xsl"/>

<xsl:template match="testsuites">
	<HTML>
		<HEAD>
			<LINK REL ="stylesheet" TYPE="text/css" TITLE="Style">
				<xsl:attribute name="href"><xsl:call-template name="path"><xsl:with-param name="path" select="testsuite[position() = 1]/@package"/></xsl:call-template>stylesheet.css</xsl:attribute>
			</LINK>
		</HEAD>
		<BODY><xsl:attribute name="onload">open('classes-list.html','classListFrame')</xsl:attribute>
			<xsl:call-template name="header">
				<xsl:with-param name="useFrame">yes</xsl:with-param>	
				<xsl:with-param name="path" select="testsuite/@package"/>
			</xsl:call-template>
			<!-- create an anchor to this package name -->
			<h3>Package <xsl:value-of select="testsuite/@package"/></h3>
			
			<table border="0" cellpadding="5" cellspacing="2" width="95%">
				<!--Header-->
				<xsl:call-template name="packageSummaryHeader"/>				
				
				<!-- write a summary for the package -->
				<tr bgcolor="#EEEEE" valign="top">
					<td><xsl:value-of select="testsuite/@package"/></td>
					<td><xsl:value-of select="sum(testsuite/@tests)"/></td>
					<td><xsl:value-of select="sum(testsuite/@errors)"/></td>
					<td><xsl:value-of select="sum(testsuite/@failures)"/></td>
					<td><xsl:value-of select="format-number(sum(testsuite/@time),'#,###0.000')"/></td>
				</tr>
			</table>
	
			<H2>Classes</H2>
			<p>
			<table border="0" cellpadding="5" cellspacing="2" width="95%">
				<!--Header-->
				<xsl:call-template name="packageSummaryHeader"/>
				
				<!--Value-->
				<xsl:apply-templates select="testsuite">
					<xsl:sort select="@name"/>
				</xsl:apply-templates>
			</table>
			</p>
		</BODY>
	</HTML>
</xsl:template>

<xsl:template match="testsuite">
		<tr bgcolor="#EEEEE" valign="top">
			<!-- set a nice color depending if there is an error/failure -->
			<xsl:attribute name="class">
				<xsl:choose>
					<xsl:when test="@errors[.&gt; 0]">Error</xsl:when>
					<xsl:when test="@failures[.&gt; 0]">Failure</xsl:when>
				</xsl:choose>
			</xsl:attribute>
		
			<!-- print testsuite information -->
			<td><a href="{@name}-details.html" target="classFrame"><xsl:value-of select="@name"/></a></td>
			<td><xsl:value-of select="@tests"/></td>
			<td><xsl:value-of select="@errors"/></td>
			<td><xsl:value-of select="@failures"/></td>
			<td><xsl:value-of select="format-number(@time,'#,###0.000')"/></td>
		</tr>
</xsl:template>

</xsl:stylesheet>