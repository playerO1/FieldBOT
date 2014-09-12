/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package fieldbot;

import fieldbot.baseplaning.ABasePlaning;
import fieldbot.baseplaning.TBasePlaning_tetris;
import fieldbot.baseplaning.TBasePlaning_randomNear;
import fieldbot.baseplaning.TBasePlaning_spiral;
import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.*;
import fieldbot.AIUtil.MathPoints;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;


/**
 * Модель базы. Содержит юниты, постройки, локальную экономику. Представляет собой самостоятельный ИИ выполняющий стратегические команды развития.
 * @author PlayerO1
 */
public class TBase extends AGroupManager{
   protected static final boolean ENABLE_CASHING=true; // вкл/выкл кэш.
    
    
   public final FieldBOT owner;
    
   /**
    * Центр базы
    */
   public AIFloat3 center;
   /**
    * Радиус базы (размер)
    */
   public float radius;
   
   /**
    * Юниты принадлежащие базе
    */
   public final ArrayList<com.springrts.ai.oo.clb.Unit>
           idleCons,workingCons,
           building,army, // строения, армия
           buildLst, buildLst_noAssistable; // buildLst - строющиеся юниты.
   
   /**
    * Hight priority building work list
    */
   public TBaseTarget currentBaseTarget;
   public ABasePlaning stroyPlaning;
   
   protected static final int DEFAULT_TIMEOUT = Integer.MAX_VALUE;
   public int BUILD_FACING = 0; // stump, TODO
   
   /**
    * Base stupid protected system. TODO.
    * if enemy come near to base (check every 100 update frame), this variable set to near enemy position, then send owner army with fight to this point and set null to this variable.
    */
   protected AIFloat3 lastAgressorPoint;
   
    /**
     * Create empty base. Dont forget add builder unit!
     * @param owner link to AI
     * @param center init base center
     * @param radius base radius (size)
     */
    public TBase(FieldBOT owner, AIFloat3 center, float radius)
    {
        this.owner=owner;
        this.center=new AIFloat3(center);
        
        if (radius<=0) radius=1000.0f; // !!!!
        this.radius=radius;
        
        this.baseType=0; // !!!
        
        idleCons=new ArrayList<Unit>();
        workingCons=new ArrayList<Unit>();
        building=new ArrayList<Unit>();
        army=new ArrayList<Unit>();
        
        buildLst=new ArrayList<Unit>();
        buildLst_noAssistable=new ArrayList<Unit>();
        
        currentBaseTarget=new TBaseTarget();

        // TODO !!!!!!!!!!!
        switch (owner.defauldBasePlaning) {
            case 0:
                stroyPlaning=new TBasePlaning_randomNear(this);
                break;
            case 1:
                stroyPlaning=new TBasePlaning_spiral(this);
                break;
            //case 2:
                //stroyPlaning=new ...(this);
                //break;
            case 3:
                stroyPlaning=new TBasePlaning_tetris(this);
                break;
            default:
                owner.sendTextMsg("Error: planing level not found! Level №="+owner.defauldBasePlaning, FieldBOT.MSG_ERR);
                stroyPlaning=new TBasePlaning_spiral(this);
                // TODO base planing
        }
        // // TODO Test разные планирования.
        
        lastAgressorPoint=null;
        
        //if (ENABLE_CASHING) dropCashe();// Init cashes.
    }
    
    /**
     * Переопределяет центр базы относительно среднего кол-ва построек.
     */
    public void reinitCenter() {
        double mx,my,mz;
        //float maxRadius=radius;
        int n;
        mx=my=mz=0.0;
        List<Unit> allUnit=getAllUnits(false);
        n=allUnit.size();
        if (n==0) return;// !!!
        for (Unit u:allUnit) {
            AIFloat3 p=u.getPos();
            mx+=p.x; my+=p.y; mz+=p.z;
            float r=u.getDef().getBuildDistance();
            if (r>radius) radius=r; // !!! для NOTA - радиус строительных башен...
        }
        mx=mx/(double)n;// !!!
        my=my/(double)n;
        mz=mz/(double)n;
        //center=new AIFloat3((float)mx,(float)my,(float)mz);
        moveTo(new AIFloat3((float)mx,(float)my,(float)mz));
    }
    
    /**
     * Moving base center, and drop cashe.
     * @param newCenter 
     */
    public void moveTo(AIFloat3 newCenter) {
        center=newCenter;
        dropAllCashe();// update canBuildOnBaseSurface cashe
        stroyPlaning.onBaseMoving();
    }
    

   @Override
    public AIFloat3 getCenter() {
        return center;
    }
    
    // ----- cashe -----
    private HashSet<UnitDef> cashe_getContainUnitDefsList_false=null;
    private HashSet<UnitDef> cashe_getBuildList_false=null;
    private HashSet<UnitDef> cashe_getBuildList_true=null;
    private HashSet<UnitDef> cashe_NOTcanBuildOnBase_false=null; // список того, чего НЕ может строить на базе
    private HashSet<UnitDef> cashe_CANBuildOnBase_false=null; // список того, чего МОЖЕТ строить на базе
    private HashSet<UnitDef> cashe_NOTcanBuildOnBase_true=null; // список того, чего НЕ может строить на базе
    private HashSet<UnitDef> cashe_CANBuildOnBase_true=null; // список того, чего МОЖЕТ строить на базе
    /**
     * Сбросить кэши.
     */
    private void dropCashe()
    {
        cashe_getContainUnitDefsList_false=null;
        cashe_getBuildList_false=null;
        cashe_getBuildList_true=null;
        
        cashe_NOTcanBuildOnBase_true=null;
        cashe_CANBuildOnBase_true=null;
        //не сбрасывать кэш построек...
        //cashe_NOTcanBuildOnBase_false=null;
        //cashe_CANBuildOnBase_false=null;
    }
    /**
     * Сбросить кэши.
     */
    private void dropAllCashe()
    {
        dropCashe();
//       было cashe_getContainUnitDefsList_false=null;
//        cashe_getBuildList_false=null;
//        cashe_getBuildList_true=null;
        //Дополнительно:
        cashe_NOTcanBuildOnBase_false=null;
        cashe_CANBuildOnBase_false=null;
    }
    // -----------
    
