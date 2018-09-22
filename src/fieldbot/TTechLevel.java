/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package fieldbot;

import com.springrts.ai.oo.clb.CommandDescription;
import com.springrts.ai.oo.clb.Resource;
import com.springrts.ai.oo.clb.Unit;
import com.springrts.ai.oo.clb.UnitDef;
import fieldbot.AIUtil.UnitSelector;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

// TODO при botClb.modSpecific.specificUnitEnabled учесть строительство лаборатории, как тех. уровень.
// botClb.modSpecific.removeUnitWhenCantBuildWithTeclLevel(newUnitDefs, tmpTechLevels)...



/**
 * TechLevel define new technology units and techno way, and average profit from new tech level.
 * What need build for tech up. What coast and time for do tech up.
 * @author PlayerO1
 */
public class TTechLevel {
    private final FieldBOT botClb;// for call
    
    /**
     * list of main builders for pass this tech level
     */
    public final ArrayList<UnitDef> needBaseBuilders;
    
    /**
     * morph tech up support
     */
    public final Unit byMorph;
    public final CommandDescription morphCMD;
    public final UnitDef morphTo;
    
    
    /**
     * What new building can build on this new level
     */
    public final ArrayList<UnitDef> levelNewDef;
    
    /**
     * List of Spring "techLevel" for this level UnitDefs
     */
    public final int[] techLevelNumber;
    
    // Economic info
    /**
     * Max economic 1 building profit
     * см. FieldBOT.advEco.resName
     */
    public final float ecoK[];
    /**
     * Max build power by 1 best constructor
     */
    public final float builderK;
    
    // War info, can be null
    public final float maxAtackRange_unit; // for movable unit
    public final float[] maxAtackPower_unit;
    public final float maxUnitSpeed;
    
    public final float maxAtackRange_def; // for defence towers
    public final float[] maxAtackPower_def;
    
    
    
    /**
     * Remove all that can not build on base surface from list
     * @param lstDef
     * @param onBase 
     */
    private static void removeWhereCantBuildOnBaseSurface(Iterable<UnitDef> lstDef,TBase onBase) {
        Iterator<UnitDef> itr1 = lstDef.iterator(); // Цикл...
        while (itr1.hasNext()) {
            UnitDef def = itr1.next();
            if (!onBase.canBuildOnBase(def, false)) itr1.remove(); // remove from list
        }
    }
    
