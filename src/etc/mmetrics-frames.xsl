<xsl:stylesheet	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
	xmlns:lxslt="http://xml.apache.org/xslt"
	xmlns:xalan="http://xml.apache.org/xalan"
	xmlns:redirect="org.apache.xalan.xslt.extensions.Redirect"
	exclude-result-prefixes="xalan"
	extension-element-prefixes="redirect">

<!--
	For xalan 1.2.2
	xmlns:xalan-nodeset="org.apache.xalan.xslt.extensions.Nodeset"
-->
<xsl:output	method="html" indent="yes"/>
<xsl:decimal-format decimal-separator="." grouping-separator="," />

<!--
    Xalan redirect extension writes relative file based on the parent directory
    from the main output file, unfortunately, this is never set and you have
    to do it yourself on the API. The code that does it in command line was
    commented out in Xalan 1.2.2 :-(
    
    Therefore I will use a stylesheet param for the output directory.
    
    This has to be invoked as follows from the command line:
    
    java -classpath bsf.jar;xalan.jar;xerces.jar org.apache.xalan.xslt.Process -IN metrics.xml -XSL metrics.xsl -PARAM output.dir './report'
-->
<xsl:param name="output.dir" select="'.'"/>

<!-- default max value for the metrics -->
<xsl:param name="vg.max" select="10"/>
<xsl:param name="loc.max" select="1000"/>
<xsl:param name="dit.max" select="10"/>
<xsl:param name="noa.max" select="250"/>
<xsl:param name="nrm.max" select="50"/>
<xsl:param name="nlm.max" select="250"/>
<xsl:param name="wmc.max" select="250"/>
<xsl:param name="rfc.max" select="50"/>
<xsl:param name="dac.max" select="10"/>
<xsl:param name="fanout.max" select="10"/>
<xsl:param name="cbo.max" select="15"/>
<xsl:param name="lcom.max" select="10"/>
<xsl:param name="nocl.max" select="10"/>


<!-- create a tree fragment -->
<xsl:variable name="doctree.var">
    <xsl:element name="classes">
        <xsl:for-each select=".//class">
            <xsl:element name="class">
                <xsl:attribute name="package">
                    <xsl:value-of select="(ancestor::package)[last()]/@name"/>
                </xsl:attribute>
                <xsl:copy-of select="@*"/>
                <xsl:attribute name="name">
					<xsl:apply-templates select="." mode="class.name"/>                
                </xsl:attribute>
                <xsl:copy-of select="method"/>
            </xsl:element>
        </xsl:for-each>
    </xsl:element>
</xsl:variable>

<xsl:variable name="doctree" select="xalan:nodeset($doctree.var)"/>

<xsl:template match="metrics">

	<!-- create the index.html -->
	<redirect:write file="{$output.dir}/index.html">
		<xsl:call-template name="index.html"/>
	</redirect:write>

	<!-- create the stylesheet.css -->
	<redirect:write file="{$output.dir}/stylesheet.css">
		<xsl:call-template name="stylesheet.css"/>
	</redirect:write>

	<!-- create the overview-packages.html at the root -->
	<redirect:write file="{$output.dir}/overview-summary.html">
		<xsl:apply-templates select="." mode="overview.packages"/>
	</redirect:write>

	<!-- create the all-packages.html at the root -->
	<redirect:write file="{$output.dir}/overview-frame.html">
		<xsl:apply-templates select="." mode="all.packages"/>
	</redirect:write>
	
	<!-- create the all-classes.html at the root -->
	<redirect:write file="{$output.dir}/allclasses-frame.html">
		<xsl:apply-templates select="." mode="all.classes"/>
	</redirect:write>
	
	<!-- process all packages -->
	<xsl:apply-templates select=".//package"/>
</xsl:template>


<xsl:template match="package">
	<xsl:variable name="package.name" select="@name"/>
	<xsl:variable name="package.dir">
		<xsl:if test="not($package.name = 'unnamed package')"><xsl:value-of select="translate($package.name,'.','/')"/></xsl:if>
		<xsl:if test="$package.name = 'unnamed package'">.</xsl:if>
	</xsl:variable>	
	<xsl:message>Processing package <xsl:value-of select="@name"/> in <xsl:value-of select="$output.dir"/>
	</xsl:message>
	<!-- create a classes-list.html in the package directory -->
	<redirect:write file="{$output.dir}/{$package.dir}/package-frame.html">
		<xsl:apply-templates select="." mode="classes.list"/>
	</redirect:write>
	
	<!-- create a package-summary.html in the package directory -->
	<redirect:write file="{$output.dir}/{$package.dir}/package-summary.html">
		<xsl:apply-templates select="." mode="package.summary"/>
	</redirect:write>
	
	<!-- for each class, creates a @name.html -->
	<!-- @bug there will be a problem with inner classes having the same name, it will be overwritten -->
	<xsl:for-each select="$doctree/classes/class[@package = current()/@name]">
	    <!--Processing <xsl:value-of select="$class.name"/><xsl:text>&#10;</xsl:text> -->
		<redirect:write file="{$output.dir}/{$package.dir}/{@name}.html">
			<xsl:apply-templates select="." mode="class.details"/>
		</redirect:write>
	</xsl:for-each>
</xsl:template>

<!-- little trick to compute the classname for inner and non inner classes -->
<!-- this is all in one line to avoid CRLF in the name -->
<xsl:template match="class" mode="class.name">
    <xsl:if test="parent::class"><xsl:apply-templates select="parent::class" mode="class.name"/>.<xsl:value-of select="@name"/></xsl:if><xsl:if test="not(parent::class)"><xsl:value-of select="@name"/></xsl:if>
</xsl:template>


<xsl:template name="index.html">
<HTML>
	<HEAD><TITLE>Metrics Results.</TITLE></HEAD>
	<FRAMESET cols="20%,80%">
		<FRAMESET rows="30%,70%">
			<FRAME src="overview-frame.html" name="packageListFrame"/>
			<FRAME src="allclasses-frame.html" name="classListFrame"/>
		</FRAMESET>
		<FRAME src="overview-summary.html" name="classFrame"/>
	</FRAMESET>
	<noframes>
		<H2>Frame Alert</H2>
		<P>
		This document is designed to be viewed using the frames feature. If you see this message, you are using a non-frame-capable web client.
		</P>
	</noframes>
</HTML>
</xsl:template>

<!-- this is the stylesheet css to use for nearly everything -->
<xsl:template name="stylesheet.css">
BODY {
	font:normal 68% verdana,arial,helvetica;
	color:#000000;
}
TD {
	FONT-SIZE: 68%
}
P {
	line-height:1.5em;
	margin-top:0.5em; margin-bottom:1.0em;
}
H1 {
	MARGIN: 0px 0px 5px; FONT: 165% verdana,arial,helvetica
}
H2 {
	MARGIN-TOP: 1em; MARGIN-BOTTOM: 0.5em; FONT: bold 125% verdana,arial,helvetica
}
H3 {
	MARGIN-BOTTOM: 0.5em; FONT: bold 115% verdana,arial,helvetica
}
H4 {
	MARGIN-BOTTOM: 0.5em; FONT: bold 100% verdana,arial,helvetica
}
H5 {
	MARGIN-BOTTOM: 0.5em; FONT: bold 100% verdana,arial,helvetica
}
H6 {
	MARGIN-BOTTOM: 0.5em; FONT: bold 100% verdana,arial,helvetica
}
.Error {
	font-weight:bold; color:red;
}

</xsl:template>

<!-- print the metrics of the class -->
<xsl:template match="class" mode="class.details">
	<!--xsl:variable name="package.name" select="(ancestor::package)[last()]/@name"/-->
	<xsl:variable name="package.name" select="@package"/>
	<HTML>
		<HEAD>
			<xsl:call-template name="create.stylesheet.link">
				<xsl:with-param name="package.name" select="$package.name"/>
			</xsl:call-template>
		</HEAD>
		<BODY>
			<xsl:call-template name="pageHeader"/>	
			<H3>Class <xsl:if test="not($package.name = 'unnamed package')"><xsl:value-of select="$package.name"/>.</xsl:if><xsl:value-of select="@name"/></H3>

			
			<table border="0" cellpadding="5" cellspacing="2" width="95%">
				<xsl:call-template name="all.metrics.header"/>				
				<xsl:apply-templates select="." mode="print.metrics"/>
			</table>
	
			<H2>Methods</H2>
			<p>
			<table border="0" cellpadding="5" cellspacing="2" width="95%">
				<xsl:call-template name="method.metrics.header"/>
				<xsl:apply-templates select="method" mode="print.metrics"/>
			</table>			
			
			</p>
		</BODY>
	</HTML>
</xsl:template>


<!-- list of classes in a package -->
<xsl:template match="package" mode="classes.list">
	<HTML>
		<HEAD>
			<xsl:call-template name="create.stylesheet.link">
				<xsl:with-param name="package.name" select="@name"/>
			</xsl:call-template>
		</HEAD>
		<BODY>
			<table width="100%">
				<tr>
					<td nowrap="nowrap">
						<H2><a href="package-summary.html" target="classFrame"><xsl:value-of select="@name"/></a></H2>
					</td>
				</tr>
			</table>
	
			<H2>Classes</H2>
			<p>
			<TABLE WIDTH="100%">
				<!-- xalan-nodeset:nodeset for Xalan 1.2.2 -->
    		    <xsl:for-each select="$doctree/classes/class[@package = current()/@name]">
    		        <xsl:sort select="@name"/>
					<tr>
						<td nowrap="nowrap">
							<a href="{@name}.html" target="classFrame"><xsl:value-of select="@name"/></a>
						</td>
					</tr>    		        
    		    </xsl:for-each>
			</TABLE>
			</p>
		</BODY>
	</HTML>
</xsl:template>


<!--
	Creates an all-classes.html file that contains a link to all package-summary.html
	on each class.
-->
<xsl:template match="metrics" mode="all.classes">
	<html>
		<head>
			<xsl:call-template name="create.stylesheet.link">
				<xsl:with-param name="package.name" select="''"/>
			</xsl:call-template>
		</head>
		<body>
			<h2>Classes</h2>
			<p>
			<table width="100%">
			    <xsl:for-each select="$doctree/classes/class">
			        <xsl:sort select="@name"/>
			        <xsl:apply-templates select="." mode="all.classes"/>
			    </xsl:for-each>
			</table>
			</p>
		</body>
	</html>
</xsl:template>

<xsl:template match="class" mode="all.classes">
    <xsl:variable name="package.name" select="@package"/>
    <xsl:variable name="class.name" select="@name"/>
	<tr>
		<td nowrap="nowrap">
			<a target="classFrame">
				<xsl:attribute name="href">
					<xsl:if test="not($package.name='unnamed package')">
						<xsl:value-of select="translate($package.name,'.','/')"/><xsl:text>/</xsl:text>
					</xsl:if>
					<xsl:value-of select="$class.name"/><xsl:text>.html</xsl:text>
				</xsl:attribute>
				<xsl:value-of select="$class.name"/>
			</a>
		</td>
	</tr>
</xsl:template>

<!--
	Creates an html file that contains a link to all package-summary.html files on
	each package existing on testsuites.
	@bug there will be a problem here, I don't know yet how to handle unnamed package :(
-->
<xsl:template match="metrics" mode="all.packages">
	<html>
		<head>
			<xsl:call-template name="create.stylesheet.link">
				<xsl:with-param name="package.name" select="./package/@name"/>
			</xsl:call-template>
		</head>
		<body>
			<h2><a href="overview-summary.html" target="classFrame">Home</a></h2>
			<h2>Packages</h2>
			<p>
				<table width="100%">
					<xsl:apply-templates select=".//package[not(./@name = 'unnamed package')]" mode="all.packages">
						<xsl:sort select="@name"/>
					</xsl:apply-templates>
				</table>
			</p>
		</body>
	</html>
</xsl:template>

<xsl:template match="package" mode="all.packages">
	<tr>
		<td nowrap="nowrap">
			<a href="{translate(@name,'.','/')}/package-summary.html" target="classFrame">
				<xsl:value-of select="@name"/>
			</a>
		</td>
	</tr>
</xsl:template>


<xsl:template match="metrics" mode="overview.packages">
	<html>
		<head>
			<xsl:call-template name="create.stylesheet.link">
				<xsl:with-param name="package.name" select="''"/>
			</xsl:call-template>
		</head>
		<body onload="open('allclasses-frame.html','classListFrame')">
		<xsl:call-template name="pageHeader"/>
		<h2>Summary</h2>
		<table border="0" cellpadding="5" cellspacing="2" width="95%">
		<tr bgcolor="#A6CAF0" valign="top">
			<td><b>V(G)</b></td>
			<td><b>LOC</b></td>
			<td><b>DIT</b></td>
			<td><b>NOA</b></td>
			<td><b>NRM</b></td>
			<td><b>NLM</b></td>
			<td><b>WMC</b></td>
			<td><b>RFC</b></td>
			<td><b>DAC</b></td>
			<td><b>FANOUT</b></td>
			<td><b>CBO</b></td>
			<td><b>LCOM</b></td>
			<td><b>NOCL</b></td>
		</tr>
		<xsl:apply-templates select="." mode="print.metrics"/>
		</table>
		<table border="0" width="95%">
		<tr>
		<td	style="text-align: justify;">
		Note: Metrics evaluate the quality of software by analyzing	the	program	source and quantifying
		various	kind of	complexity.	Complexity is a	common source of problems and defects in software.
		High complexity	makes it more difficult	to develop,	understand,	maintain, extend, test and debug
		a program.
		<p/>
		The primary use of metrics is to focus your	attention on those parts of code that potentially are
		complexity hot spots. Once the complex areas your program have been uncovered, you can take remedial
		actions.
		For additional information about metrics and their meaning, please consult
		Metamata Metrics manual.
		</td>
		</tr>
		</table>

		<h2>Packages</h2>
		<table border="0" cellpadding="5" cellspacing="2" width="95%">
			<xsl:call-template name="all.metrics.header"/>
			<xsl:for-each select=".//package[not(@name = 'unnamed package')]">
				<xsl:sort select="@name" order="ascending"/>				
				<xsl:apply-templates select="." mode="print.metrics"/>
			</xsl:for-each>
		</table>
		<!-- @bug there could some classes at this level (classes in unnamed package) -->
		</body>
		</html>
</xsl:template>

<xsl:template match="package" mode="package.summary">
	<HTML>
		<HEAD>
			<xsl:call-template name="create.stylesheet.link">
				<xsl:with-param name="package.name" select="@name"/>
			</xsl:call-template>
		</HEAD>
		<body onload="open('package-frame.html','classListFrame')">
			<xsl:call-template name="pageHeader"/>
			<!-- create an anchor to this package name -->
			<h3>Package <xsl:value-of select="@name"/></h3>
			
			<table border="0" cellpadding="5" cellspacing="2" width="95%">
				<xsl:call-template name="all.metrics.header"/>
				<xsl:apply-templates select="." mode="print.metrics"/>
			</table>
			
			<table border="0" width="95%">
			<tr>
			<td	style="text-align: justify;">
			Note: Metrics evaluate the quality of software by analyzing	the	program	source and quantifying
			various	kind of	complexity.	Complexity is a	common source of problems and defects in software.
			High complexity	makes it more difficult	to develop,	understand,	maintain, extend, test and debug
			a program.
			<p/>
			The primary use of metrics is to focus your	attention on those parts of code that potentially are
			complexity hot spots. Once the complex areas your program have been uncovered, you can take remedial
			actions.
			For additional information about metrics and their meaning, please consult
			Metamata Metrics manual.
			</td>
			</tr>
			</table>
			
			
			<xsl:if test="count($doctree/classes/class[@package = current()/@name]) &gt; 0">
				<H2>Classes</H2>
				<p>
				<table border="0" cellpadding="5" cellspacing="2" width="95%">
					<xsl:call-template name="all.metrics.header"/>
					<xsl:for-each select="$doctree/classes/class[@package = current()/@name]">
			        	<xsl:sort select="@name"/>
			        	<xsl:apply-templates select="." mode="print.metrics"/>
			    </xsl:for-each>
				</table>
				</p>
			</xsl:if>
		</body>
	</HTML>
</xsl:template>


<!--
    transform string like a.b.c to ../../../
    @param path the path to transform into a descending directory path
-->
<xsl:template name="path">
	<xsl:param name="path"/>
	<xsl:if test="contains($path,'.')">
		<xsl:text>../</xsl:text>	
		<xsl:call-template name="path">
			<xsl:with-param name="path"><xsl:value-of select="substring-after($path,'.')"/></xsl:with-param>
		</xsl:call-template>	
	</xsl:if>
	<xsl:if test="not(contains($path,'.')) and not($path = '')">
		<xsl:text>../</xsl:text>	
	</xsl:if>
</xsl:template>


<!-- create the link to the stylesheet based on the package name -->
<xsl:template name="create.stylesheet.link">
	<xsl:param name="package.name"/>
	<LINK REL ="stylesheet" TYPE="text/css" TITLE="Style"><xsl:attribute name="href"><xsl:if test="not($package.name = 'unnamed package')"><xsl:call-template name="path"><xsl:with-param name="path" select="$package.name"/></xsl:call-template></xsl:if>stylesheet.css</xsl:attribute></LINK>
</xsl:template>


<!-- Page HEADER -->
<xsl:template name="pageHeader">
	<h1>Metrics Results</h1>
	<table width="100%">
	<tr>
		<td align="left"></td>
		<td align="right">Designed for use with <a href='http://www.webgain.com/products/quality_analyzer/'>Webgain QA/Metamata Metrics</a> and <a href='http://jakarta.apache.org'>Ant</a>.</td>
	</tr>
	</table>
	<hr size="1"/>
</xsl:template>

<!-- class header -->
<xsl:template name="all.metrics.header">
	<tr bgcolor="#A6CAF0" valign="top">
		<td width="80%"><b>Name</b></td>
		<td nowrap="nowrap"><b>V(G)</b></td>
		<td><b>LOC</b></td>
		<td><b>DIT</b></td>
		<td><b>NOA</b></td>
		<td><b>NRM</b></td>
		<td><b>NLM</b></td>
		<td><b>WMC</b></td>
		<td><b>RFC</b></td>
		<td><b>DAC</b></td>
		<td><b>FANOUT</b></td>
		<td><b>CBO</b></td>
		<td><b>LCOM</b></td>
		<td><b>NOCL</b></td>
	</tr>
</xsl:template>

<!-- method header -->
<xsl:template name="method.metrics.header">
	<tr bgcolor="#A6CAF0" valign="top">
		<td width="80%"><b>Name</b></td>
		<td nowrap="nowrap"><b>V(G)</b></td>
		<td><b>LOC</b></td>
		<td><b>FANOUT</b></td>
		<td><b>CBO</b></td>
	</tr>
</xsl:template>

<!-- method information -->
<xsl:template match="method" mode="print.metrics">
	<tr bgcolor="#EEEEE" valign="top">
		<td><xsl:apply-templates select="@name"/></td>
		<td><xsl:apply-templates select="@vg"/></td>
		<td><xsl:apply-templates select="@loc"/></td>
		<td><xsl:apply-templates select="@fanout"/></td>
		<td><xsl:apply-templates select="@cbo"/></td>
	</tr>
</xsl:template>

<!-- class information -->
<xsl:template match="class" mode="print.metrics">
	<tr bgcolor="#EEEEE" valign="top">		
		<td><a href="{@name}.html"><xsl:value-of select="@name"/></a></td>
		<td><xsl:apply-templates select="@vg"/></td>
		<td><xsl:apply-templates select="@loc"/></td>
		<td><xsl:apply-templates select="@dit"/></td>
		<td><xsl:apply-templates select="@noa"/></td>
		<td><xsl:apply-templates select="@nrm"/></td>
		<td><xsl:apply-templates select="@nlm"/></td>
		<td><xsl:apply-templates select="@wmc"/></td>
		<td><xsl:apply-templates select="@rfc"/></td>
		<td><xsl:apply-templates select="@dac"/></td>
		<td><xsl:apply-templates select="@fanout"/></td>
		<td><xsl:apply-templates select="@cbo"/></td>
		<td><xsl:apply-templates select="@lcom"/></td>
		<td><xsl:apply-templates select="@nocl"/></td>
	</tr>
</xsl:template>

<xsl:template match="file|package" mode="print.metrics">
	<tr bgcolor="#EEEEE" valign="top">		
		<td><xsl:value-of select="@name"/></td>
		<td><xsl:apply-templates select="@vg"/></td>
		<td><xsl:apply-templates select="@loc"/></td>
		<td><xsl:apply-templates select="@dit"/></td>
		<td><xsl:apply-templates select="@noa"/></td>
		<td><xsl:apply-templates select="@nrm"/></td>
		<td><xsl:apply-templates select="@nlm"/></td>
		<td><xsl:apply-templates select="@wmc"/></td>
		<td><xsl:apply-templates select="@rfc"/></td>
		<td><xsl:apply-templates select="@dac"/></td>
		<td><xsl:apply-templates select="@fanout"/></td>
		<td><xsl:apply-templates select="@cbo"/></td>
		<td><xsl:apply-templates select="@lcom"/></td>
		<td><xsl:apply-templates select="@nocl"/></td>
	</tr>
</xsl:template>

<xsl:template match="metrics" mode="print.metrics">
	<tr bgcolor="#EEEEE" valign="top">
	    <!-- the global metrics is the top package metrics -->
		<td><xsl:apply-templates select="./package/@vg"/></td>
		<td><xsl:apply-templates select="./package/@loc"/></td>
		<td><xsl:apply-templates select="./package/@dit"/></td>
		<td><xsl:apply-templates select="./package/@noa"/></td>
		<td><xsl:apply-templates select="./package/@nrm"/></td>
		<td><xsl:apply-templates select="./package/@nlm"/></td>
		<td><xsl:apply-templates select="./package/@wmc"/></td>
		<td><xsl:apply-templates select="./package/@rfc"/></td>
		<td><xsl:apply-templates select="./package/@dac"/></td>
		<td><xsl:apply-templates select="./package/@fanout"/></td>
		<td><xsl:apply-templates select="./package/@cbo"/></td>
		<td><xsl:apply-templates select="./package/@lcom"/></td>
		<td><xsl:apply-templates select="./package/@nocl"/></td>
	</tr>
</xsl:template>


<!-- how to display the metrics with their max value -->
<!-- @todo the max values must be external to the xsl -->

	<xsl:template match="@vg">
		<xsl:call-template name="display-value">
			<xsl:with-param	name="value" select="current()"/>
			<xsl:with-param	name="max" select="$vg.max"/>
		</xsl:call-template>
	</xsl:template>

	<xsl:template match="@loc">
		<xsl:call-template name="display-value">
			<xsl:with-param	name="value" select="current()"/>
			<xsl:with-param	name="max" select="$loc.max"/>
		</xsl:call-template>
	</xsl:template>
	
	<xsl:template match="@dit">
		<xsl:call-template name="display-value">
			<xsl:with-param	name="value" select="current()"/>
			<xsl:with-param	name="max" select="$dit.max"/>
		</xsl:call-template>
	</xsl:template>

	<xsl:template match="@noa">
		<xsl:call-template name="display-value">
			<xsl:with-param	name="value" select="current()"/>
			<xsl:with-param	name="max" select="$noa.max"/>
		</xsl:call-template>
	</xsl:template>

	<xsl:template match="@nrm">
		<xsl:call-template name="display-value">
			<xsl:with-param	name="value" select="current()"/>
			<xsl:with-param	name="max" select="$nrm.max"/>
		</xsl:call-template>
	</xsl:template>

	<xsl:template match="@nlm">
		<xsl:call-template name="display-value">
			<xsl:with-param	name="value" select="current()"/>
			<xsl:with-param	name="max" select="$nlm.max"/>
		</xsl:call-template>
	</xsl:template>

	<xsl:template match="@wmc">
		<xsl:call-template name="display-value">
			<xsl:with-param	name="value" select="current()"/>
			<xsl:with-param	name="max" select="$wmc.max"/>
		</xsl:call-template>
	</xsl:template>

	<xsl:template match="@rfc">
		<xsl:call-template name="display-value">
			<xsl:with-param	name="value" select="current()"/>
			<xsl:with-param	name="max" select="$rfc.max"/>
		</xsl:call-template>
	</xsl:template>

	<xsl:template match="@dac">
		<xsl:call-template name="display-value">
			<xsl:with-param	name="value" select="current()"/>
			<xsl:with-param	name="max" select="$dac.max"/>
		</xsl:call-template>
	</xsl:template>

	<xsl:template match="@fanout">
		<xsl:call-template name="display-value">
			<xsl:with-param	name="value" select="current()"/>
			<xsl:with-param	name="max" select="$fanout.max"/>
		</xsl:call-template>
	</xsl:template>

	<xsl:template match="@cbo">
		<xsl:call-template name="display-value">
			<xsl:with-param	name="value" select="current()"/>
			<xsl:with-param	name="max" select="$cbo.max"/>
		</xsl:call-template>
	</xsl:template>

	<xsl:template match="@lcom">
		<xsl:call-template name="display-value">
			<xsl:with-param	name="value" select="current()"/>
			<xsl:with-param	name="max" select="$lcom.max"/>
		</xsl:call-template>
	</xsl:template>

	<xsl:template match="@nocl">
		<xsl:call-template name="display-value">
			<xsl:with-param	name="value" select="current()"/>
			<xsl:with-param	name="max" select="$nocl.max"/>
		</xsl:call-template>
	</xsl:template>

	<xsl:template name="display-value">
		<xsl:param name="value"/>
		<xsl:param name="max"/>
		<xsl:if	test="$value > $max">
			<xsl:attribute name="class">Error</xsl:attribute>
		</xsl:if>
		<xsl:value-of select="$value"/>
	</xsl:template>

</xsl:stylesheet>
	
