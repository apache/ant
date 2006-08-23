package org.apache.tools.ant.taskdefs.rmic;

import org.apache.tools.ant.types.Commandline;

/**
 * Run rmic in a new process with -Xnew set.
 * This switches rmic to use a new compiler, one that doesnt work in-process
 * on ant on java1.6 
 * @see: http://issues.apache.org/bugzilla/show_bug.cgi?id=38732
 */
public class XNewRmic extends ForkingSunRmic {

    /**
     * the name of this adapter for users to select
     */
    public static final String COMPILER_NAME = "xnew";
    
    public XNewRmic() {
    }

    /**
     * Create a normal command line, then with -Xnew at the front
     * @return a command line that hands off to thw
     */
    protected Commandline setupRmicCommand() {
        String options[]=new String[] {
                "-Xnew"
        };
        Commandline commandline = super.setupRmicCommand(options);
        return commandline;
    }

}
