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


package org.apache.tools.ant.taskdefs.optional.ejb;

import java.io.*;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.io.StringReader;
import java.net.*;
import java.util.*;
import java.util.Iterator;
import java.util.jar.*;
import javax.xml.parsers.*;

import org.apache.tools.ant.*;
import org.apache.tools.ant.taskdefs.ExecTask;
import org.apache.tools.ant.taskdefs.Java;
import org.apache.tools.ant.types.*;
import org.apache.tools.ant.types.Commandline.Argument;
import org.xml.sax.*;

/**
 * BorlandGenerateClient is dedicated to the Borland Application Server 4.5
 * This task generates the client jar using as input the ejb jar file.
 * 
 * @author     <a href="mailto:benoit.moussaud@criltelecom.com">Benoit Moussaud</a>
 *
 */
public class BorlandGenerateClient extends Task
{

    /** debug the generateclient task */
    boolean debug = false;

    /** hold the ejbjar file name */
    File ejbjarfile = null;

    /** hold the client jar file name */
    File clientjarfile = null;

    public void setDebug(boolean debug)
    {
        this.debug = debug;
    }

    public void setEjbjar(File ejbfile)
    {
        ejbjarfile = ejbfile;
    }
    public void setClientjar(File clientjar)
    {
        clientjarfile = clientjar;
    }

    /**
     * Do the work.
     *
     * The work is actually done by creating a separate JVM to run a java task. 
     *
     * @exception BuildException if someting goes wrong with the build
     */
    public void execute() throws BuildException 
    {
        try
        {
            if ( ejbjarfile == null ||
                 ejbjarfile.isDirectory()) 
            {
                throw new BuildException("invalid ejb jar file.");
            } // end of if ()

            if ( clientjarfile == null ||
                 clientjarfile.isDirectory()) 
            {
                log("invalid or missing client jar file.",Project.MSG_VERBOSE);
                String ejbjarname = ejbjarfile.getAbsolutePath();
                //clientname = ejbjarfile+client.jar
                String clientname = ejbjarname.substring(0,ejbjarname.lastIndexOf("."));
                clientname = clientname + "client.jar";
                clientjarfile = new File(clientname);

            } // end of if ()

            log("client jar file is " + clientjarfile);

            org.apache.tools.ant.taskdefs.ExecTask execTask = null;                
            execTask = (ExecTask) getProject().createTask("exec");
                       
            execTask.setDir(new File("."));
            execTask.setExecutable("iastool");
            execTask.createArg().setValue("generateclient");
            if ( debug ) 
            {
                execTask.createArg().setValue("-trace");                
            } // end of if ()

            //
            execTask.createArg().setValue("-short");
            execTask.createArg().setValue("-jarfile");
            // ejb jar file
            execTask.createArg().setValue(ejbjarfile.getAbsolutePath());
            //client jar file
            execTask.createArg().setValue("-single");
            execTask.createArg().setValue("-clientjarfile");
            execTask.createArg().setValue(clientjarfile.getAbsolutePath());

            log("Calling java2iiop",Project.MSG_VERBOSE);                       
            execTask.execute();        
        }
        catch (Exception e) 
        {
            // Have to catch this because of the semantics of calling main()
            String msg = "Exception while calling generateclient Details: " + e.toString();
            throw new BuildException(msg, e);
        }

    }
    
}
