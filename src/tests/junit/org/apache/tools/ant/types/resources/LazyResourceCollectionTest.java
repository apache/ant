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
package org.apache.tools.ant.types.resources;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import junit.framework.TestCase;

import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;

public class LazyResourceCollectionTest extends TestCase {

    private class StringResourceCollection implements ResourceCollection {
        List resources = Arrays.asList(new Resource[] {});

        List createdIterators = new ArrayList();

        public int size() {
            return resources.size();
        }

        public Iterator iterator() {
            StringResourceIterator it = new StringResourceIterator();
            createdIterators.add(it);
            return it;
        }

        public boolean isFilesystemOnly() {
            return false;
        }
    }

    private class StringResourceIterator implements Iterator {
        int cursor = 0;

        public void remove() {
            throw new UnsupportedOperationException();
        }

        public Object next() {
            if (cursor < 3) {
                cursor++;
                return new StringResource("r" + cursor);
            }
            return null;
        }

        public boolean hasNext() {
            return cursor < 3;
        }
    }

    public void testLazyLoading() throws Exception {
        StringResourceCollection collectionTest = new StringResourceCollection();
        LazyResourceCollectionWrapper lazyCollection = new LazyResourceCollectionWrapper();
        lazyCollection.add(collectionTest);

        Iterator it = lazyCollection.iterator();
        assertOneCreatedIterator(collectionTest);
        StringResourceIterator stringResourceIterator = (StringResourceIterator) collectionTest.createdIterators
                .get(0);
        assertEquals("A resource was loaded without iterating", 1,
                stringResourceIterator.cursor);

        StringResource r = (StringResource) it.next();
        assertOneCreatedIterator(collectionTest);
        assertEquals("r1", r.getValue());
        assertEquals("Iterating once load more than 1 resource", 2,
                stringResourceIterator.cursor);

        r = (StringResource) it.next();
        assertOneCreatedIterator(collectionTest);
        assertEquals("r2", r.getValue());
        assertEquals("Iterating twice load more than 2 resources", 3,
                stringResourceIterator.cursor);

        r = (StringResource) it.next();
        assertOneCreatedIterator(collectionTest);
        assertEquals("r3", r.getValue());
        assertEquals("Iterating 3 times load more than 3 resources", 3,
                stringResourceIterator.cursor);

        try {
            it.next();
            fail("NoSuchElementException shoudl have been raised");
        } catch (NoSuchElementException e) {
            // ok
        }
    }

    private void assertOneCreatedIterator(
            StringResourceCollection testCollection) {
        assertEquals("More than one iterator has been created", 1,
                testCollection.createdIterators.size());
    }

    public void testCaching() throws Exception {
        StringResourceCollection collectionTest = new StringResourceCollection();
        LazyResourceCollectionWrapper lazyCollection = new LazyResourceCollectionWrapper();
        lazyCollection.add(collectionTest);

        assertTrue(lazyCollection.isCache());
        Iterator it1 = lazyCollection.iterator();
        assertOneCreatedIterator(collectionTest);
        Iterator it2 = lazyCollection.iterator();
        assertOneCreatedIterator(collectionTest);

        StringResourceIterator stringResourceIterator = (StringResourceIterator) collectionTest.createdIterators
                .get(0);
        assertEquals("A resource was loaded without iterating", 1,
                stringResourceIterator.cursor);

        StringResource r = (StringResource) it1.next();
        assertEquals("r1", r.getValue());
        assertEquals("Iterating once load more than 1 resource", 2,
                stringResourceIterator.cursor);

        r = (StringResource) it2.next();
        assertEquals("r1", r.getValue());
        assertEquals(
                "The second iterator did not lookup in the cache for a resource",
                2, stringResourceIterator.cursor);

        r = (StringResource) it2.next();
        assertEquals("r2", r.getValue());
        assertEquals("Iterating twice load more than 2 resources", 3,
                stringResourceIterator.cursor);

        r = (StringResource) it1.next();
        assertEquals("r2", r.getValue());
        assertEquals(
                "The first iterator did not lookup in the cache for a resource",
                3, stringResourceIterator.cursor);

        r = (StringResource) it2.next();
        assertEquals("r3", r.getValue());
        assertEquals("Iterating 3 times load more than 3 resources", 3,
                stringResourceIterator.cursor);

        r = (StringResource) it1.next();
        assertEquals("r3", r.getValue());
        assertEquals(
                "The first iterator did not lookup in the cache for a resource",
                3, stringResourceIterator.cursor);

        try {
            it1.next();
            fail("NoSuchElementException should have been raised");
        } catch (NoSuchElementException e) {
            // ok
        }

        try {
            it2.next();
            fail("NoSuchElementException should have been raised");
        } catch (NoSuchElementException e) {
            // ok
        }
    }
}
