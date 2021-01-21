Different Algorithms which can be used when running Files (The Test Suite outputs are also labbeled according to this):
AS - A*
ASID - Iterative Deepening A*
DFID - Depth First Iterative Deepening
BDAS - Bi-Directional A*

Different Heuristics which can be used when running Files (The Test Suite outputs are also labbeled according to this):
MH - Manhattan
SLD - Staight Line Distance
DIJK - Uniform Cost (zero) heuristic
MHNook - Manhattan with nook and cranny pruning



Commands:
make - Compile
make clean - Clean All Output and Class Files
./run.sh [Lattice File Name] [Start x] [Start y] [End x] [End y] [Algorithm] [Hueristic] [Output Folder Name] - Run a specific file as a lattice and output the data to a folder.
./test.sh - Runs a test suite of simple cases which can be inspected for correctness. The ouput folders are named as [Algorithm]_[Hueristic]_[Maze Name]
