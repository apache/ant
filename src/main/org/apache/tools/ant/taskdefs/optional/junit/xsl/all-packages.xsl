<?xml version="1.0" encoding="ISO-8859-1"?>

<!-- This style sheet should contain just a named templates that used in the other specific templates -->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<!-- import the commun templates -->
<xsl:include href="toolkit.xsl"/>

<xsl:template match="testsuites">
	<HTML>
		<HEAD>
			<LINK REL ="stylesheet" TYPE="text/css" HREF="./stylesheet.css" TITLE="Style"/>
		</HEAD>
		<BODY>
	
			<H2><a href="all-classes.html" target="classListFrame">Home</a></H2>
			<!-- create a summary on this testcase-->
			<!--xsl:call-template name="SummaryTableHeadRootPackage"/-->
			<H2>Packages</H2>

			<!-- Get the list of the subpackage -->
			<p>
				<table width="100%">
					<!-- For each packages node apply the style describe in the below template-->
					<xsl:apply-templates select="testsuite[not(./@package = preceding-sibling::testsuite/@package)]">
						<xsl:sort select="@package"/>
					</xsl:apply-templates>
				</table>
			</p>
		</BODY>
	</HTML>
</xsl:template>

<xsl:template match="testsuite">
	<tr>
		<td nowrap="nowrap">
			<a href="{translate(@package,'.','/')}/package-summary.html" target="classFrame"><xsl:value-of select="@package"/></a>
		</td>
	</tr>
</xsl:template>

</xsl:stylesheet>