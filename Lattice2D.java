import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.function.Function;

import javax.imageio.ImageIO;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.time.Instant;
import java.awt.image.BufferedImage;
import java.time.Duration;

/**
 * A Class for representing a 2D lattice with obstacles.
 * 
 * @author      Christoff van Zyl <20072015@sun.ac.za>
 * @version     1.0
 * 
 */
public class Lattice2D {

    //Search Types
    public static final int SEARCH_TYPE_DFID = 0;   //Depth First Iterative Deepening (Tree-Search)
    public static final int SEARCH_TYPE_ASID = 1;   //A* Iterative Deepening (Tree-Search)
    public static final int SEARCH_TYPE_AS = 2;     //A* (Graph-Search)
    public static final int SEARCH_TYPE_BDAS = 3;   //Bi-Directional A* (Graph-Search)
 
    public static final int TUNNEL_AUTO_EXPLORE_MAX = 1000;

    public static long getLongPos(int[] pos) {
        return (long) (((long)pos[0]) << 32) | (pos[1] & 0xffffffffL);
    }

    /**
     * A simple 4 parameter Function interface to use when creating hueristic lambda functions.
     */
    @FunctionalInterface
    public static interface Function4<One, Two, Three, Four, Five> {
        public Five apply(One one, Two two, Three three, Four four);
    }

    /**
     * A Simple subclass of a LinkedList in which poll returns the last, not the first element.
     */
    public static class LIFOQueue<E> extends LinkedList<E> {

        @Override
        public E poll() {
            return super.pollLast();
        }
    }

    /**
     * Used as a data structure to contain data regarding a search on a 2DLattice, as well as providing
     * functionality with respect to exporting the data.
     */
    public static class SearchResults {
        public Function<int[], Boolean> probe;
        public int[][] start;
        public int[][] end;
        public LinkedList<int[]> path;
        public double pathLen;
        public ArrayList<Double> leftDepths = null;
        public ArrayList<Double> rightDepths = null;
        public ArrayList<Long> leftExplored = null;
        public ArrayList<Long> rightExplored = null;
        public ArrayList<Long> timeTaken = null;
        public int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        public BufferedImage img;
        public long totalExplore = 0;
        public long totalTime = 0;

        /**
         * Initializes the class and stores all the search data.
         * 
         * @param probe The relevant probe function.
         * @param start The start position(s).
         * @param end The possible end position(s).
         * @param path The optimal path between any start and end position, null if non-existant.
         * @param pathLen The length of the optimal path.
         * @param leftClosed All the explored nodes starting from the start positions.
         * @param rightClosed All the explored nodes starting from the end positions.
         * @param leftDepths The f values of the left depths.
         * @param rightDepths The f values of the right depths.
         * @param leftExplored The total nodes explored from the start positions at each left depth.
         * @param rightExplored The total noded explored from the end positions at each right depth.
         * @param timeTaken The time taken (in milliseconds) for each depth search.
         */
        public SearchResults(Function<int[], Boolean> probe, int[][] start, int[][] end, LinkedList<int[]> path, double pathLen, HashMap<Long, Node> leftClosed, HashMap<Long, Node> rightClosed,
                            ArrayList<Double> leftDepths, ArrayList<Double> rightDepths, ArrayList<Long> leftExplored, ArrayList<Long> rightExplored, ArrayList<Long> timeTaken) {
            this.probe = probe;
            this.start = start;
            this.end = end;
            this.path = path;
            this.pathLen = pathLen;
            this.leftDepths = leftDepths;
            this.rightDepths = rightDepths;
            this.leftExplored = leftExplored;
            this.rightExplored = rightExplored;
            this.timeTaken = timeTaken;

            for (long explored : leftExplored) {
                totalExplore += explored;
            }
            for (long explored : rightExplored) {
                totalExplore += explored;
            }
            for (long timeMilli : timeTaken) {
                totalTime += timeMilli;
            }

            for (int[] pos : start) {
                if (pos[0] < minX) {
                    minX = pos[0];
                }

                if (pos[0] > maxX) {
                    maxX = pos[0];
                }

                if (pos[1] < minY) {
                    minY = pos[1];
                }

                if (pos[1] > maxY) {
                    maxY = pos[1];
                }
            }

            for (int[] pos : end) {
                if (pos[0] < minX) {
                    minX = pos[0];
                }

                if (pos[0] > maxX) {
                    maxX = pos[0];
                }

                if (pos[1] < minY) {
                    minY = pos[1];
                }

                if (pos[1] > maxY) {
                    maxY = pos[1];
                }   
            }

            if (path != null) {
                for (int[] pos : path) {
                    if (pos[0] < minX) {
                        minX = pos[0];
                    }

                    if (pos[0] > maxX) {
                        maxX = pos[0];
                    }

                    if (pos[1] < minY) {
                        minY = pos[1];
                    }

                    if (pos[1] > maxY) {
                        maxY = pos[1];
                    }
                }
            }

            int [] pos;
            for (Node n : leftClosed.values()) {
                pos = n.pos;
                if (pos[0] < minX) {
                    minX = pos[0];
                }

                if (pos[0] > maxX) {
                    maxX = pos[0];
                }

                if (pos[1] < minY) {
                    minY = pos[1];
                }

                if (pos[1] > maxY) {
                    maxY = pos[1];
                }
            }

            for (Node n : rightClosed.values()) {
                pos = n.pos;
                if (pos[0] < minX) {
                    minX = pos[0];
                }

                if (pos[0] > maxX) {
                    maxX = pos[0];
                }

                if (pos[1] < minY) {
                    minY = pos[1];
                }

                if (pos[1] > maxY) {
                    maxY = pos[1];
                }
            }

            minX-=5;
            minY-=5;
            maxX+=5;
            maxY+=5;


            int diffX = maxX - minX + 1;
            int diffY = maxY - minY + 1;

            try {
                BufferedImage img = new BufferedImage(diffX, diffY, BufferedImage.TYPE_INT_RGB);
                pos = new int[2];
                Node node = new Node(null, pos, 0.0, 0.0);
                for (int y = 0; y < diffY; y++) {
                    pos[1] = minY + y;
                    for (int x = 0; x < diffX; x++) {
                        pos[0] = minX + x;
                        
                        if (probe.apply(pos)) {
                            img.setRGB(x, y, 0xFF000000);
                        } else if (leftClosed.containsKey(node.getLongPos())) {
                            img.setRGB(x, y, 0xFF00FF00);
                        } else if (rightClosed.containsKey(node.getLongPos())) {
                            img.setRGB(x, y, 0xFF0000FF);
                        } else {
                            img.setRGB(x, y, 0xFFFFFFFF);
                        }
                        
                    }
                }
                if (path != null) {
                    for (int[] pathPos : path) {
                        img.setRGB(pathPos[0] - minX, pathPos[1] - minY, 0xFFFF0000);
                    }
                }

                this.img = img;
            } catch (Exception ex) {
                this.img = null;
            }
        }

