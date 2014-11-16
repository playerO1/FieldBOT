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

    /**
     * Enable economic control
     */
    public boolean makeEco;
    
    private static final boolean USE_STATIONAR_BUILDER=false; //TODO do not using as NANO builders T3 factory! // Do build nano tower for assist?
    private static final float MINIMAL_BUILD_TIME=2.6f; // !!! nothing faster 2 second can not build.
    private static final float MAX_TECHUP_BUILD_TIME=6*60; // время, дольше которого нельзя делать переход на новый уровень
    private static final float MAX_BUILD_TIME_FOR_GENERATORRES=4*60; // не выбирать здания, которые строятся дольше.
    private static final float USE_STORAGE_PERCENT=0.7f; // на сколько полагаться на хранящиеся ресурсы.

    
    
    public TEcoStrategy(FieldBOT owner) {
        this.owner=owner;
        useOptimisticResourceCalc=true;
        makeEco=true;
    }
    
    
    // =============== Utils =================
    
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
     * @param currentRes current resource
     * @return best resource generator. Maybe null.
     */
    protected UnitDef getBestGeneratorRes(Resource res,Collection<UnitDef> buildVariants,float buildPower, float[][] currentRes) {
//     if (res!=null) owner.sendTextMsg(" (for resource "+res.getName()+")" , FieldBOT.MSG_DBG_ALL);
//     else owner.sendTextMsg(" (for build power)" , FieldBOT.MSG_DBG_ALL);
        
        UnitDef bestDef=null;
        float maxK=0.0f;
        float minT,maxT,currentT; // минимальное, максимальное время строительства объектов. Если разница >2, то информация не актуальна (нелинейная зависимость)
        minT=maxT=currentT=-1.0f; // Todo using magic number Float.NaN
        for (UnitDef ud:buildVariants) { // Select max better from list
            float r=0.0f;
            if (res!=null) r=owner.getUnitResoureProduct(ud,res);
            else { 
                if (ModSpecification.isRealyAbleToAssist(ud)) { // select worker
                    r=ud.getBuildSpeed();
                    if (!USE_STATIONAR_BUILDER && !ModSpecification.isRealyAbleToMove(ud)) r=r/1000.0f; // TODO что лучше: неподвижный или подвижный строитель?
                }
            }
            if (r>0.0f)
            {
                float t=getRashotBuildTime(ud, currentRes, buildPower, USE_STORAGE_PERCENT);
                float k=r/t; // make resource per build time
                if (t>MAX_BUILD_TIME_FOR_GENERATORRES) k=k/(10+t); // do not select long time building.
                // square coast. TODO more better (at the beginning - 0%, at the end - 70%)
                if (!ModSpecification.isRealyAbleToMove(ud)) k = 0.8f*k + 0.2f*k/(1+ud.getXSize()*ud.getZSize());

                // better Metal Makers factor
                float makerK=1.0f;
                if (owner.modSpecific.exist_MetalMaker_lua)
                 if (owner.modSpecific.isMetalMaker(ud)) // TODO test it
                {
                    makerK=owner.modSpecific.getConversionKMetalMaker(ud);
                }
                // better Metal Makers in Evo RTS (getting convertion factor from other function)
                if (owner.modSpecific.exist_MetalMaker_native)
                 if (owner.modSpecific.isNativeMetalMaker(ud)) // TODO test it
                {
                    makerK=owner.modSpecific.getNativeConversionKMetalMaker(ud);
                }
                if (makerK!=1.0f) k = (0.2f*k)+(0.8f * k * makerK );
                
    //     owner.sendTextMsg(" >item "+ud.getName()+" k="+k+" =r/t, r="+r+" t="+t , FieldBOT.MSG_DBG_ALL);

                if (k>0.0f) { // if this can product some need resource
                    if (minT>t || minT==-1.0f) minT=t; // для статистики
                    if (maxT<t || maxT==-1.0f) maxT=t;
                    // если разница во времени большая, то зависимость НЕ ЛИНЕЙНАЯ!
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

                // --- from linear code fragment
                float itrInfo[]=getDynamicRashotBuildInfo(ud, currentRes, buildPower, maxItr, currentT);
                float t=itrInfo[0];
                float n=itrInfo[1];
                r = r * n; // r * count

                float k=r/t; // отношение выработки ресурса к скорости постройки
//                owner.sendTextMsg(" >item (itr): "+ud.getName()+" k="+k+"=r*n/t t="+t+" n="+n+" r="+r , FieldBOT.MSG_DBG_ALL);

                if (k>0.0f) { // только для тех, кто производит хоть как-то заданный ресурс.

                if (t>MAX_BUILD_TIME_FOR_GENERATORRES*n) k=k/(10+t/n); // do not select long time building.
                // square coast
                if (!ModSpecification.isRealyAbleToMove(ud)) k = 0.8f*k + 0.2f*k/(1+ud.getXSize()*ud.getZSize());

                // better Metal Makers factor
                float makerK=1.0f;
                if (owner.modSpecific.exist_MetalMaker_lua)
                 if (owner.modSpecific.isMetalMaker(ud))
                {
                    makerK=owner.modSpecific.getConversionKMetalMaker(ud);
                }
                // better Metal Makers in Evo RTS
                if (owner.modSpecific.exist_MetalMaker_native)
                 if (owner.modSpecific.isNativeMetalMaker(ud))
                {
                    makerK=owner.modSpecific.getNativeConversionKMetalMaker(ud);
                }
                if (makerK!=1.0f) k = (0.2f*k)+(0.8f * k * makerK );
                // --- end from linear code fragment
                
    //     owner.sendTextMsg(" >item "+ud.getName()+" k="+k+" =r/t, r="+r+" t="+t , FieldBOT.MSG_DBG_ALL);

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
            
            // - - - compare with last select - - -
            if (lst_maxK>maxK) {
//                owner.sendTextMsg(" Skeep dynamic select (lstK="+lst_maxK+" dynamicK="+maxK+"). Back to "+lst_bestDef.getName(), FieldBOT.MSG_DBG_SHORT);
                bestDef=lst_bestDef;
                maxK=lst_maxK;
            }
            // - - -

        }
        return bestDef;
    }

    
    //TODO use it!
    /**
     * Return best (fast build) storage of resource
     * @param res for this resource type, TODO null - all res
     * @param buildVariants list for select
     * @param buildPower current build power
     * @param currentRes current resource
     * @return best resource storage. Maybe null.
     */
    protected UnitDef getBestStorageRes(Resource res,Collection<UnitDef> buildVariants,float buildPower, float[][] currentRes) {
        UnitDef bestDef=null;
        float maxK=0.0f;
        float minT,maxT,currentT; // минимальное, максимальное время строительства объектов. Если разница >2, то информация не актуальна (нелинейная зависимость)
        minT=maxT=currentT=-1.0f; // Todo using magic number Float.NaN
        for (UnitDef ud:buildVariants) { // Select max better from list
            float r = ud.getStorage(res);
            if (r>0.0f)
            {
                float t=getRashotBuildTime(ud, currentRes, buildPower, USE_STORAGE_PERCENT);
                float k=r/t; // make profit per build time
                if (t>MAX_BUILD_TIME_FOR_GENERATORRES) k=k/(10+t); // do not select long time building.
                // square coast. TODO more better (at the beginning - 0%, at the end - 70%)
                if (!ModSpecification.isRealyAbleToMove(ud)) k = 0.8f*k + 0.2f*k/(1+ud.getXSize()*ud.getZSize());

                if (k>0.0f) { // if this can product some need resource
                    if (minT>t || minT==-1.0f) minT=t; // для статистики
                    if (maxT<t || maxT==-1.0f) maxT=t;
                    // если разница во времени большая, то зависимость НЕ ЛИНЕЙНАЯ!
                    // TODO время вычислять не учитывая накопленные запасы, или их часть.

                    if (k>=maxK) {
                        bestDef=ud;
                        maxK=k;
                        currentT=t;
                    }
                }
            }
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
        return buildPower; // recomended build power for build
    }
    
    /**
     * Check time for build list of units
     * @param builds unit to build
     * @param startRes current resource state
     * @param buildSpeed current build power
     * @return time to build all unit in list
     */
    protected float getRashotBuildTimeLst (ArrayList<UnitDef> builds,float[][] startRes,float buildSpeed) {
        float T=0.0f;
        float[][] currentRes = AdvECO.cloneFloatMatrix(startRes);

        float buildPower=buildSpeed; // build power
        float lastIsNoAssistableBP=-1; // if >=0 - ise this, <0 use buildPower.
        UnitDef lastNoAssustableBuilder=null;// lastIsNoAssistableBP>0 - think that only last unit can build new unit
        float tmpBP;
                
        for (UnitDef def:builds) {
            if (lastIsNoAssistableBP<0 && 
                !(lastNoAssustableBuilder!=null && lastNoAssustableBuilder.getBuildOptions().contains(def)) // last unit is factory for build this
               )     tmpBP=buildPower;
                else tmpBP=lastIsNoAssistableBP;
            float t=getRashotBuildTime(def, startRes, tmpBP, 1.0f);
            T+=t;
            float resProduct[]=owner.getUnitResoureProduct(def);
            for (int i=0;i<resProduct.length;i++) {
                currentRes[AdvECO.R_Current][i]+= t * (currentRes[AdvECO.R_Income][i]-currentRes[AdvECO.R_Usage][i])
                                               - def.getCost(owner.avgEco.resName[i]);
                float deltaRes=resProduct[i];
                if (deltaRes>0.0f)
                    currentRes[AdvECO.R_Income][i] += deltaRes;
                else if ((deltaRes<0.0f))
                    currentRes[AdvECO.R_Usage][i] += deltaRes;

                currentRes[AdvECO.R_Storage][i]+= def.getStorage(owner.avgEco.resName[i]); // TODO где используется storage при расчётах?
            }
            if (def.isAbleToAssist() || def.isBuilder()) { // for builder and worker
                if (def.isAssistable()) {
                    buildPower += def.getBuildSpeed();
                    lastIsNoAssistableBP=-1;
                    //lastNoAssustableBuilder=null;
                } else {
                    lastIsNoAssistableBP=def.getBuildSpeed(); //???
                    lastNoAssustableBuilder=def;
                }
            }
        }
        return T;
    }
    
    /**
     * How many time need wait for store resource level (don't build, only wait)
     * @param currentRes current economic state
     * @param toRes target resource have
     * @return wait time
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
     * Math build time
     * @param build build time for
     * @param currentRes current resources
     * @param buildSpeed build power of worker
     * @param resCurrentMultipler 1.0 - use current res and incoming, 0.0 - only incoming without current.
     * @return time for build (second)
     */
    protected float getRashotBuildTime(UnitDef build,float[][] currentRes,float buildSpeed, float resCurrentMultipler) {
        final Resource resName[]=owner.avgEco.resName; // !!! test
        float T=MINIMAL_BUILD_TIME;// TODO test with MINIMAL_BUILD_TIME;
        // TODO сделать boolean mojetVliatNaPosleduyshie=false;... если критич. ресурсы не меняются, то нет и зависимость линейна.
        // TODO use getWaitTimeForProductResource. часть кода отсюда уже скопированно в getWaitTimeForProductResource()
        for (int rN=0; rN<currentRes[resName.length].length;rN++) {
            float nehvatkaRes=build.getCost(resName[rN])-resCurrentMultipler*currentRes[AdvECO.R_Current][rN];
            if (nehvatkaRes>0.0f) {
                //float resPostupl=currentRes[AdvECO.R_Income][rN]-currentRes[AdvECO.R_Usage][rN]; // было
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
        
        if (buildSpeed>0.0f && buildSpeed!=Float.POSITIVE_INFINITY) T=Math.max(T, build.getBuildTime()/buildSpeed);
        T += 1;  // "fuzzy" limit: nothink faster that 2 second can not build.
        return T;
    }
    
    // You may see 'bug' on getDynamicRashotBuildInfo on TA mod, CORE - do not using adv solar on T1 level.
    // It is no bug - just small solar need < build power that adv BP.
    
    /**
     * Dynamic modeling of one economic building when time may have recursion depends of build.
     * @param build what build
     * @param startRes start economic info
     * @param startBuildSpeed start build power
     * @param maxItr iteration limit
     * @param maxTime time limit
     * @return array contain 2 float: [0] - total time [1] num of iter (building count)
     */
    private float[] getDynamicRashotBuildInfo(UnitDef build,float[][] startRes,float startBuildSpeed,int maxItr,float maxTime) {
        //TODO !доделать!: Если дальше последовательность будет тормозить себя, то возвращает только ближайщую, не считает ухудшение. !!!
        int bestI=0;
        float bestT=0;
        float bestK=0;
        
        // cashe for unit economic change, for more faster work
        final float[][] unitEffectEco=new float[startRes.length][startRes[0].length];
        final int numRes=owner.avgEco.resName.length;
        float resProduct[]=owner.getUnitResoureProduct(build);
        for (int i=0;i<numRes;i++) {
            Resource res=owner.avgEco.resName[i];
            unitEffectEco[AdvECO.R_Current][i] = -build.getCost(res); // decrease current res
            float deltaRes=resProduct[i];
            unitEffectEco[AdvECO.R_Income][i]=Math.max(0,deltaRes);
            unitEffectEco[AdvECO.R_Usage][i]=Math.max(0,-deltaRes); // increase usage
            unitEffectEco[AdvECO.R_Storage][i]=build.getStorage(res);
        }
        final float deltaBPower; // cashe for Puild Power effect
        if (build.isAbleToAssist()) deltaBPower = build.getBuildSpeed();
                               else deltaBPower = 0.0f;
        
        float buildPower=startBuildSpeed;
        float[][] curRes=AdvECO.cloneFloatMatrix(startRes);
        float timeLost=0;
        int itrN;
        for (itrN=1; itrN<=maxItr && (timeLost<maxTime || itrN==1); itrN++)
        {
            float t=getRashotBuildTime(build, curRes, buildPower, 1.0f);
            timeLost+=t;
            if (timeLost>maxTime && itrN>1) {
                timeLost-=t;
                break; // !!
            }
            buildPower += deltaBPower; // check worker build power
            //owner.sendTextMsg("dbg: build:"+build.getName()+" t<MaxT: t="+t+" maxTime="+maxTime, FieldBOT.MSG_DBG_ALL);
            for (int r=0;r<numRes;r++) {
                float rNew = t * (curRes[AdvECO.R_Income][r]-curRes[AdvECO.R_Usage][r]);
                curRes[AdvECO.R_Current][r] += rNew;
            }
            for (int i=0;i<curRes.length;i++) // R_Current, R_Income, R_Usage, R_Storage.
                for (int r=0;r<numRes;r++) 
                    curRes[i][r]+=unitEffectEco[i][r];


            float k=(float)itrN/timeLost;
            if (k>=bestK || itrN==1) {
                bestI=itrN;
                bestT=timeLost;
                bestK=k;
            }
        }
        int allI=itrN;
        float allT=timeLost;
        float allK=(float)allI/timeLost;
        
        // ------------
        float itrInfo[] = {bestI, bestT};//old: {t,nItrPass};
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
        Resource bestUnitR=null; // select best unit for generate this resource
        //Economy eco = owner.clb.getEconomy();
        final Resource resM=owner.clb.getResourceByName("Metal");
        final Resource resE=owner.clb.getResourceByName("Energy");
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
        
        // TODO ...
        // mmaker = 70%-80%
        float avg_cur_M=currentRes[AdvECO.R_Current][0];//owner.avgEco.getAvgCurrent(resM);
        float avg_stor_M=currentRes[AdvECO.R_Storage][0];//owner.avgEco.getAvgStorage(resM);
        float avg_delta_M=currentRes[AdvECO.R_Income][0]-currentRes[AdvECO.R_Usage][0];//owner.avgEco.getAvgIncome(resM)-owner.avgEco.getAvgUsage(resM);
        float avg_cur_E=currentRes[AdvECO.R_Current][1];//owner.avgEco.getAvgCurrent(resE);
        float avg_stor_E=currentRes[AdvECO.R_Storage][1];//owner.avgEco.getAvgStorage(resE);
        float avg_delta_E=currentRes[AdvECO.R_Income][1]-currentRes[AdvECO.R_Usage][1];//owner.avgEco.getAvgIncome(resE)-owner.avgEco.getAvgUsage(resE);
        
        // Economic - select need resources to do resource generators
        final float EconvertK=Math.min(owner.modSpecific.metal_maker_workZone*1.19f, 0.97f);
        
        // See on future, do not see on current time (as on stock exchange).
        final float T_FUTURE=0.035f; // see on ? second to future
        if (avg_delta_E>0) { //  && avg_cur_E/avg_stor_E<EconvertK // FIXME avg_delta_E and T_FUTURE!!!!!!!!!!!! Maybe just do not using this?
            float pEstor1=avg_cur_E/avg_stor_E; // Energy storage percent
            float pEstor2=(avg_cur_E+avg_delta_E *T_FUTURE)/avg_stor_E; // Energy storage percent
            if (pEstor2-pEstor1>0.3 || pEstor2<EconvertK)
            {
                avg_cur_E += avg_delta_E *T_FUTURE;
                if (avg_cur_E>avg_stor_E) avg_cur_E=avg_stor_E;
            } // TODO else ??? need know max conversion power.
        }
        if (avg_delta_M>0) {
            avg_cur_M += avg_delta_M *T_FUTURE;
            if (avg_cur_M>avg_stor_M) avg_cur_M = avg_stor_M;
        }
        
        float pEstor=avg_cur_E/avg_stor_E; // Energy storage percent
        float pMstor=avg_cur_M/avg_stor_M; // Metal storage percent
        
        // FIXME Zero-K not have metal makers - first build extractors, then make energy spam only. Check free extractor points.
        if (owner.isMetalFieldMap==FieldBOT.IS_NO_METAL || owner.isMetalFieldMap==FieldBOT.IS_NORMAL_METAL)
        {
            if ( pEstor <= EconvertK || (avg_delta_E<1 && pEstor <= 0.90)) {
//                owner.sendTextMsg(" cel->electrostanciya1" , FieldBOT.MSG_DBG_SHORT);
                bestUnitR = resE;
            }
            if ( pMstor < 0.5  &&  pEstor >= Math.min(EconvertK*1.07,0.97) && avg_delta_E>1) {
//                owner.sendTextMsg(" cel->metal2" , FieldBOT.MSG_DBG_SHORT);
                bestUnitR = resM;
            }
            if (bestUnitR==null) {
//                owner.sendTextMsg(" cel->metal3" , FieldBOT.MSG_DBG_SHORT);
                bestUnitR = resM;
            }
        }
        else if (owner.isMetalFieldMap==FieldBOT.IS_METAL_FIELD)
        {
            if ( pEstor <= 0.50 || (avg_delta_E*2<avg_delta_M && pMstor>0.20)) {
//                owner.sendTextMsg(" cel->electrostanciya1-" , FieldBOT.MSG_DBG_SHORT);
                bestUnitR = resE;
            } // FIXME this is bad logic, if pE<0.1 or pM<0 then can be have problem
            if ( (pMstor < 0.50 && pEstor>pMstor) || (avg_delta_M<0 && avg_delta_E>1 && pEstor>0.30)) {
//                owner.sendTextMsg(" cel->metal2-" , FieldBOT.MSG_DBG_SHORT);
                bestUnitR = resM;
            }
            if (bestUnitR==null) {
//                owner.sendTextMsg(" cel->electrostanciya3-" , FieldBOT.MSG_DBG_SHORT);
                bestUnitR = resE;
            }
        } else owner.sendTextMsg(" uncknown map resource type = "+owner.isMetalFieldMap , FieldBOT.MSG_DBG_SHORT); // or FieldBOT.MSG_ERR?
        if ( pMstor>0.9 && pEstor>EconvertK && avg_delta_M>0 || bestUnitR==null) {
//            owner.sendTextMsg(" cel->4" , FieldBOT.MSG_DBG_SHORT);
            final float resourceProportion[]=owner.modSpecific.resourceProportion; // last:{1.0f, 10.0f};
            if (currentRes[AdvECO.R_Income][0]/resourceProportion[0]>currentRes[AdvECO.R_Income][1]/resourceProportion[1])
                 bestUnitR = resE;
            else bestUnitR = resM; // FIXME on metal map do build too much metal extractors on ARM!
        }

        //TODO if >90% storage have, and delta Res >20% of storage, then make storage - getBestStorageRes()
        
//        owner.sendTextMsg(" dbg, avg_cur_M="+avg_cur_M+", avg_stor_M="+avg_stor_M+", avg_delta_M="+avg_delta_M+"; avg_cur_E="+avg_cur_E+", avg_stor_E="+avg_stor_E+", avg_delta_E="+avg_delta_E+" ; EconvertK="+EconvertK, FieldBOT.MSG_DBG_ALL);
//        owner.sendTextMsg(" do call getBestGeneratorRes for resource:"+bestUnitR.getName(), FieldBOT.MSG_DBG_ALL);
        UnitDef bestUnit = getBestGeneratorRes(bestUnitR, buildVariants,buildPower, currentRes);

        // -------- ------- --------
        // build storage, if deltaRes+- > 20% and storage using >80%
        if (   pEstor>EconvertK && pMstor>0.8
            && avg_delta_M>1 && avg_delta_E>1) {
            if (avg_delta_E/avg_stor_E>0.35) bestUnit=getBestStorageRes(resE, buildVariants, buildPower, currentRes);
            if (avg_delta_M/avg_stor_M>0.35) bestUnit=getBestStorageRes(resM, buildVariants, buildPower, currentRes);
        }

        // workers:
        if (bestUnit!=null && avg_cur_E/avg_stor_E > 0.60 && avg_cur_M/avg_stor_M >0.50 && avg_delta_M>1 && (avg_delta_E>2 || avg_cur_E/avg_stor_E >= EconvertK)) {

            // TODO maybe using needBuildWorkerBefore()?
            
            float optimalBPower=getOptimalBuildPowerFor(bestUnit, currentRes, 0.50f);

            UnitDef bestUnit_B = getBestGeneratorRes(bestUnitR, buildVariants, Float.POSITIVE_INFINITY, currentRes); // !!!!!!!
            if (bestUnit_B!=null && bestUnit!=bestUnit_B) {
                float optimalBPower_B = getOptimalBuildPowerFor(bestUnit_B, currentRes, 0.25f);
                optimalBPower = Math.max(optimalBPower,optimalBPower_B);
            } else bestUnit_B=null;
            
//            owner.sendTextMsg(" Check worker: buildPower="+buildPower+" optimalBPower="+optimalBPower , FieldBOT.MSG_DBG_SHORT); // DEBUG DBG_ALL
            // TODO workers
            if (buildPower*0.98f < optimalBPower) { // if need have more build power
                UnitDef worker=getBestGeneratorRes(null,buildVariants,buildPower, currentRes);
                if (worker!=null)
                // TODO test this line: 
                    if (worker.getBuildSpeed() < (optimalBPower-buildPower)*2.2f)
                {
                    float t1;
                    if (bestUnit_B==null) t1=getRashotBuildTime(bestUnit, currentRes, buildPower, 0.4f); // time for build unit/building
                                         else t1=getRashotBuildTime(bestUnit_B, currentRes, buildPower, 0.4f); // time for build unit/building
                    float t2=getRashotBuildTime(worker, currentRes, buildPower, 0.4f); // time for build worker
                    if (t2*1.01<t1) // if make worker is faster that make building
                    {
                        ArrayList<UnitDef> bLst=new ArrayList<UnitDef>(2);
                        bLst.add(0, worker);
                        if (bestUnit_B==null) bLst.add(1, bestUnit);
                                         else bLst.add(1, bestUnit_B);
                        t2=getRashotBuildTimeLst(bLst, currentRes, buildPower);
                        if (t2<=t1) bestUnit=worker;
//                    owner.sendTextMsg(" Nujni rabochie current buildPower="+buildPower+" optimalBPower="+optimalBPower , FieldBOT.MSG_DBG_SHORT);
                    }
                }
            }
        }
        // -------- ------- --------
        
//        if (bestUnit!=null) owner.sendTextMsg(" vibor: "+bestUnit.getName()+" - " + bestUnit.getHumanName() , FieldBOT.MSG_DBG_ALL);
//        else owner.sendTextMsg(" NO SELECT!" , FieldBOT.MSG_DBG_SHORT);

        return bestUnit;
    }
  
    /**
     * Do need worker for assist build this unit, or no?
     * @param forBuildUnit unit for decrease build time
     * @param currentBuildPower
     * @param buildVariants list with workers
     * @param currentRes resource, can be null
     * @return null - not need new builder, or better buildef for increase build power
     */
    public UnitDef needBuildWorkerBefore(UnitDef forBuildUnit, float currentBuildPower, List<UnitDef> buildVariants, float[][] currentRes) {
        final float EconvertK=Math.min(owner.modSpecific.metal_maker_workZone*1.19f, 0.97f);
        //float[][] currentRes;// can be on parameter, can be nill
        if (currentRes==null) {
            if (useOptimisticResourceCalc) currentRes= owner.avgEco.getSmartAVGResourceToArr();
            else currentRes= owner.avgEco.getAVGResourceToArr();
        }
        
        // TODO test, use it, more smart...
        // mmaker = 70%-80%
        float avg_cur_M=currentRes[AdvECO.R_Current][0];
        float avg_stor_M=currentRes[AdvECO.R_Storage][0];
        float avg_delta_M=currentRes[AdvECO.R_Income][0]-currentRes[AdvECO.R_Usage][0];
        float avg_cur_E=currentRes[AdvECO.R_Current][1];
        float avg_stor_E=currentRes[AdvECO.R_Storage][1];
        float avg_delta_E=currentRes[AdvECO.R_Income][1]-currentRes[AdvECO.R_Usage][1];

        if (avg_cur_E/avg_stor_E > 0.20 && avg_cur_M/avg_stor_M >0.20 || (avg_delta_M>3 && (avg_delta_E>3 || avg_cur_E/avg_stor_E >= EconvertK))) {

            float optimalBPower=getOptimalBuildPowerFor(forBuildUnit, currentRes, 0.45f);
            if (currentBuildPower*0.98f < optimalBPower) { // if not enought workers
                UnitDef worker=getBestGeneratorRes(null,buildVariants,currentBuildPower, currentRes);
                if (worker!=null)
                // if (worker.getBuildSpeed() < (optimalBPower-currentBuildPower)*1.01f) TODO it.
                {
                    float t1=getRashotBuildTime(forBuildUnit, currentRes, currentBuildPower, 0.4f); // time for build unit/building
                    float t2=getRashotBuildTime(worker, currentRes, currentBuildPower, 0.4f); // time for build worker
                    if (t2*1.12f<t1) // if build worker faster that build building
//                    owner.sendTextMsg("needBuildWorkerBefore 1: t1="+t1+" t2="+t2 , FieldBOT.MSG_DBG_SHORT);// DEBUG!
                    {
                        ArrayList<UnitDef> bLst=new ArrayList<UnitDef>(2);
                        bLst.add(0, worker);
                        bLst.add(1, forBuildUnit);
                        t2=getRashotBuildTimeLst(bLst, currentRes, currentBuildPower);
//                        owner.sendTextMsg("needBuildWorkerBefore 2: t1="+t1+" t2="+t2 , FieldBOT.MSG_DBG_SHORT);// DEBUG!
                        if (t2<=t1) return worker; // If realy faster build worker before new unit
                    }
                }// else owner.sendTextMsg("needBuildWorkerBefore 1: worker is null!" , FieldBOT.MSG_DBG_SHORT);// DEBUG!
            }// else owner.sendTextMsg("needBuildWorkerBefore 0: build bower is enought. optimalBP="+optimalBPower+" currentBP="+currentBuildPower , FieldBOT.MSG_DBG_SHORT);// DEBUG!
        }//  else owner.sendTextMsg("needBuildWorkerBefore 0: no have resource." , FieldBOT.MSG_DBG_SHORT);// DEBUG!
        return null; // not need worker
    }
    
    
// ---- Tech Lewel chooise ----
    
    /**
     * Select better Tech Level that have now
     * @param TechLvls list for choose
     * @param currEcoLvl for compare level, best that this eco product
     * @param currentMaxBuilder max build power of 1 better builder
     * @return list with Tech Levels when same items of currEcoLvl[] > that currentEco[]
     */
    public ArrayList<TTechLevel> selectLvl_whoBetterThat(ArrayList<TTechLevel> TechLvls, float[] currEcoLvl, float currentMaxBuilder)
    {
        ArrayList<TTechLevel> bestLvls=new ArrayList<TTechLevel>();
//        float maxNFactor=-1; // choose level with more diffetent factor (metal,energy, build power).
        // Выбор уровня с лучшей экономикой
        for (TTechLevel level:TechLvls) {
//            float nFactor=0;
            
            float kk[]=new float[currEcoLvl.length];
            for (int i=0;i<currEcoLvl.length;i++) {
                float delta=level.ecoK[i]-currEcoLvl[i];
                if (delta>0) {
                    kk[i]=delta;
//                    nFactor++;
                } else kk[i]=0;
            }
            float k=owner.avgEco.getResourceCoast(kk);

            float workK=level.builderK-currentMaxBuilder;
            if (workK>0) {
                k+=owner.avgEco.getBuildPowerCoast(workK)/100; //TODO test it! just multipler.
//                nFactor++;
            }
                    
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
    float maxNFactor=-1; // choose level with more diffetent factor (metal,energy, build power).
    for (TTechLevel level:TechLvls)
    {
        float k, nFactor=0;
        float kk[]=new float[bestEco.length];
        for (int i=0;i<bestEco.length;i++) {
            float delta=level.ecoK[i]-bestEco[i];
            if (delta>0) {
                kk[i]=delta;
                nFactor++;
            } else kk[i]=0; //last: k+=delta;!!!
        }
        k=owner.avgEco.getResourceCoast(kk);

        float workK=level.builderK-currentMaxBuilder;
        if (workK>0) {
            k+=owner.avgEco.getBuildPowerCoast(workK)/100; //TODO test it! just multipler.
            nFactor++;
        }
        k = k * (1+nFactor) ; // For more different resources

        float tBuild=level.getTimeForBuildTechLevel(onBase.getBuildPower(false, true), currentRes, owner);
        if (tBuild<10.0f) tBuild=10.0f;// Нельзя построить так быстро...
        k= k / tBuild; // Выгодность / время постройки.
        // TODO учесть + время строительства электростанций

        //TODO check tBuild with MAX_TECHUP_BUILD_TIME
        if (k>0.0f)
        {
            if (nFactor>maxNFactor) { // First by NFactor
                owner.sendTextMsg(" >>best level by NFactor n="+nFactor+" t="+tBuild+" (AssistWorkTime="+level.getAssistWorkTime()+" getBuildPower="+onBase.getBuildPower(false, true)+" NOAssistWorkTime="+level.getNOAssistWorkTime()+" level:"+level.toString(), FieldBOT.MSG_DBG_ALL); //DEBUG !!!
                bestEcoK=k;
                bestLvl=level;
                maxNFactor=nFactor;
            } else if (nFactor>=maxNFactor)
            { // Second by K
                if (k==bestEcoK) owner.sendTextMsg(" !!! Tech level have equals bestK="+bestEcoK+" K="+k+" level="+level.toString(),FieldBOT.MSG_DBG_SHORT);

                if (k>bestEcoK) { // Лучше. А можно это построить сейчас?
                    owner.sendTextMsg(" >>best level k="+k+" t="+tBuild+" (AssistWorkTime="+level.getAssistWorkTime()+" getBuildPower="+onBase.getBuildPower(false, true)+" NOAssistWorkTime="+level.getNOAssistWorkTime()+" level:"+level.toString(), FieldBOT.MSG_DBG_ALL); //DEBUG !!!
                    bestEcoK=k;
                    bestLvl=level;
                }
            }
        }
    }
    return bestLvl;
}


/**
 * Fast check: do need execute tech up now?
 * Not optimized tech up time, only max MAX_TECHUP_BUILD_TIME check.
 * @param onBase only for getBuildPower TODO replace on buildPower!
 * @param CurrentLevelUnitsCanBuild what current level can build
 * @param toLevel new level
 * @return true - need tech up now, false - do not tech up to this level now.
 */
public boolean isActualDoTechUp_fastCheck(TBase onBase, HashSet<UnitDef> CurrentLevelUnitsCanBuild, TTechLevel toLevel) {
    HashSet<UnitDef> futureBuildLst=new HashSet<UnitDef>(CurrentLevelUnitsCanBuild);
    futureBuildLst.addAll(toLevel.needBaseBuilders);
    futureBuildLst.addAll(toLevel.levelNewDef);
    
    // Check for all resource - what better check
    float buildPower=onBase.getBuildPower(false,true); //TODO replace on buildPower in argument!
    int toNewLvl=0; // last: , toLastLvl=0;

    // add on resource "null" for check workers too
    Resource[] checksRes=new Resource[owner.avgEco.resName.length+1];
    System.arraycopy(owner.avgEco.resName, 0, checksRes, 0, owner.avgEco.resName.length); // last: for (int r=0;r<owner.avgEco.resName.length;r++) checksRes[r]=owner.avgEco.resName[r];
    checksRes[checksRes.length-1]=null;// workers
    
    float currentRes[][];
    if (useOptimisticResourceCalc) currentRes= owner.avgEco.getSmartAVGResourceToArr();
    else currentRes= owner.avgEco.getAVGResourceToArr();
    
    for (Resource res:checksRes) {
        UnitDef ud = //ecoStrategy.getOptimalBuildingForNow(onBase, futureBuildLst);
                getBestGeneratorRes(res,futureBuildLst, buildPower, currentRes);
        if (ud!=null) {
            if (toLevel.levelNewDef.contains(ud)) toNewLvl++;
            //last: if (CurrentLevelUnitsCanBuild.contains(ud)) toLastLvl++;
        }
    }
    //TODO учесть за период + расходы на строительство!!!
    return toNewLvl>0 && toLevel.getTimeForBuildTechLevel(buildPower, currentRes, owner)<MAX_TECHUP_BUILD_TIME;
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
        if (!isEcoUnit) for (Resource r:owner.clb.getResources())
        {
            if ( owner.getUnitResoureProduct(def, r) > 0 ) { // TODO do replace to float[]owner.getUnitResoureProduct(def) is actualy for speed?
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
    
    final int MAX_ITERATION=500; // !!!
    int nItr=0;
    float T=0;
    UnitDef build=null;
    do {
        nItr++;
        build=getOptimalBuildingForNow(newEcoState.assistBuildPower, ecoBuildWariant, currentRes);
            // FIXME check for null!
        float t=getRashotBuildTime(build, newEcoState.resources, newEcoState.assistBuildPower, 1.0f);
//    owner.sendTextMsg("  nItr="+nItr+" build="+build.getName()+" t="+t, FieldBOT.MSG_DBG_ALL);//!!!
        if (T+t<maxT) {
            T+=t; // учёт времени.
            if (build.isAbleToAssist()) newEcoState.assistBuildPower += build.getBuildSpeed(); // учесть строительство
            else newEcoState.noAssistBuildPower += build.getBuildSpeed(); // !!!
            
            float resProduct[]=owner.getUnitResoureProduct(build);
            for (int i=0;i<resProduct.length;i++) {
                currentRes[AdvECO.R_Current][i]=currentRes[AdvECO.R_Current][i]
                        + t * (currentRes[AdvECO.R_Income][i]-currentRes[AdvECO.R_Usage][i]) // !!!!!!!!! Math.max(startRes[AdvECO.R_Income][r]-startRes[AdvECO.R_Usage][r],startRes[AdvECO.R_Income][r]/2.0f)
                        - build.getCost(owner.avgEco.resName[i]);

                // Учесть потери от нехватки складов
                if (currentRes[AdvECO.R_Current][i]>currentRes[AdvECO.R_Storage][i])
                    currentRes[AdvECO.R_Current][i]=currentRes[AdvECO.R_Storage][i];
                // И невозможности отрицательных ресурсов
                if (currentRes[AdvECO.R_Current][i]<0) currentRes[AdvECO.R_Current][i]=0;// !!!! TODO если не пропали, то что-то уменьшилось.
                
                float deltaRes=resProduct[i];
                if (deltaRes>0.0f)
                    currentRes[AdvECO.R_Income][i] += deltaRes;
                else if ((deltaRes<0.0f))
                    currentRes[AdvECO.R_Usage][i] += deltaRes;

                // storage
                currentRes[AdvECO.R_Storage][i]+= + build.getStorage(owner.avgEco.resName[i]);
            }
            
        } else break;// if time not enought - stop cicle.
    } while (T<maxT && nItr<MAX_ITERATION && build!=null);
    newEcoState.time+=T;
    return newEcoState;
}


// ----------- Smart Tech Up -----------
// Next methods not support multithreading call (have chance concurrent modification, becouse using private variables).
private float[][] exp_tupTriggerR;
private float exp_endRes[][];
private float exp_tupTimeStart;
private float exp_totalTime;
private float exp_levelBuildTime;
private float exp_K;

private final float TEST_TIME=20*60; // 20 min for tech up modeling
private final int N_ITER=18; // TODO exchange FOR to "dichotomy method" "метод дихотомии"
    
private boolean techUpVirtualModeling(ArrayList<UnitDef> currentLevelDefs,ArrayList<UnitDef> futureBuildLst,
                float currentLvlPower, float startRes[][], TTechLevel toLevel ) {
    
// for level - print, owner.sendTextMsg(" <getPrecissionTechUpTimeFor>, workers F: "+currentLvlPower+" N_ITER="+N_ITER, FieldBOT.MSG_DBG_ALL);
    
    float[][] tmp_exp_tupTriggerR;// TODO!!!!!
    
    boolean TUpModifed=false;
    
    for (int i=0; i<N_ITER; i++)
    {
        // Итерация
        float t1=TEST_TIME*(0.50f * (float)i/(float)N_ITER); // 0%-50% of TEST_TIME
        EcoStateInfo ecoState=new EcoStateInfo(startRes, currentLvlPower);
        ecoState=calcEcoAftherTime(t1,ecoState, currentLevelDefs);

        // use max storage - lost resource when above storage
        for (int r=0;r<ecoState.resources[AdvECO.R_Current].length;r++)
            ecoState.resources[AdvECO.R_Current][r] = Math.min(ecoState.resources[AdvECO.R_Current][r], ecoState.resources[AdvECO.R_Storage][r]);
        
        tmp_exp_tupTriggerR=AdvECO.cloneFloatMatrix(ecoState.resources);
        
owner.sendTextMsg("i="+i+", t1="+t1+" ecoState="+ecoState.toString(), FieldBOT.MSG_DBG_ALL);
        // -- TechUP --
        float t2w=0; // time for build worker before tech up
        if (!toLevel.byMorph()) { //check - do need build worker before: at TBase this finction has been have.
            UnitDef assistWorker=needBuildWorkerBefore(toLevel.needBaseBuilders.get(0), ecoState.assistBuildPower, currentLevelDefs, ecoState.resources);
            if (assistWorker!=null) {
                t2w=getRashotBuildTime(assistWorker, ecoState.resources, ecoState.assistBuildPower, 1.0f);
                for (int r=0;r<ecoState.resources[0].length;r++) {
                    ecoState.resources[AdvECO.R_Current][r] += 
                            t2w *
                            (ecoState.resources[AdvECO.R_Income][r]-ecoState.resources[AdvECO.R_Usage][r])
                            - assistWorker.getCost(owner.avgEco.resName[r]);
                }
                ecoState.assistBuildPower+=assistWorker.getBuildSpeed();
            }
        }
        
        float t2=toLevel.getTimeForBuildTechLevel(ecoState.assistBuildPower, ecoState.resources, owner); // t2 - tech up time
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
                } else t2+=1e+20; //!!!!!!!!
                owner.sendTextMsg("> error, resource "+r+" is negative! value="+ecoState.resources[AdvECO.R_Current][r]+", time T2 fix to "+t2, FieldBOT.MSG_DBG_SHORT);
            }
        }
        for (UnitDef lvlBuilder:toLevel.needBaseBuilders) {
            if (lvlBuilder.isAbleToAssist())
                ecoState.assistBuildPower +=lvlBuilder.getBuildSpeed();
            if (lvlBuilder.isBuilder() && !lvlBuilder.isAbleToAssist())
                ecoState.noAssistBuildPower +=lvlBuilder.getBuildSpeed();
            //TODO add resource delta: product/usage.
        }
        // if by morph level
        if (toLevel.byMorph()) {
            // Warning! This math do on getTimeForBuildTechLevel() too, but not apply to eco state.
            UnitDef morphFrom=toLevel.byMorph.getDef();
            if (morphFrom.isAbleToAssist()) ecoState.assistBuildPower -= morphFrom.getBuildSpeed();
            if (morphFrom.isBuilder() && !morphFrom.isAbleToAssist()) ecoState.noAssistBuildPower -= morphFrom.getBuildSpeed();
            
            float resProduct[]=owner.getUnitResoureProduct(morphFrom);
            // resource income/usage for lost morph unit.
            for (int r=0;r<ecoState.resources[0].length;r++) {
                float deltaRes=resProduct[r];
                if (deltaRes>0.0f)
                    ecoState.resources[AdvECO.R_Income][r] -= deltaRes;
                else if ((deltaRes<0.0f))
                    ecoState.resources[AdvECO.R_Usage][r] -= deltaRes;
            }
        }
        ecoState.time+=t2+t2w;
        
        // use max storage - lost resource when above storage
        for (int r=0;r<ecoState.resources[AdvECO.R_Current].length;r++)
            ecoState.resources[AdvECO.R_Current][r] = Math.min(ecoState.resources[AdvECO.R_Current][r], ecoState.resources[AdvECO.R_Storage][r]);
        
        // ----
owner.sendTextMsg("i="+i+", t2="+t2+" ecoState="+ecoState.toString(), FieldBOT.MSG_DBG_ALL);
        
        float t3=TEST_TIME-t1; // -t2; // TODO if T3<0 ?
        
        ecoState=calcEcoAftherTime(t3,ecoState, futureBuildLst);

owner.sendTextMsg("i="+i+", t3="+t3+" ecoState="+ecoState.toString(), FieldBOT.MSG_DBG_ALL);
        if (ecoState.time>(TEST_TIME+MAX_TECHUP_BUILD_TIME)*1.2 || t2>MAX_TECHUP_BUILD_TIME) {
            owner.sendTextMsg("Result not apply - time too long. ecoState.time="+ecoState.time, FieldBOT.MSG_DBG_ALL);
        } else {
            // compare best variant
            float resCoast=owner.avgEco.getResourceCoast(ecoState.resources);
            resCoast /= ecoState.time; // TODO check ecoState.time>0, T3>0
            if ( resCoast > exp_K ) { // last: && ecoState.time <= ? // TODO make fuzzy condition
                exp_K = resCoast;
                exp_tupTriggerR=tmp_exp_tupTriggerR;
                exp_endRes=ecoState.resources;
                exp_tupTimeStart=t1;
                exp_levelBuildTime=t2;
                exp_totalTime=ecoState.time;
                //exp_no_TechUp=false;
                TUpModifed=true;
            }
        }
    }
    return TUpModifed;
}

/**
 * Precission that isActualDoTechUp_fastCheck, required CPU time!
 * @param onBase for build power, and create trigger
 * @param CurrentLevelUnitsCanBuild current level info
 * @param toLevel new level
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
    
    // prepare variables
    float currentLvlPower=onBase.getBuildPower(false,true); // !!!
    
    float startRes[][];
    if (useOptimisticResourceCalc) startRes= owner.avgEco.getSmartAVGResourceToArr();
    else startRes= owner.avgEco.getAVGResourceToArr();
    
owner.sendTextMsg(" <getPrecissionTechUpTimeFor>, workers F: "+currentLvlPower+" N_ITER="+N_ITER, FieldBOT.MSG_DBG_ALL);
    
    exp_tupTriggerR=null;
    exp_endRes=null;
    exp_tupTimeStart=0;
    exp_totalTime=0;
    exp_levelBuildTime=0;
    exp_K=0.0f;
    
    // Economic state without tech up
    EcoStateInfo noTUp_ecoState=new EcoStateInfo(startRes, currentLvlPower);
    noTUp_ecoState=calcEcoAftherTime(TEST_TIME,noTUp_ecoState, currentLevelDefs);
    exp_K=owner.avgEco.getResourceCoast(noTUp_ecoState.resources);
    exp_K /= noTUp_ecoState.time;
    //TTechLevel exp_betterLvl=null;
    
    boolean lvlIsBest;
    lvlIsBest=techUpVirtualModeling(currentLevelDefs, futureBuildLst, currentLvlPower, startRes, toLevel);
    
    if (exp_K==0.0f || exp_tupTriggerR==null || !lvlIsBest) return null; // TODO too many check
    
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

    /**
     * Absolute best choose tech level, required too many CPU resource, return realy better level. Modeling all variants on virtual economic for choose - hight CPU loading for it.
     * @param onBase for getBuildPower and create tech up trigger info
     * @param CurrentLevelUnitsCanBuild
     * @param newLevels list of new levels
     * @return tech up trigger, or null if current level is better
     */
    public TechUpTrigger getAbsolutePrecissionTechUpTimeFor(TBase onBase, HashSet<UnitDef> CurrentLevelUnitsCanBuild, List<TTechLevel> newLevels)
{
    ArrayList<UnitDef> currentLevelDefs=select_EconomicUDefs(CurrentLevelUnitsCanBuild); // remove no economic units, for faster
    
    // prepare variables
    float currentLvlPower=onBase.getBuildPower(false,true); // !!!
    
    float startRes[][];
    if (useOptimisticResourceCalc) startRes= owner.avgEco.getSmartAVGResourceToArr();
    else startRes= owner.avgEco.getAVGResourceToArr();
    
owner.sendTextMsg(" <getPrecissionTechUpTimeFor>, workers F: "+currentLvlPower+" N_ITER="+N_ITER, FieldBOT.MSG_DBG_ALL);
    
    exp_tupTriggerR=null;
    exp_endRes=null;
    exp_tupTimeStart=0;
    exp_totalTime=0;
    exp_levelBuildTime=0;
    exp_K=0.0f;
    
    // Economic state without tech up
    EcoStateInfo noTUp_ecoState=new EcoStateInfo(startRes, currentLvlPower);
    noTUp_ecoState=calcEcoAftherTime(TEST_TIME,noTUp_ecoState, currentLevelDefs);
    exp_K=owner.avgEco.getResourceCoast(noTUp_ecoState.resources);
    exp_K /= noTUp_ecoState.time;
    TTechLevel exp_betterLvl=null;
    
    for (TTechLevel toLevel:newLevels) {
        HashSet<UnitDef> hs_futureBuildLst=new HashSet<UnitDef>(toLevel.needBaseBuilders);
        hs_futureBuildLst.addAll(toLevel.levelNewDef);
        // TODO check futureBuildLst.isEmpty() - если да, то в уровне нет экономических юнитов.
        hs_futureBuildLst.addAll(currentLevelDefs);
        ArrayList<UnitDef> futureBuildLst=select_EconomicUDefs(hs_futureBuildLst); // убрать лишних юнитов для ускорения расчётов
        
        boolean lvlIsBest=techUpVirtualModeling(currentLevelDefs, futureBuildLst, currentLvlPower, startRes, toLevel);
        if (lvlIsBest) exp_betterLvl=toLevel;
    }
    
    
    if (exp_K==0.0f || exp_tupTriggerR==null || exp_betterLvl==null) return null; // TODO too many check
    
    float[] triggerRes=new float[exp_tupTriggerR[AdvECO.R_Current].length];
    for (int r=0; r<triggerRes.length; r++) {
        triggerRes[r] = exp_tupTriggerR[AdvECO.R_Current][r] 
                      +
                ( exp_tupTriggerR[AdvECO.R_Income][r]-exp_tupTriggerR[AdvECO.R_Usage][r] ) * exp_levelBuildTime;
        // TODO use variable useOptimisticResourceCalc
    }
    TechUpTrigger trigger=new TechUpTrigger(owner, exp_betterLvl,
            exp_tupTimeStart, exp_levelBuildTime,
            onBase, triggerRes);
    
    return trigger;
}

}