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
import java.util.List;

/**
 *
 * @author PlayerO1
 */
public class TScoutModule {
    private static final boolean SAFE_MODE=true;// disable some message work, test for Spring <96
    private static final boolean REFRESH_MAP_TICK=true;// disable refresh enemy scaner
    
    private final FieldBOT owner;
    private final Map map;

    private final float cellScanRadius; // size of one cell in map

    public final float mapWidth, mapHeight;
    
    public float[][] scountMap_enemy; // this contain info about enemy detect (enemy count)/(last time scout * ~alpha).
    public float[][] scountMap_scout; // this contain info about scouting (scout=1)/(last time scout * ~alpha).
    // TODO use scountMap!, TODO store: +last scout time, enemy count
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

    /**
     * new float[w][h], then fill 0 all cells.
     * @param w
     * @param h
     * @return 
     */
    private float[][] createAScoutMap(int w,int h) {
        float [][]aScountMap=new float[w][h] ;//[Math.round(mapWidth/smCellWidth)][Math.round(mapHeight/smCellHeight)];
        for (int i=0; i<aScountMap.length; i++) Arrays.fill(aScountMap[i], 0.0f);
        return aScountMap;
    }
    
    public TScoutModule(FieldBOT owner) {
        this.owner=owner;
        this.map=owner.clb.getMap();
        mapWidth=map.getWidth()*8; // TEST !!!!
        mapHeight=map.getHeight()*8;
        
        smCellWidth=Math.min(mapWidth/30, 50);
        smCellHeight=Math.min(mapHeight/30, 50);
        scountMap_enemy=createAScoutMap(Math.round(mapWidth/smCellWidth),Math.round(mapHeight/smCellHeight));
        owner.sendTextMsg("Create TScoutManager, map index size="+scountMap_enemy.length+"x"+scountMap_enemy[0].length, FieldBOT.MSG_DBG_SHORT);
        scountMap_scout=createAScoutMap(scountMap_enemy.length,scountMap_enemy[0].length);
        
        if (!SAFE_MODE) {
            enemyUnits=new LinkedList<Unit>();
            enemyInLOS=new LinkedList<Unit>();
            enemyInRadar=new LinkedList<Unit>();
        } else {// SAFE_MODE
            enemyUnits=enemyInLOS=enemyInRadar=null;
        }
        last_enemyComeWatch=last_enemyLeaveWatch=lastEnemmyBuildings=null;
        
        cellScanRadius=(float)Math.sqrt(mapWidth*smCellWidth+smCellHeight*smCellHeight);
    }
    
