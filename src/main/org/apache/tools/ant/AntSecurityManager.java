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

package org.apache.tools.ant;

import java.security.*;
import java.io.*;
import java.net.*;

/**
 * The "almost" security manager that allows everything but exit();
 *
 * @author stefano@apache.org
 */

public class AntSecurityManager extends SecurityManager {

    private boolean exit = false;
    
    public AntSecurityManager() {
        super();
    }
    
    public void setExit(boolean allowExit) {
        this.exit = allowExit;
    }

    public void checkExit(int status) {
        if (!exit) {
            throw new SecurityException("Not Allowed.");
        }
    }

    // everything else should be allowed
/*    
Removed the following interfaces as they won't compile with JDK 1.1,
and the defaults for JDK 1.2 appear to be sufficient.  If you have
a problem, let me know.  Sam Ruby - rubys@us.ibm.com

    public void checkPermission(Permission perm) {
        // allowed
    }

    public void checkPermission(Permission perm, Object context) {
        // allowed
    }
*/
    
    public void checkCreateClassLoader() {
        // allowed
    }

    public void checkAccess(Thread t) {
        // allowed
    }
    
    public void checkAccess(ThreadGroup g) {
        // allowed
    }
    
    public void checkExec(String cmd) {
        // allowed
    }
    
    public void checkLink(String lib) {
        // allowed
    }
    
    public void checkRead(FileDescriptor fd) {
        // allowed
    }

    public void checkRead(String file) {
        // allowed
    }

    public void checkRead(String file, Object context) {
        // allowed
    }

    public void checkWrite(FileDescriptor fd) {
        // allowed
    }

    public void checkWrite(String file) {
        // allowed
    }

    public void checkDelete(String file) {
        // allowed
    }

    public void checkConnect(String host, int port) {
        // allowed
    }

    public void checkConnect(String host, int port, Object context) {
        // allowed
    }

    public void checkListen(int port) {
        // allowed
    }

    public void checkAccept(String host, int port) {
        // allowed
    }

    public void checkMulticast(InetAddress maddr) {
        // allowed
    }

    public void checkMulticast(InetAddress maddr, byte ttl) {
        // allowed
    }

    public void checkPropertiesAccess() {
        // allowed
    }

    public void checkPropertyAccess(String key) {
        // allowed
    }

    public void checkPrintJobAccess() {
        // allowed
    }

    public void checkSystemClipboardAccess() {
        // allowed
    }

    public void checkAwtEventQueueAccess() {
        // allowed
    }

    public void checkPackageAccess(String pkg) {
        // allowed
    }

    public void checkPackageDefinition(String pkg) {
        // allowed
    }

    public void checkSetFactory() {
        // allowed
    }

    public void checkMemberAccess(Class clazz, int which) {
        // allowed
    }

    public void checkSecurityAccess(String target) {
        // allowed
    }
    
    public boolean checkTopLevelWindow(Object window) {
        return true;
    }
}
