/*
 * Copyright (C) 2014 PlayerO1
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package fieldbot;

import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.Unit;
import com.springrts.ai.oo.clb.UnitDef;
import com.springrts.ai.oo.clb.WeaponDef;
import com.springrts.ai.oo.clb.WeaponMount;
import fieldbot.AIUtil.MathOptimizationMethods;
import fieldbot.AIUtil.MathPoints;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 *
 * @author PlayerO1
 */
public class TWarStrategy {
    
    // Названия обозначений качества юнитов
    public final static int P_SPAM=0;  // колличество юнитов
    public final static int P_SPEED=1; // скорость
    public final static int P_HEALTH=2;// броня
    public final static int P_ATACK_POWER=3;   // мощность атаки
    public final static int P_ATACK_DISTANCE=4;// дальность атаки
    public final static int P_RADAR=5;   // радиус радара
    public final static int P_STEALTH=6; // скрытый от радара
    public final static int P_CONSTRUCTOR=7; // скрытый от радара
    // перечисление: spam, speed, health, atack, range, radar, stealth
    public final static int NUM_P=P_CONSTRUCTOR+1; // сколько параметров
    public final static String P_NAMES[]={"spam", "speed", "health", "atack", "range", "radar", "stealth","constructor"};
    public final static String P_NAMES_forParsing[][]={{"spam"}, {"speed","fast"}, {"health","HP"}, {"atack","weapon","damage"}, {"range","distance","long"}, {"radar"}, {"stealth","hidde"},{"constructor","work"}};
    
    private final FieldBOT owner;
    
    private final boolean USE_MULTIPLY=true; // Использовать мультипликативную модель для оценки качеств юнитов (иначе адитивная модель)
    
    /**
     * Enable army control
     */
    public boolean makeArmy;
    
    public TWarStrategy(FieldBOT owner) {
        this.owner=owner;
        makeArmy=false; // TODO set true by default
    }
    
    
    private static double[] getZeroVector(int size) {
        double v[] = new double[size];
        Arrays.fill(v, 0.0); //for (int j=0;j<size;j++) v[j]=0.0; // zero vector
        return v;
    }

    /**
     * Summ damage from all unit weapon.
     * @param unit
     * @return summ of damage + some bonus for area of effect
     */
    public static final float[] getSumWeaponDamage(UnitDef unit) {
        float[] wDmg=null;
        for (WeaponMount weaponMount:unit.getWeaponMounts()) {
            WeaponDef weapon=weaponMount.getWeaponDef();
            if (!weapon.isParalyzer()
                && weapon.isAbleToAttackGround() && !weapon.isShield()) {
                if (wDmg==null) { // init array
                    wDmg=new float[weapon.getDamage().getTypes().size()];
                    Arrays.fill(wDmg, 0);
                }
                float dMultipler=weapon.getSalvoSize()*weapon.getProjectilesPerShot();
                float areaOfEffect=weapon.getAreaOfEffect();
                for (int i=0;i<wDmg.length;i++) {
                    float dmg= weapon.getDamage().getTypes().get(i); // get(0) - default damage
                    dmg = 0.8f*dmg + 0.2f*dmg*areaOfEffect;// как повреждения распространяются по площади.
                    wDmg[i] += dmg*dMultipler;
                }
            }
        }
        return wDmg;
    }
    
