/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2003 The Apache Software Foundation.  All rights
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
 * 4. The names "Ant" and "Apache Software
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

package org.apache.tools.ant.taskdefs.optional.perforce;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.SequenceInputStream;

import org.apache.tools.ant.BuildException;

public abstract class P4HandlerAdapter implements P4Handler {

    public abstract void process(String line);


    String p4input = "";

    //set any data to be written to P4's stdin - messy, needs work
    public void setOutput(String p4Input) {
        this.p4input = p4Input;
    }


    public void start() throws BuildException {

        try {
            //First write any output to P4
            if (p4input != null && p4input.length() > 0 && os != null) {
                os.write(p4input.getBytes());
                os.flush();
                os.close();
            }

            //Now read any input and process
            Thread output = new Thread(new Reader(is));
            Thread error = new Thread(new Reader(es));
            output.start();
            error.start();

        } catch (Exception e) {
            throw new BuildException(e);
        }
    }

    OutputStream os;    //OUtput
    InputStream is;     //Input
    InputStream es;     //Error

    public void setProcessInputStream(OutputStream os) throws IOException {
        this.os = os;
    }

    public void setProcessErrorStream(InputStream is) throws IOException {
        this.es = is;
    }

    public void setProcessOutputStream(InputStream is) throws IOException {
        this.is = is;
    }

    public void stop() {
    }
    
    public class Reader implements Runnable {
        protected InputStream mystream;
        public Reader(InputStream is)
        {
            mystream=is;
        }
        public void setStream(InputStream is) {
            mystream=is;
        }
        public void run() {
            BufferedReader input = new BufferedReader(
                    new InputStreamReader(mystream));

            String line;
            try {
                while ((line = input.readLine()) != null) {
                    synchronized (this){
                        process(line);
                    }
                }
            }
            catch (Exception e) {
                throw new BuildException(e);
            }
        }

    }
}

