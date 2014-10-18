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
import fieldbot.AIUtil.MathPoints;
import java.util.ArrayList;
import java.util.Collection;

/**
 *
 * @author PlayerO1
 */
public class TArmyGroup extends AGroupManager{
    
    public ArrayList<Unit> army;
    public Unit slowUnit;
    
    public int tactic;
    public AIFloat3 moveTarget; // TODO non-line moving road.
    
    
    public TArmyGroup(FieldBOT owner) {
        super(owner); //this.owner=owner;
        baseType=1;
        army=new ArrayList<Unit>();
        slowUnit=null;
        
        tactic=0;
        moveTarget=null;
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
        return moveTarget!=null;
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
          if (slowUnit!=null) {
            // send command
            slowUnit.fight(moveTarget, (short)0, Integer.MAX_VALUE);
            for (Unit au:army) if (au!=slowUnit) au.guard(slowUnit, (short)0, Integer.MAX_VALUE);
      owner.sendTextMsg("Moving army: folow my slow unit",FieldBOT.MSG_DBG_ALL);
          } else {
            for (Unit au:army) if (au!=slowUnit) au.fight(MathPoints.getRandomPointInCircle(moveTarget, 20), (short)0, Integer.MAX_VALUE);
      owner.sendTextMsg("Moving army: all by fight",FieldBOT.MSG_DBG_ALL);
          }
        }
    }
    
    public void moveTo(AIFloat3 target) {
        this.moveTarget=target;
        // send command
        updateUnitCommands();
    }
    
    @Override
    public int update(int frame) {
        if (frame%5==0) {
            if (moveTarget!=null && slowUnit!=null) {
                if (MathPoints.getDistanceBetween3D(moveTarget, slowUnit.getPos())<1) {
                    moveTarget=null; // finish.
                    onCommandFinished();
                }
//                    }) {
//                // send command
//                slowUnit.fight(moveTarget, (short)0, Integer.MAX_VALUE);
//                for (Unit au:army) if (au!=slowUnit) au.guard(slowUnit, (short)0, Integer.MAX_VALUE);
            }
        }
        return 0;
    }
    
    
    
    // TODO method call -------------------------

@Override
public int commandFinished(Unit unit, int commandId, int commandTopicId) {
    if (unit.getCurrentCommands().isEmpty()) {
        if (unit==slowUnit && moveTarget!=null) {
            moveTarget=null; // failed move to this target
            updateUnitCommands();
            onCommandFinished(); // !!!
        }
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
        moveTarget=null; // failed move to this target
        this.updateUnitCommands();
        onCommandFinished(); // !!!
    }
    return 0; // signaling: OK
}

@Override
public int unitMoveFailed(Unit unit) {
    if (unit.getCurrentCommands().isEmpty()) {  // !!!!!!!!!!!! TEST
        if (unit==slowUnit && moveTarget!=null) {
            moveTarget=null; // failed move to this target
            this.updateUnitCommands();
            onCommandFinished(); // !!!
        }
    }
    return 0; // signaling: OK
}




}
