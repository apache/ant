/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2002 The Apache Software Foundation.  All rights
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

package org.apache.tools.ant;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Vector;
import java.util.Properties;
import java.util.Enumeration;
import java.util.Stack;
import java.lang.reflect.Modifier;


import org.apache.tools.ant.types.FilterSet; 
import org.apache.tools.ant.types.FilterSetCollection; 
import org.apache.tools.ant.util.FileUtils; 

// XXX It would be a good idea to merge all 'hooks' into one AntInterceptor
// mechanism. That would make it much easier to read and extend.

/** 
 * Abstract component creation and configuration.
 *
 * TODO: Multiple ComponentHelpers can be registered in a project, either by an
 * embedding application or by taskdef-like tasks.
 *
 * After a ProjectComponentHelper is registered, it'll manage the construction and 
 * configuration of tasks/types/etc. It has full control over how the
 * component is created - and may provide runtime wrapping for components
 * not implementing the Task/DataType interfaces.
 * It works in close relation with TaskAdapter and RuntimeConfigurable
 * to handle delayed evaluation of tasks or custom attribute->task mapping.
 * If it returns a wrapper for Task, the wrapper is required to extend
 * TaskAdapter.
 *
 * The common 'Chain' pattern is used to construct
 * tasks, with the original behavior ( Class registry ) tried last, by the
 * default helper implementation.
 *
 * Note that 'delayed' construction of tasks is used.
 *
 * @author Costin Manolache
 */
public interface ProjectComponentFactory {

    /** Creates an ant component..
     *
     * A factory may have knowledge about the tasks it creates. It can return
     * an object extending TaskAdapter that emulates Task/DataType. If null is returned,
     * the next helper is tried.
     *
     * @param project the context for task creation.
     * @param role Class name for the expected role ( Task, Type, Filter, etc ). Null for 'any'
     * @param ns namespace if a SAX2 parser is used, null for 'classical' ant
     * @param taskName the (local) name of the task.
     */
    public Object createProjectComponent( Project project,
                                          String ns,
                                          String taskName )
        throws BuildException;
    // XXX class loader ? Can use the ns, but additional hints may be available in taskdef
    // 
}
