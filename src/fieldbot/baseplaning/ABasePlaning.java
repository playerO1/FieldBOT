/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package fieldbot.baseplaning;

import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.Feature;
import com.springrts.ai.oo.clb.Map;
import com.springrts.ai.oo.clb.Resource;
import com.springrts.ai.oo.clb.Unit;
import com.springrts.ai.oo.clb.UnitDef;
import fieldbot.ModSpecification;
import fieldbot.AIUtil.MathPoints;
import fieldbot.TBase;
import java.util.List;

/**
 * Обеспечивает размещение зданий на базе, упорядочивает их.
 * @author user2
 */
public abstract class ABasePlaning {
    
    public final TBase owner;
    public final Map map;
    
    protected float containUnitSquare;// площадь стационарных юнитов
    protected float freeSpaceUsingK; // коэффициент использования свободной площади, для full

    /**
     * Для ускорения подтверждения возможности строить.
     * Using in canBuildOnBase()
     */
    protected AIFloat3 cashe_LastCanBuildPoint;
    protected Resource resMetal;

    public ABasePlaning(TBase owner) {
        this.owner=owner;
        this.map=owner.owner.clb.getMap();
        this.resMetal=owner.owner.clb.getResourceByName("Metal");
        
        freeSpaceUsingK=1.35f;
        containUnitSquare=0.0f;
        
        cashe_LastCanBuildPoint=null;
    }
    
    /**
     * на сколько заполнена база
     * @return 
     */
    public float full() {
        return (containUnitSquare * freeSpaceUsingK) / (2*(float)Math.PI*owner.radius*owner.radius);// TODO test
    }
    /**
     * Возвращает рекомендованный радиус базы (для расширения)
     * @return 
     */
    public float getRecomendedRadius() {
        return (owner.radius+1.0f)*(full()+0.3f*freeSpaceUsingK); // TODO
    }
    
    
//    public boolean pointOnMap(AIFloat3 pt) { // Test it
//        return (pt.x>0 && pt.z>0 && pt.x<map.getWidth()*8 && pt.z<map.getHeight()*8);
//    }
    
    /**
     * Подготавливает место заранее для юнитов (используется в некоторых планах базы)
     * @param unitType тип юнита
     * @param count предполагаемое колличество
     */
    public void preparePositionFor(UnitDef unitType,int count)
    {
        // Stumpt.
    }
    
    /**
     * При перемещении центра базы. Улучшает планирование некоторых TBasePlaning.
     */
    public void onBaseMoving() {
        cashe_LastCanBuildPoint=null;
    }
    
     /**
     * Возвращает рекомендованную позицию для постройки зданий
     * @param unitType тип здания (или юнита)
     * @param mainBuilder строитель, может быть null
     * @return точка для строительства или null
     */
    abstract public AIFloat3 getRecomendetBuildPosition(UnitDef unitType, Unit mainBuilder);    
    
    private AIFloat3 findFreeGeoPointAt(UnitDef unitType, AIFloat3 buildCenter,float r)
    {
        //TODO search nearest point.
        // Если нужна геотермальная точка
        List<Feature> features=owner.owner.clb.getFeaturesIn(buildCenter, r);
        for (Feature f:features) if (f.getDef().isGeoThermal())
        { // TODO можно кэшировать.
            AIFloat3 buildPos=f.getPosition();
            if (map.isPossibleToBuildAt(unitType, buildPos, owner.BUILD_FACING))
                return buildPos;
        }
        return null;
    }
    
    /**
     * Проверяет, нуждается ли постройка в шахтах или нет.
     * @param unitType
     * @return 
     */
    public boolean needMetalPosition(UnitDef unitType) {
        //boolean needMetalSpots=false;
        //float extrRadius=0.0f;
        if (owner.owner.isMetalFieldMap==0) { // постройка шахты на обычной карте
            if (unitType.getExtractsResource(resMetal)!=0) {
                return true;
                //extrRadius=map.getExtractorRadius(resMetal);
            }
        }
        return false;
    }
    
