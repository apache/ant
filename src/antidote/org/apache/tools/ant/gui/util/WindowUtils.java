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

import java.awt.Window;
import java.awt.Rectangle;
import java.awt.Dimension;
import java.awt.event.WindowEvent;

/**
 * Function container for various window operations.
 * 
 * @version $Revision$ 
 * @author Simeon Fitch 
 */
public class WindowUtils {
	/** 
	 * Default ctor.
	 * 
	 */
	private WindowUtils() {}

	/** 
	 * Send a close event to the given window.
	 * 
	 * @param window Window to send close event to.
	 */
	public static void sendCloseEvent(Window window) {
        window.dispatchEvent(
            new WindowEvent(window, WindowEvent.WINDOW_CLOSING));
	}

	/** 
	 * Center the given child window with repsect to the parent window.
	 * 
	 * @param parent Window to base centering on.
	 * @param child Window to center.
	 */
	public static void centerWindow(Window parent, Window child) {
		Rectangle bounds = parent.getBounds();
		Dimension size = child.getSize();
		child.setLocation(bounds.x + (bounds.width - size.width)/2,
						  bounds.y + (bounds.height - size.height)/2);
	}

	/** 
	 * Center the given child window with repsect to the root.
	 * 
	 * @param child Window to center.
	 */
	public static void centerWindow(Window child) {
        Dimension rsize = child.getToolkit().getScreenSize();
        Dimension size = child.getSize();
        child.setLocation((rsize.width - size.width)/2,
                          (rsize.height - size.height)/2);
    }
}
