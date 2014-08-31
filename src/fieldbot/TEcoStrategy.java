/*
 * (C) PlayerO1 2014, GNU GPL v2 or above.
 */

package fieldbot;

import com.springrts.ai.oo.clb.Resource;
import com.springrts.ai.oo.clb.UnitDef;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;


class EcoStateInfo {
    public float resources[][];
    public float assistBuildPower;
    public float noAssistBuildPower;
    public float time;
    
    EcoStateInfo() {
        resources=null; // !!!
        assistBuildPower=noAssistBuildPower=0;
        time=0;
    }
    EcoStateInfo(float initResource[][]) {
        resources=AdvECO.cloneFloatMatrix(initResource);
        assistBuildPower=noAssistBuildPower=0;
        time=0;
    }
    EcoStateInfo(float initResource[][], float bPower) {
        resources=AdvECO.cloneFloatMatrix(initResource);
        assistBuildPower=bPower;
        noAssistBuildPower=0;
        time=0;
    }
    EcoStateInfo(EcoStateInfo copyFrom) {
        if (copyFrom.resources==null) resources=null;
        else resources=AdvECO.cloneFloatMatrix(copyFrom.resources);
        assistBuildPower=copyFrom.assistBuildPower;
        noAssistBuildPower=copyFrom.noAssistBuildPower;
        time=copyFrom.time;
    }
    
    @Override
    public String toString() {
        return "assistBuildPower="+assistBuildPower+" noAssist="+noAssistBuildPower+
               " time="+time+" resHave: "+Arrays.toString(resources[AdvECO.R_Current])+
               " resIncome: "+Arrays.toString(resources[AdvECO.R_Income])+
               " resUsage: "+Arrays.toString(resources[AdvECO.R_Usage]);
    }
}

/**
 *
 * @author PlayerO1
 */
public class TEcoStrategy {
    
    public final FieldBOT owner;
    /**
     * If true - using only incoming, if false - using incoming-usage resource.
     */
    public boolean useOptimisticResourceCalc;
    
    private static final boolean USE_STATIONAR_BUILDER=true; // Строить или нет NANO башни?
    private static final float MINIMAL_BUILD_TIME=2.6f; // !!! ничего быстрее 2-х секунд не построится.
    private static final float MAX_TECHUP_BUILD_TIME=7*60; // время, дольше которого нельзя делать переход на новый уровень
    private static final float MAX_BUILD_TIME_FOR_GENERATORRES=6*60; // не выбирать здания, которые строятся дольше.
  
    
    
    public TEcoStrategy(FieldBOT owner) {
        this.owner=owner;
        useOptimisticResourceCalc=true;
    }
    
    
    // =============== Вспомогательные утилиты =================
    
    /**
     * Вычисляет коэффициент затраченных ресурсов к произведённым.
     * @param def для кого расчитывать
     * @return выработка/стоимость
     */
    /*
    protected float getEcoProductK(UnitDef def) {
        float rIncoming,rEating,rCoast;
        rIncoming=rEating=rCoast=0.0f;
        for (Resource res : owner.avgEco.resName) {
            float r=owner.getUnitResoureProduct(def, res);
            if (r>0) rIncoming+=r;
            else if (r<0) rEating+=r;
            rCoast+=def.getCost(res);
        }
        // TODO разная стоимость ресурсов. Пока для простых сравнений
        float k = rIncoming / rCoast;
        if (rEating>1.0f) k = k/rEating;
        return k;
        //... 
    }//*/
    
    
    
// ---- Current economic ----
    
