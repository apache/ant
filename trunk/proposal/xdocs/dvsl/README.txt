This directory contains support for generating HTML task documentation
from the XDoclet generated XML.  DVSL
(http://jakarta.apache.org/velocity/dvsl/) is used for this purpose.  It has
many of the benefits of XSLT but uses Velocity as its template language.

This is in a subdirectory as it is effectively a sub-proposal demonstrating
one way the task XML can be transformed into HTML.

Directions:
1. Generate the XML task docs by running the build.xml in the parent
   directory.
2. Generate the HTML docs by running the build file in this directory.
   The output is written to ../build/docs/manual.

-Bill Burton <billb@progress.com>

