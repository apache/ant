<xsl:stylesheet xmlns:xsl="http://www.w3.org/TR/WD-xsl" xmlns:HTML="http://www.w3.org/Profiles/XHTML-transitional">

<xsl:template match="/">
	<html>
		<body>
			<xsl:apply-templates/>
		</body>
	</html>
</xsl:template>

<xsl:template match="*">
	<xsl:apply-templates/>
</xsl:template>

<xsl:template match="build">
	<center>
		<table width="100%" bgcolor="#CCCCCC"><tr>
			<xsl:if test=".[@error]">
				<td align="left" width="30%"><b>Build Failed</b></td> 
				<td align="center" width="40%"><b><xsl:value-of select="@error"/></b></td>
			</xsl:if>
			<xsl:if test=".[not(@error)]">
				<td><b>Build Complete</b></td>
			</xsl:if>
			<td align="right" width="30%"><b>Total Time: <xsl:value-of select="@time"/></b></td>
		</tr></table>
		
		<br/>
		<table >
			<xsl:apply-templates/>
		</table>
		
	</center>

</xsl:template>

<xsl:template match="message[@priority!='debug']">
	<tr valign="top">
		
		<td><b><pre><xsl:value-of select="../@location"/></pre></b></td>
		<td><b><pre><xsl:value-of select="../@name"/></pre></b></td>

		<td>
			<xsl:attribute name="STYLE">color:
				<xsl:choose>
					<xsl:when test="@priority[.='error']">red</xsl:when>
					<xsl:when test="@priority[.='warn']">brown</xsl:when>
					<xsl:when test="@priority[.='info']">gray</xsl:when>
					<xsl:when test="@priority[.='debug']">gray</xsl:when>
				</xsl:choose>
			</xsl:attribute>
			<pre><xsl:value-of select="text()"/></pre>
		</td>
	</tr>
</xsl:template>

</xsl:stylesheet>