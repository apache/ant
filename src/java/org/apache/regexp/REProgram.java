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

import org.apache.regexp.RE;
import java.util.Hashtable;

/**
 * A class that holds compiled regular expressions.  This is exposed mainly
 * for use by the recompile utility (which helps you produce precompiled
 * REProgram objects). You should not otherwise need to work directly with
 * this class.
*
 * @see RE
 * @see RECompiler
 *
 * @author <a href="mailto:jonl@muppetlabs.com">Jonathan Locke</a>
 * @version $Id: REProgram.java,v 1.1 2000/04/27 01:22:33 jon Exp $
 */
public class REProgram
{
    static final int OPT_HASBACKREFS = 1;

    char[] instruction;         // The compiled regular expression 'program'
    int lenInstruction;         // The amount of the instruction buffer in use
    char[] prefix;              // Prefix string optimization
    int flags;                  // Optimization flags (REProgram.OPT_*)

    /**
     * Constructs a program object from a character array
     * @param instruction Character array with RE opcode instructions in it
     */
    public REProgram(char[] instruction)
    {
        this(instruction, instruction.length);
    }

    /**
     * Constructs a program object from a character array
     * @param instruction Character array with RE opcode instructions in it
     * @param lenInstruction Amount of instruction array in use
     */
    public REProgram(char[] instruction, int lenInstruction)
    {
        setInstructions(instruction, lenInstruction);
    }

    /**
     * Returns a copy of the current regular expression program in a character
     * array that is exactly the right length to hold the program.  If there is
     * no program compiled yet, getInstructions() will return null.
     * @return A copy of the current compiled RE program
     */
    public char[] getInstructions()
    {
        // Ensure program has been compiled!
        if (lenInstruction != 0)
        {
            // Return copy of program 
            char[] ret = new char[lenInstruction];
            System.arraycopy(instruction, 0, ret, 0, lenInstruction);
            return ret;
        }
        return null;
    }

    /**
     * Sets a new regular expression program to run.  It is this method which
     * performs any special compile-time search optimizations.  Currently only
     * two optimizations are in place - one which checks for backreferences
     * (so that they can be lazily allocated) and another which attempts to
     * find an prefix anchor string so that substantial amounts of input can
     * potentially be skipped without running the actual program.
     * @param instruction Program instruction buffer
     * @param lenInstruction Length of instruction buffer in use
     */
    public void setInstructions(char[] instruction, int lenInstruction)
    {
        // Save reference to instruction array
        this.instruction = instruction;
        this.lenInstruction = lenInstruction;

        // Initialize other program-related variables
        flags = 0;
        prefix = null;

        // Try various compile-time optimizations if there's a program
        if (instruction != null && lenInstruction != 0)
        {
            // If the first node is a branch
            if (lenInstruction >= RE.nodeSize && instruction[0 + RE.offsetOpcode] == RE.OP_BRANCH)
            {
                // to the end node
                int next = instruction[0 + RE.offsetNext];
                if (instruction[next + RE.offsetOpcode] == RE.OP_END)
                {
                    // and the branch starts with an atom
                    if (lenInstruction >= (RE.nodeSize * 2) && instruction[RE.nodeSize + RE.offsetOpcode] == RE.OP_ATOM)
                    {
                        // then get that atom as an prefix because there's no other choice
                        int lenAtom = instruction[RE.nodeSize + RE.offsetOpdata];
                        prefix = new char[lenAtom];
                        System.arraycopy(instruction, RE.nodeSize * 2, prefix, 0, lenAtom);
                    }
                }
            }

            BackrefScanLoop:

            // Check for backreferences
            for (int i = 0; i < lenInstruction; i += RE.nodeSize)
            {
                switch (instruction[i + RE.offsetOpcode])
                {
                    case RE.OP_ANYOF:
                        i += (instruction[i + RE.offsetOpdata] * 2);
                        break;

                    case RE.OP_ATOM:
                        i += instruction[i + RE.offsetOpdata];
                        break;

                    case RE.OP_BACKREF:
                        flags |= OPT_HASBACKREFS;
                        break BackrefScanLoop;
                }
            }
        }
    }
}