    /**
     * Create new Tech Level on based builders and factory
     * @param baseUnit first level builder
     * @param unitOldBuild list of current builds variants (for select new unit)
     * @param botClb for getting resource list
     * @param onBase on base surface (remove unit when can not build on this surface), can be null.
     */
    public TTechLevel(UnitDef baseUnit,HashSet<UnitDef> unitOldBuild, FieldBOT botClb, TBase onBase)
    {
        this.botClb=botClb;
        
        byMorph=null; morphCMD=null; morphTo=null; // это не morph.
        
        // 1. Base level builder
        needBaseBuilders=new ArrayList<UnitDef>();
        needBaseBuilders.add(baseUnit);
        
        HashSet<Integer> tmpTechLevels=new HashSet<Integer>();
        tmpTechLevels.add( ModSpecification.getTechLevel(baseUnit) );
        
        // 2. Way to economic
        
        
        // 2. List of new units
        HashSet<UnitDef> newUnitDefs=new HashSet<UnitDef>();
        newUnitDefs.addAll(baseUnit.getBuildOptions());
        newUnitDefs.removeAll(unitOldBuild);
        
        
        if (onBase!=null) removeWhereCantBuildOnBaseSurface(newUnitDefs, onBase); // только то, что можно строить на поверхности базы.
        
        // Check mod specific: research center on TA, NOTA
        if (botClb.modSpecific.specificUnitEnabled)
            botClb.modSpecific.removeUnitWhenCantBuildWithTeclLevel(newUnitDefs, needBaseBuilders);// TODO Test it! Проверка на возможность зависимости от тех. уровней.
        
        
        if (ModSpecification.isStationarFactory(baseUnit)) {
            float lastEcoK=0; //calcEcoK(unitOldBuild,botClb);
            UnitDef bestWorker=null;
            
            // + worker
            //FIXME Check: NOTA not select builder T2 for mexes.
            for (UnitDef worker:newUnitDefs) if (worker.isBuilder()) {
                HashSet<UnitDef> newWorkerUnitDefs=new HashSet<UnitDef>(worker.getBuildOptions());
                newWorkerUnitDefs.removeAll(unitOldBuild);
                newWorkerUnitDefs.removeAll(newUnitDefs);
                float k=calcEcoK(newWorkerUnitDefs,botClb);
                if (k>lastEcoK) {
                    lastEcoK=k;
                    bestWorker=worker;
                }
            }
            if (bestWorker!=null) {
                HashSet<UnitDef> newWorkerUnitDefs=new HashSet<UnitDef>(bestWorker.getBuildOptions());
                newWorkerUnitDefs.removeAll(unitOldBuild);

                tmpTechLevels.add( ModSpecification.getTechLevel(bestWorker) );

                if (botClb.modSpecific.specificUnitEnabled)
                    botClb.modSpecific.removeUnitWhenCantBuildWithTeclLevel(newWorkerUnitDefs, needBaseBuilders);
                
                if (onBase!=null) removeWhereCantBuildOnBaseSurface(newWorkerUnitDefs, onBase); // only on base surface
                newWorkerUnitDefs.removeAll(newUnitDefs);
                if (!newWorkerUnitDefs.isEmpty()) { // TODO other workers check too
                    newUnitDefs.addAll(newWorkerUnitDefs);
                    needBaseBuilders.add(bestWorker);
                }
            }
        }
        
        levelNewDef=new ArrayList<UnitDef>(newUnitDefs);
        
        for (UnitDef def:levelNewDef) tmpTechLevels.add( ModSpecification.getTechLevel(def) );
        
        techLevelNumber = new int[tmpTechLevels.size()]; // List of all tech levels
        int i=0;
        for (Integer iLvl:tmpTechLevels) {
            techLevelNumber[i]=iLvl.intValue();
            i++;
        }
        
        // Write economic params
        ecoK = getMaxResProduct(levelNewDef, botClb);
        builderK= getMaxBuildPower(levelNewDef);
        
        // Write war params
        ArrayList<UnitDef> movableNewUnit = UnitSelector.movableUnits(levelNewDef);
        ArrayList<UnitDef> stationaryNewUnit; // = UnitSelector.stationarUnits(levelNewDef);
            stationaryNewUnit = new ArrayList(levelNewDef);
            stationaryNewUnit.removeAll(movableNewUnit);
        
        maxAtackRange_unit=getMaxAtackRange(movableNewUnit);
        maxAtackPower_unit=getMaxAtackDamage(movableNewUnit);
        maxUnitSpeed=getMaxSpeed(movableNewUnit);
        maxAtackRange_def=getMaxAtackRange(movableNewUnit);
        maxAtackPower_def=getMaxAtackDamage(movableNewUnit);
    }
    
    /**
     * Max resource product per better units
     * @param ecoUnits list of units for select
     * @param botClb for call getUnitResoureProduct()
     * @return max resources array
     */
    public static float[] getMaxResProduct(Collection<UnitDef> ecoUnits,FieldBOT botClb)
    {
        float[] maxRes=new float[botClb.avgEco.resName.length];
        Arrays.fill(maxRes, 0.0f);
        for (UnitDef def:ecoUnits) {
            float unitResP[]=botClb.getUnitResoureProduct(def);
            for (int i=0;i<maxRes.length;i++) 
                if (unitResP[i]>maxRes[i]) maxRes[i]=unitResP[i];
        }
        return maxRes;
    }
    
    /**
     * Return max build power from better builder
     * @param ecoUnits
     * @return 
     */
    public static float getMaxBuildPower(Collection<UnitDef> ecoUnits)
    {
        float maxB=0;
        for (UnitDef def:ecoUnits) if (ModSpecification.isRealyAbleToAssist(def))
        {
            float r=def.getBuildSpeed();
            if (r>maxB) maxB= r;
        }
        return maxB;
    }
    
