<!--
    Copyright 2005 The Apache Software Foundation
   
     Licensed under the Apache License, Version 2.0 (the "License");
     you may not use this file except in compliance with the License.
     You may obtain a copy of the License at
   
         http://www.apache.org/licenses/LICENSE-2.0
   
     Unless required by applicable law or agreed to in writing, software
     distributed under the License is distributed on an "AS IS" BASIS,
     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
     See the License for the specific language governing permissions and
     limitations under the License.
   
-->
<xsl:stylesheet
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    version="1.0">
  <xsl:param name="title"/>
  <xsl:param name="repo"/>

  <xsl:output method="html" indent="yes"/>

  <!-- Copy standard document elements.  Elements that
       should be ignored must be filtered by apply-templates
       tags. -->
  <xsl:template match="*">
    <xsl:copy>
      <xsl:copy-of select="attribute::*[. != '']"/>
      <xsl:apply-templates/>
    </xsl:copy>
  </xsl:template>

  <xsl:template match="revisiondiff">
    <HTML>
      <HEAD>
        <TITLE><xsl:value-of select="$title"/></TITLE>
      </HEAD>
      <BODY link="#000000" alink="#000000" vlink="#000000" text="#000000">
        <style type="text/css">
          body, p {
          font-family: verdana,arial,helvetica;
          font-size: 80%;
          color:#000000;
          }
	  .dateAndAuthor {
          font-family: verdana,arial,helvetica;
          font-size: 80%;
          font-weight: bold;
          text-align:left;
          background:#a6caf0;
	  }
          tr, td{
          font-family: verdana,arial,helvetica;
          font-size: 80%;
          background:#eeeee0;
          }	  
	  </style>        
          <h1>
            <a name="top"><xsl:value-of select="$title"/></a>
          </h1>
          diff between <xsl:value-of select="@start"/> and <xsl:value-of select="@end"/>
          <p align="right">Designed for use with <a href="http://ant.apache.org/">Apache Ant</a>.</p>
          <hr size="2"/>
	<a name="TOP"/>
	<table width="100%">
		<tr>
			<td align="right">
				<a href="#New">New Files</a> |
				<a href="#Modified">Modified Files</a> |
				<a href="#Removed">Removed Files</a>
			</td>
		</tr>
	</table>
        <TABLE BORDER="0" WIDTH="100%" CELLPADDING="3" CELLSPACING="1">
		<xsl:call-template name="show-paths">
			<xsl:with-param name="title">New Files</xsl:with-param>
			<xsl:with-param name="anchor">New</xsl:with-param>
			<xsl:with-param name="paths" select=".//path[action='added']"/>
		</xsl:call-template>

		<xsl:call-template name="show-paths">
			<xsl:with-param name="title">Modified Files</xsl:with-param>
			<xsl:with-param name="anchor">Modified</xsl:with-param>
			<xsl:with-param name="paths" select=".//path[action='modified']"/>
		</xsl:call-template>

		<xsl:call-template name="show-paths">
			<xsl:with-param name="title">Removed Files</xsl:with-param>
			<xsl:with-param name="anchor">Removed</xsl:with-param>
			<xsl:with-param name="paths" select=".//path[action='deleted']"/>
		</xsl:call-template>
        </TABLE>
        
      </BODY>
    </HTML>
  </xsl:template>

  <xsl:template name="show-paths">
	<xsl:param name="title"/>
	<xsl:param name="anchor"/>
	<xsl:param name="paths"/>
	<TR>
		<TD colspan="2" class="dateAndAuthor">
			<a>
				<xsl:attribute name="name"><xsl:value-of select="$anchor"/></xsl:attribute>
				<xsl:value-of select="$title"/> - <xsl:value-of select="count($paths)"/> entries
			</a>
			<a href="#TOP">(back to top)</a>
		</TD>
	</TR>
	<TR>
		<TD width="20">
			<xsl:text>    </xsl:text>
		</TD>
		<TD>
		        <ul>
				<xsl:apply-templates select="$paths"/>
			</ul>
		</TD>
	</TR>
  </xsl:template>  

  <xsl:template match="path">
    <li>
      <a target="_new">
        <xsl:attribute name="href"><xsl:value-of select="$repo"/>/<xsl:value-of select="name" /></xsl:attribute>
        <xsl:value-of select="name" />
      </a>
    </li>
  </xsl:template>

  <!-- Any elements within a msg are processed,
       so that we can preserve HTML tags. -->
  <xsl:template match="msg">
    <b><xsl:apply-templates/></b>
  </xsl:template>
  
</xsl:stylesheet>
