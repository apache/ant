<?xml version="1.0" encoding="ISO-8859-1"?>

<!-- This style sheet should contain just a named templates that used in the other specific templates -->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:include href="toolkit.xsl"/>

<!-- Calculate all summary values -->
<xsl:variable name="testCount" select="sum(//testsuite/@tests)"/>
<xsl:variable name="errorCount" select="sum(//testsuite/@errors)"/>
<xsl:variable name="failureCount" select="sum(//testsuite/@failures)"/>
<xsl:variable name="timeCount" select="sum(//testsuite/@time)"/>
<xsl:variable name="successRate" select="($testCount - $failureCount - $errorCount) div $testCount"/>

<xsl:template match="testsuites">
	<HTML>
		<HEAD>
			<LINK REL ="stylesheet" TYPE="text/css" HREF="stylesheet.css" TITLE="Style"/>
		</HEAD>
		<BODY>
			<xsl:call-template name="header">
				<xsl:with-param name="useFrame">yes</xsl:with-param>	
			</xsl:call-template>

			<xsl:call-template name="summary"/>


		<xsl:if test="count(testsuite[not(./@package = preceding-sibling::testsuite/@package)])&gt;0">	
		<h2>Packages</h2>
		<table border="0" cellpadding="5" cellspacing="2" width="95%">
			<!--Header-->
			<xsl:call-template name="packageSummaryHeader"/>
			
			<!-- write a summary for the package -->
			<xsl:apply-templates select="testsuite[not(./@package = preceding-sibling::testsuite/@package) and not(./@package = '') ]" mode="package">
				<xsl:sort select="@package"/>
			</xsl:apply-templates>
		</table>
		<br/>
		</xsl:if>


		<xsl:if test="count(testsuite[./@package = ''])&gt;0">	
		<h2>Classes</h2>
		<table border="0" cellpadding="5" cellspacing="2" width="95%">
			<!--Header-->
			<xsl:call-template name="packageSummaryHeader"/>
				
			<!-- write a summary for the package -->
			<xsl:apply-templates select="testsuite[./@package = '']" mode="class">
				<xsl:sort select="@name"/>
			</xsl:apply-templates>
		</table>
		</xsl:if>
		</BODY>
	</HTML>
</xsl:template>

<xsl:template match="testsuite" mode="package">
	<xsl:variable name="isError" select="(sum(//testsuite[@package = current()/@package]/@errors) + sum(//testsuite[@package = current()/@package]/@failures))&gt;0"/>
	<!-- write a summary for the package -->
	<tr bgcolor="#EEEEE" valign="top">
		<td><xsl:if test="$isError"><xsl:attribute name="class">Error</xsl:attribute></xsl:if><a href="{translate(@package,'.','/')}/package-summary.html"><xsl:value-of select="@package"/></a></td>
		<xsl:call-template name="statistics">
			<xsl:with-param name="isError" select="$isError"/>
		</xsl:call-template>
	</tr>
</xsl:template>

<xsl:template match="testsuite" mode="class">
	<xsl:variable name="isError" select="(@errors + @failures)&gt;0"/>
	<!-- write a summary for the package -->
	<tr bgcolor="#EEEEE" valign="top">
		<td><xsl:if test="$isError"><xsl:attribute name="class">Error</xsl:attribute></xsl:if><a href="{translate(@package,'.','/')}/summary.html"><xsl:value-of select="@name"/></a></td>
		<xsl:call-template name="statistics">
			<xsl:with-param name="isError" select="$isError"/>
		</xsl:call-template>
	</tr>
</xsl:template>


<xsl:template name="statistics">
	<xsl:variable name="isError"/>
		<td><xsl:if test="$isError"><xsl:attribute name="class">Error</xsl:attribute></xsl:if><xsl:value-of select="sum(//testsuite[@package = current()/@package]/@tests)"/></td>
		<td><xsl:if test="$isError"><xsl:attribute name="class">Error</xsl:attribute></xsl:if><xsl:value-of select="sum(//testsuite[@package = current()/@package]/@errors)"/></td>
		<td><xsl:if test="$isError"><xsl:attribute name="class">Error</xsl:attribute></xsl:if><xsl:value-of select="sum(//testsuite[@package = current()/@package]/@failures)"/></td>
		<td><xsl:if test="$isError"><xsl:attribute name="class">Error</xsl:attribute></xsl:if><xsl:value-of select="format-number(sum(//testsuite[@package = current()/@package]/@time),'#,###0.000')"/></td>
</xsl:template>

</xsl:stylesheet>