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
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.taskdefs.MatchingTask;

/**
 * Creates a file or directory element.
 *
 * @see http://clearcase.rational.com/doc/latest/ccase_ux/ccref/mkelem.html
 *
 * @author <a href="mailto:sbailliez@apache.org">Stephane Bailliez</a>
 */
public class CCMkelem extends CCMatchingTask {

    private String type;

    private boolean nocheckout;

    private boolean checkin;

    private boolean preserveTime;

    private Hashtable codirs = new Hashtable();

    public void execute(String[] args, CCFile file) throws BuildException {
        CCFile parent = (CCFile)codirs.get(file.getParent());
        if (parent == null){
            parent = new CCFile(file.getParent());
            if ( !parent.isVersioned() ){
                mkelemDirectory(parent);
                // ensure versioned dir
            } else if ( parent.isCheckedIn() ){
                utils.checkout( parent );
            }
            codirs.put(parent.getPath(), parent);
        }
        args[args.length - 1] = file.getAbsolutePath();
        CmdResult res = CCUtils.cleartool(args);
        if (res.getStatus() != 0) {
            throw new BuildException(res.getStdErr());
        }

    }

    protected void postExecute() {
        // checkin back all co directories
        Enumeration dirs = codirs.elements();
        while( dirs.hasMoreElements() ){
            File dir = (File)dirs.nextElement();
            utils.checkin( dir );
        }
        super.postExecute();
    }

    /** create the command line options based on user input */
    protected Vector getOptions(){
        Vector v = new Vector();
        v.addElement("mkelem");
        if (type != null){
            v.addElement("-eltype");
            v.addElement(type);
        }
        if (comment == null){
            v.addElement("-nc");
        } else {
            commentFile = CCUtils.createCommentFile(comment);
            v.addElement("-cfi");
            v.addElement(commentFile.getAbsolutePath());
        }
        if (nocheckout){
            v.addElement("-nco");
        } else if (checkin){
            v.addElement("-ci");
            if (preserveTime){
                v.addElement("-ptime");
            }
        }
        v.addElement("<pname>"); // dummy arg for file
        return v;
    }

    private void mkelemDirectory(CCFile dir) throws BuildException {
        // resolve symoblic link if any...
        dir = new CCFile( utils.resolveSymbolicLink(dir.getAbsoluteFile()).getAbsolutePath() );

        // make sure that the parent is versioned...
        CCFile parent = new CCFile(dir.getParent());
        boolean should_ci = false;
        if ( !parent.isVersioned() ){
            mkelemDirectory(parent);
            codirs.put(parent.getPath(), parent.getAbsoluteFile());
        }
        // ...and checkout it if already checked in.
        if ( parent.isCheckedIn() ){
            utils.checkout(parent.getAbsoluteFile());
            codirs.put(parent.getPath(), parent.getAbsoluteFile());
        }

        // rename the unversioned directory into a temporary one...
        File mkelem_file = new File(dir.getAbsolutePath() + "_mkelem");
        dir.renameTo( mkelem_file );
        // then create it via Clearcase...
        utils.mkdir( dir );
        codirs.put(dir.getPath(), dir.getAbsoluteFile());
        // .. and populate it back with its files...
        File[] files = dir.listFiles();
        for (int i = 0; i < files.length; i++){
            File newFile = new File(dir, files[i].getName());
            if ( !files[i].renameTo( newFile ) ) {
                throw new BuildException("Could not rename dir '" + files[i] + "' into '" + newFile + "'" );
            }
        }
        // delete this one only if things went smoothly...
        mkelem_file.delete();
    }

// bean setters
    public void setType(String value){
        type = value;
    }

    public void setNoCheckout(boolean value){
        nocheckout = value;
    }

    public void setCheckin(boolean value){
        checkin = value;
    }

    public void setPreserveTime(boolean value){
        preserveTime = value;
    }


}
