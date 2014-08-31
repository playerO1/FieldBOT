/*
 * Copyright (C) 2014 user2
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

import com.springrts.ai.oo.clb.UnitDef;
import com.springrts.ai.oo.clb.WeaponDef;
import com.springrts.ai.oo.clb.WeaponMount;
import fieldbot.AIUtil.MathOptimizationMethods;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import org.apache.commons.math3.optim.MaxIter;
import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.linear.LinearConstraint;
import org.apache.commons.math3.optim.linear.LinearConstraintSet;
import org.apache.commons.math3.optim.linear.LinearObjectiveFunction;
import org.apache.commons.math3.optim.linear.NonNegativeConstraint;
import org.apache.commons.math3.optim.linear.Relationship;
import org.apache.commons.math3.optim.linear.SimplexSolver;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;

/*
import org.apache.commons.math3.optim.MaxIter;
import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.linear.LinearConstraint;
import org.apache.commons.math3.optim.linear.LinearConstraintSet;
import org.apache.commons.math3.optim.linear.LinearObjectiveFunction;
import org.apache.commons.math3.optim.linear.NonNegativeConstraint;
import org.apache.commons.math3.optim.linear.Relationship;
import org.apache.commons.math3.optim.linear.SimplexSolver;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
*/

/**
 *
 * @author user2
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
    
    public final FieldBOT owner;
    
    private final boolean USE_MULTIPLY=true; // Использовать мультипликативную модель для оценки качеств юнитов (иначе адитивная модель)
    private final boolean USE_SMART_MULTIPLY=true; // Использовать сглаженную мультипликативную модель (при перемножении с нулями не нулём будет)
    
    
    public TWarStrategy(FieldBOT owner) {
        this.owner=owner;
    }
    
    
    private double[] getZeroVector(int size) {
        double v[] = new double[size];
        Arrays.fill(v, 0.0); //for (int j=0;j<size;j++) v[j]=0.0; // zero vector
        return v;
    }
    
    private double[] getUnitKachestva(UnitDef unit) {
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
                float dmg= weapon.getDamage().getTypes().get(0); // get(0) - default damage
                dmg = 0.8f*dmg + 0.2f*dmg*weapon.getAreaOfEffect();// как повреждения распространяются по площади.
                 //TODO дополнительный параметр по оружию: насколько "массово" должно быть оружие - против толпы мелких, например.
                    //weapon.getDamage().getImpulseBoost();
              owner.sendTextMsg(" Weapon "+weapon.getName()+" damage="+dmg+" x multipler="+dMultipler+" (isSelfExplode="+weapon.isSelfExplode()+" isNoSelfDamage="+weapon.isNoSelfDamage()+" isParalyzer="+weapon.isParalyzer()+" isShield="+weapon.isShield()+" isAbleToAttackGround="+weapon.isAbleToAttackGround()+");" ,FieldBOT.MSG_DBG_ALL);
                damage += dmg*dMultipler; // TODO Test
            } else owner.sendTextMsg(" Weapon "+weapon.getName()+" isSelfExplode="+weapon.isSelfExplode()+", isNoSelfDamage="+weapon.isNoSelfDamage()+" isParalyzer="+weapon.isParalyzer()+" isShield="+weapon.isShield()+" isAbleToAttackGround="+weapon.isAbleToAttackGround()+";" ,FieldBOT.MSG_DBG_ALL);
        }
        if (unit.isAbleToKamikaze()) {
            damage=0;
            owner.sendTextMsg(" Unit able to kamikadze. Weapon damage skip to 0. Unit="+unit.getName()+";" ,FieldBOT.MSG_DBG_ALL);
        }
        //---------
        
        kachestva[P_ATACK_POWER]=damage; // за оружие
        kachestva[P_ATACK_DISTANCE]=range; // за расстояние
        
        kachestva[P_RADAR]=unit.getRadarRadius(); // за радиус радара
        if (unit.isStealth()) kachestva[P_RADAR]=1.0f; else kachestva[P_RADAR]=0.0f; // за скрытность
        if (unit.isAbleToAssist()) kachestva[P_CONSTRUCTOR]=unit.getBuildSpeed(); else kachestva[P_CONSTRUCTOR]=0.0; // за строительства
        
        return kachestva;
    }
    
    /**
     * Уменьшает пропорционально значения до максимального 1.0.
     * @param v 
     */
    private void noramilzeVector(float[] v) {
        float max=v[0];
        for (int i=1;i<v.length; i++) if (v[i]>max) max=v[i];
        for (int i=0;i<v.length; i++) v[i] = v[i]/max;
    }

    /**
     * Вычислить оптимальные типы и колличество военных юнитов для постройки
     * @param onBase на какой базе (для определения силы строителей и возможности строить здесь)
     * @param buildVariants список всех вариантов, из которых нужно выбрать
     * @param timeLimit лимит времени на постройку всей армии в секундах
     * @param unitLimit ограничение колличества юнитов
     * @param kachestva какие качества нужны (от 0 - не нужно до 1 - нужно)
     * @return список (кого,сколько) или null!
     */
    public HashMap<UnitDef,Integer> shooseArmy(TBase onBase, ArrayList<UnitDef> buildVariants, float timeLimit, int unitLimit, float kachestva[])
    {
        if (kachestva.length!=NUM_P) throw new ArrayIndexOutOfBoundsException();// !!!
        if (buildVariants.isEmpty()) throw new ArrayIndexOutOfBoundsException();// !!!        
        
        // 1. Выбрать все движущиеся юниты, которые можно строить на поверхности базы
        ArrayList<UnitDef> buildVars=new ArrayList<UnitDef>();
        for (UnitDef def:buildVariants) if (ModSpecification.isRealyAbleToMove(def) && onBase.canBuildOnBase(def, false)) {
            // так же убрать с нулевыми коэффициентами
            double current_k[]=getUnitKachestva(def);
            boolean check=false;
            for (int i=0;i<NUM_P;i++) if ((kachestva[i]!=0)&&(current_k[i]!=0)) {
                check=true;
                break;
            }
            if (check) buildVars.add(def);
        }
        // TODO проверка, умеют ли строить строители
        
        if (buildVars.isEmpty()) return null; // !!! Если не из чего выбирать!!!

        //2. Назначение коэффициентов выгодности
        double min_k[]=new double[NUM_P];
        double max_k[]=new double[NUM_P];
        
        // 2.1 Вычисление максимальных и минимальных значений
        min_k=getUnitKachestva(buildVariants.get(0));
        max_k=getUnitKachestva(buildVariants.get(0));
        for (UnitDef unit: buildVariants) {
            double current_k[]=getUnitKachestva(unit);
            for (int i=0;i<NUM_P;i++) {
                if (current_k[i]>max_k[i]) max_k[i]=current_k[i];
                if (current_k[i]<min_k[i]) min_k[i]=current_k[i];
            }
        }
        for (int i=0;i<NUM_P;i++) if (max_k[i]==0) max_k[i]=1.0f; // !!!!
        
        noramilzeVector(kachestva); // нормализация вектора параметров к: от 0 до 1.
        
// x * 1/max_k[i];// !!!! нормализация значений (далее)
        
        // 2.2 Назначение таблицы коэффициентов и матрицы стоимости
        
        // Определение кто кого может строить и кому помогать...
        //!!!!
        // TODO 2.2 !!
        
        // Определение сколько ресурсов...
        int numRes=owner.avgEco.resName.length;
        int numTimeRes=1;//!!!
        int posTimeRes=numRes;//!!!
        int posCountLimit=posTimeRes+numTimeRes;//!!!
        if (unitLimit==Integer.MAX_VALUE) posCountLimit=-1;
        int numAllRes=numRes+numTimeRes;// !
        if (posCountLimit>=0) numAllRes++;
        
        
        double M_k[]=new double[buildVars.size()];
        double M_res[][]=new double[numAllRes][buildVars.size()]; // было [buildVars.size()][numRes+1];
        
        double K_PRECISSION=1.0e+4; // !!! TODO пока это для лучшего значения!!!! если расчёты не в плавающих а в целых числах.
        if (USE_MULTIPLY) K_PRECISSION*=1e+6;// !!!
        
        for (int i=0; i<buildVars.size(); i++) { //!!
            UnitDef unit = buildVars.get(i);
                    
            double unitK[]=getUnitKachestva(unit);
            double k; // + или *  , 0 или 1  ??? выбрать!
            if (USE_MULTIPLY) k=1; else k=0;
            for (int j=0;j<NUM_P;j++) {
                double x = unitK[j]/max_k[j] * kachestva[j];
                if (USE_MULTIPLY) { // * or +
                    if (!USE_SMART_MULTIPLY) k *= x;
                    else k = (0.1*k + 0.9*k*x); // !!!
                } else k += x;
            }
            M_k[i]=k * K_PRECISSION;// TODO!!!
            
            for (int j=0;j<numRes;j++) M_res[j][i]=unit.getCost(owner.avgEco.resName[j]); //было M_res[i][j]
            M_res[posTimeRes][i]=unit.getBuildTime(); // время постройки как ресурс. // было M_res[i][numRes]
            // TODO 
            if (posCountLimit>=0) M_res[posCountLimit][i]=1.0; // колличество, как ресурс
            
            
            i++;
        }

        float tmpEndTRes[]=owner.avgEco.getAvgCurrentFromTime(timeLimit, true); // !!!
        double V_Res[]=new double[numAllRes]; // Вектор ограничения ресурсов (максимум)
        for (int i=0;i<numRes;i++) V_Res[i]=tmpEndTRes[i];

        V_Res[posTimeRes]=onBase.getBuildPower(false, false) * timeLimit;// TODO OnlyFactory for NO ASSISTABLE (NOTA) !!!
        // TODO разные заводы и их кол-во.
        // особенно хоошо для NOTA
        
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
        if (resK<=0) return null;// Если ни один план не даёт выиграша.
        
        // - - -
        // вынести в класс MathOptimizationMethods
// describe the optimization problem
        /*
        double[] fVector = getZeroVector(V_Res.length);
        fVector[0]=1.0; // вектор с одной единицей. !!!
        LinearObjectiveFunction f = new LinearObjectiveFunction(fVector, 0); // !!!
        
        ArrayList<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
        for(int i=0; i<V_Res.length; i++) { // Ограничения
            double[] _c = getZeroVector(V_Res.length);
            _c[i]=1.0; // вектор с одной единицей.
            constraints.add( new LinearConstraint(_c, Relationship.LEQ, V_Res[i]) ); // _c[i]*x[i]<=V_Res[i]
            
            //_c = getZeroVector(V_Res.length);
            //constraints.add( new LinearConstraint(_c, Relationship.GEQ, V_Res[i]) ); // _c[i]*x[i]<=V_Res[i]
        }
        // TODO при переработке энергии в металл можно учесть с помощью двух переменных и ограничения!!!
        

        SimplexSolver solver = new SimplexSolver();
        PointValuePair optSolution = solver.optimize(new MaxIter(100), f,
                new LinearConstraintSet(constraints),
                GoalType.MAXIMIZE,
                new NonNegativeConstraint(true));


        double[] solution;
        solution = optSolution.getPoint();      
        //*/
        // - - -
        
        
        //...
        
        // 4 Обработка результата
        HashMap<UnitDef,Integer> outArmy=new HashMap<UnitDef,Integer>();
        for (int i=0; i<buildVars.size(); i++) { //!!
            if (simplOtvet[i]!=0) {
                UnitDef unit = buildVars.get(i);
                int n=(int)Math.round(simplOtvet[i]); // !!!!!
                outArmy.put(unit, n);
            }
        }

        return outArmy;
    }

    // hash map http://habrahabr.ru/post/128017/
    
}
