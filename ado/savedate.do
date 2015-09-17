/*

savedate.ado

A small utility to save a file with the current date to avoid overriding old 
versions.

(c) 2015 Markus Schaffner

*/
cap program drop savedate
program savedate
	version 13
	syntax anything

	di "{text}"

    di "saveing "`anything'
    local root = trim(subinstr(`anything',".dta","",.))
    di `"saveing `root'"' 
    
    di "potential other/older versions:"
    dir `"`root'*.dta"'
    
    local sign: display %td_CCYYNNDD date(c(current_date), "DMY")
    di "`sign'"

	local sign = trim(`sign')

    cap noi save `"`root'_`sign'.dta"', replace

end

// eof
