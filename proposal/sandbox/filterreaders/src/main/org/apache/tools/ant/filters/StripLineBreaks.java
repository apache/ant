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

    private String lineBreaks = DEFAULT_LINE_BREAKS;

    private boolean initialized = false;

    /**
     * Create a new filtered reader.
     *
     * @param in  a Reader object providing the underlying stream.
     */
    public StripLineBreaks(final Reader in) {
        super(in);
    }

    public final int read() throws IOException {
        if (!initialized) {
            String userDefinedLineBreaks = null;
            if (parameters != null) {
                for (int i = 0; i < parameters.length; i++) {
                    if (LINE_BREAKS_KEY.equals(parameters[i].getName())) {
                        userDefinedLineBreaks = parameters[i].getValue();
                        break;
                    }
                }
            }

            if (userDefinedLineBreaks != null) {
                lineBreaks = userDefinedLineBreaks;
            }

            initialized = true;
        }

        int ch = in.read();
        while (ch != -1) {
            if (lineBreaks.indexOf(ch) == -1) {
                break;
            } else {
                ch = in.read();
            }
        }
        return ch;
    }

    public final int read(final char cbuf[], final int off,
                          final int len) throws IOException {
        for (int i = 0; i < len; i++) {
            final int ch = read();
            if (ch == -1) {
                if (i == 0) {
                    return -1;
                } else {
                    return i;
                }
            }
            cbuf[off + i] = (char) ch;
        }
        return len;
    }

    public final long skip(long n) throws IOException {
        for (long i = 0; i < n; i++) {
            if (in.read() == -1) {
                return i;
            }
        }
        return n;
    }

    /**
     * Set Parameters
     */
    public final void setParameters(final Parameter[] parameters) {
        this.parameters = parameters;
    }
}
