package org.opengeo.data.importer.job;

import java.io.Serializable;

import org.geotools.util.DefaultProgressListener;
import org.geotools.util.SimpleInternationalString;

@SuppressWarnings("serial")
public class ProgressMonitor extends DefaultProgressListener implements Serializable {
    
    private volatile int totalToProcess;
    
    private volatile int numberProcessed;

    public int getNumberProcessed() {
        return numberProcessed;
    }

    public void setNumberProcessed(int numberProcessed) {
        this.numberProcessed = numberProcessed;
        if (totalToProcess > 0) {
            progress(numberProcessed / totalToProcess);
        }
    }

    public int getTotalToProcess() {
        return totalToProcess;
    }

    public void setTotalToProcess(int totalToProcess) {
        this.totalToProcess = totalToProcess;
    }

    public void setTask(String message) {
        super.setTask(new SimpleInternationalString(message));
    };
    
}
