/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2003 The Apache Software Foundation.  All rights
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

package org.apache.tools.ant.taskdefs.optional.dotnet;

import java.io.File;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Runs a NAnt build process.
 */
public class NAntTask extends AbstractBuildTask {

    public NAntTask() {
        super();
    }

    protected String getExecutable() {
        return "NAnt.exe";
    }

    protected String[] getBuildfileArguments(File buildFile) {
        if (buildFile != null) {
            return new String[] {
                "-buildfile:" + buildFile.getAbsolutePath()
            };
        } else {
            return new String[0];
        }
    }

    protected String[] getTargetArguments(List targets) {
        ArrayList al = new ArrayList(targets.size());
        Iterator iter = targets.iterator();
        while (iter.hasNext()) {
            AbstractBuildTask.Target t = (AbstractBuildTask.Target) iter.next();
            al.add(t.getName());
        }
        return (String[]) al.toArray(new String[al.size()]);
    }

    protected String[] getPropertyArguments(List properties) {
        ArrayList al = new ArrayList(properties.size());
        Iterator iter = properties.iterator();
        while (iter.hasNext()) {
            AbstractBuildTask.Property p = 
                (AbstractBuildTask.Property) iter.next();
            al.add("-D:" + p.getName() + "=" + p.getValue());
        }
        return (String[]) al.toArray(new String[al.size()]);
    }

    /**
     * Turn the DocumentFragment into a DOM tree suitable as a build
     * file when serialized.
     *
     * <p>If we have exactly one <project> child, return that.
     * Otherwise assume that this is a valid build file snippet that
     * just needs an empty project wrapped around it.</p>
     */
    protected Element makeTree(DocumentFragment f) {
        NodeList nl = f.getChildNodes();
        if (nl.getLength() == 1 
            && nl.item(0).getNodeType() == Node.ELEMENT_NODE
            && nl.item(0).getNodeName().equals("project")) {
            return (Element) nl.item(0);
        } else {
            Element e = f.getOwnerDocument().createElement("project");
            e.appendChild(f);
            return e;
        }
    }
}
