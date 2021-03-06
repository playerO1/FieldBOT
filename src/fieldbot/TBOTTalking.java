/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package fieldbot;

import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.Command;
import com.springrts.ai.oo.clb.CommandDescription;
import com.springrts.ai.oo.clb.Game;
import com.springrts.ai.oo.clb.Map;
import com.springrts.ai.oo.clb.Point;
import com.springrts.ai.oo.clb.Resource;
import com.springrts.ai.oo.clb.Unit;
import com.springrts.ai.oo.clb.UnitDef;
import fieldbot.AIUtil.MathPoints;
import fieldbot.AIUtil.UnitSelector;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;

/**
 *
 * @author PlayerO1
 */
public class TBOTTalking {
    
    public final FieldBOT owner;
    public final Map ownerMap;
    
    /**
     * Принимать текстовые команды от противников?
     */
    public boolean allowEnemyTeamCommand=false;
    
    /**
     * На какие имена будет отзываться бот
     */
    private final String MyNames[];
    
    private final int DEFAULT_TIMEOUT=Integer.MAX_VALUE; // !!!
    
    private static String getColorName(Color myColor) {
        final String colorNames[]={"black","white","gray","dark", "red","green","blue",
                                "yellow","pink"};
        final Color colors[]={Color.black,Color.white,Color.gray,Color.darkGray, Color.red,Color.green,Color.blue,
                                Color.yellow,Color.pink};
        double minD=0.90*3;
        int bestColorName=-1;
        float myColorComponent[]=new float[3];
        float tmpColorComponent[]=new float[3];
        myColorComponent=myColor.getRGBColorComponents(myColorComponent);
        for (int i=0;i<colorNames.length;i++) {
            tmpColorComponent=colors[i].getRGBColorComponents(tmpColorComponent);
            double d=0;
            for (int c=0; c<tmpColorComponent.length; c++)
                d+= (tmpColorComponent[c]-myColorComponent[c])*(tmpColorComponent[c]-myColorComponent[c]);
            if (d<minD) {
                minD=d;
                bestColorName=i;
            }
        }
        if (bestColorName!=-1) return colorNames[bestColorName]; // TODO Test!
        return null; // not found
    }
    
    public TBOTTalking(FieldBOT owner) {
        this.owner=owner;
        this.ownerMap=owner.clb.getMap();
        lstToShare=null;
        
        MyNames=new String[3];
        MyNames[0]="bot";
        MyNames[1]=owner.botShortName;
        MyNames[2]=getColorName(owner.clb.getGame().getTeamColor(owner.teamId));
        if (MyNames[2]==null) MyNames[2]=MyNames[0];// Color name not found!!!
    }
    