    // -- - - --
    // Команды по распределению юнитов

    /**
     * Add unit into base
     * @param unit
     * @return modifed
     */
   @Override
    public boolean addUnit(Unit unit) {
        stroyPlaning.onAdd(unit);// !!!
        if (unit.isBeingBuilt()) {
            if (!buildLst.contains(unit) && !buildLst_noAssistable.contains(unit)) {
                buildLst.add(unit);
                // FIXME что делать с !!!!! buildLst_noAssistable ???
            }
            if (ENABLE_CASHING) dropCashe();
            return true;
        }
        UnitDef ut=unit.getDef();
        if (ut.isBuilder() || ut.isCommander() || ut.isAbleToAssist()) { // TODO test!
            if (!idleCons.contains(unit) && !workingCons.contains(unit)) idleCons.add(unit);
            if (ENABLE_CASHING) dropCashe();
            return true;
        }
        // TODO what doing with nano-tower builders!?!
        
        if (!ut.isAbleToMove()) {
            if (!building.contains(unit)) building.add(unit);
            if (ENABLE_CASHING) dropCashe();
            return true;
        }
        if (ut.isAbleToAttack()) {
            if (!army.contains(unit)) army.add(unit);
            if (ENABLE_CASHING) dropCashe();
            return true;
        }
        return false; // ???
        //if (unit.getDef().isCommander())
    }
    
    /**
     * Remove unit from base
     * @param unit
     * @return modifed
     */
   @Override
    public boolean removeUnit(Unit unit) {
        stroyPlaning.onRemove(unit);
        boolean found=false;
        int dbgNumR=0;
        if (idleCons.contains(unit)) {
            idleCons.remove(unit);
            found=true;
            dbgNumR++;
        }
        if (workingCons.contains(unit)) {
            workingCons.remove(unit);
            found=true;
            dbgNumR++;
        }
        if (building.contains(unit)) {
            building.remove(unit);
            found=true;
            dbgNumR++;
        }
        if (army.contains(unit)) {
            army.remove(unit);
            found=true;
            dbgNumR++;
        }
        if (buildLst.contains(unit)) {
            buildLst.remove(unit);
            found=true;
            dbgNumR++;
        }
        if (buildLst_noAssistable.contains(unit)) {
            buildLst_noAssistable.remove(unit);
            found=true;
            dbgNumR++;
        }

        if (ENABLE_CASHING) if (found) dropCashe();
        // TODO проверить, вдруг в 2-х списках одновременно!!!!
        owner.sendTextMsg(" call removeUnit result="+found+" num of removing list="+dbgNumR+" test contain (false-no error)="+contains(unit) , FieldBOT.MSG_DBG_SHORT );
        return found;
    }
    
   @Override
    public boolean removeAllUnits(Collection<Unit> units) {
        boolean modifed = idleCons.removeAll(units) || workingCons.removeAll(units) || building.removeAll(units) || army.removeAll(units) || buildLst.removeAll(units) || buildLst_noAssistable.removeAll(units);
        if (modifed) {
            if (ENABLE_CASHING) dropCashe();
            for (Unit u:units) stroyPlaning.onRemove(u);
        }
        return modifed;
    }
    
    /**
     * Содержит ли юнита в списках?
     * @param unit
     * @return 
     */
   @Override
    public boolean contains(Unit unit) {
        return idleCons.contains(unit) || workingCons.contains(unit) || building.contains(unit) || army.contains(unit) || buildLst.contains(unit) || buildLst_noAssistable.contains(unit);
    }
    
    public boolean setAsWorking(Unit unit) {
        if (idleCons.contains(unit)) idleCons.remove(unit);
        if (!workingCons.contains(unit)) workingCons.add(unit);
        if (ENABLE_CASHING) dropCashe();
        return true;
    }
    public boolean setAsIdle(Unit unit) {
        if (workingCons.contains(unit)) workingCons.remove(unit);
        if (!idleCons.contains(unit)) idleCons.add(unit);
        if (ENABLE_CASHING) dropCashe();
        return true;
    }
    
    public boolean setAllAsWorking(List<Unit> units) {
        //if (idleCons.contains(unit)) idleCons.remove(unit);
        //if (!workingCons.contains(unit)) workingCons.add(unit);
        //TIXME от списка не рабочих юнитов проверку.
        if (idleCons.removeAll(units)) {
            workingCons.addAll(units); // TODO если в списке не строители?
            if (ENABLE_CASHING) dropCashe();
        }
        return true;
    }
    // TODO setAllAsWork/idle()

    /**
     * На сколько нужен этот юнит данной группе
     * @return 
     */
   @Override
    public float doYouNeedUnit(Unit unit) {
        float k=0.7f;
        UnitDef ud=unit.getDef();
        // TODO учесть дистанцию
        switch (baseType) {
            case 0: // главная база
                if (ud.isBuilder()) k+=0.2f;
                if (ud.isCommander()) k+=0.2f;
                break;
            case 1: // развед база
                if (ud.isAbleToRepair()) k+=0.1f;
                if (ud.isBuilder()) k+=0.1f;
                if (ud.isAbleToAttack()) k+=0.2f;
                if (ud.isCommander()) k-=0.2f; // командир не нужен в развед части
                if (ud.isStealth()) k+=0.2f;
                break;
            default:// ?? Not set.
                k-=0.2f;
        }
        
        float l = MathPoints.getDistanceBetweenFlat(unit.getPos(), center);
        if (l>radius) l = l / radius; else l=1.0f;
        if (l>7.0f) l=7.0f;
        k = k / (l+1.0f);
        
        return k; // TODO процент нужды в юните
    }
    
