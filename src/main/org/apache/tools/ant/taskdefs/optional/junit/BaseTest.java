/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000 The Apache Software Foundation.  All rights
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
 * 4. The names "The Jakarta Project", "Ant", and "Apache Software
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

package org.apache.tools.ant.taskdefs.optional.junit;

import java.io.File;
import java.util.Vector;

/**
 * Baseclass for BatchTest and JUnitTest.
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a> 
 * @author <a href="mailto:sbailliez@imediation.com">Stephane Bailliez</a>
 */
public abstract class BaseTest {
    protected boolean haltOnError = false;
    protected boolean haltOnFail = false;
    protected boolean fork = false;
    protected String ifProperty = null;
    protected String unlessProperty = null;
    protected Vector formatters = new Vector();
    /** destination directory */
    protected File destDir = null;

    public void setFork(boolean value) {
        fork = value;
    }

    public boolean getFork() {
        return fork;
    }

    public void setHaltonerror(boolean value) {
        haltOnError = value;
    }

    public void setHaltonfailure(boolean value) {
        haltOnFail = value;
    }

    public boolean getHaltonerror() {
        return haltOnError;
    }

    public boolean getHaltonfailure() {
        return haltOnFail;
    }

    public void setIf(String propertyName) {
        ifProperty = propertyName;
    }

    public void setUnless(String propertyName) {
        unlessProperty = propertyName;
    }

    public void addFormatter(FormatterElement elem) {
        formatters.addElement(elem);
    }

    /**
     * Sets the destination directory.
     */
    public void setTodir(File destDir) {
        this.destDir = destDir; 
    }

    /**
     * @return the destination directory as an absolute path if it exists
     *			otherwise return <tt>null</tt>
     */
    public String getTodir(){
        if (destDir != null){
            return destDir.getAbsolutePath();
        }
        return null;
    }

}
