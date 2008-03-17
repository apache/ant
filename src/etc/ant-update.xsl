<?xml version="1.0"?>
<xsl:stylesheet	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
<xsl:output method="xml" indent="yes"/>
<!--
   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 -->
 
<!--

  The purpose have this XSL is to provide a fast way to update a buildfile
  from deprecated tasks.
  
  It should particularly be useful when there is a lot of build files to migrate.
  If you do not want to migrate to a particular task and want to keep it for
  various reason, just comment the appropriate template.
  
  !!!! Use at your own risk. !!!!
  
  @author <a href="sbailliez@apache.org">Stephane Bailliez</a>
  
-->
 
 
  <!-- (zip|jar|war|ear)file attributes are replaced by destfile in their respective task -->
  <xsl:template match="zip">
    <zip destfile="{@zipfile}">
      <xsl:apply-templates select="@*[not(name()='zipfile')]|node()"/>
    </zip>
  </xsl:template>
  <xsl:template match="jar">
    <jar destfile="{@jarfile}">
      <xsl:apply-templates select="@*[not(name()='jarfile')]|node()"/>
    </jar>
  </xsl:template>
  <xsl:template match="war">
    <war destfile="{@warfile}">
      <xsl:apply-templates select="@*[not(name()='warfile')]|node()"/>
    </war>
  </xsl:template>
  <xsl:template match="ear">
    <ear destfile="{@earfile}">
      <xsl:apply-templates select="@*[not(name()='earfile')]|node()"/>
    </ear>
  </xsl:template>
   
 
  <!-- copydir is replaced by copy -->
  <xsl:template match="copydir">
    <copy todir="{@dest}">
      <xsl:apply-templates select="@flatten|@filtering"/>
      <xsl:if test="@forceoverwrite">
        <xsl:attribute name="overwrite"><xsl:value-of select="@forceoverwrite"/></xsl:attribute>
      </xsl:if>
      <fileset dir="{@src}">
          <xsl:apply-templates select="@includes|@includesfile|@excludes|@excludesfile|node()"/>
      </fileset>
    </copy>
  </xsl:template>

  <!-- copyfile is replaced by copy -->
  <xsl:template match="copyfile">
    <copy file="{@src}" tofile="{@dest}">
      <xsl:apply-templates select="@filtering"/>
      <xsl:if test="@forceoverwrite">
        <xsl:attribute name="overwrite"><xsl:value-of select="@forceoverwrite"/></xsl:attribute>
      </xsl:if>
    </copy>
  </xsl:template>

  <!-- deltree is replaced by delete -->
  <xsl:template match="deltree">
    <delete dir="{@dir}"/>
  </xsl:template>

  <!-- execon is replaced by apply -->
  <xsl:template match="execon">
    <apply>
      <xsl:apply-templates select="@*|node()"/>
    </apply>
  </xsl:template>

  <!-- rename is replaced by move -->
  <xsl:template match="rename">
    <move file="{@src}" tofile="{@dest}">
      <xsl:if test="@replace">
        <xsl:attribute name="overwrite"><xsl:value-of select="@replace"/></xsl:attribute>
      </xsl:if>
    </move>
  </xsl:template>

  <!-- javadoc2 is replaced by javadoc -->
  <xsl:template match="javadoc2">
    <javadoc>
      <xsl:apply-templates select="@*|node()"/>
    </javadoc>
  </xsl:template>


  <!-- Copy every node and attributes recursively -->
  <xsl:template match="node()|@*">
    <xsl:copy>
      <xsl:apply-templates select="@*|node()"/>
    </xsl:copy>
  </xsl:template>
  
</xsl:stylesheet>