    /**
     * Возвращает всех юнитов приписанных к базе
     * @param selectWhoCanNow - true = выбирает только тех, кто может сейчас строить (достроенных, и не paralyzed)
     * @return 
     */
   @Override
    public ArrayList<Unit> getAllUnits(boolean selectWhoCanNow) {
        ArrayList<Unit> unitLst=new ArrayList<Unit>(); // типы юнитов
        // TODO кэшировать...
        if (selectWhoCanNow) {
            for (Unit u:idleCons) if (!u.isParalyzed()) unitLst.add(u);// && !u.isBeingBuilt()
            for (Unit u:workingCons) if (!u.isParalyzed()) unitLst.add(u);
            for (Unit u:building) if (!u.isParalyzed()) unitLst.add(u);
            for (Unit u:army) if (!u.isParalyzed()) unitLst.add(u);
        } else {
            unitLst.addAll(idleCons);
            unitLst.addAll(workingCons);
            unitLst.addAll(building);
            unitLst.addAll(army);
            unitLst.addAll(buildLst); // !!!!!!!!!!!!!!!!
            unitLst.addAll(buildLst_noAssistable);// !!!!!!!!!!!!!!!
        }

//      owner.sendTextMsg("getAllUnits count = "+unitLst.size() , FieldBOT.MSG_DBG_ALL);// DEBUG!!!
        return unitLst;
    }
    
   @Override
    public boolean isEmpty() {
        return idleCons.isEmpty() && workingCons.isEmpty() && building.isEmpty() && army.isEmpty() && buildLst.isEmpty() && buildLst_noAssistable.isEmpty();
    }
    /**
     * Возвращает перечень всех типов юнитов на базе
     * @param onlyWhatIdle только тех, кто не занят и не строится.
     * @return 
     */
   @Override
    public HashSet<UnitDef> getContainUnitDefsList(boolean onlyWhatIdle) {
        // -- кэш --
        if (ENABLE_CASHING) if (!onlyWhatIdle && cashe_getContainUnitDefsList_false!=null) return cashe_getContainUnitDefsList_false;
        // -- --
        
        HashSet<UnitDef> currentUnitTypesSet=new HashSet<UnitDef>(); // типы юнитов
        for (Unit builder:getAllUnits(onlyWhatIdle)) currentUnitTypesSet.add(builder.getDef());
        
        // -- обновить кэш --
        if (ENABLE_CASHING) if (!onlyWhatIdle) cashe_getContainUnitDefsList_false=currentUnitTypesSet;
        // -- --
        
        return currentUnitTypesSet;
        // TODO кэшировать вывод
    }
    
    /**
     * Множество того, что может производить база (без учёта что нет ресурсов)
     * @param onlyWhatCanBuildOnBaseSurface только те, которые могут быть построены на поверхности базы (корабли, или сухопутные....)
     * @return список типов юнитов (не повторяются копии типов, как set)
     */
    public HashSet<UnitDef> getBuildList(boolean onlyWhatCanBuildOnBaseSurface) {
        
        // -- кэш --
        if (ENABLE_CASHING) {
          if (!onlyWhatCanBuildOnBaseSurface && cashe_getBuildList_false!=null) return cashe_getBuildList_false;
          if (onlyWhatCanBuildOnBaseSurface && cashe_getBuildList_true!=null) return cashe_getBuildList_true;
        }
        // -- --
        
        HashSet<UnitDef> currentUnitTypesSet=null; // типы юнитов
        HashSet<UnitDef> lst=new HashSet<UnitDef>(); // типы того что можно строить

       // проверка specificUnitEnabled (и хотья бы какой-то учёт лаборатории и уровней)
      //if (!owner.modSpecific.specificUnitEnabled) { // Если в моде нет сложных зависимостей...  
          currentUnitTypesSet=getContainUnitDefsList(false);
        //for (Unit builder:getAllUnits(false)) currentUnitTypesSet.add(builder.getDef());
        // TODO проверять isParalyzed()
        for (UnitDef unitType:currentUnitTypesSet) 
          for (UnitDef canBuild:unitType.getBuildOptions())
          {
                if (onlyWhatCanBuildOnBaseSurface) {
                    if (!lst.contains(canBuild)) if (canBuildOnBase(canBuild, true)) lst.add(canBuild);
                } else {
                    lst.add(canBuild); // TODO addAll т.к. HashSet не позволит повторяющиеся. может быть быстреебудет...
                }
          }
        
        // Для проверки возможности построить с лабораторией в TA/RD
        if (owner.modSpecific.specificUnitEnabled)
            owner.modSpecific.removeUnitWhenCantBuildWithTeclLevel(lst);
        
        // TODO проверка specificUnitEnabled !!!! Пока не полностью работает из-за modSpecific.getRealBuildList(unit)
        // Очень точно, но медленно
//      } else {
//          // Медленно, но гарантированно что это возможно строить.
//          currentUnitTypesSet=new HashSet<UnitDef>(); // Какие типы юнитов уже просмотренны.
//        for (Unit unit:getAllUnits(false)) {
//            UnitDef def=unit.getDef();
//            if (def.isBuilder() && !currentUnitTypesSet.contains(def)) {
//                ArrayList<UnitDef> unitLst=owner.modSpecific.getRealBuildList(unit);
//                owner.sendTextMsg(" special get unitLst size="+unitLst.size(), FieldBOT.MSG_DBG_SHORT);
//                if (onlyWhatCanBuildOnBaseSurface) {
//                    for (UnitDef canBuild:unitLst)
//                     if (!lst.contains(canBuild)) if (canBuildOnBase(canBuild, true)) lst.add(canBuild);
//                } else lst.addAll(unitLst);
//                currentUnitTypesSet.add(def);
//            }
//        }
//      }
//                owner.sendTextMsg(" special finish unitLst size="+lst.size(), FieldBOT.MSG_DBG_SHORT);
        
        // -- обновить кэш --
        if (ENABLE_CASHING) {
          if (!onlyWhatCanBuildOnBaseSurface) cashe_getBuildList_false=lst;
          if (onlyWhatCanBuildOnBaseSurface) cashe_getBuildList_true=lst;
        }
        // -- --
        
        return lst;
    }
    /**
     * Множество того, что могут построить эти строители (без учёта что нет ресурсов)
     * @param onlyWhatCanBuildOnBaseSurface только те, которые могут быть построены на поверхности базы (корабли, или сухопутные....)
     * @return список типов юнитов (не повторяются копии типов, как set)
     */
    
