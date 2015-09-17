/*

mivars.ado

A small utility to report on which variable cause the number on the regression
to drop.

(c) 2014 Markus Schaffner

*/
cap program drop mivars
program mivars
	version 13
	syntax [varlist(default=none)]

	di "{text}"

/*
  check if a varlist is supplied, otherwise use e(cmdline)
*/
if ( "`varlist'"!="" ) {
    local vars = "`varlist'"
}
else {
    local vars = e(cmdline)
	local commapos = strpos("`vars'",",")
	if ( `commapos' > 0 ) {
	    local vars = substr("`vars'",1,`commapos')
	}
	local r = e(cmd)
    local vars : list vars - r
}

/*
  check for factor variables and lags (not supported) 
  as well as for non-variable entries and remove them from
  the list.
*/
foreach v of local vars {
    if ( strpos("`v'",".") > 0 ) {
        local vars : list vars - v
        noi di "note: `v' omitted, use xi: and generate lags as variables."
		noi di ""
	} 
	else {
	    cap unab tmp : `v'
		if ( _rc != 0 ) {
            local vars : list vars - v
		}
	}
}

/*
  unab expands the varlist into seperate variables
*/
unab vars : `vars'


/*
  setup the header with the number of observations in the dataset
*/
local total = c(N)
local currentobs = `total'

di "  # obs" _column(10) " {c |} # loss " _column(20) /*
   */ " {c |}    % " _column(30) " {c |}  % tot " _column(40) " {c |} Variable"
di "{hline 10}{c +}{hline 9}{c +}{hline 9}{c +}{hline 9}{c +}{hline 32}"
di " " %7.0f `total' _column(10) " {c |} " _column(20) /*
   */ " {c |} " _column(30) " {c |} " _column(40) " {c |} (total)"

/*
  setup the line for the dependent variable (first in the list)
*/
local depvar :word 1 of `vars'
local condition = "!mi(`depvar') "
qui sum `depvar' if `condition'
local delta = r(N) - `currentobs'
local currentobs = r(N)

di " " %7.0f `currentobs' _column(10) " {c |}" %7.0f `delta' _column(20) /*
   */ " {c |} " %6.2f (-100*`delta'/`total') _column(30) /*
   */ " {c |} " %6.2f (-100*`delta'/`total') _column(40) /*
   */ " {c |} `depvar' (dep)" 
di "{hline 10}{c +}{hline 9}{c +}{hline 9}{c +}{hline 9}{c +}{hline 32}"

// remove depvar from the list
local vars : list vars - depvar

/*
  loop for each variable
*/
local p :list sizeof vars
forvalues x = 1/`p' {
  local maxdrop = `currentobs'
  local maxdropvar = ""
  
  /*
    loop over all variables and find the biggest drop
  */
  foreach v of varlist `vars' {
    qui sum `v' if `condition'

    if ( r(N) <= `maxdrop' ) {
      local maxdrop = r(N)
      local maxdropvar "`v'"
    }
  }

  /* 
    calculate drop and display (only report drop if > 0
  */
  local delta = `maxdrop' - `currentobs'
  if ( `delta'<0 ) {
       di " " %7.0f `maxdrop' _column(10) " {c |}" %7.0f `delta' _column(20) /*
       */ " {c |} " %6.2f (-100*`delta'/`currentobs') _column(30) /*
       */ " {c |} " %6.2f (-100*`delta'/`total') _column(40) /*
       */ " {c |} `maxdropvar' " 
  } 
  else {
       di " " %7.0f `maxdrop' _column(10) " {c |}" _column(20) /*
       */ " {c |} " _column(30) " {c |} " _column(40) " {c |} `maxdropvar' " 
  }

  /*
    update the condition, vars and currentobs
  */
  local vars : list vars - maxdropvar
  local condition = "`condition' &  !mi(`maxdropvar') " 
  local currentobs = `maxdrop'
}

di "{hline 10}{c BT}{hline 9}{c BT}{hline 9}{c BT}{hline 9}{c BT}{hline 32}"

end

/*
//
// example
//

clear
sysuse auto

gen rnd = .

foreach v of varlist * {
    replace rnd = uniform()
	cap replace `v' = .  if rnd>0.90
	cap replace `v' = "" if rnd>0.90
}

reg  price i.mpg rep78 headroom trunk weight length displacement gear_ratio 
mivars

xi:reg  price i.mpg rep78 headroom trunk weight length displacement gear_ratio 
mivars

mivars price  rep78 headroom trunk weight length
*/