<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
<!--

  A little bit of automated transformation to help conversion
  from Anakia to Forrest/Cocoon docs.
  
  This is purely experimental, it hem...should work, well mostly
  but it should manage to break a couple of things so you will
  need a manual pass to fix things after that.
  
  Stephane Bailliez, sbailliez@apache.org
  
  -->
  <xsl:output method="xml" indent="yes" doctype-system="book-cocoon-v10.dtd"
  doctype-public="-//APACHE//DTD Cocoon Documentation Book V1.0//EN" encoding="ISO-8859-1"/>

  <xsl:template match="project">
    <book copyright="2002 The Apache Software Foundation"
          xmlns:xlink="http://www.w3.org/1999/xlink"
          software="{@name}"
          title="{title/text()}">
        <xsl:apply-templates select="body/menu"/>
    </book>
  </xsl:template>

  <xsl:template match="menu">
    <menu label="{@name}">
        <xsl:apply-templates select="item"/>
    </menu>
  </xsl:template>

  <xsl:template match="item">
    <menu-item label="{@name}" href="{@href}"/>
  </xsl:template>

</xsl:stylesheet>