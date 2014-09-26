/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package fieldbot;

import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.Unit;
import com.springrts.ai.oo.clb.UnitDef;
import java.util.ArrayList;

/**
 * 
 * @author PlayerO1
 */
public class TBaseTarget {
    public ArrayList<com.springrts.ai.oo.clb.UnitDef> currentBuildLst; // What build for now
    public ArrayList<Integer> currentBuildCount; // How many build
    public ArrayList<AIFloat3> currentBuildPos; // predefine position, or null
    
    private ArrayList<UnitDef> inProgressBuildLst; // What connamd send to build
    private ArrayList<Integer> inProgressBuildCount; // how many command to build send
    private ArrayList<Integer> inProgressBuildTime; // wait time for start build (frame)
    
    private ArrayList<Unit> inBuildingBuildLst; // What start building now, and not finished

    // Ligic object trajectory:
    // currentBuildLst -> inProgressBuildLst -> inBuildingBuildLst -> (done! or goto currentBuildLst)
    
    public boolean targetHaveComplite; // signalized about last list of target complited or no
    public boolean commandNow; // for base, execute emidetly
    public boolean needPrecissionRes; // for base, do not economic when this target is not complited.
    
    
    public TBaseTarget() {
        targetHaveComplite=false;
        commandNow=false;
        needPrecissionRes=false;
        
        currentBuildLst=new ArrayList<com.springrts.ai.oo.clb.UnitDef>();
        currentBuildCount=new ArrayList<Integer>();
        currentBuildPos=new ArrayList<AIFloat3>();
        
        inProgressBuildLst=new ArrayList<UnitDef>();
        inProgressBuildCount=new ArrayList<Integer>();
        inProgressBuildTime=new ArrayList<Integer>();
        inBuildingBuildLst=new ArrayList<Unit>();
    }
    
    private int lastFrame=0;
    /**
     * For update list (for timeout).
     * Checl only when frame delta >10
     * @param frame current frame
     */
    public void update(int frame) {
        int frameD=frame-lastFrame;
        
        if (frameD>10) { // do not using CPU too often.
           if (!inProgressBuildTime.isEmpty()) // fount and fixed OutOfBoundsException. Don't know how it possible, but I see message of this line with this exception at next line (for).
            for (int i=inProgressBuildLst.size()-1;i>=0;i--) {
                int c=inProgressBuildTime.get(i);
                c-=frameD;
                if (c<=0) {
                    add(inProgressBuildLst.get(i), inProgressBuildCount.get(i));
                    inProgressBuildLst.clear();
                    inProgressBuildCount.clear();
                    inProgressBuildTime.clear();
                } else inProgressBuildTime.set(i, c);
            }
            
            lastFrame=frame;
        }
    }
    
    /**
     * On all target finished.
     */
    private void onFinishTarget() {
        // disable special option.
        commandNow=false;
        needPrecissionRes=false;
        targetHaveComplite=true;
        // TODO call to owner
    }
    
    /**
     * Add build target
     * @param def what build
     * @param count count of build def
     * @param buildPos position for build it
     */
    public void add(UnitDef def,int count,AIFloat3 buildPos) {
        // TODO при сбое строительства позиция теряется. При создании группы юнитов и последущего добавления позиция может не сбросится.
        // Всегда добавляет как новые. !!!!
        int p=currentBuildLst.size();
        currentBuildLst.add(p,def);
        currentBuildCount.add(p,count);
        currentBuildPos.add(p, buildPos);
        targetHaveComplite=false;
        
        //TODO if (count>1) owner.basePlaning,prepareFor()...
    }
    /**
     * Add build target
     * @param def what build
     * @param count count of build def
     */
    public void add(UnitDef def,int count) {
        int p=currentBuildLst.indexOf(def);
        if (p==-1) { // нет элемента
            p=currentBuildLst.size();
            currentBuildLst.add(p,def);
            currentBuildCount.add(p,count);
            currentBuildPos.add(p, null); // без позиции.
        } else { // элемент уже есть.
            int c=currentBuildCount.get(p);
            c+=count;
            currentBuildCount.set(p, c); // increment
        }
        targetHaveComplite=false;
    }
    
    /**
     * Clear target list
     */
    public void removeAllBuildingTarget() {
        currentBuildLst.clear();
        currentBuildCount.clear();
        currentBuildPos.clear();

        inProgressBuildLst.clear();
        inProgressBuildCount.clear();
        inProgressBuildTime.clear();
        inBuildingBuildLst.clear();
        
        commandNow=false;
        needPrecissionRes=false;
    }
    
    public boolean isEmpty() {
        return currentBuildLst.isEmpty() && inProgressBuildLst.isEmpty() && inBuildingBuildLst.isEmpty();
    }
    
