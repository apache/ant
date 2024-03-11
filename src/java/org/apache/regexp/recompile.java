package org.apache.regexp;

/*
 * ====================================================================
 * 
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999 The Apache Software Foundation.  All rights 
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
 * 4. The names "The Jakarta Project", "Jakarta-Regexp", and "Apache Software
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
 *
 */ 

import org.apache.regexp.RECompiler;
import org.apache.regexp.RESyntaxException;

/**
 * 'recompile' is a command line tool that pre-compiles one or more regular expressions
 * for use with the regular expression matcher class 'RE'.  For example, the command
 * "java recompile a*b" produces output like this:
 *
 * <pre>
 *
 *    // Pre-compiled regular expression "a*b"
 *    char[] re1Instructions =
 *    {
 *        0x007c, 0x0000, 0x001a, 0x007c, 0x0000, 0x000d, 0x0041,
 *        0x0001, 0x0004, 0x0061, 0x007c, 0x0000, 0x0003, 0x0047,
 *        0x0000, 0xfff6, 0x007c, 0x0000, 0x0003, 0x004e, 0x0000,
 *        0x0003, 0x0041, 0x0001, 0x0004, 0x0062, 0x0045, 0x0000,
 *        0x0000,
 *    };
 *
 *    REProgram re1 = new REProgram(re1Instructions);
 *
 * </pre>
 *
 * By pasting this output into your code, you can construct a regular expression matcher
 * (RE) object directly from the pre-compiled data (the character array re1), thus avoiding
 * the overhead of compiling the expression at runtime.  For example:
 *
 * <pre>
 *
 *    RE r = new RE(re1);
 *
 * </pre>
 *
 * @see RE
 * @see RECompiler
 *
 * @author <a href="mailto:jonl@muppetlabs.com">Jonathan Locke</a>
 * @version $Id: recompile.java,v 1.1 2000/04/27 01:22:33 jon Exp $
 */
public class recompile
{
    /**
     * Main application entrypoint.
     * @param arg Command line arguments
     */
    static public void main(String[] arg)
    {
        // Create a compiler object
        RECompiler r = new RECompiler();

        // Print usage if arguments are incorrect
        if (arg.length <= 0 || arg.length % 2 != 0)
        {
            System.out.println("Usage: recompile <patternname> <pattern>");
            System.exit(0);
        }

        // Loop through arguments, compiling each
        for (int i = 0; i < arg.length; i += 2)
        {
            try
            {
                // Compile regular expression
                String name         = arg[i];
                String pattern      = arg[i+1];
                String instructions = name + "PatternInstructions";

                // Output program as a nice, formatted character array
                System.out.print("\n    // Pre-compiled regular expression '" + pattern + "'\n" 
                                 + "    private static char[] " + instructions + " = \n    {");

                // Compile program for pattern
                REProgram program = r.compile(pattern);

                // Number of columns in output
                int numColumns = 7;

                // Loop through program
                char[] p = program.getInstructions();
                for (int j = 0; j < p.length; j++)
                {
                    // End of column?
                    if ((j % numColumns) == 0)
                    {
                        System.out.print("\n        ");
                    }

                    // Print character as padded hex number
                    String hex = Integer.toHexString(p[j]);
                    while (hex.length() < 4)
                    {
                        hex = "0" + hex;
                    }
                    System.out.print("0x" + hex + ", ");
                }

                // End of program block
                System.out.println("\n    };");
                System.out.println("\n    private static RE " + name + "Pattern = new RE(new REProgram(" + instructions + "));");
            }
            catch (RESyntaxException e)
            {
                System.out.println("Syntax error in expression \"" + arg[i] + "\": " + e.toString());
            }
            catch (Exception e)
            {
                System.out.println("Unexpected exception: " + e.toString());
            }
            catch (Error e)
            {
                System.out.println("Internal error: " + e.toString());
            }
        }
    }
}