    /**
     * Проверяет, нуждается ли постройка в специальных точках (шахтах)
     * @param unitType
     * @return 
     */
    public boolean needSpecialPointPosition(UnitDef unitType) {
        // TODO можно проверять двигается или нет.
        return unitType.isNeedGeo() || needMetalPosition(unitType);
    }
    
    protected AIFloat3 getBuildPosition_Sppiral(UnitDef unitType, AIFloat3 buildCenter,double startAng, double minR, double maxR) {
        
        boolean needMetalSpots=needMetalPosition(unitType);

        final int NUM_APPROX_POINTS=50; // число пробных точек
        final int NUM_SpiralRot=4; // число витков спирали
        int numOfTry=0; // колличество попыток. Max = NUM_APPROX_POINTS  
        AIFloat3 buildPos = null;
        //double minR = FieldBOT.getRadiusFlat(unitType);//unitType.getRadius(); // минимальный радиус

        //double startAng = 0.0;
        //  if (full>0.7) startAng = 2*Math.random()*Math.PI;
       // double maxDR = owner.radius;//unitType.getRadius();
        
        
        double deltaR=maxR-minR;
        
        
        if (unitType.isNeedGeo()) return findFreeGeoPointAt(unitType, buildCenter, (float)maxR);
        // TODO test it!
        
      do {
        buildPos = new AIFloat3(buildCenter);
        
        double r = minR + deltaR*( (double)numOfTry / (double)NUM_APPROX_POINTS ); // радиус точки.
        double ang=startAng + ((double)NUM_SpiralRot * (double)numOfTry / (double)NUM_APPROX_POINTS * (2*Math.PI));
        buildPos.x+=r*Math.sin(ang);buildPos.z+=r*Math.cos(ang);

        if (needMetalSpots)
        { // если шахты одиночные.
            AIFloat3 metalP=map.getResourceMapSpotsNearest(resMetal, buildPos);
            
            // correct position - FIX bug on getResourceMapSpotsNearest - по центру шахт. 
            //metalP=new AIFloat3(metalP.x+map.getExtractorRadius(resMetal)/3, metalP.y, metalP.z+map.getExtractorRadius(resMetal)/3);
            // TODO test it! плозо работает - шахты плозо строит.
            
            if (map.isPossibleToBuildAt(unitType, metalP, owner.BUILD_FACING))
                buildPos=metalP;
              else
                buildPos=null;
        } else
        { // простые здания
            // проверка на возможность строить здание здесь
            /* Было: без учёта границ между соседними зданиями
            if (!map.isPossibleToBuildAt(unitType, buildPos, owner.BUILD_FACING)) {
                AIFloat3 np=map.findClosestBuildSite(unitType, buildPos, FieldBOT.getRadiusFlat(unitType)*3.5f, Math.min(unitType.getXSize(), unitType.getZSize())/2, owner.BUILD_FACING); // TODO test!
                if (!FieldBOT.isValidPoint(np)) buildPos=null;
                else buildPos=np;
            }*/
            // стало с учётом отступов
            AIFloat3 np=map.findClosestBuildSite(unitType, buildPos, MathPoints.getRadiusFlat(unitType)*1.8f, Math.min(unitType.getXSize(), unitType.getZSize())/2, owner.BUILD_FACING); // TODO test!
            if (!MathPoints.isValidPoint(np)) buildPos=null; else buildPos=np;
        }
        
        // ---
        if (buildPos!=null) {
            if (!ModSpecification.isRealyAbleToMove(unitType)) // !!!!
              if (MathPoints.getDistanceBetweenFlat(buildPos, owner.center) < 120) buildPos=null;
            // TODO временно. FIX для canBuildOnBase !!!
        } 
        // ---
        
        numOfTry++;
        
      } while (numOfTry<NUM_APPROX_POINTS && (buildPos==null));

      // убрать, было if (full<numOfTry/NUM_APPROX_POINTS) full=numOfTry/NUM_APPROX_POINTS; // подсчёт на сколько заполнено.

      return buildPos;
    }

