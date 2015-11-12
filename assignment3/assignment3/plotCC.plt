# You can uncomment the following lines to produce a png figure
#set terminal png enhanced
#set output 'plot.png'

set title "Average Clustering Coefficient (RING)"
set xlabel "cycles"
set ylabel "clustering coefficient (log)"
set key right top
set logscale y 
plot "ccRandom30.txt" title 'Random Graph c = 30' with lines, \
	"ccRing30.txt" title 'Shuffle Ring c = 30' with lines, \
	"ccRandom50.txt" title 'Random Graph c = 50' with lines, \
	"ccRing50.txt" title 'Shuffle Ring c = 50' with lines
