<?xml version="1.0" ?>
<!-- :mode=xml:indentSize=2 -->
<!-- note the lack of a language setting here. crimson whined when ISO-8859-1 was set,
	 that it thought it was loading a file of type ISO_8859_1 and
	 so there was a mismatch, even though the mismatch is only 
	 between hyphen types -->
<jsp:root
  xmlns:jsp="http://java.sun.com/JSP/Page"
  version="1.2"
  >
<jsp:directive.page language="java" />
<jsp:directive.page contentType="application/xml" />
<timestamp>
<jsp:expression>System.currentTimeMillis()</jsp:expression>
</timestamp>
</jsp:root>
