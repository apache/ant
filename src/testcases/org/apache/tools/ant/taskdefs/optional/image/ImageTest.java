/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2002 The Apache Software Foundation.  All rights
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

package org.apache.tools.ant.taskdefs.optional.image;

import org.apache.tools.ant.BuildFileTest;

import java.io.IOException;
import java.io.File;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.BufferedReader;
import java.util.Properties;

/**
 * Tests the Image task.
 *
 * @author    Eric Pugh <a href="mailto:dep4b@yahoo.com">dep4b@yahoo.com</a>
 * @since     Ant 1.5
 */
public class ImageTest extends BuildFileTest {

    private final static String TASKDEFS_DIR = "src/etc/testcases/taskdefs/optional/image/";
    private final static String LARGEIMAGE = "largeimage.jpg";

    public ImageTest(String name) {
        super(name);
    }


    public void setUp() {
        configureProject(TASKDEFS_DIR + "image.xml");
    }


    public void tearDown() {
        executeTarget("cleanup");
    }

    public void testEchoToLog() {
        expectLogContaining("testEchoToLog", "Processing File");
    }

    public void testSimpleScale(){
      expectLogContaining("testSimpleScale", "Processing File");
      File f = createRelativeFile( "/dest/" + LARGEIMAGE  );
          assertTrue(
              "Did not create "+f.getAbsolutePath(),
        f.exists() );

    }

    public void testOverwriteTrue() {
      expectLogContaining("testSimpleScale", "Processing File");
      File f = createRelativeFile( "/dest/" + LARGEIMAGE  );
      long lastModified = f.lastModified();
      expectLogContaining("testOverwriteTrue", "Processing File");
      f = createRelativeFile( "/dest/" + LARGEIMAGE  );
      long overwrittenLastModified = f.lastModified();
      assertTrue("File was not overwritten.",lastModified < overwrittenLastModified);
    }

    public void testOverwriteFalse() {
      expectLogContaining("testSimpleScale", "Processing File");
      File f = createRelativeFile( "/dest/" + LARGEIMAGE  );
      long lastModified = f.lastModified();
      expectLogContaining("testOverwriteFalse", "Processing File");
      f = createRelativeFile( "/dest/" + LARGEIMAGE  );
      long overwrittenLastModified = f.lastModified();
      assertTrue("File was overwritten.",lastModified == overwrittenLastModified);
    }


    public void off_testFailOnError() {
      try {
        expectLogContaining("testFailOnError", "Unable to process image stream");
      }
      catch (RuntimeException re){
        assertTrue("Run time exception should say 'Unable to process image stream'. :" + re.toString(),re.toString().indexOf("Unable to process image stream") > -1);
      }
    }



  protected File createRelativeFile( String filename ) {
        if (filename.equals( "." )) {
            return getProjectDir();
        }
        // else
        return new File( getProjectDir(), filename );
    }
}

