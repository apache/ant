/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.util.depend;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

import org.apache.bcel.classfile.Visitor;
import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.CodeException;
import org.apache.bcel.classfile.ConstantClass;
import org.apache.bcel.classfile.ConstantDouble;
import org.apache.bcel.classfile.ConstantFieldref;
import org.apache.bcel.classfile.ConstantFloat;
import org.apache.bcel.classfile.ConstantInteger;
import org.apache.bcel.classfile.ConstantInterfaceMethodref;
import org.apache.bcel.classfile.ConstantLong;
import org.apache.bcel.classfile.ConstantMethodref;
import org.apache.bcel.classfile.ConstantNameAndType;
import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantString;
import org.apache.bcel.classfile.ConstantUtf8;
import org.apache.bcel.classfile.ConstantValue;
import org.apache.bcel.classfile.Deprecated;
import org.apache.bcel.classfile.ExceptionTable;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.InnerClass;
import org.apache.bcel.classfile.InnerClasses;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.classfile.LineNumber;
import org.apache.bcel.classfile.LineNumberTable;
import org.apache.bcel.classfile.LocalVariable;
import org.apache.bcel.classfile.LocalVariableTable;
import org.apache.bcel.classfile.SourceFile;
import org.apache.bcel.classfile.StackMap;
import org.apache.bcel.classfile.StackMapEntry;
import org.apache.bcel.classfile.Synthetic;
import org.apache.bcel.classfile.Unknown;

public class Dependencies implements Visitor
{
    private boolean verbose = false;
    private Set dependencies = new HashSet();
    private ConstantPool constantPool;

    private JavaClass javaClass;

    public static void applyFilter( Collection collection, Filter filter )
    {
        Iterator i = collection.iterator();
        while( i.hasNext() )
        {
            Object next = i.next();
            if( !filter.accept( next ) )
            {
                i.remove();
            }
        }
    }

    public static void main( String[] args )
    {
        try
        {
            Dependencies visitor = new Dependencies();

            Set set = new TreeSet();
            Set newSet = new HashSet();

            int o = 0;
            String arg = null;
            if( "-base".equals( args[ 0 ] ) )
            {
                arg = args[ 1 ];
                if( !arg.endsWith( File.separator ) )
                {
                    arg = arg + File.separator;
                }
                o = 2;
            }
            final String base = arg;

            for( int i = o; i < args.length; i++ )
            {
                String fileName = args[ i ].substring( 0, args[ i ].length() - ".class".length() );
                if( base != null && fileName.startsWith( base ) ) {
                    fileName = fileName.substring( base.length() );
                }
                newSet.add( fileName );
            }
            set.addAll( newSet );

            do
            {
                Iterator i = newSet.iterator();
                while( i.hasNext() )
                {
                    String fileName = i.next() + ".class";

                    if( base != null )
                    {
                        fileName = base + fileName;
                    }

                    JavaClass javaClass = new ClassParser( fileName ).parse();
                    javaClass.accept( visitor );
                }
                newSet.clear();
                newSet.addAll( visitor.getDependencies() );
                visitor.clearDependencies();

                applyFilter( newSet,
                             new Filter()
                             {
                                 public boolean accept( Object object )
                                 {
                                     String fileName = object + ".class";
                                     if( base != null ) {
                                         fileName = base + fileName;
                                     }
                                     return new File( fileName ).exists();
                                 }
                             } );
                newSet.removeAll( set );
                set.addAll( newSet );
            } while( newSet.size() > 0 );

            Iterator i = set.iterator();
            while( i.hasNext() )
            {
                System.out.println( i.next() );
            }
        }
        catch( Exception e )
        {
            System.err.println( e.getMessage() );
            e.printStackTrace( System.err );
        }
    }

    public Set getDependencies()
    {
        return dependencies;
    }

    public void clearDependencies()
    {
        dependencies.clear();
    }

    public void visitCode( Code obj )
    {
    }

    public void visitCodeException( CodeException obj )
    {
    }

