<?xml version="1.0"?>

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

  <xsl:template match="echo-template">
    <echo message="This is an example of how to use a template to expand" />
    <echo message="a single element into a list of tasks to do. In this" />
    <echo message="example it is largely a case of echoing an attribute" />
    <echo message="ie. msg='{@msg}'" />
  </xsl:template>

  <xsl:template match="@*|node()">
    <xsl:copy>
      <xsl:apply-templates select="@*"/>
      <xsl:apply-templates/>
    </xsl:copy>
  </xsl:template>

</xsl:stylesheet>