        @Override
        public String toString() {
            return Double.toString(pathLen);
        }

        /**
         * Generates a .png image representing the nodes in the frontier and explored nodes (if present) (Green from start and Blue from end), as well as the optimal path (Red) if present.
         *
         * @param fname the name of the file to save the image to.
         */
        public void genImage(String fname) {
           
            if (img == null) {
                return;
            }

            try {
                ImageIO.write(img, "bmp",  new File(fname + ".bmp"));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        /**
         * Generates a .csv file with all relevant search information.
         *
         * @param fname the name of the file to save the output data to.
         */
        public void genCSV(String fname) {
            try {
                FileWriter csvWriter = new FileWriter(fname + ".csv");

                csvWriter.append("Basic Info Labels:, Basic Info: , , Path X, Path Y, , Left Depth, Left Nodes Explored, Right Depth, Right Nodes Explored,Time Taken (ms)\n");
                int i = 0;
                boolean tryNext = true;
                int [] pos;
                while(tryNext) {
                    tryNext = false;
                    if (i == 0) {
                        csvWriter.append("Total Nodes Explored:, ");
                        csvWriter.append(Long.toString(totalExplore));
                        csvWriter.append(",");
                        tryNext = true;

                    } else if (i == 1) {
                        csvWriter.append("Total Time Taken (ms):,");
                        csvWriter.append(Long.toString(totalTime));
                        csvWriter.append(",");
                        tryNext = true;
                    } else if (i == 2) {
                        csvWriter.append("Optimal Path Length:,");
                        csvWriter.append(Double.toString(pathLen));
                        csvWriter.append(",");
                        tryNext = true;
                    } else {
                        csvWriter.append(",,");
                    }
                    csvWriter.append(",");

                    if (path != null && !path.isEmpty()) {
                        pos = path.pollFirst();
                        csvWriter.append(Integer.toString(pos[0]));
                        csvWriter.append(",");
                        csvWriter.append(Integer.toString(pos[1]));
                        csvWriter.append(",");
                        tryNext = true;
                    } else {
                        csvWriter.append(",,");
                    }
                    csvWriter.append(",");


                    if (i < leftDepths.size()) {
                        csvWriter.append(Double.toString(leftDepths.get(i)));
                        csvWriter.append(",");
                        csvWriter.append(Long.toString(leftExplored.get(i)));
                        csvWriter.append(",");
                        tryNext = true;
                    } else {
                        csvWriter.append(",,");
                    }

                    if (i < rightDepths.size()) {
                        csvWriter.append(Double.toString(rightDepths.get(i)));
                        csvWriter.append(",");
                        csvWriter.append(Long.toString(rightExplored.get(i)));
                        csvWriter.append(",");
                        tryNext = true;
                    } else {
                        csvWriter.append(",,");
                    }

                    if (i < timeTaken.size()) {
                        csvWriter.append(Long.toString(timeTaken.get(i)));
                        tryNext = true;
                    }


                    csvWriter.append("\n");
                    i++;
                }

                csvWriter.flush();
                csvWriter.close();
            } catch (Exception ex) {}
        }

        public void genFolder(String folderName) {
            try {
                new File(folderName).mkdirs();
                genCSV(folderName + "/Data");
                genImage(folderName + "/Image");
            } catch (Exception ex) {}
        }

    }

    /**
     * Generates all the valid neighbours of a position.
     *
     * @param pos the position of which to return the neighbours for.
     * @param probe the 2D Lattice probe function.
     * @return an arraylist of valid neighbour positions.
     */
    public static ArrayList<int[]> nodeNN(int[] pos, Function<int[], Boolean> probe) {
        ArrayList<int[]> nn = new ArrayList<int[]>();

        int[] nnPos = pos.clone();
        nnPos[0]++;

        if (!probe.apply(nnPos)) {
            nn.add(nnPos);
        }

        nnPos = pos.clone();
        nnPos[0]--;

        if (!probe.apply(nnPos)) {
            nn.add(nnPos);
        }

        nnPos = pos.clone();
        nnPos[1]++;

        if (!probe.apply(nnPos)) {
            nn.add(nnPos);
        }

        nnPos = pos.clone();
        nnPos[1]--;

        if (!probe.apply(nnPos)) {
            nn.add(nnPos);
        }
        return nn;

    }

     /**
     * Generates all the valid neighbours of a Node, will tunnel if a tunnel is detected (A node that has only 1 neighbour due to obstacles).
     *
     */
    private static LinkedList<Node> genValidNeighbours(Node node, Function<int[], Boolean> probe, Function4<Function<int[], Boolean>, int[], Node, int[][], Double> h,
                    int[][] dest, HashMap<Long, Node> selfClosed, HashMap<Long, Node> otherClosed, boolean onlyRefine) {
        LinkedList<Node> neighbours = new LinkedList<Node>();
        Node checkNode;
        int[] parentPos = null;
        double tempH;
        int[] pos;
        int counter = 0;
        int probeCount;
        while (counter < TUNNEL_AUTO_EXPLORE_MAX) {
            neighbours.clear();
            if (node.parent != null) {
                parentPos = node.parent.pos;
            } else {
                parentPos = null;   
            }
            probeCount = 0;
            for (int posDim = 0; posDim < 2; posDim++) {
                for (int add = 1; add >= -1; add -= 2) {
                    pos = node.pos.clone();
                    pos[posDim] += add;
                    if (probe.apply(pos)) {
                        probeCount++;
                        continue;
                    }

                    if (parentPos != null) {
                        if (pos[0] == parentPos[0] && pos[1] == parentPos[1]) {
                            probeCount++;
                            continue;
                        }
                    }

                    

                    if ((((checkNode = selfClosed.get(getLongPos(pos))) != null) && (checkNode.gVal <= node.gVal + 1.0)) || (onlyRefine && checkNode == null)) {
                        continue;
                    }
                    tempH = h.apply(probe, pos, node, dest);
                    if (tempH != Double.POSITIVE_INFINITY) {
                        neighbours.add(new Node(node, pos, node.gVal + 1.0, tempH));
                    }
                    
                }
            }

            counter++;
            if (probeCount == 3 && neighbours.size() == 1) {
                node = neighbours.peek();
                if (otherClosed.get(node.getLongPos()) != null) {
                    break;
                }
                continue;
            }
            break;
            
        }
        
        
        return neighbours;

    }

    /**
    * A Data structure used to represent positions in the search space, with respect to their positions, h(n) and g(n) values, as well as their parent position.
    */
    public static class Node implements Comparable<Node>{
        public Node parent;
        public int[] pos;
        public double gVal, hVal;

        public Node(Node parent, int[] pos, double gVal, double hVal) {
            this.parent = parent;
            this.pos = pos;
            this.gVal = gVal;
            this.hVal = hVal;
        }

        @Override
        public int hashCode() {
            int hash = 17;
            hash = ((hash + pos[0]) << 5) - (hash + pos[0]);
            hash = ((hash + pos[1]) << 5) - (hash + pos[1]);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }

            if (!(obj instanceof Node)) {
                return false;
            }

            Node o = (Node) obj;
            if (pos[0] != o.pos[0] || pos[1] != o.pos[1]) {
                return false;
            }

            return true;
        }

        public int compareTo(Node o) {
            if (this.gVal + this.hVal < o.gVal + o.hVal) {
                return -1;
            }

            if (this.gVal + this.hVal > o.gVal + o.hVal) {
                return 1;
            }

            if (this.hVal < o.hVal) {
                return -1;
            }

            if (this.hVal > o.hVal) {
                return 1;
            }

            return 0;
        }

        public long getLongPos() {
            return (long) (((long)pos[0]) << 32) | (pos[1] & 0xffffffffL);
        }

    }

   

