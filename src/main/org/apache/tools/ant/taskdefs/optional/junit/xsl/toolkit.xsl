<?xml version="1.0" encoding="ISO-8859-1"?>

<!-- This style sheet should contain just a named templates that used in the other specific templates -->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<!-- transform string like a.b.c to ../../../  -->
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

<!--
	template that will convert a carriage return into a br tag
	@param word the text from which to convert CR to BR tag
-->
<xsl:template name="br-replace">
	<xsl:param name="word"/>
	<xsl:choose>
		<xsl:when test="contains($word,'&#xA;')">
			<xsl:value-of select="substring-before($word,'&#xA;')"/>
			<br/>
			<xsl:call-template name="br-replace">
				<xsl:with-param name="word" select="substring-after($word,'&#xA;')"/>
			</xsl:call-template>
		</xsl:when>
		<xsl:otherwise>
			<xsl:value-of select="$word"/>
		</xsl:otherwise>
	</xsl:choose>
</xsl:template>

<!-- 
		=====================================================================
		classes summary header
		=====================================================================
-->
<xsl:template name="header">
	<xsl:param name="useFrame">no</xsl:param>
	<xsl:param name="path"/>
	<h1>Unit Tests Results</h1>
	<table width="100%">
	<tr>
		<td align="left">
			<!--xsl:choose>
				<xsl:when test="$useFrame='yes'">Frames&#160;
				<a target="_top">
					<xsl:attribute name="href"><xsl:call-template name="path"><xsl:with-param name="path" select="$path"/></xsl:call-template>noframes.html</xsl:attribute>No frames</a></xsl:when>
				<xsl:when test="$useFrame='no'"><a target="_top"><xsl:attribute name="href"><xsl:call-template name="path"><xsl:with-param name="path" select="$path"/></xsl:call-template>index.html</xsl:attribute>Frames</a>&#160;No frames</xsl:when>
				<xsl:otherwise><code>ERROR : useFrame must have 'no' or 'yes' as value.</code></xsl:otherwise>
			</xsl:choose-->
		</td>
		<td align="right">Designed for use with <a href='http://www.junit.org'>JUnit</a> and <a href='http://jakarta.apache.org'>Ant</a>.</td>
	</tr>
	</table>
	<hr size="1"/>
</xsl:template>

<xsl:template name="summaryHeader">
	<tr bgcolor="#A6CAF0" valign="top">
		<td><b>Tests</b></td>
		<td><b>Failures</b></td>
		<td><b>Errors</b></td>
		<td><b>Success Rate</b></td>
		<td nowrap="nowrap"><b>Time(s)</b></td>
	</tr>
</xsl:template>

<!-- 
		=====================================================================
		package summary header
		=====================================================================
-->
<xsl:template name="packageSummaryHeader">
	<tr bgcolor="#A6CAF0" valign="top">
		<td width="75%"><b>Name</b></td>
		<td width="5%"><b>Tests</b></td>
		<td width="5%"><b>Errors</b></td>
		<td width="5%"><b>Failures</b></td>
		<td width="10%" nowrap="nowrap"><b>Time(s)</b></td>
	</tr>
</xsl:template>

<!-- 
		=====================================================================
		classes summary header
		=====================================================================
-->
<xsl:template name="classesSummaryHeader">
	<tr bgcolor="#A6CAF0" valign="top">
		<td width="18%"><b>Name</b></td>
		<td width="7%"><b>Status</b></td>
		<td width="70%"><b>Type</b></td>
		<td width="5%" nowrap="nowrap"><b>Time(s)</b></td>
	</tr>
</xsl:template>

<!-- 
		=====================================================================
		Write the summary report
		It creates a table with computed values from the document:
		User | Date | Environment | Tests | Failures | Errors | Rate | Time
		Note : this template must call at the testsuites level
		=====================================================================
-->
	<xsl:template name="summary">
		<h2>Summary</h2>
		<xsl:variable name="testCount" select="sum(./testsuite/@tests)"/>
		<xsl:variable name="errorCount" select="sum(./testsuite/@errors)"/>
		<xsl:variable name="failureCount" select="sum(./testsuite/@failures)"/>
		<xsl:variable name="timeCount" select="sum(./testsuite/@time)"/>
		<xsl:variable name="successRate" select="($testCount - $failureCount - $errorCount) div $testCount"/>
		<table border="0" cellpadding="5" cellspacing="2" width="95%">
		<xsl:call-template name="summaryHeader"/>
		<tr bgcolor="#EEEEE" valign="top">
			<xsl:attribute name="class">
				<xsl:choose>
					<xsl:when test="./failure | ./error">Error</xsl:when>
					<xsl:otherwise>TableRowColor</xsl:otherwise>
				</xsl:choose>
			</xsl:attribute>		
			<td><xsl:value-of select="$testCount"/></td>
			<td><xsl:value-of select="$failureCount"/></td>
			<td><xsl:value-of select="$errorCount"/></td>
			<td><xsl:value-of select="format-number($successRate,'#,##0.00%')"/></td>
			<td><xsl:value-of select="format-number($timeCount,'#,###0.000')"/></td>
		</tr>
		</table>
		Note: <i>failures</i> are anticipated and checked for with assertions while <i>errors</i> are unanticipated.
	</xsl:template>

<!-- 
		=====================================================================
		testcase report
		=====================================================================
-->
<xsl:template match="testcase">
	<TR bgcolor="#EEEEE" valign="top"><xsl:attribute name="class">
			<xsl:choose>
				<xsl:when test="./failure | ./error">Error</xsl:when>
				<xsl:otherwise>TableRowColor</xsl:otherwise>
			</xsl:choose>
		</xsl:attribute>
		<TD><xsl:value-of select="./@name"/></TD>
		<xsl:choose>
			<xsl:when test="./failure">
				<td>Failure</td>
				<td><xsl:apply-templates select="./failure"/></td>
			</xsl:when>
			<xsl:when test="./error">
				<TD>Error</TD>
				<td><xsl:apply-templates select="./error"/></td>
			</xsl:when>
			<xsl:otherwise>
				<TD>Success</TD>
				<TD></TD>
			</xsl:otherwise>
		</xsl:choose>
		<td><xsl:value-of select="format-number(@time,'#,###0.000')"/></td>
	</TR>
</xsl:template>

<!-- Note : the below template error and failure are the same style
            so just call the same style store in the toolkit template -->
<xsl:template match="failure">
	<xsl:call-template name="display-failures"/>
</xsl:template>

<xsl:template match="error">
	<xsl:call-template name="display-failures"/>
</xsl:template>

<!-- Style for the error and failure in the tescase template -->
<xsl:template name="display-failures">
	<xsl:choose>
		<xsl:when test="not(@message)">N/A</xsl:when>
		<xsl:otherwise>
			<xsl:value-of select="@message"/>
		</xsl:otherwise>
	</xsl:choose>
	<!-- display the stacktrace -->
	<code>
		<p/>
		<xsl:call-template name="br-replace">
			<xsl:with-param name="word" select="."/>
		</xsl:call-template>
	</code>
	<!-- the later is better but might be problematic for non-21" monitors... -->
	<!--pre><xsl:value-of select="."/></pre-->
</xsl:template>

<!-- I am sure that all nodes are called -->
<xsl:template match="*">
	<xsl:apply-templates/>
</xsl:template>

</xsl:stylesheet>