    /**
     * Множество того, что могут построить эти строители (без учёта что нет ресурсов)
     * @param builders список юнитов-строителей
     * @param onlyWhatCanBuildOnBaseSurface только те, которые могут быть построены на поверхности базы (корабли, или сухопутные....)
     * @return список типов юнитов (не повторяются копии типов, как set)
     */
    public HashSet<UnitDef> getBuildListFor(List<Unit> builders,boolean onlyWhatCanBuildOnBaseSurface) {
        // -- кэш --
        /*
        if (ENABLE_CASHING) {
          TODO
        }
        */
        // -- --
        HashSet<UnitDef> currentUnitTypesSet=new HashSet<UnitDef>(); // Какие типы юнитов уже просмотренны.
        HashSet<UnitDef> lst=new HashSet<UnitDef>(); // типы того что можно строить

       // проверка specificUnitEnabled (и хотья бы какой-то учёт лаборатории и уровней)
        for (Unit unit:builders) {
            UnitDef def=unit.getDef();
            if (def.isBuilder() && !currentUnitTypesSet.contains(def)) {
                List<UnitDef> unitLst;
                // TODO !!!
//                if (owner.modSpecific.specificUnitEnabled) unitLst=owner.modSpecific.getRealBuildList(unit);
//                    else
                unitLst=def.getBuildOptions();
                //owner.sendTextMsg(" special get unitLst size="+unitLst.size(), FieldBOT.MSG_DBG_SHORT);
                if (onlyWhatCanBuildOnBaseSurface) {
                    for (UnitDef canBuild:unitLst)
                     if (!lst.contains(canBuild)) if (canBuildOnBase(canBuild, true)) lst.add(canBuild);
                } else lst.addAll(unitLst);
                currentUnitTypesSet.add(def);
            }
        }
        
        // -- обновить кэш --
        /*
        if (ENABLE_CASHING) {
          TODO
        }
        */
        // -- --
        
        return lst;
    }
 
    // TODO GetBuildList_idleworkers!!!
    
