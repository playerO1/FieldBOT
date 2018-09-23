/*
 * Copyright (C) 2018 PlayerO1
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

import com.springrts.ai.oo.clb.Resource;
import com.springrts.ai.oo.clb.UnitDef;
import fieldbot.AIUtil.TimeCache;
import java.util.HashMap;
import java.util.List;

/**
 * Cache between Java and JNI Spring API. 
 * Usefull eco functin. See also ModSpecification.
 * @author PlayerO1
 */
public class EnvironmentInterface {
    private ModSpecification modSpecific;
    private AdvECO avgEco;
    private FieldBOT fieldBot;

    private final static class UnitResKey {
        final int unitDefId, resurceId;

        public UnitResKey(int UnitDefId, int resurceId) {
            this.unitDefId = UnitDefId;
            this.resurceId = resurceId;
        }
        public UnitResKey(UnitDef def, Resource res) {
            this(def.getUnitDefId(), res.getResourceId());
        }
        @Override
        public int hashCode() {
            int hash = 7;
            hash = 37 * hash + this.unitDefId;
            hash = 37 * hash + this.resurceId;
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            final UnitResKey other = (UnitResKey) obj;
            if (this.unitDefId != other.unitDefId) return false;
            if (this.resurceId != other.resurceId) return false;
            return true;
        }
    }
    
    private static class ResourceItem {
        int unitDef;
        float value; //def.getMakesResource(res)+def.getResourceMake(res)-def.getUpkeep(res);
        float windValue; // def.getWindResourceGenerator(res); // windValue/120.0
        float tidalValue; // def.getTidalResourceGenerator(res);
        float extractorValue;//def.getExtractsResource(res);
        
        public float calculate(Resource res, EnvironmentInterface ei) {
            //TODO make faster!
            float resProduct = value;
            float windMap;
            if (windValue!=0 && (windMap=ei.getMapWindValue())!=0) {
                resProduct += windValue/120.0f*windMap; // test, maybe mod specifed
    //            sendTextMsg("Wind calc for "+def.getName()+" map wind="+mapWind+" def wind res gen="+defWindGenerator+ " resProduct+="+resProduct, MSG_DBG_ALL);
            }
            float mapTidal;
            if (tidalValue!=0 && (mapTidal=ei.getMapTidalValue())!=0) {
                resProduct += tidalValue*mapTidal;
    //            sendTextMsg("Tidal calc for "+def.getName()+" map tid shreng="+mapTidal+" def tid res gen="+defTidalGenerator+ " resProduct+="+resProduct, MSG_DBG_ALL);
            }

            if (ei.fieldBot.isMetalFieldMap>=0) {
                if (extractorValue!=0) {  // add extract resource
                    resProduct+=extractorValue * ei.getExtractingFromMap(res);
        //            sendTextMsg("Extract calc for "+def.getName()+" extr square="+metalExSquare+" map avg res income="+mapResAvgExtract+" def extract res gen="+defExtractRes+ " resProduct+="+resProduct, MSG_DBG_ALL);
                }
            }

            return resProduct;
        }
    }
    
    //
    HashMap<UnitResKey, ResourceItem> resourceProductCache;
    HashMap<Integer,float[]> unitCostCache=new HashMap<>();
    final List<Resource> resourceTypeList;

    public EnvironmentInterface(ModSpecification modSpecific, AdvECO avgEco, FieldBOT fieldBot) {
        this.modSpecific = modSpecific;
        this.avgEco = avgEco;
        this.fieldBot = fieldBot;
        this.resourceProductCache=new HashMap<>();
        resourceTypeList=fieldBot.clb.getResources();
    }
    
    
    public float getUnitResoureProduct(UnitDef def, Resource res) {
        UnitResKey cacheKey = new UnitResKey(def, res);
        ResourceItem resItem;
        synchronized (resourceProductCache) {
            resItem=resourceProductCache.get(cacheKey);
            if (resItem==null) {
                resItem=new ResourceItem();
                //-----
                float resProduct = 0.0f + def.getMakesResource(res)+def.getResourceMake(res)-def.getUpkeep(res);
                // - - Depends of MOD part! MMaker - -
                float modSpecRes[]=modSpecific.getResourceConversionFor(def);
                if (modSpecRes!=null) {
                   if (res.equals(avgEco.resName[0])) resProduct += modSpecRes[0];
                   if (res.equals(avgEco.resName[1])) resProduct +=modSpecRes[1];
                }
                // TODO In mod EvolutionRTS all metal extractor make 1 (or 0.5) metall! In Zero-K extract metal on special formuls!
                resItem.value = resProduct;

                resItem.windValue=def.getWindResourceGenerator(res)/120.0f;
                resItem.tidalValue=def.getTidalResourceGenerator(res);
                if (fieldBot.isMetalFieldMap>=0)  resItem.extractorValue=def.getExtractsResource(res);
                else resItem.extractorValue=0;
                //-----
                resourceProductCache.put(cacheKey, resItem);
            }
        }
        float resProduct=resItem.calculate(res, this);
        return resProduct;
    }

    public void invalidateCache() {
        synchronized(resourceProductCache) {
            resourceProductCache.clear();
        }
    }
    