    /**
     * Return best producter of resource
     * @param res for this resource type, can use null for build power
     * @param buildVariants list for select
     * @param buildPower current build power
     * @return best resource generator. Maybe null.
     */
    protected UnitDef getBestGeneratorRes(Resource res,Collection<UnitDef> buildVariants,float buildPower, float[][] currentRes) {
//     if (res!=null) owner.sendTextMsg(" (for resource "+res.getName()+")" , FieldBOT.MSG_DBG_ALL);
//     else owner.sendTextMsg(" (for build power)" , FieldBOT.MSG_DBG_ALL);
        // TODO вынести в переменные класса
        final float USE_STORAGE_PERCENT=0.7f; // на сколько полагаться на хранящиеся ресурсы.
        
        UnitDef bestDef=null;
        float maxK=0.0f;
        float minT,maxT,currentT; // минимальное, максимальное время строительства объектов. Если разница >2, то информация не актуальна (нелинейная зависимость)
        minT=maxT=currentT=-1.0f; // Todo using magic number Float.NaN
        for (UnitDef ud:buildVariants) { // Взять максимально производящий электричество
            float r=0.0f;
            if (res!=null) r=owner.getUnitResoureProduct(ud,res);
            else { 
                if (ModSpecification.isRealyAbleToAssist(ud)) { // выбор рабочего...
                    r=ud.getBuildSpeed();
                    if (!USE_STATIONAR_BUILDER && !ModSpecification.isRealyAbleToMove(ud)) r=r/1000.0f; // TODO что лучше: неподвижный или подвижный строитель?
                }
            }
            if (r>0.0f)
            {
                float t=getRashotBuildTime(ud, currentRes, buildPower, USE_STORAGE_PERCENT); // было ..., owner.clb.getEconomy() ...
                float k=r/t; // отношение выработки ресурса к скорости постройки
                if (t>MAX_BUILD_TIME_FOR_GENERATORRES) k=k/(10+t); // не выбирать долгострой.
                // TODO УЧЕСТЬ НЕХВАТКУ РАЗМЕРА БАЗЫ, стоимость полщади!!!
                // !!!!!!!!!!!!!!!!!
                if (!ModSpecification.isRealyAbleToMove(ud)) k = 0.7f*k + 0.3f*k/(ud.getRadius()*2);
                // !!!!!!!!!!!!!!!!!

                // Учёт выгодности Metal Makers
                if (owner.modSpecific.exist_MetalMaker_lua)
                 if (owner.modSpecific.isMetalMaker(ud.getName())) // TODO test it
                {
                    float makerK=owner.modSpecific.getConversionKMetalMaker(ud.getName());
                    if (makerK!=Float.POSITIVE_INFINITY) k = (0.5f*k)+(0.5f * k * makerK );
                }
                // учёт выгодности Metal Makers в Evo RTS (там коэффициэнт по другим функциям считается)
                if (owner.modSpecific.exist_MetalMaker_native)
                 if (owner.modSpecific.isNativeMetalMaker(ud)) // TODO test it
                {
                    float makerK=owner.modSpecific.getNativeConversionKMetalMaker(ud);
                    if (makerK!=Float.POSITIVE_INFINITY) k = (0.5f*k)+(0.5f * k * makerK );
                }
    //     owner.sendTextMsg(" >item "+ud.getName()+" k="+k+" =r/t, r="+r+" t="+t , FieldBOT.MSG_DBG_ALL);

                if (k>0.0f) { // только для тех, кто производит хоть немного заданный ресурс.
                    if (minT>t || minT==-1.0f) minT=t; // для статистики
                    if (maxT<t || maxT==-1.0f) maxT=t;
                    // если разница во времени большая, то зависимость НЕ ЛИНЕЙНАЯ!!!!!!!!!!!!!!!
                    // TODO время вычислять не учитывая накопленные запасы, или их часть.

                    if (k>=maxK) {
                        bestDef=ud;
                        maxK=k;
                        currentT=t;
                    }
                }
            }
        }
        //if (maxT>minT*2.0f) owner.sendTextMsg(" nelineynaya zavisimost <- build time: minT="+minT+" maxT="+maxT+" currentT="+currentT , FieldBOT.MSG_DBG_ALL);
        if (minT*2.0f<currentT) {// более чем в 2 раза отличается минимальное и текущее время постройки.
            // - - - сохранение прошлых расчётных параметров - - -
            float lst_maxK=maxK;
            UnitDef lst_bestDef=bestDef;
            // - - -
//            owner.sendTextMsg(" !!!Non-linear зависимость ТОЧНО! (current >> min) TODO Доделать!" , FieldBOT.MSG_DBG_SHORT);
//            if (bestDef!=null) owner.sendTextMsg(" last linear select: "+bestDef.getName()+" time="+currentT , FieldBOT.MSG_DBG_SHORT);
            // TODO нелинейно всё пересчитать. getDynamicRashotBuildTime !!!
            maxK=0.0f; // TODO может не обнулять?...
            bestDef=null; // !!!!!!
            float bestN=0.0f;// FOR DEBUG!!!
            for (UnitDef ud:buildVariants) { // Взять максимально производящий ресурс
               float r=0.0f;
               if (res!=null) r=owner.getUnitResoureProduct(ud,res);
                else { 
                  if (ud.isAbleToAssist()) r=ud.getBuildSpeed(); // выбор рабочего...
                  if (ModSpecification.isStationarFactory(ud)) r=0.0f; //не строить фабрики... ??? // TODO !!!!
                }
               if (r>0) {
                int maxItr=Math.min(150, Math.round(currentT/minT)); // this using constant value!

                float itrInfo[]=getDynamicRashotBuildInfo(ud, currentRes, buildPower, maxItr, currentT);
                float t=itrInfo[0];
                float n=itrInfo[1];
                r = r * n; // r * кол-во
                float k=r/t; // отношение выработки ресурса к скорости постройки
//                owner.sendTextMsg(" >item (itr): "+ud.getName()+" k="+k+"=r*n/t t="+t+" n="+n+" r="+r , FieldBOT.MSG_DBG_ALL);
                if (k>0.0f) { // только для тех, кто производит хоть как-то заданный ресурс.
                    //if (minT>t || minT==-1.0f) minT=t;
                    //if (maxT<t || maxT==-1.0f) maxT=t;
                    if (k>=maxK) {
                        bestDef=ud;
                        maxK=k;
                        currentT=t;
                        bestN=n;
                    }
                }
               }
            }
//            if (bestDef!=null) owner.sendTextMsg(" new dynamic select: "+bestDef.getName()+" time="+currentT+" n="+bestN , FieldBOT.MSG_DBG_ALL);
//                else owner.sendTextMsg(" dynamic select is NULL!", FieldBOT.MSG_DBG_SHORT);
            
            // - - - сравнение с прошлыми расчётными параметрами - - -
            if (lst_maxK>maxK) {
//                owner.sendTextMsg(" Skeep dynamic select (lstK="+lst_maxK+" dynamicK="+maxK+"). Back to "+lst_bestDef.getName(), FieldBOT.MSG_DBG_SHORT);
                bestDef=lst_bestDef;
                maxK=lst_maxK;
            }
            // - - -

        }
        return bestDef;
    }
    