    /**
     * Можно ли построить на базе это?
     * @param unitType
     * @param checkForFreeMetalSpots
     * @param buildFacing
     * @return 
     */
    public boolean canBuildOnBase(UnitDef unitType, boolean checkForFreeMetalSpots, int buildFacing) {
        
        if (unitType.isNeedGeo()) return findFreeGeoPointAt(unitType, owner.center, owner.radius)!=null; // TODO test it!
        
        boolean needMetalSpots=needMetalPosition(unitType);
        
        // быстро проверить в центре
        if (needMetalSpots) {
            AIFloat3 metalP=owner.owner.clb.getMap().getResourceMapSpotsNearest(resMetal, owner.center);
            float l=MathPoints.getDistanceBetweenFlat(metalP, owner.center);
            if (l<owner.radius ) { //|| l<extrRadius) { // !!!!!
                if (map.isPossibleToBuildAt(unitType, metalP, buildFacing)) return true;
            } else {
                // Если намного превышает радиус базы то НЕ СТРОИТЬ!!!
                if (l>owner.radius *1.4f) return false;
            }
        } else {
            if (cashe_LastCanBuildPoint!=null) {
                if (map.isPossibleToBuildAt(unitType, cashe_LastCanBuildPoint, buildFacing)) return true;
            } else {
                if (map.isPossibleToBuildAt(unitType, owner.center, buildFacing)) {
                    cashe_LastCanBuildPoint=new AIFloat3(owner.center);
                    return true;
                }
            }
        }
        // FIXME если центр застроен возвращает FALSE !!!!! иногда
        
        // или обход по спирали до края базы...
        final int NUM_APPROX_POINTS=25; // число пробных точек
        final int NUM_SpiralRot=3; // число витков спирали
        AIFloat3 buildPos;
        double startAng=2*Math.random()*Math.PI;
        for (int i=1; i<=NUM_APPROX_POINTS; i++) { // TODO использовать лучший вид спирали
            //if (mainBuilder!=null) buildPos = mainBuilder.getPos();
            buildPos=new AIFloat3(owner.center);
            double r = owner.radius * ((1.0/(double)NUM_APPROX_POINTS)*(double)i);
            double ang=startAng + ((double)NUM_SpiralRot * ((double)i/(2*Math.PI)));
            buildPos.x+=r*Math.sin(ang);buildPos.z+=r*Math.cos(ang);

            if (needMetalSpots) {
                AIFloat3 metalP=owner.owner.clb.getMap().getResourceMapSpotsNearest(resMetal, buildPos);
                float l=MathPoints.getDistanceBetweenFlat(metalP, buildPos);
                if (l<owner.radius ) { //|| l<extrRadius) { // !!!!!!!!!
                    if (map.isPossibleToBuildAt(unitType, metalP, buildFacing)) return true;
                }
            } else {
                if (map.isPossibleToBuildAt(unitType, buildPos, buildFacing)) {
                    cashe_LastCanBuildPoint=buildPos;
                    return true;
                } else {
                    // ближную (полуускоренно)...
                    AIFloat3 np=map.findClosestBuildSite(unitType, buildPos, owner.radius/NUM_APPROX_POINTS*2, Math.min(unitType.getXSize(), unitType.getZSize())/2, buildFacing); // TODO test!
                    if (MathPoints.isValidPoint(np)) {
                        cashe_LastCanBuildPoint=np;
                        return true;
                    }
                }
            }
        }
        
        return false;
    }
    
    
    public void onAdd(Unit unit) {
        UnitDef def=unit.getDef();
        if (!ModSpecification.isRealyAbleToMove(def)) containUnitSquare+=def.getXSize()*def.getZSize();
    };
    public void onRemove(Unit unit) {
        UnitDef def=unit.getDef();
        if (!ModSpecification.isRealyAbleToMove(def)) containUnitSquare-=def.getXSize()*def.getZSize();
    };
    
}
