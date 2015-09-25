# 0.9.2-beta (unreleased)



# 0.9.2-beta (25/09/2015)

- images get copied to output folder
- add @statdocrun functionality
- add logo
- fixing matching
- EmbedFileTask to embed files

## known limitations/bugs

- matching is not perfect yet
- hickups on non *nix platforms to be expected
- stata needs to be restarted to make it work
- unit testing has hugh gaps
- trailing /// gets marked as a comments in do files
- issues with regex parsing

# 0.9.1-beta (17/06/2015)

- improve smcl parsing
- changed stata executable to path only and detect binary automatically n
  `statdoc.stata.path`
- general clean up of the code base
- move part of the file handling to Java 7 noi classes
- add a restriction for the numbers of variables 
  with `statdoc.stata.maxvarobs`
- improve temp file deletion
- add changelog

## known limitations/bugs

- matching is not perfect yet
- hickups on non *nix platforms to be expected
- stata needs to be restarted to make it work
- unit testing has hugh gaps

# 0.9.0-beta

first full functional release