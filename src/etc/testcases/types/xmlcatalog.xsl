<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet 
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  version="1.0">

  <!-- I belong to:
       org.apache.tools.ant.types.XMLCatalogBuildFileTest.java 
       -->

  <xsl:output method="text"/>

  <!-- name of the output parameter to write -->
  <xsl:param name="outprop">value</xsl:param>

  <xsl:strip-space elements="*"/>

  <xsl:template match="/">
    <xsl:value-of select="$outprop"/>: <xsl:apply-templates select="/fragment/para"/>
  </xsl:template>

  <!-- This will only be matched in doc2.xml -->
  <xsl:template match="Ref">
    <xsl:apply-templates select="document(@file)/fragment/para"/>
  </xsl:template>

  <!-- This will only be matched in doc1.xml -->
  <xsl:template match="text()">
    <xsl:value-of select="normalize-space(.)"/>
  </xsl:template>

</xsl:stylesheet>
