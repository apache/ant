<?xml version="1.0" encoding="ISO-8859-1"?>

<!-- This style sheet should contain just a named templates that used in the other specific templates -->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:include href="toolkit.xsl"/>

<xsl:template match="testsuites">
	<HTML>
		<HEAD>
			<LINK REL ="stylesheet" TYPE="text/css" HREF="./stylesheet.css" TITLE="Style"/>
		</HEAD>
		<BODY onload="open('overview-packages.html','classFrame')">
			<H2>Classes</H2>
			<p>
			<TABLE WIDTH="100%">
					<xsl:apply-templates select="testsuite">
						<xsl:sort select="@name"/>
					</xsl:apply-templates>
			</TABLE>
			</p>
		</BODY>
	</HTML>
</xsl:template>

<xsl:template match="testsuite">
	<tr>
		<td nowrap="nowrap">
			<a target="classFrame">
				<xsl:attribute name="href">
					<xsl:if test="not(@package='')">
						<xsl:value-of select="translate(@package,'.','/')"/><xsl:text>/</xsl:text>
					</xsl:if><xsl:value-of select="@name"/><xsl:text>-details.html</xsl:text>
				</xsl:attribute>
				<xsl:value-of select="@name"/></a>
		</td>
	</tr>
</xsl:template>

</xsl:stylesheet>