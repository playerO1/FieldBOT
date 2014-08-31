/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package fieldbot.AIUtil;

import com.springrts.ai.oo.clb.Unit;
import java.util.ArrayList;

/**
 * TODO Use IT!
 * @author user2
 */
public class TBuildInfo {
    public Unit unit;
    public ArrayList<Unit> builders;
    public boolean isAssistable;
    public float recomendedBuildPower,haveBuildPower;
    
// TODO !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! использовать это
    
    public TBuildInfo(Unit unit,Unit builder) {
        this.unit=unit;
        builders=new ArrayList<Unit>();
        if (builder!=null) {
            builders.add(builder);
            isAssistable=builder.getDef().isAssistable();
            haveBuildPower=builder.getDef().getBuildSpeed();
        } else {
            isAssistable=true; // !!!!
            haveBuildPower=0.0f;
        }
    }
    
    public void addBuilder(Unit builder) {
        // TODO проверка повторений и может ли этот помочь.
        if (builders.add(builder))
            haveBuildPower+=builder.getDef().getBuildSpeed();
    }
    public void removeBuilder(Unit builder) {
        // TODO проверка повторений и может ли этот помочь.
        if (builders.remove(builder))
            haveBuildPower-=builder.getDef().getBuildSpeed();
    }
    
    public boolean containsBuilder(Unit builder) {
        return builders.contains(builder);
    }
}
