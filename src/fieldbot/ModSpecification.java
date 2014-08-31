/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package fieldbot;

import com.springrts.ai.oo.clb.Command;
import com.springrts.ai.oo.clb.CommandDescription;
import com.springrts.ai.oo.clb.Resource;
import com.springrts.ai.oo.clb.Unit;
import com.springrts.ai.oo.clb.UnitDef;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Особенности модов.
 * @author user2
 */
public class ModSpecification {
    //private String ecoName[]; было
    //private float eco_res[][];

    //TreeMap<String,float[]> treeMap_Eco; // параметры банок
    private final HashMap<String,float[]> treeMap_Eco; // параметры банок !!!!!!!! TEST!!!!!!!
    // TODO можно ли использовать HashMap для этого? !!!! см. хэш Стринг...
    
    private final FieldBOT owner;
    private final List<UnitDef> allUnitDefs; // для ассоциации с Morph...
    
    private final HashMap<UnitDef,Integer> specialTLevelRequired;//!!! Для TA и RD, смотри specificUnitEnabled
    
    /**
     * This mod (for Tech Anihilation) have special unit enabled by tech level (laboratory).
     * If true then for real unit list required use functions: <code>getRealBuildList()</code> and (TODO).
     */
    public final boolean specificUnitEnabled;
    /**
     * Afther corrent/income>=metal_maker_workZone then metal makers will be work
     */
    public final float metal_maker_workZone;
    /**
     * From resource ID make resource_to, if -1 then mod no have maker
     */
    public final int maker_resource_from;
    /**
     * From resource ID make resource_to, if -1 then mod no have maker
     */
    public final int maker_resource_to;
    
    /**
     * Существуют MetalMaker встроенные (EVO RTS) (через isNativeMetalMaker, getNativeConversionKMetalMaker )
     */
    public final boolean exist_MetalMaker_native;
    /**
     * Существуют MetalMaker скриптовые (BA, TA, NOTA) (через getResourceConversionFor, isMetalMaker, getConversionKMetalMaker)
     */
    public final boolean exist_MetalMaker_lua;
    
