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

package org.apache.tools.ant.taskdefs.email;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @since Ant 1.6
 */
public class EmailAddressTest {

    @Test
    public void test1() {
        expectNameAddress(new EmailAddress("address (name)"));
    }

    @Test
    public void test2() {
        expectNameAddress(new EmailAddress("(name) address"));
    }

    @Test
    public void test3() {
        expectNameAddress(new EmailAddress("name <address>"));
    }

    @Test
    public void test4() {
        expectNameAddress(new EmailAddress("<address> name"));
    }

    @Test
    public void test5() {
        expectNameAddress(new EmailAddress("<address> (name)"));
    }

    @Test
    public void test6() {
        expectNameAddress(new EmailAddress("(name) <address>"));
    }

    @Test
    public void test7() {
        expectNameAddress2(new EmailAddress("address (<name>)"));
    }

    @Test
    public void test8() {
        expectNameAddress2(new EmailAddress("(<name>) address"));
    }

    @Test
    public void test9() {
        expectNameAddress3(new EmailAddress("address"));
    }

    @Test
    public void testA() {
        expectNameAddress3(new EmailAddress("<address>"));
    }

    @Test
    public void testB() {
        expectNameAddress3(new EmailAddress(" <address> "));
    }

    @Test
    public void testC() {
        expectNameAddress3(new EmailAddress("< address >"));
    }

    @Test
    public void testD() {
        expectNameAddress3(new EmailAddress(" < address > "));
    }

    private void expectNameAddress(EmailAddress e) {
        assertEquals("name", e.getName());
        assertEquals("address", e.getAddress());
    }

    // where the name contains <>
    private void expectNameAddress2(EmailAddress e) {
        assertEquals("<name>", e.getName());
        assertEquals("address", e.getAddress());
    }

    // where only an address is supplied
    private void expectNameAddress3(EmailAddress e) {
        assertNull("Expected null, found <" + e.getName() + ">", e.getName());
        assertEquals("address", e.getAddress());
    }
}
