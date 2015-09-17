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
replace y = y + 2.0

set scheme s2mono

tw scatter y x,  aspect(1) title( "  " ) legend(off) ///
    ytitle("")   xtitle(" Statdoc ", size(vhuge))  ///
    xlabel(none) ///
    ylabel(none) ///
	mcolor( eggshell ) ///
    graphregion( color(orange*0.6 ) ) ///
    plotregion(  margin(zero)  color( dkorange ) ) ///
	xsize(1) ysize(1)

graph export logo_512.png, width(512) replace
graph export logo_128.png, width(128) replace
graph export logo_64.png, width(64) replace
graph export logo_32.png, width(32) replace
	 
// eof
