<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
    xmlns:lxslt="http://xml.apache.org/xslt"
    xmlns:redirect="org.apache.xalan.lib.Redirect"
    extension-element-prefixes="redirect">

<!--
 The Apache Software License, Version 1.1

 Copyright (c) 2003 The Apache Software Foundation.  All rights
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

    <xsl:output method="xml" indent="yes"/>
    <xsl:decimal-format decimal-separator="." grouping-separator="," />

    <xsl:param name="output.dir" select="'.'"/>
    <xsl:param name="basedir" select="'.'"/>

    <xsl:template match="checkstyle">
      <document>
        <properties>
          <title>Checkstyle Audit</title>
        </properties>

        <body>
          <xsl:apply-templates select="." mode="summary"/>
          <!-- File list part -->
          <xsl:apply-templates select="." mode="filelist"/>
          <xsl:apply-templates select="file[count(error) != 0]"/>
        </body>
      </document>
    </xsl:template>

    <xsl:template match="checkstyle" mode="filelist">
      <section name="Files">
        <table>
            <tr>
                <th>Name</th>
                <th>Errors</th>
            </tr>
            <xsl:apply-templates select="file[count(error) != 0]" mode="filelist">
                <xsl:sort select="count(error)" order="descending" data-type="number"/>
            </xsl:apply-templates>
        </table>
      </section>
    </xsl:template>

    <xsl:template match="file" mode="filelist">
        <tr>
            <xsl:call-template name="alternated-row"/>
            <td nowrap="nowrap">
                <a>
                    <xsl:attribute name="href">
                        <xsl:text>files</xsl:text><xsl:value-of select="substring-after(@name, $basedir)"/><xsl:text>.html</xsl:text>
                    </xsl:attribute>
                    <xsl:value-of select="substring-after(@name, $basedir)"/>
                </a>
            </td>
            <td><xsl:value-of select="count(error)"/></td>
        </tr>
    </xsl:template>

    <xsl:template match="file">
      <redirect:write file="{$output.dir}/files{substring-after(@name, $basedir)}.xml">
        <document>
          <properties>
            <title>Checkstyle Audit</title>
          </properties>

          <body>
            <section name="Details for {substring-after(@name, $basedir)}">
              <table>
                  <tr>
                      <th>Error Description</th>
                      <th>Line</th>
                  </tr>
                  <xsl:for-each select="error">
                      <tr>
                          <xsl:call-template name="alternated-row"/>
                          <td><a title="{@source}"><xsl:value-of select="@message"/></a></td>
                          <td><xsl:value-of select="@line"/></td>
                      </tr>
                  </xsl:for-each>
              </table>
            </section>
          </body>
        </document>
      </redirect:write>
    </xsl:template>

    <xsl:template match="checkstyle" mode="summary">
      <section name="Summary">
        <xsl:variable name="fileCount" select="count(file)"/>
        <xsl:variable name="errorCount" select="count(file/error)"/>
        <xsl:variable name="fileErrorCount" select="count(file[count(error) != 0])"/>
        <table>
            <tr>
                <th>Files</th>
                <th>Files With Errors</th>
                <th>Errors</th>
            </tr>
            <tr>
                <xsl:call-template name="alternated-row"/>
                <td><xsl:value-of select="$fileCount"/></td>
                <td><xsl:value-of select="$fileErrorCount"/></td>
                <td><xsl:value-of select="$errorCount"/></td>
            </tr>
        </table>
      </section>
    </xsl:template>

    <xsl:template name="alternated-row">
        <xsl:attribute name="class">
            <xsl:if test="position() mod 2 = 1">oddrow</xsl:if>
            <xsl:if test="position() mod 2 = 0">evenrow</xsl:if>
        </xsl:attribute>
    </xsl:template>
</xsl:stylesheet>

