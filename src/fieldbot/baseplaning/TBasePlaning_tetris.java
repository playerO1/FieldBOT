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

package fieldbot.baseplaning;

import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.Unit;
import com.springrts.ai.oo.clb.UnitDef;
import fieldbot.FieldBOT;
import fieldbot.ModSpecification;
import fieldbot.AIUtil.MathPoints;
import fieldbot.TBase;
import java.util.ArrayList;
import java.util.Iterator;

/**
 *
 * @author PlayerO1
 */
public class TBasePlaning_tetris extends ABasePlaning{

    protected ArrayList<TPlanCell> cells;
    
    private float DEFAULT_CELL_PADDING=60.2f;
    private float DEFAULT_CELL_SPACING=5.5f;
    
    private final UnitDef SPECIAL_CELL_DEF=null; // mark this as UnitDef special cell for reserved point territory.
    
    public TBasePlaning_tetris(TBase owner) {
        super(owner);
        cells=new ArrayList<TPlanCell>();
        TPlanCell.clb=owner.owner;// only for DEBUG !!!!!!!!
        freeSpaceUsingK=1.16f;
        
        // Reserve territory for metal extractors and geotermal points
        reservePointPositions();
    }
    
    /**
     * Find cell for unit type
     * @param def
     * @param count
     * @return cell with free position or null
     */
    private TPlanCell findFreeCell(UnitDef def,int count) {
        for (TPlanCell cell:cells) if (cell.forUnitType==def && cell.getNumFreePos()>=count) return cell;
        return null;
    }
    
    private TPlanCell findCollisionCell(AIFloat3 p) {
        for (TPlanCell cell:cells) if (cell.collision(p)) return cell;
        return null;
    }
    private TPlanCell findCollisionCell(TPlanCell c1) {
        for (TPlanCell cell2:cells) if (cell2!=c1) if (c1.collision(cell2)) return cell2;
        return null;
    }
    
    
    private void reservePointPositions() {
        ArrayList<AIFloat3> forPoints=new ArrayList<AIFloat3>(getGeoPointsAt(owner.center, owner.radius));
        if (owner.owner.isMetalFieldMap==FieldBOT.IS_NORMAL_METAL)
        {
            ArrayList<AIFloat3> metalPoints=owner.owner.getMetalSpotsInRadius(owner.center, owner.radius);
            if (metalPoints!=null) forPoints.addAll(metalPoints);
            else // !!! For debug TODO init prepare metal point on first base
                owner.owner.sendTextMsg("Warning: tetris base planing can not reserved metal points, becouse metal data structure was not initialized.", FieldBOT.MSG_ERR);
        }
        float cellPadding=DEFAULT_CELL_PADDING*2;
        for (AIFloat3 p:forPoints) { // reserve all this point
            TPlanCell newCell=new TPlanCell(SPECIAL_CELL_DEF, 1, cellPadding, DEFAULT_CELL_SPACING);
            //TODO maybe create class TPlanCell_reservePoint ?
            newCell.moveTo(p.x, p.z);
            //newCell.updateZanatoPoz(map, 0); //!!!
            cells.add(newCell);
        }
    }
    /**
     * Remove all special TPlanCell with mark SPECIAL_CELL_DEF from list.
     * @return modify
     */
    private boolean clearReservedPointPositions() {
        if (cells.isEmpty()) return false;
        boolean modify=false;
        Iterator<TPlanCell> itr1 = cells.iterator();
        while (itr1.hasNext()) {
            TPlanCell cell = itr1.next();
            if (cell.forUnitType==SPECIAL_CELL_DEF) {
                itr1.remove();
                modify=true;
            }
        }
        return modify;
    }
    