    /**
     * 
     * @param unit
     * @param weaponDamagType defauld 0, specifed by weapon.getDamage().getTypes().get(weaponDamagType);
     * @return 
     */
    private double[] getUnitKachestva(UnitDef unit, int weaponDamagType) {
        double[] kachestva=new double[NUM_P];
        
        kachestva[P_SPAM]=1.0; // за присутствие
        kachestva[P_SPEED]=unit.getSpeed(); // за скорость
        kachestva[P_HEALTH]=unit.getHealth(); // за живучесть
        
        //unit.getResourceExtractorRange(null) ???
        //unit.getWingAngle() // крыло?...
        //unit.getWingDrag()
        
        kachestva[P_ATACK_POWER]=0;
        kachestva[P_ATACK_DISTANCE]=0;
        
        //---------
        double damage=0.0,
               range=unit.getMaxWeaponRange();//0.0;
        // TODO use getOnlyTargetCategory() !!!
        for (WeaponMount weaponMount:unit.getWeaponMounts()) {
            WeaponDef weapon=weaponMount.getWeaponDef();
            if (!weapon.isParalyzer()  // Если не парализует... 
                && weapon.isAbleToAttackGround() && !weapon.isShield()) { // и т.д.
                //range = Math.max(range, weapon.getRange());
                //weapon.getDamage().getTypes() !!!!!!
                owner.sendTextMsg(" Weapon types: "+weapon.getDamage().getTypes().toString(), FieldBOT.MSG_DBG_ALL);

                float dMultipler=weapon.getSalvoSize()*weapon.getProjectilesPerShot();
                float dmg= weapon.getDamage().getTypes().get(weaponDamagType);
                dmg = 0.8f*dmg + 0.2f*dmg*weapon.getAreaOfEffect();// как повреждения распространяются по площади.
                 //TODO дополнительный параметр по оружию: насколько "массово" должно быть оружие - против толпы мелких, например.
                    //weapon.getDamage().getImpulseBoost();
              owner.sendTextMsg(" Weapon "+weapon.getName()+" damage="+dmg+" x multipler="+dMultipler+" (isSelfExplode="+weapon.isSelfExplode()+" isNoSelfDamage="+weapon.isNoSelfDamage()+" isParalyzer="+weapon.isParalyzer()+" isShield="+weapon.isShield()+" isAbleToAttackGround="+weapon.isAbleToAttackGround()+");" ,FieldBOT.MSG_DBG_ALL);
                damage += dmg*dMultipler;
            } else owner.sendTextMsg(" Weapon "+weapon.getName()+" isSelfExplode="+weapon.isSelfExplode()+", isNoSelfDamage="+weapon.isNoSelfDamage()+" isParalyzer="+weapon.isParalyzer()+" isShield="+weapon.isShield()+" isAbleToAttackGround="+weapon.isAbleToAttackGround()+";" ,FieldBOT.MSG_DBG_ALL);
        }
        if (unit.isAbleToKamikaze()) {
            damage=0;
            owner.sendTextMsg(" Unit able to kamikadze. Weapon damage skip to 0. Unit="+unit.getName()+";" ,FieldBOT.MSG_DBG_ALL);
        }
        //---------
        
        kachestva[P_ATACK_POWER]=damage; // за оружие
        kachestva[P_ATACK_DISTANCE]=range; // за расстояние
        
        kachestva[P_RADAR]=Math.max(unit.getRadarRadius() , unit.getLosRadius()); // за радиус радара
        if (unit.isStealth()) kachestva[P_RADAR]=1.0f; else kachestva[P_RADAR]=0.0f; // за скрытность
        if (unit.isAbleToAssist()) kachestva[P_CONSTRUCTOR]=unit.getBuildSpeed(); else kachestva[P_CONSTRUCTOR]=0.0; // за строительства
        
        return kachestva;
    }
    
    /**
     * Return one value from array value
     * @param unitK unit kachestva
     * @param normalizeTo_maxK max of all units from selected kachestva
     * @param kachestva required kachestva
     * @return summ (or multiply) of unitK[j]/normalizeTo_maxK[j] * kachestva[j]
     */
    private double svestiZnacheniyaK(double unitK[], double normalizeTo_maxK[], float kachestva[]) {
        double k; // + or *  , 0 or 1  ? choose best formul!
        if (USE_MULTIPLY) k=1; else k=0;
        for (int j=0;j<NUM_P;j++) {
            double x = unitK[j]/normalizeTo_maxK[j] * kachestva[j];
            if (USE_MULTIPLY) { // * or +
                k *= (1.0 + x); // !!!
            } else k += x;
        }
        if (USE_MULTIPLY) k-=1.0;
        return k;
    }
    
    /**
     * Normalize max value up to 1.0.
     * @param v 
     */
    private void noramilzeVector(float[] v) {
        float max=v[0];
        for (int i=1;i<v.length; i++) if (v[i]>max) max=v[i];
        for (int i=0;i<v.length; i++) v[i] = v[i]/max;
    }

