/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999, 2000 The Apache Software Foundation.  All rights
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
package org.apache.tools.ant.gui.util;

import java.io.StringWriter;
import java.io.PrintWriter;
import java.util.StringTokenizer;

/*
 * Class for parsing the stack frame information from a Throwable's stack frame
 * dump. Probably only suitable for debugging, as it depends on the specific 
 * output format of the stack dump, which could change with JVM releases.
 *
 * @version $Revision$ 
 * @author Simeon Fitch
 */
public class StackFrame {
	private String _method = null;
	private int _line = -1;
	private String _clazz = null;

	/** 
	 * Default ctor. Gets the stack frame info for the calling method.
	 * 
	 */
	public StackFrame() {
		this(1);
	}

    public StackFrame(int frame) {
		if(frame < 0) {
			throw new IllegalArgumentException("Frame number must be <= 0");
		}
		// Add a stack frame for this method and for 
		// the fillInStackTrace method.
		frame += 2;

		Throwable t = new Throwable();
		t.fillInStackTrace();
		String text = toString(t);

		// Extract the line that has the stack frame info we want on it.
		StringTokenizer tok = new StringTokenizer(text, "\n");
		// Ignore the first line as it just has the exception type on it.
		tok.nextToken();
		String hit = null;
		while(tok.hasMoreTokens() && frame-- >= 0) {
			hit = tok.nextToken();
		}

		// This should always pass. '4' is the number of characters to get
		// to the start of the class name ("\tat ").
		if(hit != null && hit.length() > 4) {
			int idx = hit.indexOf('(');
			if(idx > 4) {
				String before = hit.substring(4, idx);
				String after = hit.substring(idx + 1, hit.length() - 1);

				// Extract the method name and class name.
				idx = before.lastIndexOf('.');
				if(idx >= 0) {
					_clazz = before.substring(0, idx);
					_method = before.substring(
						idx + 1, before.length());
				}
				idx = after.lastIndexOf(':');
				// Extract the line number. If it fails in any way
				// then just leave the value at -1 which is a valid value.
				try {
					_line = Integer.parseInt(
						after.substring(idx + 1, after.length()));
				}
				catch(Exception ex) {
                    // Ignore.
                }
			}
		}
    }

	/** 
	 * Utility method for converting a throwable object to a string.
	 * 
	 * @param t Throwable to convert.
	 * @return String representation.
	 */
	public static String toString(Throwable t) {
		StringWriter writer = new StringWriter();
		t.printStackTrace(new PrintWriter(writer));
		return writer.toString();
	}

	/** 
	 * Get the stack frame class.
	 * 
	 * @return 
	 */
	public String getClassName() {
		return _clazz;
	}

	/** 
	 * Get the name of the stack frame method 
	 * 
	 * @return 
	 */
	public String getMethodName() {
		return _method;
	}

	/** 
	 * Get the line number for the frame call.
	 * 
	 * @return Line number, or -1 if unknown.
	 */
	public int getLineNumber() {
		return _line;
	}

	public String toString() {
		return getClassName() + "." + getMethodName() + "(line " + 
			(getLineNumber() >= 0 ? 
			 String.valueOf(getLineNumber()) : "unknown") + ")";
	}

	/** 
	 * Test code.
	 * 
	 * @param args Ignored.
	 */
/*
	public static void main(String[] args) {
		//Test class for generating a bunch of stack frames.
		class Test {
			public Test() {
				System.out.println("Main method: " + new StackFrame(2));
				recurse(20);
			}

			private void recurse(int val) {
				if(val == 0) {
					System.out.println("Recurse method: " + new StackFrame());
				}
				else if(val % 2 == 0) {
					recurse(val - 1);
				}
				else {
					recurse(val - 1);
				}
			}
		}

		new Test();
	}
*/
}


