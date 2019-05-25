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

import org.apache.tools.ant.util.StringUtils;

/**
 * This is a class derived from the Jasper code
 * (org.apache.jasper.compiler.CommandLineCompiler) to map from a JSP filename
 * to a valid Java classname.
 *
 */
public class JspNameMangler implements JspMangler {

    // CheckStyle:ConstantNameCheck OFF - bc

    /**
     * this is the list of keywords which can not be used as classnames
     */
    public static final String[] keywords = { //NOSONAR
            "assert",
            "abstract", "boolean", "break", "byte",
            "case", "catch", "char", "class",
            "const", "continue", "default", "do",
            "double", "else", "extends", "final",
            "finally", "float", "for", "goto",
            "if", "implements", "import",
            "instanceof", "int", "interface",
            "long", "native", "new", "package",
            "private", "protected", "public",
            "return", "short", "static", "super",
            "switch", "synchronized", "this",
            "throw", "throws", "transient",
            "try", "void", "volatile", "while"
            };

    // CheckStyle:ConstantNameCheck ON

    /**
     * map from a jsp file to a java filename; does not do packages
     *
     * @param jspFile file
     * @return java filename
     */
    @Override
    public String mapJspToJavaName(File jspFile) {
        return mapJspToBaseName(jspFile) + ".java";
    }


    /**
     * map from a jsp file to a base name; does not deal with extensions
     *
     * @param jspFile jspFile file
     * @return extensionless potentially remapped name
     */
    private String mapJspToBaseName(File jspFile) {
        String className = stripExtension(jspFile);

        // since we don't mangle extensions like the servlet does,
        // we need to check for keywords as class names
        for (String keyword : keywords) {
            if (className.equals(keyword)) {
                className += "%";
                break;
            }
        }

        // Fix for invalid characters. If you think of more add to the list.
        StringBuilder modifiedClassName = new StringBuilder(className.length());
        // first char is more restrictive than the rest
        char firstChar = className.charAt(0);
        if (Character.isJavaIdentifierStart(firstChar)) {
            modifiedClassName.append(firstChar);
        } else {
            modifiedClassName.append(mangleChar(firstChar));
        }
        // this is the rest
        for (final char subChar : className.substring(1).toCharArray()) {
            if (Character.isJavaIdentifierPart(subChar)) {
                modifiedClassName.append(subChar);
            } else {
                modifiedClassName.append(mangleChar(subChar));
            }
        }
        return modifiedClassName.toString();
    }


    /**
     * get short filename from file
     *
     * @param jspFile file in
     * @return file without any jsp extension
     */
    private String stripExtension(File jspFile) {
        return StringUtils.removeSuffix(jspFile.getName(), ".jsp");
    }


    /**
     * definition of the char escaping algorithm
     *
     * @param ch char to mangle
     * @return mangled string; 5 digit hex value
     */
    private static String mangleChar(char ch) {
        // CheckStyle:MagicNumber OFF
        if (ch == File.separatorChar) {
            ch = '/';
        }
        String s = Integer.toHexString(ch);
        int nzeros = 5 - s.length();
        char[] result = new char[6];
        result[0] = '_';
        for (int i = 1; i <= nzeros; ++i) {
            result[i] = '0';
        }
        int resultIndex = 0;
        for (int i = nzeros + 1; i < 6; ++i) {
            result[i] = s.charAt(resultIndex++);
        }
        return new String(result);
        // CheckStyle:MagicNumber ON
    }

    /**
     * taking in the substring representing the path relative to the source dir
     * return a new string representing the destination path
     * not supported, as jasper in tomcat4.0 doesn't either
     * @param path not used
     * @return null always.
     */
    @Override
    public String mapPath(String path) {
        return null;
    }
}