    public ModSpecification(FieldBOT owner) {
        this.owner=owner;
        allUnitDefs=owner.clb.getUnitDefs(); // !!! получение большого списка.
        
        String modName = owner.clb.getMod().getShortName();
        
        //TODO в XML...
        
        // Tech Anihilation //MOD short name:TA
        // Robot Defence MOD short name: RD
        
        specificUnitEnabled = modName.equals("TA") || modName.equals("RD"); // TODO specificUnitEnabled !
        
        
        if (modName.equals("EvoRTS")) {
            metal_maker_workZone=0.012f;
        } else {
            metal_maker_workZone=0.697f;
        }
        maker_resource_from=1; // from energy
        maker_resource_to=0; // to metal make
        
        treeMap_Eco=new HashMap<String, float[]>();
        
        // Параметры см. в https://github.com/n3wm1nd/TA/blob/master/luarules/configs/maker_defs.lua
        if (modName.equals("TA")||modName.equals("RD") || modName.equals("BA")) { // TODO BA отдельно вынести
            // ARM
            treeMap_Eco.put("ametalmakerlvl1", new float[] {  2.8f, -200f } ); // T1.5 Metal Maker
            treeMap_Eco.put("ametalmakerlvl2", new float[] { 288.0f, -16000f } ); // T3 Metal Maker
            treeMap_Eco.put("armamaker", new float[] { 2.8f, -200f } ); // Advanced Metal Maker (T1,5)
            treeMap_Eco.put("armckmakr", new float[] { 6.0f, -400f } ); // Cloakable Metal Maker
            treeMap_Eco.put("armfmkr", new float[] { 1.3f, -100f } ); // Floating Metal Maker (T1)
            treeMap_Eco.put("armmakr", new float[] { 0.6f, -50f } ); // Metal Maker (T1)
            treeMap_Eco.put("armmmkr", new float[] { 12.8f,-800f } ); // Moho Metal Maker
            treeMap_Eco.put("armuwmmm", new float[] { 17.0f, -1000f } );// Underwater Moho Metal Maker
            // +armgen 1.0/100

            // CORE
            treeMap_Eco.put("cmetalmakerlvl1", new float[] {  3.584f, -256f } ); // T1.5 Metal Maker
            treeMap_Eco.put("cmetalmakerlvl2", new float[] { 368.64f, -20480f } ); // T3 Metal Maker
            treeMap_Eco.put("coramaker", new float[] { 3.584f, -256f } ); // Advanced Metal Maker
            treeMap_Eco.put("corfmkr", new float[] {  1.664f, -128f } ); // Floating Metal Maker
            treeMap_Eco.put("corhmakr", new float[] { 44.8512f, -3072f } ); // Hardened Metal Maker
            treeMap_Eco.put("cormakr", new float[] { 0.768f, -64f } ); // Metal Maker (T1)
            treeMap_Eco.put("cormmkr", new float[] { 16.384f, -1024f } ); // Moho Metal Maker
            treeMap_Eco.put("coruwmmm", new float[] { 21.76f, -1280f } ); // Underwater Moho Metal Maker
            // +corgen 1.0/100

            // TLL 
            treeMap_Eco.put("tllammaker", new float[] { 14.2f, -1000f } ); // Moho Metal Maker
            treeMap_Eco.put("tllmm", new float[] { 1.08f, -100f } ); // Metal Maker
            treeMap_Eco.put("tllwmmohoconv", new float[] { 14.7f, -1000f } ); // Floating Moho Metal Maker
            treeMap_Eco.put("tllwmconv", new float[] { 1.16f, -100f } ); // Metal Producer (floating)
        }

        // NOTA
        if (modName.equals("NOTA")) {
            treeMap_Eco.put("armmakr", new float[] { 0.6f, -60f } ); // Metal Maker (T1)
            //TODO ecoName[6]="armmmkr"; eco_res[6][E]=-800f; eco_res[6][M]=8.0f?; // Moho Metal Maker
            treeMap_Eco.put("cormakr", new float[] { 0.6f, -60f } ); // Metal Maker (T1)
            treeMap_Eco.put("cormmkr", new float[] { 8.0f, -800f } ); // Moho Metal Maker (T2)
        }
        
        // EvoRTS
        if (modName.equals("EvoRTS")) {
            treeMap_Eco.put("emetalextractor", new float[] { 0.5f, 0.0f } ); // Metal Extractor
            treeMap_Eco.put("euwmetalextractor", new float[] { 0.5f, 0.0f } ); // Underwater Metal Extractor
        }

        // ZeroK
        if (modName.equals("ZK")) {
            treeMap_Eco.put("cormex", new float[] { 1.0f, 0.0f } ); // Metal Extractor
            // TODO !!!!!!!!!!!!!!!!!!! extractor metal multiplier = sqrt(1+energy/4)) 
        }

        exist_MetalMaker_lua=treeMap_Eco.isEmpty();
        exist_MetalMaker_native= modName.equals("EvoRTS") || 
                !(modName.equals("NOTA")||modName.equals("BA")||modName.equals("TA")||modName.equals("RD"));
        
        
        if (specificUnitEnabled) {
            specialTLevelRequired=new HashMap<UnitDef, Integer>();
            // For Tech Anililation and Robot Defence
            UnitDef def;
            def=owner.clb.getUnitDefByName("armmarv");
            if (def!=null) specialTLevelRequired.put(def, -1);
            def=owner.clb.getUnitDefByName("corfred");
            if (def!=null) specialTLevelRequired.put(def, -1);
        } else {
            specialTLevelRequired=null;
        }
        
    }
    
    /**
     * Сведения о Metal Maker
     * @param defName название банки
     * @return что вырабатывает, или null если нет спецификаций.
     */
    public float[] getResourceConversionFor(String defName) {
        return treeMap_Eco.get(defName);
        /* было
        for (int i=0;i<ecoName.length;i++) {
            if (defName.contains(ecoName[i])) return eco_res[i];
        }
        //float ret0[]=new float[2];
        //ret0[0]=0.0f; ret0[1]=0.0f;
        return null;//ret0;
        */
    }
    
