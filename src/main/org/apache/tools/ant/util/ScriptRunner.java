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
package org.apache.tools.ant.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import org.apache.bsf.BSFException;
import org.apache.bsf.BSFManager;
import org.apache.tools.ant.BuildException;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

/**
 * This class is used to run BSF scripts
 *
 * @author Sam Ruby <a href="mailto:rubys@us.ibm.com">rubys@us.ibm.com</a>
 * @author Conor MacNeill
 */
public class ScriptRunner {
    /** Script language */
    private String language;

    /** Script content */
    private String script = "";

    /** Beans to be provided to the script */
    private Map beans = new HashMap();


    /**
     * Add a list of named objects to the list to be exported to the script
     *
     * @param dictionary a map of objects to be placed into the script context
     *        indexed by String names.
     */
    public void addBeans(Map dictionary) {
        for (Iterator i = dictionary.keySet().iterator(); i.hasNext();) {
            String key = (String) i.next();
            addBean(key, dictionary.get(key));
        }
    }

    /**
     * Add a single object into the script context.
     *
     * @param key the name in the context this object is to stored under.
     * @param bean the object to be stored in the script context.
     */
    public void addBean(String key, Object bean) {
        boolean isValid = key.length() > 0
            && Character.isJavaIdentifierStart(key.charAt(0));

        for (int i = 1; isValid && i < key.length(); i++) {
            isValid = Character.isJavaIdentifierPart(key.charAt(i));
        }

        if (isValid) {
            beans.put(key, bean);
        }
    }

    /**
     * Do the work.
     *
     * @param execName the name that will be passed to BSF for this script
     *        execution.
     *
     * @exception BuildException if someting goes wrong exectuing the script.
     */
    public void executeScript(String execName) throws BuildException {
        if (language == null) {
            throw new BuildException("script language must be specified");
        }

        try {
            BSFManager manager = new BSFManager ();

            for (Iterator i = beans.keySet().iterator(); i.hasNext();) {
                String key = (String) i.next();
                Object value = beans.get(key);
                manager.declareBean(key, value, value.getClass());
            }

            // execute the script
            manager.exec(language, execName, 0, 0, script);
        } catch (BSFException be) {
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
     * Defines the language (required).
     *
     * @param language the scripting language name for the script.
     */
    public void setLanguage(String language) {
        this.language = language;
    }

    /**
     * Get the script language
     *
     * @return the script language
     */
    public String getLanguage() {
        return language;
    }

    /**
     * Load the script from an external file ; optional.
     *
     * @param file the file containing the script source.
     */
    public void setSrc(File file) {
        if (!file.exists()) {
            throw new BuildException("file " + file.getPath() + " not found.");
        }

        int count = (int) file.length();
        byte[] data = new byte[count];

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
     * Set the script text.
     *
     * @param text a component of the script text to be added.
     */
    public void addText(String text) {
        this.script += text;
    }
}
