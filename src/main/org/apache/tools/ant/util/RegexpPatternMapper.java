/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000,2002 The Apache Software Foundation.  All rights
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

package org.apache.tools.ant.util;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.util.regexp.RegexpMatcher;
import org.apache.tools.ant.util.regexp.RegexpMatcherFactory;


import java.util.Vector;

/**
 * Implementation of FileNameMapper that does regular expression
 * replacements.
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 */
public class RegexpPatternMapper implements FileNameMapper {
    protected RegexpMatcher reg = null;
    protected char[] to = null;
    protected StringBuffer result = new StringBuffer();

    public RegexpPatternMapper() throws BuildException {
        reg = (new RegexpMatcherFactory()).newRegexpMatcher();
    }
    
    /**
     * Sets the &quot;from&quot; pattern. Required.
     */
    public void setFrom(String from) throws BuildException {
        try {
            reg.setPattern(from);
        } catch (NoClassDefFoundError e) {
            // depending on the implementation the actual RE won't
            // get instantiated in the constructor.
            throw new BuildException("Cannot load regular expression matcher",
                                     e);
        }
    }

    /**
     * Sets the &quot;to&quot; pattern. Required.
     */
    public void setTo(String to) {
        this.to = to.toCharArray();
    }

    /**
     * Returns null if the source file name doesn't match the
     * &quot;from&quot; pattern, an one-element array containing the
     * translated file otherwise.
     */
    public String[] mapFileName(String sourceFileName) {
        if (reg == null  || to == null
            || !reg.matches(sourceFileName)) {
            return null;
        }
        return new String[] {replaceReferences(sourceFileName)};
    }

    /**
     * Replace all backreferences in the to pattern with the matched
     * groups of the source.
     */
    protected String replaceReferences(String source) {
        Vector v = reg.getGroups(source);
        
        result.setLength(0);
        for (int i = 0; i < to.length; i++) {
            if (to[i] == '\\') {
                if (++i < to.length) {
                    int value = Character.digit(to[i], 10);
                    if (value > -1) {
                        result.append((String) v.elementAt(value));
                    } else {
                        result.append(to[i]);
                    }
                } else {
                    // XXX - should throw an exception instead?
                    result.append('\\');
                }
            } else {
                result.append(to[i]);
            }
        }
        return result.toString();
    }

}
