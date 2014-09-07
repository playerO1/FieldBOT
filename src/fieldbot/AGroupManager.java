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
import com.springrts.ai.oo.clb.UnitDef;
import com.springrts.ai.oo.clb.WeaponDef;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

/**
 *
 * @author PlayerO1
 */
public abstract class AGroupManager {

    
    
    
   /**
    * Base type
    * 0 - main, economic
    * 1 - war group, army or war base
    */
   public int baseType=0; // TODO specification
    
    /**
     * Add unit in group
     * @param unit
     * @return acepted or no acepted
     */
    public abstract boolean addUnit(Unit unit);
    
    public boolean addAllUnits(Collection<Unit> units) {
        boolean modifed=false;
        for (Unit u:units) {
            boolean m=addUnit(u);
            modifed = modifed || m;
        }
        return modifed;
    }
    
    /**
     * Remove unit from this group
     * @param unit
     * @return removed or no removed
     */
    public abstract boolean removeUnit(Unit unit);

    public boolean removeAllUnits(Collection<Unit> units) {
        boolean modifed=false;
        for (Unit u:units) {
            boolean m=removeUnit(u);
            modifed = modifed || m;
        }
        return modifed;
    }

    /**
     * Do contain this unit in list
     * @param unit
     * @return 
     */
    public abstract boolean contains(Unit unit);
    
    /**
     * Do this unit need to this group
     * @return 
     */
    public abstract float doYouNeedUnit(Unit unit);
    
    /**
     * Return all units that contain this group
     * @param selectWhoCanNow - true = select only non working buidlers, no paralyzed and no in behin building.
     * @return 
     */
    public abstract ArrayList<Unit> getAllUnits(boolean selectWhoCanNow);
   
    /**
     * Do contain some units or no
     * @return 
     */
    public boolean isEmpty() {
        return getAllUnits(false).isEmpty();
    }
    
    /**
     * Return list of all unit type of all unit in group
     * @param onlyWhatIdle only what not work and not behin build
     * @return 
     */
    public HashSet<UnitDef> getContainUnitDefsList(boolean onlyWhatIdle) {
        HashSet<UnitDef> currentUnitTypesSet=new HashSet<UnitDef>(); // Unit types
        for (Unit builder:getAllUnits(onlyWhatIdle)) currentUnitTypesSet.add(builder.getDef());
        
        return currentUnitTypesSet;
        // TODO кэшировать вывод
        
    }
    

    
    
    // Command, sending from bot, from Spring.
    
    //@Override
    public int update(int frame) {
        return 0;
    }
    
   //@Override
    public int unitCreated(Unit unit, Unit builder) {
        return 0;
    }
    
    //@Override
    public int unitFinished(Unit unit) {
        return 0;
    }
    
    //@Override
    public int unitIdle(Unit unit) {
        return 0; // signaling: OK
    }
    
    //@Override
    public int unitMoveFailed(Unit unit) {
        return 0; // signaling: OK
    }

    //@Override
    public int unitDamaged(Unit unit, Unit attacker, float damage, AIFloat3 dir, WeaponDef weaponDef, boolean paralyzed) {
        return 0; // signaling: OK
    }

    //@Override
    public boolean unitDestroyed(Unit unit, Unit attacker) {
        return false;
    }
    
    //@Override
    public boolean unitGiven(Unit unit, int oldTeamId, int newTeamId) {
        return false;
    }

    //@Override
    public int unitCaptured(Unit unit, int oldTeamId, int newTeamId) {
        return 0; // signaling: OK
    }

    //@Override
    public int enemyEnterLOS(Unit enemy) {
        return 0; // signaling: OK
    }

    //@Override
    public int enemyLeaveLOS(Unit enemy) {
        return 0; // signaling: OK
    }

    //@Override
    public int enemyEnterRadar(Unit enemy) {
        return 0; // signaling: OK
    }

    //@Override
    public int enemyLeaveRadar(Unit enemy) {
        return 0; // signaling: OK
    }

    //@Override
    public int enemyDamaged(Unit enemy, Unit attacker, float damage, AIFloat3 dir, WeaponDef weaponDef, boolean paralyzed) {
        return 0; // signaling: OK
    }

    //@Override
    public int enemyDestroyed(Unit enemy, Unit attacker) {
        return 0; // signaling: OK
    }

    //@Override
    public int weaponFired(Unit unit, WeaponDef weaponDef) {
        return 0; // signaling: OK
    }

    //@Override
    public int playerCommand(java.util.List<Unit> units, int commandTopicId, int playerId) {
        return 0; // signaling: OK
    }

    //@Override
    public int commandFinished(Unit unit, int commandId, int commandTopicId) {
        return 0; // signaling: OK
    }

    //@Override
    public int seismicPing(AIFloat3 pos, float strength) {
        return 0; // signaling: OK
    }

    //@Override
    public int load(String file) {
        return 0; // signaling: OK
    }
    //@Override
    public int save(String file) {
        return 0; // signaling: OK
    }

    //@Override
    public int enemyCreated(Unit enemy) {
        return 0; // signaling: OK
    }

    //@Override
    public int enemyFinished(Unit enemy) {
        return 0; // signaling: OK
    }

}
