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
 *
 * Portions of this software are based upon public domain software
 * originally written at the National Center for Supercomputing Applications,
 * University of Illinois, Urbana-Champaign.
 */

package org.apache.tools.ant.taskdefs.optional.perforce;

import org.apache.tools.ant.*;

import java.util.Date;
import java.text.SimpleDateFormat;


/** P4Label - create a Perforce Label.
 *
 *  P4Label inserts a label into perforce reflecting the
 *  current client contents.
 *
 *  Label name defaults to AntLabel if none set.
 *
 * Example Usage: <P4Label name="MyLabel-${TSTAMP}-${DSTAMP}" desc="Auto Build Label" />
 *
 * @author <A HREF="mailto:leslie.hughes@rubus.com">Les Hughes</A>
 */
public class P4Label extends P4Base {

    protected String name;
    protected String desc;
    protected String lock;
    
    public void setName(String name) {
        this.name = name;
    }
    
    public void setDesc(String desc) {
        this.desc = desc;
    }
   
 	public void setLock(String lock)  {
    	this.lock = lock;
 	}
        
    public void execute() throws BuildException {
        log("P4Label exec:",Project.MSG_INFO);
        
        if(P4View == null || P4View.length() < 1) {
            log("View not set, assuming //depot/...", Project.MSG_WARN);
            P4View = "//depot/...";
        }
        
        if(desc == null || desc.length() < 1) {
            log("Label Description not set, assuming 'AntLabel'", Project.MSG_WARN);
            desc = "AntLabel";
        }
        
		if(lock != null && !lock.equalsIgnoreCase("locked")) {
        	log("lock attribute invalid - ignoring",Project.MSG_WARN);
		}
        
        if(name == null || name.length() < 1) {
            SimpleDateFormat formatter = new SimpleDateFormat ("yyyy.MM.dd-hh:mm");
            Date now = new Date();
            name = "AntLabel-"+formatter.format(now);
            log("name not set, assuming '"+name+"'", Project.MSG_WARN);
        }
        
        
        //We have to create a unlocked label first
        String newLabel = 
            "Label: "+name+"\n"+
            "Description: "+desc+"\n"+
            "Options: unlocked\n"+
            "View: "+P4View+"\n";

        P4Handler handler = new P4HandlerAdapter() {
            public void process(String line) {
                log(line, Project.MSG_VERBOSE);
            }
        };

        handler.setOutput(newLabel);

        execP4Command("label -i", handler);
        
        execP4Command("labelsync -l "+name, new P4HandlerAdapter() {
            public void process(String line) {
                log(line, Project.MSG_VERBOSE);
            }
        });
        
        
        log("Created Label "+name+" ("+desc+")", Project.MSG_INFO);

        //Now lock if required
        if (lock != null && lock.equalsIgnoreCase("locked"))  {
        
        	log("Modifying lock status to 'locked'",Project.MSG_INFO);

        	final StringBuffer labelSpec = new StringBuffer();
            
			//Read back the label spec from perforce, 
            //Replace Options
            //Submit back to Perforce
            
        	handler = new P4HandlerAdapter()  {
           		public void process(String line)  {
                	log(line, Project.MSG_VERBOSE);
                    
					if(util.match("/^Options:/",line)) {
   	                	line = "Options: "+lock;
					}
                    
                    labelSpec.append(line+"\n");
           		}
        	};
        
        	
            
			execP4Command("label -o "+name, handler);
            log(labelSpec.toString(),Project.MSG_DEBUG);

            log("Now locking label...",Project.MSG_VERBOSE);
			handler = new P4HandlerAdapter() {
				public void process(String line) {
					log(line, Project.MSG_VERBOSE);
				}
        	};

            handler.setOutput(labelSpec.toString());
			execP4Command("label -i", handler);
        }
        
        
    }

}
