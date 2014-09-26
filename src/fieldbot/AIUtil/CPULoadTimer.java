/*
 * (C) PlayerO1, 2014. GNU GPL v2 or above license.
 * This class could be used in the past did not GNU programs by PlayerO1.
 */

package fieldbot.AIUtil;

import java.util.Date;

/**
 *
 * @author PlayerO1
 */
public class CPULoadTimer {
    
    private Date timeFirstStart,timerStartAt,timerStopAt,timerGameTime; // для подсчёта времени
	
    public boolean lastHasStarted=false; // TODO улучшить. временная переменная для хранения промежуточного состояния...
	
    
    /**
     * Check timer state
     */
    public boolean isActivated() {
    	return timerStartAt!=null;
    }
    
    public CPULoadTimer()
    {
        timerStartAt=timerStopAt=null; //...
        timerGameTime=new Date(0); // set game time = 0 milisecond
        timeFirstStart=new Date(); // time of init
    }
    
    /**
     * Start clock
     */
    public void start()
    {
        if (timerStartAt!=null) stop(); // !!!!!!
        timerStartAt=new Date();//!!!!!
    }
    
    /**
     * Clear contain time, reset clock.
     */
    public void reset() {
        timerStartAt=timerStopAt=null;
        timerGameTime=new Date(0);
        timeFirstStart=new Date();
    }
    
    /**
     * Stop clock
     */
    public void stop()
    {
	timerStopAt=new Date();
    	if (timerStartAt!=null) {
    		timerGameTime.setTime( timerGameTime.getTime() + (timerStopAt.getTime()-timerStartAt.getTime()) ); // ... long - 9,223,372,036,854,775,807 много лет работать будет :)
    		timerStartAt=null;
    	} // else ?
    }
    
    /**
     * Get clock time
     * @return work time
     */
    public Date getTime() {
    	if (timerStartAt==null) {
    		return timerGameTime;
    	} else {
        	return new Date( timerGameTime.getTime() + (new Date().getTime()-timerStartAt.getTime()) );
    	}
    }

    
    /**
     * Return time milisec (CPU % loar)
     * @return human readable time
     */
    @Override
    public String toString() {
        Date gameTime = getTime(); 
        long cpuTime=gameTime.getTime();
        // TODO улучшить формат вывода
        long totalTime=new Date().getTime()-timeFirstStart.getTime();
        double percentLoad=(double)cpuTime*100.0/(double)totalTime;
        percentLoad = Math.round(percentLoad*100.0)/100.0 ; // округление до сотых
        String out = cpuTime+" ms ("+percentLoad+"% load)"; //gameTime.getMinutes() + ":" + gameTime.getSeconds(); 
    	return out;
    }    
    
}
