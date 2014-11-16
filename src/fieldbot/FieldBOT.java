/*
Copyright (c) 2014 PlayerO1, https://github.com/playerO1
This class has been rewritten from NullOOJavaAI Copyright (c) 2008 Robin Vobruba <hoijui.quaero@gmail.com>

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <http://www.gnu.org/licenses/>.

*/

package fieldbot;


import fieldbot.AIUtil.CPULoadTimer;
import com.springrts.ai.AI;
import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.OOAI;
import com.springrts.ai.oo.clb.*;
import fieldbot.AIUtil.MathPoints;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import java.util.Properties;
import java.util.logging.*;


import com.json.parsers.JSONParser; // For ZeroK LUA message
import com.json.parsers.JsonParserFactory;

/**
* Serves as Interface for a Java Skirmish AIs for the Spring engine.
*
* @author hoijui - author of NullOOAI, rewrite to FieldBOT by PlayerO1
*/
public class FieldBOT extends OOAI implements AI {

    
private static class MyCustomLogFormatter extends Formatter {

private DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss:SSS dd.MM.yyyy");

public String format(LogRecord record) {

// Create a StringBuffer to contain the formatted record
// start with the date.
StringBuffer sb = new StringBuffer();

// Get the date from the LogRecord and add it to the buffer
Date date = new Date(record.getMillis());
sb.append(dateFormat.format(date));
sb.append(" ");

// Get the level name and add it to the buffer
sb.append(record.getLevel().getName());
sb.append(": ");

// Get the formatted message (includes localization
// and substitution of paramters) and add it to the buffer
sb.append(formatMessage(record));
sb.append("\n");

return sb.toString();
}
}

private static void logProperties(Logger log, Level level, Properties props) {

log.log(level, "properties (items: " + props.size() + "):");
for (String key : props.stringPropertyNames()) log.log(level, key + " = " + props.getProperty(key));
}

private int skirmishAIId = -1;
protected int teamId = -1;
private Properties info = null;
private Properties optionValues = null;
public OOAICallback clb = null;
private String myLogFile = null;
private Logger log = null;

private static final int DEFAULT_ZONE = 0; // ????

public int currentFrime=-1; // Game frime
public CPULoadTimer cpuTimer; // for check CPU usage

/**
 * Init
 */
public FieldBOT() {
    cpuTimer=new CPULoadTimer();
}


// Message level
public static final int MSG_DBG_ALL=4;
public static final int MSG_DBG_SHORT=3;
public static final int MSG_ERR=2;
public static final int MSG_DLG=1;
public static final int MSG_NOSHOW=0;

protected static int LOG_level_show=MSG_ERR; // in game message log level

/**
 * Send message to chat and to log file
 * @param msg text
 * @param msgShowLvl MSG_DBG_ALL, MSG_DBG_SHORT, MSG_ERR, MSG_DLG. It is only for chat, check with LOG_level_show. All message write in log file.
 * @return 0 - success, -1 - fail
 */
public int sendTextMsg(String msg, int msgShowLvl) {
  try {
    if (msgShowLvl<=LOG_level_show) clb.getGame().sendTextMessage(msg, DEFAULT_ZONE);
    log.info(msg);
  } catch (Exception ex) {
    ex.printStackTrace();
    return -1;
  }
  return 0;
}
/**
 * Print next message to chat and log file:
 *  ERROR %comment%, exception: e.toString()
 *  STACK TRACE> ... stacktrake lines
 * @param comment method name, or other small comment
 * @param e exception
 */
private void printStackTrakeToChat(String comment, Exception e) {
    sendTextMsg("ERROR "+comment+", exception: "+e.toString(), MSG_ERR);
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    e.printStackTrace(pw);
    sendTextMsg(" STACK TRACE> "+sw.toString(), MSG_ERR);
}

public boolean isDebugging() {
  return true;
}


// ---------

public String botShortName;
//protected ArrayList<TBase> bases; // TODO ПЕРЕДЕЛАТЬ в AGroupManager. Было: Список баз.
protected ArrayList<AGroupManager> smartGroups;
private ArrayList<AGroupManager> smartGroupsToRemove; // temp list for remove from cycle.

/**
 * Average resource info
 */
public AdvECO avgEco;

public TEcoStrategy ecoStrategy;
public TWarStrategy warStrategy;
public ModSpecification modSpecific;
public TBOTTalking talkingDialogModule;
public TScoutModule scoutModule;

/**
 * List of dead or sharing to other team units. Bot do not action with this list.
 */
protected ArrayList<Unit> deadLstUnit;

/**
 * List of having commanders.
 * TODO use it.
 */
protected ArrayList<Unit> commanders; //TODO use it

/**
 * List of TechLevel (key) and unit count of this level (value)
 */
protected HashMap<Integer,Integer> techLevels; // TODO what better that HashMap for Integer?

public static final int IS_METAL_FIELD=1;
public static final int IS_NO_METAL=-1;
public static final int IS_NORMAL_METAL=0;
/**
 * Metal map type
 * -1 no metal map
 *  0 normal map with metal spots
 *  1 metal field - metal on all area
 * (error init = -2)
 */
public int isMetalFieldMap=-2;

/**
 * Use class TechUpTrigger for smart tech up process.
 * 0 - fast
 * 1 - smart, required some CPU time
 * 2 - expert (better that smart, required more CPU)
 */
public int useTechUpMode;

private TechUpTrigger techUpTrigger;

/**
 * Class for planing base square (ABasePlaning).
 * 0 random
 * 1 spiral
 * 2 circle
 * 3 tetris
 */
public int defauldBasePlaning=1;
/**
 * Do automatic tech up. Or by text chat command "bot do tech up!"
 */
public boolean autoTechUp=true;
// ---------


@Override
public int init(int skirmishAIId, OOAICallback callback) {
cpuTimer.start();
 int ret = -1;

this.skirmishAIId = skirmishAIId;
this.clb = callback;

this.teamId = clb.getSkirmishAI().getTeamId();
this.botShortName="bot";// ??? !!!

info = new Properties();
Info inf = clb.getSkirmishAI().getInfo();
int numInfo = inf.getSize();
for (int i = 0; i < numInfo; i++) {
  String key = inf.getKey(i);
  String value = inf.getValue(i);
  info.setProperty(key, value);
  if (key.equals("shortName")) this.botShortName=value;
}


// initialize the log
try {
    myLogFile = callback.getDataDirs().allocatePath("log-team-" + teamId + ".txt", true, true, false, false);
    FileHandler fileLogger = new FileHandler(myLogFile, false);
    fileLogger.setFormatter(new MyCustomLogFormatter());
    fileLogger.setLevel(Level.ALL);
    log = Logger.getLogger("fieldbot");
    log.addHandler(fileLogger);
    if (isDebugging()) {
        log.setLevel(Level.ALL);
    } else {
        log.setLevel(Level.INFO);
    }
} catch (Exception ex) {
  System.err.println("Field BOT: Failed initializing the logger!");
  ex.printStackTrace();
  ret = -2;
}

short opt_talkingDialogModule_allowEnemyTeamCommand=0;
short opt_armyControl=0;

try {
    optionValues = new Properties();
    OptionValues opVals = clb.getSkirmishAI().getOptionValues();
    int numOpVals = opVals.getSize();
    for (int i = 0; i < numOpVals; i++) {
      String key = opVals.getKey(i);
      String value = opVals.getValue(i);
      optionValues.setProperty(key, value);

      // --- Определение параметров запуска бота ---
      if (key.equals("commandbyenemy")) {
          if (value.equals("1")) opt_talkingDialogModule_allowEnemyTeamCommand=1;
          if (value.equals("0")) opt_talkingDialogModule_allowEnemyTeamCommand=-1;
      }
      if (key.equals("autotechup")) {
          if (value.equals("1")) autoTechUp=true;
          if (value.equals("0")) autoTechUp=false;
      }
      useTechUpMode=0; // default
      if (key.equals("smarttechup")) {
          if (value.equals("2")) useTechUpMode=2;
          if (value.equals("1")) useTechUpMode=1;
          if (value.equals("0")) useTechUpMode=0;
      }
      if (key.equals("usearmy")) {
          if (value.equals("1")) opt_armyControl=1;
          if (value.equals("0")) opt_armyControl=-1;
      }
      if (key.equals("messagelevel")) {
          try {
            int lvl=Integer.parseInt(value);
            LOG_level_show=lvl;
          } catch (NumberFormatException e) {
              //TODO show error.
          }
      }
      if (key.equals("builddecorator")) {
          if (value.equals("random")) defauldBasePlaning=0;
          if (value.equals("spiral")) defauldBasePlaning=1;
          if (value.equals("circle")) defauldBasePlaning=2;
          if (value.equals("tetris")) defauldBasePlaning=3;
      }
      if (key.equals("metaldetect")) {
          try {
            isMetalFieldMap=Integer.parseInt(value);
          } catch (NumberFormatException e) {
              //TODO show error.
          }
      }


      // ------
    }
} catch (Exception e) {
    log.log(Level.WARNING, "Failed read start properties.", e);
    //ret = -5;
}


 try {
    log.info("initializing team " + teamId);

    log.log(Level.FINE, "info:");
    logProperties(log, Level.FINE, info);

    log.log(Level.FINE, "options:");
    logProperties(log, Level.FINE, optionValues);

    ret = 0;
 } catch (Exception ex) {
    log.log(Level.SEVERE, "Failed initializing", ex);
    ret = -3;
}
 
 //----
    try {
        smartGroups=new ArrayList<AGroupManager>();
        smartGroupsToRemove = new ArrayList<AGroupManager>();
        commanders=new ArrayList<Unit>();
        techLevels=new HashMap<Integer, Integer>();
        // TODO maybe same final?
        
         if (isMetalFieldMap==-2) checkForMetal(); // test for metal map
        modSpecific = new ModSpecification(this);
        avgEco = new AdvECO(14, clb, modSpecific); // TODO check more better time for collect information
         initMetalData(); // init metal data (required know about isMetalFieldMap)
        scoutModule = new TScoutModule(this);// TODO maybe this make crash on S94 with NOTALobby?
        ecoStrategy = new TEcoStrategy(this);
        warStrategy = new TWarStrategy(this);
        talkingDialogModule = new TBOTTalking(this);
         if (opt_talkingDialogModule_allowEnemyTeamCommand==1) talkingDialogModule.allowEnemyTeamCommand=true;
         if (opt_talkingDialogModule_allowEnemyTeamCommand==-1) talkingDialogModule.allowEnemyTeamCommand=false;
         if (opt_armyControl==1) warStrategy.makeArmy=true;
         if (opt_armyControl==-1) warStrategy.makeArmy=false;

         
        deadLstUnit=new ArrayList<Unit>();
        techUpTrigger=null;
        
        // Check human query for start position (DON't WORK!)
        AIFloat3 queryStartPoint=talkingDialogModule.checkStartPositionMessage();
        if (queryStartPoint!=null) {
            sendTextMsg("Command acepted: start at "+queryStartPoint.x+":"+queryStartPoint.z, MSG_DLG);
            clb.getGame().sendStartPosition(false, queryStartPoint);
            clb.getGame().sendStartPosition(true, queryStartPoint);
            // TODO DO NOT WORK! Command acepted, but start on other position. It maybe bug on Spring 94.1. TEst it later.
        }
        
        
        addSmartGroup(new TBase(this, clb.getMap().getStartPos(), 2000));  // Init first base
        
    } catch (Exception e) {
        ret=-4;
        sendTextMsg("ERROR init, exception: "+e.toString(), MSG_ERR);
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
      sendTextMsg(" STACK TRACE> "+sw.toString(), MSG_ERR);
    }

 //----
    if(ret!=0) sendTextMsg("ERROR: init return "+ret+";", MSG_ERR);
         
  cpuTimer.stop();
return ret;
}

@Override
public int release(int reason) {
    sendTextMsg("release: reason="+reason, MSG_DBG_SHORT);
 return 0; // signaling: OK
}

/**
 * Set variable isMetalFieldMap, test map for metal type
 */
public void checkForMetal()
{
    Resource metal=clb.getResourceByName("Metal");//Resource.getInstance(clb, 0);
    if (metal==null) {
        sendTextMsg("Resource Metal not found in MOD!", MSG_ERR);
        isMetalFieldMap=-2;// !!!!!!!
        return;
    }
    AIFloat3 metalspot=clb.getMap().getResourceMapSpotsNearest(metal,clb.getMap().getStartPos());
    if( !MathPoints.isValidPoint(metalspot) ) // y - высота
    {
        sendTextMsg("This is a map with no metal spots", MSG_DLG);
        isMetalFieldMap=IS_NO_METAL;
    }
    else // isMetalFieldMap 0 and 1
    {
        // check for metal field
        float dLmax=0.0f,dLavg=0.0f;
        final double extractorR=clb.getMap().getExtractorRadius(metal);
        final double r=extractorR*7.5;
        final int nApprox=10;
        AIFloat3 pCenter=clb.getMap().getStartPos();
        for (int i=0;i<nApprox;i++) {
            AIFloat3 pFind= new AIFloat3(pCenter);
            double ang=2*Math.PI * ((double)i/(double)(nApprox+1));
            pFind.x+=Math.cos( ang )*r; pFind.z+=Math.sin( ang )*r;
            AIFloat3 p=clb.getMap().getResourceMapSpotsNearest(metal,pFind);
        sendTextMsg("METAL TEST >pos="+pFind.x+":"+pFind.z+" result k="+p.x+":"+p.z, MSG_DBG_ALL);
            float d = MathPoints.getDistanceBetweenFlat(pFind, p);
        sendTextMsg("    >d="+d, MSG_DBG_ALL);
            
            if (d>r) d=Math.min(Math.abs(pFind.x-p.x), Math.abs(pFind.z-p.z));// FIXME ??? почему??? но только так на метал. мап работает.
        sendTextMsg("    >d fix="+d, MSG_DBG_ALL);
        
            dLmax = Math.max(dLmax, d);
            dLavg += d;
        }
        sendTextMsg("METAL TEST PARAM. pos="+pCenter.x+":"+pCenter.z+" Avg k="+dLavg+" nApprox="+nApprox+" dLmax="+dLmax+" extractorR="+extractorR, MSG_DBG_SHORT);
        dLavg = ( dLavg / nApprox + dLmax ) / 2;
        if (dLavg<=extractorR*2) {
            isMetalFieldMap=IS_METAL_FIELD;
            sendTextMsg("This is a map with metal field. Average k="+dLavg, MSG_DLG);
        } else {
            isMetalFieldMap=IS_NORMAL_METAL;
            sendTextMsg("This is a map with normal metal spots. Near Points: "+metalspot.toString(), MSG_DLG);
        }
    }
}
/**
 * Should be call for init metal data.
 * Write info to allMetallSpots.
 */
private void initMetalData() {
    // Init metal points
    if (isMetalFieldMap==IS_METAL_FIELD || isMetalFieldMap==-2) allMetallSpots=null;
    else {
        Resource metal=clb.getResourceByName("Metal");
        if (metal==null) sendTextMsg("Resource Metal not found on this mod for initMetalData()!", MSG_ERR);
        if (allMetallSpots==null) allMetallSpots=new ArrayList<AIFloat3>();
        else return;// !!!
        if (isMetalFieldMap==IS_NORMAL_METAL) { //&& (allMetallSpots==null || allMetallSpots.isEmpty())) {
            String modName = modSpecific.modName; // last: clb.getMod().getShortName()
            boolean isZKmod=modName.equals("ZK");
//            if (!isZKmod) {
            // -- by engine --
                allMetallSpots=clb.getMap().getResourceMapSpotsPositions(metal);
              sendTextMsg("Num of metal point: "+allMetallSpots.size(), MSG_DLG);
            // -----
//            } else { // MOD SPECIFED FOR Zero-K!
//                //TODO metal for Zero-K
//            }
        }
    }    
}

private List<AIFloat3> allMetallSpots;

/**
 * return point for metal spots in radius.
 * @param center
 * @param r radius for search
 * @return list with metal spots, or empty list if non-metal map or not have metal. Null if it is metal map (-1), or map type is not set (-2).
 */
public ArrayList<AIFloat3> getMetalSpotsInRadius(AIFloat3 center, float r) {
    if (isMetalFieldMap==IS_METAL_FIELD || isMetalFieldMap==-2 || allMetallSpots==null) return null;
    ArrayList<AIFloat3> mPoints=new ArrayList<AIFloat3>();
    if (isMetalFieldMap==IS_NO_METAL) return mPoints;
    
    for (AIFloat3 p:allMetallSpots) if (MathPoints.getDistanceBetweenFlat(center, p)<=r) mPoints.add(p);
    
    return mPoints;
}
   
//TODO move getUnitResoureProduct to ModSpecification, or to AdvECO
    /**
     * Get unit resource income-usage (can work with LUA metal maker by using modSpecific.getResourceConversionFor(...)).
     * @param def unit type
     * @param res resource type
     * @return resource incoming (+) or resource using (-)
     */
    public float getUnitResoureProduct(UnitDef def, Resource res) {
        //TODO make faster!
        float resProduct = 0.0f + def.getMakesResource(res)+def.getResourceMake(res)-def.getUpkeep(res);
        
        float mapWind=clb.getMap().getCurWind();
        float defWindGenerator=def.getWindResourceGenerator(res);
        if (mapWind>0 && defWindGenerator!=0) {
            resProduct += defWindGenerator/120.0f*mapWind; // !!! test, maybe mod specifed!!
//            sendTextMsg("Wind calc for "+def.getName()+" map wind="+mapWind+" def wind res gen="+defWindGenerator+ " resProduct+="+resProduct, MSG_DBG_ALL);
        }
        
        float mapTidal=clb.getMap().getTidalStrength();
        float defTidalGenerator=def.getTidalResourceGenerator(res);
        if (mapTidal>0 && defTidalGenerator!=0) {
            resProduct += defTidalGenerator*mapTidal; // TODO TEST tidal res !!!!
//            sendTextMsg("Tidal calc for "+def.getName()+" map tid shreng="+mapTidal+" def tid res gen="+defTidalGenerator+ " resProduct+="+resProduct, MSG_DBG_ALL);
        }
        
        if (isMetalFieldMap>=0) {
            float defExtractRes=def.getExtractsResource(res);
            if (defExtractRes!=0) {  // add extract resource
                float mapResAvgExtract=clb.getMap().getExtractorRadius(res);
                double metalExSquare = mapResAvgExtract;
                    metalExSquare = metalExSquare * metalExSquare * Math.PI;
                resProduct+=defExtractRes * metalExSquare * clb.getMap().getResourceMapSpotsAverageIncome(res);
    //            sendTextMsg("Extract calc for "+def.getName()+" extr square="+metalExSquare+" map avg res income="+mapResAvgExtract+" def extract res gen="+defExtractRes+ " resProduct+="+resProduct, MSG_DBG_ALL);
            }
        }

        // - - Depends of MOD part! MMaker - -
        float modSpecRes[]=modSpecific.getResourceConversionFor(def);
        if (modSpecRes!=null) {
            if (res.equals(avgEco.resName[0])) resProduct += modSpecRes[0];
            if (res.equals(avgEco.resName[1])) resProduct +=modSpecRes[1];
        }
        // TODO In mod EvolutionRTS all metal extractor make 1 (or 0.5) metall! In Zero-K extract metal on special formuls!
        
        return resProduct;
    }   
    
