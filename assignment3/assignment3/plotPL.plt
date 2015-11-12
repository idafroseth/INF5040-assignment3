# You can uncomment the following lines to produce a png figure
#set terminal png enhanced
#set output 'plot.png'

set title "Average Path Length (STAR)"
set xlabel "cycles"
set ylabel "average path length (log)"
set key right top
set logscale y 
plot "plRandom30.txt" title 'Random Graph c = 30' with lines, \
	"plStar30.txt" title 'Shuffle Star c = 30' with lines, \
	"plRandom50.txt" title 'Random Graph c = 50' with lines, \
	"plStar50.txt" title 'Shuffle Star c = 50' with lines
