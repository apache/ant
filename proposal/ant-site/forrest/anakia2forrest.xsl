<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
<!--

  A little bit of automated transformation to help conversion
  from Anakia to Forrest/Cocoon docs.
  
  This is purely experimental, it hem...should work, well mostly
  but it should manage to break a couple of things so you will
  need a manual pass to fix things after that.
  
  Stephane Bailliez, sbailliez@apache.org
  
  -->
  <xsl:output method="xml" indent="yes" doctype-system="document-v11.dtd"
  doctype-public="-//APACHE//DTD Documentation V1.1//EN" encoding="ISO-8859-1"/>

  <!-- properties = header in Forrest language -->
  <xsl:template match="properties">
    <header>
      <xsl:apply-templates select="*[ local-name() != 'author']"/>
      <author>
        <xsl:apply-templates select="author"/>
      </author>
    </header>
  </xsl:template>

  <xsl:template match="author">
    <person id="{text()}">
      <!--
      not everyone gives his mail in order not to receive too much spam... or be
      assimilated as 24/24 7/7 support
      -->
      <xsl:if test="@email"><xsl:attribute name="email"><xsl:value-of select="@email"/></xsl:attribute></xsl:if>
    </person>
  </xsl:template>

  <!-- section = subsection with forrest and the title is an element -->
  <xsl:template match="section|subsection">
    <section>
      <title><xsl:value-of select="@name"/></title>
      <xsl:apply-templates/>
    </section>
  </xsl:template>

  <!-- Ignore those tags they should not be here, this will clean up
  some but will break others, anyways. br is evil :) -->
  <xsl:template match="br|nobr">
    <xsl:apply-templates/>
  </xsl:template>

  <!-- font should not be there, it was used to do a source (or code ?)-->
  <xsl:template match="font">
    <source>
      <xsl:apply-templates/>
    </source>
  </xsl:template>

  <!-- assumes img = icon rather than figure -->
  <xsl:template match="img">
    <icon>
    <xsl:apply-templates select="@alt|@src|@width|@height"/>
    </icon>
  </xsl:template>
  
  <!-- assume every anchor with a name is an anchor -->
  <xsl:template match="a[@name]">
    <anchor>
      <xsl:apply-templates select="@*"/>
      <xsl:apply-templates/>
    </anchor>  
  </xsl:template>
  
  <!-- try to be super smart to detect link/anchor/jump -->
  <xsl:template match="a[@href]">
    <xsl:choose>
      <!-- something with a hashmark is a jump -->
      <xsl:when test="starts-with(@href, '#')">
        <jump>
          <xsl:apply-templates select="@*"/>
          <xsl:apply-templates/>        
        </jump>
      </xsl:when>
      <!--
        assume everything out of apache domain would be better with a fork
        This is really a life style, I hate windows forking all over my
        desktop but it's quite convenient sometimes when too lazy to shift.
        One super thing would be to do like Microsoft.com and put an
        extra icon via forrest after the link when it send outside
        the apache site.
      -->
      <xsl:when test="starts-with(@href, 'http://') and not(contains(@href, 'apache.org'))">
        <fork>
          <xsl:apply-templates select="@*"/>
          <xsl:apply-templates/>        
        </fork>
      </xsl:when>
      <!-- fallback to a basic link -->
      <xsl:otherwise>
        <link>
          <xsl:apply-templates select="@*"/>
          <xsl:apply-templates/>
        </link>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <!---
    Ugly hack to define an axial table
    The css could be defined as text-align:right !important;
    -->
  <xsl:template match="table">
    <table>
    <!--
      (tr[1]/th and tr[1]/td) would give the same result
      but I'm using following-sibbling to sound super smart
      and obfuscate my code. It hard to write, so it should
      be hard to understand ;-)
      -->
    <xsl:if test="tr[1]/th[following-sibling::td]">
      <xsl:attribute name="class">axial</xsl:attribute>
    </xsl:if>
    <xsl:apply-templates select="@*"/>
    <xsl:apply-templates/>
    </table>
  </xsl:template>

<!-- wide rule, copy all nodes and attributes -->
  <xsl:template match="node()|@*" priority="-1">
    <xsl:copy>
      <xsl:apply-templates select="@*"/>
      <xsl:apply-templates/>
    </xsl:copy>
  </xsl:template>
</xsl:stylesheet>