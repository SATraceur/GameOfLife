
package assignment03;

public class GuiWorker implements Runnable {

    private LifeProcessor lp;
    private LifeProcessor.ComputeMode computeMode;
    private int generations;
    
    public GuiWorker(LifeProcessor lp, int generations, LifeProcessor.ComputeMode cm) {
        this.lp = lp;
        this.generations = generations;
        this.computeMode = cm;
    }
    
    @Override
    public void run() {
        long startTime = System.currentTimeMillis();
        lp.processLife(generations, computeMode);
        long endTime = System.currentTimeMillis();
        GameOfLifeGUI.jTextFieldElapsedTime.setText(getElapsedTimeString(endTime - startTime));
    }
    
    private String getElapsedTimeString(long elapsedTime) {
        String format = String.format("%%0%dd", 2);
        String millisecs = String.format(format, elapsedTime % 1000);
        elapsedTime /= 1000;
        String seconds = String.format(format, elapsedTime % 60);
        String minutes = String.format(format, (elapsedTime % 3600) / 60);
        String hours = String.format(format, elapsedTime / 3600);
        String time = hours + ":" + minutes + ":" + seconds + ":" + millisecs;
        return time;
    }
}
