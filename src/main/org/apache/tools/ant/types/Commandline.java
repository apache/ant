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

package org.apache.tools.ant.types;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.util.StringUtils;
import java.io.File;
import java.util.Vector;
import java.util.StringTokenizer;


/**
 * Commandline objects help handling command lines specifying processes to
 * execute.
 *
 * The class can be used to define a command line as nested elements or as a
 * helper to define a command line by an application.
 * <p>
 * <code>
 * &lt;someelement&gt;<br>
 * &nbsp;&nbsp;&lt;acommandline executable="/executable/to/run"&gt;<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&lt;argument value="argument 1" /&gt;<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&lt;argument line="argument_1 argument_2 argument_3" /&gt;<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&lt;argument value="argument 4" /&gt;<br>
 * &nbsp;&nbsp;&lt;/acommandline&gt;<br>
 * &lt;/someelement&gt;<br>
 * </code>
 * The element <code>someelement</code> must provide a method
 * <code>createAcommandline</code> which returns an instance of this class.
 *
 * @author thomas.haas@softwired-inc.com
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 */
public class Commandline implements Cloneable {

    private Vector arguments = new Vector();
    private String executable = null;

    protected static final String DISCLAIMER =
        StringUtils.LINE_SEP
        + "The \' characters around the executable and arguments are"
        + StringUtils.LINE_SEP
        + "not part of the command."
        + StringUtils.LINE_SEP;

    public Commandline(String to_process) {
        super();
        String[] tmp = translateCommandline(to_process);
        if (tmp != null && tmp.length > 0) {
            setExecutable(tmp[0]);
            for (int i = 1; i < tmp.length; i++) {
                createArgument().setValue(tmp[i]);
            }
        }
    }

    public Commandline() {
        super();
    }

    /**
     * Used for nested xml command line definitions.
     */
    public static class Argument {

        private String[] parts;

        /**
         * Sets a single commandline argument.
         *
         * @param value a single commandline argument.
         */
        public void setValue(String value) {
            parts = new String[] {value};
        }

        /**
         * Line to split into several commandline arguments.
         *
         * @param line line to split into several commandline arguments
         */
        public void setLine(String line) {
            if (line == null) {
                return;
            }
            parts = translateCommandline(line);
        }

        /**
         * Sets a single commandline argument and treats it like a
         * PATH - ensures the right separator for the local platform
         * is used.
         *
         * @param value a single commandline argument.
         */
        public void setPath(Path value) {
            parts = new String[] {value.toString()};
        }

        /**
         * Sets a single commandline argument to the absolute filename
         * of the given file.
         *
         * @param value a single commandline argument.
         */
        public void setFile(File value) {
            parts = new String[] {value.getAbsolutePath()};
        }

        /**
         * Returns the parts this Argument consists of.
         */
        public String[] getParts() {
            return parts;
        }
    }

    /**
     * Class to keep track of the position of an Argument.
     */
    // <p>This class is there to support the srcfile and targetfile
    // elements of &lt;execon&gt; and &lt;transform&gt; - don't know
    // whether there might be additional use cases.</p> --SB
    public class Marker {

        private int position;
        private int realPos = -1;

        Marker(int position) {
            this.position = position;
        }

        /**
         * Return the number of arguments that preceeded this marker.
         *
         * <p>The name of the executable - if set - is counted as the
         * very first argument.</p>
         */
        public int getPosition() {
            if (realPos == -1) {
                realPos = (executable == null ? 0 : 1);
                for (int i = 0; i < position; i++) {
                    Argument arg = (Argument) arguments.elementAt(i);
                    realPos += arg.getParts().length;
                }
            }
            return realPos;
        }
    }

    /**
     * Creates an argument object.
     *
     * <p>Each commandline object has at most one instance of the
     * argument class.  This method calls
     * <code>this.createArgument(false)</code>.</p>
     *
     * @see #createArgument(boolean)
     * @return the argument object.
     */
    public Argument createArgument() {
        return this.createArgument(false);
    }

