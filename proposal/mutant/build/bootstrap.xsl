<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
  <xsl:strip-space elements="*"/>
  <xsl:output method="text" omit-xml-declaration="yes"/>

  <xsl:template match="project">
    <xsl:text>package org.apache.ant.builder;&#10;</xsl:text>
    <xsl:text>public class </xsl:text>
    <xsl:value-of select="attribute::name"/>
    <xsl:text>Builder {&#10;</xsl:text>
    <xsl:text>    protected void _init(BuildHelper helper) {&#10;</xsl:text>
    <xsl:apply-templates select="property"/>
    <xsl:apply-templates select="path"/>
    <xsl:text>    }&#10;</xsl:text>
    <xsl:apply-templates select="target"/>
    <xsl:text>}&#10;</xsl:text>
  </xsl:template>

  <xsl:template match="property">
    <xsl:text>        helper.setProperty(&quot;</xsl:text>
    <xsl:value-of select="attribute::name"/>
    <xsl:text>&quot;, &quot;</xsl:text>
    <xsl:value-of select="attribute::value"/>
    <xsl:text>&quot;);&#10;</xsl:text>
  </xsl:template>

  <xsl:template match="antcall">
    <xsl:text>        {&#10;</xsl:text>
    <xsl:text>            BuildHelper subHelper = new BuildHelper();&#10;</xsl:text>
    <xsl:for-each select="param">
      <xsl:text>            subHelper.setProperty(&quot;</xsl:text>
      <xsl:value-of select="attribute::name"/>
      <xsl:text>&quot;, helper.resolve(&quot;</xsl:text>
      <xsl:value-of select="attribute::value"/>
      <xsl:text>&quot;));&#10;</xsl:text>
    </xsl:for-each>
    <xsl:text>            subHelper.setParent(helper);&#10;</xsl:text>
    <xsl:text>            _init(subHelper);&#10;</xsl:text>
    <xsl:text>            </xsl:text>
    <xsl:value-of select="attribute::target"/>
    <xsl:text>(subHelper);&#10;</xsl:text>
    <xsl:text>        }&#10;</xsl:text>
  </xsl:template>

  <xsl:template match="echo">
  </xsl:template>

  <xsl:template match="path">
    <xsl:text>        helper.createPath(&quot;</xsl:text>
    <xsl:variable name="pathName" select="attribute::id"/>
    <xsl:value-of select="$pathName"/>
    <xsl:text>&quot;);&#10;</xsl:text>

    <xsl:for-each select="fileset">
      <xsl:text>        </xsl:text>
      <xsl:text>helper.addFileSetToPath(&quot;</xsl:text>
      <xsl:value-of select="$pathName"/>
      <xsl:text>&quot;, &#10;</xsl:text>
      <xsl:text>                        &quot;</xsl:text>
      <xsl:value-of select="attribute::dir"/>
      <xsl:text>&quot;, </xsl:text>
      <xsl:choose>
        <xsl:when test="attribute::includes">
          <xsl:text>&quot;</xsl:text>
          <xsl:value-of select="attribute::includes"/>
          <xsl:text>&quot;</xsl:text>
        </xsl:when>
        <xsl:otherwise>
          <xsl:text>null</xsl:text>
        </xsl:otherwise>
      </xsl:choose>
      <xsl:text>);&#10;</xsl:text>
    </xsl:for-each>

    <xsl:for-each select="pathelement">
      <xsl:text>        </xsl:text>
      <xsl:text>helper.addPathElementToPath(&quot;</xsl:text>
      <xsl:value-of select="$pathName"/>
      <xsl:text>&quot;, &quot;</xsl:text>
      <xsl:value-of select="attribute::location"/>
      <xsl:text>&quot;);&#10;</xsl:text>
    </xsl:for-each>

    <xsl:for-each select="path">
      <xsl:text>        </xsl:text>
      <xsl:text>helper.addPathToPath(&quot;</xsl:text>
      <xsl:value-of select="$pathName"/>
      <xsl:text>&quot;, &quot;</xsl:text>
      <xsl:value-of select="attribute::refid"/>
      <xsl:text>&quot;);&#10;</xsl:text>
    </xsl:for-each>
  </xsl:template>

  <xsl:template match="target">
    <xsl:text>    protected void </xsl:text>
    <xsl:value-of select="translate(attribute::name, '-', '_')"/>
    <xsl:text>(BuildHelper helper) {&#10;</xsl:text>
    <xsl:text>        helper.runDepends(this, &quot;</xsl:text>
    <xsl:value-of select="translate(attribute::name, '-', '_')"/>
    <xsl:text>&quot;, &quot;</xsl:text>
    <xsl:value-of select="translate(attribute::depends, '-', '_')"/>
    <xsl:text>&quot;);&#10;</xsl:text>
    <xsl:text>        System.out.println(&quot;</xsl:text>
    <xsl:value-of select="attribute::name"/>
    <xsl:text>: &quot;);&#10;</xsl:text>
    <xsl:apply-templates/>
    <xsl:text>    }&#10;</xsl:text>
  </xsl:template>

  <xsl:template match="mkdir">
    <xsl:text>        helper.mkdir(&quot;</xsl:text>
    <xsl:value-of select="attribute::dir"/>
    <xsl:text>&quot;);&#10;</xsl:text>
  </xsl:template>

  <xsl:template match="javac">
    <xsl:text>        helper.javac(&quot;</xsl:text>
    <xsl:value-of select="attribute::srcdir"/>
    <xsl:text>&quot;, &quot;</xsl:text>
    <xsl:value-of select="attribute::destdir"/>
    <xsl:text>&quot;, </xsl:text>
    <xsl:choose>
      <xsl:when test="classpath">
        <xsl:text>&quot;</xsl:text>
        <xsl:value-of select="classpath/attribute::refid"/>
        <xsl:text>&quot;</xsl:text>
      </xsl:when>
      <xsl:otherwise>
        <xsl:text>null</xsl:text>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:text>);&#10;</xsl:text>
  </xsl:template>

  <xsl:template match="jar">
    <xsl:text>        helper.jar(&quot;</xsl:text>
    <xsl:value-of select="attribute::basedir"/>
    <xsl:text>&quot;, &quot;</xsl:text>
    <xsl:value-of select="attribute::jarfile"/>
    <xsl:text>&quot;,&#10;                   </xsl:text>
    <xsl:choose>
      <xsl:when test="metainf">
        <xsl:text>&quot;</xsl:text>
        <xsl:value-of select="metainf/attribute::dir"/>
        <xsl:text>&quot;, </xsl:text>
        <xsl:choose>
          <xsl:when test="metainf/attribute::includes">
            <xsl:text>&quot;</xsl:text>
            <xsl:value-of select="metainf/attribute::includes"/>
            <xsl:text>&quot;, </xsl:text>
          </xsl:when>
          <xsl:otherwise>
            <xsl:text>null, </xsl:text>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:when>
      <xsl:otherwise>
        <xsl:text>null, null, </xsl:text>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:choose>
      <xsl:when test="manifest/attribute[attribute::name='Class-Path']">
        <xsl:text>&quot;</xsl:text>
        <xsl:value-of select="manifest/attribute[attribute::name='Class-Path']/attribute::value"/>
        <xsl:text>&quot;, </xsl:text>
      </xsl:when>
      <xsl:otherwise>null, </xsl:otherwise>
    </xsl:choose>
    <xsl:choose>
      <xsl:when test="manifest/attribute[attribute::name='Main-Class']">
        <xsl:text>&quot;</xsl:text>
        <xsl:value-of select="manifest/attribute[attribute::name='Main-Class']/attribute::value"/>
        <xsl:text>&quot;</xsl:text>
      </xsl:when>
      <xsl:otherwise>null</xsl:otherwise>
    </xsl:choose>
    <xsl:text>);&#10;</xsl:text>
  </xsl:template>


  <xsl:template match="copy/fileset">
    <xsl:choose>
      <xsl:when test="attribute::refid">
        <xsl:text>        helper.copyFilesetRef(&quot;</xsl:text>
        <xsl:value-of select="attribute::refid"/>
        <xsl:text>&quot;, &quot;</xsl:text>
        <xsl:value-of select="../attribute::todir"/>
        <xsl:text>&quot;);&#10;</xsl:text>
      </xsl:when>
      <xsl:otherwise>
        <xsl:text>        helper.copyFileset(&quot;</xsl:text>
        <xsl:value-of select="attribute::dir"/>
        <xsl:text>&quot;, &quot;</xsl:text>
        <xsl:value-of select="../attribute::todir"/>
        <xsl:text>&quot;);&#10;</xsl:text>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
</xsl:stylesheet>
