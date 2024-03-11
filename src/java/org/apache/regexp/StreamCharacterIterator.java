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

import java.io.InputStream;
import java.io.IOException;

/** Encapsulates InputStream, ...
 *
 * @author <a href="mailto:ales.novak@netbeans.com">Ales Novak</a>
 */
public final class StreamCharacterIterator implements CharacterIterator
{
    /** Underlying is */
    private final InputStream is;

    /** Buffer of read chars */
    private final StringBuffer buff;

    /** read end? */
    private boolean closed;

    /** @param is an InputStream, which is parsed */
    public StreamCharacterIterator(InputStream is)
    {
        this.is = is;
        this.buff = new StringBuffer(512);
        this.closed = false;
    }

    /** @return a substring */
    public String substring(int offset, int length)
    {
        try
        {
            ensure(offset + length);
            return buff.toString().substring(offset, length);
        }
        catch (IOException e)
        {
            throw new StringIndexOutOfBoundsException(e.getMessage());
        }
    }

    /** @return a substring */
    public String substring(int offset)
    {
        try
        {
            readAll();
            return buff.toString().substring(offset);
        }
        catch (IOException e)
        {
            throw new StringIndexOutOfBoundsException(e.getMessage());
        }
    }


    /** @return a character at the specified position. */
    public char charAt(int pos)
    {
        try
        {
            ensure(pos);
            return buff.charAt(pos);
        }
        catch (IOException e)
        {
            throw new StringIndexOutOfBoundsException(e.getMessage());
        }
    }

    /** @return <tt>true</tt> iff if the specified index is after the end of the character stream */
    public boolean isEnd(int pos)
    {
        if (buff.length() > pos)
        {
            return false;
        }
        else
        {
            try
            {
                ensure(pos);
                return (buff.length() <= pos);
            }
            catch (IOException e)
            {
                throw new StringIndexOutOfBoundsException(e.getMessage());
            }
        }
    }

    /** Reads n characters from the stream and appends them to the buffer */
    private int read(int n) throws IOException
    {
        if (closed)
        {
            return 0;
        }

        int c;
        int i = n;
        while (--i >= 0)
        {
            c = is.read();
            if (c < 0) // EOF
            {
                closed = true;
                break;
            }
            buff.append((char) c);
        }
        return n - i;
    }

    /** Reads rest of the stream. */
    private void readAll() throws IOException
    {
        while(! closed)
        {
            read(1000);
        }
    }

    /** Reads chars up to the idx */
    private void ensure(int idx) throws IOException
    {
        if (closed)
        {
            return;
        }

        if (idx < buff.length())
        {
            return;
        }

        read(idx + 1 - buff.length());
    }
}