    /**
     * Create empty new cell
     * @param def for this unit
     * @param count planing count of contain units
     * @param center start search point
     * @param maxR bound radius
     * @return link to new cell, or null
     */
    private TPlanCell createCell(UnitDef def,int count, AIFloat3 center,float maxR, int buildFacing) {
        // 1. Создать площадь
        
        // TODO use def.isAbleToKamikaze(), def.getKamikazeDist(), _def.weapons..._ !!!!, def.getSelfDExplosion()
        float cellPadding=DEFAULT_CELL_PADDING;
        if (ModSpecification.isStationarFactory(def)) cellPadding+=def.getRadius()/2;//TODO test: base planing: free space in factory
        TPlanCell newCell=new TPlanCell(def, count, cellPadding, DEFAULT_CELL_SPACING);
        // 2. Подобрать место
        AIFloat3 p=MathPoints.getRandomPointOnRadius(center,maxR/10); // что лучше? !!! было new AIFloat3(center);
        
        TPlanCell collisionCell=null;
        final int MAX_COUNT_ITER=50;
        int numOfTry=0;
        do { // перебор клеток ближайших...
            //collisionCell=findCollisionCell(p);
            //if (collisionCell==null) {
                newCell.moveTo(p.x, p.z);
                collisionCell=findCollisionCell(newCell);
            //}
            if (collisionCell!=null) { // сдвинуться
                owner.owner.sendTextMsg("tetris> Collision at "+p, FieldBOT.MSG_DBG_ALL);// DEBUG!!!
                // сначало обойти вокруг этого...
                //TODO test it!
                TPlanCell centerCell=collisionCell;
                for (int i=0; i<4 && (collisionCell!=null || newCell.getNumFreePos()==0); i++)
                {
                    int ofsX,ofsY;
                    switch (i) { // TODO !!!!!
                        case 0: ofsX=1; ofsY=0; break;
                        case 1: ofsX=0; ofsY=1; break;
                        case 2: ofsX=-1; ofsY=-1; break;
                        case 3: ofsX=-1; ofsY=0; break;
                            // TODO по краям-по углам
                        default:
                            ofsX=1; ofsY=1;
                            // TODO warning!
                    }
                    p=centerCell.getPointForNearPos(newCell, ofsX, ofsY);
                    newCell.moveTo(p.x, p.z);
                    collisionCell=findCollisionCell(newCell);
                    if (collisionCell==null) newCell.updateZanatoPoz(map, buildFacing);
                    else owner.owner.sendTextMsg("tetris> moving near cell, but new collision "+p, FieldBOT.MSG_DBG_ALL);// DEBUG!!!
                }
                if (collisionCell!=null) owner.owner.sendTextMsg("tetris> moving near to "+p+" but collision.", FieldBOT.MSG_DBG_ALL);// DEBUG!!!
                
                
                if (collisionCell!=null)
                { // По спирали, если рядом не определено
                    p = new AIFloat3(center);
                    double ang = (5*(double)numOfTry/(double)MAX_COUNT_ITER) * 2.0 * Math.PI; // 5 = число оборотов спирали
                    double r=maxR*((double)numOfTry/(double)MAX_COUNT_ITER*0.8+0.2);
                    p.x += r * Math.sin(ang);
                    p.z += r * Math.cos(ang);
                    //p=MathPoints.getRandomPointOnRadius(center,maxR); // было
                }
            } else {
                //newCell.moveTo(p.x, p.z);
                newCell.updateZanatoPoz(map, buildFacing);
            } //!!!
            
            numOfTry++;
        } while ((collisionCell!=null || newCell.getNumFreePos()==0) && numOfTry<MAX_COUNT_ITER);// !!!
        
        if (numOfTry>=MAX_COUNT_ITER) makeClearing();// TODO улучшить.
        
        if ((collisionCell!=null) || (newCell.getNumFreePos()==0)) newCell=null; // если есть перекрытие, то отмена
        
        else {
            newCell.updateZanatoPoz(map, buildFacing);
            if (newCell.getNumFreePos()==0) {
                // TODO если неудачное место !!!!
                newCell=null;
            }
        }

        // 3. Apply and save
        if (newCell!=null) cells.add(newCell);
        return newCell;
    }
    
