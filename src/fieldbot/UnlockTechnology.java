/*
 * Copyright (C) 2014 user2
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

import com.springrts.ai.oo.clb.OOAICallback;
import com.springrts.ai.oo.clb.UnitDef;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

/**
 *
 * @author PlayerO1
 */
public class UnlockTechnology {
    /**
     * List of required for this technolody units, they unlock new units
     */
    public final HashSet<UnitDef> requiredBuildings;
    //public final Integer requiredTechLevel=null;
    
    /**
     * List for units that defauld is locked, but unlocked if ths technology was exist.
     */
    public final HashSet<UnitDef> lockUnits;

    private int numOfRequiredBuildingsHave;
    
    public UnlockTechnology(HashSet<UnitDef> requiredBuildings, HashSet<UnitDef> lockUnits) {
        this.requiredBuildings=requiredBuildings;
        this.lockUnits=lockUnits;
        numOfRequiredBuildingsHave=0;
    }
    public UnlockTechnology(UnitDef[] requiredBuildings, UnitDef[] lockUnits) {
        if (requiredBuildings!=null) {
            this.requiredBuildings=new HashSet<UnitDef>();
            for (int i=0; i<requiredBuildings.length; i++) this.requiredBuildings.add(requiredBuildings[i]);
        } else this.requiredBuildings=null;
        if (lockUnits!=null) {
            this.lockUnits=new HashSet<UnitDef>();
            for (int i=0; i<lockUnits.length; i++) this.lockUnits.add(lockUnits[i]);
        } else this.lockUnits=null;
        numOfRequiredBuildingsHave=0;
    }
    public UnlockTechnology(OOAICallback aiClb,String[] requiredBuildings, String[] lockUnits) {
        if (requiredBuildings!=null) {
            this.requiredBuildings=new HashSet<UnitDef>();
            for (int i=0; i<requiredBuildings.length; i++) {
                UnitDef uDef=aiClb.getUnitDefByName(requiredBuildings[i]);
                if (uDef!=null) this.requiredBuildings.add(uDef);
                // else
            }
        } else this.requiredBuildings=null;
        if (lockUnits!=null) {
            this.lockUnits=new HashSet<UnitDef>();
            for (int i=0; i<lockUnits.length; i++) {
                UnitDef uDef=aiClb.getUnitDefByName(lockUnits[i]);
                if (uDef!=null) this.lockUnits.add(uDef);
                // else
            }
        } else this.lockUnits=null;
        numOfRequiredBuildingsHave=0;
    }
    
    /**
     * Check current unit for unclock this technology.
     * @return true if you can build locked unit now, or false - if you don't have required units (for example not have research center)
     */
    public boolean unlocked() {
        //if (numOfRequiredBuildingsHave<0) error!
        return numOfRequiredBuildingsHave>0;
    }
    
    public boolean unlockedBy(Collection<UnitDef> byUnit) {
        if (requiredBuildings!=null)
          for (UnitDef def:byUnit) if (requiredBuildings.contains(def)) return true;
        return false;
    }
    
    // for Register lock/unlock on real time
    public void onAddUnit(UnitDef newUnit) {
        if (requiredBuildings!=null)
          if (requiredBuildings.contains(newUnit)) numOfRequiredBuildingsHave++;
    }
    public void onRemoveUnit(UnitDef oldUnit) {
        if (requiredBuildings!=null)
          if (requiredBuildings.contains(oldUnit)) numOfRequiredBuildingsHave--;
    }
}
