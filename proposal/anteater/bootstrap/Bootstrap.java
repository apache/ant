// -------------------------------------------------------------------------------
// Copyright (c)2000 Apache Software Foundation
// -------------------------------------------------------------------------------

import java.io.*;
import java.util.*;
import java.util.zip.*;

/**
 * Quick and dirty single class bootstrap utility for getting Ant off
 * the ground when in need. To use, compile this file in the directory
 * where the source code is in the repository, then execute it. That's
 * it.<p>
 *
 * No pretense is made that this is an elegant peice of code. This code
 * only exists to do a ground zero build of Ant. Any other building of
 * Ant should be done with itself whenever possible.
 *
 * @author James Duncan Davidson (duncan@apache.org)
 */
public class Bootstrap {
   
    private static String base = "../";
    private static String[] modules = new String[]{"copy", "echo", "jar", "javac"};

    /**
     * Command line entry point.
     */
    public static void main(String[] args) {
    
        // check for secret sugar left by MRJ startup script...
    
        if (args.length > 0) {
            if (args[0].equals("osx")) {
                base = "";
            }
        }
    
        long startTime = System.currentTimeMillis();
    
        System.out.println("Starting Bootstrap....");

        // ------------------------------------------------------------
        // first create dirs that we need for strapping
        // ------------------------------------------------------------

        mkdir(base + "bootstrap/temp");
        mkdir(base + "bootstrap/temp/main");
        mkdir(base + "bootstrap/temp/tasks");
        mkdir(base + "bootstrap/temp/taskjars");
        
        for (int i = 0; i < modules.length; i++) {
            mkdir(base + "bootstrap/temp/tasks/" + modules[i]);
        }
        
        // ------------------------------------------------------------
        // build the main thing
        // ------------------------------------------------------------        
        
        Vector v = getSources(base + "source/main");
        doCompile(base + "bootstrap/temp/main", v);
        
        // ------------------------------------------------------------
        // now build each of the needed peices into their
        // areas within the strapping area
        // ------------------------------------------------------------

        for (int i = 0; i < modules.length; i++) {
            buildModule(modules[i]);
        }

        // ------------------------------------------------------------
        // now, set classpaths and launch an Ant build to
        // have Ant build itself nicely
        // ------------------------------------------------------------

        System.out.println();
        System.out.println("-------------------------------------------");
        System.out.println("STARTING REAL BUILD");
        System.out.println("-------------------------------------------");
        System.out.println();     
        
        String[] cmdarray = new String[9];
        cmdarray[0] = "java";
        cmdarray[1] = "-cp";
        cmdarray[2] = base + "bootstrap/temp/main:" + base + "lib/jaxp.jar:" +
                      base + "lib/crimson.jar";
        cmdarray[3] = "org.apache.ant.cli.Main";
        cmdarray[4] = "-taskpath";
        cmdarray[5] = base + "bootstrap/temp/taskjars";
        cmdarray[6] = "-buildfile";
        cmdarray[7] = base + "source/main.ant"; 
        cmdarray[8] = "default";
        
        try {
            Runtime runtime = Runtime.getRuntime();
            Process process = runtime.exec(cmdarray);
            
            // echo output from process
            
            InputStream in = process.getInputStream();
            byte[] buf = new byte[80];
            int count = 0;
            count = in.read(buf, 0, buf.length);
            while (count != -1) {
                System.out.write(buf, 0, count);
                count = in.read(buf, 0, buf.length);
            }
            
            in = process.getErrorStream();
            count = in.read(buf, 0, buf.length);          
            if (count > 0) {
                System.out.println();
                System.out.println("Error Stream Output:");
             
                while (count != -1) {
                    System.out.write(buf, 0, count);
                    count = in.read(buf, 0, buf.length);
                }
            }
        } catch (Exception e) {
            System.out.println("OUCHY: " + e);
            return;
        }
        
        System.out.println();
        System.out.println("-------------------------------------------");
        System.out.println("FINISHED WITH REAL BUILD");
        System.out.println("-------------------------------------------");
        System.out.println();
        
        // ------------------------------------------------------------
        // Remove Temporary classes
        // ------------------------------------------------------------

        // delete(tempDirName);

        // ------------------------------------------------------------
        // Print Closer
        // ------------------------------------------------------------

        long endTime = System.currentTimeMillis();
        long elapsd = endTime - startTime;
        System.out.println("Bootstrap Time: " + (elapsd/1000) + "." + (elapsd%1000) + 
                           " seconds");
    }

