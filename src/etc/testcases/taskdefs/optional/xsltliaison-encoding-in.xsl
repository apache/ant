<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

<xsl:template match="/">
    <root>
    <xsl:for-each select="/root/message">
        <message><xsl:value-of select="."/></message>
    </xsl:for-each>
    </root>
</xsl:template>
</xsl:stylesheet>