/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "Ant" and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */
package org.apache.tools.ant.taskdefs.optional.metamata;

import java.util.Vector;
import java.io.File;

import org.apache.tools.ant.util.regexp.RegexpMatcher;
import org.apache.tools.ant.util.regexp.RegexpMatcherFactory;
import org.apache.tools.ant.util.StringUtils;

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

    MAuditParser(){
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
    Violation parseLine(String line){
        Vector matches = matcher.getGroups(line);
        if (matches == null){
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