    private static void mkdir(String arg) {
        File dir = new File(arg);
        if (dir.exists() && !dir.isDirectory()) {
            System.out.println("Oh, horrors! Dir " + arg + " " +
                               "doesn't seem to be a dir... Stop!");
            System.exit(1);
        }
        if (!dir.exists()) {
            System.out.println("Making dir: " + arg);
            dir.mkdir();
        }
    }

    private static void buildModule(String arg) {
        System.out.println("Building " + arg);
     
        // get all sources and hand them off to the compiler to
        // build over into destination

        Vector v = getSources(base + "source/coretasks/" + arg);
        if (v.size() > 0) {
            doCompile(base + "bootstrap/temp/tasks/" + arg, v);
        }
        

        // move taskdef.properties for the module

        copyfile(base + "source/coretasks/" + arg + "/taskdef.properties",
                 base + "bootstrap/temp/tasks/" + arg + "/taskdef.properties");
                 
        // jar up tasks
        try {
            jarDir(new File(base + "bootstrap/temp/tasks/" + arg), 
                new File(base + "bootstrap/temp/taskjars/" + arg + ".jar"));
        } catch(IOException ioe) {
            System.out.println("problem jar'ing: " + arg);
        }
    }

    private static Vector getSources(String arg) {

        File sourceDir = new File(arg);
        
        Vector v = new Vector();
        scanDir(sourceDir, v, ".java");
        return v;
    }

    private static void jarDir(File dir, File jarfile) throws IOException {
        String[] files = dir.list();
        if (files.length > 0) {
            System.out.println("Jaring: " + jarfile);
            FileOutputStream fos = new FileOutputStream(jarfile);
            ZipOutputStream zos = new ZipOutputStream(fos);
            jarDir(dir, "", zos);
            zos.close();      
        }
    }
    
    private static void jarDir(File dir, String prefix, ZipOutputStream zos) throws 
        IOException 
    {
        String[] files = dir.list();
        for (int i = 0; i < files.length; i++) {
            File f = new File(dir, files[i]);
            if (f.isDirectory()) {
                jarDir(f, prefix + "/" + files[i], zos);
            } else {
                ZipEntry ze = new ZipEntry(prefix + "/" + files[i]);
                zos.putNextEntry(ze);
                FileInputStream fis = new FileInputStream(f);
                int count = 0;
                byte[] buf = new byte[8 * 1024];
                count = fis.read(buf, 0, buf.length);
                while (count != -1) {
                    zos.write(buf, 0, count);
                    count = fis.read(buf, 0, buf.length);
                }
                fis.close();
            }
        }
    }

    private static void scanDir(File dir, Vector v, String endsWith) {
        // System.out.println("user.dir=" + System.getProperty("user.dir"));
        // System.out.println("Scanning: " + dir);
        String[] files = dir.list();
        // System.out.println("Files: " + files);
        for (int i = 0; i < files.length; i++) {
            File f = new File(dir, files[i]);
            if (f.isDirectory()) {
                scanDir(f, v, endsWith);
            } else {
                if (files[i].endsWith(endsWith)) {
                    v.addElement(f);
                }
            }
        }
    }

    private static void doCompile(String dest, Vector sources) {
        System.out.println("   Compiling " + sources.size() + " files to " + dest);
        
        // XXX This should be more forgiving about compiling wherever
        // under whatever compiler, but this works so...
        
        sun.tools.javac.Main compiler = new sun.tools.javac.Main(System.out, 
                                                                 "javac");        
        String[] args = new String[sources.size() + 4];
        args[0] = "-classpath";
        args[1] = base + "bootstrap/temp/main:" + base + "lib/jaxp.jar:" + 
                  base + "lib/crimson.jar";
        args[2] = "-d";
        args[3] = dest;
        for (int i = 0; i < sources.size(); i++) {
            args[4+i] = ((File)sources.elementAt(i)).toString();
        }
        
        // System.out.print("javac ");
        // for (int i = 0; i < args.length; i++) {
        //     System.out.print(args[i] + " ");
        // }
        // System.out.println();
        
        compiler.compile(args);
    }

    private static void copyfile(String from, String dest) {
        File fromF = new File(from);
        File destF = new File(dest);
        if (fromF.exists()) {
            System.out.println("   Copying " + from);
            try {
                FileInputStream in = new FileInputStream(fromF);
                FileOutputStream out = new FileOutputStream(destF);
                byte[] buf = new byte[1024 * 16];
                int count = 0;
                count = in.read(buf, 0, buf.length);
                if (count != -1) {
                    out.write(buf, 0, count);
                    count = in.read(buf, 0, buf.length);
                }
                
                in.close();
                out.close();
            } catch (IOException ioe) {
                System.out.println("OUCH: " + from);
                System.out.println(ioe);
            }
        }
    }
}