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

package org.apache.tools.ant.gui.ide;

import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.Project;
import java.util.StringTokenizer;
import java.io.File;

/**
 * Abstract base class for BuildListener specializations interested
 * in notification of build errors in source files.
 *
 * @version $Revision$ 
 * @author <a href="mailto:simeon@fitch.net">Simeon H.K. Fitch</a>
 */
abstract class FileErrorNotifier implements BuildListener {
    /** Command to run to communicate with emacs. */
    private static final String EMACS = "emacsclient";

    /**
     * Fired whenever a message is logged. Parses error messages looking
     * for a filename and line number specification, which when found
	 * is then sent to the method <code>fireFileErrorNotification()</code>
     * source of the error. Only the first error is processed.
     *
	 * @param event Incoming event that is filtered for error messages.
     */
    public void messageLogged(BuildEvent event) {
        if(event.getPriority() == Project.MSG_ERR ||
           event.getPriority() == Project.MSG_WARN) {
            StringTokenizer tok = new StringTokenizer(event.getMessage(), ":");
            File file = null;
            int line = -1;
            
            if(tok.hasMoreTokens()) {
                file = new File(tok.nextToken());
                if(tok.hasMoreTokens()) {
                    try {
                        line = Integer.parseInt(tok.nextToken());
                    }
                    catch(NumberFormatException ex) {
                        // Allow execption to fall through as we test
                        // success by the value of 'line' below.
                    }
                }
            }

            // Test for successful filename and line number detection.
            if(file != null && line > 0 && file.exists()) {
                // Since we only want to trigger on the first error, 
                // remove ourself from being notified of others.
				// XXX are there any reasons this should occur after
				// notification?
                event.getProject().removeBuildListener(this);

				// Send notification.
				fireFileErrorNotification(event.getProject(), file, line);
            }
        }
    }

	/** 
	 * Called when a message has been logged indicating that there
	 * is an error of some sort at the given file and line number.
	 * 
	 * @param file File containing the error.
	 * @param lineNum Line number of error.
	 */
	protected abstract void fireFileErrorNotification(
		Project project, File file, int lineNum);

    // Event types that are ignored. Looks like we really need a
    //  BuildAdapter class...
    /** Ignored */
    public void buildStarted(BuildEvent event) {
    }
    /** Ignored */
    public void buildFinished(BuildEvent event) {
    }
    /** Ignored */
    public void targetStarted(BuildEvent event) {
    }
    /** Ignored */
    public void targetFinished(BuildEvent event) {
    }
    /** Ignored */
    public void taskStarted(BuildEvent event) {
    }
    /** Ignored */
    public void taskFinished(BuildEvent event) {
    }
}
