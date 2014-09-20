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
import fieldbot.AIUtil.MathPoints;
import fieldbot.FieldBOT;
import fieldbot.ModSpecification;
import fieldbot.TBase;
import java.util.ArrayList;
import java.util.List;

/**
 * Planing pfree position on base surface.
 * @author PlayerO1
 */
public abstract class ABasePlaning {
    
    public final TBase owner;
    public final Map map;
    
    protected float containUnitSquare;// square of stantionary building unit.
    protected float freeSpaceUsingK; // percend of square usage

    /**
     * For acseleration canBuildOnBaseSurface()
     * Using in canBuildOnBase()
     */
    protected AIFloat3 cashe_LastCanBuildPoint;
    /**
     * Cashe for building points.
     */
    private ArrayList<AIFloat3> cashe_GeoPoints,cashe_MetalPoints;

    protected final Resource resMetal;
    protected final UnitDef luaMExtractor; // for Zero-K mod

    public ABasePlaning(TBase owner) {
        this.owner=owner;
        this.map=owner.owner.clb.getMap();
        this.resMetal=owner.owner.clb.getResourceByName("Metal");
        
        this.luaMExtractor=owner.owner.modSpecific.luaMetalExtractor;
        
        freeSpaceUsingK=1.35f;
        containUnitSquare=0.0f;
        
        cashe_LastCanBuildPoint=null;
        cashe_GeoPoints=null;
        cashe_MetalPoints=null;
    }
    
    /**
     * Percent of base square usage
     * @return 
     */
    public float full() {
        return (containUnitSquare * freeSpaceUsingK) / (2*(float)Math.PI*owner.radius*owner.radius);// TODO test
    }
    /**
     * Return recomended radius for base. TODO!
     * @return 
     */
    public float getRecomendedRadius() {
        return (owner.radius+1.0f)*(full()+0.3f*freeSpaceUsingK); // TODO
    }
    
    
//    public boolean pointOnMap(AIFloat3 pt) { // Test it
//        return (pt.x>0 && pt.z>0 && pt.x<map.getWidth()*8 && pt.z<map.getHeight()*8);
//    }
    
    /**
     * Rezerve position for future building (not all BasePlaning support this)
     * @param unitType unit type
     * @param count max count for planing
     */
    public void preparePositionFor(UnitDef unitType,int count)
    {
        // Stumpt.
    }
    
    /**
     * On move base center. For better base planing and cashe update.
     */
    public void onBaseMoving() {
        // drop cashe
        cashe_LastCanBuildPoint=null;
        cashe_GeoPoints=null;
        cashe_MetalPoints=null;
    }
    
     /**
     * Return recomended build position for unit
     * @param unitType unit tupe, or building tupe
     * @param mainBuilder builder, may be null
     * @return point for build or null
     */
    abstract public AIFloat3 getRecomendetBuildPosition(UnitDef unitType, Unit mainBuilder);    
    
    private AIFloat3 findNear_noClosePoint(UnitDef unitType, AIFloat3 buildCenter,float r, List<AIFloat3> points) {
        float L=r,l;
        AIFloat3 nearP=null;
        for (AIFloat3 p:points) if (map.isPossibleToBuildAt(unitType, p, owner.BUILD_FACING)) 
        {
            l=MathPoints.getDistanceBetween3D(p, buildCenter);
            if (l<L) { // last: (L<0 || l<L)
                L=l;
                nearP=p;
            }
            //last: return p;
        }
        return nearP; //last: null;
    }
    
    private AIFloat3 findFreeGeoPointAt(UnitDef unitType, AIFloat3 buildCenter,float r)
    {
        if (buildCenter.equals(owner.center)) // TODO && r==owner.radius
        {// cahse
            if (cashe_GeoPoints==null) {
                cashe_GeoPoints=new ArrayList<AIFloat3>();
                List<Feature> features=owner.owner.clb.getFeaturesIn(buildCenter, r);
                for (Feature f:features) if (f.getDef().isGeoThermal()) cashe_GeoPoints.add(f.getPosition());
            }
            return findNear_noClosePoint(unitType, buildCenter, r, cashe_GeoPoints);
        } else {// no cashe
            ArrayList<AIFloat3> GeoPoints=new ArrayList<AIFloat3>();
            List<Feature> features=owner.owner.clb.getFeaturesIn(buildCenter, r);
            for (Feature f:features) if (f.getDef().isGeoThermal()) GeoPoints.add(f.getPosition());
            return findNear_noClosePoint(unitType, buildCenter, r, GeoPoints);
        }
    }
    private AIFloat3 findFreeMetalPointAt(UnitDef unitType, AIFloat3 buildCenter,float r)
    {
        if (buildCenter.equals(owner.center)) // TODO && r==owner.radius
        {// cashe
            if (cashe_MetalPoints==null) cashe_MetalPoints=owner.owner.getMetalSpotsInRadius(buildCenter, r);
            if (cashe_MetalPoints!=null) return findNear_noClosePoint(unitType, buildCenter, r, cashe_MetalPoints);
            else return null;// !!! TODO ?
        } else { // no cashe
            ArrayList<AIFloat3> MetalPoints=owner.owner.getMetalSpotsInRadius(buildCenter, r);
            if (MetalPoints!=null) return findNear_noClosePoint(unitType, buildCenter, r, MetalPoints);
            else return null;// !!! TODO ?
        }
    }
    
