<%@ page language="java" %>
<%@ taglib uri="/WEB-INF/tlds/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/tlds/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/tlds/struts-template.tld" prefix="template" %>
<html:html locale="true">
<head>
<title>shouldnt compile</title>
<html:base/>
</head>
<body>

This page should not compile because refers to TLDs that arent around.

</body>

</html:html>
