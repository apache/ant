/* 
 * Copyright  2002,2004 Apache Software Foundation
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
package org.apache.tools.ant.taskdefs.optional.metamata;

import java.io.File;
import java.util.Vector;
import org.apache.tools.ant.util.StringUtils;
import org.apache.tools.ant.util.regexp.RegexpMatcher;
import org.apache.tools.ant.util.regexp.RegexpMatcherFactory;

/**
 * Parser that will parse an output line of MAudit and return an
 * interpreted violation if any.
 *
 * <p>
 * MAudit is supposed to be configured with -fullpath so that it can
 * correctly locate the file and attribute violation to the appropriate
 * file (there might be several classes with the same name in
 * different packages)
 * </p>
 *
 * @author <a href="mailto:sbailliez@apache.org">Stephane Bailliez</a>
 */
final class MAuditParser {

    /** pattern used by maudit to report the error for a file */
    /** RE does not seems to support regexp pattern with comments so i'm stripping it*/
    // (?:file:)?((?#filepath).+):((?#line)\\d+)\\s*:\\s+((?#message).*)
    private static final String AUDIT_PATTERN = "(?:file:)?(.+):(\\d+)\\s*:\\s+(.*)";

    /** matcher that will be used to extract the info from the line */
    private final RegexpMatcher matcher;

    MAuditParser() {
        /** the matcher should be the Oro one. I don't know about the other one */
        matcher = (new RegexpMatcherFactory()).newRegexpMatcher();
        matcher.setPattern(AUDIT_PATTERN);
    }

    /**
     * Parse a line obtained from MAudit.
     * @param line a line obtained from the MAudit output.
     * @return the violation corresponding to the displayed line
     * or <tt>null</tt> if it could not parse it. (might be a
     * message info or copyright or summary).
     */
    Violation parseLine(String line) {
        Vector matches = matcher.getGroups(line);
        if (matches == null) {
            return null;
        }
        final String file = (String) matches.elementAt(1);
        Violation violation = new Violation();
        violation.file = file;
        violation.line = (String) matches.elementAt(2);
        violation.error = (String) matches.elementAt(3);
        // remove the pathname from any messages and let the classname only.
        final int pos = file.lastIndexOf(File.separatorChar);
        if ((pos != -1) && (pos != file.length() - 1)) {
            String filename = file.substring(pos + 1);
            violation.error = StringUtils.replace(violation.error,
                    "file:" + file, filename);
        }
        return violation;
    }

    /** the inner class used to report violation information */
    static final class Violation {
        String file;
        String line;
        String error;
    }
}
