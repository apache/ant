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
 * 4. The names "The Jakarta Project", "Tomcat", and "Apache Software
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

package org.apache.tools.ant.gui.ide;

import org.apache.tools.ant.taskdefs.ExecTask;
import org.apache.tools.ant.Project;
import java.util.StringTokenizer;
import java.io.File;

/**
 * FileErrorNotifier specialization for sending file error
 * messages to emacs via the emacsclient command. This
 * command <i>must</i> be in the runtime path of the JVM for
 * it to be found.  
 * <p> Install the notifier by running Ant as follows:<br> 
 * <code> 
 * &nbsp;&nbsp;ant -listener net.noemis.ant.EmacsNotifier 
 * </code>
 * 
 * @version $Revision$ 
 * @author <a href="mailto:simeon@fitch.net">Simeon H.K. Fitch</a> */
public class EmacsNotifier extends FileErrorNotifier {
    /** Command to run to communicate with emacs. */
	// XXX This should only be a default. A property should be checked
	// for the actual version. Should Project.getProperty() or 
	// Project.getUserProperty() be used???
    private static final String EMACS = "emacsclient";

	/** 
	 * Called when a message has been logged indicating that
	 * there is an error of some sort at the given file and
	 * line number.  Sends a message bto emacs to make emacs
	 * visit the file and place the mark at the source of
	 * the error.
	 * 
	 * @param file File containing the error.
	 * @param lineNum Line number of error.  
	 */
	protected void fireFileErrorNotification(
		Project project, File file, int lineNum) {
		// Launch our command using the built in exec support.
		ExecTask exec = (ExecTask) project.createTask("exec");
		// Construct the command to communicate with emacs.
		// Command has the form:
		//     emacsclient [-n] [--no-wait] [+LINENUMBER] FILENAME
		exec.setExecutable(EMACS);
		exec.createArg().setValue("-n");
		exec.createArg().setValue("--no-wait");
		exec.createArg().setValue("+" + lineNum);
		exec.createArg().setValue(file.toString());
		exec.setFailonerror(false);
		exec.execute();
	}
}
