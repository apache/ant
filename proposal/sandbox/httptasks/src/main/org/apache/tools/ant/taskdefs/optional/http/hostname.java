/*
 *  The Apache Software License, Version 1.1
 *
 *  Copyright (c) 2002 The Apache Software Foundation.  All rights
 *  reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions
 *  are met:
 *
 *  1. Redistributions of source code must retain the above copyright
 *  notice, this list of conditions and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright
 *  notice, this list of conditions and the following disclaimer in
 *  the documentation and/or other materials provided with the
 *  distribution.
 *
 *  3. The end-user documentation included with the redistribution, if
 *  any, must include the following acknowlegement:
 *  "This product includes software developed by the
 *  Apache Software Foundation (http://www.apache.org/)."
 *  Alternately, this acknowlegement may appear in the software itself,
 *  if and wherever such third-party acknowlegements normally appear.
 *
 *  4. The names "The Jakarta Project", "Ant", and "Apache Software
 *  Foundation" must not be used to endorse or promote products derived
 *  from this software without prior written permission. For written
 *  permission, please contact apache@apache.org.
 *
 *  5. Products derived from this software may not be called "Apache"
 *  nor may "Apache" appear in their names without prior written
 *  permission of the Apache Group.
 *
 *  THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 *  WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 *  OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 *  ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 *  USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 *  OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 *  OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 *  SUCH DAMAGE.
 *  ====================================================================
 *
 *  This software consists of voluntary contributions made by many
 *  individuals on behalf of the Apache Software Foundation.  For more
 *  information on the Apache Software Foundation, please see
 *  <http://www.apache.org/>.
 */
package org.apache.tools.ant.taskdefs.optional.http;

import java.io.*;
import java.net.*;
import java.util.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.net.InetAddress;

import org.apache.tools.ant.*;

/**
 * trivial task to get the hostname of a box; as IPaddr, hostname, or
 * fullname.
 *
 * @created 07 January 2002
 */

public class Hostname extends Task {

    /**
     * Description of the Field
     */
    private String property;

    /**
     * Description of the Field
     */
    private boolean failonerror = true;

    /**
     * Description of the Field
     */
    private boolean address = false;


    /**
     * Sets the FailOnError attribute of the Hostname object
     *
     * @param failonerror The new FailOnError value
     */
    public void setFailOnError(boolean failonerror) {
        this.failonerror = failonerror;
    }


    /**
     * Sets the Address attribute of the Hostname object
     *
     * @param address The new Address value
     */
    public void setAddress(boolean address) {
        this.address = address;
    }


    /**
     * Does the work.
     *
     * @exception BuildException Thrown in unrecoverable error.
     */
    public void execute()
        throws BuildException {
        if(property==null) {
           throw new BuildException("Property attribute must be defined"); 
        }
        String result;
        String exception = null;
        try {
            if (address) {
                result = getAddress();
            }
            else {
                result = getHostname();
            }
            project.setNewProperty(property, result);
        } catch (UnknownHostException e) {
            exception = e;
        } catch (SecurityException e) {
            exception = e;
        }
        if (e != null) {
            if (failonerror) {
                throw new BuildException("resolving hostname", e);
            }
            else {
                log("failed to resolve local hostname", Project.MSG_ERR);
            }
        }
    }


    /**
     * Gets the Address attribute of the Hostname object
     *
     * @return The Address value
     * @exception SecurityException Description of Exception
     * @exception UnknownHostException Description of Exception
     */
    public String getAddress()
        throws SecurityException, UnknownHostException {
        return getLocalHostAddress().getHostAddress();
    }


    /**
     * Gets the Hostname attribute of the Hostname object
     *
     * @return The Hostname value
     * @exception SecurityException Description of Exception
     * @exception UnknownHostException Description of Exception
     */
    public String getHostname()
        throws SecurityException, UnknownHostException {
        return getLocalHostAddress().getHostName();
    }


    /**
     * Gets the LocalHostAddress attribute of the Hostname object
     *
     * @return The LocalHostAddress value
     * @exception UnknownHostException Description of Exception
     */
    public InetAddress getLocalHostAddress()
        throws UnknownHostException {
        return InetAddress.getLocalHost();
    }

}

