/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package fieldbot;

import com.springrts.ai.oo.clb.*;
import java.util.List;

/**
 * Улучшенная информация об экономике. Поддерживает усреднённые значения.
 * @author PlayerO1
 */
public class AdvECO {
    /**
     * Названия ресурсов (заголовки)
     */
    public final Resource resName[];
    
    public static final int R_Current=0;
    public static final int R_Income=1;
    public static final int R_Storage=2;
    public static final int R_Usage=3;
    
    /**
     * Current resources
     */
    private float resCurrent[][]; // TODO...
    
    /**
     * средние текущие ресурсы (кэш)
     */
    private float resAvg[][];
    /**
     * средние для постройки доступные ресурсы (smart) (кэш)
     */
    private float resSAvg[][];
    
    /**
     * Число усреднённых значений.
     */
    public final int avgTime;
    
    /**
     * сырая статистика для обработки
     */
    private float resStatisticRav[][][];
    private int dataI;
    
    private final OOAICallback clb;
    private final ModSpecification modMakerSpecific;
    
    private final boolean isZeroK_mod; // Mod specification!
    private final float zeroK_EStorX=10000.0f;// decrease E storage limit
    
    /**
     * 
     * @param numStatisticTime число измерений для усреднения
     */
    public AdvECO(int numStatisticTime, OOAICallback clb, ModSpecification modMakerSpecific) {
        avgTime = numStatisticTime;
        this.clb= clb;
        final Economy currentEco=clb.getEconomy();
        this.modMakerSpecific=modMakerSpecific;
        this.isZeroK_mod=modMakerSpecific.modName.equals("ZK");
        
        List<Resource> resLst=clb.getResources();
        resName=new Resource[resLst.size()];
        for (int i=0;i<resLst.size();i++) resName[i]=resLst.get(i);
        //resName=clb.getResources().toArray(resName);
        
        float[][] resCurrent=new float[4][resName.length];
            
        resStatisticRav=new float[avgTime][4][resName.length];
        for (int i=0; i<resName.length; i++) {
            resCurrent[R_Current][i]=currentEco.getCurrent(resName[i]);
            resCurrent[R_Income][i]=currentEco.getIncome(resName[i]);
            resCurrent[R_Storage][i]=currentEco.getStorage(resName[i]);
            resCurrent[R_Usage][i]=currentEco.getUsage(resName[i]);
            if (isZeroK_mod && i==1) { // For ZeroK E storage.
                resCurrent[R_Storage][i]-=zeroK_EStorX;
                resCurrent[R_Current][i]=Math.min(resCurrent[R_Current][i], resCurrent[R_Storage][i]);
            } 
            for (int s=0;s<resStatisticRav.length;s++) for (int t=0;t<4;t++) resStatisticRav[s][t][i]=resCurrent[t][i];
        }
        dataI=0;

        resAvg=null;
        resSAvg=null;
    }
    
    /**
     * Обновить сведения о экономики.
     * @param frame 
     */
    public void update(int frame) {
        resAvg=null;
        resSAvg=null;
        
        //int f = frame % resStatisticRav.length;
        dataI = (dataI+1)%resStatisticRav.length;
        Economy currentEco=clb.getEconomy();
        for (int i=0; i<resName.length; i++) {
            resStatisticRav[dataI][R_Current][i]=currentEco.getCurrent(resName[i]);
            resStatisticRav[dataI][R_Income][i]=currentEco.getIncome(resName[i]);
            resStatisticRav[dataI][R_Storage][i]=currentEco.getStorage(resName[i]);
            resStatisticRav[dataI][R_Usage][i]=currentEco.getUsage(resName[i]);
            
            // TODO проверить работу при заполненных запасах... нужно или нет: ?
            /*
            if (resStatisticRav[dataI][R_Current][i]==resStatisticRav[dataI][R_Storage][i]
                    &&
                resStatisticRav[dataI][R_Income][i]==0 && resStatisticRav[dataI][R_Usage][i]!=0
                ) {
                resStatisticRav[dataI][R_Income][i]=resStatisticRav[dataI][R_Usage][i]*1.2f;
                // "Костыль" от того что поступление ресурсов = 0 при заполнении хранилищь
            }
            */
        }
        if (isZeroK_mod) { // For ZeroK E storage.
            resStatisticRav[dataI][R_Storage][1]-=10000.0f;
            resStatisticRav[dataI][R_Current][1]=Math.min(resStatisticRav[dataI][R_Current][1], resStatisticRav[dataI][R_Storage][1]);
        }
    }
    
