//created from ildasm of example.cs



.assembly extern /*23000001*/ mscorlib
{
  .publickeytoken = (B7 7A 5C 56 19 34 E0 89 )                         // .z\V.4..
  .ver 1:0:3300:0
}
.assembly /*20000001*/ example
{
  // --- The following custom attribute is added automatically, do not uncomment -------
  //  .custom /*0C000001:0A000001*/ instance void [mscorlib/* 23000001 */]System.Diagnostics.DebuggableAttribute/* 01000002 */::.ctor(bool,
  //                                                                                                                                  bool) /* 0A000001 */ = ( 01 00 00 01 00 00 ) 
  .hash algorithm 0x00008004
  .ver 0:0:0:0
}
.module example.exe
// MVID: {641E4709-F7B6-4BB0-BC5D-49C2CF1F1CF8}
.imagebase 0x00400000
.subsystem 0x00000003
.file alignment 512
.corflags 0x00000001
// Image base: 0x03180000
//
// ============== CLASS STRUCTURE DECLARATION ==================
//
.class /*02000002*/ public auto ansi beforefieldinit Example
       extends [mscorlib/* 23000001 */]System.Object/* 01000001 */
{
} // end of class Example


// =============================================================


// =============== GLOBAL FIELDS AND METHODS ===================


// =============================================================


// =============== CLASS MEMBERS DECLARATION ===================
//   note that class flags, 'extends' and 'implements' clauses
//          are provided here for information only

.class /*02000002*/ public auto ansi beforefieldinit Example
       extends [mscorlib/* 23000001 */]System.Object/* 01000001 */
{
  .method /*06000001*/ public hidebysig static 
          void  Main(string[] args) cil managed
  // SIG: 00 01 01 1D 0E
  {
    .entrypoint
    // Method begins at RVA 0x2050
    // Code size       11 (0xb)
    .maxstack  1
    IL_0000:  /* 72   | (70)000001       */ ldstr      "hello, I look like Java, but I'm really .NET" /* 70000001 */
    IL_0005:  /* 28   | (0A)000002       */ call       void [mscorlib/* 23000001 */]System.Console/* 01000003 */::WriteLine(string) /* 0A000002 */
    IL_000a:  /* 2A   |                  */ ret
  } // end of method Example::Main

  .method /*06000002*/ public hidebysig specialname rtspecialname 
          instance void  .ctor() cil managed
  // SIG: 20 00 01
  {
    // Method begins at RVA 0x2068
    // Code size       7 (0x7)
    .maxstack  1
    IL_0000:  /* 02   |                  */ ldarg.0
    IL_0001:  /* 28   | (0A)000003       */ call       instance void [mscorlib/* 23000001 */]System.Object/* 01000001 */::.ctor() /* 0A000003 */
    IL_0006:  /* 2A   |                  */ ret
  } // end of method Example::.ctor

} // end of class Example



