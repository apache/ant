This is the standard dom4j 1.2 release with a patch applied to jaxen.  The
patch fixes a problem where DocumentHelper.createPattern("aaa | bbb") would
fail never matching the element <bbb> because the pattern for "bbb" had been
overwritten by the pattern for "aaa".

--- org/jaxen/pattern/UnionPattern.java.orig    Wed Aug  8 17:29:49 2001
+++ org/jaxen/pattern/UnionPattern.java Wed Mar  6 01:39:51 2002
@@ -91,7 +91,7 @@
     public Pattern simplify()
     {
         this.lhs = lhs.simplify();
-        this.rhs = lhs.simplify();
+        this.rhs = rhs.simplify();
         init();
         return this;
     }



