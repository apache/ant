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

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * 
 */
class JupiterParameterizedSampleTest {

	@ParameterizedTest(name = "{index}: fib({0}) = {1}")
	@CsvSource({"0, 0", "1, 1", "2, 1", "3, 2", "4, 3", "5, 5", "6, 8"})
	void fibonacci(final int input, final int expected) {
		assertEquals(expected, compute(input));
	}

    static int compute(int n) {
    	int result = 0;
    	
        if (n <= 1) { 
        	result = n; 
        } else { 
        	result = compute(n - 1) + compute(n - 2); 
        }
        
        return result;
    }
}