    /**
     * Если это специальный MMaker управляемый скриптами, то можно использовать getConversionKMetalMaker()
     * @param defName
     * @return это ммакер (если это "нативный" метал макер (resourceMake) то не возвращает истинну)
     */
    public boolean isMetalMaker(String defName) {
        return treeMap_Eco.containsKey(defName);
    }
    /**
     * Возвращает коэффициент конверсии для "LUA" метал мейкера
     * @param defName
     * @return коэффициент, чем > тем лучше. Или Float.POSITIVE_INFINITY.
     */
    public float getConversionKMetalMaker(String defName) {
        float[] val=treeMap_Eco.get(defName);
        if (val==null) return 1.0f;
        if (val[1]==0) return Float.POSITIVE_INFINITY;// !!!!
        return val[0]/val[1]; // TODO check!!! зависит от порядка...
    }
    
    /**
     * Определяет, является ли этот юнит "нативным" конвертером ресурсов
     * @param def тип юнита
     * @return true, если является нативным metal maker, или energy maker :) .
     */
    public boolean isNativeMetalMaker(UnitDef def) {
        // TODO кэшировать список
        float min,max;
        boolean existMakeRes=false;
        min=max=0.0f;
        for (Resource r:owner.clb.getResources()) {
            float x=owner.getUnitResoureProduct(def, r);
            if (def.getMakesResource(r)!=0.0f) existMakeRes=true;
            min=Math.min(min,x);
            max=Math.max(max,x);
        }
        return (min<0.0f)&&(max>0.0f)&& existMakeRes;
    }
    /**
     * Возвращает коэффициент конверсии для "нативного" метал мейкера
     * Вычисляется как: Sum(вырабатываемые)/Sum(потребляемые).
     * @param def тип юнита
     * @return коэффициэнт >0.0, чем больше тем эффективнее, или Float.POSITIVE_INFINITY.
     */
    public float getNativeConversionKMetalMaker(UnitDef def) {
        float sMin,sMax;
        sMin=sMax=0.0f;
        for (Resource r:owner.clb.getResources()) {
            float x=owner.getUnitResoureProduct(def, r);
            if (x<0.0f) sMin+=x;
            else sMax+=x;
        }
        if (sMin<0.0) return sMax/(-sMin);
        else return Float.POSITIVE_INFINITY;
    }

// работа с TechLevel и особенностями модов, например TA - research center

    /**
     * Для TA/RD некоторые юниты нельзя строить, пока нет лаборатории
     * @param def
     * @return true если нужна проверка
     */
    public boolean requiredSpecialTechLevel(UnitDef def) {
        return (specialTLevelRequired==null) || (specialTLevelRequired.get(def)==null);
    }
    public int getRequiredTechLevel(UnitDef def) {
        return specialTLevelRequired.get(def);
    }
    public boolean haveTechLevelFor(UnitDef def) {
        if (specialTLevelRequired==null) return true;
        Integer reqTL=specialTLevelRequired.get(def);
        if (reqTL==null) return true;
        return owner.techLevels.get(reqTL)!=null;
        // было:
        //return !requiredSpecialTechLevel(def) ||
        //        (owner.techLevels.get(getRequiredTechLevel(def))!=null);
    }
    /**
     * Ищет уровень
     * @param def
     * @param techLvl список имеющихся уровней
     * @return может строить или нет (нехватка тех уровней)
     */
    public boolean haveTechLevelFor(UnitDef def, HashSet<Integer> techLvl) { //было int haveLevels[]
        if (specialTLevelRequired==null) return true;
        Integer reqTL=specialTLevelRequired.get(def);
        if (reqTL==null) return true;
        return techLvl.contains(reqTL);
        //return Arrays.binarySearch(haveLevels, reqTL)!=-1;
        //было return !requiredSpecialTechLevel(def) ||
        //        Arrays.binarySearch(haveLevels, getRequiredTechLevel(def))!=-1;
    }
    
    /**
     * Убирает всех юнитов, которых нельзя построить из-за требования тех. уровня (лаборатории в TA)
     * @param lstDef
     * @return список, который может строить при текущих уровнях
     */
    public boolean removeUnitWhenCantBuildWithTeclLevel(Collection<UnitDef> lstDef) {
        Iterator<UnitDef> itr1 = lstDef.iterator(); // Цикл...
        boolean modifed=false;
        while (itr1.hasNext()) {
            UnitDef def = itr1.next();
            if (!haveTechLevelFor(def)) {
                itr1.remove(); // убрать из списка
                modifed=true;
            }
        }
        return modifed;
    }
    