    /**
     * Creates an argument object and adds it to our list of args.
     *
     * <p>Each commandline object has at most one instance of the
     * argument class.</p>
     *
     * @param insertAtStart if true, the argument is inserted at the
     * beginning of the list of args, otherwise it is appended.
     */
    public Argument createArgument(boolean insertAtStart) {
        Argument argument = new Argument();
        if (insertAtStart) {
            arguments.insertElementAt(argument, 0);
        } else {
            arguments.addElement(argument);
        }
        return argument;
    }

    /**
     * Sets the executable to run.
     */
    public void setExecutable(String executable) {
        if (executable == null || executable.length() == 0) {
            return;
        }
        this.executable = executable.replace('/', File.separatorChar)
            .replace('\\', File.separatorChar);
    }


    public String getExecutable() {
        return executable;
    }


    public void addArguments(String[] line) {
        for (int i = 0; i < line.length; i++) {
            createArgument().setValue(line[i]);
        }
    }

    /**
     * Returns the executable and all defined arguments.
     */
    public String[] getCommandline() {
        final String[] args = getArguments();
        if (executable == null) {
            return args;
        }
        final String[] result = new String[args.length + 1];
        result[0] = executable;
        System.arraycopy(args, 0, result, 1, args.length);
        return result;
    }


    /**
     * Returns all arguments defined by <code>addLine</code>,
     * <code>addValue</code> or the argument object.
     */
    public String[] getArguments() {
        Vector result = new Vector(arguments.size() * 2);
        for (int i = 0; i < arguments.size(); i++) {
            Argument arg = (Argument) arguments.elementAt(i);
            String[] s = arg.getParts();
            if (s != null) {
                for (int j = 0; j < s.length; j++) {
                    result.addElement(s[j]);
                }
            }
        }

        String [] res = new String[result.size()];
        result.copyInto(res);
        return res;
    }


    public String toString() {
        return toString(getCommandline());
    }

    /**
     * Put quotes around the given String if necessary.
     *
     * <p>If the argument doesn't include spaces or quotes, return it
     * as is. If it contains double quotes, use single quotes - else
     * surround the argument by double quotes.</p>
     *
     * @exception BuildException if the argument contains both, single
     *                           and double quotes.
     */
    public static String quoteArgument(String argument) {
        if (argument.indexOf("\"") > -1) {
            if (argument.indexOf("\'") > -1) {
                throw new BuildException("Can\'t handle single and double quotes in same argument");
            } else {
                return '\'' + argument + '\'';
            }
        } else if (argument.indexOf("\'") > -1 || argument.indexOf(" ") > -1) {
            return '\"' + argument + '\"';
        } else {
            return argument;
        }
    }

    /**
     * Quotes the parts of the given array in way that makes them
     * usable as command line arguments.
     */
    public static String toString(String [] line) {
        // empty path return empty string
        if (line == null || line.length == 0) {
            return "";
        }

        // path containing one or more elements
        final StringBuffer result = new StringBuffer();
        for (int i = 0; i < line.length; i++) {
            if (i > 0) {
                result.append(' ');
            }
            result.append(quoteArgument(line[i]));
        }
        return result.toString();
    }

    public static String[] translateCommandline(String to_process) {
        if (to_process == null || to_process.length() == 0) {
            return new String[0];
        }

        // parse with a simple finite state machine

        final int normal = 0;
        final int inQuote = 1;
        final int inDoubleQuote = 2;
        int state = normal;
        StringTokenizer tok = new StringTokenizer(to_process, "\"\' ", true);
        Vector v = new Vector();
        StringBuffer current = new StringBuffer();
        boolean lastTokenHasBeenQuoted = false;

        while (tok.hasMoreTokens()) {
            String nextTok = tok.nextToken();
            switch (state) {
            case inQuote:
                if ("\'".equals(nextTok)) {
                    lastTokenHasBeenQuoted = true;
                    state = normal;
                } else {
                    current.append(nextTok);
                }
                break;
            case inDoubleQuote:
                if ("\"".equals(nextTok)) {
                    lastTokenHasBeenQuoted = true;
                    state = normal;
                } else {
                    current.append(nextTok);
                }
                break;
            default:
                if ("\'".equals(nextTok)) {
                    state = inQuote;
                } else if ("\"".equals(nextTok)) {
                    state = inDoubleQuote;
                } else if (" ".equals(nextTok)) {
                    if (lastTokenHasBeenQuoted || current.length() != 0) {
                        v.addElement(current.toString());
                        current.setLength(0);
                    }
                } else {
                    current.append(nextTok);
                }
                lastTokenHasBeenQuoted = false;
                break;
            }
        }

        if (lastTokenHasBeenQuoted || current.length() != 0) {
            v.addElement(current.toString());
        }

        if (state == inQuote || state == inDoubleQuote) {
            throw new BuildException("unbalanced quotes in " + to_process);
        }

        String[] args = new String[v.size()];
        v.copyInto(args);
        return args;
    }

