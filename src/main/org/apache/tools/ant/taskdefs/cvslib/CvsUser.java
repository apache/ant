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
package org.apache.tools.ant.taskdefs.cvslib;

import org.apache.tools.ant.BuildException;

/**
 * Represents a CVS user with a userID and a full name.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @author <a href="mailto:jeff.martin@synamic.co.uk">Jeff Martin</a>
 * @version $Revision$ $Date$
 */
public class CvsUser {
    /** The user's Id */
    private String m_userID;
    /** The user's full name */
    private String m_displayName;


    /**
     * Set the user's fullname
     *
     * @param displayName the user's full name
     */
    public void setDisplayname(final String displayName) {
        m_displayName = displayName;
    }


    /**
     * Set the user's id
     *
     * @param userID the user's new id value.
     */
    public void setUserid(final String userID) {
        m_userID = userID;
    }


    /**
     * Get the user's id.
     *
     * @return The userID value
     */
    String getUserID() {
        return m_userID;
    }


    /**
     * Get the user's full name
     *
     * @return the usre's full name
     */
    String getDisplayname() {
        return m_displayName;
    }


    /**
     * validate that this objetc is configured.
     *
     * @exception BuildException if the instance has not be correctly
     *            configured.
     */
    void validate() throws BuildException {
        if (null == m_userID) {
            final String message = "Username attribute must be set.";

            throw new BuildException(message);
        }
        if (null == m_displayName) {
            final String message =
                "Displayname attribute must be set for userID " + m_userID;

            throw new BuildException(message);
        }
    }
}

