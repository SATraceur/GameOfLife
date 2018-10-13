package assignment03;

import java.awt.Dimension;
import java.awt.Point;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LifeProcessor {
    
    public enum ComputeMode { JAVA_SINGLE, JAVA_MULTI };
    private Dimension gameBoardSize = null;
    private ArrayList<Point> point = new ArrayList<>(0);
    public static int[] birth;
    public static int[] survives;
    private ArrayList<LifeListener> listeners;
    private boolean stop = false;
    
    /**
     * "B3/S23"
     *
     * @param birth
     * @param survives
     */
    public LifeProcessor(int[] birth, int[] survives, ArrayList<Point> point, Dimension gameBoardSize) {
        this.birth = birth;
        this.survives = survives;
        this.point = point;
        this.gameBoardSize = gameBoardSize;
        this.listeners = new ArrayList<>();
    }

    public void stopLife() { this.stop = true; }
    
    public void clear() { point.clear(); }
    
    public void addLifeListener(LifeListener l) { this.listeners.add(l); }
    
    public void processLife(int generations, ComputeMode m) {
        
        switch (m) {
            case JAVA_SINGLE:
                compute_java_single(generations);
                break;
            case JAVA_MULTI:
                compute_java_multi(generations);
                break;           
        }
    }
    
    private void compute_java_single(int generations) {

        int ilive = 0;
        int movesPerSecond = 0;
        if (generations < 0) {
            movesPerSecond = -generations;
            ilive = generations-1;                                              // ignore the ilive (go until keepLiving is false)
        }
        
        while (!stop && ilive < generations) {

            boolean[][] gameBoard = new boolean[gameBoardSize.width + 2][gameBoardSize.height + 2]; // Define gameboard size
            for (int i = 0; i < point.size(); i++) {                                                // Point is ArrayList of points
                Point current = point.get(i);
                gameBoard[current.x + 1][current.y + 1] = true;                                     // Set existing points in bool matrix == true
            }
            
            
            ArrayList<Point> survivingCells = new ArrayList<>(0);                                   // Create array for surviving cells
            
            for (int i = 1; i < gameBoard.length - 1; i++) {                    // Iterate through the array, follow game of life rules
                for (int j = 1; j < gameBoard[0].length - 1; j++) {             // calculate number of surrounding cells
                    int surrounding = 0;
                    if (gameBoard[i - 1][j - 1]) { surrounding++; }
                    if (gameBoard[i - 1][j]) { surrounding++; }
                    if (gameBoard[i - 1][j + 1]) { surrounding++; }
                    if (gameBoard[i][j - 1]) { surrounding++; }
                    if (gameBoard[i][j + 1]) { surrounding++; }
                    if (gameBoard[i + 1][j - 1]) { surrounding++; }
                    if (gameBoard[i + 1][j]) { surrounding++; }
                    if (gameBoard[i + 1][j + 1]) { surrounding++; }
                    if (gameBoard[i][j]) {
                        boolean survive = true;
                        for (int si = 0; si < this.survives.length; si++) {     // Cell is alive, Can the cell live? (Conway, 2-3)
                            if (this.survives[si] == surrounding) {             // survivial!!
                                survivingCells.add(new Point(i - 1, j - 1));    // Store the cell in the survival matrix
                                break;
                            }
                        }
                    } else {                                                    // Cell is dead, will the cell be given birth? (Conway, 3)
                        for (int bi = 0; bi < this.birth.length; bi++) {
                            if (this.birth[bi] == surrounding) {                // survivial!! 
                                survivingCells.add(new Point(i - 1, j - 1));    // Store the cell in the survival matrix
                                break;
                            }
                        }
                    }
                }
            }

            // update the points
            point.clear();
            point.addAll(survivingCells);                                       // Add all surviving cells to game matrix
            
            for (LifeListener l : listeners) { l.lifeUpdated(); }               // notify listeners

            if (generations > 0) { ilive++; }
            else {
                try { Thread.sleep(1000 / movesPerSecond); }
                catch (InterruptedException ex) { break; }
            }

        }
    }
    
    private void compute_java_multi(int generations) {     
        int threads = Runtime.getRuntime().availableProcessors();               // Define number of threads
        ExecutorService exec = Executors.newFixedThreadPool(threads);           // Declare threadpool       
        int rowDivisor = gameBoardSize.height/threads;                          // Calculate number of rows for each thread.
        
        int ilive = 0;
        int movesPerSecond = 0;
        if (generations < 0) {
            movesPerSecond = -generations;
            ilive = generations-1;                                              // ignore the ilive (go until keepLiving is false)
        }
               
        while(!stop && ilive < generations) {                                                       // While there are still generations to compute
            CountDownLatch latch = new CountDownLatch(threads);                                     // Create new latch
            boolean[][] gameBoard = new boolean[gameBoardSize.width + 2][gameBoardSize.height + 2]; // Define gameboard size
            ArrayList<Point> survivingCells = new ArrayList<>(0);                                   // Create array for surviving cells
            
            /**
             * Initilize board... LINEAR
             */
            for (int i = 0; i < point.size(); i++) {                                                // Point is ArrayList of points
                Point current = point.get(i);
                gameBoard[current.x + 1][current.y + 1] = true;                                     // Set existing points in bool matrix == true
            }
              
            /**
             * Calculate survival... PARALLEL
             */
            int startRow = 1;
            int endRow = rowDivisor;
            for (int i = 0; i < threads; i++) {                                     
                exec.execute(new LifeProcessWorker(startRow, endRow, survivingCells, gameBoard, latch));                     // Start worker threads
                startRow = endRow + 1;                                                                                       // Update row range for next thread
                if(endRow + rowDivisor > gameBoardSize.height || i == threads) { endRow = gameBoardSize.height; }
                else { endRow += rowDivisor; }         
            }  
            try {
                latch.await();                                                                                               // Main waits at latch for the worker threads to finish.
            } catch (InterruptedException ex) {
                Logger.getLogger(LifeProcessor.class.getName()).log(Level.SEVERE, null, ex);
            }
                      
            /**
             * Update listeners and generation stuff.. LINEAR
             */
            point.clear();                                                      // Clear the list of current points.
            point.addAll(survivingCells);                                       // Add all surviving cells (points) to game matrix.         
            for (LifeListener l : listeners) { l.lifeUpdated(); }               // notify listeners
            if (generations > 0) { ilive++; }
            else {
                try { Thread.sleep(1000 / movesPerSecond); }
                catch (InterruptedException ex) { break; }
            }          
        }
        exec.shutdown();                                                        // Shutdown threadpool   
        
    }

}
