/*
    playerO1, optimization calls function
*/

package fieldbot.AIUtil;

import java.util.*;
import org.apache.commons.math3.optim.MaxIter;
import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.linear.LinearConstraint;
import org.apache.commons.math3.optim.linear.LinearConstraintSet;
import org.apache.commons.math3.optim.linear.LinearObjectiveFunction;
import org.apache.commons.math3.optim.linear.NonNegativeConstraint;
import org.apache.commons.math3.optim.linear.Relationship;
import org.apache.commons.math3.optim.linear.SimplexSolver;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;

/**
 * Some of this function may be using APACHE MATH3 library.
 * @author PlayerO1
 */
public class MathOptimizationMethods {
    
    // A - resources for make units
    // b - resources limit
    // c - profit
    // x - count of units (out)

    /**
     * 
     * @param A resources for make units [resource][unit]
     * @param b resources limit (depends)
     * @param c profit
     * @param x count of units (out)
     * @return profit for x
     */
public static double simplexDouble(double[][] A, double[] b, double[] c, double[] x) {
      //TODO low limit: minimal atack,HP,speed...
//    return justOneMaximize(A,b,c, x); // maybe fast, but not precission
    
    // - - -
    LinearObjectiveFunction f = new LinearObjectiveFunction(c, 0); // !!!

    ArrayList<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
    for(int i=0; i<A.length; i++) { // Ограничения
        constraints.add( new LinearConstraint(A[i], Relationship.LEQ, b[i]) ); // _c[i]*x[i]<=V_Res[i]
        double[] zeroV = new double[A[i].length];
        Arrays.fill(zeroV, 0);
        constraints.add( new LinearConstraint(zeroV, Relationship.GEQ, 0) ); // _c[i]*x[i]<=V_Res[i]
    }
    // TODO при переработке энергии в металл можно учесть с помощью двух переменных и ограничения!!!

    // FIXME precission - do not use between 0.00001 and 1.0, only 1,2,3...
    SimplexSolver solver = new SimplexSolver();
    PointValuePair optSolution = solver.optimize(new MaxIter(1000), f,
            new LinearConstraintSet(constraints),
            GoalType.MAXIMIZE,
            new NonNegativeConstraint(true));

    //double[] solution = optSolution.getPoint();
    double[] x_=optSolution.getPointRef(); // TODO it is bad fix for n*1.0. To do iteration.
    for (int i=0;i<x_.length;i++) if (x_[i]>0) x_[i]=Math.round(x_[i]-0.5);

    System.arraycopy(x_, 0, x, 0, x.length);
    return optSolution.getValue();
    // - - -    
}

/**
 * Maybe fast that simplex, but non precission, can not choose more that 1 variant.
 * @param A resource to build unit [номер юнита][номер ресурса] ПЕРЕПУТАНЫ! TODO check m,n.
 * @param b resource limit (depends)
 * @param c profit from unit
 * @param x out, plan for product (unit count)
 * @return the profit from x plane
 */
public static double justOneMaximize(double[][] A, double[] b, double[] c, double[] x) {
    int bestPlanI=-1; // best unit ID on list
    double maxF=0.0f, // max profit
           bestX=0;   // max count of unit
    for (int i=0;i<A[0].length;i++) {
        double minN,N; // count of max production of this unit
        minN=Double.POSITIVE_INFINITY;
        for (int j=0;j<A.length;j++) if (A[j][i]!=0.0) {
            N=b[j]/A[j][i];
            minN=Math.min(minN, N);
        }
        if (minN>0) minN=Math.round(minN-0.5); // round unit count. -0.5 for limit of no split
        double profit=minN*c[i];
        if (profit>maxF) {
            maxF=profit;
            bestPlanI=i;
            bestX=minN;
        }
    }
    Arrays.fill(x, 0.0f);
    if (bestPlanI!=-1) x[bestPlanI]=bestX;
    return maxF;
}

}