    /**
     * Get max atack range from 1 best unit
     * @param warUnits
     * @return 
     */
    public static float getMaxAtackRange(Collection<UnitDef> warUnits)
    {
        float maxR=0;
        for (UnitDef def:warUnits) if (def.isAbleToAttack())
        {
            float r=def.getMaxWeaponRange();
            if (r>maxR) maxR= r;
        }
        return maxR;
    }
    
    /**
     * Get max weapon damage by damage type.
     * @param warUnits
     * @return max damage of [unit weapon damage summ], or null if not weapon on this list, or all weapon is were specifed.
     */
    public static float[] getMaxAtackDamage(Collection<UnitDef> warUnits)
    {
        float[] maxDmg=null;
        for (UnitDef def:warUnits) 
        {
          if (def.isAbleToAttack()) {
              float[] tmpDmg=TWarStrategy.getSumWeaponDamage(def); // TODO isParalyzed, isAbleToGround/air...
              if (tmpDmg!=null) {
                if (maxDmg==null) maxDmg=tmpDmg.clone();
                else // TODO check tmpDmg.length and maxDmg.length
                  for (int i=0; i<tmpDmg.length;i++) if (tmpDmg[i]>maxDmg[i]) maxDmg[i]=tmpDmg[i];
              }
          }
        }
        return maxDmg;
    }    
    
    public static float getMaxSpeed(Collection<UnitDef> warUnits)
    {
        float maxS=0.0f;
        for (UnitDef def:warUnits) if (def.isAbleToMove())
        { // maybe check ModSpecification.isRealyAbleToMove(), if some modifed will be on isRealyAbleToMove().
              float s=def.getSpeed();
              if (s>maxS) maxS= s;
        }
        return maxS;
    }
            
