/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2002 The Apache Software Foundation.  All rights 
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
package org.apache.tools.ant.util.regexp;

import org.apache.tools.ant.BuildException;
import org.apache.oro.text.regex.Perl5Substitution;
import org.apache.oro.text.regex.Substitution;
import org.apache.oro.text.regex.Util;



/***
 * Regular expression implementation using the Jakarta Oro package
 * @author Matthew Inger <a href="mailto:mattinger@mindless.com">mattinger@mindless.com</a>
 */
public class JakartaOroRegexp extends JakartaOroMatcher implements Regexp {

    public JakartaOroRegexp() {
        super();
    }

    public String substitute(String input, String argument, int options)
        throws BuildException {
        // translate \1 to $1 so that the Perl5Substitution will work
        StringBuffer subst = new StringBuffer();
        for (int i = 0; i < argument.length(); i++) {
            char c = argument.charAt(i);
            if (c == '\\') {
                if (++i < argument.length()) {
                    c = argument.charAt(i);
                    int value = Character.digit(c, 10);
                    if (value > -1) {
                        subst.append("$").append(value);
                    } else {
                        subst.append(c);
                    }
                } else {
                    // XXX - should throw an exception instead?
                    subst.append('\\');
                }
            } else {
                subst.append(c);
            }
        }
        

        // Do the substitution
        Substitution s = 
            new Perl5Substitution(subst.toString(), 
                                  Perl5Substitution.INTERPOLATE_ALL);
        return Util.substitute(matcher,
                               getCompiledPattern(options),
                               s,
                               input,
                               getSubsOptions(options));
    }

    protected int getSubsOptions(int options) {
        boolean replaceAll = RegexpUtil.hasFlag(options, REPLACE_ALL);
        int subsOptions = 1;
        if (replaceAll) {
            subsOptions = Util.SUBSTITUTE_ALL;
        }
        return subsOptions;
    }

}
