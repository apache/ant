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
 * 4. The names "The Jakarta Project", "Tomcat", and "Apache Software
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
import java.net.*;
/**
 * Get a particular source. 
 *
 * @author costin@dnt.ro
 */
public class Get extends Task {
    private String source; // required
    private String dest; // required
    private String verbose = "";
    String ignoreErrors=null;
    
    /**
     * Does the work.
     *
     * @exception BuildException Thrown in unrecovrable error.
     */
    public void execute() throws BuildException {
	try {
            URL url = null;
            try {
                url = new URL(source);
            } catch (MalformedURLException e) {
                throw new BuildException(e.toString());
            }

	    log("Getting: " + source);

	    File destF=new File(dest);
	    FileOutputStream fos = new FileOutputStream(destF);

	    InputStream is=null;
	    for( int i=0; i< 3 ; i++ ) {
		try {
		    is = url.openStream();
		    break;
		} catch( IOException ex ) {
		    log( "Error opening connection " + ex );
		}
	    }
	    if( is==null ) {
		log( "Can't get " + source + " to " + dest);
		if( ignoreErrors != null ) return;
		throw new BuildException( "Can't get " + source + " to " + dest);
	    }
		
	    byte[] buffer = new byte[100 * 1024];
	    int length;
	    
	    while ((length = is.read(buffer)) >= 0) {
		fos.write(buffer, 0, length);
		if ("true".equals(verbose)) System.out.print(".");
	    }
	    if( "true".equals(verbose)) System.out.println();
	    fos.close();
	    is.close();
	} catch (IOException ioe) {
	    log("Error getting " + source + " to " + dest );
	    if( ignoreErrors != null ) return;
	    throw new BuildException(ioe.toString());
	}
    }

    /**
     * Set the URL.
     *
     * @param d URL for the file.
     */
    public void setSrc(String d) {
	this.source=d;
    }

    /**
     * Where to copy the source file.
     *
     * @param dest Path to file.
     */
    public void setDest(String dest) {
	this.dest = dest;
    }

    /**
     * Be verbose, if set to "<CODE>true</CODE>".
     *
     * @param v if "true" then be verbose
     */
    public void setVerbose(String v) {
	verbose = v;
    }

    /**
     * Don't stop if get fails if set to "<CODE>true</CODE>".
     *
     * @param v if "true" then be verbose
     */
    public void setIgnoreErrors(String v) {
	ignoreErrors = v;
    }
}
