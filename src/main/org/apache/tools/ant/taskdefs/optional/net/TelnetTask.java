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

package org.apache.tools.ant.taskdefs.optional.net;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.taskdefs.MatchingTask;
import com.oroinc.net.telnet.*;
import org.apache.tools.ant.BuildException;
import java.io.*;
import java.lang.*;
import java.util.*;

/**
 * Class to provide automated telnet protocol support for the Ant build tool
 *
 * @author Scott Carlson<a href="mailto:ScottCarlson@email.com">ScottCarlson@email.com</a>
 * @version $Revision$
 */

public class TelnetTask extends Task {
    /**
     *  The userid to login with, if automated login is used
     */
    private String userid  = null;

    /**
     *  The password to login with, if automated login is used
     */
    private String password= null;

    /**
     *  The server to connect to. 
     */
    private String server  = null;

    /**
     *  The tcp port to connect to. 
     */
    private int port = 23;

    /**
     *  The Object which handles the telnet session.
     */
    private AntTelnetClient telnet = null;

    /**
     *  The list of read/write commands for this session
     */
    private Vector telnetTasks = new Vector();

    /** 
     *  Verify that all parameters are included. 
     *  Connect and possibly login
     *  Iterate through the list of Reads and writes 
     */
    public void execute() throws BuildException 
    {
       /**  A server name is required to continue */
       if (server== null)
           throw new BuildException("No Server Specified");
       /**  A userid and password must appear together 
        *   if they appear.  They are not required.
        */
       if (userid == null && password != null)
           throw new BuildException("No Userid Specified");
       if (password == null && userid != null)
           throw new BuildException("No Password Specified");

       /**  Create the telnet client object */
       telnet = new AntTelnetClient();
       try {
           telnet.connect(server, port);
       } catch(IOException e) {
           throw new BuildException("Can't connect to "+server);
       }
       /**  Login if userid and password were specified */
       if (userid != null && password != null)
          login();
       /**  Process each sub command */
       Enumeration tasksToRun = telnetTasks.elements();
       while (tasksToRun!=null && tasksToRun.hasMoreElements())
       {
           TelnetSubTask task = (TelnetSubTask) tasksToRun.nextElement();
           task.execute(telnet);
       }
    }

    /**  
     *  Process a 'typical' login.  If it differs, use the read 
     *  and write tasks explicitely
     */
    private void login()
    {
       telnet.waitForString("ogin:");
       telnet.sendString(userid);
       telnet.waitForString("assword:");
       telnet.sendString(password);
    }

    /**
     *  Set the userid attribute 
     */
    public void setUserid(String u) { this.userid = u; }
    /**
     *  Set the password attribute 
     */
    public void setPassword(String p) { this.password = p; }
    /**
     *  Set the server address attribute 
     */
    public void setServer(String m) { this.server = m; }
    /**
     *  Set the tcp port to connect to attribute 
     */
    public void setPort(int p) { this.port = p; }

    /**
     *  A subTask <read> tag was found.  Create the object, 
     *  Save it in our list, and return it.
     */
   
    public TelnetSubTask createRead()
    {
        TelnetSubTask task = (TelnetSubTask)new TelnetRead();
        telnetTasks.addElement(task);
        return task;
    }

    /**
     *  A subTask <write> tag was found.  Create the object, 
     *  Save it in our list, and return it.
     */
    public TelnetSubTask createWrite()
    {
        TelnetSubTask task = (TelnetSubTask)new TelnetWrite();
        telnetTasks.addElement(task);
        return task;
    }

    /**  
     *  This class is the parent of the Read and Write tasks.
     *  It handles the common attributes for both.
     */
    public class TelnetSubTask
    {
        protected String taskString= "";
        public void execute(AntTelnetClient telnet) 
                throws BuildException
        {
            throw new BuildException("Shouldn't be able instantiate a SubTask directly");
        }
        public void addText(String s) { setString(s);}
        public void setString(String s)
        {
           taskString += s; 
        }
    }
    /**
     *  This class sends text to the connected server 
     */
    public class TelnetWrite extends TelnetSubTask
    {
        public void execute(AntTelnetClient telnet) 
               throws BuildException
        {
            telnet.sendString(taskString);
        }
    }
    /**
     *  This class reads the output from the connected server
     *  until the required string is found. 
     */
    public class TelnetRead extends TelnetSubTask
    {
        public void execute(AntTelnetClient telnet) 
               throws BuildException
        {
            telnet.waitForString(taskString);
        }
    }
    /**
     *  This class handles the abstraction of the telnet protocol.
     *  Currently it is a wrapper around <a href="www.oroinc.com">ORO</a>'s 
     *  NetComponents
     */
    public class AntTelnetClient extends TelnetClient
    {
      public void waitForString(String s)
      {
        InputStream is =this.getInputStream();
        try {
          StringBuffer sb = new StringBuffer();
          while (sb.toString().indexOf(s) == -1)
          {
              while (is.available() == 0);
              int iC = is.read();
              Character c = new Character((char)iC);
              sb.append(c);
          }
          log(sb.toString(), Project.MSG_INFO);
        } catch (Exception e)
        { 
            throw new BuildException(e, getLocation());
        }
      }
    
      public void sendString(String s)
      {
        OutputStream os =this.getOutputStream();
        try {
          os.write((s + "\n").getBytes());
          log(s, Project.MSG_INFO);
          os.flush();
        } catch (Exception e)
        { 
          throw new BuildException(e, getLocation());
        }
      }
    }
}
