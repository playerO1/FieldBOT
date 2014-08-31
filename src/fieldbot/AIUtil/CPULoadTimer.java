/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package fieldbot.AIUtil;

import java.util.Date;

/**
 *
 * @author user2
 */
public class CPULoadTimer {
    
    private Date timeFirstStart,timerStartAt,timerStopAt,timerGameTime; // для подсчёта времени
	
    public boolean lastHasStarted=false; // TODO улучшить. временная переменная для хранения промежуточного состояния...
	
    
    /**
     * Проверка включённости таймера...
     * @return работает или приостановлен
     */
    public boolean isActivated() {
    	return timerStartAt!=null;
    }
    
    public CPULoadTimer()
    {
        timerStartAt=timerStopAt=null; //...
        timerGameTime=new Date(0); // время игры - 0 милисекунд...
        timeFirstStart=new Date(); // время начала...
    }
    
    /**
     * Запуск/возобновление счётчика
     */
    public void start()
    {
        if (timerStartAt!=null) stop(); // !!!!!!
        timerStartAt=new Date();//!!!!!
    }
    
    /**
     * Сбросить счётчик
     */
    public void reset() {
        timerStartAt=timerStopAt=null;
        timerGameTime=new Date(0);
        timeFirstStart=new Date();
    }
    
    /**
     * Остановка счётчика
     */
    public void stop()
    {
	timerStopAt=new Date();
    	if (timerStartAt!=null) {
    		timerGameTime.setTime( timerGameTime.getTime() + (timerStopAt.getTime()-timerStartAt.getTime()) ); // ... long - 9,223,372,036,854,775,807 много лет работать будет :)
    		timerStartAt=null;
    	} // TODO else ... иначе ничего не прибавляется?
    }
    
    /**
     * Сколько времени насчитал счётчик всего.
     * @return время работы.
     */
    public Date getTime() {
    	if (timerStartAt==null) {
    		return timerGameTime;
    	} else {
        	return new Date( timerGameTime.getTime() + (new Date().getTime()-timerStartAt.getTime()) );
    	}
    }

    
    /**
     * Возвращает время в таймете в формате минуты:секунды.
     * @return строка с милисекундами
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