    public int size() {
        return getCommandline().length;
    }

    public Object clone() {
        Commandline c = new Commandline();
        c.setExecutable(executable);
        c.addArguments(getArguments());
        return c;
    }

    /**
     * Clear out the whole command line.  */
    public void clear() {
        executable = null;
        arguments.removeAllElements();
    }

    /**
     * Clear out the arguments but leave the executable in place for
     * another operation.
     */
    public void clearArgs() {
        arguments.removeAllElements();
    }

    /**
     * Return a marker.
     *
     * <p>This marker can be used to locate a position on the
     * commandline - to insert something for example - when all
     * parameters have been set.</p>
     */
    public Marker createMarker() {
        return new Marker(arguments.size());
    }

    /**
     * Returns a String that describes the command and arguments
     * suitable for verbose output before a call to
     * <code>Runtime.exec(String[])<code>
     *
     * @since Ant 1.5
     */
    public String describeCommand() {
        return describeCommand(this);
    }

    /**
     * Returns a String that describes the arguments suitable for
     * verbose output before a call to
     * <code>Runtime.exec(String[])<code>
     *
     * @since Ant 1.5
     */
    public String describeArguments() {
        return describeArguments(this);
    }

    /**
     * Returns a String that describes the command and arguments
     * suitable for verbose output before a call to
     * <code>Runtime.exec(String[])<code>
     *
     * @since Ant 1.5
     */
    public static String describeCommand(Commandline line) {
        return describeCommand(line.getCommandline());
    }

    /**
     * Returns a String that describes the arguments suitable for
     * verbose output before a call to
     * <code>Runtime.exec(String[])<code>
     *
     * @since Ant 1.5
     */
    public static String describeArguments(Commandline line) {
        return describeArguments(line.getArguments());
    }

    /**
     * Returns a String that describes the command and arguments
     * suitable for verbose output before a call to
     * <code>Runtime.exec(String[])<code>.
     *
     * <p>This method assumes that the first entry in the array is the
     * executable to run.</p>
     * 
     * @since Ant 1.5
     */
    public static String describeCommand(String[] args) {
        if (args == null || args.length == 0) {
            return "";
        }
        
        StringBuffer buf = new StringBuffer("Executing \'");
        buf.append(args[0]);
        buf.append("\'");
        if (args.length > 0) {
            buf.append(" with ");
            buf.append(describeArguments(args, 1));
        } else {
            buf.append(DISCLAIMER);
        }
        return buf.toString();
    }

    /**
     * Returns a String that describes the arguments suitable for
     * verbose output before a call to
     * <code>Runtime.exec(String[])<code>
     *
     * @since Ant 1.5
     */
    public static String describeArguments(String[] args) {
        return describeArguments(args, 0);
    }

    /**
     * Returns a String that describes the arguments suitable for
     * verbose output before a call to
     * <code>Runtime.exec(String[])<code>
     *
     * @param offset ignore entries before this index
     *
     * @since Ant 1.5
     */
    protected static String describeArguments(String[] args, int offset) {
        if (args == null || args.length <= offset) {
            return "";
        }

        StringBuffer buf = new StringBuffer("argument");
        if (args.length > offset) {
            buf.append("s");
        }
        buf.append(":").append(StringUtils.LINE_SEP);
        for (int i = offset; i < args.length; i++) {
            buf.append("\'").append(args[i]).append("\'")
                .append(StringUtils.LINE_SEP);
        }
        buf.append(DISCLAIMER);
        return buf.toString();
    }
}
