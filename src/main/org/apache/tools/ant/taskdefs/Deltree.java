/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999 The Apache Software Foundation.  All rights 
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

package org.apache.tools.ant.taskdefs;

import org.apache.tools.ant.*;
import java.io.*;

/**
 *
 *
 * @author duncan@x180.com
 *
 * @deprecated The deltree task is deprecated.  Use delete instead.
 */

public class Deltree extends Task {

    private File dir;

    public void setDir(File dir) {
	this.dir = dir;
    }
    
    public void execute() throws BuildException {
        log("DEPRECATED - The deltree task is deprecated.  Use delete instead.");

        if (dir == null) {
            throw new BuildException("dir attribute must be set!", location);
        } 

	if (dir.exists()) {
	    if (!dir.isDirectory()) {
		if (!dir.delete()) {
        	    throw new BuildException("Unable to delete directory " 
                                             + dir.getAbsolutePath(),
                                             location);
	        }
		return;
		// String msg = "Given dir: " + dir.getAbsolutePath() +
		// " is not a dir";
		// throw new BuildException(msg);
	    }

            log("Deleting: " + dir.getAbsolutePath());

            try {
                removeDir(dir);
            } catch (IOException ioe) {
                String msg = "Unable to delete " + dir.getAbsolutePath();
                throw new BuildException(msg, location);
            }
        }
    }
    
    private void removeDir(File dir) throws IOException {

        // check to make sure that the given dir isn't a symlink
        // the comparison of absolute path and canonical path
        // catches this
	
	//        if (dir.getCanonicalPath().equals(dir.getAbsolutePath())) {
	// (costin) It will not work if /home/costin is symlink to /da0/home/costin ( taz
	// for example )
	String[] list = dir.list();
	for (int i = 0; i < list.length; i++) {
	    String s = list[i];
	    File f = new File(dir, s);
	    if (f.isDirectory()) {
		removeDir(f);
	    } else {
		if (!f.delete()) {
        	    throw new BuildException("Unable to delete file " + f.getAbsolutePath());
	        }
	    }
	}
        if (!dir.delete()) {
	    throw new BuildException("Unable to delete directory " + dir.getAbsolutePath());
	}
    }
}