    /**
     * Убирает всех юнитов, которых нельзя построить из-за требования тех. уровня c доп. параметрами (лаборатории в TA)
     * @param lstDef
     * @param techLvl2 дополнительные тех. уровни, которые "добавляются" к owner.techLevels
     * @return список, который может строить при текущих уровнях или при доп. уровнях (techLvl2)
     */
    public boolean removeUnitWhenCantBuildWithTeclLevel(Collection<UnitDef> lstDef, HashSet<Integer> techLvl2) {
        Iterator<UnitDef> itr1 = lstDef.iterator(); // Цикл...
        boolean modifed=false;
        while (itr1.hasNext()) {
            UnitDef def = itr1.next();
            if (!haveTechLevelFor(def) && !haveTechLevelFor(def, techLvl2)) {
                itr1.remove(); // убрать из списка
                modifed=true;
            }
        }
        return modifed;
    }
    
    /**
     * Выборка юнитов нужного уровня
     * @param defs список для выбора
     * @param level требуемый тех уровень (равно)
     * @return отобранный список с заданным тех. уровнем, может быть пустым.
     */
    public ArrayList<UnitDef> selectDefByTechLevel(ArrayList<UnitDef> defs,int level)
    {
        ArrayList<UnitDef> selectedLst=new ArrayList<UnitDef>();
        for (UnitDef def:defs) if (def.getTechLevel()==level) selectedLst.add(def);
        return selectedLst;
    }
    
    /**
     * Альтернативный метод получения списка возможных построек юнитов. Для особых модов, например TA.
     * TODO НЕ РАБОТАЕТ правильно!!!!
     * @param uBuilder
     * @return список того, что может строить этот юнит
     */
    public ArrayList<UnitDef> getRealBuildList(Unit uBuilder) {
        // FIXME TODO NOT WORK!!!
        ArrayList<UnitDef> outLst=new ArrayList<UnitDef>(); // TODO test!!!
        
        for (UnitDef def:uBuilder.getDef().getBuildOptions())
          if (haveTechLevelFor(def)) outLst.add(def);
        // FIXME Buggy, to test only! rewrite!
        /*
        for (CommandDescription cmd :uBuilder.getSupportedCommands())
          if (!cmd.isDisabled() && cmd.getId()<0) // Если включено и это строительство юнита
            outLst.add(allUnitDefs.get(-cmd.getId()));
        */
        
        return outLst;
    }
    
// ......... 
protected static final int CMD_MORPH_STOP = 32410;
protected static final int CMD_MORPH = 31410;

    public static boolean isMorphCMD(CommandDescription cmd) {
        return (cmd.getId() >= CMD_MORPH) && (cmd.getId() < CMD_MORPH_STOP);
    }
    public static boolean isMorphCMD(Command cmd) {
        // FIXME not work! getCommandId or getId
        return (cmd.getCommandId()>= CMD_MORPH) && (cmd.getCommandId() < CMD_MORPH_STOP);
    }

