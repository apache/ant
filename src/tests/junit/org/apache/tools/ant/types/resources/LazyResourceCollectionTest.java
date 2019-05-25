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
package org.apache.tools.ant.types.resources;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LazyResourceCollectionTest {

    private class StringResourceCollection implements ResourceCollection {
        List<StringResourceIterator> createdIterators = new ArrayList<>();

        @Override
        public int size() {
            return 3;
        }

        @Override
        public Iterator<Resource> iterator() {
            StringResourceIterator it = new StringResourceIterator();
            createdIterators.add(it);
            return it;
        }

        @Override
        public boolean isFilesystemOnly() {
            return false;
        }
    }

    private class StringResourceIterator implements Iterator<Resource> {
        int cursor = 0;

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        @Override
        public StringResource next() {
            if (cursor < 3) {
                cursor++;
                return new StringResource("r" + cursor);
            }
            return null;
        }

        @Override
        public boolean hasNext() {
            return cursor < 3;
        }
    }

    @Test
    public void testLazyLoading() {
        StringResourceCollection collectionTest = new StringResourceCollection();
        LazyResourceCollectionWrapper lazyCollection = new LazyResourceCollectionWrapper();
        lazyCollection.add(collectionTest);

        Iterator<Resource> it = lazyCollection.iterator();
        assertOneCreatedIterator(collectionTest);
        StringResourceIterator stringResourceIterator = collectionTest.createdIterators.get(0);
        assertEquals("A resource was loaded without iterating", 1,
            stringResourceIterator.cursor);

        assertStringValue("r1", it.next());
        assertOneCreatedIterator(collectionTest);
        assertEquals("Iterating once load more than 1 resource", 2,
            stringResourceIterator.cursor);

        assertStringValue("r2", it.next());
        assertOneCreatedIterator(collectionTest);
        assertEquals("Iterating twice load more than 2 resources", 3,
            stringResourceIterator.cursor);

        assertStringValue("r3", it.next());
        assertOneCreatedIterator(collectionTest);
        assertEquals("Iterating 3 times load more than 3 resources", 3,
            stringResourceIterator.cursor);
    }

    @Test(expected = NoSuchElementException.class)
    public void testLazyLoadingFailsOnceWrappedCollectionIsExhausted() {
        StringResourceCollection collectionTest = new StringResourceCollection();
        LazyResourceCollectionWrapper lazyCollection = new LazyResourceCollectionWrapper();
        lazyCollection.add(collectionTest);

        Iterator<Resource> it = lazyCollection.iterator();
        it.next();
        it.next();
        it.next();
        it.next();
    }

    private void assertOneCreatedIterator(
            StringResourceCollection testCollection) {
        assertEquals("More than one iterator has been created", 1,
                testCollection.createdIterators.size());
    }

    @Test
    public void testCaching() {
        StringResourceCollection collectionTest = new StringResourceCollection();
        LazyResourceCollectionWrapper lazyCollection = new LazyResourceCollectionWrapper();
        lazyCollection.add(collectionTest);

        assertTrue(lazyCollection.isCache());
        Iterator<Resource> it1 = lazyCollection.iterator();
        assertOneCreatedIterator(collectionTest);
        Iterator<Resource> it2 = lazyCollection.iterator();
        assertOneCreatedIterator(collectionTest);

        StringResourceIterator stringResourceIterator = collectionTest.createdIterators.get(0);
        assertEquals("A resource was loaded without iterating", 1,
            stringResourceIterator.cursor);

        assertStringValue("r1", it1.next());
        assertEquals("Iterating once load more than 1 resource", 2,
            stringResourceIterator.cursor);

        assertStringValue("r1", it2.next());
        assertEquals(
            "The second iterator did not lookup in the cache for a resource", 2,
            stringResourceIterator.cursor);

        assertStringValue("r2", it2.next());
        assertEquals("Iterating twice load more than 2 resources", 3,
            stringResourceIterator.cursor);

        assertStringValue("r2", it1.next());
        assertEquals(
            "The first iterator did not lookup in the cache for a resource", 3,
            stringResourceIterator.cursor);

        assertStringValue("r3", it2.next());
        assertEquals("Iterating 3 times load more than 3 resources", 3,
            stringResourceIterator.cursor);

        assertStringValue("r3", it1.next());
        assertEquals(
            "The first iterator did not lookup in the cache for a resource", 3,
            stringResourceIterator.cursor);
    }

    @Test(expected = NoSuchElementException.class)
    public void testCachingFailsOnceWrappedCollectionIsExhausted() {
        StringResourceCollection collectionTest = new StringResourceCollection();
        LazyResourceCollectionWrapper lazyCollection = new LazyResourceCollectionWrapper();
        lazyCollection.add(collectionTest);

        Iterator<Resource> it1 = lazyCollection.iterator();
        Iterator<Resource> it2 = lazyCollection.iterator();

        it1.next();
        it2.next();
        it2.next();
        it1.next();
        it2.next();
        it1.next();

        // next() must throw the expected NoSuchElementException;
        // if that does not happen, assertions throw the unexpected Assertion error
        try {
            it1.next();
            assertTrue(it1.hasNext());
        } finally {
            it2.next();
            assertTrue(it2.hasNext());
        }
    }

    private void assertStringValue(String expected, Resource r) {
        assertEquals(expected, r.as(StringResource.class).getValue());
    }
}
