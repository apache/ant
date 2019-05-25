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
package org.apache.tools.ant.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Vector;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.apache.tools.ant.BuildException;

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
    @Deprecated
    public static final String LINE_SEP = System.lineSeparator();

    /**
     * Splits up a string into a list of lines. It is equivalent
     * to <code>split(data, '\n')</code>.
     * @param data the string to split up into lines.
     * @return the list of lines available in the string.
     */
    public static Vector<String> lineSplit(String data) {
        return split(data, '\n');
    }

    /**
     * Splits up a string where elements are separated by a specific
     * character and return all elements.
     * @param data the string to split up.
     * @param ch the separator character.
     * @return the list of elements.
     */
    public static Vector<String> split(String data, int ch) {
        Vector<String> elems = new Vector<>();
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
     * @deprecated Use {@link String#replace(CharSequence, CharSequence)} now.
     */
    @Deprecated
    public static String replace(String data, String from, String to) {
        return data.replace(from, to);
    }

    /**
     * Convenient method to retrieve the full stacktrace from a given exception.
     * @param t the exception to get the stacktrace from.
     * @return the stacktrace from the given exception.
     */
    public static String getStackTrace(Throwable t) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw, true);
        t.printStackTrace(pw); //NOSONAR
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
        // See https://issues.apache.org/bugzilla/show_bug.cgi?id=37169
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
     * also subs \s -&gt; " \n\r\t\f"
     * a trailing '\' will be ignored
     *
     * @param input raw string with possible embedded '\'s
     * @return converted string
     * @since Ant 1.7
     */
    public static String resolveBackSlash(String input) {
        StringBuilder b = new StringBuilder();
        boolean backSlashSeen = false;
        for (final char c : input.toCharArray()) {
            if (!backSlashSeen) {
                if (c == '\\') {
                    backSlashSeen = true;
                } else {
                    b.append(c);
                }
            } else {
                switch (c) {
                    case '\\':
                        b.append('\\');
                        break;
                    case 'n':
                        b.append('\n');
                        break;
                    case 'r':
                        b.append('\r');
                        break;
                    case 't':
                        b.append('\t');
                        break;
                    case 'f':
                        b.append('\f');
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
    public static long parseHumanSizes(String humanSize) throws Exception { //NOSONAR
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
        try {
            return factor * Long.parseLong(humanSize);
        } catch (NumberFormatException e) {
            throw new BuildException("Failed to parse \"" + humanSize + "\"", e);
        }
    }

    /**
     * Removes the suffix from a given string, if the string contains
     * that suffix.
     * @param string String for check
     * @param suffix Suffix to remove
     * @return the <i>string</i> with the <i>suffix</i>
     */
    public static String removeSuffix(String string, String suffix) {
        if (string.endsWith(suffix)) {
            return string.substring(0, string.length() - suffix.length());
        }
        return string;
    }

    /**
     * Removes the prefix from a given string, if the string contains
     * that prefix.
     * @param string String for check
     * @param prefix Prefix to remove
     * @return the <i>string</i> with the <i>prefix</i>
     */
    public static String removePrefix(String string, String prefix) {
        if (string.startsWith(prefix)) {
            return string.substring(prefix.length());
        }
        return string;
    }

    /**
     * Joins the string representation of the elements of a collection to
     * a joined string with a given separator.
     * @param collection Collection of the data to be joined (may be null)
     * @param separator Separator between elements (may be null)
     * @return the joined string
     */
    public static String join(Collection<?> collection, CharSequence separator) {
        if (collection == null) {
            return "";
        }
        return collection.stream().map(String::valueOf)
            .collect(joining(separator));
    }

    /**
     * Joins the string representation of the elements of an array to
     * a joined string with a given separator.
     * @param array Array of the data to be joined (may be null)
     * @param separator Separator between elements (may be null)
     * @return the joined string
     */
    public static String join(Object[] array, CharSequence separator) {
        if (array == null) {
            return "";
        }
        return join(Arrays.asList(array), separator);
    }

    private static Collector<CharSequence, ?, String> joining(CharSequence separator) {
        return separator == null ? Collectors.joining() : Collectors.joining(separator);
    }

    /**
     * @param inputString String to trim
     * @return null if the input string is null or empty or contain only empty spaces.
     * It returns the input string without leading and trailing spaces otherwise.
     *
     */
    public static String trimToNull(String inputString) {
        if (inputString == null) {
            return null;
        }
        String tmpString = inputString.trim();
        return tmpString.isEmpty() ? null : tmpString;
    }

}
