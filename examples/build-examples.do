/**
 * a do file to build all examples one by one
 *
 * @author Markus Schaffner
 */
 
local homedir = "/Users/mas/"
local basedir = "`homedir'/workspace/web/statdoc"
local dropdir = "`homedir'/Dropbox"


// merging
statdoc, source("`dropdir'/National_Pride_Large_Data/") output("`basedir'/merging/") i d

// example
statdoc, source("`dropdir'/Stata_conference_2015/example") output("`basedir'/example/")

// autorun
//statdoc, source("`dropdir'/Stata_conference_2015/autorun") output("`basedir'/autorun/")

// itsp
statdoc, source("`dropdir'/Stata_conference_2015/itsp") output("`basedir'/itsp/")

// ado
// nothing right now

// camerson
statdoc, source("`dropdir'/Public/cameron") output("`basedir'/cameron/")

// allcot
statdoc, source("`dropdir'/Stata_conference_2015/Lightbulbs") output("`basedir'/allcot/")
