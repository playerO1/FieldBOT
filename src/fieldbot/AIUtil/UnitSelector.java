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

package fieldbot.AIUtil;

import com.springrts.ai.oo.clb.CommandDescription;
import com.springrts.ai.oo.clb.Unit;
import com.springrts.ai.oo.clb.UnitDef;
import fieldbot.FieldBOT;
import fieldbot.ModSpecification;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * This class contain some function for select UnitDef and Unit from list.
 * @author PlayerO1
 */
public class UnitSelector {
    
    
    /*
    TODO Some method was contain on other class. Need move to this class.
    */
    
    /**
     * Select only movable units, compare by ModSpecification.isRealyAbleToMove(def)
     * @param selectFrom
     * @return movable units, or empty list.
     */
    public static ArrayList<UnitDef> movableUnits(List<UnitDef> selectFrom) {
        ArrayList<UnitDef> movableUnits=new ArrayList<UnitDef>();
        for (UnitDef def:selectFrom) if (ModSpecification.isRealyAbleToMove(def))
            movableUnits.add(def);
        return movableUnits;
    }
    
    /**
     * Select only non-movable units, compare by !ModSpecification.isRealyAbleToMove(def)
     * @param selectFrom
     * @return stationary units, or empty list.
     */
    public static ArrayList<UnitDef> stationarUnits(List<UnitDef> selectFrom) {
        ArrayList<UnitDef> stationaryUnits=new ArrayList<UnitDef>();
        for (UnitDef def:selectFrom) if (!ModSpecification.isRealyAbleToMove(def))
            stationaryUnits.add(def);
        return stationaryUnits;
    }
    
    /**
     * Select only able to atack units.
     * @param selectFrom
     * @return ableToAtack units, or empty list.
     */
    public static ArrayList<UnitDef> atackUnits(List<UnitDef> selectFrom) {
        ArrayList<UnitDef> atackUnits=new ArrayList<UnitDef>();
        for (UnitDef def:selectFrom) if (def.isAbleToAttack() && !ModSpecification.isStationarFactory(def))
            atackUnits.add(def);
        return atackUnits;
    }
    
    /**
     * Select unit from allUnits where unit.getDef()==selectOnly;
     * @param allUnits list for select
     * @param selectOnly what unitDef select in list
     * @return units with selectOnly UnitDef
     */
    public static ArrayList<Unit> selectUnitWithDef(ArrayList<Unit> allUnits, UnitDef selectOnly) {
        ArrayList<Unit> selected = new ArrayList<Unit>();
        for (Unit unit : allUnits) {
            if (selectOnly.equals(unit.getDef())) {
                selected.add(unit);
            }
        }
        return selected;
    }

    /**
     * Return list of all unit type of all unit in list in argument
     * This method is copy and modifed from AGroupManager.getContainUnitDefsList() TODO share using!
     * @param fromUnits select UnitDef from this unit list
     * @return list of UnitDefs
     */
    public static HashSet<UnitDef> getUnitDefsFromUnitList(List<Unit> fromUnits) {
        HashSet<UnitDef> currentUnitTypesSet = new HashSet<UnitDef>();
        for (Unit unit : fromUnits) {
            currentUnitTypesSet.add(unit.getDef());
        }
        return currentUnitTypesSet;
    }
    
    // ----------
    
    
    // --- Export from other class ---
    
    // ModSpecification.getRealBuildList()
    // ModSpecification.selectDefByTechLevel()
    @Deprecated
    public static ArrayList<UnitDef> selectDefByTechLevel(ArrayList<UnitDef> defs,int level) {
        return ModSpecification.selectDefByTechLevel(defs, level); // Export, TODO move to this file, and remove on mod specification.
    }
    
    /**
     * Select unit, who can morph
     * @param units
     * @param selectDisabledToo select unit, where can morph, but now can not morph (no have XP, or some other reason for disabled morph)
     * @return 
     */
    public static ArrayList<Unit> selectMorphingUnits(ArrayList<Unit> units,boolean selectDisabledToo)
    {
        ArrayList<Unit> morphingUnits=new ArrayList<Unit>();
        for (Unit u:units) {
            List<CommandDescription> morphLst=ModSpecification.getMorphCmdList(u, selectDisabledToo);
            if (!morphLst.isEmpty()) morphingUnits.add(u);
        }
        return morphingUnits;
    }
    

