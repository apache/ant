/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2003 The Apache Software Foundation.  All rights
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
package org.apache.tools.ant.types.optional;

import org.apache.tools.ant.filters.TokenFilter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import org.apache.bsf.BSFException;
import org.apache.bsf.BSFManager;
import org.apache.tools.ant.BuildException;


/**
 * Most of this is CAP (Cut And Paste) from the Script task
 * ScriptFilter class, implements TokenFilter.Filter
 * for scripts to use.
 * This provides the same beans as the Script Task
 * to a script.
 * The script is meant to use get self.token and
 * set self.token in the reply.
 */

public class ScriptFilter
    extends TokenFilter.ChainableReaderFilter
{
    /** The language - attribute of element */
    private String language;
    /** The script - inline text or external file */
    private String script = "";
    /** The beans - see ScriptTask */
    private Hashtable beans = new Hashtable();
    /** Has this object been initialized ? */
    private boolean initialized = false;
    /** the BSF manager */
    private BSFManager manager;
    /** the token used by the script */
    private String token;

    /**
     * Defines the language (required).
     *
     * @param msg Sets the value for the script variable.
     */
    public void setLanguage(String language) {
        this.language = language;
    }


    /**
     * Add a list of named objects to the list to be exported to the script
     * CAP from taskdefs.optional.Script
     */
    private void addBeans(Hashtable dictionary) {
        for (Enumeration e = dictionary.keys(); e.hasMoreElements();) {
            String key = (String) e.nextElement();

            boolean isValid = key.length() > 0 &&
                Character.isJavaIdentifierStart(key.charAt(0));

            for (int i = 1; isValid && i < key.length(); i++) {
                isValid = Character.isJavaIdentifierPart(key.charAt(i));
            }

            try {
                if (isValid) {
                    beans.put(key, dictionary.get(key));
                }
            }
            catch (Throwable t) {
                throw new BuildException(t);
                //System.err.println("What the helll");
            }
        }
    }
    /**
     * Initialize, mostly CAP from taskdefs.option.Script#execute()
     *
     * @exception BuildException if someting goes wrong
     */
    private void init() throws BuildException {
        if (initialized)
            return;
        initialized = true;
        if (language == null)
            throw new BuildException(
                "scriptfilter: language is not defined");

        try {
            addBeans(getProject().getProperties());
            addBeans(getProject().getUserProperties());
            addBeans(getProject().getTargets());
            addBeans(getProject().getReferences());

            beans.put("project", getProject());

            beans.put("self", this);

            manager = new BSFManager ();

            for (Enumeration e = beans.keys() ; e.hasMoreElements() ;) {
                String key = (String) e.nextElement();
                Object value = beans.get(key);
                manager.declareBean(key, value, value.getClass());
            }

        }
        catch (BSFException e) {
            Throwable t = e;
            Throwable te = e.getTargetException();
            if (te != null) {
                if (te instanceof BuildException) {
                    throw (BuildException) te;
                } else {
                    t = te;
                }
            }
            throw new BuildException(t);
        }

    }

    /**
     * The current token
     *
     * @param token the string filtered by the script
     */
    public void setToken(String token) {
        this.token = token;
    }

    /**
     * The current token
     *
     * @return the string filtered by the script
     */
    public String getToken() {
        return token;
    }

    /**
     * Called filter the token.
     * This sets the token in this object, calls
     * the script and returns the token.
     *
     * @param token the token to be filtered
     * @return the filtered token
     */
    public String filter(String token) {
        init();
        setToken(token);
        try {
            manager.exec(language, "<ANT>", 0, 0, script);
            return getToken();
        }
        catch (BSFException be) {
            Throwable t = be;
            Throwable te = be.getTargetException();
            if (te != null) {
                if  (te instanceof BuildException) {
                    throw (BuildException) te;
                } else {
                    t = te;
                }
            }
            throw new BuildException(t);
        }
    }
    /**
     * Load the script from an external file ; optional.
     *
     * @param msg Sets the value for the script variable.
     */
    public void setSrc(String fileName) {
        File file = new File(fileName);
        if (!file.exists()) {
            throw new BuildException("file " + fileName + " not found.");
        }

        int count = (int) file.length();
        byte data[] = new byte[count];

        try {
            FileInputStream inStream = new FileInputStream(file);
            inStream.read(data);
            inStream.close();
        } catch (IOException e) {
            throw new BuildException(e);
        }

        script += new String(data);
    }

    /**
     * The script text.
     *
     * @param msg Sets the value for the script variable.
     */
    public void addText(String text) {
        this.script += text;
    }
}
