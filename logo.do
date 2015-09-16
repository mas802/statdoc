

clear

set obs 5000

//...     t = mdp.numx_rand.random(npoints)
gen t = uniform()

local var = 0.4

//...     y = mdp.numx_rand.random(npoints)*5.
// gen y = uniform() * 5

//...     theta = 3.*mdp.numx.pi*(t-0.5)
gen theta = 3 * 3.151429 * ( t - 0.5 )

//...     x = mdp.numx.sin(theta)
gen x = sin( theta ) + uniform() * `var'

//...     z = mdp.numx.sign(theta)*(mdp.numx.cos(theta) - 1.)
gen z = sign( theta ) * (cos( theta ) - 1) + uniform() * `var'

replace x = x * (1.0 - 0.2*z) 

set scheme s2mono

tw scatter z x,  aspect(1) title( "  " ) legend(off)  yscale(range( 1 1 )) ///
    ytitle("")   xtitle(" Statdoc ")  ///
    ylabel(none) ///
	xlabel(none) ///
	mcolor( eggshell ) ///
    graphregion( color(orange ) ) ///
    plotregion(  margin(zero)  color( dkorange ) ) ///
	 xsize(1) ysize(1)

// eof
