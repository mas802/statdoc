/**
 * draw the statdoc logo
 *
 * draws a nice logo for statdoc
 *
 * @author Markus Schaffner
 *
 */

cd "/Users/mas/workspace/java/statdoc/"
 
clear
set seed 802
set obs 3000

local var = 0.4
gen t = uniform()
gen theta = 3 * 3.151429 * ( t - 0.5 )
gen x = sin( theta ) + uniform() * `var'
gen y = sign( theta ) * (cos( theta ) - 1) + uniform() * `var'

replace x = x * (1.0 - 0.2*y) 
replace y = y 


local steps = 80
local stepas = round(1.2*`steps')

gen uv2 = invnormal(uniform())/4
gen uv1 = invnormal(uniform())/4

gen v1 = uv1
gen v2 = uv2

//tw scatter v1 v2, name("start", replace)
//graph export "raw.png", replace

forvalue stepa=0/`stepas' {

if ( `stepa' < `steps' ) {
   local step = `stepa'
}



replace v1 = uv1 + (`step'*(y-uv1))/`steps'
replace v2 = uv2 + (`step'*(x-uv2))/`steps'

set scheme s2color

local frac = round((255*`step')/`steps')
local invfrac = round(255 - ((255*`step')/`steps'))

local ysize = 5.5 - (1.5*(`step'))/`steps' 
local aspect = 0.66 + (0.34*(`step'))/`steps'
local titlesize = 0.0 + (5.0*(`step'))/`steps' 
local marg = 2.0 - (2.0*(`step'))/`steps' 

if ( `step' < (`steps'/2) ) { 
  local tsize = 3.5 - (7.0*(`step'))/`steps' 
  local title = "v2"
  local tlsize = 1.5 - (3.0*(`step'))/`steps' 
  local ylabelsize = 3.5 - (7.0*(`step'))/`steps' 
} 
else {
  local tsize = -10.0 + (20.0*(`step'))/`steps' 
  local title = "Statdoc"
  local tlsize = 0.0
  local ylabelsize = 0.0
}

local rmc = round( 26 + (255- 26)*((`step')/`steps')^8)
local gmc = round( 71 + (255- 71)*((`step')/`steps')^16)
local bmc = round(111 + (255-111)*((`step')/`steps')^16)

local rgr = round(234 + ((255-234)*`step')/`steps')
local ggr = round(242 + ((178-242)*`step')/`steps')
local bgr = round(243 + ((102-243)*`step')/`steps')

local rpr = round(255 + ((227-255)*`step')/`steps')
local gpr = round(255 + ((126-255)*`step')/`steps')
local bpr = round(255 + ((  0-255)*`step')/`steps')

tw scatter v1 v2,  aspect(`aspect') title( "  ", size(`titlesize') ) legend(off) ///
    ytitle("v1", size(`ylabelsize')) xtitle(" `title' ", size(`tsize'))  ///
 	xlabel(-1(0.5)1,labsize(`ylabelsize') tlength(`tlsize') ) ///
	ylabel(-1(0.5)1,labsize(`ylabelsize') tlength(`tlsize') nogrid ) ///
    mcolor( "`rmc' `gmc' `bmc'" ) ///
	graphregion( color( `rgr' `ggr' `bgr') ) ///round(
	plotregion(  margin(`marg' `marg' `marg' `marg') color( `rpr' `gpr' `bpr' ) ) ///
	name("move`stepa'", replace)
    
	local out = string(`stepa',"%05.0f") 
	
	graph export "/Volumes/Photos/statdoc/move`out'.png", replace width(720) 
	 // mcolor 255 251 240
	 // gr 255 178 102
	 // pr 227 126 0
	 
	// mcolor( eggshell ) ///
	// color( dkorange ) 
	// graphregion( color(orange*0.6 ) ) ///
	 
	// xsize(`ysize') ysize(4.0) ///

}
/*
tw scatter y x,  aspect(1) title( "  ", size(`titlesize')  ) legend(off) ///
    ytitle("", size(`ylabelsize'))   xtitle(" Statdoc ", size(`tsize'))  ///
    xlabel(none) ///
    ylabel(none) ///
	mcolor( eggshell ) ///
    graphregion( color(orange*0.6 ) ) ///
    plotregion(  margin(zero)  color( dkorange ) ) ///
	xsize(4.0) ysize(4.0) name("target",replace)
*/
// eof
