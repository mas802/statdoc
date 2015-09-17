/*

savesig.ado

A small utility to report on which variable cause the number on the regression
to drop.

(c) 2014 Markus Schaffner

*/
cap program drop savesig
program savesig
	version 13
	syntax anything, [replace]

	di "{text}"

    di "saveing "`anything'
    local root = subinstr(`anything',".dta","",.)
    di `"saveing `root'"' 
    
    di "potential other/older versions:"
    dir `"`root'*.dta"'
    
    datasignature
    local sign = subinstr(subinstr(subinstr("`r(datasignature)'",":","_",.),")","_",.),"(","_",.)
    di "`sign'"

    cap noi save `"`root'_`sign'.dta"'

end

savesig "data/test.dta"