    public float getAvgCurrent(Resource resID) {
        for (int rI=0;rI<resName.length;rI++) if (resName[rI].equals(resID)) {
            float sum=0.0f;
            for ( int t=0; t<avgTime; t++ ) sum+=resStatisticRav[t][R_Current][rI];
            return sum / (float)avgTime;
        }
        return 0.0f; // !!!!!!!!
    }
    public float getAvgIncome(Resource resID) {
        for (int rI=0;rI<resName.length;rI++) if (resName[rI].equals(resID)) {
            float sum=0.0f;
            for ( int t=0; t<avgTime; t++ ) sum+=resStatisticRav[t][R_Income][rI];
            return sum / (float)avgTime;
        }
        return 0.0f; // !!!!!!!!
    }
    public float getAvgStorage(Resource resID) {
        for (int rI=0;rI<resName.length;rI++) if (resName[rI].equals(resID)) {
            float sum=0.0f;
            for ( int t=0; t<avgTime; t++ ) sum+=resStatisticRav[t][R_Storage][rI];
            return sum / (float)avgTime;
        }
        return 0.0f; // !!!!!!!!
    }
    public float getAvgUsage(Resource resID) {
        for (int rI=0;rI<resName.length;rI++) if (resName[rI].equals(resID)) {
            float sum=0.0f;
            for ( int t=0; t<avgTime; t++ ) sum+=resStatisticRav[t][R_Usage][rI];
            return sum / (float)avgTime;
        }
        return 0.0f; // !!!!!!!!
    }
    
    // ----- Вспомогательные, преобразуют в массив текущие ресурсы -----
    public float[] getCurrentToArr() {
        Economy eco=clb.getEconomy();
        float r[]=new float[resName.length];
        for (int i=0;i<resName.length;i++) r[i]=eco.getCurrent(resName[i]);
        if (isZeroK_mod) { // For ZeroK E storage.
            r[1]=Math.max(r[1], eco.getStorage(resName[1])-zeroK_EStorX);
        }
        return r;
    }
    public float[] getIncomeToArr() {
        Economy eco=clb.getEconomy();
        float r[]=new float[resName.length];
        for (int i=0;i<resName.length;i++) r[i]=eco.getIncome(resName[i]);
        return r;
    }
    public float[] getUsageToArr() {
        Economy eco=clb.getEconomy();
        float r[]=new float[resName.length];
        for (int i=0;i<resName.length;i++) r[i]=eco.getUsage(resName[i]);
        return r;
    }
    
