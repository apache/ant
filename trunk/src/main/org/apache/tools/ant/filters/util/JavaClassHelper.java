/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.tools.ant.filters.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.ConstantValue;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.JavaClass;

// CheckStyle:HideUtilityClassConstructorCheck OFF - bc
/**
 * Helper class that filters constants from a Java Class
 *
 */
public final class JavaClassHelper {
    /** System specific line separator. */
    private static final String LS = System.getProperty("line.separator");

    /**
     * Get the constants declared in a file as name=value
     *
     * @param bytes the class as a array of bytes
     * @return a StringBuffer contains the name=value pairs
     * @exception IOException if an error occurs
     */
    public static StringBuffer getConstants(byte[] bytes)
        throws IOException {
        final StringBuffer sb = new StringBuffer();
        final ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        final ClassParser parser = new ClassParser(bis, "");
        final JavaClass javaClass = parser.parse();
        final Field[] fields = javaClass.getFields();
        for (int i = 0; i < fields.length; i++) {
            final Field field = fields[i];
            if (field != null) {
                final ConstantValue cv = field.getConstantValue();
                if (cv != null) {
                    String cvs = cv.toString();
                    //Remove start and end quotes if field is a String
                    if (cvs.startsWith("\"") && cvs.endsWith("\"")) {
                        cvs = cvs.substring(1, cvs.length() - 1);
                    }
                    sb.append(field.getName());
                    sb.append('=');
                    sb.append(cvs);
                    sb.append(LS);
                }
            }
        }
        return sb;
    }
}