    /**
     * Math optimal build plan for army to maximize query properties.
     * @param onBase base for product army (for check build power, and non-assistable build power depends TODO)
     * @param chooseBuilding false - select only from moving unit, true - only non-moving (stationar) unit.
     * @param buildVariants list of all variants for selected
     * @param timeLimit time linit for build all army
     * @param unitLimit unit limit, or Integer.MAX_VALUE for unlimited.
     * @param kachestva specific unit selected properties (from 0 - not select up to 1 - best required)
     * @return list of [unit type, count] or null.
     */
    public HashMap<UnitDef,Integer> shooseArmy(TBase onBase, boolean chooseBuilding, ArrayList<UnitDef> buildVariants, float timeLimit, int unitLimit, float kachestva[], int weaponDamageType)
    {
        if (kachestva.length!=NUM_P) throw new ArrayIndexOutOfBoundsException();// !!!
        if (buildVariants.isEmpty()) throw new ArrayIndexOutOfBoundsException();// !!!        
        
        // 1. Select all moving/no-moving unit on this base surface
        ArrayList<UnitDef> buildVars=new ArrayList<UnitDef>();
        for (UnitDef def:buildVariants)
          if (ModSpecification.isRealyAbleToMove(def)!=chooseBuilding
                  && onBase.canBuildOnBase(def, false) // TODO <- this check was realy required?
                  )
          {
            // filter that realy can not pass for kachestva[].
            double current_k[]=getUnitKachestva(def, weaponDamageType);
            boolean check=false;
            for (int i=0;i<NUM_P;i++) if ((kachestva[i]!=0)&&(current_k[i]!=0)) {
                check=true;
                break;
            }
            if (check) buildVars.add(def);
        }
        
        if (buildVars.isEmpty()) return null; // !!! If not variant to choose.

        //2. Select profit coast
        double min_k[]=new double[NUM_P];
        double max_k[]=new double[NUM_P];
        
        // 2.1 Get max, min for normalization vector
        min_k=getUnitKachestva(buildVariants.get(0), weaponDamageType);
        max_k=getUnitKachestva(buildVariants.get(0), weaponDamageType);
        for (UnitDef unit: buildVariants) {
            double current_k[]=getUnitKachestva(unit, weaponDamageType);
            for (int i=0;i<NUM_P;i++) {
                if (current_k[i]>max_k[i]) max_k[i]=current_k[i];
                if (current_k[i]<min_k[i]) min_k[i]=current_k[i];
            }
        }
        for (int i=0;i<NUM_P;i++) if (max_k[i]==0) max_k[i]=1.0f; // !!!!
        
        noramilzeVector(kachestva); // normalize vector: from 0 up to 1.0.
        
        // 2.2 Назначение таблицы коэффициентов и матрицы стоимости
        
        // Определение кто кого может строить и кому помогать.
        // TODO check: builder realy can build it
        
        // Resource limit borders.
        int numRes=owner.avgEco.resName.length;
        int numTimeRes=1;//!!!
        int posTimeRes=numRes;//!!!
        int posCountLimit=posTimeRes+numTimeRes;//!!!
        if (unitLimit==Integer.MAX_VALUE) posCountLimit=-1;
        int numAllRes=numRes+numTimeRes;// !
        if (posCountLimit>=0) numAllRes++;
        
        
        double M_k[]=new double[buildVars.size()];
        double M_res[][]=new double[numAllRes][buildVars.size()];
        
        double K_PRECISSION=1.0e+4; // for precission multipler. Or using precission on simplex method.
        if (USE_MULTIPLY) K_PRECISSION*=1e+6;// !!!
        
        for (int i=0; i<buildVars.size(); i++) { //!!
            UnitDef unit = buildVars.get(i);
                    
            double unitK[]=getUnitKachestva(unit, weaponDamageType);
            
            // TODO Use svestiZnacheniyaK()
            double k; // + or *  , 0 or 1  ? choose best formul!
            if (USE_MULTIPLY) k=1; else k=0;
            for (int j=0;j<NUM_P;j++) {
                double x = unitK[j]/max_k[j] * kachestva[j]; // normalize values
                if (USE_MULTIPLY) { // * or +
                    k *= (1.0 + x); // !!!
                } else k += x;
            }
            if (USE_MULTIPLY) k-=1.0;
            M_k[i]=k * K_PRECISSION;// test
            
            for (int j=0;j<numRes;j++) M_res[j][i]=unit.getCost(owner.avgEco.resName[j]); //было M_res[i][j]
            M_res[posTimeRes][i]=unit.getBuildTime(); // build time(required build power) as resource
            // TODO 
            if (posCountLimit>=0) M_res[posCountLimit][i]=1.0; // count/unit limit as resource

        }

        float tmpEndTRes[]=owner.avgEco.getAvgCurrentFromTime(timeLimit, true); // !!!
        double V_Res[]=new double[numAllRes]; // Вектор ограничения ресурсов (max resource)
        for (int i=0;i<numRes;i++) V_Res[i]=tmpEndTRes[i];

        V_Res[posTimeRes]=onBase.getBuildPower(false, false) * timeLimit;// TODO OnlyFactory for NO ASSISTABLE (NOTA) !!!
        // TODO different factoru and factoru count
        // it will be best for NOTA
        
        if (posCountLimit>=0) V_Res[posCountLimit]=unitLimit +0.05; // Ограничение на колличество, +0.05 на погрешность вычислений!!!
         
        // 3 Отправка в симплекс-метод
        // V_Res[] - Вектор ограничения ресурсов (максимум)
        // M_k[] - Вектор коэффициента полезности
        // M_res[][] - матрица затрат
        
        // Целевая функция M_k => max ... или M_res => min добавить?

        double simplOtvet[]=new double[buildVars.size()];// !!!
        
        // -DEBUG-
//        owner.sendTextMsg("M_res.length="+M_res.length+" M_res[0].length="+M_res[0].length+" V_Res.length="+V_Res.length+" M_k.length="+M_k.length, FieldBOT.MSG_DLG); // DEBUG !!!
//        owner.sendTextMsg("Potimization data:", FieldBOT.MSG_DLG); // DEBUG !!!
//        
//        String mresTXT="", vresTXT="", mkTXT="";
//        for (int i=0;i<M_res.length;i++) {
//            mresTXT+="{";
//            for (int j=0;j<M_res[i].length;j++) {
//                mresTXT+=M_res[i][j];
//                if (i<M_res[i].length-1) mresTXT+=", ";
//            }
//            mresTXT+="}";
//            if (i<M_res.length-1) mresTXT+=", ";
//        }
//        for (int i=0;i<V_Res.length;i++) {
//            vresTXT+=V_Res[i];
//            if (i<V_Res.length-1) vresTXT+=", ";
//        }
//        for (int i=0;i<M_k.length;i++) {
//            mkTXT+=M_k[i];
//            if (i<M_k.length-1) mkTXT+=", ";
//        }
//        owner.sendTextMsg("M_res={"+mresTXT+"};", FieldBOT.MSG_DBG_ALL);
//        owner.sendTextMsg("V_Res={"+vresTXT+"};", FieldBOT.MSG_DBG_ALL);
//        owner.sendTextMsg("M_k={"+mkTXT+"};", FieldBOT.MSG_DBG_ALL);
        // -DEBUG end-
        
        
        
        double resK=MathOptimizationMethods.simplexDouble(M_res, V_Res, M_k, simplOtvet);
        if (resK<=0) return null;// If no one plan do not profit
        
        // 4 Обработка результата
        HashMap<UnitDef,Integer> outArmy=new HashMap<UnitDef,Integer>();
        for (int i=0; i<buildVars.size(); i++) {
            if (simplOtvet[i]!=0) {
                UnitDef unit = buildVars.get(i);
                int n=(int)Math.round(simplOtvet[i]); // !!!!!
                outArmy.put(unit, n);
            }
        }

        return outArmy;
    }

