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
 * Specific for different mods
 * @author PlayerO1
 */
public class ModSpecification {
    private final HashMap<String,float[]> treeMap_Eco; // LUA metal makers info
    
    private final FieldBOT owner;
    protected final List<UnitDef> allUnitDefs; // for Morph, get unit by ID.
    
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
    
    /**
     * MOD short name
     */
    public final String modName;
    
    public ModSpecification(FieldBOT owner) {
        this.owner=owner;
        allUnitDefs=owner.clb.getUnitDefs(); // !!! getting large list, required time
        
        modName = owner.clb.getMod().getShortName();
        
        //TODO read from XML.
        
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
            treeMap_Eco.put("cormex", new float[] { 2.0f, 0.0f } ); // Metal Extractor
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
        // last:
        //return !requiredSpecialTechLevel(def) ||
        //        (owner.techLevels.get(getRequiredTechLevel(def))!=null);
    }
    /**
     * Seek tech level required
     * @param def
     * @param techLvl list of have tech level
     * @return can build, or need to build new tech level before
     */
    public boolean haveTechLevelFor(UnitDef def, HashSet<Integer> techLvl) { //было int haveLevels[]
        if (specialTLevelRequired==null) return true;
        Integer reqTL=specialTLevelRequired.get(def);
        if (reqTL==null) return true;
        return techLvl.contains(reqTL);
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
    public boolean removeUnitWhenCantBuildWithTeclLevel(Collection<UnitDef> lstDef, HashSet<Integer> techLvl2) {
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
    public ArrayList<UnitDef> selectDefByTechLevel(ArrayList<UnitDef> defs,int level)
    {
        ArrayList<UnitDef> selectedLst=new ArrayList<UnitDef>();
        for (UnitDef def:defs) if (def.getTechLevel()==level) selectedLst.add(def);
        return selectedLst;
    }
    
    /**
     * Return true list of real build options (only where enabled). It is require for TA/RD mod.
     * TODO DO NOT WORK correct!
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
     * @return future UnitDef (or NULL, TODO null value)
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
            default:
//                FIXME // WTF I DOING? IT IS BOT API, NO HUMAN! The bot do not need read human names, there BOT life on numbers, no words.
//                String unitName=parsingValueString(morphCmd.getName(),"Morph into a","");
//                if (unitName.isEmpty()) return null;
//                owner.sendTextMsg("Def name= '"+unitName+"'." , FieldBOT.MSG_ERR);
//                // Search by name
//                for () {
//                    throw new IllegalArgumentException("TODO parsingValueString!");// FIXME TODO THIS!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
//                }
                return null;
        }
        
        
//        owner.sendTextMsg("MORPHING CMD ID="+morphCmd.getId()+" supported ID="+morphCmd.getSupportedCommandId(), FieldBOT.MSG_DBG_ALL);
        UnitDef def=allUnitDefs.get(mUnitDefID-1);
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
    private float parsingValueFloat(String source, String key,float defaultVal) {
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
                throw new IllegalArgumentException("TODO parsingValueString!");// FIXME TODO THIS!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                //val=xStr.substring(0, p1);!!!
            } else {
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
                t=parsingValueFloat(morphCmd.getName(),"Time:", 60.0f); // TODO test!
                owner.sendTextMsg(" no morph info for ID"+morphCmd.getId()+", but parsing from text="+t+" (default 60).", FieldBOT.MSG_ERR);
        }
        // TODO !!!!
//        for (String param:morphCmd.getParams()) {
//            owner.sendTextMsg(" parsing param: ["+param+"] to float exception: ", FieldBOT.MSG_ERR);
//        }
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
                res[i]=parsingValueFloat(morphCmd.getName(),owner.avgEco.resName[i].getName()+":", 0.0f); // TODO test!
                owner.sendTextMsg("  >parsing from text="+res[i]+" (default 0).", FieldBOT.MSG_DBG_SHORT);
               }
        }
        return res;
    }
    
    /**
     * Can unit build new base. Check can this unit build other builder.
     * @param def builder
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
        boolean isAble = def.isAbleToAssist() && !isStationarFactory(def);
        if (!isAble) return false;
        String hName=def.getHumanName();
        if (hName.contains("Air Repair") || hName.contains("Air Support Pad")) return false; // for Air Repair platform - usualy they can not assist, but they have assist option.
        if (hName.equals("Giant")) return false; // TA mod, TLL air repair ship with anti-nuke
        return true;
    }
    
    public static boolean isStationarFactory(UnitDef def) {
        return def.isBuilder() && (def.isAbleToMove() && (def.getSpeed()==0)) && !def.getBuildOptions().isEmpty();
    }
    
}
