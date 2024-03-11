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

import java.applet.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;

/**
 * Interactive demonstration and testing harness for regular expressions classes.
 * @author <a href="mailto:jonl@muppetlabs.com">Jonathan Locke</a>
 * @version $Id: REDemo.java,v 1.1 2000/04/27 01:22:33 jon Exp $
 */
public class REDemo extends Applet implements TextListener 
{
    /**
     * Matcher and compiler objects
     */
    RE r = new RE();
    REDebugCompiler compiler = new REDebugCompiler();

    /**
     * Components
     */
    TextField fieldRE;          // Field for entering regexps
    TextField fieldMatch;       // Field for entering match strings
    TextArea outRE;             // Output of RE compiler
    TextArea outMatch;          // Results of matching operation

    /**
     * Add controls and init applet
     */
    public void init()
    {
        // Add components using the dreaded GridBagLayout
        GridBagLayout gb = new GridBagLayout();
        setLayout(gb);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);
        c.anchor = c.EAST;
        gb.setConstraints(add(new Label("Regular expression:", Label.RIGHT)), c);
        c.gridy = 0;
        c.anchor = c.WEST;
        gb.setConstraints(add(fieldRE = new TextField("\\[([:javastart:][:javapart:]*)\\]", 40)), c);
        c.gridx = 0;
        c.gridy = c.RELATIVE;
        c.anchor = c.EAST;
        gb.setConstraints(add(new Label("String:", Label.RIGHT)), c);
        c.gridy = 1;
        c.gridx = c.RELATIVE;
        c.anchor = c.WEST;
        gb.setConstraints(add(fieldMatch = new TextField("aaa([foo])aaa", 40)), c);
        c.gridy = 2;
        c.gridx = c.RELATIVE;
        c.fill = c.BOTH;
        c.weighty = 1.0;
        c.weightx = 1.0;
        gb.setConstraints(add(outRE = new TextArea()), c);
        c.gridy = 2;
        c.gridx = c.RELATIVE;
        gb.setConstraints(add(outMatch = new TextArea()), c);

        // Listen to text changes
        fieldRE.addTextListener(this);
        fieldMatch.addTextListener(this);

        // Initial UI update
        textValueChanged(null);
    }

    /**
     * Say something into RE text area
     * @param s What to say
     */
    void sayRE(String s)
    {
        outRE.setText(s);
    }

    /**
     * Say something into match text area
     * @param s What to say
     */
    void sayMatch(String s)
    {
        outMatch.setText(s);
    }

    /**
     * Convert throwable to string
     * @param t Throwable to convert to string
     */
    String throwableToString(Throwable t)
    {
        String s = t.getClass().getName();
        String m;
        if ((m = t.getMessage()) != null)
        {
            s += "\n" + m;
        }
        return s;
    }

    /**
     * Change regular expression
     * @param expr Expression to compile
     */
    void updateRE(String expr)
    {
        try
        {
            // Compile program 
            r.setProgram(compiler.compile(expr));

            // Dump program into RE feedback area
            CharArrayWriter w = new CharArrayWriter();
            compiler.dumpProgram(new PrintWriter(w));
            sayRE(w.toString());
            System.out.println(w);
        }
        catch (Exception e)
        {
            r.setProgram(null);
            sayRE(throwableToString(e));
        }
        catch (Throwable t)
        {
            r.setProgram(null);
            sayRE(throwableToString(t));
        }
    }

    /**
     * Update matching info by matching the string against the current
     * compiled regular expression.
     * @param match String to match against
     */
    void updateMatch(String match)
    {
        try
        {
            // If the string matches the regexp
            if (r.match(match))
            {
                // Say that it matches
                String out = "Matches.\n\n";

                // Show contents of parenthesized subexpressions
                for (int i = 0; i < r.getParenCount(); i++)
                {
                    out += "$" + i + " = " + r.getParen(i) + "\n";
                }
                sayMatch(out);
            }
            else
            {
                // Didn't match!
                sayMatch("Does not match");
            }
        }
        catch (Throwable t)
        {
            sayMatch(throwableToString(t));
        }
    }

    /**
     * Called when text values change
     * @param e TextEvent
     */
    public void textValueChanged(TextEvent e)
    {
        // If it's a generic update or the regexp changed...
        if (e == null || e.getSource() == fieldRE)
        {
            // Update regexp
            updateRE(fieldRE.getText());
        }

        // We always need to update the match results
        updateMatch(fieldMatch.getText());
    }

    /**
     * Main application entrypoint.
     * @param arg Command line arguments
     */
    static public void main(String[] arg)
    {
        JFrame f = new JFrame("RE Demo");
        // f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.addWindowListener(new WindowAdapter()
        {
            public void windowClosing(WindowEvent e)
            {
                System.exit(0);
            }
        });
        Container c = f.getContentPane();
        c.setLayout(new FlowLayout());
        REDemo demo = new REDemo();
        c.add(demo);
        demo.init();
        f.pack();
        f.setVisible(true);
    }
}
