<!--
 
 Sample stylesheet to be used with JProbe 3.0 XML output.
 
 It creates a set of HTML files a la javadoc where you can browse easily
 through all packages and classes.
 
 It is best used with JProbe Coverage Ant task that gives you the benefit
 of a reference classpath so that you have the list of classes/methods
 that are not used at all in a given classpath.
 
 @author Stephane Bailliez <a href="sbailliez@imediation.com"/>
 
-->
<xsl:stylesheet	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
	xmlns:lxslt="http://xml.apache.org/xslt"
	xmlns:redirect="org.apache.xalan.xslt.extensions.Redirect"
	extension-element-prefixes="redirect">

<xsl:output	method="html" indent="yes"/>
<xsl:decimal-format decimal-separator="." grouping-separator="," />

<!-- default output directory is current directory -->
<xsl:param name="output.dir" select="'.'"/>

<!-- ======================================================================
	Root element
    ======================================================================= -->
<xsl:template match="/snapshot">
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
	<xsl:apply-templates select="./package" mode="write"/>
</xsl:template>

<!-- =======================================================================
	Frameset definition. Entry point for the report.
	3 frames: packageListFrame, classListFrame, classFrame
    ======================================================================= -->
<xsl:template name="index.html">
<html>
	<head><title>Coverage Results.</title></head>
	<frameset cols="20%,80%">
		<frameset rows="30%,70%">
			<frame src="overview-frame.html" name="packageListFrame"/>
			<frame src="allclasses-frame.html" name="classListFrame"/>
		</frameset>
		<frame src="overview-summary.html" name="classFrame"/>
	</frameset>
	<noframes>
		<h2>Frame Alert</h2>
		<p>
		This document is designed to be viewed using the frames feature. If you see this message, you are using a non-frame-capable web client.
		</p>
	</noframes>
</html>
</xsl:template>

<!-- =======================================================================
	Stylesheet CSS used
    ======================================================================= -->
