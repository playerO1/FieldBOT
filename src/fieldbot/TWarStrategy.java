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
import fieldbot.AIUtil.UnitSelector;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
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
    public final static String P_NAMES_forParsing[][]={{"spam","units"}, {"speed","fast"}, {"health","HP"}, {"atack","weapon","damage"}, {"range","distance","long"}, {"radar"}, {"stealth","hidde"},{"constructor","work"}};
    
    // Predefine template for kachestva:
    public static final float KACHESTVA_ARMY[]={ 0.002f, 0.16f, 0.4f, 1.0f, 0.3f, 0.06f,0.01f, 0.0f }; // spam, speed, health, atack, range, radar, stealth, worker
    public static final float KACHESTVA_DEFTOWER[]={ 0, 0, 0.05f, 1.0f, 0.3f, 0.005f,0.002f, 0.0f };
    public static final float KACHESTVA_RADAR[]=   { 0, 0, 0, 0, 0, 1.0f, 0, 0 };

    public static final float KACHESTVA_U_SCOUT[]={ 0.15f, 0.9f, 0.12f, 0.2f, 0.1f, 0.6f,0.36f, 0.0f };
    public static final float KACHESTVA_U_RAIDER[]={ 0, 0.1f, 0.6f, 1.0f, 0.19f, 0.01f,0.02f, 0.0f };
    public static final float KACHESTVA_U_LRATACK[]={ 0, 0.01f, 0.3f, 0.6f, 1.0f, 0.01f,0.01f, 0.0f };
    
    private final FieldBOT owner;
    
    private final boolean USE_MULTIPLY=false; // Использовать мультипликативную модель для оценки качеств юнитов (иначе адитивная модель)
    private final double MULT_ADD=1.0; // TODO theck this line. 0.0- only with all kachestva, 1.0 - more additive modeling on mul
    
    /**
     * Enable army control
     */
    public boolean makeArmy;
    
    public TWarStrategy(FieldBOT owner) {
        this.owner=owner;
        makeArmy=false; // TODO set true by default
    }
    
    //TODO move getZeroVector(int) to Utils
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
        
        //---------
        double damage=0.0,
               range=unit.getMaxWeaponRange();//0.0;
        // TODO use getOnlyTargetCategory() !!!
        if (unit.isAbleToAttack())
          for (WeaponMount weaponMount:unit.getWeaponMounts())
        {
            WeaponDef weapon=weaponMount.getWeaponDef();
            if (!weapon.isParalyzer() 
                && weapon.isAbleToAttackGround() && !weapon.isShield()) { // и т.д.
                //range = Math.max(range, weapon.getRange());
                //weapon.getDamage().getTypes() !!!!!!
//                owner.sendTextMsg(" Weapon types: "+weapon.getDamage().getTypes().toString(), FieldBOT.MSG_DBG_ALL);

                float dMultipler=weapon.getSalvoSize()*weapon.getProjectilesPerShot();
                float dmg= weapon.getDamage().getTypes().get(weaponDamagType);
                dmg = 0.8f*dmg + 0.2f*dmg*weapon.getAreaOfEffect();// как повреждения распространяются по площади.
                 //TODO дополнительный параметр по оружию: насколько "массово" должно быть оружие - против толпы мелких, например.
                    //weapon.getDamage().getImpulseBoost();
//              owner.sendTextMsg(" Weapon "+weapon.getName()+" damage="+dmg+" x multipler="+dMultipler+" (isSelfExplode="+weapon.isSelfExplode()+" isNoSelfDamage="+weapon.isNoSelfDamage()+" isParalyzer="+weapon.isParalyzer()+" isShield="+weapon.isShield()+" isAbleToAttackGround="+weapon.isAbleToAttackGround()+");" ,FieldBOT.MSG_DBG_ALL);
                damage += dmg*dMultipler;
            } // DEBUG: else owner.sendTextMsg(" Weapon "+weapon.getName()+" isSelfExplode="+weapon.isSelfExplode()+", isNoSelfDamage="+weapon.isNoSelfDamage()+" isParalyzer="+weapon.isParalyzer()+" isShield="+weapon.isShield()+" isAbleToAttackGround="+weapon.isAbleToAttackGround()+";" ,FieldBOT.MSG_DBG_ALL);
        }
        if (unit.isAbleToKamikaze()) {
            damage=0;
//            owner.sendTextMsg(" Unit able to kamikadze. Weapon damage skip to 0. Unit="+unit.getName()+";" ,FieldBOT.MSG_DBG_ALL);
        }
        //---------
        if (damage==0.0) range=0;
        kachestva[P_ATACK_POWER]=damage; // за оружие
        kachestva[P_ATACK_DISTANCE]=range; // за расстояние
        
        kachestva[P_RADAR]=Math.max((float)unit.getRadarRadius()*8 , unit.getLosRadius()); // max see radius TODO test radar radius
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
                k *= (MULT_ADD + x); // !!!
            } else k += x;
        }
        if (USE_MULTIPLY) k-=MULT_ADD;
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
                    k *= (MULT_ADD + x); // !!!
                } else k += x;
            }
            if (USE_MULTIPLY) k-=MULT_ADD;
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
        
        
        
        double resK;
        if (unitLimit==1) { // more fast optimization
            resK=MathOptimizationMethods.justOneMaximize(M_res, V_Res, M_k, simplOtvet);
            //TODO test justOneMaximize!
        } else { // more smart optimization
            resK=MathOptimizationMethods.simplexDouble(M_res, V_Res, M_k, simplOtvet);
        }
        if (resK<=0) return null;// If no one plan do not profit
        
        // 4 Обработка результата
        HashMap<UnitDef,Integer> outArmy=new HashMap<UnitDef,Integer>();
        for (int i=0; i<buildVars.size(); i++) {
            if (simplOtvet[i]!=0) {
                UnitDef unit = buildVars.get(i);
                int n=(int)Math.round(simplOtvet[i]); //TODO check Math.round on this line !!!!!
                if (n>0) outArmy.put(unit, n);
            }
        }

        return outArmy;
    }

    public void sendBuildMessageToBase(HashMap<UnitDef,Integer> armyUnits, TBase toBase, boolean defenceTower)
    {
        for (Map.Entry<UnitDef,Integer> armyItem:armyUnits.entrySet())
          if (armyItem.getValue()>0) // !!!!
        {
          owner.sendTextMsg(" army>"+armyItem.getKey().getHumanName()+" * "+armyItem.getValue(), FieldBOT.MSG_DBG_SHORT);
          if (!defenceTower) toBase.currentBaseTarget.add(armyItem.getKey(), armyItem.getValue());
          else {
              for (int i=0;i< armyItem.getValue();i++) {
                  AIFloat3 defPoint; // TODO check on null!
                  int nTry=0;
                  do {
                    defPoint=MathPoints.getRandomPointOnRadius(toBase.center, toBase.radius*(0.5f+0.2f*(float)Math.random()));
                    nTry++;
                  } while 
                    (!owner.clb.getMap().isPossibleToBuildAt(armyItem.getKey(), defPoint, toBase.BUILD_FACING) 
                     || nTry<10 );
                  toBase.currentBaseTarget.add(armyItem.getKey(), 1, defPoint);// TODO set defence tower position better.
              }
          }
        }
    }
    
    /**
     * Select one of max better kachestva unit.
     * @param chooseBuilding
     * @param buildVariants
     * @param kachestva
     * @param weaponDamageType
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
     * Select units, best for kachestva
     * @param chooseBuilding select building or moving objects
     * @param units
     * @param kachestva
     * @return units with one UnitDef, better for compare (or max) to kachestva
     */
    public ArrayList<Unit> shooseBestFrom(boolean chooseBuilding, ArrayList<Unit> units, float kachestva[],int weaponDamageType)
    {
        HashSet<UnitDef> unitDefs=UnitSelector.getUnitDefsFromUnitList(units);
        ArrayList<UnitDef> unitDefLst=new ArrayList<UnitDef>(unitDefs);
        UnitDef bestUDef= shooseBestFor(chooseBuilding, unitDefLst, kachestva, weaponDamageType);
        if (bestUDef==null) return null;
        return UnitSelector.selectUnitWithDef(units, bestUDef);
    }
    
