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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class JUnit4Test {

    private static File outputFile;

    @BeforeClass
    public static void readExpectedValue() {
        String file = System.getProperty("testOutputFile");
        System.out.println("Using " + file + " for output");
        outputFile = new File(file);
    }

    static List<String> list = new ArrayList<String>();
    @Test public void testA() {list.add("A");}
    @Test public void testB() {list.add("B");}
    @Test public void testC() {list.add("C");}
    @Test public void testD() {list.add("D");}
    @Test public void testE() {list.add("E");}
    @Test public void testF() {list.add("F");}
    @Test public void testG() {list.add("G");}
    @Test public void testH() {list.add("H");}
    @Test public void testI() {list.add("I");}
    @Test public void testJ() {list.add("J");}
    @Test public void testK() {list.add("K");}
    @Test public void testL() {list.add("L");}
    @Test public void testM() {list.add("M");}
    @Test public void testN() {list.add("N");}
    @Test public void testO() {list.add("O");}
    @Test public void testP() {list.add("P");}
    @Test public void testQ() {list.add("Q");}
    @Test public void testR() {list.add("R");}
    @Test public void testS() {list.add("S");}
    @Test public void testT() {list.add("T");}
    @Test public void testU() {list.add("U");}
    @Test public void testV() {list.add("V");}
    @Test public void testW() {list.add("W");}
    @Test public void testX() {list.add("X");}
    @Test public void testY() {list.add("Y");}
    @Test public void testZ() {list.add("Z");}



    @AfterClass
    public static void printAll() throws IOException {
        FileWriter output = new FileWriter(outputFile, true);
        System.out.println("Tests order: " + list);
        output.write(list.toString());
        output.write("\n");
        output.close();
    }

}
