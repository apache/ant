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
package org.apache.tools.ant.gui.core;

import javax.swing.JComponent;
import javax.swing.BorderFactory;

/**
 * Abstract base class for a "module", which is really anything that
 * can send or receive events, or edit or view the model.
 * 
 * @version $Revision$ 
 * @author Simeon Fitch 
 */
public abstract class AntModule extends JComponent {

    /** The application context. */
    private AppContext _context = null;

    /** 
     * Default constructor.
     */
    protected AntModule() {
        // Create a dummy border so that the widget will at least have a
        // minimal display in a bean environment.
        setBorder(BorderFactory.createTitledBorder(getClass().getName()));
    }

    /** 
     * This method is called after instantiation when the application context
     * is available for constructing the class' display. Think of this in
     * a similar manner to Applet.init() or Servlet.init(). It should 
     * immediately call #setContext() with the given parameter.
     * 
     * @param context Valid application context providing
     *                all required resources.  
     */
    public abstract void contextualize(AppContext context);

    /** 
     * Set the application context.
     * 
     * @param context Application context.
     */
    protected void setContext(AppContext context) {
        _context = context;
        setBorder(_context == null ? null : 
                  BorderFactory.createTitledBorder(getName()));
    }

    /** 
     * Get the application context.
     * 
     * @return Application context.
     */
    public AppContext getContext() {
        if(_context == null) {
            throw new IllegalStateException(
                "The AppContext has not been set.");
        }
        return _context;
    }
    /** 
     * Get the name of the editor.
     * 
     * @return Editor's name.
     */
    public String getName() {
        return getContext().getResources().getString(getClass(), "name");
    }
}
