/*
 *  The Apache Software License, Version 1.1
 *
 *  Copyright (c) 2002 The Apache Software Foundation.  All rights
 *  reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions
 *  are met:
 *
 *  1. Redistributions of source code must retain the above copyright
 *  notice, this list of conditions and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright
 *  notice, this list of conditions and the following disclaimer in
 *  the documentation and/or other materials provided with the
 *  distribution.
 *
 *  3. The end-user documentation included with the redistribution, if
 *  any, must include the following acknowlegement:
 *  "This product includes software developed by the
 *  Apache Software Foundation (http://www.apache.org/)."
 *  Alternately, this acknowlegement may appear in the software itself,
 *  if and wherever such third-party acknowlegements normally appear.
 *
 *  4. The names "Ant" and "Apache Software
 *  Foundation" must not be used to endorse or promote products derived
 *  from this software without prior written permission. For written
 *  permission, please contact apache@apache.org.
 *
 *  5. Products derived from this software may not be called "Apache"
 *  nor may "Apache" appear in their names without prior written
 *  permission of the Apache Group.
 *
 *  THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 *  WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 *  OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 *  ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 *  USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 *  OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 *  OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 *  SUCH DAMAGE.
 *  ====================================================================
 *
 *  This software consists of voluntary contributions made by many
 *  individuals on behalf of the Apache Software Foundation.  For more
 *  information on the Apache Software Foundation, please see
 *  <http://www.apache.org/>.
 */
package org.apache.tools.ant.taskdefs;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildFileTest;
import java.lang.reflect.InvocationTargetException;

/**
 * @author steve_l
 * @created 13 January 2002
 */
public class ConditionTest extends BuildFileTest {

    /**
     * Constructor for the ConditionTest object
     *
     * @param name we dont know
     */
    public ConditionTest(String name) {
        super(name);
    }


    /**
     * The JUnit setup method
     */
    public void setUp() {
        configureProject("src/etc/testcases/taskdefs/condition.xml");
    }


    /**
     * The teardown method for JUnit
     */
    public void tearDown() {
        executeTarget("cleanup");
    }

    public void testBasic() {
       expectPropertySet("basic","basic"); 
    }

    public void testConditionIncomplete() {
        expectSpecificBuildException("condition-incomplete", 
                                     "property attribute has been omitted",
                                     "The property attribute is required."); 
    }
    
    public void testConditionEmpty() {
        expectSpecificBuildException("condition-empty", 
                                     "no conditions",
                                     "You must nest a condition into <condition>"); 
    }    

    public void testShortcut() {
        expectPropertySet("shortcut","shortcut","set"); 
    }
    
    public void testUnset() {
        expectPropertyUnset("dontset","dontset"); 
    }
  
    public void testSetValue() {
        expectPropertySet("setvalue","setvalue","woowoo"); 
    }
    
    public void testNegation() {
        expectPropertySet("negation","negation"); 
    }
    
    public void testNegationFalse() {
        expectPropertyUnset("negationfalse","negationfalse"); 
    }
    
    public void testNegationIncomplete() {
        expectSpecificBuildException("negationincomplete", 
                                     "no conditions in <not>",
                                     "You must nest a condition into <not>"); 
    }
    
    public void testAnd() {
        expectPropertySet("and","and"); 
    }   
        
    public void testAndFails() {
        expectPropertyUnset("andfails","andfails"); 
    }   
 
    public void testAndIncomplete() {
        expectPropertyUnset("andincomplete","andincomplete"); 
    } 

    public void testAndempty() {
        expectPropertySet("andempty","andempty"); 
    }   
    
    public void testOr() {
        expectPropertySet("or","or"); 
    }

    public void testOrincomplete() {
        expectPropertySet("or","or"); 
    } 
  
    public void testOrFails() {
        expectPropertyUnset("orfails","orfails"); 
    }   
 
    public void testOrboth() {
        expectPropertySet("orboth","orboth"); 
    }   

    public void testFilesmatchIdentical() {
        expectPropertySet("filesmatch-identical","filesmatch-identical"); 
    }       
    
    
    public void testFilesmatchIncomplete() {
        expectSpecificBuildException("filesmatch-incomplete", 
                                     "Missing file2 attribute",
                                     "both file1 and file2 are required in filesmatch"); 
    }
    
    public void testFilesmatchOddsizes() {
        expectPropertyUnset("filesmatch-oddsizes","filesmatch-oddsizes"); 
    }    
    
    public void testFilesmatchExistence() {
        expectPropertyUnset("filesmatch-existence", "filesmatch-existence"); 
    } 

    public void testFilesmatchDifferent() {
        expectPropertyUnset("filesmatch-different","filesmatch-different"); 
    } 
    
    public void testFilesmatchMatch() {
        expectPropertySet("filesmatch-match","filesmatch-match"); 
    }   
    
    public void testFilesmatchDifferentSizes() {
        expectPropertyUnset("filesmatch-different-sizes",
            "filesmatch-different-sizes"); 
    } 

    public void testFilesmatchDifferentOnemissing() {
        expectPropertyUnset("filesmatch-different-onemissing",
            "filesmatch-different-onemissing"); 
    } 
    
    public void testContains() {
        expectPropertySet("contains","contains"); 
    }   
        
    
    public void testContainsDoesnt() {
        expectPropertyUnset("contains-doesnt","contains-doesnt"); 
    }   

    public void testContainsAnycase() {
        expectPropertySet("contains-anycase","contains-anycase"); 
    } 

    
    public void testContainsIncomplete1() {
        expectSpecificBuildException("contains-incomplete1", 
                    "Missing contains attribute",
                    "both string and substring are required in contains"); 
    } 
    
    public void testContainsIncomplete2() {
        expectSpecificBuildException("contains-incomplete2", 
                    "Missing contains attribute",
                    "both string and substring are required in contains"); 
    } 
    
    public void testIstrue() {
        expectPropertySet("istrue","istrue"); 
    } 

    public void testIstrueNot() {
        expectPropertyUnset("istrue-not","istrue-not"); 
    } 
 
    public void testIstrueFalse() {
        expectPropertyUnset("istrue-false","istrue-false"); 
    } 

    
    public void testIstrueIncomplete1() {
        expectSpecificBuildException("istrue-incomplete", 
                    "Missing attribute",
                    "Nothing to test for truth"); 
    } 

    public void testIsfalseTrue() {
        expectPropertyUnset("isfalse-true","isfalse-true"); 
    } 

    public void testIsfalseNot() {
        expectPropertySet("isfalse-not","isfalse-not"); 
    } 
 
    public void testIsfalseFalse() {
        expectPropertySet("isfalse-false","isfalse-false"); 
    } 

    
    public void testIsfalseIncomplete1() {
        expectSpecificBuildException("isfalse-incomplete", 
                    "Missing attribute",
                    "Nothing to test for falsehood"); 
    }     
    
}

