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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.Hashtable;
import java.util.Vector;
import java.util.Enumeration;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.util.regexp.RegexpMatcher;
import org.apache.tools.ant.util.regexp.RegexpMatcherFactory;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.taskdefs.Execute;
import org.apache.tools.ant.taskdefs.ExecuteStreamHandler;
import org.apache.tools.ant.taskdefs.PumpStreamHandler;

/**
 * Helper methods related to clearcase commands.
 *
 * @author <a href="mailto:sbailliez@apache.org">Stephane Bailliez</a>
 */
public final class CCUtils {

    public final static String DEFAULT_COMMENT = "\"Automatic operation from Jakarta Ant\"";

    private final static RegexpMatcherFactory __reFactory = new RegexpMatcherFactory();

    /** the matchers cache: pattern/matcher */
    private final static Hashtable matchers = new Hashtable();

    private Task task;

    public CCUtils(Task task){
        this.task = task;
    }

    /**
     * return a group of matches of a given RE in a string.
     * @param pattern the pattern to match in the input data.
     * @param input the data where to look for the pattern.
     * @return the group of matches if any, 0 being the full match
     * and the rest being parenthesized expressions. <tt>null</tt>
     * if there are no matches.
     */
    public Vector matches(String pattern, String input){
        RegexpMatcher matcher = (RegexpMatcher)matchers.get(pattern);
        if (matcher == null){
            matcher = __reFactory.newRegexpMatcher();
            matcher.setPattern(pattern);
            matchers.put(pattern, matcher);
        }
        return matcher.getGroups(input);
    }

    /**
     * Try to resolve a symbolic link if it is one.
     * @param toresolve the symbolic link to resolve.
     * @return the resolved link if it is a symbolic link, otherwise
     * return the original link.
     */
    public File resolveSymbolicLink(File toresolve) throws BuildException {
        String[] args = { "ls", "-l", toresolve.getAbsolutePath() };
        CmdResult res = cleartool(args);
        if (res.getStatus() != 0 ){
            throw new BuildException(res.getStdErr());
        }
        Vector groups = matches("symbolic link(.*)-->(.*)", res.getStdout());
        if (groups == null){
            return toresolve; // or null ?
        }
        String path = (String)groups.elementAt(2);
        path = path.trim();
        File resolved = new File(path);
        if ( !resolved.isAbsolute() ){
            resolved = new File(toresolve.getParent(), path);
        }
        return resolved;
    }

    /**
     * Move a file to another. (ie rename)
     */
    public void move(File from, File to) throws BuildException {
        String[] args = {"move", "-nc", from.getPath(), to.getPath()};
        CmdResult res = cleartool(args);
        if (res.getStatus() != 0) {
            throw new BuildException(res.getStdErr());
        }
    }

    /**
     * return the list of checkedout files in a given viewpath.
     * @param viewpath the path to the view/directory to look for
     * checkedout files.
     * @param recurse <tt>true</tt> to look for files recursively,
     * otherwise <tt>false</tt>
     * @return the list of checkedout files in the view (full pathname).
     */
    public Hashtable lsco(File viewpath, boolean recurse) {
        String recurseParam = recurse ? "-r" : "";
        String fullpath = viewpath.getAbsolutePath();
        //@fixme is -cvi conflicting with -r ?
        String[] args = {"lsco", recurseParam, "-cvi", "-s", "-me", fullpath};
        CmdResult res = cleartool(args);
        if (res.getStatus() != 0) {
            throw new BuildException(res.getStdErr());
        }

        Vector lines = res.getStdoutLines();
        Hashtable map = toFiles(lines);
        return map;
    }

    /**
     * Transform a set of paths into canonical paths.
     * Typically this should be used to transform a set of
     * output lines by cleartool representing file paths.
     */
    public static Hashtable toFiles(Vector paths){
        Hashtable map = new Hashtable();
        for (int i = 0; i < paths.size(); i++) {
            String path = (String) paths.elementAt(i);
            try {
                // the path is normally the full path, we normally
                // not need to do a new File(viewpath, path)
                File f = new File(path);
                path = f.getCanonicalPath();
                map.put(path, path);
            } catch (IOException e) {
                // assume it's not a file...
            }
        }
        return map;
    }

    /**
     * Returns the list of files that are *not* checked out.
     * @see #lsco(File, boolean)
     */
    public Hashtable lsnco(File viewpath){
        String[] args = {"find", viewpath.getAbsolutePath(), "-type", "f", "-cvi", "-nxn", "-print"};
        CmdResult res = cleartool(args);
        Vector lines = res.getStdoutLines();
        Hashtable all = toFiles(lines);
        Hashtable co = lsco(viewpath, true);
        // remove the co files
        Enumeration keys = co.keys();
        while ( keys.hasMoreElements() ){
            Object path = keys.nextElement();
            Object o = all.remove(path);
            if (o == null){
                // oops how come a co file is not found by find ?
            }
        }
        return all;
    }

    /** returns the list of private files in the view */
    public Hashtable lsprivate(File viewpath){
        // for a snapshot view, we must use ls -r -view_only
        return null;
    }

    public void checkin(File file){
        String[] args = {"ci", "-nc", "-identical", file.getAbsolutePath()} ;
        CmdResult res = cleartool(args);
        if (res.getStatus() != 0){
            throw new BuildException(res.getStdErr());
        }
    }

    public void checkout(File file){
        String[] args = {"co", "-nc", "-unreserved", file.getAbsolutePath()} ;
        CmdResult res = cleartool(args);
        if (res.getStatus() != 0){
            throw new BuildException(res.getStdErr());
        }
    }

    public void uncheckout(File file){
        String[] args = {"unco", "-rm", file.getAbsolutePath() };
        CmdResult res = cleartool(args);
        if (res.getStatus() != 0){
            throw new BuildException(res.getStdErr());
        }
    }

    public void mkdir(File file, String comment) {

    }

    public void mkdir(File file){
        String[] args = {"mkdir", "-nc", file.getAbsolutePath() };
        CmdResult res = cleartool(args);
        if (res.getStatus() != 0){
            throw new BuildException(res.getStdErr());
        }
    }

    /**
     * Helper method to execute a given cleartool command.
     * @param args the parameters used to execute cleartool.
     * @return the result of the command.
     */
    public static CmdResult cleartool(String[] args) {
        String[] nargs = new String[args.length + 1];
        nargs[0] = "cleartool";
        System.arraycopy(args, 0, nargs, 1, args.length);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        ExecuteStreamHandler handler = new PumpStreamHandler(out, err);
        Execute exe = new Execute(handler);
        exe.setCommandline(nargs);
        try {
            int retcode = exe.execute();
            return new CmdResult(retcode, out.toString(), err.toString());
        } catch (IOException e){
            throw new BuildException(e);
        }
    }

    /**
     * Create the comment file used by cleartool commands.
     */
    public static File createCommentFile(String comment) {
        FileUtils futils = FileUtils.newFileUtils();
        File f = futils.createTempFile("ant_cc", ".tmp", new File("."));
        Writer writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(f));
            writer.write(comment);
            writer.flush();
        } catch (IOException e){
            throw new BuildException(e);
        } finally {
            if (writer != null){
                try {
                    writer.close();
                } catch (IOException e){
                }
            }
        }
        return f;
    }

}
