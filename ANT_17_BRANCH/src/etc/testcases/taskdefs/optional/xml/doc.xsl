<xsl:stylesheet 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:template="struts template" 
    version="1.0">
<xsl:output method="text"/>
<xsl:template match="/">
<xsl:value-of select="/doc/section"/>
</xsl:template> 
</xsl:stylesheet>