    /**
     * Select one of max better kachestva unit.
     * @param chooseBuilding
     * @param buildVariants
     * @param kachestva
     * @return best UnitDef with max compare (better) for kachestva. Or null
     */
    public UnitDef shooseBestFor(boolean chooseBuilding, ArrayList<UnitDef> buildVariants, float kachestva[], int weaponDamageType)
    {
        if (kachestva.length!=NUM_P) throw new ArrayIndexOutOfBoundsException();// !!!
        if (buildVariants.isEmpty()) throw new ArrayIndexOutOfBoundsException();// !!!        
        
        // 1. Select all moving/no-moving unit on this base surface
        ArrayList<UnitDef> buildVars=new ArrayList<UnitDef>();
        for (UnitDef def:buildVariants)
          if (ModSpecification.isRealyAbleToMove(def)!=chooseBuilding)
          {
            // filter that realy can not pass for kachestva[].
            double current_k[]=getUnitKachestva(def, weaponDamageType);
            boolean check=false;
            for (int i=0;i<NUM_P;i++) if ((kachestva[i]!=0)&&(current_k[i]!=0)) {
                check=true;
                break;
            }
            if (check) buildVars.add(def);
        }
        
        if (buildVars.isEmpty()) return null; // !!! If not variant to choose.

        //2. Select profit coast
        double min_k[];
        double max_k[];
        
        // 2.1 Get max, min for normalization vector
        min_k=getUnitKachestva(buildVariants.get(0), weaponDamageType);
        max_k=getUnitKachestva(buildVariants.get(0), weaponDamageType);
        for (UnitDef unit: buildVariants) {
            double current_k[]=getUnitKachestva(unit, weaponDamageType);
            for (int i=0;i<NUM_P;i++) {
                if (current_k[i]>max_k[i]) max_k[i]=current_k[i];
                if (current_k[i]<min_k[i]) min_k[i]=current_k[i];
            }
        }
        for (int i=0;i<NUM_P;i++) if (max_k[i]==0) max_k[i]=1.0f; // !!!!
        
        noramilzeVector(kachestva); // normalize vector: from 0 up to 1.0.
        
        // 2.2
        double maxK=0.0;
        UnitDef bestUnit=null;
        
        for (UnitDef unit:buildVariants) {
            double unitK[]=getUnitKachestva(unit, weaponDamageType);
            double k= svestiZnacheniyaK(unitK, max_k, kachestva);
            
            if (bestUnit==null || k>maxK) { // TODO if k==maxK
                maxK=k;
                bestUnit=unit;
            }
        }

        return bestUnit;
    }
    
