<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:include href="toolkit.xsl"/>

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
	<tr valign="top">
		<xsl:attribute name="class">
			<xsl:choose>
				<xsl:when test="$isError">Failure</xsl:when>
				<xsl:otherwise>Pass</xsl:otherwise>
			</xsl:choose>
		</xsl:attribute>
		<td>
			<a href="{translate(@package,'.','/')}/package-summary.html">
				<xsl:value-of select="@package"/>
			</a>
		</td>
		<xsl:call-template name="statistics">
			<xsl:with-param name="isError" select="$isError"/>
		</xsl:call-template>
	</tr>
</xsl:template>

<xsl:template match="testsuite" mode="class">
	<xsl:variable name="isError" select="(@errors + @failures)&gt;0"/>
	<!-- write a summary for the package -->
	<tr valign="top">
		<xsl:attribute name="class">
			<xsl:choose>
				<xsl:when test="$isError">Failure</xsl:when>
				<xsl:otherwise>Pass</xsl:otherwise>
			</xsl:choose>
		</xsl:attribute>
		<td>

			<a href="{translate(@package,'.','/')}/summary.html">
				<xsl:value-of select="@name"/>
			</a>
		</td>
		<xsl:call-template name="statistics">
			<xsl:with-param name="isError" select="$isError"/>
		</xsl:call-template>
	</tr>
</xsl:template>


<xsl:template name="statistics">
	<xsl:variable name="isError"/>
		<td>
			<xsl:value-of select="sum(//testsuite[@package = current()/@package]/@tests)"/></td>
		<td>
			<xsl:value-of select="sum(//testsuite[@package = current()/@package]/@errors)"/></td>
		<td>
			<xsl:value-of select="sum(//testsuite[@package = current()/@package]/@failures)"/></td>
		<td>
            <xsl:call-template name="display-time">
            	<xsl:with-param name="value" select="sum(//testsuite[@package = current()/@package]/@time)"/>
            </xsl:call-template>
		</td>
</xsl:template>

</xsl:stylesheet>
