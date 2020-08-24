import java.util.function.Function;

//Merely used to generate small test cases to check if algorithms work correctly.
public class TestSuite {
    public static void main(String[] args) {
        int[][] start = {{31, 44}};
        int[][] end = {{44, 11}};
        boolean[][] board = new boolean[1200][1200];

        OpenSimplexNoise alpha = new OpenSimplexNoise(99l);
        OpenSimplexNoise beta = new OpenSimplexNoise(420l);

        
        Function<int[], Boolean> easyNoise = (pos) -> {
            if (pos[0] < 0 || pos[0] > 100 || pos[1] < 0 || pos[1] > 100) {
                return true;
            }

            if (Math.abs(alpha.eval(pos[0], pos[1])) < 0.45) {
                return false;
            }

            return true;
        };

        Function<int[], Boolean> testNoise1 = (pos) -> {
            if (pos[0] < 0 || pos[0] > 10 || pos[1] < 0 || pos[1] > 10) {
                return true;
            }

            if (Math.abs(alpha.eval(pos[0], pos[1])) < 0.6) {
                return false;
            }

            return true;
        };

        

        Function<int[], Boolean> testNoise2 = (pos) -> {
            if (pos[0] < 0 || pos[0] > 10 || pos[1] < 0 || pos[1] > 10) {
                return true;
            }

            if (beta.eval(pos[0] + 30, pos[1]) < 0.2) {
                return false;
            }

            return true;
        };

        Function<int[], Boolean> nookTest = (pos) -> {
            if (pos[0] < 0 || pos[0] > 50 || pos[1] < 0 || pos[1] > 50) {
                return true;
            }

            if (pos[0] == 25) {
                return pos[1] < 42;
            }

            if (pos[0] == 42) {
                return pos[1] > 15;
            } 

            if (pos[1] == 22) {
                if (10 < pos[0] && pos[0] < 15) {
                    return true;
                }
            } 

            if (pos[1] == 7) {
                if (10 < pos[0] && pos[0] < 25) {
                    return true;
                }
            } 
            
            if (Math.abs(alpha.eval(pos[0] / 5.0 + 100, pos[1] / 5.0)) < 0.4) {
                return false;
            }

            return true;
        };

        Function<int[], Boolean> doubleSlit = (pos) -> {
           if (pos[1] == 7) {
               if (pos[0] != 1) {
                   return true;
               }
           }

           if (pos[1] == 4) {
               if (pos[0] != 9) {
                   return true;
               }
           }

           if (pos[0] < 0 || pos[0] > 10) {
               return true;
           }

           if (pos[1] < 0 || pos[1] > 10) {
               return true;
           }

            return false;
        };

        Function<int[], Boolean> easyEmpty = (pos) -> {
            if (pos[0] < 0 || pos[0] > 10) {
                return true;
            }
 
            if (pos[1] < 0 || pos[1] > 10) {
                return true;
            }
 
             return false;
        };

        Function<int[], Boolean> mediumEmpty = (pos) -> {
        if (pos[0] < 0 || pos[0] > 50) {
            return true;
        }

        if (pos[1] < 0 || pos[1] > 50) {
            return true;
        }

            return false;
        };

        Function<int[], Boolean> hardEmpty = (pos) -> {
            if (pos[0] < 0 || pos[0] > 500) {
                return true;
            }

            if (pos[1] < 0 || pos[1] > 500) {
                return true;
            }

                return false;
        };

        start[0][0] = 0;
        start[0][1] = 0;

        end[0][0] = 10;
        end[0][1] = 10;

        //Empty Board Test
        Lattice2D test = new Lattice2D(easyEmpty, start, end);
        Lattice2D.SearchResults results;
        
        results = test.solve(Lattice2D.hMH, Lattice2D.hMH, Lattice2D.SEARCH_TYPE_BDAS);
        results.genFolder("Output/BDAS_MH_Empty");

        results = test.solve(Lattice2D.hMH, null, Lattice2D.SEARCH_TYPE_AS);
        results.genFolder("Output/AS_MH_Empty");

        results = test.solve(Lattice2D.hMH, null, Lattice2D.SEARCH_TYPE_ASID);
        results.genFolder("Output/ASID_MH_Empty");

        results = test.solve(null, null, Lattice2D.SEARCH_TYPE_DFID);
        results.genFolder("Output/DFID_NULL_Empty");

        //Double Slit Board Test
        test = new Lattice2D(doubleSlit, start, end);

        results = test.solve(Lattice2D.hMH, Lattice2D.hMH, Lattice2D.SEARCH_TYPE_BDAS);
        results.genFolder("Output/BDAS_MH_DS");

        results = test.solve(Lattice2D.hMH, null, Lattice2D.SEARCH_TYPE_AS);
        results.genFolder("Output/AS_MH_DS");

        results = test.solve(Lattice2D.hMH, null, Lattice2D.SEARCH_TYPE_ASID);
        results.genFolder("Output/ASID_MH_DS");


         //Noise Test 1
         test = new Lattice2D(testNoise1, start, end);
         results = test.solve(Lattice2D.hMH, Lattice2D.hMH, Lattice2D.SEARCH_TYPE_BDAS);
         results.genFolder("Output/BDAS_MH_TN1");
 
         results = test.solve(Lattice2D.hMH, null, Lattice2D.SEARCH_TYPE_AS);
         results.genFolder("Output/AS_MH_TN1");
 
         results = test.solve(Lattice2D.hMH, null, Lattice2D.SEARCH_TYPE_ASID);
         results.genFolder("Output/ASID_MH_TN1");
 
         results = test.solve(null, null, Lattice2D.SEARCH_TYPE_DFID);
         results.genFolder("Output/DFID_NULL_TN1");

         //Noise Test 2
         test = new Lattice2D(testNoise2, start, end);
         results = test.solve(Lattice2D.hMH, Lattice2D.hMH, Lattice2D.SEARCH_TYPE_BDAS);
         results.genFolder("Output/BDAS_MH_TN2");
 
         results = test.solve(Lattice2D.hMH, null, Lattice2D.SEARCH_TYPE_AS);
         results.genFolder("Output/AS_MH_TN2");
 
         results = test.solve(Lattice2D.hMH, null, Lattice2D.SEARCH_TYPE_ASID);
         results.genFolder("Output/ASID_MH_TN2");
 
         results = test.solve(null, null, Lattice2D.SEARCH_TYPE_DFID);
         results.genFolder("Output/DFID_NULL_TN2");

        start[0][0] = 0;
        start[0][1] = 0;

        end[0][0] = 50;
        end[0][1] = 50;
        test = new Lattice2D(nookTest, start, end);
        //Nook Pruning Test
        results = test.solve(Lattice2D.hMH, Lattice2D.hMH, Lattice2D.SEARCH_TYPE_BDAS);
        results.genFolder("Output/BDAS_MH_NOOK");
 
        results = test.solve(Lattice2D.hMH, null, Lattice2D.SEARCH_TYPE_AS);
        results.genFolder("Output/AS_MH_NOOK");

        results = test.solve(Lattice2D.hMHNook, Lattice2D.hMHNook, Lattice2D.SEARCH_TYPE_BDAS);
        results.genFolder("Output/BDAS_MHNook_NOOK");
 
        results = test.solve(Lattice2D.hMHNook, null, Lattice2D.SEARCH_TYPE_AS);
        results.genFolder("Output/AS_MHNook_NOOK");

        
    }
}