    /**
     * Get recomended build power for build this unit.
     * @param build shoose for this
     * @param currentRes economic state
     * @param resCurrentMultipler how use resource storage (1.0 use all resource in storage - current, 0.0 no use current - only from income/usage)
     * @return optimal build power for build 
     */
    protected float getOptimalBuildPowerFor(UnitDef build,float[][] currentRes,float resCurrentMultipler)
    {
        final Resource resName[]=owner.avgEco.resName; // !!! test
        float T=MINIMAL_BUILD_TIME;
        for (int rN=0; rN<currentRes[resName.length].length;rN++) {
            float nehvatkaRes=build.getCost(resName[rN])-resCurrentMultipler*currentRes[AdvECO.R_Current][rN];
            if (nehvatkaRes>0) {
                //float resPostupl=currentRes[AdvECO.R_Income][rN]-currentRes[AdvECO.R_Usage][rN];
                float resPostupl=currentRes[AdvECO.R_Income][rN]-currentRes[AdvECO.R_Usage][rN]; // !!!!!!!!!!!!!
                if (resPostupl<=1.0f) resPostupl=currentRes[AdvECO.R_Income][rN]*1.5f/currentRes[AdvECO.R_Usage][rN];
                
                float t;
                if (resPostupl>1.0f) t = nehvatkaRes/ resPostupl;
                else {
                    t=1e+16f; // TODO !!!!!!
                }
                if (t>T) T=t; // max
            }
        }
        //if (T<MINIMAL_BUILD_TIME) T=MINIMAL_BUILD_TIME; // !!! ничего быстрее 2-х секунд не построится.
        float buildPower = build.getBuildTime() / T;
        return buildPower; // рекомендованная скорость (мощность) строителей
    }
    
    /**
     * Оценка времени постройки цепочки юнитов подряд.
     * @param builds что строить
     * @param startRes текущие ресурсы
     * @param buildSpeed текущая мощность строительства
     * @return время постройки всех зданий
     */
    protected float getRashotBuildTimeLst (ArrayList<UnitDef> builds,float[][] startRes,float buildSpeed) {
        float T=0.0f;
        float[][] currentRes = AdvECO.cloneFloatMatrix(startRes);
        // TODO if isAssistable=false...
        for (UnitDef def:builds) {
            float t=getRashotBuildTime(def, startRes, buildSpeed, 1.0f);
            T+=t;
            for (int i=0;i<owner.avgEco.resName.length;i++) {
                currentRes[AdvECO.R_Current][i]+= t * (currentRes[AdvECO.R_Income][i]-currentRes[AdvECO.R_Usage][i])
                                               - def.getCost(owner.avgEco.resName[i]);
                float deltaRes=owner.getUnitResoureProduct(def, owner.avgEco.resName[i]);
                if (deltaRes>0.0f)
                    currentRes[AdvECO.R_Income][i] += deltaRes;
                else if ((deltaRes<0.0f))
                    currentRes[AdvECO.R_Usage][i] -= deltaRes;

                currentRes[AdvECO.R_Storage][i]+= def.getStorage(owner.avgEco.resName[i]); // TODO где используется storage при расчётах?
            }
            if (def.isAbleToAssist()) buildSpeed += def.getBuildSpeed(); // учесть строительство
        }
        return T;
    }
    
