/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "The Jakarta Project", "Ant", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */

package org.apache.tools.ant.taskdefs;

import junit.framework.*;
import java.io.*;
import org.apache.tools.ant.*;

/**
 * Some basic tests mainly intended to test for jar/classname behavior.
 * @todo has to be enhanced to use TaskdefsTest instead.
 * @author <a href="mailto:sbailliez@apache.org>Stephane Bailliez</a> 
 */
public class JavaTest extends TestCase { 
    
    protected Java java;
    
    public JavaTest(String name) { 
        super(name);
    }    
    
    public void setUp() { 
        java = new Java();
    }

    public void testNoJarNoClassname(){
        try {
            java.execute();
            fail("Should have failed. Cannot run with no classname nor jar");
        } catch (BuildException e){
            assertEquals("Classname must not be null.", e.getMessage());
        }
    }

    public void testJarNoFork() {
        java.setJar( new File("test.jar") );
        java.setFork(false);
        try {
            java.execute();
            fail("Should have failed. Cannot run jar in non-forked mode");
        } catch (BuildException e){
            assertEquals("Cannot execute a jar in non-forked mode. Please set fork='true'. ", e.getMessage());
        }
    }
       
    public void testClassname() { 
        java.setClassname("test.class.Name");
    }
    
    public void testJarAndClassName() { 
        try {
            java.setJar( new File("test.jar") );
            java.setClassname("test.class.Name");
            fail("Should not be able to set both classname AND jar");
        } catch (BuildException e){
        }
    }

    public void testClassnameAndJar() { 
        try {
            java.setClassname("test.class.Name");
            java.setJar( new File("test.jar") );
            fail("Should not be able to set both classname AND jar");
        } catch (BuildException e){
        }
    }
    
}