    private int[][] start;
    private int[][] end;
    private Function<int[], Boolean> probe;


    public Lattice2D(boolean[][] board, int[][] start, int[][] end) {
        Function<int[], Boolean> probe = (pos) -> {
            if (pos[1] < 0 || pos[1] >= board.length) {
                return true;
            }

            if (pos[0] < 0 || pos[0] >= board[0].length) {
                return true;
            }
            if (board[pos[1]][pos[0]] == true) {
                System.out.println("ERRR");
            }
            return board[pos[1]][pos[0]];
        };
        this.probe = probe;
        this.start = start;
        this.end = end;
    }

    public Lattice2D(Function<int[], Boolean> probe, int[][] start, int[][] end) {
        this.probe = probe;
        this.start = start;
        this.end = end;
    }

    /**
     * Searches the lattice with a specific search method and hueristic(s).
     *
     * @param h1 the primary hueristic lambda function.
     * @param h2 the secondary hueristic lambda function (used for the backwards search in Bi-Directional A*)
     * @param searchType the search type to use, see class constants.
     * @return a SearchResults data structure containing all the pertinent information regarding the search.
     */
    public SearchResults solve(Function4<Function<int[], Boolean>, int[], Node, int[][], Double> h1, Function4<Function<int[], Boolean>, int[], Node, int[][], Double> h2, int searchType) {
        SearchResults ret;

        Queue<Node> leftOpen, rightOpen;
        double pathLen = Double.POSITIVE_INFINITY;
        long leftExplore = 0, rightExplore = 0;
        Node middleFromLeft = null, middleFromRight = null;
        HashMap<Long, Node> leftClosed = new HashMap<Long, Node>();
        HashMap<Long, Node> rightClosed = new HashMap<Long, Node>();
        ArrayList<Double> leftDepths = new ArrayList<Double>();
        ArrayList<Double> rightDepths = new ArrayList<Double>();
        ArrayList<Long> leftExplored = new ArrayList<Long>();
        ArrayList<Long> rightExplored = new ArrayList<Long>();
        ArrayList<Long> timeTaken = new ArrayList<Long>();

        Node workingNode;
        Node checkNode;
        boolean allEmpty = true, run = false;
        Instant startTime = Instant.now();
        boolean oneStart = false, oneEnd = false;

        for (int[] pos : start) {
            if (!probe.apply(pos)) {
                oneStart = true;
                break;
            }
        }

        for (int[] pos : end) {
            if (!probe.apply(pos)) {
                oneEnd = true;
                break;
            }
        }        

        run = oneStart && oneEnd;

        //Tree Iterative Deepening Searches
        if (searchType == SEARCH_TYPE_DFID || searchType == SEARCH_TYPE_ASID) {
            double leftCurrDepth = -1.0, leftNextDepth;
            leftNextDepth = 0.0;
            
            if (searchType == SEARCH_TYPE_DFID) {
                System.out.println("Running Depth First Iterative Deepening Search:");
                leftOpen = new LIFOQueue<Node>();
                h1 = hNULL;
            } else {
                System.out.println("Running A* Iterative Deepening Search:");
                leftOpen = new LIFOQueue<Node>();
            }
            Instant depthStart = (startTime = Instant.now());
            while(run) {
               
                if (allEmpty) {
                
                    if (leftNextDepth == Double.POSITIVE_INFINITY) {
                        break;
                    }
    
                    if (pathLen < Double.POSITIVE_INFINITY) {
                       break;
                    }
    
                    allEmpty = false;
    
                   

                    if (!rightClosed.isEmpty()) {
                        leftDepths.add(leftCurrDepth);
                        leftExplored.add(leftExplore);
                        long millTime = Duration.between(depthStart, Instant.now()).toMillis();
                        timeTaken.add(millTime);
                        System.out.println("Depth: " + leftCurrDepth + " || Explored: " + leftExplore + " || Time(ms): " + millTime);
                    } 
                    leftClosed.clear();
                    leftOpen.clear();

                    leftExplore = 0;
                    leftCurrDepth = leftNextDepth;
                    leftNextDepth = Double.POSITIVE_INFINITY;
                    
                    Node newNode;
                    for (int[] pos : start) {
                        if (!probe.apply(pos)) {
                            newNode = new Node(null, pos, 0, h1.apply(probe, pos, null, end));
                            leftOpen.add(newNode);
                            leftClosed.put(newNode.getLongPos(), newNode);
                            leftExplore++;
                        }
                    }
                    
                    
                    if (rightClosed.isEmpty()) {
                        for (int[] pos : end) {
                            if (!probe.apply(pos)) {
                                newNode = new Node(null, pos, 0, 0);
                                if ((checkNode = leftClosed.get(newNode.getLongPos())) != null) {
                                    middleFromLeft = checkNode;
                                    middleFromRight = newNode;
                                    pathLen = 0.0;
                                    run = false;
                                    break;
                                }
                                rightClosed.put(newNode.getLongPos(), newNode);
                                rightExplore++;
                            }
                        }
                        if (!run) {
                            break;
                        }
                    }
    
                    depthStart = Instant.now();
                    
                }
                    
            
                if ((workingNode = leftOpen.poll()) == null) {
                    allEmpty = true;
                } else if ((checkNode = leftClosed.get(workingNode.getLongPos())) != null && workingNode == checkNode) {
                    leftClosed.remove(workingNode.getLongPos());
                    for (Node newNode : genValidNeighbours(workingNode, probe, h1, end, leftClosed, rightClosed, false)) {
                        if (newNode.gVal + Math.ceil(newNode.hVal) <= leftCurrDepth) {
                            if ((checkNode = rightClosed.get(newNode.getLongPos())) != null) {
                                if (newNode.gVal < pathLen) {
                                    pathLen = newNode.gVal;
                                    middleFromLeft = newNode;
                                    middleFromRight = checkNode;
                                    run = false;
                                    leftOpen.clear();
                                    break;
                                }
                            } else {
                                leftOpen.add(newNode);
                                leftClosed.put(newNode.getLongPos(), newNode);
                                leftExplore++;
                            }
                        } else if (newNode.gVal + Math.ceil(newNode.hVal) < leftNextDepth) {
                            leftNextDepth = newNode.gVal + newNode.hVal;
                        }
                    }
                        

                }
                
               
            }

            leftDepths.add(leftCurrDepth);
            leftExplored.add(leftExplore);
        
        // Graph Searches
        } else if (searchType == SEARCH_TYPE_AS || searchType == SEARCH_TYPE_BDAS) {
            if (searchType == SEARCH_TYPE_AS) {
                System.out.println("Running A*:");
            } else {
                System.out.println("Running Bi-Directional A*:");
            }
            leftOpen = new PriorityQueue<Node>();
            rightOpen = new PriorityQueue<Node>();

            
            for (int[] pos : start) {
                if (!probe.apply(pos)) {
                    Node newNode = new Node(null, pos, 0, h1.apply(probe, pos, null , end));
                    leftOpen.add(newNode);
                    leftClosed.put(newNode.getLongPos(), newNode);
                    leftExplore++;
                }
            }
            
            
            for (int[] pos : end) {
                if (!probe.apply(pos)) {
                    Node newNode;
                    if (searchType == SEARCH_TYPE_BDAS) {
                        newNode = new Node(null, pos, 0, h2.apply(probe, pos, null, start));
                    } else {
                        newNode = new Node(null, pos, 0, 0);
                    }
                    if ((checkNode = leftClosed.get(newNode.getLongPos())) != null) {
                        middleFromLeft = checkNode;
                        middleFromRight = newNode;
                        pathLen = 0.0;
                        run = false;
                        break;
                    }
                    if (searchType == SEARCH_TYPE_BDAS) {
                        rightOpen.add(newNode);
                    }
                    rightClosed.put(newNode.getLongPos(), newNode);
                    rightExplore++;
                }
            }
            allEmpty = false;
            boolean rightOnlyRefine = false, leftOnlyRefine = false;
            startTime = Instant.now();
            while(run && !allEmpty) {
            
                if ((workingNode = leftOpen.poll()) == null) {
                    if (pathLen == Double.POSITIVE_INFINITY) {
                        break;
                    }
                    allEmpty = true;
                } else if (workingNode.gVal + workingNode.hVal < pathLen) {
                    if ((checkNode = leftClosed.get(workingNode.getLongPos())) != null && workingNode == checkNode) {
                        
                        for (Node newNode : genValidNeighbours(workingNode, probe, h1, end, leftClosed, rightClosed, leftOnlyRefine)) {  
                            if (newNode.gVal + Math.ceil(newNode.hVal) < pathLen) {
                                if ((checkNode = rightClosed.get(newNode.getLongPos())) != null) {
                                    if (newNode.gVal + checkNode.gVal < pathLen) {
                                        pathLen = newNode.gVal + checkNode.gVal;
                                        middleFromLeft = newNode;
                                        middleFromRight = checkNode;
                                        rightOnlyRefine = true;
                                    }
                                } else {
                                    leftOpen.add(newNode);
                                    //leftClosed.put(newNode.getLongPos(), newNode);
                                    do {
                                        leftClosed.put(newNode.getLongPos(), newNode);
                                        newNode = newNode.parent;
                                        leftExplore++;
                                    } while (newNode != workingNode);
                                }
                                
                            }
                        }
                    }
                    
                } else {
                    allEmpty = true;
                    rightOnlyRefine = true;
                    leftOpen.clear();
                }
    
                if (searchType == SEARCH_TYPE_BDAS) {
                    if ((workingNode = rightOpen.poll()) == null) {
                        if (pathLen == Double.POSITIVE_INFINITY) {
                            break;
                        }
                    } else if (workingNode.gVal + workingNode.hVal < pathLen) {
                        if ((checkNode = rightClosed.get(workingNode.getLongPos())) != null && workingNode == checkNode) {
                            
                            for (Node newNode : genValidNeighbours(workingNode, probe, h2, start, rightClosed, leftClosed, rightOnlyRefine)) {
                                
                                if (newNode.gVal + Math.ceil(newNode.hVal) < pathLen) {
                                    if ((checkNode = leftClosed.get(newNode.getLongPos())) != null) {
                                        if (newNode.gVal + checkNode.gVal < pathLen) {
                                            pathLen = newNode.gVal + checkNode.gVal;
                                            middleFromLeft = checkNode;
                                            middleFromRight = newNode;
                                            rightOnlyRefine = true;
                                        }
                                    } else {
                                        rightOpen.add(newNode);
                                        //rightClosed.put(newNode.getLongPos(), newNode);
                                        do {
                                            rightClosed.put(newNode.getLongPos(), newNode);
                                            newNode = newNode.parent;
                                            rightExplore++;
                                        } while (newNode != workingNode);
                                    }
                                    
                                }
                            }
                        }
                        
                    } else {
                        leftOnlyRefine = true;
                        rightOpen.clear();
                    }
                }
            }
            Instant finishTime = Instant.now();
            timeTaken.add(Duration.between(startTime, finishTime).toMillis());

            leftDepths.add(Double.POSITIVE_INFINITY);
            leftExplored.add(leftExplore);
            if (searchType == SEARCH_TYPE_BDAS) {
                rightDepths.add(Double.POSITIVE_INFINITY);
                rightExplored.add(rightExplore);
            }
        }

        
        

        LinkedList<int[]> path;
        if (pathLen < Double.POSITIVE_INFINITY) {
            path = new LinkedList<int[]>();
            while (middleFromLeft != null) {
                path.addFirst(middleFromLeft.pos);
                middleFromLeft = middleFromLeft.parent;
            }

            middleFromRight = middleFromRight.parent;
            while (middleFromRight != null) {
                path.addLast(middleFromRight.pos);
                middleFromRight = middleFromRight.parent;
            }
            
        } else {
            path = null;
        }
        
        ret = new SearchResults(probe, start, end, path, pathLen, leftClosed, rightClosed, leftDepths, rightDepths, leftExplored, rightExplored, timeTaken);
        System.out.println("Search Completed: Optimal Path Length: " + Double.toString(pathLen) + " || Total Nodes Explored: " + ret.totalExplore + " || Time(ms): " + ret.totalTime);
        return ret;
    }

