
//  Microsoft (R) .NET Framework IL Disassembler.  Version 1.0.3705.0
//  Copyright (C) Microsoft Corporation 1998-2001. All rights reserved.


// Image base: 0x03180000
//
// ============== CLASS STRUCTURE DECLARATION ==================
//
.class public auto ansi beforefieldinit Example2
       extends [mscorlib]System.Object
{
} // end of class Example2


// =============================================================


// =============== GLOBAL FIELDS AND METHODS ===================


// =============================================================


// =============== CLASS MEMBERS DECLARATION ===================
//   note that class flags, 'extends' and 'implements' clauses
//          are provided here for information only

.class public auto ansi beforefieldinit Example2
       extends [mscorlib]System.Object
{
  .field public int32 some_variable
  .method public hidebysig specialname rtspecialname 
          instance void  .ctor() cil managed
  {
    // Code size       14 (0xe)
    .maxstack  2
    IL_0000:  ldarg.0
    IL_0001:  ldc.i4.3
    IL_0002:  stfld      int32 Example2::some_variable
    IL_0007:  ldarg.0
    IL_0008:  call       instance void [mscorlib]System.Object::.ctor()
    IL_000d:  ret
  } // end of method Example2::.ctor

} // end of class Example2


// =============================================================

//*********** DISASSEMBLY COMPLETE ***********************
// WARNING: Created Win32 resource file example2.res
