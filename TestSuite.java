import java.util.function.Function;

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

        Function<int[], Boolean> bigObstacleNoise = (pos) -> {

            if (Math.abs(beta.eval(pos[0] / 10.0, pos[1] / 10.0)) < 0.5) {
                return false;
            }

            return true;
        };
        
        Function<int[], Boolean> hardNoise = (pos) -> {
            

            if (Math.abs(alpha.eval(pos[0], pos[1])) < 0.4) {
                return false;
            }

            return true;
        };

        Function<int[], Boolean> singleSlit = (pos) -> {
            if (pos[1] == 50) {
                if (pos[0] != 78) {
                    return true;
                }
            }
 
            if (pos[0] < 0 || pos[0] > 100) {
                return true;
            }
 
            if (pos[1] < 0 || pos[1] > 100) {
                return true;
            }
 
             return false;
         };

        Function<int[], Boolean> doubleSlit = (pos) -> {
           if (pos[1] == 300) {
               if (pos[0] != 600) {
                   return true;
               }
           }

           if (pos[1] == 600) {
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

        Function<int[], Boolean> easyEmpty = (pos) -> {
            if (pos[0] < 0 || pos[0] > 20) {
                return true;
            }
 
            if (pos[1] < 0 || pos[1] > 20) {
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

        start[0][0] = 35;
        start[0][1] = 50;

        end[0][0] = 46;
        end[0][1] = 63;
        Lattice2D test = new Lattice2D(easyNoise, start, end);

        Lattice2D.SearchResults results = test.solve(Lattice2D.hMH, Lattice2D.hMH, Lattice2D.SEARCH_TYPE_BDAS);
        results.genFolder("Output/BDAS_MH_EasyNoise");

        results = test.solve(Lattice2D.hMH, null, Lattice2D.SEARCH_TYPE_AS);
        results.genFolder("Output/AS_MH_EasyNoise");

        results = test.solve(Lattice2D.hSLD, Lattice2D.hMHEq, Lattice2D.SEARCH_TYPE_BDAS);
        results.genFolder("Output/BDAS_SLD_EasyNoise");

        results = test.solve(Lattice2D.hSLD, null, Lattice2D.SEARCH_TYPE_AS);
        results.genFolder("Output/AS_SLD_EasyNoise");

        results = test.solve(Lattice2D.hNULL, Lattice2D.hNULL, Lattice2D.SEARCH_TYPE_BDAS);
        results.genFolder("Output/BDAS_DIJK_EasyNoise");

        results = test.solve(Lattice2D.hNULL, null, Lattice2D.SEARCH_TYPE_AS);
        results.genFolder("Output/AS_DIJK_EasyNoise");

        results = test.solve(Lattice2D.hMH, null, Lattice2D.SEARCH_TYPE_ASID);
        results.genFolder("Output/ASID_MH_EasyNoise");

        /*results = test.solve(Lattice2D.hSLD, null, Lattice2D.SEARCH_TYPE_ASID);
        results.genFolder("Output/ASID_SLD_EasyNoise");*/

        /*results = test.solve(null, null, Lattice2D.SEARCH_TYPE_DFID);
        results.genFolder("Output/DFID_NULL_EasyNoise");*/

        start[0][0] = 0;
        start[0][1] = 0;

        end[0][0] = 1001;
        end[0][1] = 1001;
        test = new Lattice2D(hardNoise, start, end);
        results = test.solve(Lattice2D.hMH, Lattice2D.hMH, Lattice2D.SEARCH_TYPE_BDAS);
        results.genFolder("Output/BDAS_MH_HardNoise");

        results = test.solve(Lattice2D.hMH, null, Lattice2D.SEARCH_TYPE_AS);
        results.genFolder("Output/AS_MH_HardNoise");

        results = test.solve(Lattice2D.hSLD, Lattice2D.hSLD, Lattice2D.SEARCH_TYPE_BDAS);
        results.genFolder("Output/BDAS_SLD_HardNoise");

        results = test.solve(Lattice2D.hSLD, null, Lattice2D.SEARCH_TYPE_AS);
        results.genFolder("Output/AS_SLD_HardNoise");

        /*results = test.solve(Lattice2D.hMHNook, Lattice2D.hMHNook, Lattice2D.SEARCH_TYPE_BDAS);
        results.genFolder("Output/BDAS_MHNook_HardNoise");

        results = test.solve(Lattice2D.hMHNook, null, Lattice2D.SEARCH_TYPE_AS);
        results.genFolder("Output/AS_MHNook_HardNoise");*/

        results = test.solve(Lattice2D.hNULL, null, Lattice2D.SEARCH_TYPE_AS);
        results.genFolder("Output/AS_DIJK_HardNoise");

        results = test.solve(Lattice2D.hNULL, Lattice2D.hNULL, Lattice2D.SEARCH_TYPE_BDAS);
        results.genFolder("Output/BDAS_DIJK_HardNoise");

        results = test.solve(Lattice2D.hMHEq, Lattice2D.hMHEq, Lattice2D.SEARCH_TYPE_BDAS);
        results.genFolder("Output/BDAS_MHEq_HardNoise");

        results = test.solve(Lattice2D.hMHEq, null, Lattice2D.SEARCH_TYPE_AS);
        results.genFolder("Output/AS_MHEq_HardNoise");

        start[0][0] = 100;
        start[0][1] = 50;

        end[0][0] = 800;
        end[0][1] = 1000;
        test = new Lattice2D(bigObstacleNoise, start, end);
        results = test.solve(Lattice2D.hMHEq, Lattice2D.hMHEq, Lattice2D.SEARCH_TYPE_BDAS);
        results.genFolder("Output/BDAS_MHEq_BigObstacleNoise");

        results = test.solve(Lattice2D.hMHEq, null, Lattice2D.SEARCH_TYPE_AS);
        results.genFolder("Output/AS_MHEq_BigObstacleNoise");
    }
}