    private int lastPointsCount=0;// для определения кол-ва новых точек.
    
//private int sendToPlayer=-1;// кому сделать передачу
private int sendToTeam=-1;
private Resource sendingRes=null; // what resource tupe to send. If null then all.
private float sendingResCount; // how many resource send
private ArrayList<Unit> lstToShare; // list of sending units

public void update(int frame) {

    if ( sendToTeam>=0 ) {
        
        if (lstToShare==null || lstToShare.isEmpty()) {
            if (sendingRes==null) owner.sendTextMsg("res Sharing ALL count="+sendingResCount+" to team "+sendToTeam, FieldBOT.MSG_DLG);
             else owner.sendTextMsg("res Sharing "+sendingRes.getName()+" count="+sendingResCount+" to team "+sendToTeam, FieldBOT.MSG_DLG);
            if (sendingRes==null) for (Resource r:owner.clb.getResources()) owner.clb.getEconomy().sendResource(r, sendingResCount, sendToTeam);
            else {
                owner.clb.getEconomy().sendResource(sendingRes, sendingResCount, sendToTeam);
                sendingRes=null;
            }
            sendToTeam=-1;
        } else {
            //TODO ускорить изъятие... //owner.removeFromBaseAll(lstToShare);
            int nSending = owner.clb.getEconomy().sendUnits(lstToShare, sendToTeam );
            //if (nSending==lstToShare.size()) {
            //    for (TBase base:owner.bases) for (Unit u:lstToShare) base.removeUnit(u); // TODO изъять из всех списков баз... может по событиям обработка есть unitGiven... или нет?...
            //    owner.sendTextMsg(" test ");
            //}
            owner.sendTextMsg("Unit n="+nSending+" of "+lstToShare.size()+" seding to team "+sendToTeam+" (sharing).", FieldBOT.MSG_DLG);
            lstToShare=null;
            sendToTeam=-1;
        }
        
    }
    
    if (frame%10==0) {
        // FIXME повторяющиеся точки!!!
        List<Point> points=ownerMap.getPoints(true); // getPoints(boolean includeAllies)
        if (points.size()<lastPointsCount) {
            lastPointsCount=points.size();
            owner.sendTextMsg("Points removing", FieldBOT.MSG_DBG_ALL);
        }
        if (points.size()>lastPointsCount) {
            owner.sendTextMsg("Adding new point:", FieldBOT.MSG_DBG_ALL);
            // int i=0;i<points.size()-lastPointsCount;i++
            for (int i=lastPointsCount;i<points.size();i++) onNewPoint(points.get(i)); // TODO TEST!!!!!!
            lastPointsCount=points.size();
        }
    }
    
}

private boolean lastIsForMe=false;
private boolean itIsForMeMsg(String msg, boolean fromPointMarker) {
    if (fromPointMarker && owner.clb.getSkirmishAIs().getSize()<2) return true; // Если никакого другого бота нет. TODO проверить в команде.
    // проверка к кому обращаются
    boolean isForMe=false;
    int maxNamePos=4;
    String upCMsg=msg.toLowerCase();
    int p=upCMsg.indexOf("hey");
    if ((p>=0)&&(p<maxNamePos)) maxNamePos+=3;
    for (int i=0; i<MyNames.length; i++) {
        p=upCMsg.indexOf(MyNames[i].toLowerCase());
        if ( p>=0 && p<=maxNamePos ) { // p<=3
            isForMe = true;
            break;
        } 
    }
    
    if (!isForMe && lastIsForMe) { // учесть предыдущее обращение
        p=upCMsg.indexOf("and ");
        if (p>=0 && p<=maxNamePos) isForMe=true;
        // TODO check time!
    }
        
    lastIsForMe=isForMe;
    return isForMe;
}


private List<Unit> selectUnitInArea_myOnly(AIFloat3 center,float r, boolean getUnitNow) {
    // TODO быстрее сделать это (owner.clb.getFriendlyUnitsIn())
    ArrayList<Unit> selectLst=new ArrayList<Unit>();
    for (AGroupManager sGroup:owner.smartGroups) {
        for (Unit u:sGroup.getAllUnits(getUnitNow))
         if (u!=null)
         {
            float maxSearchR;
            if (r>0) maxSearchR=r;
            else maxSearchR=MathPoints.getRadiusFlat(u)*2;
            if (MathPoints.getDistanceBetweenFlat(center, u.getPos())<=maxSearchR) selectLst.add(u);
         }
    }
    return selectLst;
}
      
private Unit selectUnitInArea_myOnly_near(AIFloat3 center,float r, boolean getUnitNow) {
    // TODO быстрее сделать это (owner.clb.getFriendlyUnitsIn())
    float l=Math.max(r, 200); //!!!
    Unit selectU=null;
    for (AGroupManager sGroup:owner.smartGroups) {
        for (Unit u:sGroup.getAllUnits(getUnitNow))
         if (u!=null)
         {
            float unitR=MathPoints.getRadiusFlat(u);
            float d=MathPoints.getDistanceBetweenFlat(center, u.getPos())-unitR;
            if (d<=l) {
                selectU=u;
                l=d;
            }
         }
    }
    return selectU;
}


protected void onNewPoint(Point msgPoint) {
    if (owner.clb.getSkirmishAI().getTeamId()==-1) return; // If bot was dead.

    // от кого
    int msgTeam=-1;
    Color msgColor=msgPoint.getColor();
    Game game=owner.clb.getGame();
    for (int teamI=0;teamI<owner.clb.getTeams().getSize();teamI++) if (game.getTeamColor(teamI).equals(msgColor))
    {// TODO только ally team...
        msgTeam=teamI;
        break;
    }
    String message=msgPoint.getLabel();
    if (msgTeam==-1) {
        owner.sendTextMsg("Uncknown message team - can not detect team from color!", FieldBOT.MSG_ERR);
        // TODO test team==-1.
    }
    
    boolean itIsForMe=itIsForMeMsg(message, true); // проверка к кому обращаются
    //!!!!!!!!!!!!!!!
    if (!itIsForMe) return;

    owner.sendTextMsg(" point "+msgPoint.getPosition().x+";"+msgPoint.getPosition().z+" msg="+message+" pointID="+msgPoint.getPointId()+" teamDetectI="+msgTeam, FieldBOT.MSG_DBG_SHORT);
    
    if (message.contains("get") || message.contains("give")) {
        boolean getUnitNow=message.contains(" now")||message.contains("now!");
        
        if (lstToShare==null) lstToShare=new ArrayList<Unit>();

        List<Unit> unitSelect= selectUnitInArea_myOnly(msgPoint.getPosition(), -1, getUnitNow);
        lstToShare.addAll(unitSelect);
        
        if (lstToShare.isEmpty()) {
            owner.sendTextMsg("No unit for sharing.", FieldBOT.MSG_DLG);
            lstToShare=null;
        } else sendToTeam=msgTeam;
    }
    
    if (message.contains("reclime") || message.contains("utilize")) {
        boolean reclimeAll=message.contains("all"); // reclime all of this type
        
        if (lstToShare==null) lstToShare=new ArrayList<Unit>();

        // TODO first building, no moving!!!!
        Unit unitSelect= selectUnitInArea_myOnly_near(msgPoint.getPosition(), -1, true);
        boolean ok=false;
        if (unitSelect!=null && unitSelect.getDef().isReclaimable()) {
            AGroupManager agm=owner.getOwnerGroup(unitSelect);
            if (agm!=null && agm instanceof TBase) {
                TBase base=(TBase)agm; 
                if (reclimeAll) {
                    UnitDef uDef=unitSelect.getDef();
                    ArrayList<Unit> allUnit=base.getAllUnits(false);
                    allUnit=UnitSelector.selectUnitWithDef(allUnit,uDef);
                    for (Unit u2:allUnit) if (!base.unitToReclime.contains(u2) ) {
                        base.unitToReclime.add(u2);
                        ok=true;
                    }
                } else {
                    if (!base.unitToReclime.contains(unitSelect) ) {
                        base.unitToReclime.add(unitSelect);
                        ok=true;
                    }
                }
            }
        }
// TODO reclime unit in area
//        List<Unit> unitSelect= selectUnitInArea_myOnly(msgPoint.getPosition(), -1, true);//getUnitNow);
//        boolean ok=false;
//        for (Unit u:unitSelect) {
//        //Do check: unitSelect.getDef().isReclaimable() !!!
//            AGroupManager agm=owner.getOwnerGroup(u);
//            if (agm!=null && agm instanceof TBase) {
//                TBase base=(TBase)agm; 
//                if (reclimeAll) {
//                    UnitDef uDef=u.getDef();
//                    ArrayList<Unit> allUnit=base.getAllUnits(false);
//                    allUnit=TWarStrategy.selectUnitWithDef(allUnit,uDef);
//                    for (Unit u2:allUnit) if (!base.unitToReclime.contains(u2) ) {
//                        base.unitToReclime.add(u2);
//                        ok=true;
//                    }
//                } else {
//                    if (!base.unitToReclime.contains(u) ) {
//                        base.unitToReclime.add(u);
//                        ok=true;
//                    }
//                }
//            }
//        }
        
        if (ok) {
            owner.sendTextMsg("Add unit \""+unitSelect.getDef().getHumanName()+"\" to reclime.", FieldBOT.MSG_DLG);
        } else owner.sendTextMsg("No unit for reclime.", FieldBOT.MSG_DLG);;
    }
    
    if (message.contains("create") && message.contains("base")){// || message.contains("new"))) {
        if (message.contains("empty")) {
            // Check on free position from other TBases
            TBase otherBase=owner.getNearBase(msgPoint.getPosition());
            boolean canCreate=true;
            if (otherBase!=null) {
                if (MathPoints.getDistanceBetweenFlat(otherBase.getCenter(), msgPoint.getPosition()) <= otherBase.radius) // TODO plas second radius
                    canCreate=false; // base collision
            }
            if (canCreate) {
                TBase newB=new TBase(owner, msgPoint.getPosition(), 0);
                owner.addSmartGroup(newB);
                owner.sendTextMsg("New empty base created "+newB.toString(), FieldBOT.MSG_DLG);
            } else owner.sendTextMsg("Can not create base: collision.", FieldBOT.MSG_DLG);
        } else {
            TBase newB=owner.spawnNewBase(msgPoint.getPosition(), 0);
            if (newB!=null) {
                owner.sendTextMsg("New base created "+newB.toString()+", "+newB.getAllUnits(false).size()+" units sending to new base.", FieldBOT.MSG_DLG);
            } else {
                owner.sendTextMsg("Can't create base: no enought free unit.", FieldBOT.MSG_DLG);
            }
        }
    }
    
    if (message.contains("move") && message.contains("base")){// && (message.contains("this") || message.contains("new"))) {
        AIFloat3 to=msgPoint.getPosition();
        TBase nearB=owner.getNearBase(to);
        if (nearB!=null) {
            nearB.moveTo(to);
            owner.sendTextMsg("Base moved.", FieldBOT.MSG_DLG);
        } else owner.sendTextMsg("No base exist!", FieldBOT.MSG_DLG);
    }
    
    if (message.contains("build") || message.contains("make")) {
        final String parsingParam[]={"build","make"};

        HashSet<UnitDef> udSet=new HashSet<UnitDef>();
        for (TBase base:owner.selectTBasesList()) udSet.addAll(base.getBuildList(true)); // всех, в т.ч. недостроенных.
        UnitDef meanUnitDef=findUnitDefByHumanQuery(message,parsingParam,new ArrayList(udSet), null); // кого строить

        ArrayList<Integer> countRaw=parsingIntArr(message,parsingParam);
        int count=1;
        if (countRaw.size()==1) count=countRaw.get(0);// !!!!!!!!!
        if (meanUnitDef==null)  {
            String un=message;//"";
            //for (String t:unitNameElement) un +=" " + t;
            owner.sendTextMsg("Unit type not found: "+un, FieldBOT.MSG_DLG);
        } else {
            TBase baseForBuild=null; // near for point base
            float nearL=0.0f;
            for (TBase base:owner.selectTBasesList()) {
                float L=MathPoints.getDistanceBetweenFlat(base.center, msgPoint.getPosition())-base.radius;
                if (baseForBuild==null || L<nearL) // первая и ближайшая
                  if (base.canBuildOnBase(meanUnitDef, false) && base.getBuildList(true).contains(meanUnitDef)) // которая реально может это построить
                  {
                    nearL=L;
                    baseForBuild=base;
                  }
            }
            if (baseForBuild!=null) {
                baseForBuild.currentBaseTarget.add(meanUnitDef, count, msgPoint.getPosition());
                if (message.contains(" now")) baseForBuild.currentBaseTarget.commandNow=true;// сейчас же!
                // TODO "build now!"
                owner.sendTextMsg("Unit "+meanUnitDef.getHumanName()+" add "+count+" to list with position "+msgPoint.getPosition()+" on base: "+baseForBuild.center, FieldBOT.MSG_DLG);
            } else {
                owner.sendTextMsg("Base can't make unit "+meanUnitDef.getHumanName()+" with position "+msgPoint.getPosition()+" on base: "+baseForBuild.center, FieldBOT.MSG_DLG);
            }
        }
    }    

}

/**
 * Send army unit from all groups to other team
 * @param toPlayer 
 */
private void cmd_giveArmy(int toPlayer) {
    if (lstToShare==null) lstToShare=new ArrayList<Unit>();
    for (AGroupManager sGroup:owner.smartGroups) {
        if (sGroup instanceof TBase) {
            TBase base=(TBase)sGroup;
            lstToShare.addAll(base.army);
        } else if (sGroup.baseType>0) { //TODO give army - check by sGroup.baseType, maybe rewrite this line?
            lstToShare.addAll(sGroup.getAllUnits(true));
        }
    }
    if (lstToShare.isEmpty()) {
        owner.sendTextMsg("No army unit for sharing.", FieldBOT.MSG_DLG);
        lstToShare=null;
    } else sendToTeam=owner.clb.getGame().getPlayerTeam(toPlayer);//sendToPlayer=toPlayer;    
}

//@Override
public void message(int player, String message) { // TODO return result, boolean
    if (owner.clb.getSkirmishAI().getTeamId()==-1) return; // If bot was dead.
    
    boolean allowThisCommand=allowEnemyTeamCommand ||
            (player!=-1 && // this  Spring 94.1,96 Segmentation fault, on argument -1 (message from spectators)
            owner.clb.getGame().getPlayerTeam(player)!=-1 &&
            owner.clb.getGame().getTeamAllyTeam(owner.clb.getGame().getPlayerTeam(player))==owner.clb.getGame().getTeamAllyTeam(owner.teamId));
    
    if ( allowThisCommand && message.startsWith("I need") && message.contains("!") )
    { // Просьбы о ресурсах
            //if (player!=-1) TODO test: do need this if?
            //    sendToTeam=...
            //else owner.sendTextMsg("Can not share to spectator!", FieldBOT.MSG_ERR);
        Resource res=null;
        for (Resource r:owner.clb.getResources()) if (message.toLowerCase().contains(r.getName().toLowerCase())) { // TODO tolowercase???
            res=r;
            break;
        }
        sendingRes=res;
        if (res!=null) {
            final String parsingParam[]={"need"};
            ArrayList<Integer> intRaw=parsingIntArr(message, parsingParam);
            switch (intRaw.size()) {
                case 0:
                    sendingResCount=owner.clb.getEconomy().getCurrent(res)*0.7f;
                    sendToTeam=owner.clb.getGame().getPlayerTeam(player);
                    break;
                case 1:
                    sendingResCount=Math.min(owner.clb.getEconomy().getCurrent(res), intRaw.get(0));
                    sendToTeam=owner.clb.getGame().getPlayerTeam(player);
                    break;
                default:
                    owner.sendTextMsg("Error param for sharing res in message: "+message+" parsing N="+intRaw.size(), FieldBOT.MSG_ERR);
            }
            lstToShare=null;// !!!
        }// else ...
        if (message.equalsIgnoreCase("I need unit support!")) cmd_giveArmy(player);
          // message: "I need unit support!" - make link to "give me army"
    }
    
    boolean itIsForMe=itIsForMeMsg(message, false); // validate message target team
    
    if (itIsForMe) {
        
        if ( !allowThisCommand ) // If not allow acept command from this player
        {
            owner.sendTextMsg("I don't talk with enemy. Command rejected.", FieldBOT.MSG_DLG);
            return;
        }
        
        owner.sendTextMsg("Player " + player + " say: " + message , FieldBOT.MSG_DBG_ALL);
        if ((message.contains("get")||message.contains("give")) && message.contains("res")) {
            //if (player!=-1)
            //{
                sendToTeam=owner.clb.getGame().getPlayerTeam(player);//sendToPlayer=player;
                sendingRes=null;
                sendingResCount=500.0f; // !!!
                lstToShare=null;
            //}
        }
        
        if (message.contains("show") && message.contains("level")) {
            for (TBase base:owner.selectTBasesList()) {
                // TODO print "On base NAME:"
                ArrayList<TTechLevel> allTLvls=new ArrayList<TTechLevel>();
                
                ArrayList<TTechLevel> TechLvls=new ArrayList<TTechLevel>();
                TechLvls=TTechLevel.GetAllVariantLevel( base.getContainUnitDefsList(false),base.getBuildList(true) , base, owner);
                owner.sendTextMsg("-= Num of Tech Up wariants: "+TechLvls.size()+" =-", FieldBOT.MSG_DLG);
                for (TTechLevel level:TechLvls) {
                    owner.sendTextMsg("< "+level.toString(), FieldBOT.MSG_DLG);
                    float t=owner.ecoStrategy.getRashotBuildTimeLst(level.needBaseBuilders, owner.avgEco.getAVGResourceToArr(), base.getBuildPower(false, true));
                    // !!!!!!!
                    owner.sendTextMsg(" TIME to build level="+t+">", FieldBOT.MSG_DLG);
                }
                allTLvls.addAll(TechLvls);
                TechLvls=TTechLevel.GetAllVariantLevelByMorph(base.getAllUnits(false), base.getContainUnitDefsList(false),base.getBuildList(true) , base, owner);
                owner.sendTextMsg("-= Num of MORPHING Tech Up wariants: "+TechLvls.size()+" =-", FieldBOT.MSG_DLG);
                for (TTechLevel level:TechLvls) {
                    owner.sendTextMsg("> "+level.toString(), FieldBOT.MSG_DLG);
                    // TODO не так время расчитывать...!!!
                    float t=owner.ecoStrategy.getRashotBuildTimeLst(level.needBaseBuilders, owner.avgEco.getAVGResourceToArr(), base.getBuildPower(false, true));
                    owner.sendTextMsg(" <TIME to build level="+t+">", FieldBOT.MSG_DLG);
                    owner.sendTextMsg(" <special: assistable time="+level.getAssistWorkTime()+" noAssistable="+level.getNOAssistWorkTime()+">", FieldBOT.MSG_DLG);
                }
                allTLvls.addAll(TechLvls);
                
                // DEBUG smart TECH UP
                float bestEco[]=TTechLevel.getMaxResProduct(base.getBuildList(true), owner); // текущий уровень
                float currentRes[][];
                if (owner.ecoStrategy.useOptimisticResourceCalc) currentRes= owner.avgEco.getSmartAVGResourceToArr();
                  else currentRes= owner.avgEco.getAVGResourceToArr();
                float bestBuilderF=TTechLevel.getMaxBuildPower(base.getBuildList(true));
                TTechLevel bestLvl = owner.ecoStrategy.selectLvl_bestForNow(allTLvls, base, bestEco, bestBuilderF, currentRes);
                if (bestLvl!=null) owner.sendTextMsg("BEST TLevel="+bestLvl, FieldBOT.MSG_DLG);
                else {
                    owner.sendTextMsg("NO BEST LEVEL!", FieldBOT.MSG_DLG);
                }
                if (bestLvl!=null) { // TODO if using smart tech up getAbsolutePrecissionTechUpTimeFor()?
                    //owner.sendTextMsg("For TLevel="+bestLvl, FieldBOT.MSG_DLG);                    
                    owner.sendTextMsg("isActualDoTechUp_fastCheck: "+owner.ecoStrategy.isActualDoTechUp_fastCheck(base, base.getBuildList(true), bestLvl), FieldBOT.MSG_DLG);
                    owner.sendTextMsg("-- getPrecissionActualTechUpTimeFor --", FieldBOT.MSG_DLG);                    
                    TechUpTrigger trigger = owner.ecoStrategy.getPrecissionTechUpTimeFor
                            (base, base.getBuildList(true), bestLvl );
                    if (trigger!=null) owner.sendTextMsg("> "+trigger.toString(), FieldBOT.MSG_DLG);                    
                    else  owner.sendTextMsg("> trigger=null", FieldBOT.MSG_DLG);
                    owner.sendTextMsg("----", FieldBOT.MSG_DLG);                    
                    
                }    
                //---------------------
            }
        }
        
        
        if ((message.contains("get")||message.contains("give")) && message.contains("unit")) {
            final String parsingParam[]={"unit","units"};

            HashSet<UnitDef> udSet=new HashSet<UnitDef>();
            for (AGroupManager sGroup:owner.smartGroups) udSet.addAll(sGroup.getContainUnitDefsList(false)); // всех, в т.ч. недостроенных.
            UnitDef meanUnitDef=findUnitDefByHumanQuery(message,parsingParam,new ArrayList(udSet), null); // кого передать.
            // TODO список из существующих сейчас юнитов !!!
            ArrayList<Integer> countRaw=parsingIntArr(message,parsingParam);
            int count=1;
            if (countRaw.size()==1) count=countRaw.get(0);// !!!!!!!!!
            if (message.contains("all")) count=Integer.MAX_VALUE; //  test it!
            
            if (lstToShare==null) lstToShare=new ArrayList<Unit>();
            int n=0; // сколько будет передано
            
            boolean getUnitNow=message.contains(" now")||message.contains("now!");
            
            lbl_searchUShare: for (AGroupManager sGroup:owner.smartGroups) {
                for (Unit u:sGroup.getAllUnits(getUnitNow)) if (u.getDef().equals(meanUnitDef)) {
                    lstToShare.add(u);
                    n++;
                    if (n>=count) break lbl_searchUShare;// return from cicle faster
                }
            }
            if (lstToShare.isEmpty()) {
                owner.sendTextMsg("No unit for sharing.", FieldBOT.MSG_DLG);
                lstToShare=null;
            } else sendToTeam=owner.clb.getGame().getPlayerTeam(player);//sendToPlayer=player;
        }
        
        if ((message.contains("get")||message.contains("give")) && message.contains("army") && !message.contains("unit")) {
            //if (player==-1) owner.sendTextMsg("Can not share to spectator.", FieldBOT.MSG_DLG);
            //else {
            cmd_giveArmy(player);
            //}
        }

        if (message.contains(" stop") && message.contains(" work")) {
            boolean estChtoOstanovit=false;
            for (TBase base:owner.selectTBasesList()) { // FIXME what do with army?
                estChtoOstanovit = estChtoOstanovit || !base.currentBaseTarget.currentBuildLst.isEmpty() || !base.unitToReclime.isEmpty();
                base.currentBaseTarget.removeAllBuildingTarget();
                base.unitToReclime.clear();
            }
            if (estChtoOstanovit) owner.sendTextMsg("All your command rejected & stop.", FieldBOT.MSG_DLG);
            else owner.sendTextMsg("Not special command for rejected.", FieldBOT.MSG_DLG);
        }
        if (message.contains("status")) {
            owner.sendTextMsg("Enemy chat listen: "+allowEnemyTeamCommand+", Auto tech up: "+owner.autoTechUp+", Army control:"+owner.warStrategy.makeArmy, FieldBOT.MSG_DLG);
            owner.sendTextMsg("My name allias: "+Arrays.toString(MyNames), FieldBOT.MSG_DLG);
            owner.sendTextMsg("Have tech levels: "+owner.techLevels.keySet().toString(), FieldBOT.MSG_DLG);
            owner.sendTextMsg("Avg resources: "+owner.avgEco.toString(), FieldBOT.MSG_DLG);
            
            Runtime rt = Runtime.getRuntime();
            long maxMemory = rt.maxMemory() /1024/1024;
            long allocatedMemory = rt.totalMemory() /1024/1024;
            long freeMemory = rt.freeMemory() /1024/1024;    
            owner.sendTextMsg("BOT CPU usage: "+owner.cpuTimer.toString()+" memory (Mb): max="+maxMemory+" allocated="+allocatedMemory+" free="+freeMemory, FieldBOT.MSG_DLG);
            owner.sendTextMsg("Num of groups "+owner.smartGroups.size(), FieldBOT.MSG_DLG);
            for (AGroupManager sGroup:owner.smartGroups) {
                owner.sendTextMsg(sGroup.toString(), FieldBOT.MSG_DLG);
            }
            owner.cpuTimer.reset();// !!!!
        }
        /*
        if (message.contains("start") && (message.contains("at")||message.contains("on"))) {
            float bX=0, bY=0;
            final String parsingParam[]={"at","on"};
            ArrayList<Integer> intRaw=parsingIntArr(message, parsingParam);
            if (intRaw.size()==2) {
                bX=intRaw.get(0); bY=intRaw.get(1); // получение координат
                owner.clb.getPathing().
                AIFloat3 stPos = owner.clb.getMap().getStartPos();
       // НЕ РАБОТАЕТ ДОДЕЛАТЬ. может потом...
                owner.sendTextMsg("New start point at "+stPos.x+":"+stPos.z, FieldBOT.MSG_DLG);
            } else owner.sendTextMsg("Point COORN DNT FOUND! "+message, FieldBOT.MSG_DLG);
        }
        */
        if (message.contains("do ") && message.contains("tech up")) {
            boolean makeNow=message.contains(" now")||message.contains("now!");
            boolean testOnly=message.contains("test");
            boolean startTUp=owner.checkAndTechUp(makeNow, testOnly);
            if (testOnly) owner.sendTextMsg(" (only test! not real do tech up now)", FieldBOT.MSG_DLG);
            if (!startTUp) owner.sendTextMsg("Command rejected: not enough resources or time. ", FieldBOT.MSG_DLG);
            else owner.sendTextMsg("Command acepted.", FieldBOT.MSG_DLG);
        }
        if (message.contains("create") && message.contains("base")) {
            float bX=0, bY=0;
            final String parsingParam[]={"at","on"};
            ArrayList<Integer> intRaw=parsingIntArr(message, parsingParam);
            if (intRaw.size()==2) {
                bX=intRaw.get(0); bY=intRaw.get(1); // получение координат
                TBase newB=new TBase(owner, new AIFloat3(bX, 0, bY), 0);
                owner.addSmartGroup(newB);
                owner.sendTextMsg("New base created "+newB.toString(), FieldBOT.MSG_DLG);
            } else owner.sendTextMsg("New base COORN DNT FOUND! "+message, FieldBOT.MSG_DLG);
        }
        
        if ((message.contains("do")||message.contains("start")) && message.contains("morph")) { // TODO кого конкретно.
            boolean doNow=message.contains(" now");
            // TODO only constructor or army too?
            if (!message.contains("all")) {
                for (AGroupManager sGroup:owner.smartGroups) {
                    Unit u=null;
                    if (sGroup instanceof TBase) {
                        TBase base=(TBase)sGroup;
                        for (Unit unit:base.idleCons) {
                            List<CommandDescription> l=owner.modSpecific.getMorphCmdList(unit,false);
                            if (!l.isEmpty()) u=unit;
                        }
                        if (u!=null) base.setAsWorking(u);
                        else if (doNow) for (Unit unit:base.workingCons) {
                            List<CommandDescription> l=owner.modSpecific.getMorphCmdList(unit,false);
                            if (!l.isEmpty()) u=unit;
                        }
                    } else {
                        // FIXME makr as work unit for other group
                        for (Unit unit:sGroup.getAllUnits(!doNow)) {
                            List<CommandDescription> l=owner.modSpecific.getMorphCmdList(unit,false);
                            if (!l.isEmpty()) u=unit;
                        }
                    }
                    if (u!=null) {
                        List<CommandDescription> l=owner.modSpecific.getMorphCmdList(u,false);
                        if (!l.isEmpty()) {
                            CommandDescription cmd=l.get(0);
                            if (l.size()>1) cmd=l.get(1); // to Eco com... TODO переделать!
                            UnitDef morphTo=owner.modSpecific.getUnitDefForMprph(cmd, u.getDef());
                            if (morphTo!=null) owner.sendTextMsg(" Morphing "+u.getDef().getHumanName()+" to "+morphTo.getHumanName(), FieldBOT.MSG_DLG);
                            else owner.sendTextMsg("  error getUnitDefForMprph is null!", FieldBOT.MSG_ERR);
                            u.executeCustomCommand(cmd.getId(), new ArrayList<Float>(), (short)0, DEFAULT_TIMEOUT);
                        }
                    }

                }
            } else { // Morph all
                for (AGroupManager sGroup:owner.smartGroups) {
                    ArrayList<Unit> toMorphLst=new ArrayList<Unit>(); // потом всех чтобы сделать рабочими
                    List<Unit> toMorphUnits=null;
                    if (!doNow && (sGroup instanceof TBase)) {
                        TBase base=(TBase)sGroup;
                        toMorphUnits=base.idleCons;
                    } else toMorphUnits=sGroup.getAllUnits(true);
                    for (Unit unit:toMorphUnits) { // !!! Unit unit:base.workingCons
                        List<CommandDescription> l=owner.modSpecific.getMorphCmdList(unit,false);
                        if (!l.isEmpty()) {
                            toMorphLst.add(unit);
                          List<CommandDescription> l2=owner.modSpecific.getMorphCmdList(unit,false);
                            if (!l2.isEmpty()) {
                                CommandDescription cmd=l2.get(0);
                                if (l2.size()>1) cmd=l2.get(1); // to Eco com... TODO переделать!
                                UnitDef morphTo=owner.modSpecific.getUnitDefForMprph(cmd, unit.getDef());
                                if (morphTo!=null) owner.sendTextMsg(" Morphing "+unit.getDef().getHumanName()+" to "+morphTo.getHumanName(), FieldBOT.MSG_DLG);
                                else owner.sendTextMsg("  error getUnitDefForMprph is null!", FieldBOT.MSG_ERR);
                                unit.executeCustomCommand(cmd.getId(), new ArrayList<Float>(), (short)0, DEFAULT_TIMEOUT);
                            }
                        }
                    }
                    if (sGroup instanceof TBase) {
                        TBase base=(TBase)sGroup; // TODO test!!!!
                        base.setAllAsWorking(toMorphLst);//!!!!!
                    }
                }
            }
        }
        if (message.contains("stop") && message.contains("morph")) { // TODO кого конкретно.
            boolean existForStop=false;
            for (AGroupManager sGroup:owner.smartGroups) {
                //ArrayList<Unit> morphLst=new ArrayList<Unit>(); // потом всех чтобы сделать рабочими
                for (Unit unit:sGroup.getAllUnits(false)) { // !!! Unit unit:base.workingCons
                    boolean isMopring=false;
                    for (Command cmd: unit.getCurrentCommands())
                     if (ModSpecification.isMorphCMD(cmd))
                    {
                        isMopring=true;
                        break;
                    }
                      // FIXME !!! плохо сделано
                    isMopring=isMopring||(unit.isParalyzed());// && !unit.getCurrentCommands().isEmpty());
                    if (isMopring) {
                       // morphLst.add(unit);
                        // TODO bugs!
    //                    CommandDescription stopMorphCommand=ModSpecification.getStopMorphCmd(unit, false);
    //                    if (stopMorphCommand!=null) {
    //                      unit.executeCustomCommand(stopMorphCommand.getId(), new ArrayList<Float>(), (short)0, DEFAULT_TIMEOUT);
    //                      existForStop=true;
    //                    } else owner.sendTextMsg("Stop morph command not found!", FieldBOT.MSG_ERR);
                        unit.executeCustomCommand(ModSpecification.CMD_MORPH_STOP, new ArrayList<Float>(), (short)0, DEFAULT_TIMEOUT);
                        //unit.stop((short)0, DEFAULT_TIMEOUT);
                        existForStop=true;
                    }
                }
            }
            if (existForStop) owner.sendTextMsg("Command acepted.", FieldBOT.MSG_ERR);
                else owner.sendTextMsg("Command rejected: no morphing unit now.", FieldBOT.MSG_ERR);
        }
        
        boolean cmdMakeArmy=message.contains("make army") || message.contains(" do army");
        boolean cmdMakeDefence=message.contains("make def") || message.contains(" do defence");
        if (cmdMakeArmy && cmdMakeDefence) owner.sendTextMsg("Warning: uncknown chat command recognized as make amry and make defence too.", FieldBOT.MSG_ERR);
        if (cmdMakeArmy || cmdMakeDefence) {
            
            // 1. Подбор параметров армии, за какие сроки сделать? Для чего: скорость, защита, атака? На кого?
            float timeL=3*60;
            int unitL=Integer.MAX_VALUE;
            //TWarStrategy.NUM_P = 7
            float kachestva[]=TWarStrategy.KACHESTVA_ARMY.clone();//{ 0.01f, 0.3f, 0.6f, 0.6f, 0.2f, 0.1f,0.1f, 0.0f }; // spam, speed, health, atack, range, radar, stealth
            if (cmdMakeDefence) {
                kachestva=TWarStrategy.KACHESTVA_DEFTOWER.clone();
            }
            String msgP[]=message.split(messageSpliter);
            boolean parsingSt=false;
            for (int i=0; i<msgP.length;i++) {

                if (message.contains(" for "))
                { // with parameters
                    if (parsingSt) {
                        // TODO упростить...
                        for (int j=0;j<TWarStrategy.P_NAMES_forParsing.length;j++)
                          for (int k=0;k<TWarStrategy.P_NAMES_forParsing[j].length;k++)
                        {
                            if (msgP[i].equalsIgnoreCase(TWarStrategy.P_NAMES_forParsing[j][k])) {
                                float val=1.0f;
                                if (i<msgP.length-1) try {
                                    val=Float.parseFloat(msgP[i+1]); // !!!
                                    i++;
                                } catch (NumberFormatException nfe) {};
                                kachestva[j]=val;
                            }
                        }
                    } else if (msgP[i].equalsIgnoreCase("for")) {
                        parsingSt=true;
                        Arrays.fill(kachestva, 0.0f); // drop predefine settings
                    }
                }

                if (msgP[i].equalsIgnoreCase("time")) { // time limit
                    float val=1.0f;
                    if (i<msgP.length-1) try {
                        val=Float.parseFloat(msgP[i+1]); // !!!
                        float timeMult=60.0f; // default 1 min = 60 second.
                        if (i<msgP.length-2) {
                            if (msgP[i+2].contains("sec")) timeMult=1.0f;
                            if (msgP[i+2].contains("min")) timeMult=60.0f;
                            if (msgP[i+2].contains("hour")) timeMult=60.0f*60.0f;
                        }
                        timeL=val*timeMult;
                        i++;
                    } catch (NumberFormatException nfe) {};
                }
                if (msgP[i].equalsIgnoreCase("limit")||((i>0)&&msgP[i-1].equalsIgnoreCase("max") && msgP[i].equalsIgnoreCase("unit"))) { // unit limit
                    if (i<msgP.length-1) try {
                        int val=Integer.parseInt(msgP[i+1]); // !!!
                        if (val>0) unitL=val;
                        i++;
                    } catch (NumberFormatException nfe) {};
                }
            }
            
            String msgT="";
            for (int i=0;i<kachestva.length;i++) msgT+= TWarStrategy.P_NAMES[i]+" "+kachestva[i]+", ";
            msgT+=" time limit="+timeL+" second";
            if (unitL!=Integer.MAX_VALUE) msgT+=", unit limit="+unitL;
            msgT+=".";
            owner.sendTextMsg(" optimized plan for: "+msgT, FieldBOT.MSG_DLG);
            
            // 2. Вычисление оптимальной комбинации и кол-во юнитов
            TBase bestBase=null; HashSet<UnitDef> lstOfUDefs=new HashSet<UnitDef>();
            for (TBase base:owner.selectTBasesList()) {
                HashSet<UnitDef> tmpUDefs=base.getBuildList(true);
                if (tmpUDefs.size()>lstOfUDefs.size()) {
                    bestBase=base;
                    lstOfUDefs=tmpUDefs;
                }
            }
            
            int weaponDamageType=0;// TODO choose weapon damage type. 0 = default
            HashMap<UnitDef,Integer> armyUnits=owner.warStrategy.shooseArmy(bestBase, cmdMakeDefence, new ArrayList<UnitDef>(lstOfUDefs),timeL,unitL,kachestva, weaponDamageType);
            
            // TODO Only what can build now (with or without research center on TA mod!!!)
            
            // 3. Отправка сигнала на строительство армии по базам.
            boolean isNotImitate=!message.contains("test");
            if (!isNotImitate) owner.sendTextMsg("(test)", FieldBOT.MSG_DLG);
            if (armyUnits!=null) {
                if (isNotImitate)
                 owner.warStrategy.sendBuildMessageToBase(armyUnits, bestBase, false); //!!!
                else for (Entry<UnitDef,Integer> armyItem:armyUnits.entrySet()) {
                  // for Debug
                    owner.sendTextMsg(" army>"+armyItem.getKey().getHumanName()+" * "+armyItem.getValue(), FieldBOT.MSG_DLG);
                }
            } else owner.sendTextMsg("No plan for army!", FieldBOT.MSG_DLG);
            
        }
        else // If not build army then make special unit
        if (message.contains("build") || message.contains("make")) {
            final String parsingParam[]={"build","make","unit"};
            
            HashSet<UnitDef> udSet=new HashSet<UnitDef>();
            for (TBase base:owner.selectTBasesList()) udSet.addAll(base.getBuildList(true)); // всех, в т.ч. недостроенных.
            UnitDef meanUnitDef=findUnitDefByHumanQuery(message,parsingParam,new ArrayList(udSet), null); // кого строить
            
            ArrayList<Integer> countRaw=parsingIntArr(message,parsingParam);
            int count=1;
            if (countRaw.size()==1) count=countRaw.get(0);// !!!!!!!!!
            if (meanUnitDef==null)  {
                String un=message;//"";
                //for (String t:unitNameElement) un +=" " + t;
                owner.sendTextMsg("Unit type not found: "+un, FieldBOT.MSG_DLG);
            } else {
                for (TBase base:owner.selectTBasesList()) { // FIXME other groups?
                    if (base.canBuildOnBase(meanUnitDef, false) && base.getBuildList(true).contains(meanUnitDef))
                    { // Только те, которые способны это сделать
                        base.currentBaseTarget.add(meanUnitDef, count);
                        if (message.contains(" now")) base.currentBaseTarget.commandNow=true;// сейчас же!
                        if (count>1) base.stroyPlaning.preparePositionFor(meanUnitDef, count); // Запланировать постройку такого колличества юнитов
                        owner.sendTextMsg("Unit "+meanUnitDef.getHumanName()+" add "+count+" to list on base: "+base.center, FieldBOT.MSG_DLG);
                    }
                }
            }
        }
        
        if (message.contains("enable")) {
            boolean cmdAcepted=false;
            if ( message.contains(" auto") && message.contains("tech up"))
            {
                owner.autoTechUp=true;
                cmdAcepted=true;
            }
            if ( message.contains(" enemy chat"))
            {
                allowEnemyTeamCommand=true;
                cmdAcepted=true;
            }
            if ( message.contains(" eco"))
            {
                owner.ecoStrategy.makeEco=true;
                cmdAcepted=true;
            }
            if ( message.contains(" army control"))
            {
                owner.warStrategy.makeArmy=true;
                cmdAcepted=true;
            }
            if (cmdAcepted) owner.sendTextMsg("Command acepted.", FieldBOT.MSG_DLG);
                    else owner.sendTextMsg("Command rejected.", FieldBOT.MSG_DLG);
        }
        else
        if (message.contains("disable")) {
            boolean cmdAcepted=false;
            if ( message.contains(" auto") && message.contains("tech up"))
            {
                owner.autoTechUp=false;
                cmdAcepted=true;
            }
            if ( message.contains(" enemy chat"))
            {
                allowEnemyTeamCommand=false;
                cmdAcepted=true;
            }
            if ( message.contains(" eco"))
            {
                owner.ecoStrategy.makeEco=false;
                cmdAcepted=true;
            }
            if ( message.contains(" army control"))
            {
                owner.warStrategy.makeArmy=false;
                cmdAcepted=true;
            }
            if (cmdAcepted) owner.sendTextMsg("Command acepted.", FieldBOT.MSG_DLG);
                    else owner.sendTextMsg("Command rejected.", FieldBOT.MSG_DLG);
        }
        if (message.contains(" set"))
        {
            boolean cmdAcepted=false;
            if (message.contains(" eco"))
            {
                if ( (message.contains(" positive")||message.contains(" optimi")||message.contains(" good ")))
                {
                    owner.ecoStrategy.useOptimisticResourceCalc=true;
                    cmdAcepted=true;
                }
                if ( (message.contains(" negative")||message.contains(" pessimi")||message.contains(" bad ")))
                {
                    owner.ecoStrategy.useOptimisticResourceCalc=false;
                    cmdAcepted=true;
                }
            }
            if (cmdAcepted) owner.sendTextMsg("Command acepted.", FieldBOT.MSG_DLG);
                    else owner.sendTextMsg("Command rejected.", FieldBOT.MSG_DLG);
        }

        
        if (message.contains("help") || message.contains("list")) {
            owner.sendTextMsg("Command: 'get resource' , 'get|give unit [now]' , 'stop work'. 'Status' - debug, 'create base at X Y' , build UNIT/FACTORU_NAME, 'show level' - list tech up variants, 'do|start morph [all]', 'tech up' - check & tech up to new level."+
                    " '{enable|disable} {auto tech up | enemy chat}'", FieldBOT.MSG_DLG);
        }
    } else {
        owner.sendTextMsg("(test) Player " + player +" from team " +owner.clb.getGame().getPlayerTeam(player) + " say: '" + message+"'." , FieldBOT.MSG_DBG_ALL);
    }
    
    //return 0; // signaling: OK
}

/**
 * Check on map point label from human about start position
 * @return required by human start point, or null if messages not found
 */
public AIFloat3 checkStartPositionMessage() {
    List<Point> points=ownerMap.getPoints(true); // getPoints(boolean includeAllies)
    for (Point pt : points) {
        owner.sendTextMsg("DEBUG: checkStartPositionMessage>"+pt.getLabel(), FieldBOT.MSG_DBG_SHORT);
        String msg=pt.getLabel().toLowerCase();
        if (pt.getLabel().contains("start"))
          for (int j=0; j<MyNames.length; j++) // check for keyword "bot", ant other name
              if ( msg.contains(MyNames[j].toLowerCase()) ) return pt.getPosition();
    }
    return null;
    //onNewPoint(points.get(i));...
}

private static final String messageSpliter=" "; // word separator for message parsing

/**
 * Select number argument from string
 * @param message text message for parsing
 * @param keywordAfther key word (same of this word is signal), after keyword will be seek value
 * @return integer values, where found after key
 */
private ArrayList<Integer> parsingIntArr(String message,String[] keywordAfther) {
    String s[]=message.split(messageSpliter);
    int i,parsingStage=0;
    if (keywordAfther.length==0) parsingStage=1; // если нет ключевых слов - исследовать всё
    ArrayList<Integer> intLst=new ArrayList<Integer>();
    for (i=0;i<s.length;i++) {
        if (parsingStage>=1 ) { // && parsingStage<4
            try {
                int tmpC=Integer.parseInt(s[i]);
                intLst.add(tmpC);
                
            } catch (NumberFormatException nfe) {
                // is not number
            }

            parsingStage++;
        }
        for (int j=0;j<keywordAfther.length;j++) if (s[i].equals(keywordAfther[j])) parsingStage=1;
    }
    return intLst;
}


/**
 * Find unit def by human query
 * @param message full text with unit name name
 * @param udl list for select unit by name max relevants
 * @param keywordAfther set of keyworks, afther some of this words do search name sequence, can be empty.
 * @param prefix unitDef name prefix, can be null. Example: "arm", "cor", "tll"
 * @return found UnitDef or null
 */
protected static UnitDef findUnitDefByHumanQuery(String message,String[] keywordAfther,List<UnitDef> udl, String prefix) {
    String s[]=message.split(messageSpliter);
    int i,parsingStage=0;
    if (keywordAfther==null || keywordAfther.length==0) parsingStage=1; // если нет ключевых слов - исследовать всё
    ArrayList<String> unitNameElement=new ArrayList<String>(); // элементы названия юнита
//    int techLevel=-1; // сколько строить
    
    for (i=0;i<s.length;i++) { 
        if (parsingStage>=1 && parsingStage<4) {
            unitNameElement.add(s[i]);

//            owner.sendTextMsg(" (debug) parsing element <"+s[i]+">" , FieldBOT.MSG_DBG_ALL);

//            // T1, T2, is realy 1 2 and 3 4... Depends of MODs!
//            if (s[i].toLowerCase().equals("t1")) techLevel=1;
//            if (s[i].toLowerCase().equals("t2")) techLevel=2;
//            if (s[i].toLowerCase().equals("t3")) techLevel=3;
           
            parsingStage++;
        }
        if (keywordAfther!=null) for (int j=0;j<keywordAfther.length;j++) if (s[i].equals(keywordAfther[j])) parsingStage=1;
    }

    UnitDef meanUnitDef=null; // кого строить
    int ver=0, minVer=1; // вероятность совпадений, минимальная вероятность для доверия поиску.
    float percentQWord=0.0f;// дополнительная проверка - какой процент из слов совпал относительно размера названия.
    // TODO может всех в игре?... но долго; ошибки в написании и лишние окончания.
    if (prefix!=null && !prefix.isEmpty()) minVer++;
    for (UnitDef def:udl) {
        String uName=def.getHumanName().toLowerCase();
        int tmpV=0;
//        if (techLevel>=0) {
//            if (def.getTechLevel()==techLevel) tmpV++;
//            if (minVer<=1) minVer++; // не учитывать только по тех уровню.
//            //owner.sendTextMsg(" (debug) "+def.getName()+" tech level "+def.getTechLevel()+" find TL="+techLevel);
//        }
        if (prefix!=null && !prefix.isEmpty()) {
            String defN=def.getName();
            if ( defN.startsWith(prefix) ) tmpV++;
        }
        
        float tmpPercentW=0.0f;
        for (String nel:unitNameElement) if (uName.contains(nel.toLowerCase())) {
            tmpV++;
            tmpPercentW+=nel.length();
        }
        tmpPercentW/=uName.length();
        if (tmpV>ver) {
            percentQWord=tmpPercentW;
            ver=tmpV;
            meanUnitDef=def;
        } else if (tmpV==ver && tmpV>0) {
            if ( tmpPercentW>percentQWord) {
                percentQWord=tmpPercentW;
                ver=tmpV;
                meanUnitDef=def;
            }
        }
    }
    
    if (ver>=minVer) return meanUnitDef;
    else return null;
}
    
}