    /**
     * Get unit resource income-usage (can work with LUA metal maker by using modSpecific.getResourceConversionFor(...)).
     * It work faster that getUnitResoureProduct(UnitDef, Resource) for more that 1 resource item.
     * @param def unit type
     * @return resource incoming (+) or resource using (-) array for all resources
     */
    public float[] getUnitResoureProduct(UnitDef def) {
        final float mapWind=clb.getMap().getCurWind();
        final float mapTidal=clb.getMap().getTidalStrength();
        
        final float modSpecRes[]=modSpecific.getResourceConversionFor(def); // Depends of MOD part! MMaker

        float resProduct[] = new float[avgEco.resName.length];
        for (int r=0; r<resProduct.length; r++)
        {
            Resource res=avgEco.resName[r];
            resProduct[r] = 0.0f + def.getMakesResource(res)+def.getResourceMake(res)-def.getUpkeep(res);

            float defWindGenerator=def.getWindResourceGenerator(res);
            if (mapWind>0 && defWindGenerator!=0) {
                resProduct[r] += defWindGenerator/120.0f*mapWind; // !!! test, maybe mod specifed!!
    //            sendTextMsg("Wind calc for "+def.getName()+" map wind="+mapWind+" def wind res gen="+defWindGenerator+ " resProduct+="+resProduct, MSG_DBG_ALL);
            }
        
            float defTidalGenerator=def.getTidalResourceGenerator(res);
            if (mapTidal>0 && defTidalGenerator!=0) {
                resProduct[r] += defTidalGenerator*mapTidal; // TODO TEST tidal res !!!!
    //            sendTextMsg("Tidal calc for "+def.getName()+" map tid shreng="+mapTidal+" def tid res gen="+defTidalGenerator+ " resProduct+="+resProduct, MSG_DBG_ALL);
            }
        
            if (isMetalFieldMap>=0) {
                float defExtractRes=def.getExtractsResource(res);
                if (defExtractRes!=0) {  // add extract resource
                    float mapResAvgExtract=clb.getMap().getExtractorRadius(res);
                    double metalExSquare = mapResAvgExtract;
                        metalExSquare = metalExSquare * metalExSquare * Math.PI;
                    resProduct[r]+=defExtractRes * metalExSquare * clb.getMap().getResourceMapSpotsAverageIncome(res);
        //            sendTextMsg("Extract calc for "+def.getName()+" extr square="+metalExSquare+" map avg res income="+mapResAvgExtract+" def extract res gen="+defExtractRes+ " resProduct+="+resProduct, MSG_DBG_ALL);
                }
            }
            
            if (modSpecRes!=null) resProduct[r] += modSpecRes[r]; // Depends of MOD part! MMaker
            // TODO In mod EvolutionRTS all metal extractor make 1 (or 0.5) metall! In Zero-K extract metal on special formuls!
        }
        
        return resProduct;
    }    

// ==================================

private boolean inGameInitDone=false; // post-start initialization pass mark

/**
 * Execute tech up.
 * @param bestLvl - to level
 * @param onBase - where base will be do execute this tech up. Maybe null.
 * @return tech up execute now - true, or not - false
 */
public boolean doTechUpToLevel(TTechLevel bestLvl, TBase onBase) {
    //float t=ecoStrategy.getRashotBuildTimeLst(bestLvl.needBaseBuilders, avgEco.getAVGResourceToArr(), base.getBuildPower(false, true));
    //sendTextMsg("Start tech Up from to > time="+t+" level="+bestLvl.toString(), FieldBOT.MSG_DBG_SHORT);
    if (onBase==null) { // search base
        for (TBase base:selectTBasesList()) {
            if (bestLvl.byMorph()) {
              if (base.contains(bestLvl.byMorph)) onBase=base;
            } else {
                HashSet<UnitDef> bBldLst=base.getBuildList(true);
                for (UnitDef def:bestLvl.needBaseBuilders) if (bBldLst.contains(def))
                {
                  onBase=base;
                  break;
                }
            }
            if (onBase!=null) break;
        }
    }
    if (onBase==null) return false;
    
    if (bestLvl.byMorph()) {
        // через morph
        if (bestLvl.morphTo!=null) sendTextMsg(" Morphing "+bestLvl.byMorph.getDef().getHumanName()+" to "+bestLvl.morphTo.getHumanName(), FieldBOT.MSG_DLG);
        else sendTextMsg("  error morphTo (getUnitDefForMprph) is null!", FieldBOT.MSG_ERR);
        bestLvl.byMorph.executeCustomCommand(bestLvl.morphCMD.getId(), new ArrayList<Float>(), (short)0, Integer.MAX_VALUE);

        for (UnitDef def:bestLvl.needBaseBuilders) if (def!=bestLvl.morphTo) onBase.currentBaseTarget.add(def, 1);
    } else {
        // простой вариант
        for (UnitDef def:bestLvl.needBaseBuilders) onBase.currentBaseTarget.add(def, 1);
    }
    onBase.currentBaseTarget.needPrecissionRes=true; //!!!
    
    if (techUpTrigger!=null) techUpTrigger=null; //!!!
    
    return true;
}

/**
 * Check and do tech up
 * @param doEmidetly execute now - do not check time.
 * @param test only test- do not execute, only print to chat.
 * @return  tech up execute now - true, or not - false
 */
public boolean checkAndTechUp(boolean doEmidetly, boolean test) { // TODO ПЕРЕДЕЛАТЬ и доделать!
    boolean startTechUp=false;
    sendTextMsg("Check for Tech Up level", FieldBOT.MSG_DBG_SHORT);
    
    float currentRes[][];
    if (ecoStrategy.useOptimisticResourceCalc) currentRes= avgEco.getSmartAVGResourceToArr();
    else currentRes= avgEco.getAVGResourceToArr();
    // TODO test. Было getAVGResourceToArr()
    
    for (TBase base:selectTBasesList()) if (base.currentBaseTarget.isEmpty()) {
        
        HashSet<UnitDef> onBaseBuildLst=base.getBuildList(true);
        
        //float currentLvlK=TTechLevel.calcEcoK(base.getBuildList(true), this); // текущий уровень
        ArrayList<TTechLevel> TechLvls=TTechLevel.GetAllVariantLevel( base.getContainUnitDefsList(false),onBaseBuildLst ,base, this);
        
        ArrayList<TTechLevel> TechLvlsByMorph=TTechLevel.GetAllVariantLevelByMorph(base.getAllUnits(false), base.getContainUnitDefsList(false),onBaseBuildLst ,base, this);
        sendTextMsg(" Tech level by morph count="+TechLvlsByMorph.size(), MSG_DBG_SHORT);
        TechLvls.addAll(TechLvlsByMorph);
        
        TTechLevel bestLvl=null;
        
        float bestEco[]=TTechLevel.getMaxResProduct(onBaseBuildLst, this); // текущий уровень
        //float bestEcoK=0.0f;
        float bestBuilderF=TTechLevel.getMaxBuildPower(onBaseBuildLst);
        
        bestLvl = null;
        if ( !doEmidetly ) {
            boolean allowTechUp=true;
            switch (useTechUpMode) {
                case 0:
                    bestLvl = ecoStrategy.selectLvl_bestForNow(TechLvls, base, bestEco, bestBuilderF, currentRes);
                    if (bestLvl!=null)
                        allowTechUp = ecoStrategy.isActualDoTechUp_fastCheck(base, onBaseBuildLst, bestLvl);
                    else allowTechUp=false;
                    break;
                case 1:
                    bestLvl = ecoStrategy.selectLvl_bestForNow(TechLvls, base, bestEco, bestBuilderF, currentRes);
                    if (bestLvl!=null)
                        techUpTrigger = ecoStrategy.getPrecissionTechUpTimeFor(base, onBaseBuildLst, bestLvl );
                    else techUpTrigger = null;
                     if (techUpTrigger!=null) {
                         sendTextMsg("> "+techUpTrigger.toString(), FieldBOT.MSG_DBG_SHORT);
                         allowTechUp=techUpTrigger.trigger();
                         // TODO where remove it tech up was execute?
                     } else {
                         sendTextMsg("> tech up trigger is null!", FieldBOT.MSG_DBG_SHORT);
                         allowTechUp=false;
                     }
                    break;
                case 2:
                    //ArrayList<TTechLevel> TechLvls_eco=ecoStrategy.selectLvl_whoBetterThat(TechLvls, bestEco, bestBuilderF); // TODO test, maybe 'selectLvl_whoBetterThat' remove realy better level.
                    techUpTrigger = ecoStrategy.getAbsolutePrecissionTechUpTimeFor(base, onBaseBuildLst, TechLvls);
                     if (techUpTrigger!=null) {
                         sendTextMsg("> "+techUpTrigger.toString(), FieldBOT.MSG_DBG_SHORT);
                         allowTechUp=techUpTrigger.trigger();
                         bestLvl=techUpTrigger.level;
                     } else {
                         sendTextMsg("> tech up trigger is null!", FieldBOT.MSG_DBG_SHORT);
                         allowTechUp=false;
                     }
                    break;
            }
            if (!allowTechUp) bestLvl=null;
        }

        if (bestLvl!=null) { // Execute tech up
            float t=ecoStrategy.getRashotBuildTimeLst(bestLvl.needBaseBuilders, avgEco.getAVGResourceToArr(), base.getBuildPower(false, true));
            sendTextMsg("Start tech Up from to > time="+t+" level="+bestLvl.toString(), FieldBOT.MSG_DBG_SHORT);
            
            startTechUp=true;
            if (!test)
            {
                startTechUp = doTechUpToLevel(bestLvl,base);
            }
        }
    } else sendTextMsg("Base already have work."+base.toString(), FieldBOT.MSG_DBG_SHORT);
    // TODO только на 1 базе.
    return startTechUp;
}

public TBase getNearBase(AIFloat3 nearTo) {
    TBase nearB=null;
    float minL=Float.POSITIVE_INFINITY;
    for (TBase base:selectTBasesList()) {
        float l=MathPoints.getDistanceBetweenFlat(base.center, nearTo)-base.radius;
        if (nearB==null || l<minL) {
            minL=l;
            nearB=base;
        }
    }
    return nearB;
}

public AGroupManager getNearGroup(AIFloat3 nearTo) {
    AGroupManager nearG=null;
    float minL=Float.POSITIVE_INFINITY;
    for (AGroupManager group:smartGroups) {
        AIFloat3 gPos=group.getCenter();
        if (gPos!=null) {
            float l=MathPoints.getDistanceBetweenFlat(gPos, nearTo);
            if (nearG==null || l<minL) {
                minL=l;
                nearG=group;
            }
        }
    }
    return nearG;
}

/**
 * Create new base and send to new base constructor unit from all other base.
 * @param newBasePoint base center
 * @param workerUsagePercent how many workers take from other base to send on new base. Recomended value: 0.1-0.4; if 0 then defauld=2/5.
 * @return return new base, or null if failed
 */
public TBase spawnNewBase(AIFloat3 newBasePoint, float workerUsagePercent)
{
    // Check on free position from other TBases
    TBase otherBase=getNearBase(newBasePoint);
    if (otherBase!=null) {
        if (MathPoints.getDistanceBetweenFlat(otherBase.getCenter(), newBasePoint) <= otherBase.radius) // TODO plas second radius
            return null; // base collision
    }
    
    if (workerUsagePercent<=0) workerUsagePercent=2/5;
    
    ArrayList<Unit> shareWorker= new ArrayList<Unit>();
    for (TBase base:selectTBasesList()) { // TODO only from bases? Maybe from other group too?
        // TODO ...
        ArrayList<Unit> baseCons=new ArrayList<Unit>(base.idleCons);
        baseCons.addAll(base.workingCons);// !!!
        Iterator<Unit> itr1=baseCons.iterator();
        while (itr1.hasNext()) {
            Unit aCons = itr1.next();
            UnitDef cDef=aCons.getDef();
            if (!ModSpecification.isRealyAbleToMove(cDef)
                || cDef.isCommander()
                || aCons.isParalyzed()
                || !ModSpecification.isAbleToBuildBase(cDef) // So, they can build new base?
                ) itr1.remove(); // Если нельзя переместить на новую базу
        }
        // TODO select only fast unit
        if (baseCons.size()>1) {
            HashSet<UnitDef> baseCanBuild=base.getBuildList(true);
            for (int i=0; i<Math.max(1,baseCons.size()*workerUsagePercent); i++)
            {
                Unit unit=baseCons.get(i);
                base.removeUnit(unit);
                shareWorker.add(unit);

                UnitDef uDef=unit.getDef(); // Пополнение затрат
                if (baseCanBuild.contains(uDef)) base.currentBaseTarget.add(uDef, 1);
            }
        }
    }
    if (!shareWorker.isEmpty()) {
        TBase newB=new TBase(this, newBasePoint, 0); // 0 - auto size, default = 1000
        addSmartGroup(newB);
        for (Unit u:shareWorker) { // !!!
            newB.addUnit(u); // Добавить и пойти на базу
            u.moveTo(MathPoints.getRandomPointOnRadius(newB.center, newB.radius/2), (short)0, Integer.MAX_VALUE); //, DEFAULT_TIMEOUT);
        }
        newB.setAllAsWorking(shareWorker); // Пока не дойдут пометить как работающих
        //sendTextMsg("New base created "+newB.toString()+", "+shareWorker+" units sending to new base.", FieldBOT.MSG_DBG_SHORT);
        return newB;
    } else {
        //sendTextMsg("Can't create base: no enought free unit.", FieldBOT.MSG_DBG_SHORT);
        return null;
    }            
}

/**
 * Find base with maximal UnitDefs can build
 * @param findWithoutBaseTarget find only idle base
 * @return best base, or null
 */
public TBase getBestBase(boolean findWithoutBaseTarget) {
    TBase bestBase=null; HashSet<UnitDef> lstOfUDefs=new HashSet<UnitDef>();
    for (TBase base:selectTBasesList()) {
        HashSet<UnitDef> tmpUDefs=base.getBuildList(true);
        if (tmpUDefs.size()>lstOfUDefs.size() &&
              (!findWithoutBaseTarget || !base.haveSpecialCommand())) // idle base. 
        {
            bestBase=base;
            lstOfUDefs=tmpUDefs;
        }
    }
    return bestBase;
}

/**
 * Set group for remove. It is safe for call from cycle FOR and iteratrs. Real removing group from list will be realized on update.
 * @param group 
 */
public void removeSmartGroup(AGroupManager group) {
    if (!smartGroupsToRemove.contains(group)) {
        sendTextMsg("Group removing:"+group, FieldBOT.MSG_DBG_SHORT);
        smartGroupsToRemove.add(group);
    }
}
public void addSmartGroup(AGroupManager group) {
    smartGroups.add(group);
    //bases.add(group);
}

public AGroupManager getOwnerGroup(Unit unit) {
    for (AGroupManager agm:smartGroups) if (agm.contains(unit)) return agm;
    return null;
}

/**
 * This method call from groups, when they finish some target.
 * @param group 
 */
public void onGroupTargetFinished(AGroupManager group) {
    // TODO onGroupTargetFinished
    // send next to Eco and War strategy calls.
}

/**
 * Select TBase list from smartGroup list
 */
public ArrayList<TBase> selectTBasesList() {
    ArrayList<TBase> basesList=new ArrayList<TBase>();
    for (AGroupManager sGroup:smartGroups) 
        if (sGroup instanceof TBase) basesList.add((TBase)sGroup);
    return basesList;
}

@Override
public int update(int frame) {
  try {
  cpuTimer.start();
    currentFrime=frame;
//    sendTextMsg("update frame="+frame, MSG_DBG_ALL);
      
    if (!deadLstUnit.isEmpty()) deadLstUnit.clear();
    if (!smartGroupsToRemove.isEmpty()) { // TODO test.
        for (AGroupManager group:smartGroupsToRemove) { // send units to other groups
            List<Unit> freeUnits=group.getAllUnits(false);
            // TODO what doing with units?
            for (Unit u:freeUnits) addToSameGroup(u);
        }

        if (smartGroups.removeAll(smartGroupsToRemove)) smartGroupsToRemove.clear();
        //TODO don't forget call removeSmartGroup(null) if add some methods/cashes for calling on this function;
    }
      
    avgEco.update(frame);
    
  if (!inGameInitDone) {
        //this method move to init() : if (isMetalFieldMap==-2) checkForMetal(); // test for metal!!!
        // initMetalData(); // !!!
        inGameInitDone=true;

        Mod mod=clb.getMod();
        sendTextMsg("MOD info. Name: "+mod.getHumanName()+" short name:"+mod.getShortName()+" version:"+mod.getVersion()+" description:"+mod.getDescription(), MSG_DBG_SHORT);
        sendTextMsg("ENGINE: "+clb.getEngine().getVersion().getFull(), MSG_DBG_SHORT);
        
        for (TBase base:selectTBasesList()) base.reinitCenter(); // !!! bug fix with start position for start boxes

  // DEBUG: SHOW ALL UNITS INFO!
        // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! Печать списка всех юнитов
//        sendTextMsg("---- UnitDef list (ALL "+modSpecific.allUnitDefs.size()+") ----", FieldBOT.MSG_DBG_ALL);
//        for (UnitDef def:modSpecific.allUnitDefs) {
//            sendTextMsg("ID="+def.getUnitDefId()+" "+def.getName()+" ("+def.getHumanName()+") level "+def.getTechLevel()+" isBuilder="+def.isBuilder(), FieldBOT.MSG_DBG_ALL);
//            showUnitDefInfoDebug(def); // some other shows
//            sendTextMsg("",FieldBOT.MSG_DBG_ALL);
////            if (def.getCustomParams()!=null && !def.getCustomParams().isEmpty()) { //SEGMENTATION FAULED in Spring 96, 94.1
////                sendTextMsg(" CustomParams:", FieldBOT.MSG_DBG_ALL);
////                for (Map.Entry<String, String> params :def.getCustomParams().entrySet()) {
////                    sendTextMsg(" K:"+params.getKey()+" V:"+params.getValue(), FieldBOT.MSG_DBG_ALL);
////                }
////            }
//        }
//        sendTextMsg("----  ----", FieldBOT.MSG_DBG_ALL);
        // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
          
  } else {

    talkingDialogModule.update(frame);
    scoutModule.update(frame);
    // TODO spawn new base, send group for kame mex point if mapType=normal metal map. ecoStrategy.update(frame);
    warStrategy.update(frame); // Army control
    
    
    for (AGroupManager sGroup:smartGroups) sGroup.update(frame);
    
    // --- Zero-K special ---
    if (modSpecific.firstFactoryIsFree && frame>20) {
        boolean aceptBuildFactory=false;
        aceptBuildFactory = checkAndTechUp(true, false);
        if (!aceptBuildFactory) {
            talkingDialogModule.message(clb.getSkirmishAI().getSkirmishAIId(), "bot make Bot Factory"); // ZK: Gunship Plant, Cloaky Bot Factory, Light Vehicle Factory, Heavy Tank Factory, Amphibious Operations Plant, Shipyard
            aceptBuildFactory=true;
            //TODO make better!
            //TODO checkAntDechUp_armyTechUp()
//            TBase bestBase=getBestBase(true);
//            if (bestBase!=null) {
//                bestBase.
//            }
        }
        if (aceptBuildFactory) modSpecific.firstFactoryIsFree=false;
    }
    // --- ---
    
    // ======= Test Tech Up ==========
    if (autoTechUp){
        if (useTechUpMode>0) { // smart tech up
            boolean techUpCheckTick=false;
            if (frame%1500==1200) {
                checkAndTechUp(false, false);
                techUpCheckTick=true;
            } else if (frame%500==400) {
                if (techUpTrigger==null) checkAndTechUp(false, false);
                techUpCheckTick=true;
            }
            if ( techUpCheckTick && techUpTrigger!=null ) {
                if (techUpTrigger.trigger()) {
                    if (doTechUpToLevel(techUpTrigger.level, techUpTrigger.onBase))
                        techUpTrigger=null;//!!!
                }
            }
        } else { // fast check tech up
            if (frame%1000==500)
              checkAndTechUp(false, false); // old tech up method, bad time choose.
        }
    }
    // ===============================
        
  }

    
//    sendTextMsg("done update.", MSG_DBG_ALL);
  cpuTimer.stop();
  } catch (Exception e) {
     printStackTrakeToChat("update",e);
     cpuTimer.stop();
      return -1;
  }

    return 0; // signaling: OK
}

/**
 * Add unit to near base
 * @param unit unit without owner base
 * @return add to base, or false - not added.
 */
public boolean addToSameGroup(Unit unit) {
    sendTextMsg("Unit from uncknown owner base add to base, ... try...", MSG_DBG_ALL);
    if (unit.getTeam()!=teamId) { // !!! DEBUG!
        sendTextMsg("unit not in AI team!", MSG_DBG_SHORT);
        return false;
    }
    if (deadLstUnit.contains(unit)) {
        sendTextMsg("unit in deadLstUnit! Error!", MSG_DBG_SHORT);
        return false;
    }
    // TODO check on dead/sharing list
    
   AGroupManager nearBase=null; float bestK=Float.NEGATIVE_INFINITY;//float bestDist=Float.POSITIVE_INFINITY;
   for (AGroupManager sGroup:smartGroups) if (!smartGroupsToRemove.contains(sGroup)) {
       //float l = TBase.getDistanceBetween(unit.getPos(), base.center);
       float k=sGroup.doYouNeedUnit(unit);
       if (k>bestK) {
           bestK=k;
           nearBase=sGroup;
       }
   }
   if (nearBase!=null) {
       sendTextMsg("Unit from uncknown owner base add to base, ... add...", MSG_DBG_ALL);
       return nearBase.addUnit(unit);
   } else {
       sendTextMsg("Base not found for add unit!", MSG_DBG_SHORT);
       return false;
   } 
}

/**
 * For register unit, tech level. Only bot unit.
 */
private void onAddUnit(Unit unit) {
    if (!unit.isBeingBuilt()) {
        UnitDef def=unit.getDef();
        if (def.isCommander()) commanders.add(unit);
        
        modSpecific.onAddUnit(def); // TODO show debug message about unlock technology
        
        int uTLvl = def.getTechLevel();
        Integer foundNLvl=techLevels.get(uTLvl);
        if (foundNLvl==null) {
            sendTextMsg("Now AI have new Tech Up to "+uTLvl+" tech level! (onAddUnit)", MSG_DLG);
            techLevels.put(uTLvl, 1);
        } else {
            techLevels.put(uTLvl, foundNLvl+1); // increase N of units
        }
        
    }
}
/**
 * For register unit, tech level. Only bot unit.
 */
private void onLostUnit(Unit unit) {
    if (!unit.isBeingBuilt()) {
        UnitDef def=unit.getDef();
        if (def.isCommander()) commanders.remove(unit);

        modSpecific.onRemoveUnit(def); // TODO show debug message about lock technology
        
        int uTLvl = def.getTechLevel();
        Integer foundNLvl=techLevels.get(uTLvl);
        if (foundNLvl==null) {
            sendTextMsg("Error: unit lost with unregister tech level "+uTLvl+"! (onLostUnit)", MSG_ERR);
        } else {
            if (foundNLvl>1) techLevels.put(uTLvl, foundNLvl-1); // decrease N of unit
            else {
                techLevels.remove(uTLvl);
                sendTextMsg("Now AI have lost last unit with "+uTLvl+" tech level! (onLostUnit)", MSG_DLG);
            }
        }
    }
}


@Override
public int message(int player, String message) {
  try {
  cpuTimer.start();
    if (talkingDialogModule!=null) talkingDialogModule.message(player, message);
    else sendTextMsg("ERROR message: talkingDialogModule is null (not initialize).", MSG_ERR);
   cpuTimer.stop();
  } catch (Exception e) {
     printStackTrakeToChat("message",e);
     cpuTimer.stop();
      return -1;
  }
    return 0; // signaling: OK
}



