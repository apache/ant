/* 
 * Copyright  2001-2002,2004 Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
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
 * @author <a href="mailto:sbailliez@apache.org">Stephane Bailliez</a>
 */
public final class StringUtils {

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

}
