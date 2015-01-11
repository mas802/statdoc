args var output
/** 
 A do file to run the standard stats for a single variable. 
 It assumes the data is already loaded
 
 @version 0.1
*/

// set rmsg on

qui {
local category_cutoff = 100

cap qui log close _statdoc_var
qui log using "`output'", replace smcl name(_statdoc_var)

// this takes very long on large datasets (and does mostly the same as other things below)
// codebook `var'

local type : type `var' 
noi di "@type `type'"

local label : variable label `var' 
noi di "@label `label'"

local class = "other"

// test for string
capture confirm string variable `var'
if ( !_rc ) {
    // we now it is a string
    local isstr = "YES"
    capture tab `var', matcell( freq )
    if ( !_rc ) {
    
        // not too many obs
        local total = `r(N)'
        noi di "@N `total'"
        local unique = r(r)

        if ( `unique' < `category_cutoff' ) {
        forvalues i=1/`unique' {
            if ( `i' == 1 ) {
                noi di "@freq " _continue
            }
            noi di freq[`i',1] _continue
            if ( `i' == `unique' ) {
                noi di ""
            }
            else {
                noi di ", " _continue
            }
        }

        levelsof `var', local(levels)
        local i = 1
        foreach l of local levels {
            if ( `i' == 1 ) {
                noi di "@values " _continue
            }
            noi di `""`l'""' _continue
            if ( `i' == `unique' ) {
                noi di ""
            }
            else {
                noi di ", " _continue
            }
            local i = `i' + 1
        }
        }
        
        if ( `total' == 0 ) {
            local class = "empty"
        }
        else if ( `unique' == 1 ) {
            local class = "constant"
        }
        else if ( `unique' == 2 ) {
            local class = "dummy"
        }
        else if ( `unique' < `category_cutoff' & `unique' < `total' ) {
            local class = "category"
        }
        else if (`unique' == `total') {
            local class = "identifier"
        } 
        else {
            local class = "string"
        }        
    }
    else {
        // lots of observations, not sure what to do yet
        // should get random samples here
        noi di "@example0 " `var'[1]
    }
}
else {
    // else assume it is numeric

    noi inspect `var'

    noi di "@N `r(N)'"
	
	local unique = r(N_unique)
    if ( `r(N_unique)' < `category_cutoff' ) {
        local cat = "YES"
    }

    if ( "`r(N_unique)'" != "." ) {
        if ( `r(N)' == 0 ) {
            local class = "empty"
        }
        else if ( `r(N_unique)' == 1 ) {
            local class = "constant"
        }
        else if ( `r(N_unique)' == 2 ) {
            local class = "dummy"
        }
        else if ( `r(N_unique)' < `category_cutoff' & `r(N_unique)' < `r(N)' ) {
            capture confirm string variable `var'
            if ( !_rc ) {
                local class = "other"
            } 
            else {
                local class = "category"
            }
        }
        else if (`r(N_unique)' == `r(N)') {
            local class = "identifier"
        }
    }

// TODO this takes too long?   sum `var', detail
    noi sum `var'
    
    // TODO maybe the precision should be handled later (vm?)
    if ( ( "`type'" != "double" & "`type'" != "float" ) & "`class'" == "constant" ) {
        noi di "@mean " %9.0f r(mean)
    } 
    else {
        noi di "@mean " %9.3f r(mean)
    }
    noi di "@sd " %9.3f `r(sd)'
    // noi di "@skewness " %9.3f `r(skewness)'
    // noi di "@kurtosis " %9.3f `r(kurtosis)'
    
    if ( "`type'" == "double" | "`type'" == "float" ) {
      noi di "@min " %9.3f `r(min)'
      noi di "@max " %9.3f `r(max)'
      // noi di "@p50 " %9.3f `r(p50)'
    } 
    else {
      noi di "@min " %9.0f `r(min)'
      noi di "@max " %9.0f `r(max)'
      // noi di "@p50 " %9.0f `r(p50)'
    }
    
    
}


if ( ( "`class'" == "constant" ) & ( "`isstr'"!= "YES" ) ) {
    noi di "@values " `var'[1]
}
else if ( "`class'" == "category" | "`class'" == "dummy" ) {
    cap noi {
        if ( "`isstr'"!= "YES" ) {
            cap tab `var', matcell( freq ) matrow( name )
			if ( _rc != 0 | ("`r(r)'" != "`unique'") ) {
				local class = "other"
				local unique = r(r)
			}
			else {
                forvalues i=1/`unique' {
                    if ( `i' == 1 ) {
                        noi di "@values " _continue
                    }
                    noi di name[`i',1] _continue
                    if ( `i' == `unique' ) {
                        noi di ""
                    }
                    else {
                        noi di ", " _continue
                    }
                }
                forvalues i=1/`unique' {
                    if ( `i' == 1 ) {
                        noi di "@names " _continue
                    }
                    local val = name[`i',1]
                    local lab :label (`var') `val'
                    noi di `""`lab'""' _continue
                    if ( `i' == `unique' ) {
                        noi di ""
                    }
                    else {
                        noi di ", " _continue
                    }
                }
                forvalues i=1/`unique' {
                    if ( `i' == 1 ) {
                        noi di "@freq " _continue
                    }
                    noi di freq[`i',1] _continue
                    if ( `i' == `unique' ) {
                        noi di ""
                    }
                    else {
                        noi di ", " _continue
                    }
                }
			}
        }
    }
}
else {
    noi di "@example0 " `var'[1]
}

// histogram
if ( "`isstr'"!= "YES" ) {
cap {
    tempvar h w
    twoway__histogram_gen `var', gen( `h' `w' )
    forvalues i=1/`=r(n_x)' {
        if ( `i' == 1 ) {
            noi di "@histh " _continue
        }
        noi di `h'[`i'] _continue
        if ( `i' == `=r(n_x)' ) {
            noi di ""
        }
        else {
            noi di ", " _continue
        }
    }
    forvalues i=1/`=r(n_x)' {
        if ( `i' == 1 ) {
            noi di "@histw " _continue
        }
        noi di `w'[`i'] _continue
        if ( `i' == `=r(n_x)' ) {
            noi di ""
        }
        else {
            noi di ", " _continue
        }
    }
    }    
}


noi di "@class `class'"
noi di "@N_unique `unique'"

noi notes `var'
}


qui log close _statdoc_var

// eof
