/*
 * Copyright (C) 2014 PlayerO1
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package fieldbot;

import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.Unit;
import com.springrts.ai.oo.clb.WeaponDef;
import fieldbot.AIUtil.MathPoints;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 *
 * @author PlayerO1
 */
public class TArmyGroup extends AGroupManager{
    
    public ArrayList<Unit> army;
    public Unit slowUnit;
    
    public int tactic; // TODO
    public AIFloat3 moveTarget; // TODO non-line moving road to enemy base.
//    public boolean targetScout; // ???
    public int action; // idle, move, under atack
    public float groupRadius; // TODO groupRadius
    
    public static final int STATE_IDLE=0;
    public static final int STATE_MOVE=1;
    public static final int STATE_ATACK=2; // or UNDERATACK
    public static final int STATE_SCOUT=3; // send all idle units to any points
    
    
    public TArmyGroup(FieldBOT owner) {
        super(owner); //this.owner=owner;
        baseType=1;
        army=new ArrayList<Unit>();
        slowUnit=null;
        
        tactic=0;
        moveTarget=null;
//        targetScout=false;
        action=STATE_IDLE;
        groupRadius=10.0f;// !!!!
    }
    
    /**
     * Get max slow (no fast) unit. For max moving speed.
     * @return unit or null
     */
    private Unit getSlowUnit() {
        Unit slowU=null; float maxS=0;
        for (Unit u:army) {
            float s=u.getMaxSpeed(); // !!! TEST
            if (slowU==null || maxS>s) {
                slowU=u;
                maxS=s;
            }
        }
        return slowU;
    }
    
    @Override
    public boolean addUnit(Unit unit) {
        boolean added=army.add(unit);
        if (slowUnit==null || slowUnit.getMaxSpeed()>unit.getMaxSpeed()) onModifedUnits();
        else unit.guard(slowUnit, (short)0, 0);// !!! test
        
        float unitR=unit.getDef().getRadius();// TODO groupRadius
        if (groupRadius<unitR) groupRadius=unitR;// !!!
        
        return added;
    }
    
    @Override
    public boolean addAllUnits(Collection<Unit> units) {
        boolean modifed = army.addAll(units);
        if (modifed) onModifedUnits();
        return modifed;
    }

    @Override
    public boolean removeUnit(Unit unit) {
        boolean removed=army.remove(unit);
        if (removed) {
            if (slowUnit.equals(unit)) onModifedUnits();
        }
        return removed;
    }

    @Override
    public boolean removeAllUnits(Collection<Unit> units) {
        boolean modifed=army.removeAll(units);
        if (modifed) onModifedUnits();
        return modifed;
    }

    @Override
    public boolean contains(Unit unit) {
        return army.contains(unit);
    }
    
    @Override
    public boolean haveSpecialCommand() {
        return //moveTarget!=null || 
                action!=STATE_IDLE;
    }

    @Override
    public float doYouNeedUnit(Unit unit) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ArrayList<Unit> getAllUnits(boolean selectWhoCanNow) {
        return new ArrayList(army);
    }

    @Override
    public boolean isEmpty() {
        return army.isEmpty();
    }

    // -----------------
    private void onModifedUnits() {
        slowUnit=getSlowUnit(); // change moving speed if in moving now.        
        if (moveTarget!=null) {
            updateUnitCommands();// send command
        }
    }
    
    private void updateUnitCommands() {
        if (moveTarget!=null) {
          if (slowUnit!=null && action<STATE_ATACK) {
            // send command
            slowUnit.fight(moveTarget, (short)0, Integer.MAX_VALUE);
            for (Unit au:army) if (au!=slowUnit) au.guard(slowUnit, (short)0, Integer.MAX_VALUE);
      owner.sendTextMsg("Moving army: folow my slow unit",FieldBOT.MSG_DBG_ALL);
          } else {
            for (Unit au:army) 
                //if (au!=slowUnit)
                au.fight(MathPoints.getRandomPointInCircle(moveTarget, 20), (short)0, Integer.MAX_VALUE);
      owner.sendTextMsg("Moving army: all by fight",FieldBOT.MSG_DBG_ALL);
          }
        } else
        if (action==STATE_IDLE) {
            for (Unit au:army) {
                //if (au!=slowUnit) 
                //au.fight(MathPoints.getRandomPointInCircle(moveTarget, 20), (short)0, Integer.MAX_VALUE);
                au.stop((short)0, 0);
                // TODO far unit go to group center
            }
      owner.sendTextMsg("Moving army: all stop",FieldBOT.MSG_DBG_ALL);
        }
    }
    
