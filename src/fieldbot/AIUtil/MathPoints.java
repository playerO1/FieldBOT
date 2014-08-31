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

package fieldbot.AIUtil;

import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.Unit;
import com.springrts.ai.oo.clb.UnitDef;

/**
 *
 * @author user2
 */
public class MathPoints {

    // ==================================
    // --------- утилиты ----------
    /**
     * Проверяет действительность точки
     * @param p
     * @return
     */
    public static boolean isValidPoint(AIFloat3 p) {
        if (p == null) return false;
        if (p.x < 0 || p.z < 0) return false;
        // TODO размер карты учесть x>width, z>height....
        return true;
    }

    public static AIFloat3 getPointInMiddle(AIFloat3 p1, AIFloat3 p2) {
        return new AIFloat3((p1.x + p2.x) / 2, (p1.y + p2.y) / 2, (p1.z + p2.z) / 2);
    }

    /**
     * Расстояние между двумя точками на плоскости.
     */
    public static float getDistanceBetweenFlat(AIFloat3 p1, AIFloat3 p2) {
        float dx = p1.x - p2.x;
        float dz = p1.z - p2.z;
        return (float) Math.sqrt(dx * dx + dz * dz);
    }

    /**
     * Расстояние между двумя точками 3D.
     */
    public static float getDistanceBetween3D(AIFloat3 p1, AIFloat3 p2) {
        double dx = p1.x - p2.x;
        double dy = p1.y - p2.y;
        double dz = p1.z - p2.z;
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public static float getRadiusFlat(UnitDef def) {
        return def.getRadius();
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

    public static AIFloat3 getRandomPointInRadius(AIFloat3 center,float r) {
        AIFloat3 p = new AIFloat3(center);
        double ang = Math.random() * 2 * Math.PI;
        p.x += r * Math.sin(ang);
        p.z += r * Math.cos(ang);
        return p;
    }

    /**
     *
     * @param p1 откуда двигаться
     * @param targetPoint куда двигаться
     * @param targetDist на какой радиус подойти к цели
     * @return точка на радиусе цели
     */
    public static AIFloat3 getNearBetweenPoint(AIFloat3 p1, AIFloat3 targetPoint, float targetDist) {
        float L = getDistanceBetween3D(p1, targetPoint);
        float k = 0.0F;
        if (L != 0) k = targetDist / L;
        AIFloat3 p = new AIFloat3(p1.x - (targetPoint.x - p1.x) * k, p1.y - (targetPoint.y - p1.y) * k, p1.z - (targetPoint.z - p1.z) * k);
        return p;
    }
    
}
