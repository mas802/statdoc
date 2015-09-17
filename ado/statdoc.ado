/**
 *
 * statdoc.ado
 *
 * A utility program initiate a statdoc task 
 * 
 * @author (c) 2015 Markus Schaffner
 *
 */
cap program drop statdoc
program statdoc
	version 13

	// TODO determine installation path

	local dir = c(pwd)
	local exe = c(sysdir_stata)
	
	di "executing statdoc "
	di "with Stata in `exe'"
	di "in directory `dir'"
	di " "
	
	javacall statdoc.Stata run, args( "-s" "`dir'" "-o" "`dir'/statdoc" "statdoc.stata.path=`exe'")
		
end

/**
 * 
 */
cap program drop returnkeyval
program returnkeyval, rclass
	version 13
	syntax namelist(name=list), [Prefix(string)] [Suffix(string)] [Format(string)]
    
	foreach l of local list {
		di "@`prefix'`l'`suffix' " `format' `r(`l')'
		// global `prefix'`l'`suffix' = `r(`l')'
	}
		
end
// eof
