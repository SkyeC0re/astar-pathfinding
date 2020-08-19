import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.function.Function;

import javax.imageio.ImageIO;
import java.io.File;

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

    /**
     * A simple 3 parameter Function interface to use when creating hueristic lambda functions.
     */
    @FunctionalInterface
    public static interface Function3<One, Two, Three, Four> {
        public Four apply(One one, Two two, Three three);
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
        public ArrayList<Integer> leftExpanded = null;
        public ArrayList<Integer> rightExpanded = null;
        public int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        public BufferedImage img;


        public SearchResults(Function<int[], Boolean> probe, int[][] start, int[][] end, LinkedList<int[]> path, double pathLen, HashMap<Long, Node> leftClosed, HashMap<Long, Node> rightClosed,
                            ArrayList<Double> leftDepths, ArrayList<Double> rightDepths, ArrayList<Integer> leftExpanded, ArrayList<Integer> rightExpanded) {
            this.probe = probe;
            this.start = start;
            this.end = end;
            this.path = path;
            this.pathLen = pathLen;
            this.leftDepths = leftDepths;
            this.rightDepths = rightDepths;
            this.leftExpanded = leftExpanded;
            this.rightExpanded = rightExpanded;

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

            minX--;
            minY--;
            maxX++;
            maxY++;


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
            long l = (((long)pos[0]) << 32) | (pos[1] & 0xffffffffL);
            return l;
        }

    }

    //Search Types
    public static final int SEARCH_TYPE_DFID = 0;   //Depth First Iterative Deepening (Tree-Search)
    public static final int SEARCH_TYPE_ASID = 1;   //A* Iterative Deepening (Tree-Search)
    public static final int SEARCH_TYPE_AS = 2;     //A* (Graph-Search)
    public static final int SEARCH_TYPE_BDAS = 3;   //Bi-Directional A* (Graph-Search)

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
    public SearchResults solve(Function3<Function<int[], Boolean>, int[], int[][], Double> h1, Function3<Function<int[], Boolean>, int[], int[][], Double> h2, int searchType) {
        SearchResults ret;

        Queue<Node> leftOpen, rightOpen;
        double pathLen = Double.POSITIVE_INFINITY;
        int leftExpand = 0, rightExpand = 0;
        Node middleFromLeft = null, middleFromRight = null;
        HashMap<Long, Node> leftClosed = new HashMap<Long, Node>();
        HashMap<Long, Node> rightClosed = new HashMap<Long, Node>();
        ArrayList<Double> leftDepths = new ArrayList<Double>();
        ArrayList<Double> rightDepths = new ArrayList<Double>();
        ArrayList<Integer> leftExpanded = new ArrayList<Integer>();
        ArrayList<Integer> rightExpanded = new ArrayList<Integer>();

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
                leftOpen = new LIFOQueue<Node>();
            } else {
                leftOpen = new PriorityQueue<Node>();
            }

            while(run) {
                if (allEmpty) {
                
                    if (leftNextDepth == Double.POSITIVE_INFINITY) {
                        break;
                    }
    
                    if (pathLen < Double.POSITIVE_INFINITY) {
                       break;
                    }
    
                    allEmpty = false;
    
                    leftClosed.clear();
                    leftOpen.clear();

                    if (!rightClosed.isEmpty()) {
                        leftDepths.add(leftCurrDepth);
                        leftExpanded.add(leftExpand);
                    } 
                    
                    leftExpand = 0;
                    leftCurrDepth = leftNextDepth;
                    leftNextDepth = Double.POSITIVE_INFINITY;
                    
                    Node newNode;
                    for (int[] pos : start) {
                        if (!probe.apply(pos)) {
                            newNode = new Node(null, pos, 0, h1.apply(probe, pos, end));
                            leftOpen.add(newNode);
                            leftClosed.put(newNode.getLongPos(), newNode);
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
                            }
                        }
                        if (!run) {
                            break;
                        }
                    }
    
                    
                    
                }
                    
            
                if ((workingNode = leftOpen.poll()) == null) {
                    allEmpty = true;
                } else if ((checkNode = leftClosed.get(workingNode.getLongPos())) != null && workingNode == checkNode) {
                    leftClosed.remove(workingNode.getLongPos());
                    leftExpand++;
                    for (int[] nn : nodeNN(workingNode.pos, probe)) {
                        Node newNode = new Node(workingNode, nn, workingNode.gVal + 1, h1.apply(probe, nn, end));
                        
                        if ((checkNode = leftClosed.get(newNode.getLongPos())) == null || newNode.gVal < checkNode.gVal) {
                            if (checkNode != null) {
                                checkNode.parent = null;
                            }
                            if (newNode.gVal + newNode.hVal <= leftCurrDepth) {
                                if ((checkNode = rightClosed.get(newNode.getLongPos())) != null) {
                                    if (newNode.gVal < pathLen) {
                                        pathLen = newNode.gVal + checkNode.gVal;
                                        middleFromLeft = newNode;
                                        middleFromRight = checkNode;
                                        run = false;
                                        leftOpen.clear();
                                        break;
                                    }
                                } else {
                                    leftOpen.add(newNode);
                                    leftClosed.put(newNode.getLongPos(), newNode);
                                }
                            } else if (newNode.gVal + newNode.hVal < leftNextDepth) {
                                leftNextDepth = newNode.gVal + newNode.hVal;
                            }
                        }
                    }
                        

                }
                
               
            }

            leftDepths.add(leftCurrDepth);
            leftExpanded.add(leftExpand);
        
        // Graph Searches
        } else if (searchType == SEARCH_TYPE_AS || searchType == SEARCH_TYPE_BDAS) {
            leftOpen = new PriorityQueue<Node>();
            rightOpen = new PriorityQueue<Node>();

            Node newNode;
            for (int[] pos : start) {
                if (!probe.apply(pos)) {
                    newNode = new Node(null, pos, 0, h1.apply(probe, pos, end));
                    leftOpen.add(newNode);
                    leftClosed.put(newNode.getLongPos(), newNode);
                }
            }
            
            
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
                    if (searchType == SEARCH_TYPE_BDAS) {
                        rightOpen.add(newNode);
                    }
                    rightClosed.put(newNode.getLongPos(), newNode);
                }
            }
            allEmpty = false;
            boolean rightOnlyRefine = false, leftOnlyRefine = false;
            while(run && !allEmpty) {
            
                if ((workingNode = leftOpen.poll()) == null) {
                    if (pathLen == Double.POSITIVE_INFINITY) {
                        break;
                    }
                    allEmpty = true;
                } else if (workingNode.gVal + workingNode.hVal < pathLen) {
                    if ((checkNode = leftClosed.get(workingNode.getLongPos())) != null && workingNode == checkNode) {
                        leftExpand++;
                        for (int[] nn : nodeNN(workingNode.pos, probe)) {
                            newNode = new Node(workingNode, nn, workingNode.gVal + 1, h1.apply(probe, nn, end));
                            
                            if (newNode.gVal + newNode.hVal < pathLen && (((checkNode = leftClosed.get(newNode.getLongPos())) != null && newNode.gVal < checkNode.gVal) || (checkNode == null && !leftOnlyRefine))) {
                                if (checkNode != null) {
                                    checkNode.parent = null;
                                }
                                if ((checkNode = rightClosed.get(newNode.getLongPos())) != null) {
                                    if (newNode.gVal + checkNode.gVal < pathLen) {
                                        pathLen = newNode.gVal + checkNode.gVal;
                                        middleFromLeft = newNode;
                                        middleFromRight = checkNode;
                                        rightOnlyRefine = true;
                                    }
                                } else {
                                    leftOpen.add(newNode);
                                    leftClosed.put(newNode.getLongPos(), newNode);
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
                            rightExpand++;
                            for (int[] nn : nodeNN(workingNode.pos, probe)) {
                                newNode = new Node(workingNode, nn, workingNode.gVal + 1, h2.apply(probe, nn, start));
                                
                                if (newNode.gVal + newNode.hVal < pathLen && (((checkNode = rightClosed.get(newNode.getLongPos())) != null && newNode.gVal < checkNode.gVal) || (checkNode == null && !rightOnlyRefine))) {
                                    if (checkNode != null) {
                                        checkNode.parent = null;
                                    }
                                    if ((checkNode = leftClosed.get(newNode.getLongPos())) != null) {
                                        if (newNode.gVal + checkNode.gVal < pathLen) {
                                            pathLen = newNode.gVal + checkNode.gVal;
                                            middleFromLeft = checkNode;
                                            middleFromRight = newNode;
                                            rightOnlyRefine = true;
                                        }
                                    } else {
                                        rightOpen.add(newNode);
                                        rightClosed.put(newNode.getLongPos(), newNode);
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

            leftDepths.add(Double.POSITIVE_INFINITY);
            leftExpanded.add(leftExpand);
            if (searchType == SEARCH_TYPE_BDAS) {
                rightDepths.add(Double.POSITIVE_INFINITY);
                rightExpanded.add(rightExpand);
            }
        }

        
        Instant finishTime = Instant.now();

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

        ret = new SearchResults(probe, start, end, path, pathLen, leftClosed, rightClosed, leftDepths, rightDepths, leftExpanded, rightExpanded);
        //System.out.println("Time ms: " + Duration.between(startTime, finishTime).toMillis());
        //System.out.println("Expanded: " + (leftExpand + rightExpand));
        return ret;
    }

    public static Function3<Function<int[], Boolean>, int[], int[][], Double> hSLD = (probe, pos, end) -> {
        if (end.length < 1) {
            return Double.POSITIVE_INFINITY;
        }

        double min = Math.sqrt(Math.pow(pos[0] - end[0][0], 2) + Math.pow(pos[1] - end[0][1], 2));

        for (int i = 1; i < end.length; i++) {
            min = Math.min(min, Math.sqrt(Math.pow(pos[0] - end[i][0], 2) + Math.pow(pos[1] - end[i][1], 2)));
        }

        return min;
    };

    public static Function3<Function<int[], Boolean>, int[], int[][], Double> hMH = (probe, pos, end) -> {
        if (end.length < 1) {
            return Double.POSITIVE_INFINITY;
        }

        double min = Math.abs(pos[0] - end[0][0]) + Math.abs(pos[1] - end[0][1]);

        for (int i = 1; i < end.length; i++) {
            min = Math.min(min, Math.abs(pos[0] - end[i][0]) + Math.abs(pos[1] - end[i][1]));
        }

        return min;
    };

    public static Function3<Function<int[], Boolean>, int[], int[][], Double> hBFS = (probe, pos, end) -> {
        for (int[] endPos : end) {
            if (endPos[0] == pos[0] && endPos[1] == pos[1]) {
                return 0.0;
            }
        }

        return 1.0;
    };


    public static void main(String[] args) {
        int[][] start = {{0, 0}};
        int[][] end = {{1000, 1000}};
        boolean[][] board = new boolean[1200][1200];
        for (boolean[] row : board) {
            for (int col = 0; col < row.length; col++) {
                row[col] = false;
            }
        }

        OpenSimplexNoise A = new OpenSimplexNoise(99l);
        
        Function<int[], Boolean> probeEasy = (pos) -> {
            if (Math.abs(A.eval(pos[0], pos[1])) < 0.6) {
                return false;
            }

            return true;
        };
        
        Function<int[], Boolean> probeMed = (pos) -> {
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
       
        System.out.println("Mono: ");
        SearchResults monoRes = test.solve(hMH, null, Lattice2D.SEARCH_TYPE_AS);
        System.out.println(monoRes);
        monoRes.genImage("MONO");
        System.out.println("Bi: ");
        SearchResults biRes = test.solve(hMH, hMH, Lattice2D.SEARCH_TYPE_BDAS);
        System.out.println(biRes);
        biRes.genImage("BI");
    }

}