    /**
     * Straight Line Hueristic Function
     */
    public static Function4<Function<int[], Boolean>, int[], Node, int[][], Double> hSLD = (probe, pos, parent, end) -> {
        if (end.length < 1) {
            return Double.POSITIVE_INFINITY;
        }

        double min = Math.sqrt(Math.pow(pos[0] - end[0][0], 2) + Math.pow(pos[1] - end[0][1], 2));

        for (int i = 1; i < end.length; i++) {
            min = Math.min(min, Math.sqrt(Math.pow(pos[0] - end[i][0], 2) + Math.pow(pos[1] - end[i][1], 2)));
        }

        return min;
    };

    /**
     * Manhattan Heuristic Function
     */
    public static Function4<Function<int[], Boolean>, int[], Node, int[][], Double> hMH = (probe, pos, parent, end) -> {

        double min = Double.POSITIVE_INFINITY;

        for (int[]  endPos : end) {
            min = Math.min(min, Math.abs(pos[0] - endPos[0]) + Math.abs(pos[1] - endPos[1]));
        }

        return min;
    };

    /**
     * Manhattan Heuristic Function (Preferring equal decrease in x and y)
     */
    public static Function4<Function<int[], Boolean>, int[], Node, int[][], Double> hMHEq = (probe, pos, parent, end) -> {
       

        double ret = Double.POSITIVE_INFINITY;
        double curr;
        int xDiff, yDiff, max, min;
        for (int[] endPos : end) {
            xDiff = Math.abs(pos[0] - endPos[0]);
            yDiff = Math.abs(pos[1] - endPos[1]);
            curr = xDiff + yDiff;
            if (xDiff > yDiff) {
                max = xDiff;
                min = yDiff;
            } else {
                max = yDiff;
                min = xDiff;
            }
            if (max < 1.5*min || curr < 3.0) {
                curr -= 0.5;
            }

            ret = Math.min(ret, curr);
        }

        return ret;
    };

