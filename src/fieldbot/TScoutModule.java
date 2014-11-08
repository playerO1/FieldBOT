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
import com.springrts.ai.oo.clb.Map;
import com.springrts.ai.oo.clb.Unit;
import com.springrts.ai.oo.clb.UnitDef;
import com.springrts.ai.oo.clb.WeaponDef;
import fieldbot.AIUtil.MathPoints;
import java.util.Arrays;
import java.util.LinkedList;

/**
 *
 * @author PlayerO1
 */
public class TScoutModule {
    
    private final FieldBOT owner;
    private final Map map;
    
    public final float mapWidth, mapHeight;
    
    public float[][] scountMap; // TODO use scountMap!
    public final float smCellWidth, smCellHeight;
    
    /**
     * Watching enemy points.
     */
    public LinkedList<Unit> enemyUnits,enemyInLOS,enemyInRadar;
    
    /**
     * Last point with detect enemy. May be null.
     */
    public AIFloat3 last_enemyComeWatch;
    /**
     * Last point with enely leawe from wisible area, or dead. May be null.
     * not register, if enemy destroy
     */
    public AIFloat3 last_enemyLeaveWatch;
    public AIFloat3 lastEnemmyBuildings;
    
    public TScoutModule(FieldBOT owner) {
        this.owner=owner;
        this.map=owner.clb.getMap();
        mapWidth=map.getWidth()*8; // TEST !!!!
        mapHeight=map.getHeight()*8;
        
        smCellWidth=Math.min(mapWidth/30, 50);
        smCellHeight=Math.min(mapHeight/30, 50);
        scountMap=new float[Math.round(mapWidth/smCellWidth)][Math.round(mapHeight/smCellHeight)];
        for (int i=0; i<scountMap.length; i++) Arrays.fill(scountMap[i], 0.0f);
        owner.sendTextMsg("Create TScoutManager, map index size="+scountMap.length+"x"+scountMap[0].length, FieldBOT.MSG_DBG_SHORT);
        
        enemyUnits=new LinkedList<Unit>();
        enemyInLOS=new LinkedList<Unit>();
        enemyInRadar=new LinkedList<Unit>();
        
        last_enemyComeWatch=last_enemyLeaveWatch=lastEnemmyBuildings=null;
    }
    
    
    
    /**
     * get point, recomended for scout
     * @param nearTo search near from this point, can be null
     * @param pointID take some random to find, if send scout to different point, default = 0
     * @return point to send scout
     */
    public AIFloat3 getPointToScout(AIFloat3 nearTo,int pointID) {
        // TODO
        float x=(float)Math.random() *mapWidth;
        float y=(float)Math.random() *mapHeight;
        return new AIFloat3(x, map.getElevationAt(x, y) , y);
    }
    
    /**
     * get point, recomended for atack
     * @param nearTo search near from this point, can be null
     * @param pointID take some random to find, if send scout to different point, default = 0
     * @return point to send army, can be null
     */
    public AIFloat3 getPointForMoveArmy(AIFloat3 nearTo,int pointID) {
        if (Math.random()>0.6) {
            if (last_enemyLeaveWatch!=null) return last_enemyLeaveWatch;
            if (last_enemyComeWatch!=null) return last_enemyComeWatch;
        }
        // TODO army seek point
        AIFloat3 p=getPointToScout(nearTo, pointID);
        if (Math.random()>0.6) p=getNearEnemy(nearTo, 260, true); // !!!!
        if (p==null || pointID>0 || Math.random()>0.7) {
            float x=(float)Math.random() *mapWidth;
            float y=(float)Math.random() *mapHeight;
            p=new AIFloat3(x, map.getElevationAt(x, y) , y);
        }
        return p;
    }
    