    /**
     * Преобразует все сведения о текущих ресурсах в массив. НЕ усреднённые!
     * @return массив как [R_Current|R_Income|R_Storage|R_Usage][i], где i - номер ресурса (см. resName)
     */
    public float[][] getResourceToArr() {
        Economy currentEco=clb.getEconomy();
        float[][] resCur=new float[4][resName.length];
        for (int i=0; i<resName.length; i++) {
            resCur[R_Current][i]=currentEco.getCurrent(resName[i]);
            resCur[R_Income][i]=currentEco.getIncome(resName[i]);
            resCur[R_Storage][i]=currentEco.getStorage(resName[i]);
            resCur[R_Usage][i]=currentEco.getUsage(resName[i]);
        }
        if (isZeroK_mod) { // For ZeroK E storage.
            resCur[R_Storage][1]-=10000.0f;
            resCur[R_Current][1]=Math.min(resCur[R_Current][1],resCur[R_Storage][1]);
        }
        return resCur;
    }
    
    
    /**
     * Преобразует все УСРЕДНЁННЫЕ сведения о текущих ресурсах в массив.
     * Результат кэшируется и выдаётся ссылка на кэш!
     * @return массив как [R_Current|R_Income|R_Storage|R_Usage][i], где i - номер ресурса (см. resName)
     */
    public float[][] getAVGResourceToArr() {
        // -- кэш --
        if (resAvg!=null) return resAvg;
        // -- --
        
        float[][] resAVGCurrent=new float[4][resName.length];
        for (int i=0; i<resName.length; i++) {
            resAVGCurrent[R_Current][i]=getAvgCurrent(resName[i]);
            resAVGCurrent[R_Income][i]=getAvgIncome(resName[i]);
            resAVGCurrent[R_Storage][i]=getAvgStorage(resName[i]);
            resAVGCurrent[R_Usage][i]=getAvgUsage(resName[i]);
        }
        // -- кэш --
        resAvg=resAVGCurrent;
        // -- --
        return resAVGCurrent;
    }
    // ------
    /**
     * Выдаёт максимальную отдачу ресурсов (для metal makers). Преобразует все УСРЕДНЁННЫЕ сведения о текущих ресурсах в массив.
     * Результат кэшируется
     * @return массив как [R_Current|R_Income|R_Storage|R_Usage][i], где i - номер ресурса (см. resName)
     */
    public float[][] getSmartAVGResourceToArr() {
        // -- кэш --
        if (resSAvg!=null) return resSAvg;
        // -- --
        
        float[][] resAVGCurrent=new float[4][resName.length];
        for (int i=0; i<resName.length; i++) {
            resAVGCurrent[R_Current][i]=getAvgCurrent(resName[i]);
            resAVGCurrent[R_Income][i]=getAvgIncome(resName[i]);
            resAVGCurrent[R_Storage][i]=getAvgStorage(resName[i]);
            resAVGCurrent[R_Usage][i]=getAvgUsage(resName[i]);
        }
// TODO + учесть расходы на строительство...
        // Учесть погрешности Metal makers
        final int maker_To=modMakerSpecific.maker_resource_to;//!!! make metal
        final int maker_From=modMakerSpecific.maker_resource_from;//!!! from energy
        final float maker_Zone=modMakerSpecific.metal_maker_workZone; // where energy >=68%
        if (maker_To>=0)// Exist metal makers
        {
            if (resAVGCurrent[R_Current][maker_From]/resAVGCurrent[R_Storage][maker_From]>=maker_Zone)
            {
                resAVGCurrent[R_Income][maker_To] /= 2; // Меньше получим
                resAVGCurrent[R_Usage][maker_To] *=0.01;// /= (2*2); // TODO !!! ???
                resAVGCurrent[R_Usage][maker_From] /= 2; // Сэкономив на затратах
                //!!!!
            } else {
                for (int i=0; i<resName.length; i++) resAVGCurrent[R_Usage][i] *=0.01;
            }
            
        }
        // -- кэш --
        resSAvg=resAVGCurrent;
        // -- --
        return resAVGCurrent;
    }
    
