<?xml version="1.0" encoding="ISO-8859-1"?>

<!-- This style sheet should contain just a named templates that used in the other specific templates -->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<!-- import the commun templates -->
<xsl:include href="toolkit.xsl"/>

<xsl:template match="testsuite">
	<HTML>
		<HEAD>
			<LINK REL ="stylesheet" TYPE="text/css" TITLE="Style">
				<xsl:attribute name="href"><xsl:call-template name="path"><xsl:with-param name="path" select="@package"/></xsl:call-template>stylesheet.css</xsl:attribute>
			</LINK>
		</HEAD>
		<BODY>
			<xsl:call-template name="header">
				<xsl:with-param name="useFrame">yes</xsl:with-param>
				<xsl:with-param name="path" select="@package"/>
			</xsl:call-template>
	
			<H2>Class <xsl:if test="not(@package = '')"><xsl:value-of select="@package"/>.</xsl:if><xsl:value-of select="@name"/></H2>
			<p>
			<h3>TestCase <xsl:value-of select="@name"/></h3>
			
			<table border="0" cellpadding="5" cellspacing="2" width="95%">
				<!-- Header -->
				<xsl:call-template name="classesSummaryHeader"/>
				<!-- match the testcases of this package -->
				<xsl:apply-templates select="testcase"/>
			</table>
			</p>
		</BODY>
	</HTML>
</xsl:template>

</xsl:stylesheet>
