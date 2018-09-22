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

package fieldbot.baseplaning;

import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.Map;
import com.springrts.ai.oo.clb.UnitDef;
import fieldbot.FieldBOT;

/**
 *
 * @author user2
 */
public class TPlanCell {
    public static FieldBOT clb;// !!! for DEBUG!!!
    
    public float cx,cy; // center
    public final float width,height;// ширина высота
    
    public final UnitDef forUnitType;// contain unit type
    
    public final int maxCount;// число вмещаемых элементов.
    public int zanato;// число вмещаемых элементов
    public final int countX,countY;// размер матрицы

    private final float padding; // Устанавливает значение полей вокруг содержимого элемента. Полем называется расстояние от внутреннего края рамки элемента до воображаемого прямоугольника, ограничивающего его содержимое
    private final float spacing; // Задает расстояние между границами ячеек в таблице
    private final float elementSX,elementSY;// размер зоны элемента с учётом spacing.
    
    /**
     * 
     * @param forUnitType for this unit type /  для кого
     * @param count how many unit in group / сколько в группе мест
     * @param padding between border / отступы от границ
     * @param spacing between element / отступы между элементами
     */
    public TPlanCell(UnitDef forUnitType, int count, float padding, float spacing) {
        // TODO if count<=0 ?
        if (count<=0) throw new IllegalArgumentException();
        
        switch (count) {
            case 1:
                countX=1; countY=1;
                break;
            case 2:
                countX=2; countY=1;
                break;
            case 3:
                countX=3; countY=1;
                break;
            default:
                int tmp_countX=count/ (int)Math.round(Math.sqrt(count)); // было 3
                  if (tmp_countX==0) tmp_countX=1;
                int tmp_countY=count/tmp_countX; if (tmp_countY==0) tmp_countY=1;
                if (tmp_countX*tmp_countY<count) tmp_countX++;

                countX=tmp_countX;
                countY=tmp_countY;
        }
        maxCount=countX*countY;

        zanato=0;
        this.padding=padding;
        this.spacing=spacing;
        this.forUnitType=forUnitType;
        // !!! ??? 16 ? 16/2=8 ? size?
        if (forUnitType!=null) {
            elementSX=forUnitType.getXSize()*16/2+spacing/2;
            elementSY=forUnitType.getZSize()*16/2+spacing/2;
        } else { // TODO this function on this class, or make IllegalAgrumentException and create other class TPlanCell_reservedPoints?
            elementSX=1*16/2+spacing/2; // !!!!!!!
            elementSY=1*16/2+spacing/2;
        }
        width=elementSX*countX+padding; // TODO -spacing вокруг, или с ним оставить? 
        height=elementSY*countY+padding;
        
        cx=cy=0;// TODO -width -height !!!
        
        if (forUnitType!=null)
            clb.sendTextMsg("Debug: cell create: "+countX+"x"+countY+" for "+forUnitType.getName()+" width="+width+" height="+height+", elementSX="+elementSX+" elementSY="+elementSY, FieldBOT.MSG_DBG_ALL);//DEBUG!!!
        else
            clb.sendTextMsg("Debug: cell create: "+countX+"x"+countY+" for <null> width="+width+" height="+height+", elementSX="+elementSX+" elementSY="+elementSY, FieldBOT.MSG_DBG_ALL);//DEBUG!!!
    }
    
    /**
     * Return elelent position by element number
     * Выдаёт позицию элемента по номеру позиции
     * @param n number of position. Start from 0.
     * @return позиция с учётом центра группы
     */
    public AIFloat3 getPos(int n) {
        int i,j;
        i=n/countX;
        j=n%countX;
        // TODO test!!! check.
        float x=elementSX*i - width/2+padding/2;
        float y=elementSY*j - height/2-padding/2;
                                //!!! TEST
        return new AIFloat3(x+cx, 0, y+cy);
    }
    
    /**
     * Обновляет (пересчитывает) число занятых клеток (где гельзя строить)
     * @param map для функции проверки
     * @param buildFacing направление вращения построек...
     */
    public void updateZanatoPoz(Map map,int buildFacing) {
        if (forUnitType==null) {
            zanato=1;
            return; // !!!!
        }
        int z=0;
        for ( int i=0; i<maxCount ; i++ ) {
            AIFloat3 p=getPos(i);
            if (p!=null) {if (!map.isPossibleToBuildAt(forUnitType, p, buildFacing)) z++;}
            else clb.sendTextMsg("ERROR updateZanatoPoz: point is NULL!!!", FieldBOT.MSG_ERR);// LOG ERROR!!!!!!!!!
        }
        zanato=z;
    }
    
    public int getNumFreePos() {
        return maxCount-zanato;
    }
    
    public AIFloat3 getFreePos(Map map,int buildFacing) {
        if (forUnitType==null) return new AIFloat3(cx, 0, cy); // !!!
        
        AIFloat3 pos;
        if (getNumFreePos()>0) for (int i=0;i<maxCount;i++) {
            pos=getPos(i);
            if (map.isPossibleToBuildAt(forUnitType, pos, buildFacing)) return pos;
            if (zanato<i) clb.sendTextMsg("WARNING getFreePos: not correct free position: zanato="+zanato+" but position not free i="+i, FieldBOT.MSG_DBG_ALL);// LOG ERROR!!!!!!!!!
            // TODO check again zanato<=i ???
        }
        if (zanato<maxCount) {
            clb.sendTextMsg("WARNING getFreePos: not found free position, but: zanato="+zanato+", maxCount="+maxCount, FieldBOT.MSG_DBG_SHORT);// LOG ERROR!!!!!!!!!
            zanato=maxCount;
            // FIXME whay?
        }
        return null;
    }
    
    public void moveTo(float x,float y) {
        cx=x; cy=y;
        // TODO updateZanatoPos call
    }
    
    public boolean collision(float x,float y) {
        return (Math.abs(cx-x)<=width/2) && (Math.abs(cy-y)<=height/2);
    }
    public boolean collision(AIFloat3 point) {
        return collision(point.x,point.z);
    }
    public boolean collision(TPlanCell otherCell) {
        return (Math.abs(cx-otherCell.cx)<=(width+otherCell.width)/2.0f) &&
               (Math.abs(cy-otherCell.cy)<=(height+otherCell.height)/2.0f);
    }
    
    /**
     * Возвращает место правее,левее,выше,ниже или по углам.
     * @param forCell для какой группы расчитывать
     * @param ofsX сдвиг (+1 вправо -1 влевл)
     * @param ofsY сдвиг (вверх вниз, знаки см. координаты карты)
     * @return позицию рядом.
     */
    public AIFloat3 getPointForNearPos(TPlanCell forCell,int ofsX,int ofsY) {
        final float POGRECHNOST=0.1f;// !!!
        float x,y;
        x=cx+(width+forCell.width+POGRECHNOST)/2.0f*(float)ofsX;
        y=cy+(height+forCell.height+POGRECHNOST)/2.0f*(float)ofsY;
        return new AIFloat3(x, 0, y); // TODO test!
    }
    
}
