

/*
Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements.  See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.Assert;

import java.io.*;

/**
 * Verify if the two lines in the file are the same
 */
public class JUnit4TestOrderVerify {


    private static boolean expectToBeTheSame;
    private static File outputFile;

    @BeforeClass
    public static void readExpectedValue() {
        expectToBeTheSame = Boolean.valueOf(System.getProperty("expectToBeTheSame"));
        String file = System.getProperty("testOutputFile");
        outputFile = new File(file);
    }

    @Test

    public void verify() throws IOException {

        BufferedReader in = new BufferedReader(new FileReader(outputFile));
        String first = in.readLine();
        String second = in.readLine();

        if (expectToBeTheSame) {
            Assert.assertEquals(first, second);
        } else {
            Assert.assertNotEquals(first, second);
        }
    }
}
