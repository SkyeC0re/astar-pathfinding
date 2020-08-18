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

//import OpenSimplexNoise;

public class Lattice2D {
    @FunctionalInterface
    public static interface Function3<One, Two, Three, Four> {
        public Four apply(One one, Two two, Three three);
    }

    public static class LIFOQueue<E> extends LinkedList<E> {
        @Override
        public E poll() {
            return super.pollLast();
        }
    }

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

        //System.out.println("POS SIZE: " + nn.size());
        return nn;

    }

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

    }

    public static final int LIST_TYPE_PRIORITY = 0;
    public static final int LIST_TYPE_STACK = 1;
    public static final int LIST_TYPE_QUEUE = 2;

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

    public double solve(Function3<Function<int[], Boolean>, int[], int[][], Double> h1, Function3<Function<int[], Boolean>, int[], int[][], Double> h2, boolean isIterative, int openListType) {
        Queue<Node> leftOpen, rightOpen;
        double pathLen = Double.POSITIVE_INFINITY;
        int leftExpand = 0, rightExpand = 0;
        Node middleFromLeft = null, middleFromRight = null;
        double leftCurrDepth = 0.0, leftNextDepth;
        double rightCurrDepth = 0.0, rightNextDepth;

        if (isIterative) {
            leftNextDepth = 0.0;
            rightNextDepth = 0.0;
        } else {
            leftNextDepth = Double.POSITIVE_INFINITY;
            if (h2 == null) {
                rightNextDepth = 0.0;
            } else {
                rightNextDepth = Double.POSITIVE_INFINITY;
            }
        }

        HashMap<Node, Node> leftClosed = new HashMap<Node, Node>();
        HashMap<Node, Node> rightClosed = new HashMap<Node, Node>();

        switch (openListType) {
            case LIST_TYPE_PRIORITY:
                leftOpen = new PriorityQueue<Node>();
                rightOpen = new PriorityQueue<Node>();
                break;
            case LIST_TYPE_QUEUE:
                leftOpen = new LinkedList<Node>();
                rightOpen = new LinkedList<Node>();
                
                break;
            case LIST_TYPE_STACK:
                leftOpen = new LIFOQueue<Node>();
                rightOpen = new LIFOQueue<Node>();
                break;
            default:
                leftOpen = new PriorityQueue<Node>();
                rightOpen = new PriorityQueue<Node>();

            
        }

       

        Node workingNode;
        Node checkNode;
        boolean allEmpty = true;
        Instant startTime = Instant.now();

        while(true) {

            if (allEmpty) {
                allEmpty = false;
                if (leftCurrDepth == Double.POSITIVE_INFINITY || rightCurrDepth == Double.POSITIVE_INFINITY) {
                    System.out.println("All Empty Break");
                    break;
                }

                leftClosed.clear();
                leftOpen.clear();
                leftCurrDepth = leftNextDepth;
                leftNextDepth = Double.POSITIVE_INFINITY;
                
                rightClosed.clear();
                rightOpen.clear();
                rightCurrDepth = rightNextDepth;
                if (h2 == null) {
                    rightNextDepth = 0.0;
                } else {
                    rightNextDepth = Double.POSITIVE_INFINITY;
                }
                
                
                for (int[] pos : start) {
                    if (!probe.apply(pos)) {
                        leftOpen.add(new Node(null, pos, 0, h1.apply(probe, pos, end)));
                    }
                }
                
                Node newNode;
                for (int[] pos : end) {
                    if (!probe.apply(pos)) {
                        if (h2 == null) {
                            newNode = new Node(null, pos, 0, 0);
                            rightClosed.put(newNode, newNode);
                        } else {
                            rightOpen.add(new Node(null, pos, 0, h2.apply(probe, pos, start)));
                        }
                    }
                }
                
            }
                
        
            if ((workingNode = leftOpen.poll()) == null) {
                if (leftNextDepth == Double.POSITIVE_INFINITY) {
                    System.out.println("LEFT BREAK " + leftOpen.size());
                    break;
                }
                allEmpty = true;
            } else if (workingNode.gVal + workingNode.hVal < pathLen) {
                /*if (workingNode.pos[0] == 0 && workingNode.pos[1] == 0) {
                    System.out.println("MUST FIND FINITE PATH");
                }*/
                if ((checkNode = leftClosed.get(workingNode)) == null || workingNode.gVal < checkNode.gVal) {
                    leftClosed.put(workingNode, workingNode);
                    if ((checkNode = rightClosed.get(workingNode)) != null) {
                        if (workingNode.gVal + checkNode.gVal < pathLen) {
                            pathLen = workingNode.gVal + checkNode.gVal;
                            middleFromLeft = workingNode;
                            middleFromRight = checkNode;
                        }
                    } else {
                        leftExpand++;
                        //System.out.print("Expanded: [" + workingNode.pos[0] + ", " + workingNode.pos[1] + "] -> ");
                        for (int[] nn : nodeNN(workingNode.pos, probe)) {
                            Node newNode = new Node(workingNode, nn, workingNode.gVal + 1, h1.apply(probe, nn, end));
                            
                            if (newNode.gVal + newNode.hVal < pathLen && ((checkNode = leftClosed.get(newNode)) == null || newNode.gVal < checkNode.gVal)) {
                                
                                if (newNode.gVal + newNode.hVal <= leftCurrDepth) {
                                    leftOpen.add(newNode);
                                    //System.out.print("[" + newNode.pos[0] + ", " + newNode.pos[1] + "] | ");
                                } else if (newNode.gVal + newNode.hVal < leftNextDepth) {
                                    leftNextDepth = newNode.gVal + newNode.hVal;
                                }
                            }
                        }
                        //System.out.println();
                    }
                    
                    
                }
                
            } else if (openListType == Lattice2D.LIST_TYPE_PRIORITY) {
                leftOpen.clear();
                allEmpty = true;
            }

            if (h2 != null && pathLen == Double.POSITIVE_INFINITY) {
                if ((workingNode = rightOpen.poll()) == null) {
                    if (rightNextDepth == Double.POSITIVE_INFINITY) {
                        System.out.println("RIGHT BREAK " + rightOpen.size());
                        break;
                    }
                } else {

                    if (workingNode.gVal + workingNode.hVal < pathLen) {
                        allEmpty = false;
                        if ((checkNode = rightClosed.get(workingNode)) == null || workingNode.gVal < checkNode.gVal) {
                            rightClosed.put(workingNode, workingNode);
                            if ((checkNode = leftClosed.get(workingNode)) != null) {
                                if (checkNode.gVal + workingNode.gVal < pathLen) {
                                    pathLen = checkNode.gVal + workingNode.gVal;
                                    middleFromLeft = checkNode;
                                    middleFromRight = workingNode;
                                }
                            } else {
                                rightExpand++;
                                for (int[] nn : nodeNN(workingNode.pos, probe)) {
                                    Node newNode = new Node(workingNode, nn, workingNode.gVal + 1, h2.apply(probe, nn, start));
                                    if (newNode.gVal + newNode.hVal < pathLen && ((checkNode = rightClosed.get(newNode)) == null || newNode.gVal < checkNode.gVal)) {
                                        if (newNode.gVal + newNode.hVal <= rightCurrDepth) {
                                            rightOpen.add(newNode);
                                        } else if (newNode.gVal + newNode.hVal < rightNextDepth) {
                                            rightNextDepth = newNode.gVal + newNode.hVal;
                                        }
                                    }
                                    
                                }
                            }
                            
                            //rightExpand++;
                            
            
                        }
        
                        
                    }   else if (openListType == Lattice2D.LIST_TYPE_PRIORITY) {
                        rightOpen.clear();
                    }
                } 
            }
        }
        Instant finishTime = Instant.now();
        System.out.println("Time ms: " + Duration.between(startTime, finishTime).toMillis());
        System.out.println("Expanded: " + (leftExpand + rightExpand));
        return pathLen;
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
        int[][] start = {{1000, 0}};
        int[][] end = {{0, 13}};
        boolean[][] board = new boolean[1200][1200];
        for (boolean[] row : board) {
            for (int col = 0; col < row.length; col++) {
                row[col] = false;
            }
        }

        OpenSimplexNoise A = new OpenSimplexNoise(969l);
        
        Function<int[], Boolean> probeEasy = (pos) -> {
            if (A.eval(pos[0], pos[1]) < 0.01) {
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

            return false;
        };

        Lattice2D test = new Lattice2D(probeDesigned, start, end);
        BufferedImage img = new BufferedImage(1003, 1003, BufferedImage.TYPE_INT_RGB);
        int[] tPos = new int[2];
        for (int y = 0; y < 1003; y++) {
            tPos[1] = y;
            for (int x = 0; x < 1003; x++) {
                tPos[0] = x;
                if (probeDesigned.apply(tPos)) {
                    img.setRGB(x, y, 0xFF000000);
                } else {
                    img.setRGB(x, y, 0xFFFFFFFF);
                }
            }
        }

        for (int[] pos : end) {
            img.setRGB(pos[0], pos[1], 0xFF00FF00);
        }

        for (int[] pos : start) {
            img.setRGB(pos[0], pos[1], 0xFFFF0000);
        }
        
        try {
            ImageIO.write(img, "bmp", new File("IMGOUT"));
        } catch (Exception ex) {
            System.out.println("OOPs");
        }

        while (true) {
            System.out.println("Mono: ");
            System.out.println(test.solve(hMH, null, true, Lattice2D.LIST_TYPE_PRIORITY));
            System.out.println("Bi: ");
            System.out.println(test.solve(hMH, hMH, true, Lattice2D.LIST_TYPE_PRIORITY));
        }
    }

}