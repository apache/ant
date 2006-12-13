public class Junk1 {
    public boolean mybool;
    public static void main (String[] args) {
        System.out.println("I have      a tab");
        //      Here is a comment with an embedded tab
        if (mybool) {   /* Here is a multi-line
                           (with embedded'      'tab)
            Comment */char mychar = '   ';      //<-tab->       <-
        } // end of if (mybool)
        
    } // end of main ()
}
