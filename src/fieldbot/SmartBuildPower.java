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

import com.springrts.ai.oo.clb.Unit;
import com.springrts.ai.oo.clb.UnitDef;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * This class contain advanced method for work with assistable and not assistable BuildPower
 * Contain advanced function for work with much no assistable factorys.
 * Required too many resources, memory and CPU at init, that simple TBase.getBuildPower().
 * @author PlayerO1
 */
public class SmartBuildPower {
    
    protected float assistBP; // summary of all worker, nano tower, NOT factory.
    protected int numOfAssistUnit; // count of all worker and nano tower.
    
    protected float assistableFactoryBP; // summary of only assistable factory
    protected int numOfAssistableFactory; //count of assistable build factory
    
    protected float noAssistableFactoryBP; // summary of only NO assistable factory (and no assistable unit)
    protected int numOfNoAssistableFactory; //count of NO assistable build factory
    
    /**
     * For no assistable builders, and assistable builders
     * (factory/worker ; count)
     * Not need contain worker with buildOptions.
     */
    protected final HashMap<UnitDef,Integer> builderCount;
    
    /**
     * For no assistable units only
     * (build unit type ; builder)
     */
    protected final HashMap<UnitDef,UnitDef> noAssistableUnit; // TODO can be more that 1 of builder type for unit! Change to <UnitDef,List<UnitDef>>.
    
    /**
     * For assistable units, build on factory (factory can not assist to other unit)
     * (build unit type ; builder)
     */
    protected final HashMap<UnitDef,UnitDef> assistableUnit; // TODO can be more that 1 of builder type for unit! Change to <UnitDef,List<UnitDef>>.

    protected final HashSet<UnitDef> freeBuildUnit; // build unit by other movable and assistable unit (by not factory)
    
    //TODO protected final HashMap<UnitDef,Float> BuilderBuildPower; //for fast build power of all unit;
    
    /**
     * Init SmartBuildPower for current units.
     * @param unitList units, that can using for assist or/and build. It required for check unit count and build power.
     */
    public SmartBuildPower(List<Unit> unitList)
    {   //init data structures
        numOfAssistUnit=numOfAssistableFactory=numOfNoAssistableFactory=0;
        assistBP=assistableFactoryBP=noAssistableFactoryBP=0.0f;
        
        builderCount=new HashMap<UnitDef, Integer>();
        noAssistableUnit=new HashMap<UnitDef, UnitDef>();
        assistableUnit=new HashMap<UnitDef, UnitDef>();
        freeBuildUnit=new HashSet<UnitDef>();
        
        for (Unit bldr:unitList) // analizing units
        {
            UnitDef bldDef=bldr.getDef();
            float unitBP=bldDef.getBuildSpeed(); // cashe of BuildPower
            
            //TODO isBuilder? is unitBP>0?
            
            if (ModSpecification.isRealyAbleToAssist(bldDef)) {
                assistBP+=unitBP;
                numOfAssistUnit++;
            }
            List<UnitDef> buildOptions=bldDef.getBuildOptions();// cashe
            if (!buildOptions.isEmpty()) { // this is builder/factory, can build other unit?
                
                // Increase builder count
                if (builderCount.containsKey(bldDef)) {
                    Integer count=builderCount.get(bldDef);
                    builderCount.put(bldDef, count+1); // Inc count
                } else builderCount.put(bldDef, 1); // first
                
                if (ModSpecification.isStationarFactory(bldDef)) {
                    if (bldDef.isAssistable()) {
                        assistableFactoryBP+=unitBP;
                        numOfAssistableFactory++;
                        for (UnitDef bUD:buildOptions) {
                            if (!assistableUnit.containsKey(bUD)) assistableUnit.put(bldDef, bUD);// add factory for unit
                            // TODO and check old and this factory type; else check other factory
                        }
                    } else { // not assistable
                        noAssistableFactoryBP+=unitBP;
                        numOfNoAssistableFactory++;
                        for (UnitDef bUD:buildOptions) {
                            if (!noAssistableUnit.containsKey(bUD)) noAssistableUnit.put(bldDef, bUD);// add factory for unit
                            // TODO and check old and this factory type; else check other factory
                        }
                    }
                } else { // if not factory, then can assist...
                    if (bldDef.isAssistable()) {
                        for (UnitDef bUD:buildOptions) 
                            freeBuildUnit.add(bUD);
                    } // TODO else ?
                }
            }
        }
    }
    
    /**
     * Return build power of all worker, without factorys.
     * @return build power
     */
    public float getAssistBuildPower() {
        return assistBP;
    }
    /**
     * Return build power of all worker + factorys.
     * @return build power
     */
    public float getFullAssistBuildPower() {
        return assistBP+assistableFactoryBP;
    }
    
    /**
     * Return all build power. Assistable and not assistable.
     * @return build power
     */
    public float getAllBuildPower() {
        return assistBP+assistableFactoryBP+noAssistableFactoryBP;
    }
    
    /**
     * This unit can be take assist in build.
     * @param uDef build unit
     * @return can be other assist to build this unit.
     */
    public boolean isAssistable(UnitDef uDef) {
        return (noAssistableUnit==null) || noAssistableUnit.containsKey(uDef);
    }
    /**
     * This unit build on factory, or can build on some other walking units?
     * @param uDef build unit
     * @return true for unit in factory buildOptions
     */
    public boolean isBuildOnFactory(UnitDef uDef) {
        return (noAssistableUnit!=null) && noAssistableUnit.containsKey(uDef)
               ||
               (assistableUnit!=null) && assistableUnit.containsKey(uDef);
    }
    
    /**
     * Return true build power for asistable and not assistable unit.
     * Check factorys, assistable/non assistable, and workers build power.
     * For many factory do build power from only once factory. See also getTrueBuildPower().
     * @param uDef build this unit
     * @return build power
     */
    public float getTrueBuildPower_once(UnitDef uDef) {
        UnitDef builder=noAssistableUnit.get(uDef);
        if (builder==null) {
            builder=assistableUnit.get(uDef);
            if (builder==null) return getAssistBuildPower();//!!!
            float factoryBP=builder.getBuildSpeed();
            return assistBP + factoryBP; // getAssistBuildPower()=assistBP
        } else {
            float factoryBP=builder.getBuildSpeed();
            return factoryBP;
        }
    }
    
    /**
     * Return true build power for asistable and not assistable unit.
     * Check factorys, assistable/non assistable, and workers build power.
     * For many factory do summ of all build power. For get true build power for only one unit use getTrueBuildPower_once().
     * @param uDef build this unit
     * @return build power
     */
    public float getTrueBuildPower(UnitDef uDef) {
        UnitDef builder=noAssistableUnit.get(uDef);
        if (builder==null) {
            builder=assistableUnit.get(uDef);
            if (builder==null) return getAssistBuildPower();//!!!
            float factoryBP=builder.getBuildSpeed() * builderCount.get(builder);
            return assistBP + factoryBP; // getAssistBuildPower()=assistBP
        } else {
            float factoryBP=builder.getBuildSpeed() * builderCount.get(builder);
            return factoryBP;
        }
    }

    //TODO for Simplex optimization build power depends bounds matrix
    // see TWarStrategy
    /**
     * @param forUnit list of unit for analyzer
     * @return [mType][unitID][resID], mType=0 for coast of 1 unit, mType=1 for limited build power,
     */
    //public float[][][] getMatrixBuildPower(List<UnitDef> forUnit);
    
}