    /**
     * Вычисляет, сколько надо ждать до достижения такого колличества ресурсов
     * @param currentRes текущее состояние экономики
     * @param toRes ожидаемые запасы, выше или равно
     * @return время ожидания.
     */
    protected float getWaitTimeForProductResource(float[][] currentRes, float toRes[]) { // TODO use int at all
        final Resource resName[]=owner.avgEco.resName; // !!! test
        float T=0;
        for (int rN=0; rN<currentRes[resName.length].length;rN++) {
            float nehvatkaRes=toRes[rN]-currentRes[AdvECO.R_Current][rN];
            if (nehvatkaRes>0.0f) {
                float resPostupl=currentRes[AdvECO.R_Income][rN]-currentRes[AdvECO.R_Usage][rN];
                resPostupl = Math.max(resPostupl, currentRes[AdvECO.R_Income][rN] / 2.0f);// !!!
                //if (resPostupl<=1.0f) resPostupl=currentRes[AdvECO.R_Income][rN]/currentRes[AdvECO.R_Usage][rN];
                float t;
                if (resPostupl>0) t = nehvatkaRes/ resPostupl;
                else {
                    t = 1e+20f;// !!!!!!!!!!!!
                    //t=((currentRes[AdvECO.R_Income][rN]-currentRes[AdvECO.R_Usage][rN]))/(currentRes[AdvECO.R_Income][rN]+1.0f)*1e+6f;
                }
                if (t>T) T=t; // max
            }
        }
        return T;
    }
            
    
    
    
    /**
     * Оценить время постройки.
     * @param build что строить
     * @param currentRes текущие ресурсы (запасы+поступление)
     * @param buildSpeed мощность (скорость) строителей (buildPower)
     * @param resCurrentMultipler 1.0 - учитывать текущие ресурсы, 0.0 - учитывать как будто запасов нет.
     * @return время для постройки
     */
    protected float getRashotBuildTime(UnitDef build,float[][] currentRes,float buildSpeed, float resCurrentMultipler) {
        final Resource resName[]=owner.avgEco.resName; // !!! test
        float T=MINIMAL_BUILD_TIME;  // !!! ничего быстрее 2-х секунд не построится.
        // TODO сделать boolean mojetVliatNaPosleduyshie=false;... если критич. ресурсы не меняются, то нет и зависимость линейна.
        // TODO use getWaitTimeForProductResource. часть кода отсюда уже скопированно в getWaitTimeForProductResource()
        for (int rN=0; rN<currentRes[resName.length].length;rN++) {
            float nehvatkaRes=build.getCost(resName[rN])-resCurrentMultipler*currentRes[AdvECO.R_Current][rN];
            if (nehvatkaRes>0.0f) {
                //float resPostupl=currentRes[AdvECO.R_Income][rN]-currentRes[AdvECO.R_Usage][rN]; // было
                // ТЕСТ!
                float resPostupl=currentRes[AdvECO.R_Income][rN]-currentRes[AdvECO.R_Usage][rN];
                resPostupl = Math.max(resPostupl, currentRes[AdvECO.R_Income][rN] / 2.0f);// !!!
                //if (resPostupl<=1.0f) resPostupl=currentRes[AdvECO.R_Income][rN]/currentRes[AdvECO.R_Usage][rN];
                
                float t;
                if (resPostupl>0) t = nehvatkaRes/ resPostupl;
                else {
//                    owner.sendTextMsg("dbg: Resursov ne hvatit! income="+currentRes[AdvECO.R_Income][rN]+" usage="+currentRes[AdvECO.R_Usage][rN], FieldBOT.MSG_DBG_SHORT);
                    t = 1e+20f;// !!!!!!!!!!!!
                    //t=((currentRes[AdvECO.R_Income][rN]-currentRes[AdvECO.R_Usage][rN]))/(currentRes[AdvECO.R_Income][rN]+1.0f)*1e+6f;
                    // TODO !!!!!!
                }
                if (t>T) T=t; // max
            }
        }
        
        if (buildSpeed>0.0f) T=Math.max(T, build.getBuildTime()/buildSpeed);
        
        return T;
    }
    
    
    /**
     * Dynamic modeling of one economic building when time may have recursion depends of build.
     * @param build what build
     * @param startRes start economic info
     * @param buildSpeed start build speed
     * @param maxItr iteration limit
     * @param maxTime time limit
     * @return array contain 2 float: [0] - total time [1] num of iter (building count)
     */
    private float[] getDynamicRashotBuildInfo(UnitDef build,float[][] startRes,float buildSpeed,int maxItr,float maxTime) {
        //TODO !доделать!: Если дальше последовательность будет тормозить себя, то возвращает только ближайщую, не считает ухудшение. !!! или не надо?...)
        //TODO Remove recursion, exchange to cycle FOR.
        maxItr--; // уже виртуально "построили", -1
        float nItrPass=1.0f;
        float t=getRashotBuildTime(build, startRes, buildSpeed, 1.0f);
        if (t<maxTime && maxItr>0) {
            if (build.isAbleToAssist()) buildSpeed += build.getBuildSpeed(); // учесть строительство
            //owner.sendTextMsg("dbg: build:"+build.getName()+" t<MaxT: t="+t+" maxTime="+maxTime, FieldBOT.MSG_DBG_ALL);
            float[][] currentRes = AdvECO.cloneFloatMatrix(startRes);
            
            for (int i=0;i<owner.avgEco.resName.length;i++) {
                currentRes[AdvECO.R_Current][i]=startRes[AdvECO.R_Current][i]
                        + t * (startRes[AdvECO.R_Income][i]-startRes[AdvECO.R_Usage][i]) // !!!!!!!!! Math.max(startRes[AdvECO.R_Income][i]-startRes[AdvECO.R_Usage][i],startRes[AdvECO.R_Income][i]/2.0f)
                        - build.getCost(owner.avgEco.resName[i]);
                float deltaRes=owner.getUnitResoureProduct(build, owner.avgEco.resName[i]);
                currentRes[AdvECO.R_Income][i]=startRes[AdvECO.R_Income][i];
                if (deltaRes>0.0f)
                    currentRes[AdvECO.R_Income][i] += deltaRes;
                else if ((deltaRes<0.0f))
                    currentRes[AdvECO.R_Usage][i] -= deltaRes;
                // TODO где используется storage при расчётах?
                currentRes[AdvECO.R_Storage][i]=startRes[AdvECO.R_Storage][i] + build.getStorage(owner.avgEco.resName[i]);
            }
            
            float itrInfo[]=getDynamicRashotBuildInfo( build, currentRes, buildSpeed, maxItr, maxTime - t );
            
            //if (itrInfo[0]<=t) { // TODO test: если дальше дольше, то не считать что дальше!!!!
                t += itrInfo[0]; // !!!!!!!!!!!!!!!!!!!!!!
                nItrPass += itrInfo[1];
                //TODO !!! если ресурсы сейчас есть, то всегда будет itrInfo[0]>t, если нет - то ???
            //}
        }
        float itrInfo[] = {t,nItrPass};
        return itrInfo;
    }
    
    
     /**
     * Select economic building for now.
     * @param buildPower current build power
     * @param buildVariants list UDefs for select
     * @param currentRes shoose for this resource. May be null for using default.
     * @return best economic building, or null.
     */
    public UnitDef getOptimalBuildingForNow(float buildPower,List<UnitDef> buildVariants, float[][] currentRes) {
//        owner.sendTextMsg(" Choose best from "+buildVariants.size()+" unit defs:" , FieldBOT.MSG_DBG_SHORT);//DEBUG!
//        owner.sendTextMsg(" AVG Resourse info - "+owner.avgEco.toString() , FieldBOT.MSG_DBG_ALL);//DEBUG!
        
        // --------
        UnitDef bestUnit=null; // Что строить !!!!!!!!
        //Economy eco = owner.clb.getEconomy();
        Resource resM=owner.clb.getResourceByName("Metal");
        Resource resE=owner.clb.getResourceByName("Energy");
        // - - - - -
        
        //float buildPower=onBase.getBuildPower(false,true); // !!!
        if (buildPower<1) {
            owner.sendTextMsg("Warning! buildPower="+buildPower , FieldBOT.MSG_DBG_SHORT);
            buildPower=1;
        }
        
        //float currentRes[][];
        if (currentRes==null) {
            if (useOptimisticResourceCalc) currentRes= owner.avgEco.getSmartAVGResourceToArr();
            else currentRes= owner.avgEco.getAVGResourceToArr();
        }
        
        UnitDef electrostanciya=getBestGeneratorRes(resE,buildVariants,buildPower, currentRes);
        UnitDef metalproduct=getBestGeneratorRes(resM,buildVariants,buildPower, currentRes);
        
        // TODO ...
        // mmaker = 70%-80%
        float avg_cur_M=currentRes[AdvECO.R_Current][0];//owner.avgEco.getAvgCurrent(resM);
        float avg_stor_M=currentRes[AdvECO.R_Storage][0];//owner.avgEco.getAvgStorage(resM);
        float avg_delta_M=currentRes[AdvECO.R_Income][0]-currentRes[AdvECO.R_Usage][0];//owner.avgEco.getAvgIncome(resM)-owner.avgEco.getAvgUsage(resM);
        float avg_cur_E=currentRes[AdvECO.R_Current][1];//owner.avgEco.getAvgCurrent(resE);
        float avg_stor_E=currentRes[AdvECO.R_Storage][1];//owner.avgEco.getAvgStorage(resE);
        float avg_delta_E=currentRes[AdvECO.R_Income][1]-currentRes[AdvECO.R_Usage][1];//owner.avgEco.getAvgIncome(resE)-owner.avgEco.getAvgUsage(resE);
        
        // Экономика
        final float EconvertK=owner.modSpecific.metal_maker_workZone*0.99f;
        
        if (owner.isMetalFieldMap==FieldBOT.IS_NO_METAL || owner.isMetalFieldMap==FieldBOT.IS_NORMAL_METAL)
        {
            if ( avg_cur_E/avg_stor_E <= 0.73 || (avg_delta_E<1 && avg_cur_E/avg_stor_E <= 0.97)) {
//                owner.sendTextMsg(" cel->electrostanciya" , FieldBOT.MSG_DBG_SHORT);
                bestUnit = electrostanciya;
            }
            if ( avg_cur_M/avg_stor_M < 0.5  &&  avg_cur_E/avg_stor_E >= EconvertK && avg_delta_E>1) {
//                owner.sendTextMsg(" cel->metal" , FieldBOT.MSG_DBG_SHORT);
                bestUnit = metalproduct;
            }
            if (bestUnit==null) bestUnit = metalproduct;
            if ( avg_cur_M/avg_stor_M>0.9 && avg_delta_M>0) bestUnit = electrostanciya;
        }
        else if (owner.isMetalFieldMap==FieldBOT.IS_METAL_FIELD)
        {
            if ( avg_cur_E/avg_stor_E <= 0.50 || avg_delta_E*2<avg_delta_M) {
//                owner.sendTextMsg(" cel->electrostanciya" , FieldBOT.MSG_DBG_SHORT);
                bestUnit = electrostanciya;
            }
            if ( avg_cur_M/avg_stor_M < 0.50  || avg_delta_M*2<avg_delta_E) {
//                owner.sendTextMsg(" cel->metal" , FieldBOT.MSG_DBG_SHORT);
                bestUnit = metalproduct;
            }
            if (bestUnit==null) bestUnit = electrostanciya;
            if ( avg_cur_M/avg_stor_M>0.9 && avg_delta_M>0) bestUnit = electrostanciya;
        } //TODO else log.error!
        
        //todt use mod specification
        
        // нехватка рабочих:
        if (bestUnit!=null && avg_cur_E/avg_stor_E > 0.60 && avg_cur_M/avg_stor_M >0.55 && avg_delta_M>1 && (avg_delta_E>2 || avg_cur_E/avg_stor_E > EconvertK)) {
//            float currentRes[][];
//            if (useOptimisticResourceCalc) currentRes= owner.avgEco.getSmartAVGResourceToArr();
//            else currentRes= owner.avgEco.getAVGResourceToArr();
            float optimalBPower=getOptimalBuildPowerFor(bestUnit, currentRes, 0.20f);
//            owner.sendTextMsg(" Check rabochih: buildPower="+buildPower+" optimalBPower="+optimalBPower , FieldBOT.MSG_DBG_SHORT); // DEBUG DBG_ALL
            // TODO...
            if (buildPower*1.02f < optimalBPower) { // нехватает рабочих, они улучшают больше чем в 1,4 раза
                UnitDef worker=getBestGeneratorRes(null,buildVariants,buildPower, currentRes);
                if (worker!=null)
                 if (worker.getBuildSpeed() < optimalBPower-buildPower*1.0015f)
                {
                    bestUnit=worker;
//                    owner.sendTextMsg(" Nujni rabochie current buildPower="+buildPower+" optimalBPower="+optimalBPower , FieldBOT.MSG_DBG_SHORT);
                }
            }
        }
        // -------- ------- --------
        
//        if (bestUnit!=null) owner.sendTextMsg(" vibor: "+bestUnit.getName()+" - " + bestUnit.getHumanName()  , FieldBOT.MSG_DBG_ALL);
//        else owner.sendTextMsg(" NE VIBRAN!" , FieldBOT.MSG_DBG_SHORT);

        return bestUnit;
    }
  
    
    
// ---- Tech Lewel chooise ----
    
