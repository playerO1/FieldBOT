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

import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.Unit;
import com.springrts.ai.oo.clb.UnitDef;

/**
 *
 * @author PlayerO1
 */
public class MathPoints {

    /**
     * Check truing point
     * @param p
     * @return
     */
    public static boolean isValidPoint(AIFloat3 p) {
        if (p == null) return false;
        if (p.x < 0 || p.z < 0) return false;
        // TODO check map size: x>width, z>height....
        return true;
    }

    public static AIFloat3 getPointInMiddle(AIFloat3 p1, AIFloat3 p2) {
        return new AIFloat3((p1.x + p2.x) / 2, (p1.y + p2.y) / 2, (p1.z + p2.z) / 2);
    }

    /**
     * Distnace between 2 point on flat
     */
    public static float getDistanceBetweenFlat(AIFloat3 p1, AIFloat3 p2) {
        float dx = p1.x - p2.x;
        float dz = p1.z - p2.z;
        return (float) Math.sqrt(dx * dx + dz * dz);
    }

    /**
     * Distance between 2 point 3D.
     */
    public static float getDistanceBetween3D(AIFloat3 p1, AIFloat3 p2) {
        double dx = p1.x - p2.x;
        double dy = p1.y - p2.y;
        double dz = p1.z - p2.z;
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public static float getRadiusFlat(UnitDef def) {
        float w=def.getXSize()*8; // TODO TEST!
        float h=def.getZSize()*8;
        return (float)Math.sqrt(w*w+h*h); //last: def.getRadius();
    }

    public static float getRadiusFlat(Unit u) {
        return getRadiusFlat(u.getDef());
    }

    public static AIFloat3 getNearRandomPoint(Unit u) {
        AIFloat3 p = u.getPos();
        float r = u.getDef().getRadius() * 3.5F + u.getDef().getSpeed() * (4.5F + (float) Math.random());
        double ang = Math.random() * 2 * Math.PI;
        p.x += r * Math.sin(ang);
        p.z += r * Math.cos(ang);
        return p;
    }

    /**
     * 
     * @param center center of circle
     * @param r fixed radius
     * @return 
     */
    public static AIFloat3 getRandomPointOnRadius(AIFloat3 center,float r) {
        AIFloat3 p = new AIFloat3(center);
        double ang = Math.random() * 2 * Math.PI;
        p.x += r * Math.sin(ang);
        p.z += r * Math.cos(ang);
        return p;
    }

    /**
     * 
     * @param center center of circle
     * @param r max radius
     * @return 
     */
    public static AIFloat3 getRandomPointInCircle(AIFloat3 center,float r) {
        AIFloat3 p = new AIFloat3(center);
        double ang = Math.random() * 2 * Math.PI;
        double rr=r*Math.random();
        p.x += rr * Math.sin(ang);
        p.z += rr * Math.cos(ang);
        return p;
    }

    /**
     *
     * @param p1 walking from
     * @param targetPoint walking to
     * @param targetDist length from targetPoint - radius
     * @return point on radius on target
     */
    public static AIFloat3 getNearBetweenPoint(AIFloat3 p1, AIFloat3 targetPoint, float targetDist) {
        float L = getDistanceBetween3D(p1, targetPoint);
        float k = 0.0F;
        if (L != 0) k = targetDist / L;
        AIFloat3 p = new AIFloat3(targetPoint.x - (targetPoint.x - p1.x) * k, targetPoint.y - (targetPoint.y - p1.y) * k, targetPoint.z - (targetPoint.z - p1.z) * k);
        return p;
    }
    
}
