package org.apache.tools.ant.filters;

import java.io.FilterReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import org.apache.tools.ant.types.Parameter;
import org.apache.tools.ant.types.Parameterizable;

/**
 * Attach a prefix to every line
 *
 * Example:
 * =======
 *
 * &lt;prefixlines prefix=&quot;Foo&quot;/&gt;
 *
 * Or:
 *
 * &lt;filterreader classname=&quot;org.apache.tools.ant.filters.PrefixLines&quot;&gt;
 *    &lt;param name=&quot;prefix&quot; value=&quot;Foo&quot;/&gt;
 * &lt;/filterreader&gt;
 *
 * @author <a href="mailto:umagesh@apache.org">Magesh Umasankar</a>
 */
public final class PrefixLines
    extends FilterReader
    implements Parameterizable, CloneableReader
{
    /**
     * prefix key
     */
    private static final String PREFIX_KEY = "prefix";

    private Parameter[] parameters;

    private boolean initialized = false;

    private String prefix = null;

    private String queuedData = null;

    /**
     * This constructor is a dummy constructor and is
     * not meant to be used by any class other than Ant's
     * introspection mechanism. This will close the filter
     * that is created making it useless for further operations.
     */
    public PrefixLines() {
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
    public PrefixLines(final Reader in) {
        super(in);
    }

    public final int read() throws IOException {
        if (!getInitialized()) {
            initialize();
            setInitialized(true);
        }

        int ch = -1;

        if (queuedData != null) {
            if (queuedData.length() == 0) {
                queuedData = null;
            } else {
                ch = queuedData.charAt(0);
                queuedData = queuedData.substring(1);
            }
        } else {
            ch = in.read();
            while (ch != -1) {
                if (queuedData == null) {
                    if (prefix != null) {
                        queuedData = prefix;
                    } else {
                        queuedData = "";
                    }
                }
                queuedData += (char) ch;
                if (ch == '\n') {
                    break;
                }
                ch = in.read();
            }
            if (queuedData != null) {
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

    public final long skip(long n) throws IOException {
        for (long i = 0; i < n; i++) {
            if (in.read() == -1) {
                return i;
            }
        }
        return n;
    }

    public final void setPrefix(final String prefix) {
        this.prefix = prefix;
    }

    private final String getPrefix() {
        return prefix;
    }

    private final void setInitialized(final boolean initialized) {
        this.initialized = initialized;
    }

    private final boolean getInitialized() {
        return initialized;
    }

    public final Reader clone(final Reader rdr) {
        PrefixLines newFilter = new PrefixLines(rdr);
        newFilter.setPrefix(getPrefix());
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
                if (PREFIX_KEY.equals(parameters[i].getName())) {
                    prefix = parameters[i].getValue();
                    break;
                }
            }
        }
    }
}
