/* 
 * Copyright  2002,2004 Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
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
package org.example.junit;

import junit.framework.TestCase;

/**
 * Not really a test of Ant but a test that is run by the test of the
 * junit task.  Confused?
 *
 * @author Stefan Bodewig
 * @since Ant 1.5
 */
public class Output extends TestCase {

    public Output(String s) {
        super(s);
    }

    public void testOutput() {
        System.out.println("foo");
    }
}