    /**
     * Create new Tecl level on morph unit (morphing unit).
     * @param morphUnit unit for morph (to morphing)
     * @param morphCMD morph command
     * @param unitOldBuild list of current builds variants (for select new unit)
     * @param botClb for getting resource list
     * @param onBase on base surface (remove unit when can not build on this surface), can be null.
     */
    public TTechLevel(Unit morphUnit, CommandDescription morphCMD, HashSet<UnitDef> unitOldBuild, FieldBOT botClb, TBase onBase)
    {
        this.botClb=botClb;
        
        byMorph=morphUnit;
        this.morphCMD=morphCMD;
        morphTo=botClb.modSpecific.getUnitDefForMprph(morphCMD, byMorph.getDef());
        
        // 1. Базовые строители уровня
        needBaseBuilders=new ArrayList<UnitDef>();
        needBaseBuilders.add(morphTo);
        
        HashSet<Integer> tmpTechLevels=new HashSet<Integer>();
        tmpTechLevels.add( ModSpecification.getTechLevel(morphTo) ); // TODO check is null !!!
        
        // Ветка до экономики

        // 2. Список того, что даёт этот уровень нового
        HashSet<UnitDef> newUnitDefs=new HashSet<UnitDef>();
        newUnitDefs.addAll(morphTo.getBuildOptions());
        newUnitDefs.removeAll(unitOldBuild);
        
        if (botClb.modSpecific.specificUnitEnabled)
            botClb.modSpecific.removeUnitWhenCantBuildWithTeclLevel(newUnitDefs, needBaseBuilders);// check tech level depends

        if (onBase!=null) removeWhereCantBuildOnBaseSurface(newUnitDefs, onBase); // only on base surface

// TODO Check it. Do not build builder after morph now, it will be next level. For TA/RD it true!
        //**
        if ( ModSpecification.isStationarFactory(morphTo)
             || (morphTo.isBuilder() && newUnitDefs.size()<=1) )
        {
            float lastEcoK=0; //calcEcoK(unitOldBuild,botClb);
            UnitDef bestWorker=null;
            
            // + worker
            for (UnitDef worker:newUnitDefs) if (worker.isBuilder()) {
                HashSet<UnitDef> newWorkerUnitDefs=new HashSet<UnitDef>(worker.getBuildOptions());
                newWorkerUnitDefs.removeAll(unitOldBuild);
                newWorkerUnitDefs.removeAll(newUnitDefs);
                float k=calcEcoK(newWorkerUnitDefs,botClb);
                if (k>lastEcoK) {
                    lastEcoK=k;
                    bestWorker=worker;
                }
            }
            if (bestWorker!=null) {
                HashSet<UnitDef> newWorkerUnitDefs=new HashSet<UnitDef>(bestWorker.getBuildOptions());
                newWorkerUnitDefs.removeAll(unitOldBuild);

                tmpTechLevels.add( ModSpecification.getTechLevel(bestWorker) );

                if (botClb.modSpecific.specificUnitEnabled)
                  botClb.modSpecific.removeUnitWhenCantBuildWithTeclLevel(newWorkerUnitDefs, needBaseBuilders);// tech level depends
                
                if (onBase!=null) removeWhereCantBuildOnBaseSurface(newWorkerUnitDefs, onBase); // only on base surface
                
                newWorkerUnitDefs.removeAll(newUnitDefs);
                if (!newWorkerUnitDefs.isEmpty()) {
                    newUnitDefs.addAll(newWorkerUnitDefs);
                    needBaseBuilders.add(bestWorker);
                }
            }
        }
        //*/
        
        levelNewDef=new ArrayList<UnitDef>();
        levelNewDef.addAll(newUnitDefs); //!!!
        
        for (UnitDef def:levelNewDef) tmpTechLevels.add( ModSpecification.getTechLevel(def) );
        // Перечень всех тех. уровней
        techLevelNumber = new int[tmpTechLevels.size()];
        int i=0;
        for (Integer iLvl:tmpTechLevels) {
            techLevelNumber[i]=iLvl.intValue();
            i++;
        }

        // Write economic params
        ecoK = getMaxResProduct(levelNewDef, botClb);
        builderK= getMaxBuildPower(levelNewDef);
        
        // Write war params
        ArrayList<UnitDef> movableNewUnit = UnitSelector.movableUnits(levelNewDef);
        ArrayList<UnitDef> stationaryNewUnit; // = UnitSelector.stationarUnits(levelNewDef);
            stationaryNewUnit = new ArrayList(levelNewDef);
            stationaryNewUnit.removeAll(movableNewUnit);
        
        maxAtackRange_unit=getMaxAtackRange(movableNewUnit);
        maxAtackPower_unit=getMaxAtackDamage(movableNewUnit);
        maxUnitSpeed=getMaxSpeed(movableNewUnit);
        maxAtackRange_def=getMaxAtackRange(movableNewUnit);
        maxAtackPower_def=getMaxAtackDamage(movableNewUnit);
    }
    
    /**
     * Tech up by morphing some units
     * @return if true - check variables: byMorph, morphCMD, morphTo; if false - not by morph, only by build factory/workers
     */
    public boolean byMorph() {
        return byMorph!=null;
    }
    