// ---- Tech Lewel chooise ----
    // TODO choose war tech level
    
    /**
     * Select better Tech Level that have now
     * @param TechLvls list for choose
     * @param currDamage for compare level, best that this eco product
     * @param compareByDamageType index on currDamage[]; if -1 then compare by all damage type
     * @param currentMaxRange - max weapon range of one better unit (better by range); if Float.POSITIVE_INFINITY then range will be not check
     * @param compareByDefenceTower - if true then choose level by stationary unit (defence tower), if false then choose by movable unit
     * @return list with Tech Levels when same items of currEcoLvl[] > that currentEco[]
     */
    public ArrayList<TTechLevel> selectLvl_whoBetterThat_damage(ArrayList<TTechLevel> TechLvls, float[] currDamage, int compareByDamageType, float currentMaxRange, boolean compareByDefenceTower)
    {
        ArrayList<TTechLevel> bestLvls=new ArrayList<TTechLevel>();
        for (TTechLevel level:TechLvls) {
//            float nFactor=0;
            float k=0;
            float lvlDmgArr[];// what compare - defence tower or unit?
            if (compareByDefenceTower)
                lvlDmgArr=level.maxAtackPower_def;
                else lvlDmgArr=level.maxAtackPower_unit;
            
            if (compareByDamageType<0) { // by all damage type
                for (int i=0;i<currDamage.length;i++) {
                    float delta=lvlDmgArr[i]-currDamage[i];
                    if (delta>0) {
                        k+=delta;
    //                  nFactor++;
                    }
                }
            } else k=lvlDmgArr[compareByDamageType]-currDamage[compareByDamageType];

            float rangeK;
            if (compareByDefenceTower)
                rangeK=level.maxAtackRange_def-currentMaxRange;
                else rangeK=level.maxAtackRange_unit-currentMaxRange;;
            if (rangeK>0) {
                k+=rangeK /100; // TODO normalized atack range by atack damage!
//                nFactor++;
            }
                    
            if (k>0.0f) bestLvls.add(level);
        }
        return bestLvls;
    }
    
    

    // ---- War control ----
    
    private int armyTypeStage=0; // 0 - tower, 1 - radar, 2 and more - army
    private int armyControlStage=-1;
    private short lastBuildScoutGroup=0; // 2 - true
    
    /**
     * Army control tick.
     * @param frame 
     */
    public void update(int frame) {
        if (!makeArmy) return;
        
        // TODO Army control
        
        
            // Scout/Atack
        if (frame%400==105 || frame%400==305) {
            for (AGroupManager sGroup:owner.smartGroups)
            {
                if (sGroup instanceof TArmyGroup) {
                    TArmyGroup army=(TArmyGroup)sGroup;
                    if (army.isEmpty()) {
                        owner.removeSmartGroup(sGroup); // Remove destroyed army.
                    } else {
                        if (!army.haveSpecialCommand()) {
                            AIFloat3 p=owner.scoutModule.getPointForMoveArmy(army.getCenter(), 0);
                            army.moveTo(p); // TODO army.moveTo
        owner.sendTextMsg("army: send to point: "+p, FieldBOT.MSG_DBG_SHORT);
                        }
                    }
                }
                // TODO scout group
            }
        }
        
        switch (armyControlStage) {
         case 0: // Make war group
            boolean needMakeWarGroup=true;
            if (needMakeWarGroup) {
                for (TBase base:owner.selectTBasesList())
                  if (!base.haveSpecialCommand()) // if building army finished
                {
                    if (base.army.size()>2) {
                        // TODO only movable units!!!
                        TArmyGroup army=new TArmyGroup(owner);
                        ArrayList transferUnits=new ArrayList(base.army);
                        if (base.removeAllUnits(transferUnits)) {
                            army.addAllUnits(transferUnits);
                            owner.addSmartGroup(army);
        owner.sendTextMsg("new army created:"+army, FieldBOT.MSG_DLG);
                            if (lastBuildScoutGroup==2) {
                                army.targetScout = true;
                                lastBuildScoutGroup=0;
                            }
                        } else owner.sendTextMsg("new army CAN NOT Create: unit not removed from group, size="+transferUnits.size(), FieldBOT.MSG_DLG);
                    }
                }
            }
            
          armyControlStage++;
        break;
        
        case 1:
            // empty action
          armyControlStage++;
        break;
            
        case 2:
            // build army units
            boolean haveRes=true;
            float eco[][]=owner.avgEco.getAVGResourceToArr();
            float minResPercent=0.5f;
            if (armyTypeStage<3) minResPercent=0.37f;
            for (int i=0;i<eco[0].length; i++) {
                if ( !(eco[AdvECO.R_Current][i]/eco[AdvECO.R_Storage][i]>minResPercent && 
                     eco[AdvECO.R_Income][i]*0.95f>eco[AdvECO.R_Usage][i]) )
                        haveRes=false; //TODO if res>2 add breack for optimization;
            }

            if (haveRes)
            {
        owner.sendTextMsg("army: haveRes, start make unt, i="+armyTypeStage, FieldBOT.MSG_DBG_SHORT);
                boolean defenceTower=false; // build defence tower, or atack army
                boolean onlyRadar=false; // work if defence tower only
                if (armyTypeStage<2) {
                    defenceTower=true;
                    onlyRadar = armyTypeStage==1;
                }

                // Choose best base for make army
                TBase bestBase=owner.getBestBase(true);
               if (bestBase==null) {
                   owner.sendTextMsg("army: not best base", FieldBOT.MSG_DBG_SHORT);
                   break; // EXIT from case section
               }
                    
                ArrayList<UnitDef> unitTypes=new ArrayList<UnitDef>(bestBase.getBuildList(true));

                float timeL=2*60;
                int unitL=10; //Integer.MAX_VALUE;
                float kachestva[]=KACHESTVA_ARMY; // don't forget that do not modify kachestva items - becouse there is link to template.
                if (defenceTower) {
                    if (onlyRadar) {
                        unitL=1;
                        timeL=45;
                        kachestva=KACHESTVA_RADAR;
                    } else {
                        unitL=4;
                        timeL=1*60;
                        kachestva=KACHESTVA_DEFTOWER;
                        if (KACHESTVA_DEFTOWER[P_CONSTRUCTOR]<=0) // select nano towers, if in KACHESTVA_DEFTOWER[] exist work
                          unitTypes=UnitSelector.atackUnits(unitTypes); // select only able to atack buildings
                        // TODO preselect this for movable unit too, if not for radar/spam/work only.
                    }
                } else {
                    switch (armyTypeStage%7) {
                        case 2:
                            unitL=7;
                            kachestva=KACHESTVA_U_SCOUT;
                            if (lastBuildScoutGroup==0) lastBuildScoutGroup=1;
                            break;
                        case 3:// see case 4
                        case 4:
                            kachestva=KACHESTVA_U_RAIDER;
                            break;
                        case 5:
                            kachestva=KACHESTVA_U_LRATACK;
                            break;
                        case 6:
                            kachestva=KACHESTVA_U_RAIDER;
                            break;
                        //case 0: tower / KACHESTVA_ARMY
                        //case 1: radar / KACHESTVA_ARMY
                    }
                }

                armyTypeStage++; // inc stage
                if (armyTypeStage>30) armyTypeStage=0;
                if (armyTypeStage>5 && armyTypeStage<10) {
                    // do not do army now, concentrate to eco.
                } else {
            owner.sendTextMsg("army: bestBase="+bestBase, FieldBOT.MSG_DLG);
                    int weaponType=0;//!!! 0 - default. TODO
                    HashMap<UnitDef,Integer> armyUnits
                            =owner.warStrategy.shooseArmy(bestBase, defenceTower, unitTypes ,timeL,unitL,kachestva, weaponType);
                        // TODO special unit enabled/disabled check (research center depends, TA/RD mod!)
                    // Send signal to base for make unit
                    if (armyUnits!=null) {
                        // TODO move and create method for translate Map into TBase commands to build.
                        sendBuildMessageToBase(armyUnits, bestBase, defenceTower);
                        if (lastBuildScoutGroup==1) lastBuildScoutGroup=2;
                        //    else lastBuildScoutGroup=0;
                    } else owner.sendTextMsg("No plan for army!", FieldBOT.MSG_DBG_SHORT);                        
                }

            }
            armyControlStage++;
          break;
            
            default: // clock of activation
                if (frame%400==50 && frame>700) armyControlStage=0;

        }
    }
    
    // -----------
    
}
