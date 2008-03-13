/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.tools.ant.types;

import junit.framework.TestCase;

import org.apache.tools.ant.ExitException;

/**
 * JUnit 3 testcases for org.apache.tools.ant.types.Permissions.
 *
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
            System.out.println("testExit successful.");
        } finally {
            perms.restoreSecurityManager();
        }
    }


    public void tearDown() {
    }

}
