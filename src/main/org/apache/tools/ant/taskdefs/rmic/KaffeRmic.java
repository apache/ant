/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2002 The Apache Software Foundation.  All rights
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
 * 4. The names "Ant" and "Apache Software
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

package org.apache.tools.ant.taskdefs.rmic;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

import org.apache.tools.ant.types.Commandline;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * The implementation of the rmic for Kaffe
 *
 * @author <a href="mailto:tokamoto@rd.nttdata.co.jp">Takashi Okamoto</a>
 * @since Ant 1.4
 */
public class KaffeRmic extends DefaultRmicAdapter {

    public boolean execute() throws BuildException {
        getRmic().log("Using Kaffe rmic", Project.MSG_VERBOSE);
        Commandline cmd = setupRmicCommand();

        try {

            Class c = Class.forName("kaffe.rmi.rmic.RMIC");
            Constructor cons = c.getConstructor(new Class[] { String[].class });
            Object rmic = cons.newInstance(new Object[] { cmd.getArguments() });
            Method doRmic = c.getMethod("run", null);
            Boolean ok = (Boolean) doRmic.invoke(rmic, null);

            return ok.booleanValue();
        } catch (ClassNotFoundException ex) {
            throw new BuildException("Cannot use Kaffe rmic, as it is not "
                                     + "available.  A common solution is to "
                                     + "set the environment variable "
                                     + "JAVA_HOME or CLASSPATH.", 
                                     getRmic().getLocation());
        } catch (Exception ex) {
            if (ex instanceof BuildException) {
                throw (BuildException) ex;
            } else {
                throw new BuildException("Error starting Kaffe rmic: ", 
                                         ex, getRmic().getLocation());
            }
        }
    }
}