    /**
     * Возможна ли постройка на базе или нет? (проверка на подводные/наземные...)
     * @param unitType тип юнита что строить надо
     * @param checkForFreeMetalSpots проверять наличие незанятых мест для шахт (влияет если это шахта и обычная карта)
     * @return 
     */
    public boolean canBuildOnBase(UnitDef unitType, boolean checkForFreeMetalSpots) {

        boolean needSpecialPoint=false;//stroyPlaning.needSpecialPointPosition(unitType);
        // -- кэш --
        if (ENABLE_CASHING) {
          if (!checkForFreeMetalSpots) {
            if (cashe_NOTcanBuildOnBase_false!=null) if (cashe_NOTcanBuildOnBase_false.contains(unitType)) return false;
            if (cashe_CANBuildOnBase_false!=null) if (cashe_CANBuildOnBase_false.contains(unitType)) return true;
            needSpecialPoint=stroyPlaning.needSpecialPointPosition(unitType); // !!!
          } else {
            needSpecialPoint=stroyPlaning.needSpecialPointPosition(unitType);
            if (!needSpecialPoint) { // кэшируется только то что не нуждается в специальных позициях.
                if (cashe_NOTcanBuildOnBase_true!=null) if (cashe_NOTcanBuildOnBase_true.contains(unitType)) return false;
                if (cashe_CANBuildOnBase_true!=null) if (cashe_CANBuildOnBase_true.contains(unitType)) return true;
            }
          }
        }
        // -- --
        
        boolean canBuild=stroyPlaning.canBuildOnBase(unitType, checkForFreeMetalSpots, BUILD_FACING);
       
        // -- обновить кэш --
        if (ENABLE_CASHING)
        {
            if (!needSpecialPoint) { // кэшируется всё, только без металла.
                if (canBuild) { // can build
                  if (!checkForFreeMetalSpots) {
                    if (cashe_CANBuildOnBase_false==null) cashe_CANBuildOnBase_false=new HashSet<UnitDef>();
                    cashe_CANBuildOnBase_false.add(unitType);
                  } else {
                    if (cashe_CANBuildOnBase_true==null) cashe_CANBuildOnBase_true=new HashSet<UnitDef>();
                    cashe_CANBuildOnBase_true.add(unitType);
                  }
                } else { // can't build
                  if (!checkForFreeMetalSpots) {
                    if (cashe_NOTcanBuildOnBase_false==null) cashe_NOTcanBuildOnBase_false=new HashSet<UnitDef>();
                    cashe_NOTcanBuildOnBase_false.add(unitType);
                  } else {
                    if (cashe_NOTcanBuildOnBase_true==null) cashe_NOTcanBuildOnBase_true=new HashSet<UnitDef>();
                    cashe_NOTcanBuildOnBase_true.add(unitType);
                  }
                }
            }

            //TODO !!!!!!! TEST ~!!!!!!!!!!!!!!!!!!!
            if (needSpecialPoint && !checkForFreeMetalSpots) { // кэшируется металл, если явно отключена проверка.
                if (canBuild) { // can build
                    if (cashe_CANBuildOnBase_false==null) cashe_CANBuildOnBase_false=new HashSet<UnitDef>();
                    cashe_CANBuildOnBase_false.add(unitType);
                } else { // can't build
                    if (cashe_NOTcanBuildOnBase_false==null) cashe_NOTcanBuildOnBase_false=new HashSet<UnitDef>();
                    cashe_NOTcanBuildOnBase_false.add(unitType);
                }
            }            
        }
        // -- --
        return canBuild;
    }
    
    
    /**
     * Построить одного юнита или здание на базе. Сейчас.
     * @param unitType что строить
     * @param executeNow построить СЕЙЧАС ЖЕ, бросив текущее строительство
     * @param buildPos можно принудительно задать место строительства. Если null - выбирается лучшее автоматом.
     * @return команда принята, или отклонена (нет строителей, или никто не умеет строить)
     */
    public boolean buildUnit(UnitDef unitType,boolean executeNow, AIFloat3 buildPos) {
        Unit mainBuilder=null;
        
        // 1) найти того, кто может строить
        if (buildPos==null) {
            for (Unit cons:idleCons) if (cons.getDef().getBuildOptions().contains(unitType) && !cons.isParalyzed()) {
                mainBuilder=cons;
                break;
            }
        } else {
            float nearL=0;// поиск ближайщего к точке
            for (Unit cons:idleCons) if (cons.getDef().getBuildOptions().contains(unitType) && !cons.isParalyzed()) {
                float l=MathPoints.getDistanceBetweenFlat(buildPos, cons.getPos())-cons.getDef().getBuildDistance();
                if (mainBuilder==null || l<nearL) {
                    mainBuilder=cons;
                    nearL=l;
                }
                //break;
            }
        }
        
        if ( mainBuilder==null && executeNow )
          for (Unit cons:workingCons) if (cons.getDef().getBuildOptions().contains(unitType) && !cons.isParalyzed()) {
            mainBuilder=cons;
            break;
          }
        
        if ( mainBuilder==null) // поиск в зданиях (фабриках)?
          for (Unit cons:building) if (cons.getDef().getBuildOptions().contains(unitType) && !cons.isParalyzed()) {
            mainBuilder=cons;
            break;
          }
        
        if (mainBuilder==null) {
            // Нет конструкторов, никто не умеет это строить!
            owner.sendTextMsg("Not have builder!" , FieldBOT.MSG_DBG_ALL);// DEBUG!
            return false;
        }
        
        // 2) заложить постройку
        // Определиться где строить сначало
        //AIFloat3 buildPos = 
        if (buildPos!=null) { // проверить, можно ли здесь это строить (вдруг задали невозможное)
            if (!owner.clb.getMap().isPossibleToBuildAt(unitType, buildPos, BUILD_FACING)) {
                // или рядом
                AIFloat3 pNear=owner.clb.getMap().findClosestBuildSite(unitType, buildPos, unitType.getRadius()*3, (int)MathPoints.getRadiusFlat(unitType), BUILD_FACING);
                if (MathPoints.isValidPoint(pNear)) buildPos=pNear; else buildPos=null; // где ставить? или нельзя здесь это строить.
            }
        }
        
        if (buildPos==null) buildPos=stroyPlaning.getRecomendetBuildPosition(unitType,mainBuilder);
        if (buildPos==null) {
            owner.sendTextMsg("Not position for build! "+unitType.getHumanName()+" ("+unitType.getName()+")!" , FieldBOT.MSG_DBG_SHORT);// DEBUG!
            return false;
        }

        // TODO заводы??? или здания которые неподвижны но строят в радиусе?...
        mainBuilder.build(unitType, buildPos, BUILD_FACING, (short) 0, DEFAULT_TIMEOUT);
        setAsWorking(mainBuilder);
        currentBaseTarget.unitPlanedToBuild(unitType, 700); // TODO какая задержка между началом строительства? timeout?...
        owner.sendTextMsg("Start build "+unitType.getHumanName()+" ("+unitType.getName()+") at " + buildPos , FieldBOT.MSG_DBG_ALL);// DEBUG!
        
        // 3) найти помошников для ускорения постройки
        // может помошников искать ТОЛЬКО после того как заложен??
        // TODO ненужно очень далёких искать... и почти достроенных, или если ресурсов нет всё равно.
        // TODO использовать optimalBPower
        if (mainBuilder.getDef().isAssistable()) { // TODO проверить TA, NOTA...
            ArrayList<Unit> assistPersonal=new ArrayList<Unit>();
            for (Unit aCons:idleCons) {
                UnitDef consDef=aCons.getDef();
                if (ModSpecification.isRealyAbleToAssist(consDef) && consDef.isAbleToMove()) assistPersonal.add(aCons); //  consDef.isAbleToAssist()
                // TODO !!! для нано!!! радиус проверить... или это в другом делается???
            }
            
            if (executeNow) for (Unit aCons:workingCons) {
                UnitDef consDef=aCons.getDef();
                if (ModSpecification.isRealyAbleToAssist(consDef) && !aCons.isParalyzed()) // было consDef.isAbleToAssist()
                  if (ModSpecification.isRealyAbleToMove(consDef)) { // было consDef.isAbleToMove()
                      assistPersonal.add(aCons); // может дойти
                  } else { 
                      if ( consDef.getBuildDistance() < MathPoints.getDistanceBetweenFlat(aCons.getPos(), buildPos)) {
                          assistPersonal.add(aCons); // может дотянуться строить
                      }
                  }
            }

            assistPersonal.remove(mainBuilder); // убрать главного строителя из этого списка

            // Учесть сколько нужно на одну постройку.
            final boolean USE_BUILDER_POWER_OPTIMIZATION=true; // TODO TEST!!!!
            
            float currentRes[][];
            if (owner.ecoStrategy.useOptimisticResourceCalc) currentRes= owner.avgEco.getSmartAVGResourceToArr();
            else currentRes= owner.avgEco.getAVGResourceToArr();
            // TODO test. было owner.avgEco.getAVGResourceToArr()
            
            float requiredBuildPower=owner.ecoStrategy.getOptimalBuildPowerFor(unitType, currentRes, 1.0f);
            float haveBuildPower=mainBuilder.getDef().getBuildSpeed();

            Iterator<Unit> itr1 = assistPersonal.iterator(); // Цикл...
            while (itr1.hasNext() && (!USE_BUILDER_POWER_OPTIMIZATION || (requiredBuildPower*1.12>haveBuildPower)) ) { // для всех, пока не будет достаточна скорость постройки +12% лишних.
                Unit aCons = itr1.next();
                //aCons.guard(mainBuilder, (short)0, 300); // !!!
                UnitDef consDef=aCons.getDef();
                if (consDef.getBuildOptions().contains(unitType)) { // если может это строить
                    aCons.build(unitType, buildPos, BUILD_FACING, (short) 0, DEFAULT_TIMEOUT); // построить то же на этом месте.
                    haveBuildPower+=aCons.getDef().getBuildSpeed();
                } else { // подойти поближе к месту постройки.
                    
                    if (ModSpecification.isRealyAbleToMove(consDef)) { // consDef.isAbleToMove() && (consDef.getBuildDistance() < owner.getDistanceBetweenFlat(aCons.getPos(), buildPos))) { // подойти поближе
                        AIFloat3 nearPoint=MathPoints.getNearBetweenPoint(aCons.getPos(),buildPos,consDef.getBuildDistance());
                        aCons.moveTo(nearPoint, (short) 0, DEFAULT_TIMEOUT);
                    } else {
                        // если нано башня...
                    }
                    
                    haveBuildPower+=aCons.getDef().getBuildSpeed() / 3; // !!! Учесть 1/3 от энергии постройки

                    /*if (consDef.isAbleToMove() && consDef.isAbleToGuard()) {
                        aCons.guard(mainBuilder, (short) 0, DEFAULT_TIMEOUT); // !!!!!!!! TEST !!!!!!!!!
                        // плохо для строительства в заводах.
                    }*/
                    itr1.remove(); // не делать его как занятого
                }
            }
            //setAsWorking(aCons)
            idleCons.removeAll(assistPersonal);
            for (Unit u:assistPersonal) if (!workingCons.contains(u)) workingCons.add(u); // TODO ускорить как множество set...
        }
        return true;
    }
    // -- - - --
    
    
    /**
     * Return build power of all builders.
     * @param onlyIdle only from idle
     * @param noFactory if true then do not check factory as builders
     * @return BuildPower
     */
    public float getBuildPower(boolean onlyIdle,boolean noFactory) { // TODO boolean onlyWhoCanAssist ... или для одного
        //FIXME MAKE CASHE!!!!!!!!!!!!!
        
        float p=0.0f;
        for (Unit u:idleCons) if (!u.isParalyzed() && (!noFactory || !ModSpecification.isStationarFactory(u.getDef()))) p+= u.getDef().getBuildSpeed();
        if (!onlyIdle) for (Unit u:workingCons) if (!u.isParalyzed() && (!noFactory || !ModSpecification.isStationarFactory(u.getDef()))) p+= u.getDef().getBuildSpeed();
        // TODO noAssistable(), isAbletoAssist ?
        return p;
    }
    
    
    protected boolean sendIdleConsToAssist()
    {
        if (!buildLst.isEmpty()) {// Finish buildings
            boolean someAssistSending=false;
            Iterator<Unit> itr1 = idleCons.iterator(); // Cicle
            while (itr1.hasNext()) {
               Unit u = itr1.next();
                if (ModSpecification.isRealyAbleToAssist(u.getDef()) && !u.isParalyzed()) { // было .isAbleToAssist()
                    Unit bestToAssist=null; float minDist=1.0f;
                    for (Unit uBld:buildLst) {
                        float lt=MathPoints.getDistanceBetweenFlat(uBld.getPos(), u.getPos()); // ближайщый
                        lt -= u.getDef().getBuildDistance(); // сократить на расстояние строителя.
                        if (lt>=0) { // need walk
                            if (ModSpecification.isRealyAbleToMove(u.getDef())) { // если может ходить
                                lt = lt/u.getMaxSpeed(); // время, за которое он подберётся на расстояние постройки.
                                //lt = getAvgBuildTime(uBld.getDef(),u.getDef().getBuildSpeed()) / lt; // TODO ЭТО НЕ ПРАВИЛЬНО. коэффициент нужности ускорения.
                                // TODO check lost build time if (lt>1.8f*getAvgBuildTime(blbUnit.getDef(),u.getDef().getBuildSpeed())) {
                                if (minDist<lt || bestToAssist==null) {
                                    minDist=lt;
                                    bestToAssist=uBld;
                                }
                            }
                        } else { // уже в зоне
                            bestToAssist=uBld;
                            minDist=0;
                            break;
                        }
                    }
                    if (bestToAssist!=null) {
                        u.repair(bestToAssist, (short)0, DEFAULT_TIMEOUT); // it is work
                        // TODO do is need isAbletoRepair?
                        workingCons.add(u); // FIXME add message to refresh cashe, if cash will be to do.
                        itr1.remove(); // Set as no idle.
                        someAssistSending=true;
                    }
                }
            }
            return someAssistSending;
        } else return false; 
    }
    
