/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002 The Apache Software Foundation.  All rights
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
 * 4. The names "The Jakarta Project", "Ant", and "Apache Software
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
 */
package org.apache.ant.antlib.system;
import java.io.File;
import java.io.PrintStream;
import java.io.FileOutputStream;
import java.io.IOException;
import org.apache.ant.common.model.Project;
import org.apache.ant.common.service.ExecService;
import org.apache.ant.common.service.FileService;
import org.apache.ant.common.service.MagicProperties;
import org.apache.ant.common.util.AntException;
import org.apache.ant.common.util.FileUtils;
import org.apache.ant.common.logger.DefaultLogger;
import org.apache.ant.common.event.MessageLevel;

/**
 * The Ant task - used to execute a different build file
 *
 * @author Conor MacNeill
 * @created 4 February 2002
 */
public class Ant extends AntBase {
    /** The ant file to be run */
    private String antFileName;
    /** the base directory to use for the run */
    private File baseDir;
    /** File to capture any output */
    private String output;


    /**
     * sets the file containing the XML representation model to build
     *
     * @param antFileName the file to build
     */
    public void setAntFile(String antFileName) {
        this.antFileName = antFileName;
    }


    /**
     * Set the base directory for the execution of the build
     *
     * @param baseDir the base directory for the build
     */
    public void setDir(File baseDir) {
        this.baseDir = baseDir;
    }


    /**
     * The output file for capturing the build output
     *
     * @param output the output file for capturing the build output
     */
    public void setOutput(String output) {
        this.output = output;
    }


    /**
     * Run the sub-build
     *
     * @exception AntException if the build can't be run
     */
    public void execute() throws AntException {
        if (baseDir == null) {
            baseDir = getExecService().getBaseDir();
        }

        File antFile = null;

        if (antFileName == null) {
            antFile = new File(baseDir, "build.ant");
            if (!antFile.exists()) {
                antFile = new File(baseDir, "build.xml");
            }
        } else {
            antFile
                 = FileUtils.newFileUtils().resolveFile(baseDir, antFileName);
        }

        setProperty(MagicProperties.BASEDIR, baseDir.getAbsolutePath());

        ExecService execService = getExecService();
        Project model = execService.parseXMLBuildFile(antFile);
        Object key = execService.setupBuild(model, getProperties(), true);

        setSubBuildKey(key);

        if (output != null) {
            FileService fileService
                = (FileService) getCoreService(FileService.class);

            File outfile = null;
            if (baseDir != null) {
                outfile = FileUtils.newFileUtils().resolveFile(baseDir, output);
            } else {
                outfile = fileService.resolveFile(output);
            }
            try {
                PrintStream out
                    = new PrintStream(new FileOutputStream(outfile));
                DefaultLogger logger = new DefaultLogger();
                logger.setMessageOutputLevel(MessageLevel.MSG_INFO);
                logger.setOutputPrintStream(out);
                logger.setErrorPrintStream(out);
                execService.addBuildListener(key, logger);
            } catch (IOException ex) {
                log("Ant: Can't set output to " + output,
                    MessageLevel.MSG_INFO);
            }
        }

        execService.runBuild(key, getTargets());
        setSubBuildKey(null);
    }
}

