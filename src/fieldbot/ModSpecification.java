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

/**
 * Information about morph command.
 * @author PlayerO1
 */
class MorphInfo {
    int CMDId=-1;
    UnitDef from=null;
    UnitDef to=null;
    float time=Float.NaN;
    float resource[]=null;
    
    public MorphInfo (int cmdId) {
        this.CMDId=cmdId;
    }
    
    // TODO read/write MorphInfo to XML node for cashing
}

/**
 * Specific for different mods
 * @author PlayerO1
 */
public class ModSpecification {
    private final HashMap<String,float[]> treeMap_Eco; // LUA metal makers info
    
    private final FieldBOT owner;
    protected final List<UnitDef> allUnitDefs; // for Morph, get unit by ID.
    
    // FIXME TODO true support research center
    private final HashMap<UnitDef,UnlockTechnology> specialTLevelRequired;//(TODO temove it, it is old)For TA and RD, see specificUnitEnabled
    private final ArrayList<UnlockTechnology> lockTechno; //(new)For TA and RD, see specificUnitEnabled
    
    private final HashMap<Integer,MorphInfo> morphIDMap; // cashe for assign morph UnitDef to morph CMD ID (morphCMDID, morphToUDef)
    // TODO where Spring 97 or later that 97 version will be relase, check unitCustomParam/unitLUAParam for getting morph info and other info.
    private short MORPH_GETINFO_MODE=1; // 0 - do not get morph info, only from morphIDMap (TODO load from XML morph info); 1 - use cmdTolTip text parsing and add to morphIDMap new info; 2 - TODO after spring 97 relase getting info from unitScriptParam.
    
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
     * Exist MetalMaker native (EVO RTS) (info from isNativeMetalMaker, getNativeConversionKMetalMaker )
     */
    public final boolean exist_MetalMaker_native;
    /**
     * Exist MetalMaker by script (LUA) (BA, TA, NOTA) (info from getResourceConversionFor, isMetalMaker, getConversionKMetalMaker)
     */
    public final boolean exist_MetalMaker_lua;
    
    /**
     * Exist script (LUA) metal extractor on this mod, or null
     */
    public final UnitDef luaMetalExtractor;
    
    /**
     * Usualy resource ballance of incoming-usage. See metal makers and resource coast.
     */
    public final float resourceProportion[];
    
    /**
     * For Zero-K - first factory not need resource and build time, next factoru need.
     */
    public boolean firstFactoryIsFree;
    
    
    // TODO for Spring1944, Imperial Winter, and other capture flag MOD
    // public final boolean captureFlag;
    // public final int flagResourceID; // Metall or energy getting falg?
    
    /**
     * MOD short name
     */
    public final String modName;
    
    
    private final Resource resMetal; // for isExtractor fast test. TODO modify if use no "Metall, Energy" resource.

    
    public ModSpecification(FieldBOT owner) {
        this.owner=owner;
        allUnitDefs=owner.clb.getUnitDefs(); // !!! getting large list, required time
        
        modName = owner.clb.getMod().getShortName();
        
        //TODO read from XML.
        
        // Tech Anihilation //MOD short name:TA
        // Robot Defence MOD short name: RD
        
        specificUnitEnabled = modName.equals("TA") || modName.equals("RD"); // TODO specificUnitEnabled !
        // TODO NOTA have 2 war unit on SpecialUnitEnabled too.
        
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
            treeMap_Eco.put("armmmkr", new float[] { 8.0f, -800f } ); // Moho Metal Maker (T2)
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
            treeMap_Eco.put("cormex", new float[] { 2.7f, 0.2f } ); // Metal Extractor
            // TODO Zero-K extractor metal multiplier = sqrt(1+energy/4)) 
            luaMetalExtractor=owner.clb.getUnitDefByName("cormex");
            firstFactoryIsFree=true;
        } else {
            luaMetalExtractor=null;
            firstFactoryIsFree=false;
        }

