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

package org.apache.ant.xdoclet;

import org.apache.tools.ant.Task;
import org.apache.tools.ant.BuildException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.TreeMap;
import java.util.Arrays;
import java.util.Iterator;

/**
 * Quick and dirty index.html generator for proposal/xdocs
 *
 * @author Erik Hatcher
 */
public class IndexGen extends Task {
    private File rootDir;

    public void setRootDir(File rootDir) {
        this.rootDir = rootDir;
    }

    public void execute() throws BuildException {
        TreeMap data = new TreeMap();

        String[] categories = rootDir.list();

        if (categories == null) {
            throw new BuildException("Root directory \"" + rootDir.getPath() + "\" does not exist!", getLocation());
        }

        StringBuffer sb = new StringBuffer();
        sb.append("<html><head><title>xdocs index</title></head>");
        sb.append("<body>");

        int catCount = 0;
        int taskCount = 0;

        // grab all categories and tasks
        for (int i=0; i < categories.length; i++) {
            String category = categories[i];
            File catDir = new File(rootDir, category);

            if (!catDir.isDirectory()) {
                continue;
            }

            String[] tasks = catDir.list();
            Arrays.sort(tasks);

            data.put(category, tasks);
        }

        Iterator iter = data.keySet().iterator();
        while (iter.hasNext()) {
            catCount++;
            String category = (String) iter.next();

            sb.append("<h2>" + category + "</h2>");

            sb.append("<ul>");

            String[] tasks = (String[]) data.get(category);

            for (int j=0; j < tasks.length; j++) {
                taskCount++;
                String task = tasks[j];
                sb.append("<li>");
                sb.append("<a href=\"" + category + "/" + task + "\">" + task + "</a>");
                sb.append("</li>");
            }

            sb.append("</ul>");
        }

        sb.append("</body></html>");

        FileWriter fw = null;
        try {
            fw = new FileWriter(new File(rootDir,"index.html"));
            fw.write(sb.toString());
            fw.close();
        } catch (IOException e) {
            throw new BuildException(e);
        }

        log("Index generated: " + catCount + " categories and " + taskCount + " tasks");
    }
}