    /**
     * Class used to store a Manhattan hueristic function with pruning.
     */
    public static class MHBUG {
        int [] parity = new int[2];
        int[][] scanMin = new int[2][2];
        int[] scanDist = new int[2];
        

        public Function4<Function<int[], Boolean>, int[], Node, int[][], Double> h = (probe, pos, parent, end) -> {
            if (parent != null) {
                scanDist[0] = 0;
                scanDist[1] = 0;
                parity[0] = pos[0] - parent.pos[0];
                parity[1] = pos[1] - parent.pos[1];
                counterClockwise(parity, 2);
    
                for (int i = 0; i < 2; i++) {
                    scanMin[i][0] = pos[0];
                    scanMin[i][1] = pos[1];
                    for (int minDist = 1; minDist < 10; minDist++) {
                        scanMin[i][0] += parity[0];
                        scanMin[i][1] += parity[1];
                        if (probe.apply(scanMin[i])) {
                            scanDist[i] = minDist;
                            break;
                        }
                    }
                    parityInvert(parity);
                }
    
                
                
                if (scanDist[0] != 0 && scanDist[1] != 0) {
                    /*System.out.println("BARRIER START: [" + scanMin[0][0] + ", " + scanMin[0][1] + "]");
                    System.out.println("PARITY: [" + parity[0] + ", " + parity[1] + "]");
                    System.out.println("LOOKING FOR: ["  + scanMin[1][0] + ", " + scanMin[1][1] + "]");*/
                    int count = 0, max = (scanDist[0] + scanDist[1] - 2) * 8;
                    if (count < max) {
                        int tempQuad;
                        int[] windingNumbers = new int[end.length];
                        int[] currQuad = new int[end.length];
                        for (int endi = 0; endi < end.length; endi++) {
                            currQuad[endi] = getRelativeQuad(end[endi], pos);
                            tempQuad = getRelativeQuad(end[endi], scanMin[0]);
                            windingNumbers[endi] += getQuadJump(currQuad[endi], tempQuad);
                            currQuad[endi] = tempQuad;
                        }
        
                    
                        int rotations = 7;
                        boolean endFound = false;
                        while (count < max) {
                            parityInvert(parity);
                            rotations = 0;
                            
                            while (rotations < 7) {
                                counterClockwise(parity, 1);
                                
                                scanMin[0][0] += parity[0];
                                scanMin[0][1] += parity[1];
                                
                                if (probe.apply(scanMin[0])) {
                                    //System.out.println("NEXT BARRIER PIECE: [" + scanMin[0][0] + ", " + scanMin[0][1] + "]");
                                    for (int endi = 0; endi < end.length; endi++) {
                                        tempQuad = getRelativeQuad(end[endi], scanMin[0]);
                                        windingNumbers[endi] += getQuadJump(currQuad[endi], tempQuad);
                                        currQuad[endi] = tempQuad;
                                    }
            
                                    if (scanMin[0][0] == scanMin[1][0] && scanMin[0][1] == scanMin[1][1]) {
                                        //System.out.println("BARRIER COMPLETED");
                                        endFound = true;
                                    }
        
                                    break;
                                }
                                
                                
        
                                scanMin[0][0] -= parity[0];
                                scanMin[0][1] -= parity[1];
        
                                rotations++;
                            }
                            if (rotations == 7 || endFound) {
                                break;
                            }
                            count++;
                        }
                    
                        if (endFound) {
                        
                            for (int endi = 0; endi < end.length; endi++) {
                                tempQuad = getRelativeQuad(end[endi], pos);
                                windingNumbers[endi] += getQuadJump(currQuad[endi], tempQuad);
                                currQuad[endi] = tempQuad;
                            }
                            boolean usefulArea = false;
                            for (int wind : windingNumbers) {
                                if (wind != 0) {
                                    
                                    usefulArea = true;
                                    break;
                                }
                            }
                            if (!usefulArea) {
                                //System.out.println("PRUNED");
                                return Double.POSITIVE_INFINITY;
                            }
                        }
                    
                    }
                }
                    
                
    
    
    
            }
            
            double min = Double.POSITIVE_INFINITY;
            for (int[] endPos : end) {
                min = Math.min(min, Math.abs(pos[0] - endPos[0]) + Math.abs(pos[1] - endPos[1]));
            }
    
            return min;
        };
    }
    