    /**
     * Возвращает список в чего можно улучшить (morph) юнита.
     * @param unit
     * @param showDisabledToo
     * @return 
     */
    public static List<CommandDescription> getMorphCmdList(Unit unit,boolean showDisabledToo) { // isAbleToMorph
        List<CommandDescription> cmbList = unit.getSupportedCommands();
        ArrayList<CommandDescription> cmdMorphing=new ArrayList<CommandDescription>();
        for (CommandDescription cmd : cmbList) {
        //sendTextMsg(" command name: " + cmd.getName() + " ToolTip " + cmd.getToolTip() + " ID=" + cmd.getId()+ "\t supportedCommand="+cmd.getSupportedCommandId()+" isDisabled:"+cmd.isDisabled() +" isShowUnique:"+cmd.isShowUnique());
            if (isMorphCMD(cmd) && (showDisabledToo || !cmd.isDisabled())) cmdMorphing.add(cmd);
            //unit.executeCustomCommand(cmd.getId(), new ArrayList<Float>(), (short)0, Integer.MAX_VALUE);
        }
        return cmdMorphing;
    }
    /**
     * Возвращает команду отмены улучшения(morph) юнита
     * @param unit юнит
     * @param showDisabledToo
     * @return команда или null
     */
    public static CommandDescription getStopMorphCmd(Unit unit,boolean showDisabledToo) { // isAbleToMorph
        List<CommandDescription> cmbList = unit.getSupportedCommands();
        for (CommandDescription cmd : cmbList) {
        //sendTextMsg(" command name: " + cmd.getName() + " ToolTip " + cmd.getToolTip() + " ID=" + cmd.getId()+ "\t supportedCommand="+cmd.getSupportedCommandId()+" isDisabled:"+cmd.isDisabled() +" isShowUnique:"+cmd.isShowUnique());
            if ((cmd.getId()==CMD_MORPH_STOP) && (showDisabledToo || !cmd.isDisabled())) {
                return cmd;
            }
        }
        return null;
    }
    
    /**
     * Возвращает описание типа юнита после Morph
     * @param morphCmd
     * @return 
     */
    public UnitDef getUnitDefForMprph(CommandDescription morphCmd) {
        /*
        final int MORPH_UNITID=31368; // отнять от номера команды morph... 31000?
        int mUnitDefID=morphCmd.getId()-MORPH_UNITID;//!!!!;
        */
        int mUnitDefID;
        switch (morphCmd.getId()) {
            case 31456: mUnitDefID=90; break; // ARM Battle Commander
            case 31457: mUnitDefID=89; break; // ARM Ecoing Commander
            case 31430: mUnitDefID=408; break; // Combat Commander
            case 31431: mUnitDefID=407; break; // CORE Ecoing Commander
            case 31433: mUnitDefID=734; break; // TLL Battle Commander
            default: return null;
        }
        
//        owner.sendTextMsg("MORPHING CMD ID="+morphCmd.getId()+" supported ID="+morphCmd.getSupportedCommandId(), FieldBOT.MSG_DBG_ALL);
        UnitDef def=allUnitDefs.get(mUnitDefID-1);
        // FIXME получет не то!
        if (def.getUnitDefId()==mUnitDefID) {
            owner.sendTextMsg("Def detect №"+mUnitDefID , FieldBOT.MSG_DBG_SHORT);
//             for (int i=-4;i<4;i++) owner.sendTextMsg("Def №"+(mUnitDefID+i)+" is "+allUnitDefs.get(mUnitDefID+i-1).getHumanName() , FieldBOT.MSG_DBG_SHORT); // DEBUG!!!
        } else {
            owner.sendTextMsg("Def detect ERROR: "+mUnitDefID+" <> found "+def.getUnitDefId() , FieldBOT.MSG_ERR);
            def=null;
        }
        return def;
    }
    
    /**
     * Разбирает строку вида "абв key: value abc"
     * @param source
     * @param key
     * @return 
     */
    private float parsingValue(String source, String key,float defaultVal) {
        float val;//=defaultVal;
        String keyS=key+" ";
        int p1=source.indexOf(keyS);
        if (p1==-1) {
            keyS=key;
            p1=source.indexOf(keyS);
        }
        if (p1!=-1) {
            p1+=keyS.length();
            String intStr=source.substring(p1); // !!!
            try {
                val=Float.parseFloat(intStr);
            } catch (NumberFormatException e) {
                owner.sendTextMsg(" parsing ERROR: ["+intStr+"] to float exception: "+e.toString() , FieldBOT.MSG_ERR);
                val=defaultVal;
            }
        } else {
            owner.sendTextMsg(" parsing ERROR: symbol ["+key+"] not found on ["+source+"]." , FieldBOT.MSG_ERR);
            val=defaultVal;
        }
        // TODO parsingValue test
        return val;
    }
    
