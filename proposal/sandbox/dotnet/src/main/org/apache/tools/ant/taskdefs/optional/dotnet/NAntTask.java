/*
 * Copyright  2003-2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
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