    /**
     * All resources for do this tech up
     * @return summ of all resources for build this level (all base builder + morphing)
     */
    public float[] getNeedRes() {
        float needRes[]=new float[botClb.avgEco.resName.length];
        Arrays.fill(needRes, 0);
        // для строителей уровня
        for (UnitDef def:needBaseBuilders) {
          if (byMorph==null || def!=morphTo) // byMorph чтобы не считался по 2 раза
           for (int i=0;i<needRes.length;i++) needRes[i]+=def.getCost(botClb.avgEco.resName[i]);
        }

        if (byMorph()) {
            float t=botClb.modSpecific.getTimeForMprph(morphCMD);//morphTo.getBuildTime();// TODO как это время вычислить???!!! !!!!!!!!!!!!!!!
            float resToMorph[]=botClb.modSpecific.getResForMprph(morphCMD);
            float resProduct[]=botClb.getUnitResoureProduct(byMorph.getDef());
            for (int i=0;i<needRes.length;i++) { // учесть потери от неработающего юнита во время апгрейда...
                needRes[i] += resProduct[i]*t + resToMorph[i]; // потери от простаивания + стоимость апгрейда
            }
        }
        return needRes;
    }
    /**
     * Part of working time, can be acselerating by build power
     * @return this need divide on build power of worker
     */
    public float getAssistWorkTime()  {
        float needWork=0.0f;
        boolean lastIsAssistable=true;
        for (UnitDef def:needBaseBuilders) {
            if (lastIsAssistable && def!=morphTo) needWork+=def.getBuildTime();
            lastIsAssistable = def.isAssistable();
            // TODO check BuildPower of new worker, if they will be build on this list.
        }
        return needWork;
    }
    /**
     * Part of time, can not acselerable. Do not divide it on build power.
     * Include no assistable buildings.
     * DO NOT INCLUDE: morphing time. TODO it!
     * @return time in second.
     */
    public float getNOAssistWorkTime()  {
        float needWork=0.0f;
        if (byMorph()) needWork=botClb.modSpecific.getTimeForMprph(morphCMD);
        float lstBuildPower=Float.NaN; // first can be NaN, if first lastIsAssistable be true.
        boolean lastIsAssistable=true;
        for (UnitDef def:needBaseBuilders) {
            // TODO test it
            if (!lastIsAssistable && def!=morphTo) needWork+=def.getBuildTime()/lstBuildPower;
            lastIsAssistable = def.isAssistable();
            lstBuildPower=def.getBuildSpeed();
        }
        return needWork;
    }
    
    /**
     * Return max economic product values as ResourceCoast
     * @param unitLst economic units (all units)
     * @param botClb for call getUnitResoureProduct
     * @return botClb.avgEco.getResourceCoast(ecoR);
     */
    protected static float calcEcoK(Collection<UnitDef> unitLst,FieldBOT botClb) {
        float ecoR[]=new float[botClb.avgEco.resName.length];
        for (UnitDef def:unitLst) {
            float R=0.0f;
            float resP[]=botClb.getUnitResoureProduct(def);
            for (int i=0;i<resP.length;i++)
                if (resP[i]>ecoR[i]) ecoR[i]=resP[i];
        }
        return botClb.avgEco.getResourceCoast(ecoR);
    }

    /**
     * Get time in second for build this level.
     * Contain building and morphing time
     * @param buildPower
     * @param currRes
     * @param owner only if morph level: only for getUnitResoureProduct() and owner.avgEco.resName[i]
     * @return 
     */
    public float getTimeForBuildTechLevel(float buildPower, float currRes[][], FieldBOT owner)
    {
    //TODO улучшить ... base.getBuildPower(false, true)    
      //  float t = getAssistWorkTime() / buildPower + getNOAssistWorkTime(); // время постройки
        float t;
// TODO -> это работает, но взято из TBotTalking -> 
        if (byMorph()) {
            ArrayList<UnitDef> lvlBuilder1=new ArrayList<UnitDef>(needBaseBuilders);
            
            lvlBuilder1.remove(morphTo); // remove morphTo unit from usualy build list
            float[][] currRes2=AdvECO.cloneFloatMatrix(currRes);
            UnitDef fromMorph=byMorph.getDef(); // and remove morphFrom from future economic
            buildPower -= fromMorph.getBuildSpeed();
            float resProduct[]=owner.getUnitResoureProduct(fromMorph);
            for (int i=0;i<resProduct.length;i++) {
                float deltaRes=resProduct[i];
                if (deltaRes>0.0f)
                    currRes2[AdvECO.R_Income][i] -= deltaRes;
                else if ((deltaRes<0.0f))
                    currRes2[AdvECO.R_Usage][i] -= deltaRes;
            }
            // FIXME on TA/BA morph unit was stop (paralyzed), on Zero-K it not stop - remove before/after morph depends on economic.
            
            if (!lvlBuilder1.isEmpty()) t=botClb.ecoStrategy.getRashotBuildTimeLst(lvlBuilder1, currRes, buildPower);
            else t=0;
            //TODO check bug with getNOAssistWorkTime(), last:t+=Math.max(getNOAssistWorkTime(),botClb.ecoStrategy.getWaitTimeForProductResource(currRes, botClb.modSpecific.getResForMprph(morphCMD)));
            t+=botClb.ecoStrategy.getWaitTimeForProductResource(currRes, botClb.modSpecific.getResForMprph(morphCMD));
        } else {
            t=botClb.ecoStrategy.getRashotBuildTimeLst(needBaseBuilders, currRes, buildPower);
            //t+=getNOAssistWorkTime(); // FIXME it add buildings on on assistable factory again!
        }
        return t;
    }