     /**
     * Breadth-First Search Hueristic Function
     */
    public static Function4<Function<int[], Boolean>, int[], Node, int[][], Double> hBFS = (probe, pos, parent, end) -> {
        for (int[] endPos : end) {
            if (endPos[0] == pos[0] && endPos[1] == pos[1]) {
                return 0.0;
            }
        }

        return 1.0;
    };

     /**
     * Uniform Cost Hueristic Function (For Djikstra and Depth-First Iterative Deepening)
     */
    public static Function4<Function<int[], Boolean>, int[], Node, int[][], Double> hNULL = (probe, pos, parent, end) -> {
       return 0.0;
    };

    /**
     * 
     * @param centre The position to be treated as the origin.
     * @param arm The position for which we want to get the relative quadrant with regards to the centre.
     * @return The relative quadrant (labelled as 0 for non-negative x and y, going clockwise up to 3).
     */
    public static int getRelativeQuad (int [] centre, int[] arm) {
        if (arm[0] < centre[0]) {
            if (arm[1] < centre[1]) {
                return 2;
            } else {
                return 3;
            }  
        } else {
            if (arm[1] < centre[1]) {
                return 1;
            } else {
                return 0;
            }
        }
    }

    /**
     * 
     * @param prevQuad The previous relative quad between a centre and an arm.
     * @param nextQuad The relative quad between the same centre and a new arm.
     * @return The change in quadrants.
     */
    public static int getQuadJump (int prevQuad, int nextQuad) {
        int val = nextQuad - prevQuad;
        if (val == 0) {
            return 0;
        } else if (val == 1 || val == -3) {
            return 1;
        } else if (val == 2 || val == -2) {
            return val;
        } else {
            return -1;
        }
    }