    /**
     * Return list of all unit type of all unit in list in argument
     * This method is copy and modifed from AGroupManager.getContainUnitDefsList() TODO share using!
     * @param fromUnits select UnitDef from this unit list
     * @return list of UnitDefs
     */
    public static HashSet<UnitDef> getUnitDefsFromUnitList(List<Unit> fromUnits) {
        HashSet<UnitDef> currentUnitTypesSet=new HashSet<UnitDef>(); // Unit types
        for (Unit unit:fromUnits) currentUnitTypesSet.add(unit.getDef());
        return currentUnitTypesSet;
    }
    /**
     * Select unit from allUnits where unit.getDef()==selectOnly;
     * @param allUnits list for select
     * @param selectOnly what unitDef select in list
     * @return units with selectOnly UnitDef
     */
    public static ArrayList<Unit> selectUnitWithDef(ArrayList<Unit> allUnits, UnitDef selectOnly) {
        ArrayList<Unit> selected=new ArrayList<Unit>();
        for (Unit unit:allUnits) if (selectOnly.equals(unit.getDef())) selected.add(unit);
        return selected;
    }
    
    /**
     * Select units, best for kachestva
     * @param chooseBuilding select building or moving objects
     * @param units
     * @param kachestva
     * @return units with one UnitDef, better for compare (or max) to kachestva
     */
    public ArrayList<Unit> shooseBestFrom(boolean chooseBuilding, ArrayList<Unit> units, float kachestva[],int weaponDamageType)
    {
        HashSet<UnitDef> unitDefs=getUnitDefsFromUnitList(units);
        ArrayList<UnitDef> unitDefLst=new ArrayList<UnitDef>(unitDefs);
        UnitDef bestUDef= shooseBestFor(chooseBuilding, unitDefLst, kachestva, weaponDamageType);
        if (bestUDef==null) return null;
        return selectUnitWithDef(units, bestUDef);
    }
    
    // TODO choose war tech level

    
    private boolean baseBuildDef=false;
    
