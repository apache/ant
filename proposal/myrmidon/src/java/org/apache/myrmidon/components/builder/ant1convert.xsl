<?xml version="1.0"?>

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
  <xsl:output method="xml" indent="yes"/>
  
  <xsl:template match="/project">
    <xsl:comment>Converted Project file.</xsl:comment>
    <xsl:copy>
      <xsl:attribute name="version">2.0</xsl:attribute>
      <xsl:apply-templates select="@*[name() != 'version']" mode="copy"/>
      <xsl:apply-templates/>
    </xsl:copy>
  </xsl:template>
  
  <!-- For projects with a version attribute, simply copy the entire tree. -->
  <!-- TODO check for version >= 2.0.0 -->
  <xsl:template match="/project[@version]">
    <xsl:comment>Copied Project file.</xsl:comment>
    <xsl:copy>
      <xsl:apply-templates select="@*"/>
      <xsl:apply-templates mode="copy"/>
    </xsl:copy>
  </xsl:template>
  
  <!-- Handle simple target nodes -->
  <xsl:template match="/project/target">
    <xsl:copy>
      <xsl:apply-templates select="@*"/>
      <xsl:apply-templates/>
    </xsl:copy>
  </xsl:template>
  
  <!-- Handle target nodes with 'if' -->
  <xsl:template match="/project/target[@if]">
    <xsl:copy>
      <xsl:apply-templates select="@*[name() != 'if']"/>
      
      <!-- Put in the condition -->
      <xsl:element name="if">
        <xsl:attribute name="test"><xsl:value-of select="@if"/></xsl:attribute>
        <xsl:apply-templates/>
      </xsl:element>
    </xsl:copy>
  </xsl:template>
  
  <!-- Handle target nodes with 'unless' -->
  <xsl:template match="/project/target[@unless]">
    <xsl:copy>
      <xsl:apply-templates select="@*[name() != 'unless']"/>
      
      <!-- Put in the condition -->
      <xsl:element name="if">
        <xsl:attribute name="not-test">
          <xsl:value-of select="@unless"/>
        </xsl:attribute>
        <xsl:apply-templates/>
      </xsl:element>
    </xsl:copy>   
  </xsl:template>
  
  <!-- Handle target nodes with 'if' and 'unless' -->
  <xsl:template match="/project/target[@if and @unless]">
    <xsl:copy>
      <xsl:apply-templates select="@*[name()!='if' and name()!='unless']"/>
      
      <!-- Put in the 'if' condition -->
      <xsl:element name="if">
        <xsl:attribute name="test"><xsl:value-of select="@if"/></xsl:attribute>
        <!-- Put in the 'unless' condition -->
        <xsl:element name="if">
          <xsl:attribute name="not-test"><xsl:value-of select="@unless"/></xsl:attribute>
          <xsl:apply-templates/>
        </xsl:element>
      </xsl:element>
    </xsl:copy>
  </xsl:template>
  
  
  <!-- Handle task nodes -->
  <xsl:template match="*">
    <xsl:element name="ant1.{name()}">
        <xsl:apply-templates select="@*"/>
        <xsl:apply-templates mode="copy"/>
    </xsl:element>
  </xsl:template>
  
  <!-- Copy all elements in copy-mode -->
  <xsl:template match="*" mode="copy">
    <xsl:copy>
        <xsl:apply-templates select="@*"/>
        <xsl:apply-templates mode="copy"/>
    </xsl:copy>
  </xsl:template>
  
  <!-- Always copy attributes -->
  <xsl:template match="@*">
    <xsl:copy/>
  </xsl:template>
 
  <xsl:template match="@*" mode="copy">
    <xsl:copy/>
  </xsl:template>
 
  <!-- Always copy comments -->
  <xsl:template match="comment()">
    <xsl:copy/>
  </xsl:template>
  
  <xsl:template match="comment()" mode="copy">
    <xsl:copy/>
  </xsl:template>
</xsl:stylesheet>