    /**
     * Select better Tech Level that have now
     * @param TechLvls list for choose
     * @param onBase on Base
     * @param onBaseBuildLst current build list
     * @param currEcoLvl for compare level, best that this eco product
     * @param currentRes current resource
     * @return list with Tecl Hevels when same items of currEcoLvl[] > that currentEco[]
     */
    private ArrayList<TTechLevel> selectLvl_whoBetterThat(ArrayList<TTechLevel> TechLvls, float[] currEcoLvl, float currentMaxBuilder)
    {
        ArrayList<TTechLevel> bestLvls=new ArrayList<TTechLevel>();
        // Выбор уровня с лучшей экономикой
        for (TTechLevel level:TechLvls) {
            float k=0.0f;
            for (int i=0;i<currEcoLvl.length;i++) {
                float delta=level.ecoK[i]-currEcoLvl[i];
                if (delta>0) k+=delta;
            }
            
            float workK=level.builderK-currentMaxBuilder;
            if (workK>0) k+=workK/100; // !!!
            
            if (k>0.0f) bestLvls.add(level);
        }
        return bestLvls;
    }
    
/**
 * Choose level with best economic from level list
 * @param TechLvls list for select
 * @param onBase for get build power
 * @param bestEco current economic max 1 building can product, for choose best that this
 * @param currentMaxBuilder max build power of 1 unit when can builn on this level
 * @param currentRes current economic state
 * @return best level, or null
 */
public TTechLevel selectLvl_bestForNow(ArrayList<TTechLevel> TechLvls, TBase onBase, float bestEco[], float currentMaxBuilder, float currentRes[][])
{
    TTechLevel bestLvl=null;
    float bestEcoK=0.0f;
    for (TTechLevel level:TechLvls)
    {
        float k=0.0f;
        for (int i=0;i<bestEco.length;i++) {
            float delta=level.ecoK[i]-bestEco[i];
            if (delta>0) k+=delta;
        }

        float workK=level.builderK-currentMaxBuilder;
        if (workK>0) k+=workK/100; // !!!

        float tBuild=level.getTimeForBuildTechLevel(onBase.getBuildPower(false, true), currentRes);
        if (tBuild<10.0f) tBuild=10.0f;// Нельзя построить так быстро...
        k= k / tBuild; // Выгодность / время постройки.
        // TODO учесть + время строительства электростанций

        if (k>0.0f)
        {
            if (k==bestEcoK) owner.sendTextMsg(" !!! Tech level have equals bestK="+bestEcoK+" K="+k+" level="+level.toString(),FieldBOT.MSG_DBG_SHORT);

            if (k>bestEcoK) { // Лучше. А можно это построить сейчас?
                owner.sendTextMsg(" >>best level k="+k+" t="+tBuild+" (AssistWorkTime="+level.getAssistWorkTime()+" getBuildPower="+onBase.getBuildPower(false, true)+" NOAssistWorkTime="+level.getNOAssistWorkTime()+" level:"+level.toString(), FieldBOT.MSG_DBG_ALL); //DEBUG !!!
                //if (canTechUp) {
                bestEcoK=k;
                bestLvl=level;
                //}
            }
        }
    }
    return bestLvl;
}


/**
 * Fast check: do need execute tech up now?
 * Not optimized tech up time, only max MAX_TECHUP_BUILD_TIME check.
 * @param CurrentLevelUnitsCanBuild what current level can build
 * @param toLevel new level
 * @return true - need tech up now, false - do not tech up to this level now.
 */
public boolean isActualDoTechUp_fastCheck(TBase onBase, HashSet<UnitDef> CurrentLevelUnitsCanBuild, TTechLevel toLevel) {
    HashSet<UnitDef> futureBuildLst=new HashSet<UnitDef>(CurrentLevelUnitsCanBuild);
    futureBuildLst.addAll(toLevel.needBaseBuilders);
    futureBuildLst.addAll(toLevel.levelNewDef);
    
    // Проверка по всем ресурсам, чего лучше строить.
    float buildPower=onBase.getBuildPower(false,true); // !!!
    int toNewLvl=0, toLastLvl=0;

    // в ресурсы добавить null для сравнения рабочих
    Resource[] checksRes=new Resource[owner.avgEco.resName.length+1];
    for (int i=0;i<owner.avgEco.resName.length;i++) checksRes[i]=owner.avgEco.resName[i];
    checksRes[checksRes.length-1]=null;// workers
    
    float currentRes[][];
    if (useOptimisticResourceCalc) currentRes= owner.avgEco.getSmartAVGResourceToArr();
    else currentRes= owner.avgEco.getAVGResourceToArr();
    
    for (Resource res:checksRes) {
        UnitDef ud = //ecoStrategy.getOptimalBuildingForNow(onBase, futureBuildLst);
                getBestGeneratorRes(res,futureBuildLst, buildPower, currentRes);
        if (ud!=null) {
            if (toLevel.levelNewDef.contains(ud)) toNewLvl++;
            if (CurrentLevelUnitsCanBuild.contains(ud)) toLastLvl++;
        }
    }
    //TODO учесть за период + расходы на строительство!!!
    return toNewLvl>0 && toLevel.getTimeForBuildTechLevel(buildPower, currentRes)<MAX_TECHUP_BUILD_TIME; //TODO test it.
}

/**
 * Select only ecoing unit from list def.
 * @param defs all units
 * @return only economic units
 */
private ArrayList<UnitDef> select_EconomicUDefs(HashSet<UnitDef> defs)
{
    ArrayList<UnitDef> newLst=new ArrayList<UnitDef>();
    for (UnitDef def:defs)
    { // select: if is it builder or it is able to make some resource.
        boolean isEcoUnit= (def.isBuilder() || def.isAbleToAssist()) && def.getBuildSpeed()>0;
        if (!isEcoUnit) for (Resource r:owner.clb.getResources()) {
            if ( owner.getUnitResoureProduct(def, r) > 0 ) {
                isEcoUnit=true;
                break;
            }
        }
        if (isEcoUnit) newLst.add(def);
    }
    return newLst;
}



/**
 * Modeling economic to future.
 * @param maxT max extrapolation time
 * @param startRes start economic parameter
 * @param buildPower start economic working power (assistable)
 * @param ecoBuildWariant what building I have for make economic
 * @return Economic output information from future
 */
private EcoStateInfo calcEcoAftherTime(float maxT, EcoStateInfo ecoStart, ArrayList<UnitDef> ecoBuildWariant) {
    //float currentRes[][]= AdvECO.cloneFloatMatrix(startRes);
    final EcoStateInfo newEcoState=new EcoStateInfo(ecoStart);
    final float [][] currentRes=newEcoState.resources;// for fast call?
//    owner.sendTextMsg(" calcEcoAftherTime ecoBuildWariant size="+ecoBuildWariant.size(), FieldBOT.MSG_DBG_ALL);//!!!
    
    final int MAX_ITERATION=1000;
    int nItr=0;
    float T=0;
    UnitDef build=null;
    do {
        nItr++;
        build=getOptimalBuildingForNow(newEcoState.assistBuildPower, ecoBuildWariant, currentRes);

        float t=getRashotBuildTime(build, newEcoState.resources, newEcoState.assistBuildPower, 1.0f); // FIXME check for null!
//    owner.sendTextMsg("  nItr="+nItr+" build="+build.getName()+" t="+t, FieldBOT.MSG_DBG_ALL);//!!!
        if (T+t<maxT) {
            T+=t; // учёт времени.
            if (build.isAbleToAssist()) newEcoState.assistBuildPower += build.getBuildSpeed(); // учесть строительство
            else newEcoState.noAssistBuildPower += build.getBuildSpeed(); // !!!
            
            for (int i=0;i<owner.avgEco.resName.length;i++) {
                currentRes[AdvECO.R_Current][i]=currentRes[AdvECO.R_Current][i]
                        + t * (currentRes[AdvECO.R_Income][i]-currentRes[AdvECO.R_Usage][i]) // !!!!!!!!! Math.max(startRes[AdvECO.R_Income][i]-startRes[AdvECO.R_Usage][i],startRes[AdvECO.R_Income][i]/2.0f)
                        - build.getCost(owner.avgEco.resName[i]);

                // Учесть потери от нехватки складов
                if (currentRes[AdvECO.R_Current][i]>currentRes[AdvECO.R_Storage][i])
                    currentRes[AdvECO.R_Current][i]=currentRes[AdvECO.R_Storage][i];
                // И невозможности отрицательных ресурсов
                if (currentRes[AdvECO.R_Current][i]<0) currentRes[AdvECO.R_Current][i]=0;// !!!! TODO если не пропали, то что-то уменьшилось.
                
                float deltaRes=owner.getUnitResoureProduct(build, owner.avgEco.resName[i]);
                if (deltaRes>0.0f)
                    currentRes[AdvECO.R_Income][i] += deltaRes;
                else if ((deltaRes<0.0f))
                    currentRes[AdvECO.R_Usage][i] -= deltaRes;

                // storage
                currentRes[AdvECO.R_Storage][i]+= + build.getStorage(owner.avgEco.resName[i]);
            }
            
        } else break;// if time not enought - stop cicle.
    } while (T<maxT && nItr<MAX_ITERATION && build!=null);
    newEcoState.time+=T;
    return newEcoState;
}

/**
 * Precission that isActualDoTechUp_fastCheck, required CPU time!
 * @return Tech level trigger with economic state, current level time before tech up.
 * TRIGGER must contain: start tech up time, t=0 - now; trigger resource - ResHave+ResProduct*time_for_tech_up, tech level info
 */
public TechUpTrigger getPrecissionTechUpTimeFor(TBase onBase, HashSet<UnitDef> CurrentLevelUnitsCanBuild, TTechLevel toLevel)
{
    ArrayList<UnitDef> currentLevelDefs=select_EconomicUDefs(CurrentLevelUnitsCanBuild); // убрать лишних юнитов для ускорения расчётов
    HashSet<UnitDef> hs_futureBuildLst=new HashSet<UnitDef>(toLevel.needBaseBuilders);
    hs_futureBuildLst.addAll(toLevel.levelNewDef);
    // TODO check futureBuildLst.isEmpty() - если да, то в уровне нет экономических юнитов.
    hs_futureBuildLst.addAll(currentLevelDefs);
    ArrayList<UnitDef> futureBuildLst=select_EconomicUDefs(hs_futureBuildLst); // убрать лишних юнитов для ускорения расчётов
    
    //
    // Проверка по всем ресурсам, чего лучше строить.
    float currentLvlPower=onBase.getBuildPower(false,true); // !!!
    
    // в ресурсы добавить null для сравнения рабочих
    float startRes[][];
    if (useOptimisticResourceCalc) startRes= owner.avgEco.getSmartAVGResourceToArr();
    else startRes= owner.avgEco.getAVGResourceToArr();
    
    final float TEST_TIME=20*60; // 20 min
    final int N_ITER=12;

owner.sendTextMsg(" <getPrecissionTechUpTimeFor>, workers F: "+currentLvlPower+" N_ITER="+N_ITER, FieldBOT.MSG_DBG_ALL);
    
    float[][] exp_tupTriggerR=null, tmp_exp_tupTriggerR;// TODO!!!!!
    float exp_endRes[][];
    float exp_tupTimeStart=0;
    float exp_totalTime;
    float exp_levelBuildTime=0;
    float exp_K=0.0f;
    
    // Economic state without tech up
    EcoStateInfo noTUp_ecoState=new EcoStateInfo(startRes, currentLvlPower);
    noTUp_ecoState=calcEcoAftherTime(TEST_TIME,noTUp_ecoState, currentLevelDefs);
    exp_K=owner.avgEco.getResourceCoast(noTUp_ecoState.resources);
    exp_K /= noTUp_ecoState.time;
    boolean exp_no_TechUp=true;
    
    
    for (int i=0; i<N_ITER; i++)
    {
        // Итерация
        float t1=TEST_TIME*(0.50f * (float)i/(float)N_ITER); // 0%-70% of TEST_TIME
        EcoStateInfo ecoState=new EcoStateInfo(startRes, currentLvlPower);
        ecoState=calcEcoAftherTime(t1,ecoState, currentLevelDefs);
        
        tmp_exp_tupTriggerR=AdvECO.cloneFloatMatrix(ecoState.resources);
        
owner.sendTextMsg("i="+i+", t1="+t1+" ecoState="+ecoState.toString(), FieldBOT.MSG_DBG_ALL);
        // -- TechUP --
        float t2=toLevel.getTimeForBuildTechLevel(ecoState.assistBuildPower, ecoState.resources);
        float tUpRes[]=toLevel.getNeedRes();
        for (int r=0;r<tUpRes.length;r++) {
            ecoState.resources[AdvECO.R_Current][r] += 
                    t2 *
                    (ecoState.resources[AdvECO.R_Income][r]-ecoState.resources[AdvECO.R_Usage][r])
                    - tUpRes[r];
            if (ecoState.resources[AdvECO.R_Current][r]<0) {
                //FIXME negative resource
                float deltaR=ecoState.resources[AdvECO.R_Income][r]-ecoState.resources[AdvECO.R_Usage][r];
                if (deltaR>0) {
                    t2=Math.max(t2, -ecoState.resources[AdvECO.R_Current][r]/deltaR);
                } else t2+=1e+20; //FIXME !!!!!!!!
                owner.sendTextMsg("> error, resource "+r+" is negative! value="+ecoState.resources[AdvECO.R_Current][r]+", time T2 fix to "+t2, FieldBOT.MSG_DBG_SHORT);
            }
        }
        for (UnitDef lvlBuilder:toLevel.needBaseBuilders) {
            if (lvlBuilder.isAbleToAssist())
                ecoState.assistBuildPower +=lvlBuilder.getBuildSpeed();
            if (lvlBuilder.isBuilder() && !lvlBuilder.isAbleToAssist())
                ecoState.noAssistBuildPower +=lvlBuilder.getBuildSpeed();
        }
        // if by morph level
        if (toLevel.byMorph()) {
            UnitDef morphFrom=toLevel.byMorph.getDef();
            if (morphFrom.isAbleToAssist()) ecoState.assistBuildPower -= morphFrom.getBuildSpeed();
            if (morphFrom.isBuilder() && !morphFrom.isAbleToAssist()) ecoState.noAssistBuildPower -= morphFrom.getBuildSpeed();
            // TODO resource income/usage for lost morph unit.
        }
        ecoState.time+=t2;
        // ----
owner.sendTextMsg("i="+i+", t2="+t2+" ecoState="+ecoState.toString(), FieldBOT.MSG_DBG_ALL);
        
        float t3=TEST_TIME-t1-t2; // TODO if T3<0 ?
        
        ecoState=calcEcoAftherTime(t3,ecoState, futureBuildLst);

owner.sendTextMsg("i="+i+", t3="+t3+" ecoState="+ecoState.toString(), FieldBOT.MSG_DBG_ALL);
        if (ecoState.time>TEST_TIME*1.4) {
            owner.sendTextMsg("Result not apply - time too long 140%. ecoState.time="+ecoState.time, FieldBOT.MSG_DBG_ALL);
        } else {
            // compare best variant
            float resCoast=owner.avgEco.getResourceCoast(ecoState.resources);
            resCoast /= ecoState.time; // TODO check ecoState.time>0, T3>0
            if ( resCoast > exp_K && ecoState.time<=MAX_TECHUP_BUILD_TIME ) { // TODO сделать нечёткое условие ecoState.time<=MAX_TECHUP_BUILD_TIME
                exp_K = resCoast;
                exp_tupTriggerR=tmp_exp_tupTriggerR;
                exp_endRes=ecoState.resources;
                exp_tupTimeStart=t1;
                exp_levelBuildTime=t2;
                exp_totalTime=ecoState.time; // t3 or ecoState.time ?
                exp_no_TechUp=false;
            }
        }
    }
    if (exp_K==0.0f || exp_tupTriggerR==null || exp_no_TechUp) return null; // TODO too many check
    
    float[] triggerRes=new float[exp_tupTriggerR[AdvECO.R_Current].length];
    for (int r=0; r<triggerRes.length; r++) {
        triggerRes[r] = exp_tupTriggerR[AdvECO.R_Current][r] 
                      +
                ( exp_tupTriggerR[AdvECO.R_Income][r]-exp_tupTriggerR[AdvECO.R_Usage][r] ) * exp_levelBuildTime;
        // TODO use variable useOptimisticResourceCalc
    }
    TechUpTrigger trigger=new TechUpTrigger(owner, toLevel,
            exp_tupTimeStart, exp_levelBuildTime,
            onBase, triggerRes);
    
    return trigger;
}

}
