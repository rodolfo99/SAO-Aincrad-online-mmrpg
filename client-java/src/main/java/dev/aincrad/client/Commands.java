package dev.aincrad.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/** Exact existing GameWorld intentions. No coordinates, rewards or stats are fabricated. */
public final class Commands {
    private Commands() {}
    public static ObjectNode action(String type, Object... values) {
        Object[] pairs=new Object[values.length+2];pairs[0]="type";pairs[1]=type;System.arraycopy(values,0,pairs,2,values.length);return Json.object(pairs);
    }
    public static ObjectNode join(String name,String characterId,JsonNode appearance) { return action("join","name",name,"characterId",characterId,"appearance",appearance); }
    public static ObjectNode input(long seq,double dx,double dz) {
        if(seq<0 || !Double.isFinite(dx) || !Double.isFinite(dz) || Math.abs(dx)>1 || Math.abs(dz)>1) throw new IllegalArgumentException("Entrada inválida");
        return action("input","seq",seq,"dx",dx,"dz",dz);
    }
    public static ObjectNode move(double x,double z) {
        if(!Double.isFinite(x)||!Double.isFinite(z))throw new IllegalArgumentException("Destino inválido");
        return action("move","x",x,"z",z);
    }
    public static ObjectNode target(String id,String kind) {
        if(!kind.equals("player")&&!kind.equals("monster"))throw new IllegalArgumentException("Objetivo inválido");
        return action("target","id",id,"kind",kind);
    }
}
