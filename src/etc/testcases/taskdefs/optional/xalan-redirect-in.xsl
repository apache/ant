<?xml version="1.0"?>
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
<xsl:stylesheet	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
	xmlns:lxslt="http://xml.apache.org/xslt"
        xmlns:redirect="http://xml.apache.org/xalan/redirect"
	extension-element-prefixes="redirect">
<!--
This is a test to ensure that systemid is set correctly
for a xsl...the behavior might be dependent on Xalan1
and Xalan2...this will be a problem to erase the files :(
Can take as a systemid the base for the xsl document or
the base or the JVM working dir just like: new File("xalan-redirect-out.tmp")
-->	
<xsl:param name="xalan-version" select="'x'"/>

<xsl:template match="/">
<redirect:write file="./xalan{$xalan-version}-redirect-out.tmp">
	<test>This should be written to the file</test>
</redirect:write>
</xsl:template>

</xsl:stylesheet>
