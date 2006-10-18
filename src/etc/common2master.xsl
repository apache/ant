<?xml version="1.0" encoding="ISO-8859-1"?>
<!-- 
	This stylesheet can be used to generate a master buildfile from a common
	buildfile (see manual for <subant>).
	Foreach <target> in the common buildfile it generates a corresponding 
	target in the master buildfile for iterating over that target.
-->
<xsl:stylesheet
  version="1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

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

    <xsl:output indent="no" method="text" encoding="ISO-8859-1"/>
    <xsl:strip-space elements="*"/>


<xsl:template match="/">
    <xsl:apply-templates/>
</xsl:template>



<xsl:template match="project">
<![CDATA[
<project name="master"> 

    <macrodef name="iterate">
        <attribute name="target"/>
        <sequential>
            <subant target="@{target}">
                <fileset dir="modules" includes="*/build.xml"/>
            </subant>
        </sequential>
    </macrodef>
]]>
   
    <xsl:apply-templates/>
    
<![CDATA[
</project>
]]>
</xsl:template>


<xsl:template match="target">
    &lt;target name=&quot;<xsl:value-of select="@name"/>&quot;<xsl:if test="@description"> description=&quot;<xsl:value-of select="@description"/>&quot;</xsl:if>&gt;
        &lt;iterate target=&quot;<xsl:value-of select="@name"/>&quot;/&gt;
    &lt;/target&gt;
</xsl:template>


<xsl:template match="text()"/>



</xsl:stylesheet>