    /**
     * Find near point with enemy
     * @param nearTo search enemy near this point, if null then get some point.
     * @param inR max radius for point, 0 for all map
     * @param useLostWiew use non wisible point, only what last wiew. 
     * @return point, or null
     */
    public AIFloat3 getNearEnemy(AIFloat3 nearTo, float inR,boolean useLostWiew) {
        // TODO
        float L=Float.POSITIVE_INFINITY;
        AIFloat3 nearP=null;
        for (Unit u:enemyUnits) {
            AIFloat3 p=u.getPos();
            float l=MathPoints.getDistanceBetween3D(nearTo, p);
            if (inR<=0 || l<inR) if (nearP==null || l<L)
            {
                L=l;
                nearP=p;
            }
        }
        //if (nearP!=null) return nearP; else return new AIFloat3(last_enemyComeWatch);
        return nearP;
    }
    
// ---------------
    /**
     * 
     * @param unit
     * @param byLOS - for Spring 94.1 else maybe crash on access.
     * @return 
     */
    private boolean registerEnemy(Unit unit, boolean byLOS) {
        if (!enemyUnits.contains(unit)) {
            last_enemyComeWatch=unit.getPos();
            if (!MathPoints.isValidPoint(last_enemyComeWatch)
                || last_enemyComeWatch.x==0 && last_enemyComeWatch.y==0 && last_enemyComeWatch.z==0)
                last_enemyComeWatch=null;
            if (lastEnemmyBuildings==null && byLOS) {
                UnitDef ud=unit.getDef(); // FIXME on Spring 94 maybe take crash
                if (ud!=null && !ud.isAbleToMove())
                    lastEnemmyBuildings=last_enemyComeWatch;
            }
            // TODO
            return enemyUnits.add(unit);
        }
        return false;
    }
    /**
     * 
     * @param unit
     * @param byDestroy
     * @param byLOS - jnly for SPring 94.1
     * @return 
     */
    private boolean unRegisterEnemy(Unit unit, boolean byDestroy, boolean byLOS) {
        if (enemyUnits.contains(unit) && !byDestroy) {
            last_enemyLeaveWatch=unit.getPos();
            if (!MathPoints.isValidPoint(last_enemyLeaveWatch)
                || last_enemyLeaveWatch.x==0 && last_enemyLeaveWatch.y==0 && last_enemyLeaveWatch.z==0)
                last_enemyLeaveWatch=null;
            if (last_enemyLeaveWatch!=null && byLOS) {
                UnitDef ud=unit.getDef(); // FIXME on Spring 94 maybe take crash (radar)
                if (ud!=null && !ud.isAbleToMove())
                    lastEnemmyBuildings=last_enemyLeaveWatch;
            }
            //TODO
        }
        enemyInLOS.remove(unit);
        enemyInRadar.remove(unit);
        return enemyUnits.remove(unit);
    }
    private boolean hasRegistredEnemy(Unit unit) {
        return enemyUnits.contains(unit); // TODO ?enemyInLOS, enemyInRadar?
    }
// ---------------    

    /**
     * Convert coord from scoutMap int to map float 
     * @param x
     * @param y
     * @return 
     */
    private AIFloat3 getPointFromScountMap(int x,int y) {
        return new AIFloat3(x*smCellWidth, 0, y*smCellHeight);
    }
    
    /**
     * Convert coord from map float to scoutMap int
     * @param x on map
     * @param y on map
     * @return int[2]={x,y}
     */
    private int[] getPointToScountMap(float x, float y) {
        int ix=Math.max(Math.round(x/smCellWidth),0);
        int iy=Math.max(Math.round(y/smCellHeight),0);
        int ret[]={ix,iy};
        return ret;
    }
    /**
     * Convert coord from map float to scoutMap int
     * @return int[2]={x,y}
     */
    private int[] getPointToScountMap(AIFloat3 p) {
        return getPointToScountMap(p.x,p.z);
    }
    
// ---------------    

    
    // --- Game message ---
    //@Override
    public void update(int frame) {
        if (frame%15==5) {
            //TODO update
            //TODO USE:
//            owner.clb.getEnemyUnitsIn(last_enemyComeWatch, mapWidth);
//            owner.clb.getEnemyUnitsInRadarAndLos();
//            owner.clb.getEnemyUnits();
//            owner.clb.getNeutralUnitsIn(AIFloat3 pos, float radius)
//            owner.clb.getFriendlyUnitsIn(AIFloat3 pos, float radius);
        }
        // Update scout map...
    }
    
    //@Override
    public void unitGiven(Unit unit, int oldTeamId, int newTeamId) {
        //TODO
    }
    //@Override
    public void unitCaptured(Unit unit, int oldTeamId, int newTeamId) {
        //TODO
    }

    //@Override
    public void enemyEnterLOS(Unit enemy) {
        if (enemyInLOS.add(enemy)) registerEnemy(enemy, true);
    }
    //@Override
    public void enemyLeaveLOS(Unit enemy) {
        if (enemyInLOS.remove(enemy)) {
            if (!enemyInRadar.contains(enemy)) unRegisterEnemy(enemy, false, true);
        }
    }

    //@Override
    public void enemyEnterRadar(Unit enemy) {
        if (enemy==null) return; // !!!!!!
        if (enemyInRadar.add(enemy)) registerEnemy(enemy, false);
    }
    //@Override
    public void enemyLeaveRadar(Unit enemy) {
        if (enemyInRadar.remove(enemy)) {
            if (!enemyInLOS.contains(enemy)) unRegisterEnemy(enemy, false, false);
        }
    }

    //@Override
    public void enemyDamaged(Unit enemy, Unit attacker, float damage, AIFloat3 dir, WeaponDef weaponDef, boolean paralyzed) {
        //TODO
    }

    //@Override
    public void enemyDestroyed(Unit enemy, Unit attacker) {
//        if (enemy==null) {
//            owner.sendTextMsg("WTF ETTOR: enemyDestroyed enemy==null", FieldBOT.MSG_ERR);
//        } else 
        unRegisterEnemy(enemy, true, false); // TODO by LOS or by RADAR ????
    }

    //@Override
    public void weaponFired(Unit unit, WeaponDef weaponDef) {
        //TODO
    }

    //@Override
    public void seismicPing(AIFloat3 pos, float strength) {
        //TODO
    }

//    //@Override
//    public void enemyCreated(Unit enemy) {
//    }
//    //@Override
//    public void enemyFinished(Unit enemy) {
//    }
    
    // ------
}
