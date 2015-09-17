capture program drop impuls
program define impuls
  version 9.1
  syntax varlist(min=1) [if], ECmd(name) EDepvar(varlist min=1 max=2) [EFixvar(varlist)] [CUToff(real 0.001)] [EOpt(string)] [SEED(integer -99)] [DUMmy(string)]  

  * display a catch phrase
  display
  display as text "Impuls identification block search over regressors" 
  display


  * get parameters and display them
  local nvar : list sizeof varlist
  quietly sum `edepvar' `if'
  local nobs = r(N)
  
  * set seed either random or with given value
  if (`seed' == -99) {
    local thistime = c(current_time)
    local seed = subinstr("`thistime'",":","",.)
  }
  set seed `seed'
   
  display as text "                 command: " as result " `ecmd' `edepvar' [regressors] ""`if'"", `eopt'"
  display as text "      significance level: " as result " `cutoff'"
  display as text "  no. initial regressors: " as result " `nvar'"
  display as text "        no. observations: " as result " `nobs'"
  display as text "             random seed: " as result " `seed'"

  * determine number of blocks
  local blocks = 2
  local nelems = `nvar' / `blocks'
  while (`blocks' <= `nelems') {
    local blocks = `blocks' * 2
    local nelems = `nvar' / `blocks'
  }
  local blocks = `blocks' / 2
  while (`nelems' > `nobs') {
    local nelems = `nvar' / `blocks'
    local blocks = `blocks' * 2
  }

  local nelems = `nvar' / `blocks'
  local nelems = ceil(`nelems')

  display
  display as text "sort variables into (" as result "`blocks':`nelems'" as text ") orthogonal blocks: " 

  * do the sorting
  foreach var of varlist `varlist' {
    local bpot = `nelems' + 1
    while (`bpot' >= `nelems') {
      local apot = ceil(`blocks' * uniform())
      local bpot : list sizeof varlistA_`apot'
*      display as result "test:   `apot' : `bpot'"
    }
    display as text "." _continue
    local bpot = `bpot' + 1
    
*   display as text "sorted: `apot' : `bpot'"
    local varlistA_`apot' : list varlistA_`apot' | var
    local varlistB_`bpot' : list varlistB_`bpot' | var
  }

  display
  display
  display as text "1 Step: block search for omitted regressors"
  display as text "            no of blocks: " as result " `blocks'"
  display

  forvalues block = 1(1)`blocks' { 
  
    local nvarb : list sizeof varlistA_`block'
    display as text " > block: " as result "`block'" as text "  size = " as result "`nvarb'" _continue
   quietly { 
    `ecmd' `edepvar' `varlistA_`block'' `efixvar'  `if', `eopt'

    local df = e(df_r)
    local _AS_varnames : colnames e(b)
    foreach varn of local _AS_varnames {
      if "`varn'"!="_cons" {
        local t = _coef[`varn'] / _se[`varn']
        local p = 2*ttail(`df',abs(`t'))
        if (`p'==.) local p = 2*(1-normal(abs(`t')))
        if (abs(`p')<`cutoff') & !mi(`p') {
          display "IN:  `varn' - `t' : `p'"
          local varlistA_last : list varlistA_last | varn
        }
        else {
          display "OUT: `varn' - `t' : `p'"
        }
      } 
    }
   }
   local nvarl : list sizeof varlistA_last
   display   as text " total = " as result "`nvarl'"
  }

local nvarl : list sizeof varlistA_last
display as text "  Step 1: total=" as result "`nvarl'" _continue

quietly { 
  `ecmd' `edepvar' `varlistA_last' `efixvar' `if', `eopt'

  local df = e(df_r)
  local _AS_varnames : colnames e(b)
  foreach varn of local _AS_varnames {
    if "`varn'"!="_cons" {
      local t = _coef[`varn'] / _se[`varn']
      local p = 2*ttail(`df',abs(`t'))
      if (`p'==.) local p = 2*(1-normal(abs(`t')))
      if (abs(`p')<`cutoff') & !mi(`p') {
        display "IN:  `varn' - `t' : `p'"
        local varlistA_final :list varlistA_final | varn
      }
      else {
        display "OUT: `varn' - `t' : `p'"
      }
    } 
  }
}

  local varlistA_final : list retokenize varlistA_final
  local varlistB_last : list sort varlistA_final

  local nvarl : list sizeof varlistB_last
  display as text " found=" as result "`nvarl'"

  
  display
  display as text "2 Step: orthogonal block search for omitted regressors"
  display as text "            no of blocks: " as result " `nelems'"
  display

  forvalues block = 1(1)`nelems' { 
  
    local nvarb : list sizeof varlistB_`block'
    display as text " > block: " as result "`block'" as text "  size = " as result "`nvarb'" _continue
   quietly { 
    `ecmd' `edepvar' `varlistB_`block'' `varlistA_final' `efixvar' `if', `eopt'

    local df = e(df_r)
    local _AS_varnames : colnames e(b)
    foreach varn of local _AS_varnames {
      if "`varn'"!="_cons" {
        local t = _coef[`varn'] / _se[`varn']
        local p = 2*ttail(`df',abs(`t'))
        if (`p'==.) local p = 2*(1-normal(abs(`t')))
        if (abs(`p')<`cutoff') & !mi(`p') {
          display "IN:  `varn' - `t' : `p'"
          local varlistB_last : list varlistB_last | varn
        }
        else {
          display "OUT: `varn' - `t' : `p'"
        }
      } 
    }
   }
   local nvarl : list sizeof varlistB_last
   display   as text " total = " as result "`nvarl'"
  }

local nvarl : list sizeof varlistB_last
display as text " Step 2: total=" as result "`nvarl'" _continue

quietly { 
  `ecmd' `edepvar' `varlistB_last' `efixvar' `if', `eopt'
  
  local df = e(df_r)
  local _AS_varnames : colnames e(b)
  foreach varn of local _AS_varnames {
    if "`varn'"!="_cons" {
      local t = _coef[`varn'] / _se[`varn']
      local p = 2*ttail(`df',abs(`t'))
      if (`p'==.) local p = 2*(1-normal(abs(`t')))
      if (abs(`p')<`cutoff') & !mi(`p') {
        display "IN:  `varn' - `t' : `p'"
        local varlistB_final "`varlistB_final' `varn'"
      }
      else {
        display "OUT: `varn' - `t' : `p'"
      }
    } 
  }
}

  local varlistB_final : list retokenize varlistB_final
  local varlistB_final : list sort varlistB_final
  local varlistB_final : list uniq varlistB_final
  local varlistB_final : list varlistB_final - efixvar

  local nvarf : list sizeof varlistB_final
  display as text "  final: found=" as result "`nvarf'"
  
  display
  display as text "final model:" 
  display as input "`ecmd' `edepvar' `varlistB_final' `efixvar', `eopt'"
  
  `ecmd' `edepvar' `varlistB_final' `efixvar' `if', `eopt'  

   quietly { 
   if "`dummy'" != "" {
    foreach varn of local varlistB_final {
        gen `dummy'_`varn' = `varn' `if'
        replace `dummy'_`varn' = 1 if `dummy'_`varn' != 0 & `dummy'_`varn' != .
    }
  }
  }
  
//  return local amvariables = varlistB_final

end

* eof