    @Override
    public int luaMessage(java.lang.String inData){  
  try {
  cpuTimer.start();
        sendTextMsg("on luaMessage inData="+inData+"", MSG_DBG_SHORT);
// This function was taking and owerwrite from https://github.com/Anarchid/zkgbai/blob/master/src/zkgbai/graph/GraphManager.java#L83 (code from (C) Anarchid)
        final String DATA_METAL_MAKR="METAL_SPOTS:";
    	if(inData.startsWith(DATA_METAL_MAKR))
        {
            String json = inData.substring(DATA_METAL_MAKR.length());
            JsonParserFactory factory=JsonParserFactory.getInstance();
            JSONParser parser=factory.newJsonParser();
            ArrayList<HashMap> jsonData=(ArrayList)parser.parseJson(json).values().toArray()[0];
            
            //if (allMetallSpots==null) 
                allMetallSpots=new ArrayList<AIFloat3>();
            for (HashMap s:jsonData){
                float x = Float.parseFloat((String)s.get("x"));
                float y = Float.parseFloat((String)s.get("y"));
                float z = Float.parseFloat((String)s.get("z"));
                float m = Float.parseFloat((String)s.get("metal"));
                AIFloat3 metalP=new AIFloat3(x, y, z); // TODO use m too
                allMetallSpots.add(metalP);
            }
            sendTextMsg("Parsed JSON metalmap with "+allMetallSpots.size()+" spots", MSG_DBG_SHORT);
            
            // TODO start boxes
    	}
        //*/
   cpuTimer.stop();
  } catch (Exception e) {
     printStackTrakeToChat("luaMessage",e);
     cpuTimer.stop();
      return -1;
  }
    	return 0; //signaling: OK
    }






public void showUnitDefInfoDebug(UnitDef unit){
    sendTextMsg("Unit info: " + unit.getName()+" humanName: "+unit.getHumanName()+" techLevel: "+unit.getTechLevel()+" defID: "+unit.getUnitDefId(), MSG_DBG_SHORT);
    sendTextMsg("Unit is: AbleToAssist " + unit.isAbleToAssist()+" AbleToMove "+unit.isAbleToMove()+" Assistable "+unit.isAssistable(), MSG_DBG_SHORT);
    sendTextMsg("build: BuildDistance " + unit.getBuildDistance()+", BuildSpeed "+unit.getBuildSpeed()+"; moving speed="+unit.getSpeed(), MSG_DBG_SHORT);
    
    for (Resource res:clb.getResources())
        sendTextMsg("For resource "+res.getName()+" info: "+ 
            " def.getResourceMake(r)" + unit.getResourceMake(res) +
            " def.getMakesResource(r)" + unit.getMakesResource(res) +
            " def.getExtractsResource(r): " + unit.getExtractsResource(res) +
            " def.getTidalResourceGenerator(r): " + unit.getTidalResourceGenerator(res) +
            " def.getWindResourceGenerator(r): " + unit.getWindResourceGenerator(res) +
            " def.getResourceExtractorRange(r): " + unit.getResourceExtractorRange(res) +
            " def.getUpkeep(r): " + unit.getUpkeep(res) +
            " def.getCost(r): " + unit.getCost(res) +
            " def.getStorage(r): " + unit.getStorage(res) +
            "ResourceSumm(+map&mod specific)="+getUnitResoureProduct(unit, res)    , MSG_DBG_ALL);
    List<UnitDef> buildOptions = unit.getBuildOptions();
    for (UnitDef unitDef : buildOptions) {
        sendTextMsg(" build: " + unitDef.getName() + "\t" + unitDef.getHumanName() + "\t" + unitDef.toString() + "\t" + unitDef.hashCode(), MSG_DBG_ALL);
    }
    
    // Version specific:
//    sendTextMsg(" Engine version: '"+clb.getEngine().getVersion().getMajor()+"'" , MSG_DBG_ALL);
//    // Spring 96 and later code only, or crash:
//    if (true) { // DEBUG ONLY!!!
//        java.util.Map customP = unit.getCustomParams();
//        sendTextMsg(" Custom params size = "+customP.size() , MSG_ERR);
//        sendTextMsg(" Custom params data = "+customP.toString() , MSG_ERR);
//    }
}
public void showUnitInfoDebug(Unit unit){
    showUnitDefInfoDebug(unit.getDef());
//    List <ModParam> mParam=unit.getModParams();
//    sendTextMsg("Mod param count "+mParam.size(), MSG_DBG_ALL);
//    for (ModParam mP : mParam) {
//        sendTextMsg("param x: " + mP.getName() + " value: " + mP.getValue() + "\t" + mP.toString() + "\t getUnit=" + mP.getUnit().getDef().getName()+" : "+mP.getUnit().getDef().getHumanName(), MSG_DBG_ALL);
//    }
    List<CommandDescription> cmbList = unit.getSupportedCommands();
    sendTextMsg("Support command count "+cmbList.size(), MSG_DBG_ALL);
    for (CommandDescription cmd : cmbList) {
        sendTextMsg(" command name: " + cmd.getName() + " ToolTip " + cmd.getToolTip() + " ID=" + cmd.getId()+ "\t supportedCommand="+cmd.getSupportedCommandId()+" isDisabled:"+cmd.isDisabled() +" isShowUnique:"+cmd.isShowUnique(), MSG_DBG_ALL);
        if ((cmd.getId() >= 31000) && (cmd.getId() < 32000)) {
            sendTextMsg("   (MORPH command detected! cmdID="+cmd.getId()+")" , MSG_DBG_SHORT);
        }
        //TODO test call  cmd.getParams() on spring 97!!!
//        List<String> cmdPs=cmd.getParams(); // this call can CRASH SPRING engine on version 94 and 96.
//        if (cmdPs!=null) for (String p:cmdPs) sendTextMsg("  >cmd param: " + p, MSG_DBG_ALL);
    }
}

@Override
public int unitCreated(Unit unit, Unit builder) {
    // TODO team check !!!
   cpuTimer.start();
    int ret = -1;
  try {
    ret = sendTextMsg("unitCreated: " + unit.getDef().getName(), MSG_DBG_SHORT);
//  showUnitInfoDebug(unit); // DEBUG!
    onAddUnit(unit);
    //onFreeBuildingUnit(unit);
    boolean foundHomeBase=false;
    for (AGroupManager sGroup:smartGroups) if (!smartGroupsToRemove.contains(sGroup)) {
        sGroup.unitCreated(unit, builder);
        if (sGroup.contains(unit)) foundHomeBase=true;
    }
    if (!foundHomeBase) addToSameGroup(unit);
//    sendTextMsg("done unitCreated.", MSG_DBG_ALL);
   cpuTimer.stop();
  } catch (Exception e) {
     printStackTrakeToChat("unitCreated",e);
     cpuTimer.stop();
      return -1;
  }
return ret;
}

@Override
public int unitFinished(Unit unit) {
  cpuTimer.start();
  try {
//    sendTextMsg("unitFinished ", MSG_DBG_SHORT);
    if (deadLstUnit.contains(unit)) { // !!! DEBUG! TEST!
        sendTextMsg("unitFinished: unit in deadLstUnit!", MSG_DBG_SHORT);
      cpuTimer.stop();
        return 0;
    }
    onAddUnit(unit);
    //onFreeBuildingUnit(unit);
    boolean foundHomeBase=false;
    for (AGroupManager sGroup:smartGroups) if (!smartGroupsToRemove.contains(sGroup)) {
        sGroup.unitFinished(unit);
        if (sGroup.contains(unit)) foundHomeBase=true;
    }
    if (!foundHomeBase) addToSameGroup(unit);
    
//    sendTextMsg("done unitFinished.", MSG_DBG_ALL);
  } catch (Exception e) {
     printStackTrakeToChat("unitFinished",e);
     cpuTimer.stop();
      return -1;
  }
  cpuTimer.stop();
    return 0; // signaling: OK
}

@Override
public int unitIdle(Unit unit) {
  cpuTimer.start();
  try {
//    sendTextMsg("unitIdle ", MSG_DBG_SHORT);
    if (deadLstUnit.contains(unit)) { // TEST!
        sendTextMsg("unitIdle: unit in deadLstUnit!", MSG_DBG_SHORT);
     cpuTimer.stop();
        return 0;
    }

    boolean foundHomeBase=false;
    for (AGroupManager sGroup:smartGroups) {
        sGroup.unitIdle(unit);
        if (sGroup.contains(unit)) foundHomeBase=true; // TODO если ни одна база не приняла его?
    }
    if (!foundHomeBase) addToSameGroup(unit);
    
//    sendTextMsg("done unitIdle.", MSG_DBG_ALL);
  } catch (Exception e) {
     printStackTrakeToChat("unitIdle",e);
    cpuTimer.stop();
      return -1;
  }
  cpuTimer.stop();
    return 0; // signaling: OK
}

@Override
public int unitMoveFailed(Unit unit) {
  cpuTimer.start();
  try {
//    sendTextMsg("unitMoveFailed ", MSG_DBG_SHORT);
    if (deadLstUnit.contains(unit)) { // !!! DEBUG! TEST!!!
        sendTextMsg("unitMoveFailed: unit in deadLstUnit!", MSG_DBG_SHORT);
      cpuTimer.stop();
        return 0;
    }
    
    boolean foundHomeBase=false;
    for (AGroupManager sGroup:smartGroups) {
        sGroup.unitMoveFailed(unit);
        if (sGroup.contains(unit)) foundHomeBase=true;
    }
    if (!foundHomeBase) addToSameGroup(unit);
//    sendTextMsg("done unitMoveFailed.", MSG_DBG_ALL);
  } catch (Exception e) {
     printStackTrakeToChat("unitMoveFailed",e);
     cpuTimer.stop();
      return -1;
  }
  cpuTimer.stop();
    return 0; // signaling: OK
}

@Override
public int unitDamaged(Unit unit, Unit attacker, float damage, AIFloat3 dir, WeaponDef weaponDef, boolean paralyzed) {
    return 0; // signaling: OK
}

@Override
public int unitDestroyed(Unit unit, Unit attacker) {
  cpuTimer.start();
  try {
//    sendTextMsg("unitDestroyed: "+unit.getDef().getName(), MSG_DBG_SHORT);
    deadLstUnit.add(unit);// TODO !!!Test
    onLostUnit(unit);
    boolean foundHomeBase=false;
    for (AGroupManager sGroup:smartGroups) { // send signal
        if (sGroup.contains(unit)) foundHomeBase=true;
        sGroup.unitDestroyed(unit,attacker);
        // TODO break?
    }
//    sendTextMsg("done unitDestroyed.", MSG_DBG_ALL);
  } catch (Exception e) {
     printStackTrakeToChat("unitDestroyed",e);
  cpuTimer.stop();
      return -1;
  }
  cpuTimer.stop();
    return 0; // signaling: OK
}

@Override
public int unitGiven(Unit unit, int oldTeamId, int newTeamId) {
  cpuTimer.start();
  try {
//    sendTextMsg("unitGiven from "+oldTeamId+" to "+newTeamId, MSG_DBG_SHORT);
//    showUnitInfoDebug(unit); // for DEBUG!
    
    // TODO do need  onLostUnit(unit) ?

    boolean foundHomeBase=false;
    for (AGroupManager sGroup:smartGroups) { // send signal
        //if (base.contains(unit)) foundHomeBase=true; // TODO ...
        boolean baseR=sGroup.unitGiven(unit, oldTeamId, newTeamId);
        if (baseR) foundHomeBase=true;
        //if (base.contains(unit)) foundHomeBase=true;
    }

    if (oldTeamId==teamId && newTeamId!=teamId) { // out from AI
        deadLstUnit.add(unit);// TODO !!!Test
        for (AGroupManager sGroup:smartGroups) sGroup.removeUnit(unit); // remove from all list...
    }
    
    if (oldTeamId!=teamId && newTeamId==teamId) { // come to AI
        if (!foundHomeBase) {
            //if (newTeamId==this.teamId) 
            addToSameGroup(unit);
        }

        onAddUnit(unit);
    }
    
//    sendTextMsg("done unitGiven.", MSG_DBG_ALL);
  } catch (Exception e) {
     printStackTrakeToChat("unitGiven",e);
  cpuTimer.stop();
      return -1;
  }
  cpuTimer.stop();
    return 0; // signaling: OK
}

@Override
public int unitCaptured(Unit unit, int oldTeamId, int newTeamId) {
  cpuTimer.start();
    sendTextMsg("TODO unitCaptured from "+oldTeamId+" to "+newTeamId, MSG_DBG_SHORT);
  try {
    if (oldTeamId==teamId && newTeamId!=teamId) { // out from AI control
        deadLstUnit.add(unit);
        onLostUnit(unit);
        for (AGroupManager sGroup:smartGroups) sGroup.removeUnit(unit); // remove from all list.
    }
    // TODO captured to AI.
//    sendTextMsg("done unitCaptured.", MSG_DBG_ALL);
  } catch (Exception e) {
     printStackTrakeToChat("unitCaptured",e);
  cpuTimer.stop();
      return -1;
  }
  cpuTimer.stop();
    return 0; // signaling: OK
}

@Override
public int enemyEnterLOS(Unit enemy) {
  try {
  cpuTimer.start();
        if (enemy==null) sendTextMsg("WTF ETTOR: enemyEnterLOS enemy==null", FieldBOT.MSG_ERR);
        else {
            scoutModule.enemyEnterLOS(enemy);
        }
   cpuTimer.stop();
  } catch (Exception e) {
      printStackTrakeToChat("enemyEnterLOS",e);
     cpuTimer.stop();
      return -1;
  }
    return 0; // signaling: OK
}

@Override
public int enemyLeaveLOS(Unit enemy) {
  try {
  cpuTimer.start();
        if (enemy==null) sendTextMsg("WTF ETTOR: enemyLeaveLOS enemy==null", FieldBOT.MSG_ERR);
        else {
            scoutModule.enemyLeaveLOS(enemy);
        }
   cpuTimer.stop();
  } catch (Exception e) {
      printStackTrakeToChat("enemyLeaveLOS",e);
     cpuTimer.stop();
      return -1;
  }    return 0; // signaling: OK
}

@Override
public int enemyEnterRadar(Unit enemy) {
  try {
  cpuTimer.start();
        if (enemy==null) sendTextMsg("WTF ETTOR: enemyEnterRadar enemy==null", FieldBOT.MSG_ERR);
        else {
            scoutModule.enemyEnterRadar(enemy);
        }
   cpuTimer.stop();
  } catch (Exception e) {
      printStackTrakeToChat("enemyEnterRadar",e);
     cpuTimer.stop();
      return -1;
  }
    return 0; // signaling: OK
}

@Override
public int enemyLeaveRadar(Unit enemy) {
  try {
  cpuTimer.start();
        if (enemy==null) sendTextMsg("WTF ETTOR: enemyLeaveRadar enemy==null", FieldBOT.MSG_ERR);
        else {
            scoutModule.enemyLeaveRadar(enemy);
        }
   cpuTimer.stop();
  } catch (Exception e) {
      printStackTrakeToChat("enemyLeaveRadar",e);
     cpuTimer.stop();
      return -1;
  }
  return 0; // signaling: OK
}

@Override
public int enemyDamaged(Unit enemy, Unit attacker, float damage, AIFloat3 dir, WeaponDef weaponDef, boolean paralyzed) {
 try {
  cpuTimer.start();
        if (enemy==null) sendTextMsg("WTF ETTOR: enemyDamaged enemy==null, attacker=="+attacker, FieldBOT.MSG_ERR);
        else {
            scoutModule.enemyDamaged(enemy, attacker, damage, dir, weaponDef, paralyzed);
        }
   cpuTimer.stop();
  } catch (Exception e) {
      printStackTrakeToChat("enemyDamaged",e);
     cpuTimer.stop();
      return -1;
  }    return 0; // signaling: OK
}

@Override
public int enemyDestroyed(Unit enemy, Unit attacker) {
 try {
  cpuTimer.start();
        if (enemy==null) sendTextMsg("WTF ETTOR: enemyDestroyed enemy==null, attacker=="+attacker, FieldBOT.MSG_ERR);
        else {
            scoutModule.enemyDestroyed(enemy, attacker);
        }
   cpuTimer.stop();
  } catch (Exception e) {
      printStackTrakeToChat("enemyDestroyed",e);
     cpuTimer.stop();
      return -1;
  }
    return 0; // signaling: OK
}

@Override
public int weaponFired(Unit unit, WeaponDef weaponDef) {
    return 0; // signaling: OK
}

@Override
public int playerCommand(java.util.List<Unit> units, int commandTopicId, int playerId) {
    return 0; // signaling: OK
}

@Override
public int commandFinished(Unit unit, int commandId, int commandTopicId) {
  cpuTimer.start();
  try {
//    sendTextMsg("commandFinished ", MSG_DBG_SHORT);
    if (deadLstUnit.contains(unit)) { // !!! DEBUG! TEST!!!
        sendTextMsg("commandFinished: unit in deadLstUnit!", MSG_DBG_SHORT);
        return 0;
    }
    
    boolean foundHomeBase=false;
    for (AGroupManager sGroup:smartGroups) {
        sGroup.commandFinished(unit, commandId, commandTopicId);
        if (sGroup.contains(unit)) foundHomeBase=true;
    }
    if (!foundHomeBase) {
        addToSameGroup(unit);
        sendTextMsg("On cmdFinish unit without base. ", MSG_DBG_SHORT);
    }
//    sendTextMsg("done commandFinished.", MSG_DBG_ALL);
  } catch (Exception e) {
      printStackTrakeToChat("commandFinished",e);
  cpuTimer.stop();
      return -1;
  }
  cpuTimer.stop();
    return 0; // signaling: OK
}

@Override
public int seismicPing(AIFloat3 pos, float strength) {
 try {
  cpuTimer.start();
    scoutModule.seismicPing(pos, strength);
   cpuTimer.stop();
  } catch (Exception e) {
     printStackTrakeToChat("seismicPing",e);

     cpuTimer.stop();
      return -1;
  }
    return 0; // signaling: OK
}

@Override
public int load(String file) {
    return 0; // signaling: OK
}

@Override
public int save(String file) {
    return 0; // signaling: OK
}

@Override
public int enemyCreated(Unit enemy) {
    return 0; // signaling: OK
}

@Override
public int enemyFinished(Unit enemy) {
    return 0; // signaling: OK
}

}