/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.tools.ant.types;

import org.apache.tools.ant.ExitException;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;

/**
 * JUnit 4 testcases for org.apache.tools.ant.types.Permissions.
 */
public class PermissionsTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    Permissions perms;

    @Before
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

        // Allow loading Hamcrest Matcher classes on demand
        perm = new Permissions.Permission();
        perm.setActions("read");
        perm.setName("<<ALL FILES>>");
        perm.setClass("java.io.FilePermission");
        perms.addConfiguredGrant(perm);

        perms.setSecurityManager();
    }

    @After
    public void tearDown() {
        perms.restoreSecurityManager();
    }

    /** Tests a permission that is granted per default. */
    @Test
    public void testDefaultGranted() {
        System.getProperty("line.separator");
    }

    /** Tests a permission that has been granted later via wildcard. */
    @Test
    public void testGranted() {
        System.setProperty("user.name", System.getProperty("user.name"));
    }

    /** Tests a permission that has been granted and revoked later. */
    @Test(expected = SecurityException.class)
    public void testGrantedAndRevoked() {
        System.setProperty("user.home", System.getProperty("user.home"));
    }

    /** Tests a permission that is granted as per default but revoked later via wildcard. */
    @Test(expected = SecurityException.class)
    public void testDefaultRevoked() {
        System.getProperty("os.name");
    }

    /** Tests a permission that has not been granted or revoked. */
    @Test(expected = SecurityException.class)
    public void testOther() {
        System.setProperty("line.separator", System.lineSeparator());
    }

    /** Tests an exit condition. */
    @Test
    public void testExit() {
        thrown.expect(ExitException.class);
        thrown.expect(hasProperty("status", equalTo(3)));
        try {
            System.out.println("If this is the last line on standard out the testExit f.a.i.l.e.d");
            System.exit(3);
        } finally {
            System.out.println("testExit successful.");
        }
    }

}