    /**
     * Возвращает сколько ресурсов накопится через определённое время. Без учёта пределов хранилищя и отрицательных чисел.
     * @param time время в секундах.
     * @param smartCalc более умный расчёт - при использовании metal maker половина энергии сохраняется
     * @return 
     */
    public float[] getAvgCurrentFromTime(float time, boolean smartCalc) {
        float k=1.0f;
        float[] currRes=getCurrentToArr(); // последнее измерение
        float[][] avgResInfo;//=getAVGResourceToArr();

        if (smartCalc) {
            avgResInfo=getSmartAVGResourceToArr();
            // Ограничение от отрицательных ресурсов из-за переработки энергии
            float min=0,max=0;
            for (int i=0; i<currRes.length; i++) {
                //currRes[i]/2 +
                float x = time*(avgResInfo[R_Income][i]-avgResInfo[R_Usage][i]);
                if (x<min && avgResInfo[R_Usage][i]>0) {
                    min=x; // TODO !!!
                    float kk = 1.0f-avgResInfo[R_Usage][i]/(avgResInfo[R_Income][i] + avgResInfo[R_Usage][i]);
                    k=Math.min(k, kk);
                    // FIXME TEST!!!
                }
            }
            for (int i=0; i<currRes.length; i++) currRes[i] += k*time*(avgResInfo[R_Income][i]-avgResInfo[R_Usage][i]);
        } else {
            avgResInfo=getAVGResourceToArr();
            for (int i=0; i<currRes.length; i++) currRes[i] += time*(avgResInfo[R_Income][i]-avgResInfo[R_Usage][i]);
        }
        // Bug: на картах без металла вся энергия в металл...
        
        return currRes;
    }
    
    /**
     * This can using for compare resources.
     * @param res resource have
     * @return coast
     */
    public float getResourceCoast(float[] res) {
        float coast=0.0f;
        float[] resK=new float[resName.length];
        // TODO using energy conversion K - metal maker.
        float[][] currRes=getAVGResourceToArr();
        for (int i=0; i<resName.length; i++) {
            resK[i] = 1.0f / (2.0f+currRes[R_Income][i]) // !
                    + (1.0f-currRes[R_Current][i]/currRes[R_Storage][i]);
            coast+=res[i] * resK[i]; // !!!
        }
        //TODO use TWarStrategy - have funstion normalizeVector.
        return coast;
    }
    public float getBuildPowerCoast(float buildPower) {
        return buildPower/4500; // !!! TODO BuildPowerCoast
    }
    
    /**
     * This can using for compare resources info.
     * @param res resource have, income, usage
     * @return coast
     */
    public float getResourceCoast(float[][] res) {
        float coast=0.0f;
        float[] resK=new float[resName.length];
        // TODO using energy conversion K - metal maker.
        float[][] currIncome=getAVGResourceToArr();
        for (int i=0; i<resName.length; i++) {
            resK[i] = 1.0f + 1.0f / (2.0f + currIncome[R_Income][i]) // !
                    + (1.0f-currIncome[R_Current][i]/currIncome[R_Storage][i]);
            coast+=(res[R_Current][i] + Math.max(res[R_Income][i]-res[R_Usage][i], 0.0) * 30) * resK[i]; // !!!
        }
        //TODO use TWarStrategy - have funstion normalizeVector.
        return coast;
    }
    
    /**
     * Convert resource object to index.
     * @param res
     * @return resource index on resName[] or -1.
     */
    public int getResourceID(Resource res) {
        for (int i=0; i<resName.length; i++) if (resName[i].equals(res)) return i;
        return -1;
    }
    
    /**
     * Копирует матрицу. Создаёт новый объект.
     * @param source исходная матрица
     * @return новая независимая копия матрицы.
     */
    public static float[][] cloneFloatMatrix(float[][] source) {
        float[][] newM = new float[source.length][];
        for (int i=0;i<source.length;i++) {
            newM[i]=new float[source[i].length];
            System.arraycopy(source[i], 0, newM[i], 0, source[i].length); //было for (int j=0;j<source[i].length;j++) newM[i][j]=source[i][j];
        }
        return newM;
    }
    
    public String toString() {
        String s=" AvgECO tn="+avgTime+":";
        float [][] r=getAVGResourceToArr();
        for (int i=0;i<resName.length;i++) {
            s+=" "+resName[i].getName()+" (";
            s+=" Current="+r[R_Current][i]+",";
            s+=" Income="+r[R_Income][i]+",";
            s+=" Storage="+r[R_Storage][i]+",";
            s+=" Usage="+r[R_Usage][i]+",";
            s+=")";
        }
        return s;
    }
    
}