    // Command from bot
   @Override
    public int update(int frame)
    {
        // TODO optimized CPU usage  if (frame % 8 == 0)

        
        if (frame % 8 == 1) currentBaseTarget.update(frame);// ... not often?
        
      if (frame % 8 == 0) {
          
        // Если есть свободные рабочие
        if (!idleCons.isEmpty()) {

            sendIdleConsToAssist(); // TO do no finish build
            
            if (!idleCons.isEmpty() && (owner.ecoStrategy.makeEco || !currentBaseTarget.isEmpty())) { // Построить
                List<UnitDef> buildVariants = new ArrayList<UnitDef>(getBuildList(true));
                if (buildVariants.isEmpty()) owner.sendTextMsg(" buildVariants = empty!!!!!!!!" , FieldBOT.MSG_DBG_SHORT); // DEBUG !!!!
                
                AIFloat3 buildPosOn=null; // принудительная позиция строительства. null - по умолчанию.
                
                boolean makeNow=false; // сделать сейчас-же! (даже отвлечь от других работ строителей)
                boolean estPrinuditPrikaz=false;
                if (!currentBaseTarget.isEmpty()) { // Выполнить принудительный приказ
                    List<UnitDef> canBuildPrikaz=new ArrayList<UnitDef>(currentBaseTarget.currentBuildLst);
                    canBuildPrikaz.retainAll(buildVariants); // было for (UnitDef def:currentBaseTarget.currentBuildLst) if (buildVariants.contains(def)) canBuildPrikaz.add(def);
                    if (!canBuildPrikaz.isEmpty()) {
                        // Дополнительный отбор для паралельного строительства.
                        // Выбрать то, что можно строить свободными строителями.
                        //owner.sendTextMsg("Test: canBuildPrikaz size="+canBuildPrikaz.size(), FieldBOT.MSG_ERR);
                        HashSet<UnitDef> canBuildNow=getBuildListFor(idleCons, true);
                        //owner.sendTextMsg("Test: before size canBuildNow= "+canBuildNow.size(), FieldBOT.MSG_ERR);
                        canBuildPrikaz.retainAll(canBuildNow);
                        //owner.sendTextMsg("Test: canBuildPrikaz size="+canBuildPrikaz.size(), FieldBOT.MSG_ERR);
                        // TODO TEST !!!
                    }
                    if (!canBuildPrikaz.isEmpty()) { // если есть что выполнять сейчас
                        buildVariants=canBuildPrikaz;
                        estPrinuditPrikaz=true;
                        makeNow=currentBaseTarget.commandNow;
                    } else { // если нет чего сейчас закладывать строить
                        if (currentBaseTarget.needPrecissionRes) {
                            buildVariants=canBuildPrikaz; // присвоение пустого list.
                            if (currentBaseTarget.isEmpty()) owner.sendTextMsg("ERROR! currentBaseTarget.isEmpty()==true but currentBaseTarget.needPrecissionRes==true!", FieldBOT.MSG_ERR);
                        }
                    }
                }

                float buildPower=getBuildPower(false,true); // TODO use owner.ecoStrategy.useOptimisticResourceCalc
                UnitDef makeIt = owner.ecoStrategy.getOptimalBuildingForNow( buildPower ,buildVariants, null); // чего делать, с учётом текущих нужд в ресурсах
                if (makeIt==null && estPrinuditPrikaz && !buildVariants.isEmpty()) makeIt=buildVariants.get(0); // для исполнения принудительных приказов.
                if (estPrinuditPrikaz && !buildVariants.isEmpty()) {
                    // получить координаты, если есть
                    int pI=currentBaseTarget.currentBuildLst.indexOf(makeIt);
                    if (pI!=-1) {
                        buildPosOn=currentBaseTarget.currentBuildPos.get(pI);
                    } else owner.sendTextMsg("can't assign build_prikaz variant for find position!", FieldBOT.MSG_ERR);// DEBUG!
                }

                if (makeIt!=null) {
                    owner.sendTextMsg("build target:"+makeIt.getHumanName() , FieldBOT.MSG_DBG_SHORT);// DEBUG!
                    buildUnit(makeIt, makeNow, buildPosOn);
                } else owner.sendTextMsg("build target NULL" , FieldBOT.MSG_DBG_SHORT);// DEBUG!
            }

            // if not work, then moving walking, for save factory from stay work blocking.
            if (!idleCons.isEmpty()) for (Unit u:idleCons) if (u.getCurrentCommands().isEmpty() && !u.isParalyzed()) {
                if (!ModSpecification.isStationarFactory(u.getDef())) {
                    if (ModSpecification.isRealyAbleToMove(u.getDef())) {
                        AIFloat3 pp;
                        if (MathPoints.getDistanceBetweenFlat(u.getPos(), center)>radius) pp=MathPoints.getRandomPointInCircle(center, radius);
                        else pp=MathPoints.getNearRandomPoint(u); // patrol point
                        u.moveTo(pp, (short)0, DEFAULT_TIMEOUT);
                    }
                    else if (u.getDef().isAbleToPatrol()) u.patrolTo(MathPoints.getNearRandomPoint(u), (short)0, DEFAULT_TIMEOUT); // for nano tower
                }
            }
        }
      }
        
    // --- Find idle unit ---
      if (frame % 120 == 2) { // tick N= 8*15=120 + 2
          // TODO переделать на Iterator
        for (int i=workingCons.size()-1; i>=0; i--) if (workingCons.get(i).getCurrentCommands().size()<1) {
            owner.sendTextMsg("Found no working unit (idle) in working list! "+workingCons.get(i).getDef().getName() , FieldBOT.MSG_DBG_SHORT);// DEBUG
            setAsIdle(workingCons.get(i));
        } 
      }
    // --------
      
      // --- protect base ---
      if (frame % 100 == 3) { // TODO test it!
          List<Unit> enemyInBase = owner.clb.getEnemyUnitsIn(center, radius*1.01f);
          if (!enemyInBase.isEmpty()) {
              lastAgressorPoint=enemyInBase.get(0).getPos();
              // TODO near, or average group center
          }
      }
      if (frame % 10 == 3 && lastAgressorPoint!=null) {
          if (!army.isEmpty()) for (Unit solder:army)
          { // Send army to fight to agressor position
              // TODO check solder.getCurrentCommands().isEmpty()
              UnitDef soldDef=solder.getDef();
              if (soldDef.isAbleToAttack() && soldDef.isAbleToFight())
                  solder.fight(lastAgressorPoint, (short)0, DEFAULT_TIMEOUT);
          }
          lastAgressorPoint=null; // TODO Register this point where atack on unit...
      }
    // --------
      
        return 0;
    }
    
/**
 * При появлении свободных бездействующих юнитов отсылает в группу бездельников
 * @param unit
 * @return 
 */
public boolean onFreeUnitFound(Unit unit) {
    if (contains(unit)) {
        if (workingCons.contains(unit)) setAsIdle(unit); // !!!
        return true;
    } else {
        // Не принадлежит базе
        return false;
    }
}
    
@Override
public int unitCreated(Unit unit, Unit builder) {
    if ( builder!=null && contains(builder) ) {
        if (unit.isBeingBuilt()) {
            if (builder.getDef().isAssistable()) buildLst.add(unit); // добавить в список которые можно достроить
            else buildLst_noAssistable.add(unit); // добавить в список которые нельзя помогать достраивать
        } else addUnit(unit);
        currentBaseTarget.unitCreated(unit, builder);
    } // TODO если юнит сделан строителем базы, то он кому принадлежит?
    // builder==null???
    //if (unit.getPos())) {
    // TODO distance...
    //}
    return 0;
}

@Override
public int unitFinished(Unit unit) {
    if (buildLst.contains(unit)) {
        buildLst.remove(unit);
        addUnit(unit);
        currentBaseTarget.unitFinished(unit);
    }
    if (buildLst_noAssistable.contains(unit)) { // список недостраиваемых юнитов (нельзя помочь строить)
        buildLst_noAssistable.remove(unit);
        addUnit(unit);
        currentBaseTarget.unitFinished(unit);
    }
    // TODO если не был в списках построек базы ????
    return 0; // signaling: OK
}

@Override
public int unitIdle(Unit unit) {
    onFreeUnitFound(unit);
    return 0; // signaling: OK
}

@Override
public int unitMoveFailed(Unit unit) {
    if (unit.getCurrentCommands().isEmpty()) onFreeUnitFound(unit); // !!!!!!!!!!!! TEST
    return 0; // signaling: OK
}

/*
//@Override
public int unitDamaged(Unit unit, Unit attacker, float damage, AIFloat3 dir, WeaponDef weaponDef, boolean paralyzed) {
    return 0; // signaling: OK
}
//*/

@Override
public boolean unitDestroyed(Unit unit, Unit attacker) {
    if (contains(unit)) {
        currentBaseTarget.unitDestroyed(unit, attacker);
        return removeUnit(unit);
    }
    
    return false;
}

@Override
public boolean unitGiven(Unit unit, int oldTeamId, int newTeamId) {
    if (oldTeamId==owner.teamId && newTeamId!=owner.teamId) {
        if (buildLst.contains(unit)) buildLst.remove(unit); // !!!
        return removeUnit(unit);
    } else {
        return contains(unit); // !!!!!! ???
    }
    
    //return false;
}

@Override
public int unitCaptured(Unit unit, int oldTeamId, int newTeamId) {
    if (oldTeamId==owner.teamId && newTeamId!=owner.teamId) {
        //if (buildLst.contains(unit)) buildLst.remove(unit); // !!!
        removeUnit(unit);
    }
    return 0; // signaling: OK
}
/*
//@Override
public int enemyEnterLOS(Unit enemy) {
    return 0; // signaling: OK
}

//@Override
public int enemyLeaveLOS(Unit enemy) {
    return 0; // signaling: OK
}

//@Override
public int enemyEnterRadar(Unit enemy) {
    return 0; // signaling: OK
}

//@Override
public int enemyLeaveRadar(Unit enemy) {
    return 0; // signaling: OK
}

//@Override
public int enemyDamaged(Unit enemy, Unit attacker, float damage, AIFloat3 dir, WeaponDef weaponDef, boolean paralyzed) {
    return 0; // signaling: OK
}

//@Override
public int enemyDestroyed(Unit enemy, Unit attacker) {
    return 0; // signaling: OK
}

//@Override
public int weaponFired(Unit unit, WeaponDef weaponDef) {
    return 0; // signaling: OK
}

//@Override
public int playerCommand(java.util.List<Unit> units, int commandTopicId, int playerId) {
    return 0; // signaling: OK
}
//*/

@Override
public int commandFinished(Unit unit, int commandId, int commandTopicId) {
    /*if (unit.getDef().getName().equals("armcom")) {
        commander = unit;
    }*/
    if (unit.getCurrentCommands().isEmpty()) onFreeUnitFound(unit);
    //else не все команды выполнены.
    return 0; // signaling: OK
}

/*
//@Override
public int seismicPing(AIFloat3 pos, float strength) {
    return 0; // signaling: OK
}

//@Override
public int load(String file) {
    return 0; // signaling: OK
}

//@Override
public int save(String file) {
    return 0; // signaling: OK
}

//@Override
public int enemyCreated(Unit enemy) {
    return 0; // signaling: OK
}

//@Override
public int enemyFinished(Unit enemy) {
    return 0; // signaling: OK
}
//*/


   @Override
   public String toString() {
    String t="Base at "+center+" radius "+radius+ "contains "+getAllUnits(true).size()+" units. ";
    t+="Idle cons="+idleCons.size()+" working="+workingCons.size()+" building not finish="+buildLst.size()+".";
    t+=" Base square using: "+stroyPlaning.full()*100+"%";
    if (workingCons.size()>0) {
        t+=" { ";
        for (Unit u:workingCons) {
            t+="("+u.getDef().getName()+", "+u.getCurrentCommands().size()+":";
            for (Command cm:u.getCurrentCommands()) {
                t+=" c="+cm.getCommandId()+" timeout="+cm.getTimeOut()+";";
            }
            t+=") ";
        }
             
        t+=" }";
    }
    t+="\nBase target: "+currentBaseTarget.toString();
    return t;
}

}
