<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                version="1.0">

  <xsl:template match="author">
    <author>
      <xsl:attribute name="name">
        <xsl:value-of select="@name"/>
      </xsl:attribute>
    </author>
  </xsl:template> 
</xsl:stylesheet>
