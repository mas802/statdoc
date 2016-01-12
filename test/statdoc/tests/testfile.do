/** 
 * A java doc comment for this file
 * Some extra text that should go into the summary instead of just the tag line
 * Which could have extra lines
 *
 * @Author Markus Schaffner
 * @KevValue someKey someValue which goes until the end of the line
 * @statdocrun
 *
 */
set more off
clear
use "a_data_file.dta", clear

/*

A normal comment

*/

/**
 a comment for the summary statistic below
 
 @KeyVar someVar testVar
*/
tab someVar testVar

// a one line comment for the summary below
sum someVar* test?var

/**
  A one line summary for the regression below
  With more text below it if needed
  in multiple lines
  
  @DepVar dependent
  @KeyVar someVar
  @Control var1 var2 var3 var4
  
  */
reg dependent someVar var1 var2 var3 var4

// a for loop  
forvalue i = 1/20 {
    di "some output `i'"
    gen x`i' = test
}

