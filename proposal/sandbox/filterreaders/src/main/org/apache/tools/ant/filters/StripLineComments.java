package org.apache.tools.ant.filters;

import java.io.FilterReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Vector;

import org.apache.tools.ant.types.Parameter;
import org.apache.tools.ant.types.Parameterizable;

/**
 * This is a line comment stripper reader
 *
 * Example:
 * =======
 *
 * &lt;striplinecomments&gt;
 *   &lt;comment value=&quot;#&quot;/&gt;
 *   &lt;comment value=&quot;--&quot;/&gt;
 *   &lt;comment value=&quot;REM &quot;/&gt;
 *   &lt;comment value=&quot;rem &quot;/&gt;
 *   &lt;comment value=&quot;//&quot;/&gt;
 * &lt;/striplinecomments&gt;
 *
 * Or:
 *
 * &lt;filterreader classname=&quot;org.apache.tools.ant.filters.StripLineComments&quot;&gt;
 *    &lt;param type=&quot;comment&quot; value="#&quot;/&gt;
 *    &lt;param type=&quot;comment&quot; value=&quot;--&quot;/&gt;
 *    &lt;param type=&quot;comment&quot; value=&quot;REM &quot;/&gt;
 *    &lt;param type=&quot;comment&quot; value=&quot;rem &quot;/&gt;
 *    &lt;param type=&quot;comment&quot; value=&quot;//&quot;/&gt;
 * &lt;/filterreader&gt;
 *
 * @author <a href="mailto:umagesh@apache.org">Magesh Umasankar</a>
 */
public final class StripLineComments
    extends FilterReader
    implements Parameterizable, CloneableReader
{
    private static final String COMMENTS_KEY = "comment";

    private Parameter[] parameters;

    private boolean initialized = false;

    private Vector comments = new Vector();

    private String line = null;

    /**
     * This constructor is a dummy constructor and is
     * not meant to be used by any class other than Ant's
     * introspection mechanism. This will close the filter
     * that is created making it useless for further operations.
     */
    public StripLineComments() {
        // Dummy constructor to be invoked by Ant's Introspector
        super(new StringReader(new String()));
        try {
            close();
        } catch (IOException  ioe) {
            // Ignore
        }
    }

    /**
     * Create a new filtered reader.
     *
     * @param in  a Reader object providing the underlying stream.
     */
    public StripLineComments(final Reader in) {
        super(in);
    }

    public final int read() throws IOException {
        if (!getInitialized()) {
            initialize();
            setInitialized(true);
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
                int commentsSize = comments.size();
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

    public final void addConfiguredComment(final Comment comment) {
        comments.addElement(comment.getValue());
    }

    private void setComments(final Vector comments) {
        this.comments = comments;
    }

    private final Vector getComments() {
        return comments;
    }

    private final void setInitialized(final boolean initialized) {
        this.initialized = initialized;
    }

    private final boolean getInitialized() {
        return initialized;
    }

    public final Reader clone(final Reader rdr) {
        StripLineComments newFilter = new StripLineComments(rdr);
        newFilter.setComments(getComments());
        newFilter.setInitialized(true);
        return newFilter;
    }

    /**
     * Set Parameters
     */
    public final void setParameters(final Parameter[] parameters) {
        this.parameters = parameters;
        setInitialized(false);
    }

    private final void initialize() {
        if (parameters != null) {
            for (int i = 0; i < parameters.length; i++) {
                if (COMMENTS_KEY.equals(parameters[i].getType())) {
                    comments.addElement(parameters[i].getValue());
                }
            }
        }
    }

    public static class Comment {
        private String value;

        public final void setValue(String comment) {
            value = comment;
        }

        public final String getValue() {
            return value;
        }
    }
}