    /**
     * Change a direction a number of 1/8 full rotations clockwise.
     * 
     * @param parity A position that represents one of the eight directions around any square (e.g. on top of the square is [0, 1])
     * @param amount The amount of 1/8 full rotations to turn clockwise.
     */
    public static void clockwise(int[] parity, int amount) {
        int prod;
        for (int i = 0; i < amount; i++) {
            prod = parity[0] * parity[1];
            if (prod == -1) {
                parity[0] = 0;
            } else if (prod == 1) {
                parity[1] = 0;
            } else if (parity[0] == 0) {
                parity[0] = parity[1];
            } else if (parity[1] == 0) {
                parity[1] = -parity[0];
            }
        }

    }

     /**
     * Change a direction a number of 1/8 full rotations counter-clockwise.
     * 
     * @param parity A position that represents one of the eight directions around any square (e.g. on top of the square is [0, 1])
     * @param amount The amount of 1/8 full rotations to turn counter-clockwise.
     */
    public static void counterClockwise(int[] parity, int amount) {
        int prod;
        for (int i = 0; i < amount; i++) {
            prod = parity[0] * parity[1];
            if (prod == -1) {
                parity[1] = 0;
            } else if (prod == 1) {
                parity[0] = 0;
            } else if (parity[0] == 0) {
                parity[0] = -parity[1];
            } else if (parity[1] == 0) {
                parity[1] = parity[0];
            }
        }
    }

