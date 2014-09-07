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

import java.util.Arrays;

/**
 *
 * @author PlayerO1
 */
public class TechUpTrigger {
    /**
     * To level
     */
    public final TTechLevel level;
    /**
     * Time for start tech up from the time when this trigger was created.
     * If = 0 then need do now.
     */
    public final float timeBefore;
    /**
     * Time for build this level
     */
    public final float timeForBuildLevel;
    /**
     * Do tech up on this base
     */
    public TBase onBase;
    /**
     * Trigger resource. resource[i] = current[i] + timeForBuildLevel*income[i].
     * Current resource must be more or equals that this value.
     */
    public final float[] triggerRes;
    
    /**
     * for access to economic state.
     */
    private final FieldBOT bot;
    
    public TechUpTrigger(FieldBOT owner, TTechLevel level,float timeBefore, float timeForBuildLevel,TBase onBase, float[] triggerRes) {
        bot=owner;
        this.level=level;
        this.timeBefore=timeBefore;
        this.timeForBuildLevel=timeForBuildLevel;
        this.onBase=onBase;
        this.triggerRes=triggerRes;
    }
    
    /**
     * Check condition for execute tech up now
     * @return true if need do it now.
     */
    public boolean trigger() {
        float[] currRes=bot.avgEco.getAvgCurrentFromTime(timeForBuildLevel, bot.ecoStrategy.useOptimisticResourceCalc);
//        if (timeBefore<1) return true; // если время - сейчас
        // или есть ресурсы
        for (int i=0; i<triggerRes.length; i++) if (triggerRes[i]>currRes[i]) return false;
        return true;
    }
    
    @Override
    public String toString() {
        return "Trigger resources: "+Arrays.toString(triggerRes)+
                ", must >current+income*lvlBuildTime . time to build = "+timeForBuildLevel+", time before ="+timeBefore+
                " to level: "+level+". On base: "+onBase;
    }
}