    // TBOTTalking.findUnitDefByHumanQuery()
    // TBOTTalking.selectUnitInArea_myOnly()
    // TBOTTalking.selectUnitInArea_myOnly_near()
    
    // TBase.canBuildOnBase()
    // TBase.getBuildList() ?
    // TBase.getBuildListFor()
    // TBase.getBuildPower() ?
    // TBase.getContainUnitDefsList()
    //TBase.getBuildList()

    /**
     * Get all build options from all unit types.
     * Do not check canBuildOnBaseSurface and don't check unlock system (tech level and research center depends)
     * If you want get it from TBase, please use TBase.getBuildList() - it more important for CPU optimization and cashes.
     * 
     */
    HashSet<UnitDef> getAllBuildOptions(HashSet<UnitDef> currentUnitTypesSet) {
        HashSet<UnitDef> lst=new HashSet<UnitDef>();
        for (UnitDef unitType:currentUnitTypesSet)
            lst.addAll(unitType.getBuildOptions());
        return lst;
    }
    /**
     * Get all build options from all units.
     * Do not check canBuildOnBaseSurface and don't check unlock system (tech level and research center depends)
     * If you want get it from TBase, please use TBase.getBuildList() - it more important for CPU optimization and cashes.
     * 
     */
    HashSet<UnitDef> getAllBuildOptions(List<Unit> currentUnits) {
        HashSet<UnitDef> unitTypes=new HashSet<UnitDef>();
        for (Unit u:currentUnits) unitTypes.add(u.getDef());
        HashSet<UnitDef> lst=new HashSet<UnitDef>();
        for (UnitDef unitType:unitTypes)
            lst.addAll(unitType.getBuildOptions());
        return lst;
    }
    
    /**
     * Get only locked unit on list with all build options
     * @param owner for use ModSpecification API
     * @param buildSet list of all unit that can build, with depends level unit and lock unit
     * @return set with lock unit who depends special level or unit. Or null - if mod not support it. or no unit in list....
     */
    HashSet<UnitDef> getDependsFrom_sepcialUnlock(FieldBOT owner, HashSet<UnitDef> buildSet) {
        if (owner.modSpecific.specificUnitEnabled) return null;//!!!!!
        HashSet<UnitDef> noRequiredUList=(HashSet)buildSet.clone();
        if (owner.modSpecific.removeUnitWhenCantBuildWithTeclLevel(noRequiredUList))
        {
            HashSet<UnitDef> diffLst=(HashSet)buildSet.clone();
            diffLst.removeAll(noRequiredUList);
            return diffLst;
        }
        return null;
    }
    //TODO list for unlock units.

    
    // TEcoStrategy.getBestGeneratorRes()
    // TEcoStrategy.getOptimalBuildingForNow()
    // TEcoStrategy.selectLvl_bestForNow() ?
    // TEcoStrategy.selectLvl_whoBetterThat() ?
    // TEcoStrategy.select_EconomicUDefs()

    // TTechLevel. ... it is big selector
    // TTechLevel.getMaxAtackDamage
    // TTechLevel.getMaxAtackRange
    // TTechLevel.getMaxBuildPower
    // TTechLevel.getMaxResProduct
    // TTechLevel.getMaxSpeed
    // TTechLevel.
    // TTechLevel.

    // Can be add to:
    // select unit for build base with: ModSpecification.isAbleToBuildBase()
    // select factorys, select can using with: ModSpecification.isStationarFactory(), but it is may be not all factorys.
    
    // --- Group selector ---
    // FieldBOT.selectBestBase()
    // FieldBOT.selectTBasesList()
    // FieldBOT.getOwnerGroup()
    // FieldBOT.getNearGroup()
    // FieldBOT.getNearBase()
    
    
}
