/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package fieldbot.baseplaning;

import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.Unit;
import com.springrts.ai.oo.clb.UnitDef;
import fieldbot.ModSpecification;
import fieldbot.AIUtil.MathPoints;
import fieldbot.TBase;

/**
 *
 * @author PlayerO1
 */
public class TBasePlaning_spiral extends ABasePlaning{
    
    public TBasePlaning_spiral(TBase owner) {
        super(owner);
        freeSpaceUsingK=1.5f;
    }
    
    /**
     * на сколько заполнена база
     * @return 
     */
    @Override
    public float full() {
        return super.full();// TODO
    }
    /**
     * Возвращает рекомендованный радиус базы (для расширения)
     * @return 
     */
    @Override
    public float getRecomendedRadius() {
        return super.getRecomendedRadius();//owner.radius*(full+0.3f); // TODO
    }
    
    
     /**
     * Возвращает рекомендованную позицию для постройки зданий
     * @param unitType тип здания (или юнита)
     * @param mainBuilder строитель, может быть null
     * @return точка для строительства или null
     */
    @Override
    public AIFloat3 getRecomendetBuildPosition(UnitDef unitType, Unit mainBuilder) {

        AIFloat3 buildPos = null;
        
        double startAng = 0.0;
          if (full()>0.7) startAng = 2*Math.random()*Math.PI;
          
        double maxR = owner.radius;//unitType.getRadius();
        double minR = Math.max(maxR*0.10, MathPoints.getRadiusFlat(unitType));//unitType.getRadius(); // минимальный радиус
        
        
        AIFloat3 buildCenter;
        
        if (mainBuilder!=null &&
             (!ModSpecification.isRealyAbleToMove(mainBuilder.getDef()) // если строитель неподвижный
               ||
               ModSpecification.isRealyAbleToMove(unitType) // или то что строит движется
             )) {
            buildCenter = new AIFloat3(mainBuilder.getPos()); // если строитель не может двигаться то центр от него.
            maxR=mainBuilder.getDef().getBuildDistance();
            minR=Math.max(maxR*0.10, MathPoints.getRadiusFlat(mainBuilder.getDef()));
        }
        else buildCenter=owner.center; // центр базы для обычных строителей (рабочих)
        
        AIFloat3 searchNear;
        if (mainBuilder!=null) searchNear=mainBuilder.getPos(); else searchNear=null;
        
        // TODO это оставить для обычных ходячих строителей вне базы. Или для здания неподвижного.
        buildPos=getBuildPosition_Sppiral(unitType, buildCenter,searchNear, startAng, minR, maxR);
        
        
     // if (full<numOfTry/NUM_APPROX_POINTS) full=numOfTry/NUM_APPROX_POINTS; // подсчёт на сколько заполнено.

      return buildPos;
    }
    
    
    @Override
    public void onAdd(Unit unit) {
        super.onAdd(unit);
        // TODO
    }
    @Override
    public void onRemove(Unit unit) {
        super.onRemove(unit);
        // TODO
    }
    
}
