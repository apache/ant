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
package org.apache.tools.ant.taskdefs.optional.clearcase;

import java.io.File;

import org.apache.tools.ant.BuildException;

/**
 * An extended file that gives state information.
 *
 */
public class CCFile extends File {

    /** is it checkedout */
    private boolean checkedout = false;

    /** is it under source control ? */
    private boolean versioned = false;

    /** was this file already described once ? */
    private boolean described = false;

    public CCFile(String parent, String child) {
        super(parent, child);
    }

    public CCFile(File parent, String child) {
        super(parent, child);
    }

    public CCFile(String pathname) {
        super(pathname);
    }

    /**
     * @return whether the file is checkedout. A non checkedout file
     * does not imply it is a checkedin one.
     * @see #isCheckedIn()
     * @see #isVersioned()
     */
    public boolean isCheckedOut() {
        if (!described){
            refresh();
        }
        return checkedout;
    }

    /**
     * @return whether the file is versioned or not.
     */
    public boolean isVersioned() {
        if (!described){
            refresh();
        }
        return versioned;
    }

    /**
     * @return whether the file is checkedin or not. A non checkedin file
     * does not imply it is a checkedout one.
     * @see #isCheckedOut()
     * @see #isVersioned()
     */
    public boolean isCheckedIn(){
        return isVersioned() && !isCheckedOut();
    }

    /**
     * Refresh the file status in case it changed since the
     * first access.
     */
    public void refresh() {
        String[] args = {"describe", "-fmt", "\"%m %o\"", getAbsolutePath() };
        CmdResult res = CCUtils.cleartool(args);
        if (res.getStatus() != 0){
            throw new BuildException(res.getStdErr());
        }
        String stdout = res.getStdout();
        versioned = (stdout.indexOf("view private object") == -1);
        checkedout = (stdout.indexOf("checkout") != -1);
        described = true;
    }
}
