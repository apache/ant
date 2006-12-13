<xsl:stylesheet	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
	xmlns:lxslt="http://xml.apache.org/xslt"
	xmlns:redirect="org.apache.xalan.xslt.extensions.Redirect"
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