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
package org.apache.tools.ant.taskdefs.optional.depend;

/**
 * Utility class file routines. This class provides a number of static
 * utility methods to convert between the formats used in the Java class
 * file format and those commonly used in Java programming.
 *
 *
 */
// CheckStyle:HideUtilityClassConstructorCheck OFF (bc)
public class ClassFileUtils {

    /**
     * Convert a class name from class file slash notation to java source
     * file dot notation.
     *
     * @param name the class name in slash notation org/apache/ant
     * @return the class name in dot notation (eg. java.lang.Object).
     */
    public static String convertSlashName(String name) {
        return name.replace('\\', '.').replace('/', '.');
    }

    /**
     * Convert a class name from java source file dot notation to class file
     * slash notation..
     *
     * @param dotName the class name in dot notation (eg. java.lang.Object).
     * @return the class name in slash notation (eg. java/lang/Object).
     */
    public static String convertDotName(String dotName) {
        return dotName.replace('.', '/');
    }
}
