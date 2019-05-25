/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.tools.ant.taskdefs.optional.jsp;

import java.io.File;

/**
 * this class implements the name mangling rules of the jasper in tomcat4.1.x
 * which is likely to remain for some time
 * @see "org.apache.jasper.JspCompilationContext"
 */
public class Jasper41Mangler implements JspMangler {

    /**
     * map from a jsp file to a java filename; does not do packages
     *
     * @param jspFile file
     * @return java filename
     */
    @Override
    public String mapJspToJavaName(File jspFile) {
        String jspUri = jspFile.getAbsolutePath();
        int start = jspUri.lastIndexOf(File.separatorChar) + 1;
        StringBuilder modifiedClassName = new StringBuilder(jspUri.length() - start);
        if (!Character.isJavaIdentifierStart(jspUri.charAt(start))
            || jspUri.charAt(start) == '_') {
            // If the first char is not a start of Java identifier or is _
            // prepend a '_'.
            modifiedClassName.append('_');
        }
        for (final char ch : jspUri.substring(start).toCharArray()) {
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
    private static String mangleChar(char ch) {
        // CheckStyle:MagicNumber OFF
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
        // CheckStyle:MagicNumber ON
    }

    /**
     * taking in the substring representing the path relative to the source dir
     * return a new string representing the destination path
     * @param path not used.
     * @return null as this is not implemented.
     * @todo
     */
    @Override
    public String mapPath(String path) {
        return null;
    }

}
