<?xml version="1.0"?>

<xsl:stylesheet
  version="1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <xsl:output indent="no" method="text"/>
    <xsl:strip-space elements="*"/>

    <xsl:param name="filename">-not-set-</xsl:param>
    <xsl:param name="filedir">-not-set-</xsl:param>

<!-- use the xsl-parameter -->
<xsl:template match="/">
    filename='<xsl:value-of select="$filename"/>'
    filedir ='<xsl:value-of select="$filedir"/>'
</xsl:template>

<!-- delete the raw xml data -->
<xsl:template match="*"/>

</xsl:stylesheet>