    public void stop() {
        //TODO !!!
        action=STATE_IDLE;
        moveTarget=null;
        updateUnitCommands();// test!!!
    }
    public void moveTo(AIFloat3 target) {
        this.moveTarget=target;
        action=STATE_MOVE;
        // send command
        updateUnitCommands();
    }
    public void fightTo(AIFloat3 target) { // TODO + , boolean cmdOnStack
        // TODO FIGHT!!!
        action=STATE_ATACK;
        updateUnitCommands();
    }
    /**
     * set scout mode - send all idle units to discover map
     */
    public void doScout() {
        action=STATE_SCOUT;
        updateUnitCommands();
    }
    
    
@Override
public int update(int frame) {
    if (frame%7==4) {

//===========================
        // FIXME TODO !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
        switch (action) {
         case STATE_IDLE:
             //TODO STATE_IDLE
           break;
         case STATE_MOVE:
            if (moveTarget!=null && slowUnit!=null) {
                if (MathPoints.getDistanceBetween3D(moveTarget, slowUnit.getPos())<slowUnit.getDef().getRadius()*1.5f
                    || slowUnit.getCurrentCommands().isEmpty() // !!!!!!!!!!!!!!!!!!!!
                   )
                {
                    moveTarget=null; // finish.
                    onCommandFinished();
                    stop(); //!!!
                }
//                    }) {
//                // send command
//                slowUnit.fight(moveTarget, (short)0, Integer.MAX_VALUE);
//                for (Unit au:army) if (au!=slowUnit) au.guard(slowUnit, (short)0, Integer.MAX_VALUE);
            } //FIXME ELSE????

            if (moveTarget!=null && slowUnit==null && !isEmpty()) {
                owner.sendTextMsg("Warning! army without main unit have target.", FieldBOT.MSG_ERR); // debug
                moveTarget=null; // finish by error.
                onCommandFinished();
                stop(); //!!!
            }
            if (army.isEmpty()) {
                stop(); // TODO do self-destruct group
            }
            
           break;
             
         case STATE_ATACK:
             char nearEnemy=0;// -1=no 0=uncknown +1=yes
            for (Unit u:army) if (u.getCurrentCommands().isEmpty())
            {
                AIFloat3 uPos=slowUnit.getPos();
                float distance=MathPoints.getDistanceBetween3D(moveTarget, uPos);
                if (distance>groupRadius*2.0f) u.fight(moveTarget, (short)0, Integer.MAX_VALUE);
                else if (nearEnemy==0)
                { // search enemy
                    float searchRadius=Math.max(slowUnit.getDef().getLosRadius(),groupRadius*10.0f);
                    List<Unit> enemys=owner.clb.getEnemyUnitsIn(uPos,searchRadius);
                    if (enemys.isEmpty()) nearEnemy=(char)-1;
                    else nearEnemy=+1;
                }
                
            }
            if (nearEnemy==-1) {
                moveTarget=null; // finish
                onCommandFinished();
                stop(); // TODO save old command
            }
            if (army.isEmpty()) {
                stop(); // TODO do self-destruct group
            }
           break;
             
         case STATE_SCOUT:
             // no stoping state
            for (Unit u:army) if (u.getCurrentCommands().isEmpty())
            {
                AIFloat3 sp=owner.scoutModule.getPointToScout(u.getPos(), 0);
                u.fight(sp, (short)0, Integer.MAX_VALUE);
            }
            if (army.isEmpty()) {
                stop(); // TODO do self-destruct group
            }
           break;
        }
    }
    return 0;
}
    
    
    // TODO method call -------------------------

@Override
public int commandFinished(Unit unit, int commandId, int commandTopicId) {
    if (unit.getCurrentCommands().isEmpty()) {
       if (action!=STATE_SCOUT || action!=STATE_IDLE)
        if (unit==slowUnit && moveTarget!=null) {
            moveTarget=null; // failed move to this target
            updateUnitCommands();
            onCommandFinished(); // !!!
        }
       //TODO if (action=STATE_ATACK); - save old command

    }
    return 0; // signaling: OK
}


//@Override
//public boolean unitDestroyed(Unit unit, Unit attacker) {
//    if (contains(unit)) {
//        return removeUnit(unit); // removeUnit already contain check on "slowUnit"
//    }
//    return false;
//}


@Override
public int unitIdle(Unit unit) {
    if (unit==slowUnit && moveTarget!=null) {
        // FIXME !!!!!!!!!!     public static final int STATE_SCOUT=3; // send all idle units to any points

       if (action!=STATE_SCOUT || action!=STATE_IDLE)
        moveTarget=null; // failed move to this target
        this.updateUnitCommands();
        onCommandFinished(); // !!!
    }
    return 0; // signaling: OK
}

@Override
public int unitMoveFailed(Unit unit) {
    if (unit.getCurrentCommands().isEmpty()) {  // !!!!!!!!!!!! TEST
       if (action!=STATE_SCOUT || action!=STATE_IDLE)
        if (unit==slowUnit && moveTarget!=null) {
            moveTarget=null; // failed move to this target
            this.updateUnitCommands();
            onCommandFinished(); // !!!
        }
    }
    return 0; // signaling: OK
}


@Override
public int unitDamaged(Unit unit, Unit attacker, float damage, AIFloat3 dir, WeaponDef weaponDef, boolean paralyzed) {
    if (contains(unit)) {
        // TODO check 'dir'
        if (action!=STATE_SCOUT && action!=STATE_ATACK) {
            AIFloat3 agressorPoint=null; //dir
            if (attacker!=null) agressorPoint=attacker.getPos();
            if (agressorPoint!=null) {
                action=STATE_ATACK;// TODO save old command
                fightTo(agressorPoint);
            }
        }
    }
    return 0; // signaling: OK
}


}
