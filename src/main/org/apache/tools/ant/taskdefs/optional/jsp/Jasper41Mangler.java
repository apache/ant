/*
 *  The Apache Software License, Version 1.1
 *
 *  Copyright (c) 2003 The Apache Software Foundation.  All rights
 *  reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions
 *  are met:
 *
 *  1. Redistributions of source code must retain the above copyright
 *  notice, this list of conditions and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright
 *  notice, this list of conditions and the following disclaimer in
 *  the documentation and/or other materials provided with the
 *  distribution.
 *
 *  3. The end-user documentation included with the redistribution, if
 *  any, must include the following acknowlegement:
 *  "This product includes software developed by the
 *  Apache Software Foundation (http://www.apache.org/)."
 *  Alternately, this acknowlegement may appear in the software itself,
 *  if and wherever such third-party acknowlegements normally appear.
 *
 *  4. The names "Ant" and "Apache Software
 *  Foundation" must not be used to endorse or promote products derived
 *  from this software without prior written permission. For written
 *  permission, please contact apache@apache.org.
 *
 *  5. Products derived from this software may not be called "Apache"
 *  nor may "Apache" appear in their names without prior written
 *  permission of the Apache Group.
 *
 *  THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 *  WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 *  OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 *  ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 *  USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 *  OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 *  OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 *  SUCH DAMAGE.
 *  ====================================================================
 *
 *  This software consists of voluntary contributions made by many
 *  individuals on behalf of the Apache Software Foundation.  For more
 *  information on the Apache Software Foundation, please see
 *  <http://www.apache.org/>.
 */


package org.apache.tools.ant.taskdefs.optional.jsp;

import java.io.File;

/**
 * this class implements the name mangling rules of the jasper in tomcat4.1.x
 * which is likely to remain for some time
 * @see org.apache.jasper.JspCompilationContext
 * @author Anil K. Vijendran
 * @author Harish Prabandham
 * @author Pierre Delisle
 * @author Costin Manolache
 * @author Steve Loughran (copied from JspCompilationContext)
 */
public class Jasper41Mangler implements JspMangler {


    /**
     * map from a jsp file to a java filename; does not do packages
     *
     * @param jspFile file
     * @return java filename
     */
    public String mapJspToJavaName(File jspFile) {
        String jspUri = jspFile.getAbsolutePath();
        int start = jspUri.lastIndexOf(File.separatorChar) + 1;
        int end = jspUri.length();
        StringBuffer modifiedClassName;
        modifiedClassName = new StringBuffer(jspUri.length() - start);
        if (!Character.isJavaIdentifierStart(jspUri.charAt(start)) ||
                jspUri.charAt(start) == '_') {
            // If the first char is not a start of Java identifier or is _
            // prepend a '_'.
            modifiedClassName.append('_');
        }
        for (int i = start; i < end; i++) {
            char ch = jspUri.charAt(i);
            if (Character.isJavaIdentifierPart(ch)) {
                modifiedClassName.append(ch);
            } else if (ch == '.') {
                modifiedClassName.append('_');
            } else {
                modifiedClassName.append(mangleChar(ch));
            }
        }
        return modifiedClassName.toString();
    }

    /**
     * Mangle the specified character to create a legal Java class name.
     */
    private static final String mangleChar(char ch) {

        String s = Integer.toHexString(ch);
        int nzeros = 5 - s.length();
        char[] result = new char[6];
        result[0] = '_';
        for (int i = 1; i <= nzeros; i++) {
            result[i] = '0';
        }
        for (int i = nzeros + 1, j = 0; i < 6; i++, j++) {
            result[i] = s.charAt(j);
        }
        return new String(result);
    }


    /**
     * taking in the substring representing the path relative to the source dir
     * return a new string representing the destination path
     * @todo
     */
    public String mapPath(String path) {
        return null;
    }

}
