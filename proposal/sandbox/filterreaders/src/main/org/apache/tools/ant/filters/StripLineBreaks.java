package org.apache.tools.ant.filters;

import java.io.FilterReader;
import java.io.IOException;
import java.io.Reader;

import org.apache.tools.ant.types.Parameter;
import org.apache.tools.ant.types.Parameterizable;

/**
 * Filter to flatten the stream to a single line.
 *
 * @author Steve Loughran
 * @author <a href="mailto:umagesh@apache.org">Magesh Umasankar</a>
 */
public final class StripLineBreaks
    extends FilterReader
    implements Parameterizable
{
    /**
     * Linebreaks. What do to on funny IBM mainframes with odd line endings?
     */
    private static final String DEFAULT_LINE_BREAKS = "\r\n";

    /**
     * Linebreaks key that can be set via param element of
     * AntFilterReader
     */
    private static final String LINE_BREAKS_KEY = "linebreaks";

    private Parameter[] parameters;

    /**
     * Create a new filtered reader.
     *
     * @param in  a Reader object providing the underlying stream.
     */
    public StripLineBreaks(final Reader in) {
        super(in);
    }

    /**
     * Strip line break characters from an array.
     *
     * @exception  IOException  If an I/O error occurs
     */
    public final int read(final char[] cbuf) throws IOException {
        int length = -1;
        if (cbuf != null) {
            length = cbuf.length;
            if (in != null) {
                length = in.read(cbuf);
            }
            if (length != -1) {
                String str = new String(cbuf, 0, length);
                str = stripLineBreaks(str);
                final char[] newcbuf = str.toCharArray();
                System.arraycopy(newcbuf, 0, cbuf, 0, newcbuf.length);
                for (int j = newcbuf.length; j < cbuf.length; j++) {
                    cbuf[j] = 0;
                }
                length = newcbuf.length;
            }
        }
        return length;
    }

    /**
     * strip out all line breaks from a string.
     * @param source source
     * This implementation always duplicates the string; it is nominally possible to probe
     * the string first looking for any line breaks before bothering to do a copy. But we assume if
     * the option is requested, then line breaks are probably in the source string.
     */
    private final String stripLineBreaks(final String source) {
        final int len = source.length();
        String userDefinedLineBreaks = null;
        if (parameters != null) {
            for (int i = 0; i < parameters.length; i++) {
                if (LINE_BREAKS_KEY.equals(parameters[i].getName())) {
                    userDefinedLineBreaks = parameters[i].getValue();
                    break;
                }
            }
        }

        String lineBreaks = DEFAULT_LINE_BREAKS;
        if (userDefinedLineBreaks != null) {
            lineBreaks = userDefinedLineBreaks;
        }
        final StringBuffer dest = new StringBuffer(len);
        for(int i=0;i<len;++i) {
            final char ch=source.charAt(i);
            if(lineBreaks.indexOf(ch)==-1) {
                dest.append(ch);
            }
        }
        return new String(dest);

    }

    /**
     * Set Parameters
     */
    public final void setParameters(final Parameter[] parameters) {
        this.parameters = parameters;
    }
}