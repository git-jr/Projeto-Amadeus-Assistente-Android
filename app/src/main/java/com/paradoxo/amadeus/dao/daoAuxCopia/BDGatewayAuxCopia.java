/*

 Todas as classes do Pacote "AuxCopia" foram criadas para que um banco de dados externo possa ser copiado para o lugar do banco de dados padrão do app.

 Porque criar nocas classes e não usar as já existente?:
 Mesmo inportando um novo banco de fora e gerando novas instancias das classes DAO, elas ainda liam dados do banco anterior, o que impossibilitava a troca do mesmo.

 Criei essa solução depois de uns dos dias tentando resolver esse prorblema de outra forma, tentar resolver issso de uma maneira mais simples se possível depois

*/


package com.paradoxo.amadeus.dao.daoAuxCopia;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

public class BDGatewayAuxCopia {

    private static BDGatewayAuxCopia bdGatewayAuxCopia;
    private SQLiteDatabase db;

    private BDGatewayAuxCopia(Context ctx, String nomeOutrobanco){
        BDHelperAuxCopia helper = new BDHelperAuxCopia(ctx,nomeOutrobanco);
        db = helper.getWritableDatabase();
    }

    public static BDGatewayAuxCopia getInstanceOutroBanco(Context ctx, String nomeOutrobanco){
        if(bdGatewayAuxCopia == null)
            bdGatewayAuxCopia = new BDGatewayAuxCopia(ctx,nomeOutrobanco);
        return bdGatewayAuxCopia;
    }

    public SQLiteDatabase getDatabase(){
        return this.db;
    }


}