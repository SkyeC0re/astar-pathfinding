
all:
	mkdir -p 'Class Files'
	javac -d 'Class Files' Code/OpenSimplexNoise.java Code/Lattice2D.java Code/TestSuite.java

clean:
	rm -rf 'Class Files'/*
	rm -rf 'Output'/*
