/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002-2003 The Apache Software Foundation.  All rights
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
package org.apache.tools.ant.taskdefs.email;

/**
 * Holds an email address.
 *
 * @author roxspring@yahoo.com Rob Oxspring
 * @author Michael Davey
 * @since Ant 1.5
 */
public class EmailAddress {
    private String name;
    private String address;


    /** Creates an empty email address  */
    public EmailAddress() {
    }


    /**
     * Creates a new email address based on the given string
     *
     * @param email the email address (with or without <>)
     * Acceptable forms include:
     *    address
     *    <address>
     *    name <address>
     *    <address> name
     *    (name) address
     *    address (name)
     */
    // Make a limited attempt to extract a sanitized name and email address
    // Algorithm based on the one found in Ant's MailMessage.java
    public EmailAddress(String email) {
        final int minLen = 9;
        int len = email.length();

        // shortcut for "<address>"
        if (len > minLen) {
            if ((email.charAt(0) == '<' || email.charAt(1) == '<')
            && (email.charAt(len - 1) == '>' || email.charAt(len - 2) == '>')) {
                this.address = trim(email, true);
                return;
            }
        }

        int paramDepth = 0;
        int start = 0;
        int end = 0;
        int nStart = 0;
        int nEnd = 0;

        for (int i = 0; i < len; i++) {
            char c = email.charAt(i);
            if (c == '(') {
                paramDepth++;
                if (start == 0) {
                    end = i;  // support "address (name)"
                    nStart = i + 1;
                }
            } else if (c == ')') {
                paramDepth--;
                if (end == 0) {
                    start = i + 1;  // support "(name) address"
                    nEnd = i;
                }
            } else if (paramDepth == 0 && c == '<') {
                if (start == 0) {
                    nEnd = i;
                }
                start = i + 1;
            } else if (paramDepth == 0 && c == '>') {
                end = i;
                if (end != len - 1) {
                    nStart = i + 1;
                }
            }
        }

        // DEBUG: System.out.println( email );
        if (end == 0) {
            end = len;
        }
        // DEBUG: System.out.println( "address: " + start + " " + end );
        if (nEnd == 0) {
            nEnd = len;
        }
        // DEBUG: System.out.println( "name: " + nStart + " " + nEnd );

        this.address = trim(email.substring(start, end), true);
        this.name = trim(email.substring(nStart, nEnd), false);

        // if the two substrings are longer than the original, then name
        // contains address - so reset the name to null
        if (this.name.length() + this.address.length() > len) {
            this.name = null;
        }
    }

    /**
     *  A specialised trim() that trims whitespace,
     *  '(', ')', '"', '<', '>' from the start and end of strings
     */
    private String trim(String t, boolean trimAngleBrackets) {
        int start = 0;
        int end = t.length();
        boolean trim = false;
        do {
            trim = false;
            if (t.charAt(end - 1) == ')'
                || (t.charAt(end - 1) == '>' && trimAngleBrackets)
                || (t.charAt(end - 1) == '"' && t.charAt(end - 2) != '\\')
                || t.charAt(end - 1) <= '\u0020') {
                trim = true;
                end--;
            }
            if (t.charAt(start) == '('
                || (t.charAt(start) == '<' && trimAngleBrackets)
                || t.charAt(start) == '"'
                || t.charAt(start) <= '\u0020') {
                trim = true;
                start++;
            }
        } while (trim);
        return t.substring(start, end);
    }


    /**
     * Sets the personal / display name of the address
     *
     * @param name the display name
     */
    public void setName(String name) {
        this.name = name;
    }


    /**
     * Sets the email address
     *
     * @param address the actual email address (without <>)
     */
    public void setAddress(String address) {
        this.address = address;
    }


    /**
     * Constructs a string "name &lt;address&gt;" or "address"
     *
     * @return a string representation of the address
     */
    public String toString() {
        if (name == null) {
            return address;
        } else {
            return name + " <" + address + ">";
        }
    }


    /**
     * Returns the address
     *
     * @return the address part
     */
    public String getAddress() {
        return address;
    }


    /**
     * Returns the display name
     *
     * @return the display name part
     */
    public String getName() {
        return name;
    }
}