    /**
     * Compare tech levels
     * @param anObject
     * @return true if this levels is equals (needBaseBuilders and levelNewDef is equals).
     */
    @Override
    public boolean equals(Object anObject)
    {
        if (this == anObject) return true;
        if (anObject instanceof TTechLevel) {
            TTechLevel anotherLevel = (TTechLevel) anObject;
            if (needBaseBuilders.size()==anotherLevel.needBaseBuilders.size()
                &&
                levelNewDef.size()==anotherLevel.levelNewDef.size())
            {
                if (needBaseBuilders.containsAll(anotherLevel.needBaseBuilders)
                    &&
                    levelNewDef.containsAll(anotherLevel.levelNewDef))
                      return true;
            }
        }
        return false;
    }
    
    
    /**
     * Generate all new level variants by build new units
     * @param unitHave current exist units
     * @param unitCanBuild what can build now (what can build exist units)
     * @param onBase check build on surface (can be null)
     * @param botClb
     * @return list of all levels.
     */
    public static ArrayList<TTechLevel> GetAllVariantLevel(HashSet<UnitDef> unitHave,HashSet<UnitDef> unitCanBuild, TBase onBase, FieldBOT botClb)
    {
        // 1. Поиск новых, не построенных
        HashSet<UnitDef> newUnits=new HashSet<UnitDef>();
        newUnits.addAll(unitCanBuild);//!!! unitCanBuild.clone();
        newUnits.removeAll(unitHave);
           
        // 2. Выборка строителей.
        Iterator<UnitDef> itr1 = newUnits.iterator(); // Цикл...
        while (itr1.hasNext()) {
            UnitDef def = itr1.next();
            if (!def.isBuilder() || def.getBuildOptions().isEmpty()) itr1.remove(); // убрать не строителей
            else { // Чтобы они могли построить что-то новое
                boolean canBuildNewVariant=false;
                for (UnitDef ud:def.getBuildOptions()) if (!unitCanBuild.contains(ud)) {
                    canBuildNewVariant=true;
                    break;
                }
                if (!canBuildNewVariant) itr1.remove(); // убрать не новые возможности
            }
        }
        
        // 3. выделить группы одинаково строющих строителей
        // TODO... пока так сойдёт.
        
        // 2.2 выбрать тех, кого можно строить (по тех. уровню для TA,RD, может быть NOTA...)
        if (botClb.modSpecific.specificUnitEnabled)
            botClb.modSpecific.removeUnitWhenCantBuildWithTeclLevel(newUnits);// TODO Test it! Проверка на возможность зависимости от тех. уровней.
    
        // 4. Создание списка технических уровней
        ArrayList<TTechLevel> nextTechLst=new ArrayList<TTechLevel>();
        for (UnitDef ud:newUnits)
        {
            TTechLevel newTL=new TTechLevel(ud,unitCanBuild, botClb, onBase );
            nextTechLst.add(newTL);
        }
        
        return nextTechLst;
    }
    /**
     * Generate all new level variants by morph exist units to new unit
     * @param currentUnits current exist units (check all morph CMD from this units)
     * @param unitHave current exist units (TODO deprecated?)
     * @param unitCanBuild what can build now (what can build exist units)
     * @param onBase for call modSpecific.getUnitDefForMprph() and show debug message
     * @param botClb
     * @return list of all levels by morph.
     */
    public static ArrayList<TTechLevel> GetAllVariantLevelByMorph(List<Unit> currentUnits,HashSet<UnitDef> unitHave,HashSet<UnitDef> unitCanBuild, TBase onBase, FieldBOT botClb)
    {
        ArrayList<TTechLevel> nextTechLst=new ArrayList<TTechLevel>(); // 4. Создание списка технических уровней
        
        // 1. Поиск возможностей апгрейдов
        for (Unit u:currentUnits) {
            List<CommandDescription> morphLst=ModSpecification.getMorphCmdList(u, false);
            for (CommandDescription cmd:morphLst) {
                UnitDef morphTo=botClb.modSpecific.getUnitDefForMprph(cmd, u.getDef());
               if (morphTo==null) {
                   // TODO !!!!
                   botClb.sendTextMsg(" Can't assign unit type for "+u.getDef().getName()+" morph to morph ID="+cmd.getId(), FieldBOT.MSG_ERR);
               } else {
                if (morphTo.isBuilder() && !morphTo.getBuildOptions().isEmpty() && !unitCanBuild.contains(morphTo)) // если строитель и такого не можем строить, то
                {// проверить, что нового умеет строить
                    botClb.sendTextMsg(" analyze morph variant to "+morphTo.getHumanName(), FieldBOT.MSG_DBG_ALL); // DEBUG!
                    boolean canBuildNewVariant=false;
                    for (UnitDef ud:morphTo.getBuildOptions()) if (!unitCanBuild.contains(ud)) {
                        canBuildNewVariant=true;
                        break;
                    }
                    if (canBuildNewVariant) { // Это НОВОЕ!!!!
                        botClb.sendTextMsg("  <this variant add to list>", FieldBOT.MSG_DBG_ALL);
                        TTechLevel newTL=new TTechLevel(u, cmd , unitCanBuild, botClb, onBase);
                        nextTechLst.add(newTL);
                    }
                } else botClb.sendTextMsg(" morph variant is not Tech Level morphTo="+morphTo.getHumanName(), FieldBOT.MSG_DBG_ALL);
               }
            }
        }
        // 3. выделить группы одинаково строющих строителей
        // TODO... пока так сойдёт.
        
        return nextTechLst;
    }
    
