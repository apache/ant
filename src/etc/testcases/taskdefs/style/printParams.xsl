<?xml version="1.0"?>

<xsl:stylesheet
  version="1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:fo="http://www.w3.org/1999/XSL/Format">

<!-- get the xsl-parameter -->
<xsl:param name="set">set default value</xsl:param>
<xsl:param name="empty">empty default value</xsl:param>
<xsl:param name="undefined">undefined default value</xsl:param>

<!-- use the xsl-parameter -->
<xsl:template match="/">
set='<xsl:value-of select="$set"/>'
empty='<xsl:value-of select="$empty"/>'
undefined='<xsl:value-of select="$undefined"/>'
</xsl:template>

</xsl:stylesheet>