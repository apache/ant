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

import java.util.Vector;
import java.util.Hashtable;
import java.util.Enumeration;
import java.io.File;

import org.apache.tools.ant.Task;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.taskdefs.MatchingTask;
import org.apache.tools.ant.types.FileSet;

/**
 * Removes the name of an element or VOB symbolic link from a directory version
 *
 * @see http://clearcase.rational.com/doc/latest/ccase_ux/ccref/rmname.html
 *
 */
public class CCRmname extends CCMatchingTask {

    private boolean force = false;

    /** used to cache co directories */
    private Hashtable codirs = new Hashtable();

    protected Vector getOptions() {
        Vector v = new Vector();
        v.addElement("rmname");
        if (comment != null){
            v.addElement("-cfile");
            v.addElement(commentFile.getPath());
        } else {
            v.addElement("-comment");
            v.addElement(CCUtils.DEFAULT_COMMENT);
        }
        if (force){
            v.addElement("-f");
        }
        v.addElement("<pname>");
        return v;
    }

    public void execute(String[] args, CCFile file) throws BuildException {
        CCFile parent = new CCFile(file.getParent());
        // we have first to co the parent
        if ( parent.isCheckedIn() ){
            utils.checkout(parent);
        }
        // remove the element
        args[args.length - 1] = file.getAbsolutePath();
        CmdResult res = utils.cleartool(args);
        // if it failed, unco otherwise ci the parent
        if (res.getStatus() != 0){
            utils.uncheckout(parent);
            throw new BuildException(res.getStdErr());
        } else {
            utils.checkin(parent);
        }
    }

    protected boolean accept(CCFile file){
        return file.isCheckedIn();
    }

// bean setters
    public void setForce(boolean value){
        force = value;
    }
}