        exist_MetalMaker_lua=treeMap_Eco.isEmpty();
        exist_MetalMaker_native= modName.equals("EvoRTS") || 
                !(modName.equals("NOTA")||modName.equals("BA")||modName.equals("TA")||modName.equals("RD"));
        
        // Mod resource proportion
        if (exist_MetalMaker_lua) {
            float rM=0,rE=0;
            int n=0;
            for (Map.Entry<String,float[]> entry:treeMap_Eco.entrySet()) {
                float r[]=entry.getValue();
                rM+=Math.abs(r[0]);
                rE+=Math.abs(r[1]);
                n++;
            }
            resourceProportion=new float[]{1.0f+rM/n, 2.0f+rE/n};
        } else {
            resourceProportion=new float[]{1.0f, 12.0f};
        }
        
        if (specificUnitEnabled) {
            // FIXME not depends from tech level, depends of unit Research center only!!!
            specialTLevelRequired=new HashMap<UnitDef, UnlockTechnology>();
            
            lockTechno=new ArrayList<UnlockTechnology>();
            String t1Names[]={"corech3","armrech3", "corech18","armrech18", "corech21","armrech21", "ccovertopscentre","acovertopscentre"};
            String t2Names[]={"corech18","armrech18", "corech21","armrech21", "ccovertopscentre","acovertopscentre"};
            String t3Names[]={"corech21","armrech21", "ccovertopscentre","acovertopscentre"};
            UnlockTechnology tLvl1=new UnlockTechnology(owner.clb, t1Names, null);
            lockTechno.add(tLvl1);
            UnlockTechnology tLvl2=new UnlockTechnology(owner.clb, t2Names, null);
            lockTechno.add(tLvl2);
            UnlockTechnology tLvl3=new UnlockTechnology(owner.clb, t3Names, null);
            lockTechno.add(tLvl3);
            
            // For Tech Anililation and Robot Defence
            UnitDef def;
            def=owner.clb.getUnitDefByName("armmarv");
            if (def!=null) specialTLevelRequired.put(def, tLvl1);
            def=owner.clb.getUnitDefByName("corfred");
            if (def!=null) specialTLevelRequired.put(def, tLvl1);

            // war units T1.5
            def=owner.clb.getUnitDefByName("armpw1");// kbot
            if (def!=null) specialTLevelRequired.put(def, tLvl1);
            def=owner.clb.getUnitDefByName("armcrack");
            if (def!=null) specialTLevelRequired.put(def, tLvl1);
            def=owner.clb.getUnitDefByName("aexxec");
            if (def!=null) specialTLevelRequired.put(def, tLvl1);
            def=owner.clb.getUnitDefByName("ahermes");// wehicle
            if (def!=null) specialTLevelRequired.put(def, tLvl1);
            def=owner.clb.getUnitDefByName("armsonic");
            if (def!=null) specialTLevelRequired.put(def, tLvl1);
            def=owner.clb.getUnitDefByName("armscar");
            if (def!=null) specialTLevelRequired.put(def, tLvl1);
            def=owner.clb.getUnitDefByName("armrottweiler");
            if (def!=null) specialTLevelRequired.put(def, tLvl1);
            def=owner.clb.getUnitDefByName("armarty");
            if (def!=null) specialTLevelRequired.put(def, tLvl1);
            def=owner.clb.getUnitDefByName("armblz"); // fly
            if (def!=null) specialTLevelRequired.put(def, tLvl1);
            def=owner.clb.getUnitDefByName("armjade");
            if (def!=null) specialTLevelRequired.put(def, tLvl1);

            def=owner.clb.getUnitDefByName("corak1");// kbot
            if (def!=null) specialTLevelRequired.put(def, tLvl1);
            def=owner.clb.getUnitDefByName("corrock");
            if (def!=null) specialTLevelRequired.put(def, tLvl1);
            def=owner.clb.getUnitDefByName("gladiator");
            if (def!=null) specialTLevelRequired.put(def, tLvl1);
            def=owner.clb.getUnitDefByName("corjeag");// vehivle
            if (def!=null) specialTLevelRequired.put(def, tLvl1);
            def=owner.clb.getUnitDefByName("cslicer");
            if (def!=null) specialTLevelRequired.put(def, tLvl1);
            def=owner.clb.getUnitDefByName("corgfbt");
            if (def!=null) specialTLevelRequired.put(def, tLvl1);
            def=owner.clb.getUnitDefByName("dao");
            if (def!=null) specialTLevelRequired.put(def, tLvl1);
            def=owner.clb.getUnitDefByName("cbrutus");
            if (def!=null) specialTLevelRequired.put(def, tLvl1);
            def=owner.clb.getUnitDefByName("corfiend");// fly
            if (def!=null) specialTLevelRequired.put(def, tLvl1);

            // T2.5
            def=owner.clb.getUnitDefByName("armcav");// ARM, kbot
            if (def!=null) specialTLevelRequired.put(def, tLvl2);
            def=owner.clb.getUnitDefByName("taipan");
            if (def!=null) specialTLevelRequired.put(def, tLvl2);
            def=owner.clb.getUnitDefByName("akmech");
            if (def!=null) specialTLevelRequired.put(def, tLvl2);
            def=owner.clb.getUnitDefByName("armhdpw");
            if (def!=null) specialTLevelRequired.put(def, tLvl2);
            def=owner.clb.getUnitDefByName("armmech");
            if (def!=null) specialTLevelRequired.put(def, tLvl2);
            def=owner.clb.getUnitDefByName("armbull2");//wehicle
            if (def!=null) specialTLevelRequired.put(def, tLvl2);
            def=owner.clb.getUnitDefByName("tawf014");
            if (def!=null) specialTLevelRequired.put(def, tLvl2);
            def=owner.clb.getUnitDefByName("armorca");
            if (def!=null) specialTLevelRequired.put(def, tLvl2);
            def=owner.clb.getUnitDefByName("tankanotor");
            if (def!=null) specialTLevelRequired.put(def, tLvl2);
            def=owner.clb.getUnitDefByName("armscpion");
            if (def!=null) specialTLevelRequired.put(def, tLvl2);
            def=owner.clb.getUnitDefByName("armcd");
            if (def!=null) specialTLevelRequired.put(def, tLvl2);
            def=owner.clb.getUnitDefByName("corprot");// CORE bot
            if (def!=null) specialTLevelRequired.put(def, tLvl2);
            def=owner.clb.getUnitDefByName("coredauber");
            if (def!=null) specialTLevelRequired.put(def, tLvl2);
            def=owner.clb.getUnitDefByName("cormonsta");
            if (def!=null) specialTLevelRequired.put(def, tLvl2);
            def=owner.clb.getUnitDefByName("armkrmi");
            if (def!=null) specialTLevelRequired.put(def, tLvl2);
            def=owner.clb.getUnitDefByName("krogtaar");
            if (def!=null) specialTLevelRequired.put(def, tLvl2);
            def=owner.clb.getUnitDefByName("corpyrox");
            if (def!=null) specialTLevelRequired.put(def, tLvl2);
            def=owner.clb.getUnitDefByName("corshieldgen");
            if (def!=null) specialTLevelRequired.put(def, tLvl2);
            def=owner.clb.getUnitDefByName("cortotal");// tank
            if (def!=null) specialTLevelRequired.put(def, tLvl2);
            def=owner.clb.getUnitDefByName("nsacskv");
            if (def!=null) specialTLevelRequired.put(def, tLvl2);
            def=owner.clb.getUnitDefByName("requ1");
            if (def!=null) specialTLevelRequired.put(def, tLvl2);
            def=owner.clb.getUnitDefByName("cormddm");
            if (def!=null) specialTLevelRequired.put(def, tLvl2);

// TODO realy need do it on XML export.
        } else {
            specialTLevelRequired=null;
            lockTechno=null;
        }
        
