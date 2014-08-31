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
 * @author user2
 */
public class TBaseTarget {
    public ArrayList<com.springrts.ai.oo.clb.UnitDef> currentBuildLst; // Что строить сейчас
    public ArrayList<Integer> currentBuildCount; // сколько строить
    public ArrayList<AIFloat3> currentBuildPos; // принудительные позиции строительства
    
    private ArrayList<UnitDef> inProgressBuildLst; // Что уже заложено
    private ArrayList<Integer> inProgressBuildCount; // сколько строить
    private ArrayList<Integer> inProgressBuildTime; // время ожидания (frame)
    
    private ArrayList<Unit> inBuildingBuildLst; // Что уже строится

    // цепочка объектов
    // currentBuildLst -> inProgressBuildLst -> inBuildingBuildLst -> (done! or goto currentBuildLst)
    
    public boolean makeEco;
    public boolean targetHaveComplite;
    public boolean commandNow; // Выполнить немедленно.
    public boolean needPrecissionRes; // Нужно соблюдать точность ресурсов - ничего лишнего кроме этого списка не будет строится, пока он не закончится.
    
    
    public TBaseTarget() {
        makeEco=true;
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
     * Обновить список постройки (для timeout).
     * Срабатывает, при разницы во времени >10
     * @param frame текущее время
     */
    public void update(int frame) {
        int frameD=frame-lastFrame;
        
        if (frameD>10) { // не так часто проверять.
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
        // TODO test it.
    }
    
    /**
     * Добавить цель постройки
     * @param def что строить
     * @param count сколько
     * @param buildPos позицию, где строить эти юниты
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
     * Добавить цель постройки
     * @param def что строить
     * @param count сколько
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
     * Очистить список целей постройки
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
     * Запланированна постройка юнита
     * @param def кого заложили
     * @param timeout время ожидания до unitCreated, если превысит, то считается что отменилось и добавляется в currentBuildLst
     */
    public void unitPlanedToBuild(UnitDef def, int timeout) {
        // TODO timeout какой нужен???
        int p=currentBuildLst.indexOf(def);
        if (p==-1) { // нет элемента
            // не учитывать другие планы и юниты...
        } else { // элемент по плану.
            int c=currentBuildCount.get(p);
            c--;
            if (c<=0) { // если последний по колличеству.
                currentBuildLst.remove(p);
                currentBuildCount.remove(p); // !!!!!!
                currentBuildPos.remove(p);
            } else currentBuildCount.set(p, c);
            // Добавить в inProgress
            p=inProgressBuildLst.indexOf(def);
            if (p==-1) { // нет элемента
                p=inProgressBuildLst.size();
                inProgressBuildLst.add(p,def);
                inProgressBuildCount.add(p, 1);
                inProgressBuildTime.add(p, timeout);
            } else { // элемент по плану.
                c=inProgressBuildCount.get(p);
                c++;
                inProgressBuildCount.set(p, c);
                inProgressBuildTime.set(p, timeout);
            }
        }
    }
    
    /**
     * Вызывается, если unit принадлежит текущей базе.
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
        } else { // истекло время ожидания, но объект заложен?!!!!
            p=currentBuildLst.indexOf(unit.getDef());
            if (p!=-1) {
                // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                int c=currentBuildCount.get(p);
                c--;
                if (c<=0) { // если последний по колличеству.
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
        if (p!=-1) { // если достроен, то убрать из целей.
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
        if (isEmpty()) {
            commandNow=false;// отключить срочное выполнение.
            needPrecissionRes=false; // откл. спец. опции.
            targetHaveComplite=true;
        }
    }
    
    public void unitDestroyed(Unit unit, Unit attacker) {
        // TODO
        int p=inBuildingBuildLst.indexOf(unit);
        if (p!=-1) { // если уничтожен во время постройки
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