    public void visitConstantClass( ConstantClass obj )
    {
        if( verbose )
        {
            System.out.println( "visit ConstantClass" );
            System.out.println( obj.getConstantValue( constantPool ) );
        }
        dependencies.add( "" + obj.getConstantValue( constantPool ) );
    }

    public void visitConstantDouble( ConstantDouble obj )
    {
    }

    public void visitConstantFieldref( ConstantFieldref obj )
    {
    }

    public void visitConstantFloat( ConstantFloat obj )
    {
    }

    public void visitConstantInteger( ConstantInteger obj )
    {
    }

    public void visitConstantInterfaceMethodref( ConstantInterfaceMethodref obj )
    {
    }

    public void visitConstantLong( ConstantLong obj )
    {
    }

    public void visitConstantMethodref( ConstantMethodref obj )
    {
    }

    public void visitConstantNameAndType( ConstantNameAndType obj )
    {
    }

    public void visitConstantPool( ConstantPool obj )
    {
        if( verbose ) {
            System.out.println( "visit ConstantPool" );
        }
        this.constantPool = obj;

        // visit constants
        for( int idx = 0; idx < constantPool.getLength(); idx++ )
        {
            Constant c = constantPool.getConstant( idx );
            if( c != null )
            {
                c.accept( this );
            }
        }
    }

    public void visitConstantString( ConstantString obj )
    {
    }

    public void visitConstantUtf8( ConstantUtf8 obj )
    {
    }

    public void visitConstantValue( ConstantValue obj )
    {
    }

    public void visitDeprecated( Deprecated obj )
    {
    }

    public void visitExceptionTable( ExceptionTable obj )
    {
    }

    public void visitField( Field obj )
    {
        if( verbose )
        {
            System.out.println( "visit Field" );
            System.out.println( obj.getSignature() );
        }
        addClasses( obj.getSignature() );
    }

    public void visitInnerClass( InnerClass obj )
    {
    }

    public void visitInnerClasses( InnerClasses obj )
    {
    }

    public void visitJavaClass( JavaClass obj )
    {
        if( verbose )
        {
            System.out.println( "visit JavaClass" );
        }

        this.javaClass = obj;
        dependencies.add( javaClass.getClassName().replace( '.', '/' ) );

        // visit constant pool
        javaClass.getConstantPool().accept( this );

        // visit fields
        Field[] fields = obj.getFields();
        for( int i = 0; i < fields.length; i++ )
        {
            fields[ i ].accept( this );
        }

        // visit methods
        Method[] methods = obj.getMethods();
        for( int i = 0; i < methods.length; i++ )
        {
            methods[ i ].accept( this );
        }
    }

    public void visitLineNumber( LineNumber obj )
    {
    }

    public void visitLineNumberTable( LineNumberTable obj )
    {
    }

    public void visitLocalVariable( LocalVariable obj )
    {
    }

    public void visitLocalVariableTable( LocalVariableTable obj )
    {
    }

    public void visitMethod( Method obj )
    {
        if( verbose )
        {
            System.out.println( "visit Method" );
            System.out.println( obj.getSignature() );
        }
        String signature = obj.getSignature();
        int pos = signature.indexOf( ")" );
        addClasses( signature.substring( 1, pos ) );
        addClasses( signature.substring( pos + 1 ) );
    }

    public void visitSourceFile( SourceFile obj )
    {
    }

    public void visitStackMap( StackMap obj )
    {
    }

    public void visitStackMapEntry( StackMapEntry obj )
    {
    }

    public void visitSynthetic( Synthetic obj )
    {
    }

    public void visitUnknown( Unknown obj )
    {
    }

    void addClass( String string )
    {
        int pos = string.indexOf( 'L' );
        if( pos != -1 )
        {
            dependencies.add( string.substring( pos + 1 ) );
        }
    }

    void addClasses( String string )
    {
        StringTokenizer tokens = new StringTokenizer( string, ";" );
        while( tokens.hasMoreTokens() )
        {
            addClass( tokens.nextToken() );
        }
    }
}