    /**
     * Call this on unit planned to start building
     * @param def who add to build
     * @param timeout time (frames) for waiting unitCreated, if time will be pass then think that error build and add to currentBuildLst again.
     */
    public void unitPlanedToBuild(UnitDef def, int timeout) {
        // TODO what best timeout?
        int p=currentBuildLst.indexOf(def);
        if (p==-1) { // if not in plan?
            // do nothing
        } else { // element by plan
            int c=currentBuildCount.get(p);
            c--;
            if (c<=0) { // if last
                currentBuildLst.remove(p);
                currentBuildCount.remove(p); // !!!!!!
                currentBuildPos.remove(p);
            } else currentBuildCount.set(p, c);
            // add to inProgress
            p=inProgressBuildLst.indexOf(def);
            if (p==-1) { // no element
                p=inProgressBuildLst.size();
                inProgressBuildLst.add(p,def);
                inProgressBuildCount.add(p, 1);
                inProgressBuildTime.add(p, timeout);
            } else { // element by plan
                c=inProgressBuildCount.get(p);
                c++;
                inProgressBuildCount.set(p, c);
                inProgressBuildTime.set(p, timeout);
            }
        }
    }
    
    /**
     * Call this only if unit register on current base.
     * @param unit
     * @param builder 
     */
    public void unitCreated(Unit unit, Unit builder) {
        //if (currentBuildLst.contains(unit.getDef())) currentBuildLst.remove(unit.getDef()); // !!!!!!
        UnitDef uDef=unit.getDef();
        if (!inProgressBuildLst.contains(uDef) && !currentBuildLst.contains(uDef)) return; // не обрабатывать, если нет в списках.
        
        int p=inProgressBuildLst.indexOf(uDef);
        if (p!=-1) {
            int c=inProgressBuildCount.get(p);
            c--;
            if (c<=0) {
                inProgressBuildLst.remove(p);
                inProgressBuildCount.remove(p);
                inProgressBuildTime.remove(p);
            } else inProgressBuildCount.set(p, c);
            //inProgressBuildTime.set(p, timeout); // TODO ???
        } else { // it is lost timeout, but object was created?
            p=currentBuildLst.indexOf(unit.getDef());
            if (p!=-1) {
                int c=currentBuildCount.get(p);
                c--;
                if (c<=0) { // if last
                    currentBuildLst.remove(p);
                    currentBuildCount.remove(p); // !!!!!!
                    currentBuildPos.remove(p);
                } else currentBuildCount.set(p, c);
                
            }
        }
        inBuildingBuildLst.add(unit); // TODO inBuildingBuildLst.contain(unit) ???!!!
    }
    public void unitFinished(Unit unit) {
        //if (currentBuildLst.contains(unit.getDef())) currentBuildLst.remove(unit.getDef()); // !!!!!!
        int p=inBuildingBuildLst.indexOf(unit);
        if (p!=-1) { // if finished then remove from list
            inBuildingBuildLst.remove(p);
        } else {
            p=inProgressBuildLst.indexOf(unit.getDef());
            if (p!=-1) {
                int c=inProgressBuildCount.get(p);
                c--;
                if (c<=0) {
                    inProgressBuildLst.remove(p);
                    inProgressBuildCount.remove(p);
                    inProgressBuildTime.remove(p);
                } else inProgressBuildCount.set(p, c);
            }
        }
        
        // TODO if (currentBuildLst.contains(unit.getDef()))  ?????
        if (isEmpty()) onFinishTarget(); // signalize about target complited
    }
    
    public void unitDestroyed(Unit unit, Unit attacker) {
        int p=inBuildingBuildLst.indexOf(unit);
        if (p!=-1) { // if destroy when behin building
            add(unit.getDef(), 1);
            inBuildingBuildLst.remove(p);
        }
    }
    // TODO captured, sending...
    
    
    public String toString() {
        if (isEmpty()) return "list is empty.";
        
        String s="In a List = "+currentBuildLst.size();
        if (!currentBuildLst.isEmpty())
            for (int i=0;i<currentBuildLst.size();i++) {
                if (currentBuildPos.get(i)==null)
                    s+=", (u:"+currentBuildLst.get(i).getName()+" n:"+currentBuildCount.get(i)+")";
                else
                    s+=", (u:"+currentBuildLst.get(i).getName()+" n:"+currentBuildCount.get(i)+" p:"+currentBuildPos.get(i).toString()+")";
            }
        s+=". In a inProgressBuildLst = "+inProgressBuildLst.size();
        if (!inProgressBuildLst.isEmpty())
            for (int i=0;i<inProgressBuildLst.size();i++) s+=", (u:"+inProgressBuildLst.get(i).getName()+" n:"+inProgressBuildCount.get(i)+" t:"+inProgressBuildTime.get(i)+")";
        s+=". In a inBuildingBuildLst = "+inBuildingBuildLst.size();
        if (!inBuildingBuildLst.isEmpty())
            for (int i=0;i<inBuildingBuildLst.size();i++) s+=", (u:"+inBuildingBuildLst.get(i).getDef().getName()+")";
        return s;
    }
}
