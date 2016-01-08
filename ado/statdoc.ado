/**
 *
 * statdoc.ado
 *
 * A utility program initiate a statdoc task 
 * 
 * @author (c) 2015 Markus Schaffner
 *
 */
 
/**
 * divert subprograms
 */
cap program drop statdoc
program statdoc
	version 13
 	gettoken subcmd 0 : 0

  if ( "`subcmd'" == "rkeyval" ) {
    statdoc_rkeyval `0'
  } 
  else  if ( "`subcmd'" == "do" ) {
    statdoc_do `0'
  } 
  else if ("`subcmd'" == ",") {
		statdoc_main , `0'
	} 
	else if ("`subcmd'" == "") {
		statdoc_main
	} 
	else {
		di "invalid command."
	}
end
 
 /**
  * main/default: run statdoc
  */
cap program drop statdoc_main
program statdoc_main
	version 13
	syntax , [Output(string)] [Source(string)] [Initialise] [Derived-clear] [Clear]
	
	local version = "v0.9.3-beta"

	if ( "`source'" == "" ) {
		local source = c(pwd)
	}
	
	if ( "`output'" == "" ) {
		local dir = c(pwd)
		local output = "`dir'/statdoc"
	}
	
	local init = ""
	if ( "`initialise'" != "" ) {
		local init = `""-i""'
	}
	
	local der = ""
	if ( "`derived-clear'" != "" ) {
		local der = `""-d""'
	}
	
	local cl = ""
	if ( "`clear'" != "" ) {
		local cl = `""-c""'
	}
	
	local exe = c(sysdir_stata)
	
	di "executing statdoc "
	di "with Stata in `exe'"
	di "in directory `source'"
	di "to output in directory `output'"
	di "options: " `init' " " `der' " " `clear'
	di " "
	
	javacall statdoc.Stata run, args( "-vc" "`version'" "-s" "`source'" "-o" "`output'" "statdoc.stata.path=`exe'" `init' `der' `cl' )
		
end

 
 /**
  * do: run statdoc single do file @statdocrun
  */
cap program drop statdoc_do
program statdoc_do
  version 13
  syntax anything(name=dofile), [Output(string)]
  
  local version = "v0.9.3-beta.2"
 
  if ( "`output'" == "" ) {
    local dir = c(pwd)
    local output = "`dir'/statdoc"
  }

  if ( "`source'" == "" ) {
    local source = c(pwd)
  }
     
  if ( `dofile' == "" ) {
    di "Please specify a file name "
  }
  else {
    local exe = c(sysdir_stata)
  
    di "executing statdoc "
    di "with Stata in `exe'"
    di "to run file " `dofile'
    di "to output in directory `output'"
    di " "
    
    javacall statdoc.Stata run, args( "-vc" "`version'" "-r" `dofile' "-s" "`source'" "-o" "`output'" "statdoc.stata.path=`exe'" )
  }
      
end


/**
 * utility to list keyvalues from return list with pre and suffix
 */
cap program drop statdoc_rkeyval
program statdoc_rkeyval, rclass
	version 13
	syntax namelist(name=list), [Prefix(string)] [Suffix(string)] [Format(string)]
    
	foreach l of local list {
		di "@`prefix'`l'`suffix' " `format' `r(`l')'
		// global `prefix'`l'`suffix' = `r(`l')'
	}
		
end

// eof