    /**
     * на сколько заполнена база
     * @return 
     */
    @Override
    public float full() {
        return super.full();// TODO full()
    }
    /**
     * Возвращает рекомендованный радиус базы (для расширения)
     * @return 
     */
    @Override
    public float getRecomendedRadius() {
        return super.getRecomendedRadius();
        //return owner.radius;//owner.radius*(full+0.3f); // TODO
    }
    
    
    @Override
    public void preparePositionFor(UnitDef unitType,int count)
    { // TODO use it!!!!!
        if (ModSpecification.isRealyAbleToMove(unitType)) return;
        
        int preparePosHave=0;
        for (TPlanCell cell:cells) if (cell.forUnitType==unitType) preparePosHave+=cell.getNumFreePos();
        int needNew=count-preparePosHave;
        if (needNew>0) {
            if (needNew<4 && needNew>1) needNew=4;
            TPlanCell cell=createCell(unitType, needNew, owner.center,owner.radius, owner.BUILD_FACING);
             //TODO разбить большой массив пополам
            if (cell==null && needNew>4) {
                needNew/=2;
                cell=createCell(unitType, needNew, owner.center,owner.radius, owner.BUILD_FACING);
                if (cell!=null) cell=createCell(unitType, needNew, owner.center,owner.radius, owner.BUILD_FACING);
            }
           
        }
    }
    
    @Override
    public void onBaseMoving() {
        super.onBaseMoving();
        clearReservedPointPositions();
        clearFreeCell();
        reservePointPositions();
        // todo убрать только вне новой зоны.
    }
    
    /**
     * Delete no using cell for free position
     * @return modifed
     */
    private boolean clearFreeCell() {
        if (cells.isEmpty()) return false; // ???!!!
        boolean modify=false;
        Iterator<TPlanCell> itr1 = cells.iterator(); // Цикл...
        while (itr1.hasNext()) {
            TPlanCell cell = itr1.next();
            if (cell.zanato==0 && cell.forUnitType!=SPECIAL_CELL_DEF) {
                itr1.remove(); // убрать пустую клетку
                modify=true;
            }
        }
        return modify;
    }
    
