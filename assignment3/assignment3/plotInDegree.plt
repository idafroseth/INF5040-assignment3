#set terminal png enhanced
#set output 'plot.png'

set title "In-degree distribution (STAR)"
set xlabel "in-degree"
set ylabel "number of nodes"
set key right top
plot "ddStar30.txt" title 'Basic Shuffle Star c = 30' with histeps, \
	"ddRandom30.txt" title 'Random Graph c = 30' with histeps, \
	"ddStar50.txt" title 'Basic Shuffle Star c = 50' with histeps, \
	"ddRandom50.txt" title 'Random Graph c = 50' with histeps
	