<xsl:template name="stylesheet.css">
	BODY {
		font:normal 68% verdana,arial,helvetica;
		color:#000000;
	}
	TD {
		FONT-SIZE: 68%
	}
	TH {
		FONT-SIZE: 68%;font-weight:bold;
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
</xsl:template>

<!-- =======================================================================
	List of all classes in all packages
	This will be the first page in the classListFrame
    ======================================================================= -->
<xsl:template match="snapshot" mode="all.classes">
	<html>
		<head>
			<xsl:call-template name="create.stylesheet.link"/>
		</head>
		<body>
			<h2>Classes</h2>
			<p>
			<table width="100%">
				<xsl:for-each select="package/class">
					<xsl:sort select="@name"/>
				    <xsl:variable name="package.name" select="(ancestor::package)[last()]/@name"/>
				    <xsl:variable name="link">
						<xsl:if test="not($package.name='')">
							<xsl:value-of select="translate($package.name,'.','/')"/><xsl:text>/</xsl:text>
						</xsl:if><xsl:value-of select="@name"/><xsl:text>.html</xsl:text>
				    </xsl:variable>
					<tr>
						<td nowrap="nowrap">
							<a target="classFrame" href="{$link}"><xsl:value-of select="@name"/></a>
						</td>
					</tr>
				</xsl:for-each>
			</table>
			</p>
		</body>
	</html>
</xsl:template>

<!-- list of all packages -->
<xsl:template match="snapshot" mode="all.packages">
	<html>
		<head>
			<xsl:call-template name="create.stylesheet.link"/>
		</head>
		<body>
			<h2><a href="overview-summary.html" target="classFrame">Home</a></h2>
			<h2>Packages</h2>
			<p>
			<table width="100%">
				<xsl:for-each select="package">
					<xsl:sort select="@name" order="ascending"/>
					<tr>
						<td nowrap="nowrap">
							<a href="{translate(@name,'.','/')}/package-summary.html" target="classFrame">
								<xsl:value-of select="@name"/>
							</a>
						</td>
					</tr>
				</xsl:for-each>
			</table>
			</p>
		</body>
	</html>
</xsl:template>

<!-- overview of statistics in packages -->
<xsl:template match="snapshot" mode="overview.packages">
	<html>
		<head>
			<xsl:call-template name="create.stylesheet.link"/>
		</head>
		<body onload="open('allclasses-frame.html','classListFrame')">
		<xsl:call-template name="pageHeader"/>
		<h2>Summary</h2>
		<table border="0" cellpadding="5" cellspacing="2" width="95%">
			<tr bgcolor="#A6CAF0" valign="top">
				<th width="10%" nowrap="nowrap">Date</th>
				<th width="10%" nowrap="nowrap">Elapsed time</th>
				<th width="10%" nowrap="nowrap">Reported Classes</th>
				<th width="10%" nowrap="nowrap">Methods Hit</th>
				<th width="10%" nowrap="nowrap">Lines Hit</th>
			</tr>
			<tr bgcolor="#EEEEE" valign="top">
				<td nowrap="nowrap"><xsl:value-of select="execution_log/@program_start"/></td>
				<td><xsl:value-of select="format-number(execution_log/@elapsed_time div 1000,'0.0')"/>secs</td>
				<td><xsl:value-of select="count(package/class)"/></td>
				<td><xsl:value-of select="format-number(cov.data/@hit_methods div cov.data/@total_methods,'0.0%')"/></td>
				<td><xsl:value-of select="format-number(cov.data/@hit_lines div cov.data/@total_lines,'0.0%')"/></td>
			</tr>
		</table>
		<table border="0" width="95%">
		<tr>
		<td	style="text-align: justify;">
		To ensure accurate test runs on Java applications, developers need to know how much of
		the code has been tested, and where to find any untested code. Coverage helps you
		locate untested code, and measure precisely how much code has been exercised.
		The result is a higher quality application in a shorter period of time.
		<p/>
		</td>
		</tr>
		</table>

		<h2>Packages</h2>
		<table border="0" cellpadding="5" cellspacing="2" width="95%">
			<xsl:apply-templates select="package[1]" mode="stats.header"/>
			<!-- display packages and sort them via their coverage rate -->
			<xsl:for-each select="package">
				<xsl:sort data-type="number" select="cov.data/@hit_lines div cov.data/@total_lines"/>
				<tr bgcolor="#EEEEE" valign="top">
					<td><a href="{translate(@name,'.','/')}/package-summary.html"><xsl:value-of select="@name"/></a></td>
					<td><xsl:value-of select="format-number(cov.data/@hit_methods div cov.data/@total_methods,'0.0%')"/></td>
					<td><xsl:value-of select="format-number(cov.data/@hit_lines div cov.data/@total_lines,'0.0%')"/></td>
				</tr>
			</xsl:for-each>
		</table>
		</body>
		</html>
</xsl:template>

<!--
 detailed info for a package. It will output the list of classes
, the summary page, and the info for each class
-->
<xsl:template match="package" mode="write">
	<xsl:variable name="package.dir">
		<xsl:if test="not(@name = '')"><xsl:value-of select="translate(@name,'.','/')"/></xsl:if>
		<xsl:if test="@name = ''">.</xsl:if>
	</xsl:variable>

	<!-- create a classes-list.html in the package directory -->
	<redirect:write file="{$output.dir}/{$package.dir}/package-frame.html">
		<xsl:apply-templates select="." mode="classes.list"/>
	</redirect:write>
	
	<!-- create a package-summary.html in the package directory -->
	<redirect:write file="{$output.dir}/{$package.dir}/package-summary.html">
		<xsl:apply-templates select="." mode="package.summary"/>
	</redirect:write>
	
	<!-- for each class, creates a @name.html -->
	<xsl:for-each select="class">
		<redirect:write file="{$output.dir}/{$package.dir}/{@name}.html">
			<xsl:apply-templates select="." mode="class.details"/>
		</redirect:write>
	</xsl:for-each>
</xsl:template>

<!-- list of classes in a package -->
<xsl:template match="package" mode="classes.list">
	<html>
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
				<xsl:for-each select="class">
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
	</html>
</xsl:template>

<!-- summary of a package -->
<xsl:template match="package" mode="package.summary">
	<HTML>
		<HEAD>
			<xsl:call-template name="create.stylesheet.link">
				<xsl:with-param name="package.name" select="@name"/>
			</xsl:call-template>
		</HEAD>
		<!-- when loading this package, it will open the classes into the frame -->
		<BODY onload="open('package-frame.html','classListFrame')">
			<xsl:call-template name="pageHeader"/>
			<h3>Package <xsl:value-of select="@name"/></h3>
			
			<table border="0" cellpadding="5" cellspacing="2" width="95%">
				<xsl:apply-templates select="." mode="stats.header"/>
				<xsl:apply-templates select="." mode="stats"/>
			</table>
					
			<xsl:if test="count(class) &gt; 0">
				<H2>Classes</H2>
				<p>
				<table border="0" cellpadding="5" cellspacing="2" width="95%">
					<xsl:apply-templates select="." mode="stats.header"/>
					<xsl:apply-templates select="class" mode="stats">
						<xsl:sort data-type="number" select="cov.data/@hit_lines div cov.data/@total_lines"/>
					</xsl:apply-templates>
				</table>
				</p>
			</xsl:if>
		</BODY>
	</HTML>
</xsl:template>

<!-- details of a class -->
<xsl:template match="class" mode="class.details">
	<xsl:variable name="package.name" select="(ancestor::package)[last()]/@name"/>
	<HTML>
		<HEAD>
			<xsl:call-template name="create.stylesheet.link">
				<xsl:with-param name="package.name" select="$package.name"/>
			</xsl:call-template>
		</HEAD>
		<BODY>
			<xsl:call-template name="pageHeader"/>
			<H3>Class <xsl:if test="not($package.name = '')"><xsl:value-of select="$package.name"/>.</xsl:if><xsl:value-of select="@name"/></H3>

			<!-- class summary -->
			<table border="0" cellpadding="5" cellspacing="2" width="95%">
				<xsl:apply-templates select="." mode="stats.header"/>
				<xsl:apply-templates select="." mode="stats"/>
			</table>
	
			<!-- details of methods -->
			<H2>Methods</H2>
			<p>
			<table border="0" cellpadding="5" cellspacing="2" width="95%">
				<xsl:apply-templates select="method[1]" mode="stats.header"/>
				<xsl:apply-templates select="method" mode="stats">
					<xsl:sort data-type="number" select="cov.data/@hit_lines div cov.data/@total_lines"/>
				</xsl:apply-templates>
			</table>
			
			</p>
		</BODY>
	</HTML>

</xsl:template>

<!-- Page HEADER -->
<xsl:template name="pageHeader">
	<h1>Coverage Results</h1>
	<table width="100%">
	<tr>
		<td align="left"></td>
		<td align="right">Designed for use with <a href='http://www.sitraka.com/jprobe'>Sitraka JProbe</a> and <a href='http://jakarta.apache.org'>Ant</a>.</td>
	</tr>
	</table>
	<hr size="1"/>
</xsl:template>

<xsl:template name="table.header">
	<tr bgcolor="#A6CAF0" valign="top">
		<th width="80%">Name</th>
		<th width="10%" nowrap="nowrap">Methods Hit</th>
		<th width="10%" nowrap="nowrap">Lines Hit</th>
	</tr>
</xsl:template>

<xsl:template match="method" mode="stats.header">
	<tr bgcolor="#A6CAF0" valign="top">
		<th width="90%">Name</th>
		<th width="10%" nowrap="nowrap">Lines Hit</th>
	</tr>
</xsl:template>
<xsl:template match="method" mode="stats">
	<tr bgcolor="#EEEEE" valign="top">
		<td><xsl:value-of select="@name"/></td>
		<td>
		<xsl:value-of select="format-number(cov.data/@hit_lines div cov.data/@total_lines,'0.0%')"/>
		</td>
	</tr>
</xsl:template>

<xsl:template match="package|class" mode="stats.header">
	<tr bgcolor="#A6CAF0" valign="top">
		<th width="80%">Name</th>
		<th width="10%" nowrap="nowrap">Methods Hit</th>
		<th width="10%" nowrap="nowrap">Lines Hit</th>
	</tr>
</xsl:template>
<xsl:template match="package|class" mode="stats">
	<tr bgcolor="#EEEEE" valign="top">
		<td><xsl:value-of select="@name"/></td>
		<td><xsl:value-of select="format-number(cov.data/@hit_methods div cov.data/@total_methods,'0.0%')"/></td>
		<td><xsl:value-of select="format-number(cov.data/@hit_lines div cov.data/@total_lines,'0.0%')"/></td>
	</tr>
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

</xsl:stylesheet>