        morphIDMap=new HashMap<Integer, MorphInfo>(); // TODO init morphIDMap only on Mod, when realy need it.
        // TODO load from XML cashe file and save to file morphIDMap
        
        resMetal=owner.clb.getResourceByName("Metal");
    }
    
    /**
     * Info about LUA Metal Maker
     * @param def metal maker
     * @return addition resourse of this unit, or null if not LUA specification.
     */
    public float[] getResourceConversionFor(UnitDef def) {
        return treeMap_Eco.get(def.getName());
    }
    
    /**
     * If it is special Metal Maker by LUA script, you can use getConversionKMetalMaker()
     * @param def
     * @return true - it is LUA metal maker (can return false for native metal maker), false - no resource maker
     */
    public boolean isMetalMaker(UnitDef def) {
        // TODO For Spring >= 98 check GameRulesParam !!!!!!!!!
        // see http://springrts.com/phpbb/viewtopic.php?f=15&t=32732&start=20
        return treeMap_Eco.containsKey(def.getName());
    }
    /**
     * Return conversion ratio of "LUA" metal maker
     * @param def
     * @return value between 1.0 and 0.0. Value 1.0 - is better, for no maker. 0.7 better that 0.01.
     */
    public float getConversionKMetalMaker(UnitDef def) {
        float[] val=treeMap_Eco.get(def.getName());
        if (val==null) return 1.0f;
        if (val[1]<=0) return 1.0f; //Float.POSITIVE_INFINITY;// !!!!
        return val[0]/(val[0]+val[1]); // TODO check!!! or val[0]/(1.0f+val[1])
    }
    
    /**
     * Check of native resource maker
     * @param def unit type
     * @return true, if this unit is native metal maker, or other resource maker :)
     */
    public boolean isNativeMetalMaker(UnitDef def) {
        // TODO make cashe
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
     * Return conversion ratio of "native" metal maker
     * Вычисляется как: Sum(вырабатываемые)/Sum(потребляемые).
     * @param def тип юнита
     * @return return value between 1.0 and 0.0. Value 1.0 - better
     */
    public float getNativeConversionKMetalMaker(UnitDef def) {
        float sMin,sMax;
        sMin=sMax=0.0f;
        for (Resource r:owner.clb.getResources()) {
            float x=owner.getUnitResoureProduct(def, r);
            if (x<0.0f) sMin+=x;
            else sMax+=x;
        }
        if (sMin<0.0) return sMax/(sMax-sMin); // TODO test, or sMax/(1.0f-sMin)
        else return 1.0f;//Float.POSITIVE_INFINITY;
    }

// work with TechLevel and mod specification. FOr example on TA - required research center

    /**
     * For TA/RD mod some units can not build before building Research center
     * @param def
     * @return true if need check of special level
     */
    public boolean requiredSpecialTechLevel(UnitDef def) {
        return (specialTLevelRequired==null) || (specialTLevelRequired.get(def)!=null);
    }
// last   public Integer getRequiredTechLevel(UnitDef def) {
//        return specialTLevelRequired.get(def);
//    }
    public boolean haveTechLevelFor(UnitDef def) {
        if (specialTLevelRequired==null) return true;
        UnlockTechnology reqTL=specialTLevelRequired.get(def);
        if (reqTL==null) return true;
        return reqTL.unlocked();
        // last 2: return owner.techLevels.get(reqTL)!=null;
        // last:
        //return !requiredSpecialTechLevel(def) ||
        //        (owner.techLevels.get(getRequiredTechLevel(def))!=null);
    }
    /**
     * Seek tech level required
     * @param def
     * @param techLvl list of have tech level (have UnitDef)
     * @return can build, or need to build new tech level before
     */
    public boolean haveTechLevelFor(UnitDef def, Collection<UnitDef> techLvl) { //было int haveLevels[]
        if (specialTLevelRequired==null) return true;
        UnlockTechnology reqTL=specialTLevelRequired.get(def);
        if (reqTL==null) return true;
        return reqTL.unlocked() || reqTL.unlockedBy(techLvl);
        //techLvl.contains(reqTL);
        //return Arrays.binarySearch(haveLevels, reqTL)!=-1;
        //last: return !requiredSpecialTechLevel(def) ||
        //        Arrays.binarySearch(haveLevels, getRequiredTechLevel(def))!=-1;
    }
    
    /**
     * Remove all unit with required special tech level and this level not have now
     * @param lstDef
     * @return list with unit that can build now
     */
    public boolean removeUnitWhenCantBuildWithTeclLevel(Collection<UnitDef> lstDef) {
        Iterator<UnitDef> itr1 = lstDef.iterator();
        boolean modifed=false;
        while (itr1.hasNext()) {
            UnitDef def = itr1.next();
            if (!haveTechLevelFor(def)) {
                itr1.remove();
                modifed=true;
            }
        }
        return modifed;
    }
    
    /**
     * Remove all unit with required special tech level and this level not have in parameters
     * @param lstDef
     * @param techLvl2 add compare with this + owner.techLevels
     * @return list with unit that can build now or in future with techLvl2 levels
     */
    public boolean removeUnitWhenCantBuildWithTeclLevel(Collection<UnitDef> lstDef, Collection<UnitDef> techLvl2) {
        Iterator<UnitDef> itr1 = lstDef.iterator();
        boolean modifed=false;
        while (itr1.hasNext()) {
            UnitDef def = itr1.next();
            if (!haveTechLevelFor(def) && !haveTechLevelFor(def, techLvl2)) {
                itr1.remove();
                modifed=true;
            }
        }
        return modifed;
    }
    
    // TODO create Selector class, then move this into Selector.
    /**
     * Select units by tech level
     * @param defs list for select
     * @param level parameter for select
     * @return list with units when def.getTechLevel()==level
     */
    public static ArrayList<UnitDef> selectDefByTechLevel(ArrayList<UnitDef> defs,int level)
    {
        ArrayList<UnitDef> selectedLst=new ArrayList<UnitDef>();
        for (UnitDef def:defs) if (def.getTechLevel()==level) selectedLst.add(def);
        return selectedLst;
    }
    
    /**
     * Return true list of real build options (only where enabled). It is require for TA/RD mod.
     * @param uBuilder
     * @return what realy can build this unit now
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
    
    /**
     * Register for work with UnlockTechnology
     * @param newUnit 
     */
    public void onAddUnit(UnitDef newUnit) {
        //TODO check metal maker max energy conversion value too
        if (lockTechno!=null) {
            for (UnlockTechnology technology:lockTechno) technology.onAddUnit(newUnit);
        }
    }
    /**
     * Register for work with UnlockTechnology
     * @param newUnit 
     */
    public void onRemoveUnit(UnitDef newUnit) {
        if (lockTechno!=null) {
            for (UnlockTechnology technology:lockTechno) technology.onRemoveUnit(newUnit);
        }
    }
    
// ---- with morphing ----
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
     * Return all enabled morph CMD from unit.
     * @param unit
     * @param showDisabledToo
     * @return list of morph CMP from unit.getSupportedCommands()
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
     * Return command for stop morph
     * @param unit
     * @param showDisabledToo
     * @return CMD or null
     */
    public static CommandDescription getStopMorphCmd(Unit unit,boolean showDisabledToo) {
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
     * Return UnitDef after unit will be do Morph
     * @param morphCmd
     * @param fromUnit
     * @return future UnitDef (or NULL, if not infromation found)
     */
    public UnitDef getUnitDefForMprph(CommandDescription morphCmd, UnitDef fromUnit) {
        int morphID=morphCmd.getId();
        UnitDef morphTo=null;
        MorphInfo tmpMI=morphIDMap.get(morphID);
        if (tmpMI!=null) morphTo=tmpMI.to;
        if (morphTo!=null) return morphTo; // cashe using
        
        if (MORPH_GETINFO_MODE==0) return null; // do not add new info, if mode = 0
        if (MORPH_GETINFO_MODE>=2 || MORPH_GETINFO_MODE<0) throw new IllegalArgumentException(" this mode "+MORPH_GETINFO_MODE+" not support. To doing");
        
        // or find...
        // WTF I DOING? IT IS BOT API, NO HUMAN! The bot do not need read human names, there BOT life on numbers, no words.
        String unitName=parsingValueString(morphCmd.getToolTip(),"Morph into a",""); // FIXME check prefix for all mod, and text format
         // getToolTip() - description ""? example?
         // getName() = "Morph"
        if (unitName.isEmpty()) return null;
        owner.sendTextMsg("Parsing Def name from Cmd.getToolTip '"+unitName+"' for morphId="+morphID+"." , FieldBOT.MSG_DBG_SHORT);
        
        // Search by name
        // select by CORE/ARM/TLL only
        String fraction=null;//FIXME how true select fraction from unit name???
        if (fromUnit!=null) fraction=fromUnit.getName().substring(0, 3); // FIXME check all using "substring" and index on sources!!!!
        morphTo=TBOTTalking.findUnitDefByHumanQuery(unitName, null, allUnitDefs, fraction);

        if (morphTo==null)
            owner.sendTextMsg(" not found.", FieldBOT.MSG_DBG_SHORT);
            else owner.sendTextMsg(" found: "+morphTo.getName(), FieldBOT.MSG_DBG_SHORT);

        if (morphTo!=null) { // add to cashe
            if (tmpMI==null) {
                tmpMI=new MorphInfo(morphID);
                morphIDMap.put(morphID, tmpMI);
            }
            if (tmpMI.from==null && fromUnit!=null) tmpMI.from=fromUnit;
            if (tmpMI.to==null) tmpMI.to=morphTo;
        } 
        
        return morphTo;
    }
    
    /**
     * Parse string "qwe asd abc 123 def; key=abc: value=123"
     * @param source
     * @param key
     * @return parsing number after "key" word, or defaultVal
     */
    private float parsingValueFloat(String source, String key,float defaultVal) {
        float val;//=defaultVal;
        
        key=key.toLowerCase(); // for better comparable
        source=source.toLowerCase();
        
        String keyS=key+" ";
        int p1=source.indexOf(keyS);
        if (p1==-1) {
            keyS=key;
            p1=source.indexOf(keyS);
        }
        if (p1!=-1) {
            p1+=keyS.length();
            String intStr=source.substring(p1); // !!!
            int pNL=intStr.indexOf("\n");
            if (pNL>0) intStr=intStr.substring(0, pNL);
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
        return val;
    }
    
    /**
     * Parse string "qwe asd abc; key=asd: value=abc"
     * @param source
     * @param key
     * @return parsing number after "key" word, or defaultVal
     */
    private String parsingValueString(String source, String key,String defaultVal) {
        String val;//=defaultVal;
        String keyS=key+" ";
        int p1=source.indexOf(keyS);
        if (p1==-1) {
            keyS=key;
            p1=source.indexOf(keyS);
        }
        if (p1!=-1) {
            p1+=keyS.length();
            String xStr=source.substring(p1); // !!!
            if (xStr.length()>0) {
                int pNL=xStr.indexOf("\n");
                if (pNL>0) xStr=xStr.substring(0, pNL);
                val=xStr;
            } else {
                val=defaultVal;
            }
        } else {
            owner.sendTextMsg(" parsing ERROR: symbol ["+key+"] not found on ["+source+"]." , FieldBOT.MSG_ERR);
            val=defaultVal;
        }
        return val;
    }
    
    public float getTimeForMprph(CommandDescription morphCmd) {
        float t;// return value
        int morphID=morphCmd.getId();
        MorphInfo tmpMI=morphIDMap.get(morphID);
        if (tmpMI!=null && !Float.isNaN(tmpMI.time)) {
            t=tmpMI.time;
        } else {
            switch (MORPH_GETINFO_MODE) {
                case 0: t=0;
                    break;
                case 1:
                    t=parsingValueFloat(morphCmd.getToolTip(),"Time:", Float.NaN);
                    if (tmpMI!=null && !Float.isNaN(tmpMI.time) && !Float.isNaN(t)) tmpMI.time=t;
                    if (Float.isNaN(t)) {
                        t=0; // no time found? TODO time check for morph.
                        owner.sendTextMsg(" time not found for ID"+morphCmd.getId(), FieldBOT.MSG_DBG_SHORT);
                    }
                    owner.sendTextMsg(" new time parsing for ID"+morphCmd.getId()+", t="+t, FieldBOT.MSG_DBG_SHORT);
                    break;
                default:
                    throw new IllegalArgumentException(" this mode "+MORPH_GETINFO_MODE+" not support. To doing this."); // TODO mode 3 for Spring >97
            }
        }
// debug:
//        for (String param:morphCmd.getParams()) {
//            owner.sendTextMsg(" parsing param: ["+param+"] to float exception: ", FieldBOT.MSG_ERR);
//        }
        return t;
    }
    public float[] getResForMprph(CommandDescription morphCmd) {
        float res[];// return value
        int morphID=morphCmd.getId();
        MorphInfo tmpMI=morphIDMap.get(morphID);
        if (tmpMI!=null && tmpMI.resource!=null) return tmpMI.resource;

        res=new float[owner.avgEco.resName.length];
        Arrays.fill(res, 0.0f);
        switch (MORPH_GETINFO_MODE) {
            case 0: break;
            case 1:
                for (int i=0;i<res.length;i++) {
                  res[i]=parsingValueFloat(morphCmd.getToolTip(),owner.avgEco.resName[i].getName()+":", 0.0f); // TODO test!
        //          owner.sendTextMsg("  >parsing from text="+res[i]+" (default 0).", FieldBOT.MSG_DBG_ALL); // DEBUG
                }
                owner.sendTextMsg(" new resource parsing for ID"+morphCmd.getId()+" res="+Arrays.toString(res), FieldBOT.MSG_DBG_SHORT);
                if (tmpMI!=null) tmpMI.resource=res;
                break;
            default:
                throw new IllegalArgumentException(" this mode "+MORPH_GETINFO_MODE+" not support. To doing this."); // TODO mode 3 for Spring >97
        }
        return res;
    }
    
    public boolean isExtractor(UnitDef def) {
        return def==luaMetalExtractor || def.getExtractsResource(resMetal)>0.0f;
    }
    
    /**
     * Can unit build new base. Check, can do this unit build other builders.
     * @param def builder
     * @return 
     */
    public static boolean isAbleToBuildBase(UnitDef def) {
        // TODO can be cashed
        if (!def.isBuilder()) return false;
        List<UnitDef> bOption=def.getBuildOptions();
        if (bOption==null || bOption.isEmpty()) return false;
        for (UnitDef b2:bOption) {
            if (b2.isBuilder() && !b2.getBuildOptions().isEmpty()) return true;
        }
        return false;
    }
    
    /**
     * Can this unit realy move (in same mod need addition check, not only isAbleToMove, for example - factory)
     * @param def
     * @return 
     */
    public static boolean isRealyAbleToMove(UnitDef def) {
        return (def.isAbleToMove() && def.getSpeed()>0);
    }
    
    /**
     * Do realy this unit can assist (in same mod need addition check, not only isAbleToMove, for example - factory)
     * @param def
     * @return 
     */
    public static boolean isRealyAbleToAssist(UnitDef def) {
        // for assist, constructor or nano tower
        boolean isAble = def.isAbleToAssist() && (!isStationarFactory(def) || def.isCommander()); // for NOTA - isCommander, TODO other NOTA builder tower
        if (!isAble) return false;
        String hName=def.getHumanName();
        if (hName.contains("Air Repair") || hName.contains("Air Support Pad") ) return false; // for Air Repair platform - usualy they can not assist, but they have assist option.
        // Remark: NOTA unit SMART is realy able to assist, it is not only mobile air repair pad.
        //if (hName.equals("Giant")) return false; // TA mod, TLL air repair ship with anti-nuke. P.S. It realy can repair other units on TA 2.31, just small radius.
        return true;
    }
    
    public static boolean isStationarFactory(UnitDef def) {
        return def.isBuilder() && (def.isAbleToMove() && (def.getSpeed()==0)) && !def.getBuildOptions().isEmpty();
        // Warning: for NOTA it will be true for command center and other build tower!
    }
    
}
