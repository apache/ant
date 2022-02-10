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
package org.apache.tools.ant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;

import java.io.File;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

import org.apache.tools.ant.util.StringUtils;
import org.junit.Before;
import org.junit.Test;

public class IntrospectionHelperSetOptionalAttributesTest {
    public static class HavingOptionals {
        private Optional<String> foo;
        private Optional<File> bar;
        @SuppressWarnings("rawtypes")
        private Optional baz;
        private OptionalInt a;
        private OptionalLong b;
        private OptionalDouble c;

        public Optional<String> getFoo() {
            return foo;
        }

        public void setFoo(Optional<String> foo) {
            this.foo = foo;
        }

        public Optional<File> getBar() {
            return bar;
        }

        public void setBar(Optional<File> bar) {
            this.bar = bar;
        }

        public OptionalInt getA() {
            return a;
        }

        public void setA(OptionalInt a) {
            this.a = a;
        }

        public OptionalLong getB() {
            return b;
        }

        public void setB(OptionalLong b) {
            this.b = b;
        }

        public OptionalDouble getC() {
            return c;
        }

        public void setC(OptionalDouble c) {
            this.c = c;
        }

        @SuppressWarnings("rawtypes")
        public Optional getBaz() {
            return baz;
        }

        @SuppressWarnings("rawtypes")
        public void setBaz(Optional baz) {
            this.baz = baz;
        }
    }

    private Project p;
    private IntrospectionHelper ih;
    private HavingOptionals subject;

    @Before
    public void setup() {
        p = new Project();
        p.setBasedir(File.separator);
        ih = IntrospectionHelper.getHelper(HavingOptionals.class);
        subject = new HavingOptionals();
    }

    @Test
    public void testOptionalString() {
        ih.setAttribute(p, subject, "foo", "fooValue");
        assertEquals("fooValue", subject.getFoo().get());
    }

    @Test
    public void testEmptyOptionalString() {
        ih.setAttribute(p, subject, "foo", null);
        assertFalse(subject.getFoo().isPresent());
    }

    @Test
    public void testOptionalFile() {
        ih.setAttribute(p, subject, "bar", "barFile");
        assertEquals(p.resolveFile("barFile"), subject.getBar().get());
    }

    @Test
    public void testEmptyOptionalFile() {
        ih.setAttribute(p, subject, "bar", null);
        assertFalse(subject.getBar().isPresent());
    }

    @Test
    public void testOptionalRaw() {
        assertThrows(BuildException.class, () -> ih.setAttribute(p, subject, "baz", "bazValue"));
    }

    @Test
    public void testOptionalInt() {
        ih.setAttribute(p, subject, "a", "6");
        assertEquals(6, subject.getA().getAsInt());
    }

    @Test
    public void testEmptyOptionalInt() {
        ih.setAttribute(p, subject, "a", null);
        assertFalse(subject.getA().isPresent());
    }

    @Test
    public void testOptionalLong() throws Exception {
        ih.setAttribute(p, subject, "b", "6K");
        assertEquals(StringUtils.parseHumanSizes("6K"), subject.getB().getAsLong());
    }

    @Test
    public void testEmptyOptionalLong() throws Exception {
        ih.setAttribute(p, subject, "b", null);
        assertFalse(subject.getB().isPresent());
    }

    @Test
    public void testOptionalDouble() {
        ih.setAttribute(p, subject, "c", "6.66");
        assertEquals(6.66, subject.getC().getAsDouble(), 0.00001);
    }

    @Test
    public void testEmptyOptionalDouble() {
        ih.setAttribute(p, subject, "c", null);
        assertFalse(subject.getC().isPresent());
    }
}
