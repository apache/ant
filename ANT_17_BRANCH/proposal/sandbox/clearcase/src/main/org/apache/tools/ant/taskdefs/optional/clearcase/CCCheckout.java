/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002 The Apache Software Foundation.  All rights
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
package org.apache.tools.ant.taskdefs.optional.clearcase;

import java.util.Vector;

/**
 * Creates a modifiable copy of a version
 *
 * @see http://clearcase.rational.com/doc/latest/ccase_ux/ccref/checkout.html
 *
 */
public class CCCheckout extends CCMatchingTask {
    private boolean reserved = true;
    private String branch = null;
    private boolean version = false;
    private boolean nwarn = false;
    private String out = null;
    private boolean ndata = false;
    private boolean ptime = false;

    protected Vector getOptions(){
        Vector v = new Vector();
        v.addElement("co");
        v.addElement(reserved ? "-reserved":"-unreserved");
        if (nwarn){
            v.addElement("-nwarn");
        }
        if (branch != null){
            v.addElement("-branch");
            v.addElement(branch);
        } else if (version) {
            v.addElement("-version");
        }
        if (out != null){
            v.addElement("-out");
            v.addElement(out);
        } else if (ndata){
            v.addElement("-ndata");
        }
        if (ptime){
            v.addElement("-ptime");
        }
        v.addElement("<pname>");
        return v;
    }

    protected boolean accept(CCFile file) {
        return file.isCheckedIn();
    }

    // bean setters
    public void setPtime(boolean ptime) {
        this.ptime = ptime;
    }

    public void setReserved(boolean reserved) {
        this.reserved = reserved;
    }

    public void setOut(String out) {
        this.out = out;
    }

    public void setNdata(boolean ndata) {
        this.ndata = ndata;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public void setVersion(boolean version) {
        this.version = version;
    }

    public void setNwarn(boolean nwarn) {
        this.nwarn = nwarn;
    }
}
