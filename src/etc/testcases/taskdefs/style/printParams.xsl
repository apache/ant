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

<xsl:stylesheet
  version="1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:fo="http://www.w3.org/1999/XSL/Format">

<!-- get the xsl-parameter -->
<xsl:param name="set">set default value</xsl:param>
<xsl:param name="empty">empty default value</xsl:param>
<xsl:param name="undefined">undefined default value</xsl:param>

<!-- use the xsl-parameter -->
<xsl:template match="/">
set='<xsl:value-of select="$set"/>'
empty='<xsl:value-of select="$empty"/>'
undefined='<xsl:value-of select="$undefined"/>'
</xsl:template>

</xsl:stylesheet>
