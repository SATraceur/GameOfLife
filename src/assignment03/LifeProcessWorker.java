package assignment03;

import java.awt.Point;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

public class LifeProcessWorker implements Runnable {

    private int startRow, endRow;
    private static volatile ArrayList<Point> survivingCells;
    private static volatile boolean[][] gameBoard;
    private CountDownLatch latch; 
    
    public LifeProcessWorker(int startRow, int endRow, ArrayList<Point> survivingCells, boolean[][] gameBoard, CountDownLatch latch) {
        this.startRow = startRow;
        this.endRow = endRow;
        this.survivingCells = survivingCells;
        this.gameBoard = gameBoard;
        this.latch = latch;
    }
    
    @Override
    public void run() {
        
        for (int currentColumn = 1; currentColumn < gameBoard[0].length - 1; currentColumn++) {                 // Calculate number of surrounding cells.
            for (int currentRow = startRow; currentRow <= endRow; currentRow++) {                               // Iterate through given row range.
                
                int surrounding = 0;
                if (gameBoard[currentRow - 1][currentColumn - 1]) { surrounding++; }
                if (gameBoard[currentRow - 1][currentColumn]) { surrounding++; }
                if (gameBoard[currentRow - 1][currentColumn + 1]) { surrounding++; }
                if (gameBoard[currentRow][currentColumn - 1]) { surrounding++; }
                if (gameBoard[currentRow][currentColumn + 1]) { surrounding++; }
                if (gameBoard[currentRow + 1][currentColumn - 1]) { surrounding++; }
                if (gameBoard[currentRow + 1][currentColumn]) { surrounding++; }
                if (gameBoard[currentRow + 1][currentColumn + 1]) { surrounding++; }
                if (gameBoard[currentRow][currentColumn]) {                     // Is cell alive?
                    isAlive(surrounding, currentRow, currentColumn);
                } else {                                                        // Cell is dead, will the cell be given birth? (Conway, 3)
                    birth(surrounding, currentRow, currentColumn);
                }
            }
        }          
        latch.countDown();                                                      // Signifies worker thread is done...
    }
    
    private void isAlive(int surrounding, int currentRow, int currentColumn) {
        boolean survive = true;
        for (int si = 0; si < LifeProcessor.survives.length; si++) {                                 // Cell is alive, Can the cell live? (Conway, 2-3)
            if (LifeProcessor.survives[si] == surrounding) {                                         // survivial!!
                synchronized(survivingCells) {                                                  // Synchronize on object to be modifed.
                    survivingCells.add(new Point(currentRow - 1, currentColumn - 1));           // Store the cell in the survival matrix
                }
                break;
            }
        }
    }
    
    private void birth(int surrounding, int currentRow, int currentColumn) {
        for (int bi = 0; bi < LifeProcessor.birth.length; bi++) {
            if (LifeProcessor.birth[bi] == surrounding) {                                         // survivial!! 
                synchronized(survivingCells){                                                // Synchronize on object to be modifed.
                    survivingCells.add(new Point(currentRow - 1, currentColumn - 1));        // Store the cell in the survival matrix
                }
                break;
            }
        }
    }

   
            
    
}
