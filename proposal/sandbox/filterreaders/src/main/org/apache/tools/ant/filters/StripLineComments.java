package org.apache.tools.ant.filters;

import java.io.FilterReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Vector;

import org.apache.tools.ant.types.Parameter;
import org.apache.tools.ant.types.Parameterizable;

/**
 * This is a line comment stripper reader
 *
 * Example:
 * =======
 *
 * <filterreader classname="org.apache.tools.ant.filters.StripLineComments">
 *    <param type="comment" value="#"/>
 *    <param type="comment" value="--"/>
 *    <param type="comment" value="REM "/>
 *    <param type="comment" value="rem "/>
 *    <param type="comment" value="//"/>
 * </filterreader>
 *
 * @author <a href="mailto:umagesh@apache.org">Magesh Umasankar</a>
 */
public final class StripLineComments
    extends FilterReader
    implements Parameterizable
{
    private static final String COMMENTS_KEY = "comment";

    private Parameter[] parameters;

    private boolean initialized = false;

    private final Vector comments = new Vector();

    private int commentsSize = 0;

    private String line = null;

    /**
     * Create a new filtered reader.
     *
     * @param in  a Reader object providing the underlying stream.
     */
    public StripLineComments(final Reader in) {
        super(in);
    }

    public final int read() throws IOException {
        if (!initialized) {
            initialize();
            initialized = true;
        }

        int ch = -1;

        if (line != null) {
            ch = line.charAt(0);
            if (line.length() == 1) {
                line = null;
            } else {
                line = line.substring(1);
            }
        } else {
            ch = in.read();
            while (ch != -1) {
                if (line == null) {
                    line = "";
                }
                line = line + (char) ch;
                if (ch == '\n') {
                    break;
                }
                ch = in.read();
            }

            if (line != null) {
                for (int i = 0; i < commentsSize; i++) {
                    String comment = (String) comments.elementAt(i);
                    if (line.startsWith(comment)) {
                        line = null;
                        break;
                    }
                }
                return read();
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

    public final long skip(final long n) throws IOException {
        for (long i = 0; i < n; i++) {
            if (in.read() == -1) return i;
        }
        return n;
    }

    /**
     * Set Parameters
     */
    public final void setParameters(final Parameter[] parameters) {
        this.parameters = parameters;
        initialized = false;
    }

    private final void initialize() {
        if (parameters != null) {
            for (int i = 0; i < parameters.length; i++) {
                if (COMMENTS_KEY.equals(parameters[i].getType())) {
                    comments.addElement(parameters[i].getValue());
                }
            }
            commentsSize = comments.size();
        }
    }
}
