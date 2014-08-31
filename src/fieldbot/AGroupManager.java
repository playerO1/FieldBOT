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

import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.Unit;
import com.springrts.ai.oo.clb.UnitDef;
import com.springrts.ai.oo.clb.WeaponDef;
import java.util.ArrayList;
import java.util.HashSet;

/**
 *
 * @author user2
 */
public abstract class AGroupManager {

    
    
    
   /**
    * Тип базы
    * 0 основная, эконоимка
    * 1 боевая защитная или разведывательная(укреп район)
    */
   public int baseType=0; // TODO...
    
    /**
     * Добавить юнита в базу
     * @param unit
     * @return принят или нет (может уже есть)
     */
    public abstract boolean addUnit(Unit unit);
    
    /**
     * Убрать юнита из базы
     * @param unit
     * @return убран или нет (например не был приписан)
     */
    public abstract boolean removeUnit(Unit unit);

    /**
     * Содержит ли юнита в списках?
     * @param unit
     * @return 
     */
    public abstract boolean contains(Unit unit);
    
    /**
     * На сколько нужен этот юнит данной группе
     * @return 
     */
    public abstract float doYouNeedUnit(Unit unit);
    
    /**
     * Возвращает всех юнитов приписанных к базе
     * @param selectWhoCanNow - true = выбирает только тех, кто может сейчас строить (достроенных, и не paralyzed)
     * @return 
     */
    public abstract ArrayList<Unit> getAllUnits(boolean selectWhoCanNow);
   
    /**
     * Возвращает перечень всех типов юнитов на базе
     * @param onlyWhatIdle только тех, кто не занят и не строится.
     * @return 
     */
//    public abstract HashSet<UnitDef> getContainUnitDefsList(boolean onlyWhatIdle);
    

    
    
    // Команды, переадрисуемые ботом.
    
    //@Override
    public int update(int frame) {
        return 0;
    }
    
   //@Override
    public int unitCreated(Unit unit, Unit builder) {
        return 0;
    }
    
    //@Override
    public int unitFinished(Unit unit) {
        return 0;
    }
    
    //@Override
    public int unitIdle(Unit unit) {
        return 0; // signaling: OK
    }
    
    //@Override
    public int unitMoveFailed(Unit unit) {
        return 0; // signaling: OK
    }

    //@Override
    public int unitDamaged(Unit unit, Unit attacker, float damage, AIFloat3 dir, WeaponDef weaponDef, boolean paralyzed) {
        return 0; // signaling: OK
    }

    //@Override
    public boolean unitDestroyed(Unit unit, Unit attacker) {
        return false;
    }
    
    //@Override
    public boolean unitGiven(Unit unit, int oldTeamId, int newTeamId) {
        return false;
    }

    //@Override
    public int unitCaptured(Unit unit, int oldTeamId, int newTeamId) {
        return 0; // signaling: OK
    }

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

    //@Override
    public int commandFinished(Unit unit, int commandId, int commandTopicId) {
        return 0; // signaling: OK
    }

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

}
