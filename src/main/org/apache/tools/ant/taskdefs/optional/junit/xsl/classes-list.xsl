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
		<BODY>
			
			<table width="100%">
				<tr>
					<td nowrap="nowrap">
						<H2><a href="package-summary.html" target="classFrame"><xsl:value-of select="testsuite/@package"/></a></H2>
					</td>
				</tr>
			</table>
	
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
			<a href="{@name}-details.html" target="classFrame"><xsl:value-of select="@name"/></a>
		</td>
	</tr>
</xsl:template>

</xsl:stylesheet>