<xsl:stylesheet	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
<xsl:output method="xml" indent="yes"/>
<!--
 The Apache Software License, Version 1.1

 Copyright (c) 2001-2002 The Apache Software Foundation.  All rights
 reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions
 are met:

 1. Redistributions of source code must retain the above copyright
    notice, this list of conditions and the following disclaimer.

 2. Redistributions in binary form must reproduce the above copyright
    notice, this list of conditions and the following disclaimer in
    the documentation and/or other materials provided with the
    distribution.

 3. The end-user documentation included with the redistribution, if
    any, must include the following acknowlegement:
       "This product includes software developed by the
        Apache Software Foundation (http://www.apache.org/)."
    Alternately, this acknowlegement may appear in the software itself,
    if and wherever such third-party acknowlegements normally appear.

 4. The names "Ant" and "Apache Software
    Foundation" must not be used to endorse or promote products derived
    from this software without prior written permission. For written
    permission, please contact apache@apache.org.

 5. Products derived from this software may not be called "Apache"
    nor may "Apache" appear in their names without prior written
    permission of the Apache Group.

 THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 SUCH DAMAGE.
 ====================================================================

 This software consists of voluntary contributions made by many
 individuals on behalf of the Apache Software Foundation.  For more
 information on the Apache Software Foundation, please see
 <http://www.apache.org/>.
 -->
 
<!--

  The purpose have this XSL is to provide a fast way to update a buildfile
  from deprecated tasks.
  
  It should particulary be useful when there is a lot of build files to migrate.
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