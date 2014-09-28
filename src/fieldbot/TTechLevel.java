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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

// TODO при botClb.modSpecific.specificUnitEnabled учесть строительство лаборатории, как тех. уровень.
// botClb.modSpecific.removeUnitWhenCantBuildWithTeclLevel(newUnitDefs, tmpTechLevels)...



/**
 * TechLevel определяет что нового можно строить и средную выгоду от этого.
 * Что нужно построить чтобы достичь этого уровня. Сколько это будет стоить.
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
    
    // War info
    public final float maxAtackRange;
    public final float[] maxAtackPower;
    public final float maxUnitSpeed;
    
    
    
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
        tmpTechLevels.add( baseUnit.getTechLevel() );
        
        // 2. Way to economic
        
        
        // 2. List of new units
        HashSet<UnitDef> newUnitDefs=new HashSet<UnitDef>();
        newUnitDefs.addAll(baseUnit.getBuildOptions());//!!! unitCanBuild.clone();
        newUnitDefs.removeAll(unitOldBuild);
        
        
        if (onBase!=null) removeWhereCantBuildOnBaseSurface(newUnitDefs, onBase); // только то, что можно строить на поверхности базы.
        
        // Check mod specific: research center on TA, NOTA
        if (botClb.modSpecific.specificUnitEnabled)
            botClb.modSpecific.removeUnitWhenCantBuildWithTeclLevel(newUnitDefs, tmpTechLevels);// TODO Test it! Проверка на возможность зависимости от тех. уровней.
        
        
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

                tmpTechLevels.add( bestWorker.getTechLevel() );

                if (botClb.modSpecific.specificUnitEnabled)
                    botClb.modSpecific.removeUnitWhenCantBuildWithTeclLevel(newWorkerUnitDefs, tmpTechLevels);
                
                if (onBase!=null) removeWhereCantBuildOnBaseSurface(newWorkerUnitDefs, onBase); // only on base surface
                newWorkerUnitDefs.removeAll(newUnitDefs);
                if (!newWorkerUnitDefs.isEmpty()) { // TODO other workers check too
                    newUnitDefs.addAll(newWorkerUnitDefs);
                    needBaseBuilders.add(bestWorker);
                }
            }
        }
        
        levelNewDef=new ArrayList<UnitDef>(newUnitDefs);
        
        for (UnitDef def:levelNewDef) tmpTechLevels.add( def.getTechLevel() );
        
        techLevelNumber = new int[tmpTechLevels.size()]; // List of all tech levels
        int i=0;
        for (Integer iLvl:tmpTechLevels) {
            techLevelNumber[i]=iLvl.intValue();
            i++;
        }
        
        // Определение экон. параметров.
        ecoK = getMaxResProduct(levelNewDef, botClb);
        builderK= getMaxBuildPower(levelNewDef);
        
        maxAtackRange=getMaxAtackRange(levelNewDef);
        maxAtackPower=getMaxAtackDamage(levelNewDef);
        maxUnitSpeed=getMaxSpeed(levelNewDef);
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
        for (UnitDef def:ecoUnits) 
          for (int i=0;i<maxRes.length;i++)
          {
            float r=botClb.getUnitResoureProduct(def, botClb.avgEco.resName[i]);
            if (r>maxRes[i]) maxRes[i]=r;
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
        for (UnitDef def:ecoUnits) 
        {
          float r;
          if (ModSpecification.isRealyAbleToAssist(def)) r=def.getBuildSpeed();
          else r=0;
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
        for (UnitDef def:warUnits) 
        {
          float r;
          if (def.isAbleToAttack()) r=def.getMaxWeaponRange();
          else r=0;
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
                if (maxDmg==null) maxDmg=tmpDmg;
                else
                  for (int i=0; i<tmpDmg.length;i++) if (tmpDmg[i]>maxDmg[i]) maxDmg[i]=tmpDmg[i];
              }
          }
        }
        return maxDmg;
    }    
    
    public static float getMaxSpeed(Collection<UnitDef> warUnits)
    {
        float maxS=0;
        for (UnitDef def:warUnits) 
        {
          float s;
          if (def.isAbleToMove()) s=def.getSpeed(); // !!!
          else s=0;
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
        morphTo=botClb.modSpecific.getUnitDefForMprph(morphCMD);
        
        // 1. Базовые строители уровня
        needBaseBuilders=new ArrayList<UnitDef>();
        needBaseBuilders.add(morphTo);
        
        // было techLevelNumber=morphTo.getTechLevel(); //!!!
        HashSet<Integer> tmpTechLevels=new HashSet<Integer>();
        tmpTechLevels.add( morphTo.getTechLevel() ); // TODO check is null !!!
        
        // Ветка до экономики

        // 2. Список того, что даёт этот уровень нового
        HashSet<UnitDef> newUnitDefs=new HashSet<UnitDef>();
        newUnitDefs.addAll(morphTo.getBuildOptions());//!!! unitCanBuild.clone();
        newUnitDefs.removeAll(unitOldBuild);
        
        if (botClb.modSpecific.specificUnitEnabled)
            botClb.modSpecific.removeUnitWhenCantBuildWithTeclLevel(newUnitDefs, tmpTechLevels);// check tech level depends

        if (onBase!=null) removeWhereCantBuildOnBaseSurface(newUnitDefs, onBase); // only on base surface

// TODO Check it. Do not build builder after morph now, it will be next level. For TA/RD it true!
        /**
        if (ModSpecification.isStationarFactory(morphTo)) {
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

                // было techLevelNumber=Math.max(techLevelNumber,bestWorker.getTechLevel());
                tmpTechLevels.add( bestWorker.getTechLevel() );

                if (botClb.modSpecific.specificUnitEnabled)
                  botClb.modSpecific.removeUnitWhenCantBuildWithTeclLevel(newWorkerUnitDefs, tmpTechLevels);// tech level depends
                
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
        levelNewDef.addAll(newUnitDefs); //!!!!!!!!!!!!!!!!!!!!!!!
        
        for (UnitDef def:levelNewDef) tmpTechLevels.add( def.getTechLevel() );
        // Перечень всех тех. уровней
        techLevelNumber = new int[tmpTechLevels.size()];
        int i=0;
        for (Integer iLvl:tmpTechLevels) {
            techLevelNumber[i]=iLvl.intValue();
            i++;
        }

        // Определение экон. параметров.
        ecoK = getMaxResProduct(levelNewDef, botClb);
        builderK=getMaxBuildPower(levelNewDef);
        
        maxAtackRange=getMaxAtackRange(levelNewDef);
        maxAtackPower=getMaxAtackDamage(levelNewDef);
        maxUnitSpeed=getMaxSpeed(levelNewDef);
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
        float needWork=0.0f;
        // для строителей уровня
        for (UnitDef def:needBaseBuilders) {
          if (byMorph==null || def!=morphTo) // byMorph чтобы не считался по 2 раза
           for (int i=0;i<needRes.length;i++) needRes[i]+=def.getCost(botClb.avgEco.resName[i]);
        }

        if (byMorph()) {
            float t=botClb.modSpecific.getTimeForMprph(morphCMD);//morphTo.getBuildTime();// TODO как это время вычислить???!!! !!!!!!!!!!!!!!!
            float resToMorph[]=botClb.modSpecific.getResForMprph(morphCMD);
            for (int i=0;i<needRes.length;i++) { // учесть потери от неработающего юнита во время апгрейда...
                float w=botClb.getUnitResoureProduct(byMorph.getDef(), botClb.avgEco.resName[i]);
                needRes[i]+=w*t + resToMorph[i]; // потери от простаивания + стоимость апгрейда
            }
        }
        return needRes;
    }
    /**
     * Часть рабочего времени, ускоряемого
     * @return нужно разделить на силу строителей
     */
    public float getAssistWorkTime()  {
        float needWork=0.0f;
        boolean lastIsAssistable=true;
        for (UnitDef def:needBaseBuilders) {
            if (lastIsAssistable && def!=morphTo) needWork+=def.getBuildTime();
            lastIsAssistable = def.isAssistable();
            // TODO учесть силу построевшегося строителя
        }
        return needWork;
    }
    /**
     * Часть времени, НЕ ускоряемого другими строителями.
     * @return time in second.
     */
    public float getNOAssistWorkTime()  {
        float needWork=0.0f;
        if (byMorph()) needWork=botClb.modSpecific.getTimeForMprph(morphCMD);
        float lstBuildPower=100.0f;
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
            for (int i=0;i<botClb.avgEco.resName.length;i++) {
                Resource res=botClb.avgEco.resName[i];
                float r=botClb.getUnitResoureProduct(def, res);
                if (r>ecoR[i]) ecoR[i]=r;
            }
        }
        return botClb.avgEco.getResourceCoast(ecoR);
    }

    /**
     * Get time in second for build this level.
     * @param buildPower
     * @param currRes
     * @return 
     */
    public float getTimeForBuildTechLevel(float buildPower, float currRes[][])
    {
    //TODO улучшить ... base.getBuildPower(false, true)    
      //  float t = getAssistWorkTime() / buildPower + getNOAssistWorkTime(); // время постройки
        float t;
// TODO -> это работает, но из TBotTalking -> 
        if (byMorph()) {
            ArrayList<UnitDef> lvlBuilder1=new ArrayList<UnitDef>(needBaseBuilders);
            lvlBuilder1.remove(morphTo);
            if (!lvlBuilder1.isEmpty()) t=botClb.ecoStrategy.getRashotBuildTimeLst(lvlBuilder1, currRes, buildPower);
            else t=0;
            t+=Math.max(getNOAssistWorkTime(),botClb.ecoStrategy.getWaitTimeForProductResource(currRes, botClb.modSpecific.getResForMprph(morphCMD)));
        } else {
            t=botClb.ecoStrategy.getRashotBuildTimeLst(needBaseBuilders, currRes, buildPower);
            t+=getNOAssistWorkTime();
        }
        return t;
        
        /*
        //float t=ecoStrategy.getRashotBuildTimeLst(tLvl.needBaseBuilders, avgEco.getAVGResourceToArr(), buildPower);
// FIXME t бывает ОЧЕНЬ БОЛЬШИМ!!!
        float needRes[]=getNeedRes();
        //было float currRes[][]=avgEco.getSmartAVGResourceToArr(); // было avgEco.getAVGResourceToArr();
        // TODO test: учесть ложную нехватку ресурсов - когда можно расходы поделить на несколько строителей.    
        for (int i=0;i<currRes[AdvECO.R_Current].length;i++) {
            float dResNeed=needRes[i]-currRes[AdvECO.R_Current][i];
            float dResHave= (currRes[AdvECO.R_Income][i]-currRes[AdvECO.R_Usage][i]) ;
            float iT;
            if (dResNeed>0)
            {
              if (dResHave>0) iT= dResNeed / dResHave;
                         else return Float.POSITIVE_INFINITY;//!!!
              t=Math.max(t, iT);
            }
        }
        return t;
        //*/
    }

    /**
     * Compare tech levels
     * @param anObject
     * @return true if this levels is equals (needBaseBuilders and levelNewDef is equals).
     */
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
     * Генерирует все варианты новых уровней (переходов)
     * @param unitHave какие типы юнитов уже есть
     * @param unitCanBuild какие типы юнитов можно строить
     * @param onBase проверка возможности постройки на поверхности базы (может быть null)
     * @return лист всех вариантов экономически значимых...
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
     * Генерирует все варианты новых уровней (переходов)
     * @param currentUnits список всех юнитов (проверяется возможность morph у каждого)
     * @param unitHave какие типы юнитов уже есть
     * @param unitCanBuild какие типы юнитов можно строить
     * @return лист всех вариантов экономически значимых...
     */
    public static ArrayList<TTechLevel> GetAllVariantLevelByMorph(List<Unit> currentUnits,HashSet<UnitDef> unitHave,HashSet<UnitDef> unitCanBuild, TBase onBase, FieldBOT botClb)
    {
        ArrayList<TTechLevel> nextTechLst=new ArrayList<TTechLevel>(); // 4. Создание списка технических уровней
        
        // 1. Поиск возможностей апгрейдов
        for (Unit u:currentUnits) {
            List<CommandDescription> morphLst=ModSpecification.getMorphCmdList(u, false);
            for (CommandDescription cmd:morphLst) {
                UnitDef morphTo=botClb.modSpecific.getUnitDefForMprph(cmd);
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
     * Генерирует все варианты новых уровней (переходов), на базе текущего состояния и заданного будущего уровня.
     * @param baseLevel дополнительный уровень, от которого идти дальше...
     * @param botClb
     * @return 
     */
    public static ArrayList<TTechLevel> GetAllNextVariantLevel(TTechLevel baseLevel, TBase onBase, FieldBOT botClb)
    {
        // HashSet<UnitDef> unitHave,HashSet<UnitDef> unitCanBuild,
        HashSet<UnitDef> tmpUnitHave=new HashSet<UnitDef>();
        HashSet<UnitDef> tmpUnitCanBuild=new HashSet<UnitDef>();
        
        //tmpUnitHave.addAll(unitHave);
        tmpUnitHave.addAll(baseLevel.needBaseBuilders);
        
        //tmpUnitCanBuild.addAll(unitCanBuild);
        tmpUnitCanBuild.addAll(baseLevel.levelNewDef);
        
        return GetAllVariantLevel(tmpUnitHave, tmpUnitCanBuild, onBase, botClb);
        // TODO пока ВСЕ в т.ч. предыдущие будут!!!!
    }
         
    
    @Override
    public String toString() {
        String s="level="+Arrays.toString(techLevelNumber)+" byMorph="+byMorph()+" ecoK="+Arrays.toString(ecoK)+" build power="+builderK+" need build:(";
        for (UnitDef def:needBaseBuilders) s+=" "+def.getName()+" -tlvl"+def.getTechLevel()+"- "+def.getHumanName()+",";
        s+=") new:(";
        for (UnitDef def:levelNewDef) s+=" "+def.getName()+",";
        s+=")";
        return s;
    }

}