    /**
     * find near cell with
     * @param AMap any map data float[n][m]
     * @param nearTo start scan point
     * @param pointID for random select
     * @return good cell position={i,j}
     */
    private int[] getNearGoodPointAtMap(float[][] AMap,int c_I, int c_J,AIFloat3 nearTo,int pointID, boolean searchMAX)
    {
        // find near cell with minimal scouting coast
        float minScoutK=scountMap_scout[c_I][c_J];
        int BEST_I=-1, BEST_J=-1;// !!!
        int MAX_I=scountMap_scout.length, MAX_J=scountMap_scout[0].length;
        int MAX_R=Math.max(MAX_I,MAX_J)/3;
        for (int r=1; r< MAX_R; r++)
        { // of all radius
            int i1=Math.max(c_I-r, 0);
            int i2=Math.min(c_I+r,MAX_I-1);
            int j1=Math.max(c_J-r, 0);
            int j2=Math.min(c_J+r,MAX_J-1);

            for (int i=i1; i<i2; i++) // TODO optimized it!
             for (int j=j1; j<j2; j++)
              if (i==i1 || j==j1 || i==i2 || j==j2)
            {
                float tmpSK=scountMap_scout[i][j];// TODO + distance between scout and new point
                float distance=(float)Math.sqrt((i-c_I)*(i-c_I)+(j-c_J)*(j-c_J));//MathPoints.getDistanceBetweenFlat(nearTo, getPointFromScountMap(i, j));
                tmpSK = tmpSK / (distance+1.0f);
                if (pointID>0) tmpSK*=(0.95f+Math.random()*0.1f);
                if ((tmpSK<minScoutK) != searchMAX)
                {
                    minScoutK=tmpSK;
                    BEST_I=i;
                    BEST_J=j;
                }
            }
        }
        if (BEST_I>=0 && BEST_J>=0) {
            int result[]={BEST_I,BEST_J};
            return result;
        } else return null;
    }

    
    private int[] lstScoutingCell=null;// only for cashe;
    /**
     * get point, recomended for scout
     * @param nearTo search near from this point, can be null
     * @param pointID take some random to find, if send scout to different point, default = 0
     * @return point to send scout
     */
    public AIFloat3 getPointToScout(AIFloat3 nearTo,int pointID) {
        // TODO
        int[] mapP=getPointToScountMap(nearTo);
        if (lstScoutingCell==null || Arrays.equals(lstScoutingCell, mapP)) {
            refreshCellByScout(nearTo);//...
            lstScoutingCell=mapP;
        }
        int c_I=mapP[0], c_J=mapP[1];
        float x=-1,y=-1;
        if (pointID<=0) {
            int pIJ[]=getNearGoodPointAtMap(scountMap_scout, c_I, c_J, nearTo, pointID, false);
            if (pIJ!=null) {
                AIFloat3 sp=getPointFromScountMap(pIJ[0],pIJ[1]);
                x=sp.x; y=sp.y;
            } // else x and y is -1 by default
        }
        if (x<0 || y<0) { // is no valid point -> random point
            x=(float)Math.random() *mapWidth;
            y=(float)Math.random() *mapHeight;
        }
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
            // TODO for SAFE_MODE - last is null
        }
        // TODO army seek point
        AIFloat3 p=getPointToScout(nearTo, pointID);
        if (Math.random()>0.6) p=getNearEnemy(nearTo, cellScanRadius); // !!!!
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
     * @return point, or null
     */
    public AIFloat3 getNearEnemy(AIFloat3 nearTo, float inR) { //,boolean useLostWiew  * @param useLostWiew use non wisible point, only what last wiew. 
        // TODO
        float L=Float.POSITIVE_INFINITY;
        AIFloat3 nearP=null;
        
        List<Unit> enemyLst=owner.clb.getEnemyUnitsIn(nearTo, inR);//enemyUnits; // FIXME SAFE_MODE
        for (Unit u:enemyLst) {
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
    
    
    //---------------
    private void cell_refreshEnemyOnMap(int i,int j, boolean onlyAdd) {
        AIFloat3 p=getPointFromScountMap(i, j);
        List<Unit>unitLst=owner.clb.getEnemyUnitsIn(p, cellScanRadius);
        float count=unitLst.size();
        float mapX=scountMap_enemy[i][j];
        if (mapX<count) scountMap_enemy[i][j]=count;
        else if (!onlyAdd){
            scountMap_enemy[i][j]=scountMap_enemy[i][j]*0.8f+count*0.21f; // !!!!
        }
    }
    private void cell_refreshScoutOnMap(int i,int j, boolean onlyAdd) {
        AIFloat3 p=getPointFromScountMap(i, j);
        List<Unit>unitLst=owner.clb.getFriendlyUnitsIn(p, cellScanRadius);
        float count=Math.max(2, unitLst.size()/2.0f);// TODO + * get LOS/radar radius
        float mapX=scountMap_scout[i][j];
        if (mapX<count) scountMap_scout[i][j]=count;
        else if (!onlyAdd){
            scountMap_enemy[i][j]=scountMap_enemy[i][j]*0.8f+count*0.21f; // !!!!
        }
    }
    
    /**
     * Update scountMap data as scan all see map for enemy point.
     * Too much CPU!
     */
    private void refreshMap() {
        for (int i=0;i<scountMap_enemy.length; i++) 
          for (int j=0;j<scountMap_enemy[i].length; j++)
          {
              cell_refreshEnemyOnMap(i,j, false);
              cell_refreshScoutOnMap(i,j, false);
          }
    }
    
    private int lst_RM_i=-1;
    private int lst_RM_j=-1;
    
    /**
     * Update scountMap data as scan all see map for enemy point.
     * Not much CPU...
     */
    private void refreshMap_part(int frame) {
        int p=frame%(scountMap_enemy.length*scountMap_enemy[0].length); // !!!! check it.
        int i=p/(scountMap_enemy.length);
        int j=p%(scountMap_enemy.length); //scountMap_enemy[i].length;
          cell_refreshEnemyOnMap(i,j, false);
          cell_refreshScoutOnMap(i,j, false);
        if (lst_RM_i==-1) { // stop refresh
            lst_RM_i=i;
            lst_RM_j=j;
        } else if (lst_RM_i==i && lst_RM_j==j) {
            refreshMapTick = false;
            lst_RM_i=lst_RM_j=-1;
        }
    }
    
    /**
     * Update scountMap_ data on scout finished moving on map cell...
     */
    private void refreshCellByScout(AIFloat3 scoutPosition) {
        int i,j; // cell number
        int cellN[]=getPointToScountMap(scoutPosition);
        i=cellN[0]; j=cellN[1];
        cell_refreshEnemyOnMap(i,j, true);
        cell_refreshScoutOnMap(i,j, true);
    }
    
// ---------------
    /**
     * 
     * @param unit
     * @param byLOS - for Spring 94.1 else maybe crash on access.
     * @return 
     */
    private boolean registerEnemy(Unit unit, boolean byLOS) {
        //if (SAFE_MODE) return false;// UNCKNOWN!!!
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
        //if (SAFE_MODE) return false;// UNCKNOWN!!!
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
        if (SAFE_MODE) return false;// UNCKNOWN!!!
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

    private boolean refreshMapTick=false;
    // --- Game message ---
    //@Override
    public void update(int frame) {
        lstScoutingCell=null;
        if (frame%15==5) {
            //TODO update
            //TODO USE:
//            owner.clb.getEnemyUnitsIn(last_enemyComeWatch, mapWidth);
//            owner.clb.getEnemyUnitsInRadarAndLos();
//            owner.clb.getEnemyUnits();
//            owner.clb.getNeutralUnitsIn(AIFloat3 pos, float radius)
//            owner.clb.getFriendlyUnitsIn(AIFloat3 pos, float radius);
        }
        if (frame%250==25 && REFRESH_MAP_TICK) {
            // Update scout map
            // refreshMap(); // ... REQUIRED TOO MUCH CPU TIME!!!
            refreshMapTick=true;
        }
        if (refreshMapTick) {
            refreshMap_part(frame);
        }
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
        if (SAFE_MODE) return;// !!!
        if (enemyInLOS.add(enemy)) registerEnemy(enemy, true);
    }
    //@Override
    public void enemyLeaveLOS(Unit enemy) {
        if (SAFE_MODE) return;// !!!
        if (enemyInLOS.remove(enemy)) {
            if (!enemyInRadar.contains(enemy)) unRegisterEnemy(enemy, false, true);
        }
    }

    //@Override
    public void enemyEnterRadar(Unit enemy) {
        if (SAFE_MODE) return;// !!!
        if (enemy==null) return; // !!!!!!
        if (enemyInRadar.add(enemy)) registerEnemy(enemy, false);
    }
    //@Override
    public void enemyLeaveRadar(Unit enemy) {
        if (SAFE_MODE) return;// !!!
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
        if (SAFE_MODE) return;// !!!
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
