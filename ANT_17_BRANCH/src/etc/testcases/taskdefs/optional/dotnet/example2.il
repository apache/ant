//   Licensed to the Apache Software Foundation (ASF) under one or more
//   contributor license agreements.  See the NOTICE file distributed with
//   this work for additional information regarding copyright ownership.
//   The ASF licenses this file to You under the Apache License, Version 2.0
//   (the "License"); you may not use this file except in compliance with
//   the License.  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//   Unless required by applicable law or agreed to in writing, software
//   distributed under the License is distributed on an "AS IS" BASIS,
//   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//   See the License for the specific language governing permissions and
//   limitations under the License.

.class public auto ansi beforefieldinit Example2
       extends [mscorlib]System.Object
{
} // end of class Example2

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



