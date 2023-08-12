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
package org.example.junitlauncher.jupiter;

import org.example.junitlauncher.FooBarData;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class SharedDataAccessorTest2 {

    @Test
    void testData() {
        final String firstAccess = FooBarData.getData();
        assertNull(firstAccess, "expected FooBarData.getData() to return null on first call," +
                " but returned " + firstAccess);
        // now repeatedly set some data and expect the next call to getData() to return that value
        final String prefix = this.getClass().getName() + "-"
                + Thread.currentThread().getName() + "-";
        for (int i = 0; i < 1000; i++) {
            final String val = prefix + i;
            FooBarData.setData(val);
            assertEquals(val, FooBarData.getData(), "unexpected value from FooBarData.getData()");
        }
    }
}