    /**
     * Generate all new future tech level variants, on base current level and one new tech level.
     * Not support future level by morph unit.
     * @param baseLevel add on base this tech level
     * @param unitHave current unit have (can be null)
     * @param unitCanBuild current build list (can be null)
     * @param onBase
     * @param botClb
     * @return list of new future level
     */
    public static ArrayList<TTechLevel> GetAllNextVariantLevel(TTechLevel baseLevel, HashSet<UnitDef> unitHave, HashSet<UnitDef> unitCanBuild, TBase onBase, FieldBOT botClb)
    {
        HashSet<UnitDef> tmpUnitHave=new HashSet<UnitDef>();
        
        HashSet<UnitDef> tmpUnitCanBuild=new HashSet<UnitDef>();

        if (unitHave!=null) tmpUnitHave.addAll(unitHave);
        tmpUnitHave.addAll(baseLevel.needBaseBuilders);
        
        if (unitCanBuild!=null) tmpUnitCanBuild.addAll(unitCanBuild);
        tmpUnitCanBuild.addAll(baseLevel.levelNewDef);
        
        return GetAllVariantLevel(tmpUnitHave, tmpUnitCanBuild, onBase, botClb);
        // TODO add + morph level
    }
         
    
    @Override
    public String toString() {
        String s="level="+Arrays.toString(techLevelNumber);
        if (byMorph()) s+=" (by Morph)";
        s+=" ecoK="+Arrays.toString(ecoK)+" build power="+builderK+" need build: {";
        for (UnitDef def:needBaseBuilders) s+=" "+def.getName()+" -tlvl"+ModSpecification.getTechLevel(def)+"- "+def.getHumanName()+",";
        s+="} new: {";
        for (UnitDef def:levelNewDef) s+=" "+def.getName()+",";
        s+="}";
        return s;
    }

}
