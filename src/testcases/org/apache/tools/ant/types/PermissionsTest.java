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

package org.apache.tools.ant.types;

import junit.framework.TestCase;

import org.apache.tools.ant.ExitException;

/**
 * JUnit 3 testcases for org.apache.tools.ant.types.Permissions.
 *
 * @author <a href="mailto:martijn@kruithof.xs4all.nl>Martijn Kruithof</a>
 */
public class PermissionsTest extends TestCase {
    
    Permissions perms;

    public PermissionsTest(String name) {
        super(name);
    }
    
    public void setUp() {
        perms = new Permissions();
        Permissions.Permission perm = new Permissions.Permission();
        // Grant extra permissions to read and write the user.* properties and read to the
        // java.home property
        perm.setActions("read, write");
        perm.setName("user.*");
        perm.setClass("java.util.PropertyPermission");
        perms.addConfiguredGrant(perm);
        
        perm = new Permissions.Permission();
        perm.setActions("read");
        perm.setName("java.home");
        perm.setClass("java.util.PropertyPermission");
        perms.addConfiguredGrant(perm);

        perm = new Permissions.Permission();
        perm.setActions("read");
        perm.setName("file.encoding");
        perm.setClass("java.util.PropertyPermission");
        perms.addConfiguredGrant(perm);

        // Revoke permission to write user.home (granted above via user.*), still able to read though.
        // and the default granted permission to read os.name. 
        perm = new Permissions.Permission();
        perm.setActions("write");
        perm.setName("user.home");
        perm.setClass("java.util.PropertyPermission");
        perms.addConfiguredRevoke(perm);        
        
        perm = new Permissions.Permission();
        perm.setActions("read");
        perm.setName("os.*");
        perm.setClass("java.util.PropertyPermission");
        perms.addConfiguredRevoke(perm);        
    }
    
    /** Tests a permission that is granted per default. */
    public void testDefaultGranted() {
        perms.setSecurityManager();
        try {
            String s = System.getProperty("line.separator");      
        } finally {
            perms.restoreSecurityManager();
        }
    }

    /** Tests a permission that has been granted later via wildcard. */
    public void testGranted() {
        perms.setSecurityManager();
        try {
            String s = System.getProperty("user.name");
            System.setProperty("user.name", s);      
        } finally {
            perms.restoreSecurityManager();
        }
    }

    /** Tests a permission that has been granted and revoked later. */
    public void testGrantedAndRevoked() {
        perms.setSecurityManager();
        try {
            String s = System.getProperty("user.home");
            System.setProperty("user.home", s);
            fail("Could perform an action that should have been forbidden.");      
        } catch (SecurityException e){
            // Was expected, test passes
        } finally {
            perms.restoreSecurityManager();
        }
    }

    /** Tests a permission that is granted as per default but revoked later via wildcard. */
    public void testDefaultRevoked() {
        perms.setSecurityManager();
        try {
            System.getProperty("os.name");
            fail("Could perform an action that should have been forbidden.");      
        } catch (SecurityException e){
            // Was expected, test passes
        } finally {
            perms.restoreSecurityManager();
        }
    }
    /** Tests a permission that has not been granted or revoked. */
    public void testOther() {
        String ls = System.getProperty("line.separator");
        perms.setSecurityManager();
        try {
            String s = System.setProperty("line.separator",ls);
            fail("Could perform an action that should have been forbidden.");    
        } catch (SecurityException e){
            // Was expected, test passes
        } finally {
            perms.restoreSecurityManager();
        }
    }
    
    /** Tests an exit condition. */
    public void testExit() {
        perms.setSecurityManager();
        try {
            System.out.println("If this is the last line on standard out the testExit f.a.i.l.e.d");
            System.exit(3);
            fail("Totaly impossible that this fail is ever executed. Please let me know if it is!");
        } catch (ExitException e) {
            if (e.getStatus() != 3) {
                fail("Received wrong exit status in Exit Exception.");
            }
            System.out.println("testExit successfull.");
        } finally {
            perms.restoreSecurityManager();
        }   
    }
    
    
    public void tearDown() {
    }

}