<?xml version="1.0" encoding="utf-8"?>
<!--
  Licensed to the Apache Software Foundation (ASF) under one or more
  contributor license agreements.  See the NOTICE file distributed with
  this work for additional information regarding copyright ownership.
  The ASF licenses this file to You under the Apache License, Version 2.0
  (the "License"); you may not use this file except in compliance with
  the License.  You may obtain a copy of the License at

      https://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->
<xsl:stylesheet 
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  version="1.0">

  <!-- I belong to:
       org.apache.tools.ant.types.XMLCatalogBuildFileTest.java 
       -->

  <xsl:output method="text"/>

  <!-- name of the output parameter to write -->
  <xsl:param name="outprop">value</xsl:param>

  <xsl:strip-space elements="*"/>

  <xsl:template match="/">
    <xsl:value-of select="$outprop"/>: <xsl:apply-templates select="/fragment/para"/>
  </xsl:template>

  <!-- This will only be matched in doc2.xml -->
  <xsl:template match="Ref">
    <xsl:apply-templates select="document(@file)/fragment/para"/>
  </xsl:template>

  <!-- This will only be matched in doc1.xml -->
  <xsl:template match="text()">
    <xsl:value-of select="normalize-space(.)"/>
  </xsl:template>

</xsl:stylesheet>
