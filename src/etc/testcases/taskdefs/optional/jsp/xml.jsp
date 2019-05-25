<?xml version="1.0" ?>
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