    /**
     * Clear planing space from no using position
     * @return modifed
     */
    private boolean makeClearing() {
        if (clearFreeCell()) return true;
        else {
            // TODO сокращение наполовину использованных клеток...
            return false;
        }
    }
     /**
     * Return position for build unit
     * @param unitType unit or building type
     * @param mainBuilder builder, can be null
     * @return point for build or null
     */
    @Override
    public AIFloat3 getRecomendetBuildPosition(UnitDef unitType, Unit mainBuilder) {
        boolean needSpecialPoint=needSpecialPointPosition(unitType);
        AIFloat3 buildPos = null;
        
        double startAng = 0.0;
          if (full()>0.7) startAng = 2*Math.random()*Math.PI;
          
        float maxR = owner.radius;//unitType.getRadius();
        //double minR = Math.max(maxR*0.10, FieldBOT.getRadiusFlat(unitType));//unitType.getRadius(); // минимальный радиус
        
        AIFloat3 buildCenter;
        
        boolean etoStroenie=!ModSpecification.isRealyAbleToMove(unitType); // It is building - no moveable.
        
        if (etoStroenie) { // это здание
            // 1. Определится с масштабом зоны
            if (mainBuilder!=null && !ModSpecification.isRealyAbleToMove(mainBuilder.getDef())) // если строитель неподвижный
            {
                buildCenter = MathPoints.getPointInMiddle(owner.center, mainBuilder.getPos());// точка между центром и строителем.
                maxR=mainBuilder.getDef().getBuildDistance();
            } else {
                buildCenter=owner.center;
                maxR=owner.radius;
            }
            
            // 2. найти или создать зону строительства
            if (needSpecialPoint) { // If need special point
               // FIXME CHANGE! Not full support!!! Do: or add TPlanCell, or do some other.
                double minR=maxR*0.10;
                if (mainBuilder!=null) minR=Math.max(minR, MathPoints.getRadiusFlat(mainBuilder.getDef()));
                
                AIFloat3 searchNear;
                if (mainBuilder!=null) searchNear=mainBuilder.getPos(); else searchNear=null;
                
                buildPos=getBuildPosition_Sppiral(unitType, buildCenter,searchNear, startAng, minR, maxR);
                if (!MathPoints.isValidPoint(buildPos)) buildPos=null;
            } else {
                TPlanCell cell=findFreeCell(unitType, 1);
                if (cell==null) { // Если нет, то создать зону
                    int count=4; // TODO колличество ?
                    // TODO группы больше 4-х...
                    //if ((unitType.getXSize()*8+unitType.getZSize()*8)/2*8<70) count=6; // for Wind spam
                    //FIXME ошибка с height или с позицией по вертикали когда больше 2-х в высоту а в ширину 2.... может в ширину тоже.
                    
                    if (ModSpecification.isStationarFactory(unitType)) count=1; // !!!!!
                    cell=createCell(unitType, count, buildCenter,maxR, owner.BUILD_FACING);
                }
                if (cell!=null) buildPos=cell.getFreePos(map, owner.BUILD_FACING);
                 else owner.owner.sendTextMsg(" ERROR (tetris base planing) not free cell and can not create for "+unitType.getName()+"!", FieldBOT.MSG_ERR);
                if (buildPos==null) {
                    if (cell!=null)
                      owner.owner.sendTextMsg(" ERROR (tetris base planing) Free cell is not free!", FieldBOT.MSG_ERR);
                }
            }
            // 3. TODO!!!
            
            return buildPos;
            
        } else { // It is movable unit
            // 1. Build zone: near for main builder
            if (mainBuilder!=null) //&& !ModSpecification.isRealyAbleToMove(mainBuilder.getDef())) // если строитель неподвижный
            {
                buildCenter = new AIFloat3(mainBuilder.getPos());
                maxR=mainBuilder.getDef().getBuildDistance();
            } else {
                buildCenter=owner.center;
                maxR=owner.radius;
            }
            // 2. build near
            buildPos=new AIFloat3(buildCenter);
            double r=1.6*MathPoints.getRadiusFlat(unitType), a=2*Math.PI*Math.random();
            buildPos.x+=r*Math.cos(a);  buildPos.z+=r*Math.sin(a);
            // TODO может по спирали?...
            AIFloat3 np=map.findClosestBuildSite(unitType, buildPos, maxR, 1*Math.max(unitType.getXSize(), unitType.getZSize()) , owner.BUILD_FACING);
            if (!MathPoints.isValidPoint(np)) buildPos=null;
            else buildPos=np;

            return buildPos; // !!!           
        }
    }
    
    
    @Override
    public void onAdd(Unit unit) {
        super.onAdd(unit);
        if (ModSpecification.isRealyAbleToMove(unit.getDef())) return; // только неподвижных
        AIFloat3 p=unit.getPos();
        for (TPlanCell cell:cells) if (cell.collision(p))
        {
            cell.updateZanatoPoz(map, owner.BUILD_FACING);
          //if (cell.forUnitType==unit.getDef() && cell.zanato<cell.maxCount) cell.zanato++;//!!!
        }
    }
    @Override
    public void onRemove(Unit unit) {
        super.onRemove(unit);
        if (ModSpecification.isRealyAbleToMove(unit.getDef())) return; // only no moving
        AIFloat3 p=unit.getPos();
        //TODO use this.findCollisionCell(p)

        Iterator<TPlanCell> itr1 = cells.iterator(); // Clear. TODO using call clear function.
        while (itr1.hasNext()) {
            TPlanCell cell = itr1.next();
            if (cell.collision(p))
              if (cell.forUnitType==unit.getDef() && cell.zanato>0) {
                cell.zanato--;//!!!
                //TODO do use cell.updateZanatoPoz(map, owner.BUILD_FACING); ?
                if (cell.zanato==0) {
                    cell.updateZanatoPoz(map, owner.BUILD_FACING); // check again
                    if (cell.zanato==0 && cell.forUnitType!=SPECIAL_CELL_DEF) itr1.remove(); // убрать пустую клетку
                }
            }
            if (cell.zanato<0) owner.owner.sendTextMsg(" WARNING (tetris base planing) cell zanato="+cell.zanato+"!", FieldBOT.MSG_ERR);
        }
    }
}