    /**
     * Inverts a direction.
     * 
     * @param parity A position that represents one of the eight directions around any square (e.g. on top of the square is [0, 1])
     */
    public static void parityInvert(int[] parity) {
        parity[0] = -parity[0];
        parity[1] = -parity[1];
    }

    public static void main(String[] args) {
        int[][] start = {{31, 44}};
        int[][] end = {{44, 11}};
        boolean[][] board = new boolean[1200][1200];

        OpenSimplexNoise A = new OpenSimplexNoise(99l);
        
        Function<int[], Boolean> probeEasy = (pos) -> {
            if (Math.abs(A.eval(pos[0], pos[1])) < 0.6) {
                return false;
            }

            return true;
        };
        
        Function<int[], Boolean> probeMed = (pos) -> {
            if (pos[0] < 0 || pos[0] > 100 || pos[1] < 0 || pos[1] > 100) {
                return true;
            }

            if (Math.abs(A.eval(pos[0], pos[1])) < 0.4) {
                return false;
            }

            return true;
        };

        Function<int[], Boolean> probeDesigned = (pos) -> {
           if (pos[1] == 10) {
               if (pos[0] != 600) {
                   return true;
               }
           }

           if (pos[1] == 40) {
               if (pos[0] != 300) {
                   return true;
               }
           }

           if (pos[0] < 0 || pos[0] > 1200) {
               return true;
           }

           if (pos[1] < 0 || pos[1] > 1200) {
               return true;
           }

            return false;
        };

        Lattice2D test = new Lattice2D(probeMed, start, end);
        System.out.println("Bi: ");
        SearchResults biRes = test.solve(hMH, hMH, Lattice2D.SEARCH_TYPE_BDAS);
        biRes.genFolder("OUTPUT/BI");


        System.out.println("Mono: ");
        SearchResults monoRes = test.solve(hMH, null, Lattice2D.SEARCH_TYPE_AS);
        monoRes.genFolder("OUTPUT/MONO");

        System.out.println("IDA: ");
        SearchResults idaRes = test.solve((new MHBUG()).h, null, Lattice2D.SEARCH_TYPE_ASID);
        idaRes.genFolder("OUTPUT/IDA");

        /*System.out.println("IDA: ");
        SearchResults idaRes = test.solve(hMH, null, Lattice2D.SEARCH_TYPE_ASID);
        System.out.println(idaRes);
        idaRes.genFolder("OUTPUT/IDA");*/
        /*System.out.println("DFID: ");
        SearchResults dfidRes = test.solve(hMHBUG, null, Lattice2D.SEARCH_TYPE_DFID);
        System.out.println(dfidRes);
        idaRes.genFolder("OUTPUT/DFID");*/


    }

}