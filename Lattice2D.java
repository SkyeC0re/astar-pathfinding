import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.TreeSet;
import java.util.function.Function;

import javax.imageio.ImageIO;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
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
 

    //Visualization Colors
    private static final int COLOR_OBS = 0xff03071e;
    private static final int COLOR_EMPTY = 0xffffffff;
    private static final int COLOR_LEFT = 0xff118ab2;
    private static final int COLOR_RIGHT = 0xff06d6a0;
    private static final int COLOR_ROAD = 0xffd00000;

    public static long getLongPos(int[] pos) {
        return (long) (((long)pos[0]) << 32) | (pos[1] & 0xffffffffL);
    }

    /**
     * A simple 5 parameter Function interface to use when creating hueristic lambda functions.
     */
    @FunctionalInterface
    public static interface Function5<One, Two, Three, Four, Five, Six> {
        public Six apply(One one, Two two, Three three, Four four, Five five);
    }

    
    /**
     * Straight Line Hueristic Function
     */
    public static Function5<Function<int[], Boolean>, int[], Node, int[][], int[][], Double> hSLD = (probe, pos, parent, start, end) -> {
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
    public static Function5<Function<int[], Boolean>, int[], Node, int[][], int[][], Double> hMH = (probe, pos, parent, start, end) -> {

        double min = Double.POSITIVE_INFINITY;

        for (int[]  endPos : end) {
            min = Math.min(min, Math.abs(pos[0] - endPos[0]) + Math.abs(pos[1] - endPos[1]));
        }

        return min;
    };

    /**
     * Manhattan Heuristic Function (Preferring equal decrease in x and y). Not intended for use with multiple start and endpoints (use either but not both).
     */
    public static Function5<Function<int[], Boolean>, int[], Node, int[][], int[][], Double> hMHEq = (probe, pos, parent, start, end) -> {
       

        double ret = Double.POSITIVE_INFINITY;
        double curr, ratio;

        for (int[] startPos : start) {
            for (int[] endPos : end) {
                ratio = Math.pow(startPos[0] - endPos[0], 2) + Math.pow(startPos[1] - endPos[1], 2);
                
                if (ratio == 0.0) {
                    return 0.0;
                }
                ratio /= Math.pow(pos[0] - endPos[0], 2) + Math.pow(pos[1] - endPos[1], 2) + Math.pow(startPos[0] - pos[0], 2) + Math.pow(startPos[1] - pos[1], 2);
                curr =  Math.abs(pos[0] - endPos[0]) + Math.abs(pos[1] - endPos[1]) - 0.5 * ratio;

                ret = Math.min(ret, curr);
            }
        }

        return ret;
    };

    /**
     * A modified Manhattan Distance Function that attempts to evade nooks and crannies.
     */
    public static Function5<Function<int[], Boolean>, int[], Node, int[][], int[][], Double> hMHNook = (probe, pos, parent, start, end) -> {
            
        if (parent != null) {
            int[][] scanMin = new int[2][2];
            int[] ppos = parent.pos;
            int[] parity = pos.clone();
            parity[0] -= ppos[0];
            parity[1] -= ppos[1];
            
            /*int[] lookout = pos.clone();
            for (int i = 0; i < 5; i++) {
                lookout[0] += parity[0];
                lookout[1] += parity[1];

                if (probe.apply(lookout)) {
                    double min = Double.POSITIVE_INFINITY;
                    for (int[] endPos : end) {
                        min = Math.min(min, Math.abs(pos[0] - endPos[0]) + Math.abs(pos[1] - endPos[1]));
                    }

                    return min;
                }
            }*/

            int scanCount = 0, scanSum = 0;
            counterClockwise(parity, 2);

            for (int i = 0; i < 2; i++) {
                scanMin[i][0] = ppos[0];
                scanMin[i][1] = ppos[1];
                for (; scanSum < 15; scanSum++) {
                    scanMin[i][0] += parity[0];
                    scanMin[i][1] += parity[1];
                    if (probe.apply(scanMin[i])) {
                        scanCount++;
                        break;
                    }
                }
                parityInvert(parity);
            }

            
            
            if (scanCount == 2) {
                int count = 0, max = (scanSum) * 6;
                if (count < max) {
                    int tempQuad;
                    int[] windingNumbers = new int[end.length];
                    int[] currQuad = new int[end.length];
                    for (int endi = 0; endi < end.length; endi++) {
                        currQuad[endi] = getRelativeQuad(end[endi], ppos);
                        tempQuad = getRelativeQuad(end[endi], scanMin[0]);
                        windingNumbers[endi] += getQuadJump(currQuad[endi], tempQuad);
                        currQuad[endi] = tempQuad;
                    }
                    int rotations = 7;
                    boolean endFound = false;
                    int[] backupParity = parity.clone();
                    int angleChange = 0;
                    while (count < max) {
                        parityInvert(parity);
                        angleChange += 4;
                        rotations = 0;
                        
                        while (rotations < 7) {
                            counterClockwise(parity, 1);
                            angleChange -= 1;
                            scanMin[0][0] += parity[0];
                            scanMin[0][1] += parity[1];
                            
                            if (probe.apply(scanMin[0])) {
                                for (int endi = 0; endi < end.length; endi++) {
                                    tempQuad = getRelativeQuad(end[endi], scanMin[0]);
                                    windingNumbers[endi] += getQuadJump(currQuad[endi], tempQuad);
                                    currQuad[endi] = tempQuad;
                                }

                                if (scanMin[0][0] == scanMin[1][0] && scanMin[0][1] == scanMin[1][1]) {
                                    parityInvert(parity);
                                    angleChange += 4;

                                    while (parity[0] != backupParity[0] || parity[1] != backupParity[1]) {
                                        counterClockwise(parity, 1);
                                        angleChange -= 1;
                                    }

                                    if (angleChange == 8) {
                                        endFound = true;
                                    }
                                    
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
                            tempQuad = getRelativeQuad(end[endi], ppos);
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

    
     /**
     * Breadth-First Search Hueristic Function
     */
    public static Function5<Function<int[], Boolean>, int[], Node, int[][], int[][], Double> hBFS = (probe, pos, parent, start, end) -> {
        for (int[] endPos : end) {
            if (endPos[0] == pos[0] && endPos[1] == pos[1]) {
                return 0.0;
            }
        }

        return 1.0;
    };

     /**
     * Uniform Cost Hueristic Function (For Dijkstra and Depth-First Iterative Deepening)
     */
    public static Function5<Function<int[], Boolean>, int[], Node, int[][], int[][], Double> hNULL = (probe, pos, parent, start, end) -> {
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

            /*if (path != null) {
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
            }*/

            int [] pos;
            /*for (Node n : leftClosed.values()) {
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
            }*/

            int diffX = maxX - minX;
            int diffY = maxY - minY;

            minX -= diffX + 5;
            maxX += diffX + 5;
            minY -= diffY + 5;
            maxY += diffY + 5;

            diffX = maxX - minX + 1;
            diffY = maxY - minY + 1;

            try {
                int scale = 1, xadd, yadd;
                while (diffX * scale < 1000 && diffY * scale < 1000) {
                    scale <<= 1;
                }
                BufferedImage img = new BufferedImage(diffX * scale, diffY * scale, BufferedImage.TYPE_INT_RGB);
                pos = new int[2];
               
                int color;
                for (int y = 0; y < diffY; y++) {
                    pos[1] = minY + y;
                    for (int x = 0; x < diffX; x++) {
                        pos[0] = minX + x;
                        
                        if (probe.apply(pos)) {
                            color = COLOR_OBS;
                        } else if (leftClosed.containsKey(getLongPos(pos))) {
                            color = COLOR_LEFT;
                        } else if (rightClosed.containsKey(getLongPos(pos))) {
                            color = COLOR_RIGHT;
                        } else {
                            color = COLOR_EMPTY;
                        }

                       
                        for (yadd = 0; yadd < scale; yadd++) {
                            for (xadd = 0; xadd < scale; xadd++) {
                                img.setRGB(scale * x + xadd, scale * y + yadd, color);
                            }     
                        }
                        
                        
                    }
                }

                    if (path != null) {
                        int x, y;
                        for (int[] pathPos : path) {
                            try {
                                x = pathPos[0] - minX;
                                y = pathPos[1] - minY;
                                for (yadd = 0; yadd < scale; yadd++) {
                                    for (xadd = 0; xadd < scale; xadd++) {
                                        img.setRGB(scale * x + xadd, scale * y + yadd, COLOR_ROAD);
                                    }     
                                }
                            } catch (Exception ex) {}
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
                ImageIO.write(img, "png",  new File(fname + ".png"));
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
                String[] path = folderName.split("/");
                String name = path[path.length - 1];
                genCSV(folderName + "/" + name);
                genImage(folderName + "/" + name);
            } catch (Exception ex) {}
        }

    }


    

    /**
    * A Data structure used to represent positions in the search space, with respect to their positions, h(n) and g(n) values, as well as their parent position.
    */
    public static class Node implements Comparable<Node>{
        public Node parent;
        public int[] pos;
        public double gVal, hVal;
        public long id;

        /**
         * 
         * @param parent The parent node.
         * @param pos   The position of this node.
         * @param gVal  The travelled path length to get to the current position.
         * @param hVal  The hueristic value of this node.
         * @param id    A unique ID for the node. This is for comparison purposes.
         */
        public Node(Node parent, int[] pos, double gVal, double hVal, long id) {
            this.parent = parent;
            this.pos = pos;
            this.gVal = gVal;
            this.hVal = hVal;
            this.id = id;
        }

        @Override
        public int hashCode() {
            int hash = 17;
            hash = ((hash + pos[0]) << 5) - (hash + pos[0]);
            hash = ((hash + pos[1]) << 5) - (hash + pos[1]);
            return hash;
        }

        /**
         * Nodes are compared firstly by their f values, then their h values and finally their IDs.
         */
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

            if (this.id < o.id) {
                return -1;
            }

            if (this.id > o.id) {
                return 1;
            }
            

            return 0;
        }

        /**
         * @return Returns the node's position of two integers as a single long value. 
         */
        public long getLongPos() {
            return (long) (((long)pos[0]) << 32) | (pos[1] & 0xffffffffL);
        }

    }

   

    private int[][] start;
    private int[][] end;
    private Function<int[], Boolean> probe;
    private HashMap<Long, Node> leftClosed;
    private HashMap<Long, Node> rightClosed;
    private boolean leftOnlyRefine, rightOnlyRefine;
    private double pathLen;
    private Function5<Function<int[], Boolean>, int[], Node, int[][], int[][], Double> h1;
    private Function5<Function<int[], Boolean>, int[], Node, int[][], int[][], Double> h2;
    private long rollingID;
    private Node middleFromLeft, middleFromRight;

    /**
     * Initializes the Lattice using a file that represents the maze. The file must contain the character '0' for empty spaces,
     * and the maze need not be a square.
     * 
     * @param fname The name of the board file.
     * @param start A two dimensional array containing any number of start points as [x, y]. Note top left of the board is [0, 0]
     * @param end   A two dimensional array containing any number of end points as [x, y]. Note top left of the board is [0, 0]
     */
    public Lattice2D(String fname, int[][] start, int[][] end) {
        try {
            BufferedReader in = new BufferedReader(new FileReader(fname));
            String line;
            ArrayList<boolean[]> arrBoard = new ArrayList<boolean[]>();
            
            while((line = in.readLine()) != null) {
                boolean[] row = new boolean[line.length()];
                for (int i = 0; i < row.length; i++) {
                    if (line.charAt(i) != '0') {
                        row[i] = true;
                    }
                }
                arrBoard.add(row);   
            }
            in.close();

            Function<int[], Boolean> probe = (pos) -> {
                if (pos[1] < 0 || pos[1] >= arrBoard.size()) {
                    return true;
                }
    
                if (pos[0] < 0 || pos[0] >= arrBoard.get(pos[1]).length) {
                    return true;
                }
                return arrBoard.get(pos[1])[pos[0]];
            };
            
            
            initLattice(probe, start, end);

        } catch (Exception ex) {
            System.out.println("Error: Board File Could not be read in successfully, reverting to a completely blocked board.");
            Function<int[], Boolean> probe = (pos) -> {
                return true;
            };
            initLattice(probe , start, end);
        }
    }

    /**
     Initializes the Lattice using 2 Dimensional boolean array.

     * @param board The two dimensional boolean array representing the lattice. False is empty and True is occupied.
     * @param start A two dimensional array containing any number of start points as [x, y]. Note top left of the board is [0, 0]
     * @param end A two dimensional array containing any number of end points as [x, y]. Note top left of the board is [0, 0]
     */
    public Lattice2D(boolean[][] board, int[][] start, int[][] end) {
        Function<int[], Boolean> probe = (pos) -> {
            if (pos[1] < 0 || pos[1] >= board.length) {
                return true;
            }

            if (pos[0] < 0 || pos[0] >= board[pos[1]].length) {
                return true;
            }
            return board[pos[1]][pos[0]];
        };
       initLattice(probe, start, end);
    }

    public Lattice2D (Function<int[], Boolean> probe, int[][] start, int[][] end) {
        initLattice(probe, start, end);
    }
    
    /**
     * Initializes the Lattice programmatically.
     * 
     * @param probe A lambda function which takes a position [x, y] and returns True of False depending on whether the lattice is empty or occupied at that space.
     *              False refers to empty space.
     * @param start A two dimensional array containing any number of start points as [x, y].
     * @param end   A two dimensional array containing any number of end points as [x, y].
     */
    private void initLattice (Function<int[], Boolean> probe, int[][] start, int[][] end) {
        this.probe = probe;

        System.out.println("Scrubbing Start and End Points:");
        int validCount = 0;
        for (int[] pos : start) {
            if (probe.apply(pos)) {
               System.out.println("Obstacle detected on [" + pos[0] + ", " + pos[1] + "]. Removing start location.");
            } else {
                validCount++;
            }
        }

        if (validCount != start.length) {
            int[][] newStart = new int[validCount][2];
            int i = 0;
            for (int[] pos : start) {
                if (!probe.apply(pos)) {
                    newStart[i][0] = pos[0];
                    newStart[i][1] = pos[1];
                    i++;
                }
            }
            this.start = newStart;
        } else {
            this.start = start;
        }
        
        validCount = 0;
        for (int[] pos : end) {
            if (probe.apply(pos)) {
               System.out.println("Obstacle detected on [" + pos[0] + ", " + pos[1] + "]. Removing end location.");
            } else {
                validCount++;
            }
        }

        if (validCount != end.length) {
            int[][] newEnd = new int[validCount][2];
            int i = 0;
            for (int[] pos : end) {
                if (!probe.apply(pos)) {
                    newEnd[i][0] = pos[0];
                    newEnd[i][1] = pos[1];
                    i++;
                }
            }
            this.end = newEnd;
        } else {
            this.end = end;
        }
        
        System.out.println("Scrubbing Completed.\n");
        
    }



    /**
     * Generates all the valid neighbours of a Node.
     * 
     * @param node The node to expand.
     * @param fromLeft Tells the function whether we are search from the forward (left) or backward (right) frontier.
     */
    private LinkedList<Node> genValidNeighbours(Node node, boolean fromLeft) {
        
        LinkedList<Node> neighbours = new LinkedList<Node>();
        int[] parentPos = null;
        double tempH = node.gVal + 1.0;
        int[] pos;
        if (node.parent != null) {
            parentPos = node.parent.pos;
        } else {
            parentPos = null;   
        }
        for (int posDim = 0; posDim < 2; posDim++) {
            for (int add = 1; add >= -1; add -= 2) {
                pos = node.pos.clone();
                pos[posDim] += add;
                
                //Skip if the space is occupied
                if (probe.apply(pos)) {
                    continue;
                }

                //Skip if we are trying to move into the node's parent position
                if (parentPos != null) {
                    if (pos[0] == parentPos[0] && pos[1] == parentPos[1]) {
                        continue;
                    }
                }
                
                //Calculate h values
                if (fromLeft) {
                    tempH = h1.apply(probe, pos, node, start, end);
                } else {
                    tempH = h2.apply(probe, pos, node, end, start);
                }
                
                //Add child node
                if (tempH != Double.POSITIVE_INFINITY) {
                    neighbours.add(new Node(node, pos, node.gVal + 1.0, tempH, rollingID++));
                }
                
                    
                
            }
        }
        return neighbours;

    }

    /**
     * Searches the lattice with a specific search method and hueristic(s).
     *
     * @param h1 the primary hueristic lambda function.
     * @param h2 the secondary hueristic lambda function (used for the backwards search in Bi-Directional A*)
     * @param searchType the search type to use, see class constants.
     * @return a SearchResults data structure containing all the pertinent information regarding the search.
     */
    public SearchResults solve(Function5<Function<int[], Boolean>, int[], Node, int[][], int[][], Double> h1, Function5<Function<int[], Boolean>, int[], Node, int[][], int[][], Double> h2, int searchType) {
        
        this.h1 = h1;
        this.h2 = h2;
        rollingID = 0;
        pathLen = Double.POSITIVE_INFINITY;
        long leftExplore = 0, rightExplore = 0;
        middleFromLeft = null;
        middleFromRight = null;
        leftClosed = new HashMap<Long, Node>();
        rightClosed = new HashMap<Long, Node>();
        leftOnlyRefine = false;
        rightOnlyRefine = false;
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

        //Iterative Deepening Searches
        if (run && (searchType == SEARCH_TYPE_DFID || searchType == SEARCH_TYPE_ASID)) {
            double leftCurrDepth = -1.0, leftNextDepth;
            leftNextDepth = 0.0;
            LinkedList<Node> leftOpen = new LinkedList<Node>();
            if (searchType == SEARCH_TYPE_DFID) {
                System.out.println("Running Depth First Iterative Deepening Search:");
                this.h1 = (h1 = hNULL);
            } else {
                System.out.println("Running A* Iterative Deepening Search:");
            }
            Instant depthStart = (startTime = Instant.now());
            while(run) {
                if (allEmpty) {
                    if (leftNextDepth == Double.POSITIVE_INFINITY) {
                        break;
                    }
                    allEmpty = false;

                    //Display information after a depth is fully searched
                    if (!rightClosed.isEmpty()) {
                        leftDepths.add(leftCurrDepth);
                        leftExplored.add(leftExplore);
                        long millTime = Duration.between(depthStart, Instant.now()).toMillis();
                        timeTaken.add(Duration.between(depthStart, Instant.now()).toMillis());
                        System.out.println("Depth: " + leftCurrDepth + " || Explored: " + leftExplore + " || Time(ms): " + millTime);
                    }

                    leftOpen.clear();
                    leftClosed.clear();

                    leftExplore = 0;
                    leftCurrDepth = leftNextDepth;
                    leftNextDepth = Double.POSITIVE_INFINITY;

                    if (start.length == 0 || end.length == 0) {
                        break;
                    }
                    
                    //Add Start Points.
                    Node newNode;
                    for (int[] pos : start) {
                        newNode = new Node(null, pos, 0, h1.apply(probe, pos, null, start, end), rollingID++);
                        leftOpen.add(newNode);
                        leftClosed.put(newNode.getLongPos(), newNode);
                    }
                    
                    //Initially, add all the End Points to the right explored set (This is never updated, but merely used to check if an end point is reached).
                    if (rightClosed.isEmpty()) {
                        for (int[] pos : end) {
                            newNode = new Node(null, pos, 0, 0, rollingID++);
                            if ((checkNode = leftClosed.get(newNode.getLongPos())) != null) {
                                middleFromLeft = checkNode;
                                middleFromRight = newNode;
                                pathLen = 0.0;
                                run = false;
                                break;
                            }
                            rightClosed.put(newNode.getLongPos(), newNode);
                        }
                        if (!run) {
                            break;
                        }
                    }
    
                    depthStart = Instant.now();
                    
                }
                    
            
                if ((workingNode = leftOpen.pollLast()) == null) {
                    allEmpty = true;
                } else if ((checkNode = leftClosed.get(workingNode.getLongPos())) != null && workingNode == checkNode) {
                    leftClosed.remove(workingNode.getLongPos());
                    leftExplore++;
                    for (Node newNode : genValidNeighbours(workingNode, true)) {
                        if (newNode.gVal + Math.ceil(newNode.hVal) <= leftCurrDepth) {
                            //Check if a the position is an end point.
                            if ((checkNode = rightClosed.get(newNode.getLongPos())) != null) {
                                pathLen = newNode.gVal;
                                middleFromLeft = newNode;
                                middleFromRight = checkNode;
                                run = false;
                                leftOpen.clear();
                                break;
                            //Check if we should re-expand a node or not.
                            } else if (((checkNode = leftClosed.get(newNode.getLongPos())) == null) || (newNode.gVal < checkNode.gVal)) {
                                
                                leftOpen.add(newNode);
                                leftClosed.put(newNode.getLongPos(), newNode);
                            }
                        //Find the next minimum integer depth.
                        } else if (newNode.gVal + Math.ceil(newNode.hVal) < leftNextDepth) {
                           
                            leftNextDepth = newNode.gVal + Math.ceil(newNode.hVal);
                        }
                    }
                        

                }
                
               
            }

            timeTaken.add(Duration.between(depthStart, Instant.now()).toMillis());
            leftDepths.add(leftCurrDepth);
            leftExplored.add(leftExplore);
        
        // Tree-Graph Searches
        } else if (run && (searchType == SEARCH_TYPE_AS || searchType == SEARCH_TYPE_BDAS)) {
            if (searchType == SEARCH_TYPE_AS) {
                System.out.println("Running A*:");
            } else {
                System.out.println("Running Bi-Directional A*:");
            }
            TreeSet<Node> leftOpen = new TreeSet<Node>();
            TreeSet<Node> rightOpen = new TreeSet<Node>();

            startTime = Instant.now();
            if (start.length == 0 || end.length == 0) {
                run = false;
            } else {
                //Add Start Positions.
                for (int[] pos : start) {
                    Node newNode = new Node(null, pos, 0, h1.apply(probe, pos, null , start, end), rollingID++);
                    leftOpen.add(newNode);
                    leftClosed.put(newNode.getLongPos(), newNode);
                }
                //Add End Positions.
                for (int[] pos : end) {
                    Node newNode;
                    if (searchType == SEARCH_TYPE_BDAS) {
                        newNode = new Node(null, pos, 0, h2.apply(probe, pos, null, end, start), rollingID++);
                    } else {
                        newNode = new Node(null, pos, 0, 0, rollingID++);
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
                }
            }
            allEmpty = false;
            while(run && !allEmpty) {
            
                //Forward searching (left) frontier
                if ((workingNode = leftOpen.pollFirst()) == null) {
                    //If this Open set is empty and we have found no path yet, no path exists.
                    if (pathLen == Double.POSITIVE_INFINITY) {
                        break;
                    }
                    allEmpty = true;
                } else if (workingNode.gVal + Math.ceil(workingNode.hVal) < pathLen) {
                    
                    if ((checkNode = leftClosed.get(workingNode.getLongPos())) != null && workingNode == checkNode) {
                        leftExplore++;
                        for (Node newNode : genValidNeighbours(workingNode, true)) {
                            //Check if the optimal path length can be updated.  
                            if (newNode.gVal + Math.ceil(newNode.hVal) < pathLen) {
                                //Check if the current position is an End Point.
                                if ((checkNode = rightClosed.get(newNode.getLongPos())) != null) {
                                    if (newNode.gVal + checkNode.gVal < pathLen) {
                                        pathLen = newNode.gVal + checkNode.gVal;
                                        middleFromLeft = newNode;
                                        middleFromRight = checkNode;
                                        rightOnlyRefine = true;
                                    }
                                } else if (!((checkNode = leftClosed.get(newNode.getLongPos())) == null && leftOnlyRefine)) {
                                    //If the current position was already explored, see if it can be improved.
                                    if (checkNode != null) {
                                        if (newNode.gVal < checkNode.gVal) {
                                            leftOpen.remove(checkNode);
                                        } else {
                                            continue;
                                        }
                                    }
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
                
                //Backward searching (right) frontier.
                if (searchType == SEARCH_TYPE_BDAS) {
                    if ((workingNode = rightOpen.pollFirst()) == null) {
                        //If this Open set is empty and we have found no path yet, no path exists.
                        if (pathLen == Double.POSITIVE_INFINITY) {
                            break;
                        }
                    } else if (workingNode.gVal + Math.ceil(workingNode.hVal) < pathLen) {

                        if ((checkNode = rightClosed.get(workingNode.getLongPos())) != null && workingNode == checkNode) {
                            rightExplore++;    
                            for (Node newNode : genValidNeighbours(workingNode, false)) {
                                //Check if the optimal path length can be updated.
                                if (newNode.gVal + Math.ceil(newNode.hVal) < pathLen) {
                                    //Check if the current position is a Start Point.
                                    if ((checkNode = leftClosed.get(newNode.getLongPos())) != null) {
                                        if (newNode.gVal + checkNode.gVal < pathLen) {
                                            pathLen = newNode.gVal + checkNode.gVal;
                                            middleFromLeft = checkNode;
                                            middleFromRight = newNode;
                                            rightOnlyRefine = true;
                                        }
                                    } else if (!((checkNode = rightClosed.get(newNode.getLongPos())) == null && rightOnlyRefine)) {
                                        //If the current position was already explored, see if it can be improved.
                                        if (checkNode != null) {
                                            if (newNode.gVal < checkNode.gVal) {
                                                rightOpen.remove(checkNode);
                                            } else {
                                                continue;
                                            }
                                        }
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
            Instant finishTime = Instant.now();
            timeTaken.add(Duration.between(startTime, finishTime).toMillis());

            leftDepths.add(Double.POSITIVE_INFINITY);
            leftExplored.add(leftExplore);
            if (searchType == SEARCH_TYPE_BDAS) {
                rightDepths.add(Double.POSITIVE_INFINITY);
                rightExplored.add(rightExplore);
            }
        }

        
        
        //Generate the optimal path if it exists.
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

        //Calculate Results and return
        SearchResults ret = new SearchResults(probe, start, end, path, pathLen, leftClosed, rightClosed, leftDepths, rightDepths, leftExplored, rightExplored, timeTaken);
        System.out.println("Search Completed: Optimal Path Length: " + Double.toString(pathLen) + " || Total Nodes Explored: " + ret.totalExplore + " || Time(ms): " + ret.totalTime + "\n");
        this.h1 = null;
        this.h2 = null;
        this.leftClosed = null;
        this.rightClosed = null;
        return ret;
    }



}