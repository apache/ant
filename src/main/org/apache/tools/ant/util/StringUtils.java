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
package org.apache.tools.ant.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Vector;

/**
 * A set of helper methods related to string manipulation.
 *
 */
public final class StringUtils {
    private static final long KILOBYTE = 1024;
    private static final long MEGABYTE = KILOBYTE * 1024;
    private static final long GIGABYTE = MEGABYTE * 1024;
    private static final long TERABYTE = GIGABYTE * 1024;
    private static final long PETABYTE = TERABYTE * 1024;

    /**
     * constructor to stop anyone instantiating the class
     */
    private StringUtils() {
    }

    /** the line separator for this OS */
    public static final String LINE_SEP = System.getProperty("line.separator");

    /**
     * Splits up a string into a list of lines. It is equivalent
     * to <tt>split(data, '\n')</tt>.
     * @param data the string to split up into lines.
     * @return the list of lines available in the string.
     */
    public static Vector lineSplit(String data) {
        return split(data, '\n');
    }

    /**
     * Splits up a string where elements are separated by a specific
     * character and return all elements.
     * @param data the string to split up.
     * @param ch the separator character.
     * @return the list of elements.
     */
    public static Vector split(String data, int ch) {
        Vector elems = new Vector();
        int pos = -1;
        int i = 0;
        while ((pos = data.indexOf(ch, i)) != -1) {
            String elem = data.substring(i, pos);
            elems.addElement(elem);
            i = pos + 1;
        }
        elems.addElement(data.substring(i));
        return elems;
    }

    /**
     * Replace occurrences into a string.
     * @param data the string to replace occurrences into
     * @param from the occurrence to replace.
     * @param to the occurrence to be used as a replacement.
     * @return the new string with replaced occurrences.
     */
    public static String replace(String data, String from, String to) {
        StringBuffer buf = new StringBuffer(data.length());
        int pos = -1;
        int i = 0;
        while ((pos = data.indexOf(from, i)) != -1) {
            buf.append(data.substring(i, pos)).append(to);
            i = pos + from.length();
        }
        buf.append(data.substring(i));
        return buf.toString();
    }

    /**
     * Convenient method to retrieve the full stacktrace from a given exception.
     * @param t the exception to get the stacktrace from.
     * @return the stacktrace from the given exception.
     */
    public static String getStackTrace(Throwable t) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw, true);
        t.printStackTrace(pw);
        pw.flush();
        pw.close();
        return sw.toString();
    }

    /**
     * Checks that a string buffer ends up with a given string. It may sound
     * trivial with the existing
     * JDK API but the various implementation among JDKs can make those
     * methods extremely resource intensive
     * and perform poorly due to massive memory allocation and copying. See
     * @param buffer the buffer to perform the check on
     * @param suffix the suffix
     * @return  <code>true</code> if the character sequence represented by the
     *          argument is a suffix of the character sequence represented by
     *          the StringBuffer object; <code>false</code> otherwise. Note that the
     *          result will be <code>true</code> if the argument is the
     *          empty string.
     */
    public static boolean endsWith(StringBuffer buffer, String suffix) {
        if (suffix.length() > buffer.length()) {
            return false;
        }
        // this loop is done on purpose to avoid memory allocation performance
        // problems on various JDKs
        // StringBuffer.lastIndexOf() was introduced in jdk 1.4 and
        // implementation is ok though does allocation/copying
        // StringBuffer.toString().endsWith() does massive memory
        // allocation/copying on JDK 1.5
        // See http://issues.apache.org/bugzilla/show_bug.cgi?id=37169
        int endIndex = suffix.length() - 1;
        int bufferIndex = buffer.length() - 1;
        while (endIndex >= 0) {
            if (buffer.charAt(bufferIndex) != suffix.charAt(endIndex)) {
                return false;
            }
            bufferIndex--;
            endIndex--;
        }
        return true;
    }

    /**
     * xml does not do "c" like interpretation of strings.
     * i.e. \n\r\t etc.
     * this method processes \n, \r, \t, \f, \\
     * also subs \s -> " \n\r\t\f"
     * a trailing '\' will be ignored
     *
     * @param input raw string with possible embedded '\'s
     * @return converted string
     * @since Ant 1.7
     */
    public static String resolveBackSlash(String input) {
        StringBuffer b = new StringBuffer();
        boolean backSlashSeen = false;
        for (int i = 0; i < input.length(); ++i) {
            char c = input.charAt(i);
            if (!backSlashSeen) {
                if (c == '\\') {
                    backSlashSeen = true;
                } else {
                    b.append(c);
                }
            } else {
                switch (c) {
                    case '\\':
                        b.append((char) '\\');
                        break;
                    case 'n':
                        b.append((char) '\n');
                        break;
                    case 'r':
                        b.append((char) '\r');
                        break;
                    case 't':
                        b.append((char) '\t');
                        break;
                    case 'f':
                        b.append((char) '\f');
                        break;
                    case 's':
                        b.append(" \t\n\r\f");
                        break;
                    default:
                        b.append(c);
                }
                backSlashSeen = false;
            }
        }
        return b.toString();
    }

    /**
     * Takes a human readable size representation eg 10K
     * a long value. Doesn't support 1.1K or other rational values.
     * @param humanSize the amount as a human readable string.
     * @return a long value representation
     * @throws Exception if there is a problem.
     * @since Ant 1.7
     */
    public static long parseHumanSizes(String humanSize) throws Exception {
        long factor = 1L;
        char s = humanSize.charAt(0);
        switch (s) {
            case '+':
                humanSize = humanSize.substring(1);
                break;
            case '-':
                factor = -1L;
                humanSize = humanSize.substring(1);
                break;
            default:
                break;
        }
        //last character isn't a digit
        char c = humanSize.charAt(humanSize.length() - 1);
        if (!Character.isDigit(c)) {
            int trim = 1;
            switch (c) {
                case 'K':
                    factor *= KILOBYTE;
                    break;
                case 'M':
                    factor *= MEGABYTE;
                    break;
                case 'G':
                    factor *= GIGABYTE;
                    break;
                case 'T':
                    factor *= TERABYTE;
                    break;
                case 'P':
                    factor *= PETABYTE;
                    break;
                default:
                    trim = 0;
            }
            humanSize = humanSize.substring(0, humanSize.length() - trim);
        }
        return factor * Long.parseLong(humanSize);
    }
    
    /**
     * Removes the suffix from a given string, if the string contains
     * that suffix.
     * @param string String for check
     * @param suffix Suffix to remove
     * @return the <i>string</i> with the <i>suffix</i>
     * @since Ant 1.7.1
     */
    public static String removeSuffix(String string, String suffix) {
        if (string.endsWith(suffix)) {
            return string.substring(0, string.length() - suffix.length());
        } else {
            return string;
        }
    }

    /**
     * Removes the prefix from a given string, if the string contains
     * that prefix.
     * @param string String for check
     * @param prefix Prefix to remove
     * @return the <i>string</i> with the <i>prefix</i>
     * @since Ant 1.7.1
     */
    public static String removePrefix(String string, String prefix) {
        if (string.startsWith(prefix)) {
            return string.substring(prefix.length());
        } else {
            return string;
        }
    }
}
