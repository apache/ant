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

package org.apache.tools.ant.taskdefs.optional.sitraka;

import java.util.Vector;
import java.util.Hashtable;
import org.apache.tools.ant.BuildException;

/**
 * Trigger information. It will return as a command line argument by calling
 * the <tt>toString()</tt> method.
 *
 * @author <a href="mailto:sbailliez@imediation.com">Stephane Bailliez</a>
 */
public class Triggers {

	protected Vector triggers = new Vector();
	
	public Triggers(){
	}

	public void addMethod(Method method){
		triggers.addElement(method);
	}

	// -jp_trigger=ClassName.*():E:S,ClassName.MethodName():X:X 
	public String toString(){
		StringBuffer buf = new StringBuffer();
		final int size = triggers.size();
		for(int i = 0; i < size;  i++) {
			buf.append( triggers.elementAt(i).toString() );
			if (i < size - 1) {
				buf.append(',');
			}
		}
		return buf.toString();
	}

    
	public static class Method {
		protected String name;
		protected String event;
		protected String action;
		protected String param;
		public void setName(String value){
			name = value;
		}
		public void setEvent(String value){
			if (eventMap.get(value) == null) {
				throw new BuildException("Invalid event, must be one of " + eventMap);
			}
			event = value;
		}
		public void setAction(String value) throws BuildException {
			if (actionMap.get(value) == null) {
				throw new BuildException("Invalid action, must be one of " + actionMap);
			}
			action = value;
		}
		public void setParam(String value){
			param = value;
		}

		// return <name>:<event>:<action>[:param]
		public String toString(){
			StringBuffer buf = new StringBuffer();
			buf.append(name).append(":"); //@todo name must not be null, check for it
			buf.append(eventMap.get(event)).append(":");
			buf.append(actionMap.get(action));
			if (param != null) {
				buf.append(":").append(param);
			}
			return buf.toString();
		}
	}

	/** mapping of actions to cryptic command line mnemonics */
	private final static Hashtable actionMap = new Hashtable(3);
	
	/** mapping of events to cryptic command line mnemonics */
	private final static Hashtable eventMap = new Hashtable(3);

	static {
		actionMap.put("enter", "E");
		actionMap.put("exit", "X");
		// clear|pause|resume|snapshot|suspend|exit
		eventMap.put("clear", "C");
		eventMap.put("pause", "P");
		eventMap.put("resume", "R");
		eventMap.put("snapshot", "S");
		eventMap.put("suspend", "A");
		eventMap.put("exit", "X");
	}

}