    /**
     * Army control tick. TODO
     * @param frame 
     */
    public void update(int frame) {
        if (!makeArmy) return;
        
        // TODO !!!
        if (frame%500==4) {

            boolean haveRes=true;
            float eco[][]=owner.avgEco.getAVGResourceToArr();
            for (int i=0;i<eco[0].length; i++) {
                if ( !(eco[AdvECO.R_Current][i]/eco[AdvECO.R_Storage][i]>0.5f && 
                     eco[AdvECO.R_Income][i]*1.1f>eco[AdvECO.R_Usage][i]) )
                        haveRes=false;
            }


            if (haveRes) {
                boolean defenceTower=false;

                if (!baseBuildDef) {
                    defenceTower=true;
                    baseBuildDef=true;
                }

                float timeL=3*60;
                int unitL=5; //Integer.MAX_VALUE;
                float kachestva[]={ 0.1f, 0.3f, 0.4f, 0.7f, 0.2f, 0.1f,0.1f, 0.0f }; // spam, speed, health, atack, range, radar, stealth
                if (defenceTower) kachestva[P_SPEED]=0;

                // Choose best base for make army
                TBase bestBase=null; // TODO change to owner.getBestBase(false)
                HashSet<UnitDef> lstOfUDefs=new HashSet<UnitDef>();
                for (TBase base:owner.selectTBasesList()) {
                    HashSet<UnitDef> tmpUDefs=base.getBuildList(true);
                    if (tmpUDefs.size()>lstOfUDefs.size() && base.currentBaseTarget.isEmpty()) { // idle base.
                        bestBase=base;
                        lstOfUDefs=tmpUDefs;
                    }
                }

                if (bestBase!=null)
                {
                    int weaponType=0;//!!! 0 - default. TODO
                    HashMap<UnitDef,Integer> armyUnits
                            =owner.warStrategy.shooseArmy(bestBase, !defenceTower, new ArrayList<UnitDef>(lstOfUDefs),timeL,unitL,kachestva, weaponType);
                        // TODO special unit enabled/disabled check (research center depends, TA/RD mod!)

                    // Send signal to base for make unit
                    if (armyUnits!=null) {
                        for (Map.Entry<UnitDef,Integer> armyItem:armyUnits.entrySet())
                          if (armyItem.getValue()>0) // !!!!
                        {
                          owner.sendTextMsg(" army>"+armyItem.getKey().getHumanName()+" * "+armyItem.getValue(), FieldBOT.MSG_DBG_SHORT);
                          if (!defenceTower) bestBase.currentBaseTarget.add(armyItem.getKey(), armyItem.getValue());
                          else {
                              for (int i=0;i< armyItem.getValue();i++) {
                                  AIFloat3 defPoint=MathPoints.getRandomPointOnRadius(bestBase.center, bestBase.radius);
                                  bestBase.currentBaseTarget.add(armyItem.getKey(), 1, defPoint);// TODO test.
                              }
                          }
                        }
                    } else owner.sendTextMsg("No plan for army!", FieldBOT.MSG_DBG_SHORT);                        
                }
            }

            // Make war group
            boolean needMakeWarGroup=true;
            if (needMakeWarGroup) {
                for (TBase base:owner.selectTBasesList()) if (base.currentBaseTarget.isEmpty()) {
                    if (base.army.size()>2) {
                        // TODO only movable units!!!
                        TArmyGroup army=new TArmyGroup(owner);
                        ArrayList transferUnits=new ArrayList(base.army);
                        if (base.removeAllUnits(transferUnits)) {
                            army.addAllUnits(transferUnits);
                            owner.addSmartGroup(army);
                        }
                    }
                }
            }
            
            // Scout/Atack
            for (AGroupManager sGroup:owner.smartGroups)
            {
                if (sGroup instanceof TArmyGroup) {
                    TArmyGroup army=(TArmyGroup)sGroup;
                    if (army.isEmpty()) {
                        owner.removeSmartGroup(sGroup); // Remove destroyed army.
                    } else {
                        if (army.moveTarget==null) {
                            AIFloat3 p=owner.scoutModule.getPointForMoveArmy(army.getCenter(), 0);
                            army.moveTo(p); // TODO
                        }
                    }
                }
                // TODO scout group
            }
        }
    }
    
    
}
