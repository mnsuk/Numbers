# Numbers

A Content Analytics Studio normaliser plugin that aids in normalising numbers. It converts number strings to decimal values. By default the output format is a decimal with two digit precision. These may be mapped directly as parametric decimal field values in Watson Explorer Analytical Components server. The normaliser takes one mandatory parameter, which is the text string representing a number to normalise; the second optional parameter, specifies the precision. 

**Number forms supported**
- digits with embedded formatting e.g. 12,345,678 -> 12345678.00
- cardinals e.g. one million four hundred and thirty two thousand -> 1432000.00
- digits with magnitude e.g. 12.2B -> 12200000000.00
- digit multiplier with a single cardinal e.g. 7.15 million -> 7150000.00
- integer and cardinals e.g. 8 hundred and fifty thousand -> 850000.00
- simple fractions e.g.	1/3 -> 0.33
- fractions with whole numbers  e.g. 2 3/4i -> 2.75
- fractions e.g. three and a half  -> 3.50

The normaliser requires an accompnaying Studio project to identify the input numbers for normalisation.