    private TimeCache<Float> cache_mapWindValue=new TimeCache(2000) {
        @Override
        protected Float newValue() {
            return fieldBot.clb.getMap().getCurWind();
        }
    };
    protected float getMapWindValue() {
        return cache_mapWindValue.get();
    }
    private TimeCache<Float> cache_mapTidalValue=new TimeCache(5000) {
        @Override
        protected Float newValue() {
            return fieldBot.clb.getMap().getTidalStrength();
        }
    };
    protected float getMapTidalValue() {
        return cache_mapTidalValue.get();
    }
    private HashMap<Integer, TimeCache<Float>> cache_ExtractingFromMap=new HashMap<>();
    protected float getExtractingFromMap(final Resource res) {
        TimeCache<Float> cache=cache_ExtractingFromMap.get(res.getResourceId());
        if (cache==null) {
            cache=new TimeCache<Float>(5000) {
                @Override
                protected Float newValue() {
                    float mapResAvgExtract=fieldBot.clb.getMap().getExtractorRadius(res);
                    float metalExSquare = mapResAvgExtract; // todo check code.
                    metalExSquare = metalExSquare * metalExSquare * (float)Math.PI;
                    return metalExSquare * fieldBot.clb.getMap().getResourceMapSpotsAverageIncome(res);
                }
            };
            cache_ExtractingFromMap.put(res.getResourceId(), cache);
        }
        return cache.get();
    }
        

    /**
     * Get unit resource income-usage (can work with LUA metal maker by using modSpecific.getResourceConversionFor(...)).
     * @param def unit type
     * @param res resource type
     * @return resource incoming (+) or resource using (-)
     */
    @Deprecated
    public float getUnitResoureProduct_fromNative(UnitDef def, Resource res) {
        //TODO make faster!
        float resProduct = 0.0f + def.getMakesResource(res)+def.getResourceMake(res)-def.getUpkeep(res);
        
        float mapWind=fieldBot.clb.getMap().getCurWind();
        float defWindGenerator=def.getWindResourceGenerator(res);
        if (mapWind>0 && defWindGenerator!=0) {
            resProduct += defWindGenerator/120.0f*mapWind; // !!! test, maybe mod specifed!!
//            sendTextMsg("Wind calc for "+def.getName()+" map wind="+mapWind+" def wind res gen="+defWindGenerator+ " resProduct+="+resProduct, MSG_DBG_ALL);
        }
        
        float mapTidal=fieldBot.clb.getMap().getTidalStrength();
        float defTidalGenerator=def.getTidalResourceGenerator(res);
        if (mapTidal>0 && defTidalGenerator!=0) {
            resProduct += defTidalGenerator*mapTidal; // TODO TEST tidal res !!!!
//            sendTextMsg("Tidal calc for "+def.getName()+" map tid shreng="+mapTidal+" def tid res gen="+defTidalGenerator+ " resProduct+="+resProduct, MSG_DBG_ALL);
        }
        
        if (fieldBot.isMetalFieldMap>=0) {
            float defExtractRes=def.getExtractsResource(res);
            if (defExtractRes!=0) {  // add extract resource
                float mapResAvgExtract=fieldBot.clb.getMap().getExtractorRadius(res);
                double metalExSquare = mapResAvgExtract;
                    metalExSquare = metalExSquare * metalExSquare * Math.PI;
                resProduct+=defExtractRes * metalExSquare * fieldBot.clb.getMap().getResourceMapSpotsAverageIncome(res);
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
        //todo use cache too
        final float mapWind=fieldBot.clb.getMap().getCurWind();
        final float mapTidal=fieldBot.clb.getMap().getTidalStrength();
        
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
        
            if (fieldBot.isMetalFieldMap>=0) {
                float defExtractRes=def.getExtractsResource(res);
                if (defExtractRes!=0) {  // add extract resource
                    float mapResAvgExtract=fieldBot.clb.getMap().getExtractorRadius(res);
                    double metalExSquare = mapResAvgExtract;
                        metalExSquare = metalExSquare * metalExSquare * Math.PI;
                    resProduct[r]+=defExtractRes * metalExSquare * fieldBot.clb.getMap().getResourceMapSpotsAverageIncome(res);
        //            sendTextMsg("Extract calc for "+def.getName()+" extr square="+metalExSquare+" map avg res income="+mapResAvgExtract+" def extract res gen="+defExtractRes+ " resProduct+="+resProduct, MSG_DBG_ALL);
                }
            }
            
            if (modSpecRes!=null) resProduct[r] += modSpecRes[r]; // Depends of MOD part! MMaker
            // TODO In mod EvolutionRTS all metal extractor make 1 (or 0.5) metall! In Zero-K extract metal on special formuls!
        }
        
        return resProduct;
    }    
    
    
    public float getCost(UnitDef def, Resource res) {
        float[] coast=unitCostCache.get(def.getUnitDefId());
        if (coast==null) {
            coast=new float[resourceTypeList.size()];
            for (Resource r:resourceTypeList) coast[r.getResourceId()]=def.getCost(r);
            unitCostCache.put(def.getUnitDefId(), coast);
        }
        return coast[res.getResourceId()]; //def.getCost(res);
    }
}
