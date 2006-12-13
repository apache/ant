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

package org.apache.tools.ant.types.selectors;


import java.util.Iterator;
import org.apache.tools.ant.types.selectors.modifiedselector.Cache;

public class MockCache implements Cache {

    public boolean debug = false;
    public boolean saved = false;


    public MockCache() {
        log("()");
    }

    public boolean isValid() {
        log(".isValid()");
        return true;
    }
    public void delete() {
        log(".delete()");
    }
    public void load() {
        log(".load()");
    }
    public void save() {
        log(".save()");
        saved = true;
    }
    public Object get(Object key) {
        log(".get("+key+")");
        return key;
    }
    public void put(Object key, Object value) {
        log(".put("+key+", "+value+")");
        saved = false;
    }
    public Iterator iterator() {
        log("iterator()");
        return null;
    }
    public String toString() {
        return "MockCache@" + hashCode();
    }

    private void log(String msg) {
        if (debug) System.out.println(this+msg);
    }
}//class-MockCache