    /**
     * Chech, do unit need for mex point
     * @param unitType
     * @return if true then it is metal extractor
     */
    public boolean needMetalPosition(UnitDef unitType) {
        if (owner.owner.isMetalFieldMap<=0) { // build metal extractor on normal map, or on non-metal map (TODO test)
            if (unitType==luaMExtractor || unitType.getExtractsResource(resMetal)!=0.0f) {
                return true;
                //extrRadius=map.getExtractorRadius(resMetal);
            }
        }
        return false;
    }
    
    /**
     * Check, do this unit need special point to build (metal extractor, geotermal)
     * @param unitType
     * @return 
     */
    public boolean needSpecialPointPosition(UnitDef unitType) {
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
        
        // point depend building
        if (unitType.isNeedGeo()) return findFreeGeoPointAt(unitType, buildCenter, (float)maxR);
        if (needMetalSpots) return findFreeMetalPointAt(unitType, buildCenter, (float)maxR);
        
      do {
        buildPos = new AIFloat3(buildCenter);
        
        double r = minR + deltaR*( (double)numOfTry / (double)NUM_APPROX_POINTS ); // радиус точки.
        double ang=startAng + ((double)NUM_SpiralRot * (double)numOfTry / (double)NUM_APPROX_POINTS * (2*Math.PI));
        buildPos.x+=r*Math.sin(ang);buildPos.z+=r*Math.cos(ang);

//        if (needMetalSpots) // БЫЛО до получения метал. точек предварительно
//        { // если шахты одиночные.
//            AIFloat3 metalP=map.getResourceMapSpotsNearest(resMetal, buildPos);
//
//            if (map.isPossibleToBuildAt(unitType, metalP, owner.BUILD_FACING))
//                buildPos=metalP;
//              else
//                buildPos=null;
//        } else
//        { // простые здания
            // проверка на возможность строить здание здесь с учётом отступов
            AIFloat3 np=map.findClosestBuildSite(unitType, buildPos, MathPoints.getRadiusFlat(unitType)*1.8f, Math.min(unitType.getXSize(), unitType.getZSize())/2, owner.BUILD_FACING); // TODO test!
            if (!MathPoints.isValidPoint(np)) buildPos=null; else buildPos=np;
//        }
        
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

        if (needMetalSpots) {
            if (owner.owner.isMetalFieldMap==FieldBOT.IS_NO_METAL) return false; // !!!
            return findFreeMetalPointAt(unitType, owner.center, owner.radius)!=null;
        }

        // быстро проверить в центре
//        if (needMetalSpots) {
//            AIFloat3 metalP=owner.owner.clb.getMap().getResourceMapSpotsNearest(resMetal, owner.center);
//            float l=MathPoints.getDistanceBetweenFlat(metalP, owner.center);
//            if (l<owner.radius ) { //|| l<extrRadius) { // !!!!!
//                if (map.isPossibleToBuildAt(unitType, metalP, buildFacing)) return true;
//            } else {
//                // Если намного превышает радиус базы то НЕ СТРОИТЬ!!!
//                if (l>owner.radius *1.4f) return false;
//            }
//        } else {
            if (cashe_LastCanBuildPoint!=null) {
                if (map.isPossibleToBuildAt(unitType, cashe_LastCanBuildPoint, buildFacing)) return true;
            } else {
                if (map.isPossibleToBuildAt(unitType, owner.center, buildFacing)) {
                    cashe_LastCanBuildPoint=new AIFloat3(owner.center);
                    return true;
                }
            }
//        }
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

//            if (needMetalSpots) {
//                AIFloat3 metalP=owner.owner.clb.getMap().getResourceMapSpotsNearest(resMetal, buildPos);
//                float l=MathPoints.getDistanceBetweenFlat(metalP, buildPos);
//                if (l<owner.radius ) { //|| l<extrRadius) { // !!!!!!!!!
//                    if (map.isPossibleToBuildAt(unitType, metalP, buildFacing)) return true;
//                }
//            } else {
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
//            }
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