    public float getTimeForMprph(CommandDescription morphCmd) {
        float t=0.0f;//!!!!!!!!!!
        switch (morphCmd.getId()) {
            case 31456: t=30; break; // ARM Battle Commander
            case 31457: t=30; break; // ARM Ecoing Commander
            case 31430: t=30; break; // Combat Commander
            case 31431: t=30; break; // CORE Ecoing Commander
            case 31433: t=30; break; // TLL Battle Commander
            default:
                t=parsingValue(morphCmd.getName(),"Time:", 60.0f); // TODO test!
                owner.sendTextMsg(" no morph info for ID"+morphCmd.getId()+", but parsing from text="+t+" (default 60).", FieldBOT.MSG_ERR);
        }
        // TODO !!!!
        for (String param:morphCmd.getParams()) {
            owner.sendTextMsg(" parsing param: ["+param+"] to float exception: ", FieldBOT.MSG_ERR);
        }
        /*
        for (String param:morphCmd.getParams()) {
            if (param.contains("time:")) { // parsing time...
                int p1=param.indexOf(": ");
                //if (p1==-1) p1=param.indexOf(":"); +1
                if (p1!=-1) {
                    String intStr=param.substring(p1+2);
                    try {
                        t=Float.parseFloat(intStr);
                    } catch (NumberFormatException e) {
                        owner.sendTextMsg(" parsing ERROR: ["+intStr+"] to float exception: "+e.toString() , FieldBOT.MSG_ERR);
                    }
                } else owner.sendTextMsg(" parsing ERROR: separator not found in ["+param+"]." , FieldBOT.MSG_ERR);
            }
        }
        if (t<=0.0) {
            t=100.0f;
            owner.sendTextMsg(" parsing ERROR: time not found!", FieldBOT.MSG_ERR);
        }*/
        return t;
    }
    public float[] getResForMprph(CommandDescription morphCmd) {
        float res[]=null;
        switch (morphCmd.getId()) {
            case 0:
            case 31456: res=new float[]{1350.0f, 24500.0f}; break; // ARM Battle Commander
            case 31457: res=new float[]{1100.0f, 24000f}; break; //!!! ARM Ecoing Commander
            case 31430: res=new float[]{1350.0f, 24500.0f}; break; // Combat Commander
            case 31431: res=new float[]{1100f, 24000f}; break; //!!! CORE Ecoing Commander
            case 31433: res=new float[]{1350.0f,12000.0f}; break; // TLL Battle Commander
            default:
               res=new float[owner.avgEco.resName.length];
               Arrays.fill(res, 0.0f);
               owner.sendTextMsg(" no morph info for ID"+morphCmd.getId(), FieldBOT.MSG_ERR);
                // TODO
               for (int i=0;i<res.length;i++) {
                res[i]=parsingValue(morphCmd.getName(),owner.avgEco.resName[i].getName()+":", 0.0f); // TODO test!
                owner.sendTextMsg("  >parsing from text="+res[i]+" (default 0).", FieldBOT.MSG_DBG_SHORT);
               }
        }
        return res;
    }
    
    /**
     * Может ли юнит построить базу. Прверяет, строит ли юнит строителей
     * @param def тестируемый юнит
     * @return 
     */
    public static boolean isAbleToBuildBase(UnitDef def) {
        // TODO можно кэшировать
        if (!def.isBuilder()) return false;
        List<UnitDef> bOption=def.getBuildOptions();
        if (bOption==null || bOption.isEmpty()) return false;
        for (UnitDef b2:bOption) {
            if (b2.isBuilder() && !b2.getBuildOptions().isEmpty()) return true;
        }
        return false;
    }
    
    /**
     * Действительно ли это может двигаться (в некоторых модах не тольео isAbleToMove нужно проверять)
     * @param def
     * @return 
     */
    public static boolean isRealyAbleToMove(UnitDef def) {
        return (def.isAbleToMove() && def.getSpeed()>0);
    }
    
    /**
     * Действительно ли это может помогать (в некоторых модах не тольео isAbleToAssist нужно проверять)
     * @param def
     * @return 
     */
    public static boolean isRealyAbleToAssist(UnitDef def) {
        // для фабрик.
        return def.isAbleToAssist() && !isStationarFactory(def);
    }
    
    public static boolean isStationarFactory(UnitDef def) {
        return def.isBuilder() && (def.isAbleToMove() && (def.getSpeed()==0)) && !def.getBuildOptions().isEmpty();
    }
    
}
