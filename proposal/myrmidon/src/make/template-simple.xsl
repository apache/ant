<?xml version="1.0"?>

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

  <xsl:template match="log-template">
    <log message="This is an example of how to use a template to expand" />
    <log message="a single element into a list of tasks to do. In this" />
    <log message="example it is largely a case of echoing an attribute" />
    <log message="ie. msg='{@msg}' and embeddding an ant variable ${{year}}" />
  </xsl:template>

  <xsl:template match="@*|node()">
    <xsl:copy>
      <xsl:apply-templates select="@*"/>
      <xsl:apply-templates/>
    </xsl:copy>
  </xsl:template>

</xsl:stylesheet>