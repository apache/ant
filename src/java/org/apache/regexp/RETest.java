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

import java.io.*;

/**
 * Data driven (and optionally interactive) testing harness to exercise regular
 * expression compiler and matching engine.
 *
 * @author <a href="mailto:jonl@muppetlabs.com">Jonathan Locke</a>
 * @author <a href="mailto:jon@latchkey.com">Jon S. Stevens</a>
 * @version $Id: RETest.java,v 1.2 2000/04/30 20:42:35 jon Exp $
 */
public class RETest
{
    // Construct a matcher and a debug compiler
    RE r = new RE();
    REDebugCompiler compiler = new REDebugCompiler();

    // True if we want to see output from success cases
    static final boolean showSuccesses = false;

    /**
     * Main program entrypoint.  If an argument is given, it will be compiled
     * and interactive matching will ensue.  If no argument is given, the
     * file RETest.txt will be used as automated testing input.
     * @param arg Command line arguments (optional regular expression)
    */
    public static void main(String[] arg)
    {
        try
        {
            //new RETest(arg);
            test();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Testing entrypoint.
     * @param arg Command line arguments
     * @exception Exception thrown in case of error
    */
    public static boolean test() throws Exception
    {
        RETest test = new RETest();
        test.runAutomatedTests("docs/RETest.txt");
        return test.failures == 0;
    }

    /**
     * Constructor
    */
    public RETest()
    {
    }

    /**
     * Constructor for test
     * @param arg Command line arguments
    */
    public RETest(String[] arg)
    {
        try
        {
            // Run interactive tests against a single regexp
            if (arg.length == 2)
            {
                runInteractiveTests(arg[1]);
            }
            else if (arg.length == 1)
            {
                // Run automated tests 
                runAutomatedTests(arg[0]);
            }
            else
            {
                System.out.println ( "Usage: RETest ([-i] [regex]) ([/path/to/testfile.txt])" );
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }       
    }

    /**
     * Compile and test matching against a single expression
     * @param expr Expression to compile and test
    */
    void runInteractiveTests(String expr)
    {
        try
        {
            // Compile expression
            r.setProgram(compiler.compile(expr));

            // Show expression
            say("\n" + expr + "\n");

            // Show program for compiled expression
            compiler.dumpProgram(new PrintWriter(System.out));
            
            // Test matching against compiled expression
            while (true)
            {
                // Read from keyboard
                BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
                System.out.print("> ");
                System.out.flush();
                String match = br.readLine();

                // Try a match against the keyboard input
                if (r.match(match))
                {
                    say("Match successful.");
                }
                else
                {
                    say("Match failed.");
                }

                // Show subparen registers
                showParens(r);
            }
        }
        catch (Exception e)
        {
            say("Error: " + e.toString());
            e.printStackTrace();
        }
    }

    /**
     * Exit with a fatal error.
     * @param s Last famous words before exiting
    */
    void die(String s)
    {
        say("FATAL ERROR: " + s);
        System.exit(0);
    }

    /**
     * Fail with an error
     * @param s Failure description
    */
    void fail(String s)
    {
        failures++;
        say("\n");
        say("*******************************************************");
        say("*********************  FAILURE!  **********************");
        say("*******************************************************");
        say("\n");
        say(s);
        say("");        
        compiler.dumpProgram(new PrintWriter(System.out));
        say("\n");
    }

    /**
     * Show a success
     * @param s Success story
    */
    void success(String s)
    {
        if (showSuccesses)
        {
            show();
            say("Success: " + s);
        }
    }

    /**
     * Say something to standard out
     * @param s What to say
    */
    void say(String s)
    {
        System.out.println (s);
    }

    /**
     * Show an expression
    */
    void show()
    {
        say("\n-----------------------\n");
        say("Expression #" + (n) + " \"" + expr + "\" ");
    }

    /**
     * Dump parenthesized subexpressions found by a regular expression matcher object
     * @param r Matcher object with results to show
    */
    void showParens(RE r)
    {
        // Loop through each paren
        for (int i = 0; i < r.getParenCount(); i++)
        {
            // Show paren register
            say("$" + i + " = " + r.getParen(i));
        }
    }

    // Pre-compiled regular expression "a*b"
    char[] re1Instructions =
    {
        0x007c, 0x0000, 0x001a, 0x007c, 0x0000, 0x000d, 0x0041,
        0x0001, 0x0004, 0x0061, 0x007c, 0x0000, 0x0003, 0x0047,
        0x0000, 0xfff6, 0x007c, 0x0000, 0x0003, 0x004e, 0x0000,
        0x0003, 0x0041, 0x0001, 0x0004, 0x0062, 0x0045, 0x0000,
        0x0000,
    };

    REProgram re1 = new REProgram(re1Instructions);

    /*
     * Current expression and number in automated test
    */
    String expr;
    int n = 0;

    /*
     * Count of failures in automated test
     */
    int failures = 0;

    /**
     * Run automated tests in RETest.txt file (from Perl 4.0 test battery)
    * @exception Exception thrown in case of error
    */
    void runAutomatedTests(String testDocument) throws Exception
    {
        long ms = System.currentTimeMillis();

        // Simple test of pre-compiled regular expressions
        RE r = new RE(re1);
        say("a*b");
        say("aaaab = " + r.match("aaab"));
        showParens(r);
        say("b = " + r.match("b"));
        showParens(r);
        say("c = " + r.match("c"));
        showParens(r);
        say("ccccaaaaab = " + r.match("ccccaaaaab"));
        showParens(r);

        r = new RE("a*b");
        String[] s = r.split("xxxxaabxxxxbyyyyaaabzzz");
        r = new RE("x+");
        s = r.grep(s);
        for (int i = 0; i < s.length; i++)
        {
            System.out.println ("s[" + i + "] = " + s[i]);
        }

        r = new RE("a*b");
        String s1 = r.subst("aaaabfooaaabgarplyaaabwackyb", "-");
        System.out.println ("s = " + s1);

        // Test from script file
        File testInput = new File(testDocument);
        if (! testInput.exists())
            throw new Exception ("Could not find: " + testDocument);
        BufferedReader br = new BufferedReader(new FileReader(testInput));
        try
        {
            // While input is available, parse lines
            while (br.ready())
            {
                // Find next re test case
                String number = "";
                String yesno;
                while (br.ready())
                {
                    number = br.readLine();
                    if (number == null)
                    {
                        break;
                    }
                    number = number.trim();
                    if (number.startsWith("#"))
                    {
                        break;
                    }
                    if (!number.equals(""))
                    {
                        System.out.println ("Script error.  Line = " + number);
                        System.exit(0);
                    }
                }

                // Are we done?
                if (!br.ready())
                {
                    break;
                }

                // Get expression
                expr = br.readLine();
                n++;
                say("");
                say(n + ". " + expr);
                say("");

                // Compile it
                try
                {
                    r.setProgram(compiler.compile(expr));
                }

                // Some expressions *should* cause exceptions to be thrown
                catch (Exception e)
                {
                    // Get expected result
                    yesno = br.readLine().trim();

                    // If it was supposed to be an error, report success and continue
                    if (yesno.equals("ERR"))
                    {
                        say("   Match: ERR");
                        success("Produces an error (" + e.toString() + "), as expected.");
                        continue;
                    }

                    // Wasn't supposed to be an error
                    fail("Produces the unexpected error \"" + e.getMessage() + "\"");
                }
                catch (Error e)
                {
                    // Internal error happened
                    fail("Compiler threw fatal error \"" + e.getMessage() + "\"");
                    e.printStackTrace();
                }

                // Get string to match against
                String matchAgainst = br.readLine().trim();
                say("   Match against: '" + matchAgainst + "'");

                // Expression didn't cause an expected error
                if (matchAgainst.equals("ERR"))
                {
                    fail("Was expected to be an error, but wasn't.");
                    continue;
                }

                // Try matching
                try
                {
                    // Match against the string
                    boolean b = r.match(matchAgainst);

                    // Get expected result
                    yesno = br.readLine().trim();

                    // If match succeeded
                    if (b)
                    {
                        // Status
                        say("   Match: YES");

                        // Match wasn't supposed to succeed
                        if (yesno.equals("NO"))
                        {
                            fail("Matched \"" + matchAgainst + "\", when not expected to.");
                        }
                        else
                        if (yesno.equals("YES"))
                        {
                            // Match succeeded as expected
                            success("Matched \"" + matchAgainst + "\", as expected:");

                            // Show subexpression registers
                            if (showSuccesses)
                            {
                                showParens(r);
                            }

                            say("   Paren count: " + r.getParenCount());

                            // Check registers against expected contents
                            for (int p = 0; p < r.getParenCount(); p++)
                            {
                                // Get next register
                                String register = br.readLine().trim();
                                say("   Paren " + p + " : " + r.getParen(p));

                                // Compare expected result with actual
                                if (!register.equals(r.getParen(p)))
                                {
                                    // Register isn't what it was supposed to be
                                    fail("Register " + p + " should be = \"" + register + "\", but is \"" + r.getParen(p) + "\" instead.");
                                }
                            }
                        }
                        else
                        {
                            // Bad test script
                            die("Test script error!");
                        }
                    }
                    else
                    {
                        // Status
                        say("   Match: NO");

                        // Match failed
                        if (yesno.equals("YES"))
                        {
                            // Should have failed
                            fail("Did not match \"" + matchAgainst + "\", when expected to.");
                        }
                        else
                        if (yesno.equals("NO"))
                        {
                            // Should have failed
                            success("Did not match \"" + matchAgainst + "\", as expected.");
                        }
                        else
                        {
                            // Bad test script
                            die("Test script error!");
                        }
                    }
                }

                // Matcher blew it
                catch (Exception e)
                {
                    fail("Matcher threw exception: " + e.toString());
                    e.printStackTrace();
                }

                // Internal error
                catch (Error e)
                {
                    fail("Matcher threw fatal error \"" + e.getMessage() + "\"");
                    e.printStackTrace();
                }
            }
        }
        finally
        {
            br.close();
        }

        // Show match time
        System.out.println ("\n\nMatch time = " + (System.currentTimeMillis() - ms) + " ms.");

        // Print final results
        System.out.println ("\nTests complete.  " + n + " tests, " + failures + " failure(s).");
    }
}
