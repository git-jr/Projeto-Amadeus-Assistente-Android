/*

 Todas as classes do Pacote "AuxCopia" foram criadas para que um banco de dados externo possa ser copiado para o lugar do banco de dados padrão do app.

 Porque criar nocas classes e não usar as já existente?:
 Mesmo inportando um novo banco de fora e gerando novas instancias das classes DAO, elas ainda liam dados do banco anterior, o que impossibilitava a troca do mesmo.

 Criei essa solução depois de uns dos dias tentando resolver esse prorblema de outra forma, tentar resolver issso de uma maneira mais simples se possível depois

*/


package com.paradoxo.amadeus.dao.daoAuxCopia;

import android.content.Context;
import android.database.Cursor;

import com.paradoxo.amadeus.dao.AutorDAO;
import com.paradoxo.amadeus.modelo.Autor;
import com.paradoxo.amadeus.modelo.Mensagem;

import java.util.ArrayList;
import java.util.List;


public class MensagemDAOAuxCopia {
    private static BDGatewayAuxCopia bdGatewayAuxCopia;
    private Context context;

    public MensagemDAOAuxCopia(Context context, String nomeOutrobanco) {
        this.context = context;
        bdGatewayAuxCopia = BDGatewayAuxCopia.getInstanceOutroBanco(context, nomeOutrobanco);
    }

    public List<Mensagem> listarTodas() {
        List<Mensagem> mensagens = new ArrayList<>();
        Cursor cursor = bdGatewayAuxCopia.getDatabase().rawQuery("SELECT * FROM mensagem", null);

        while (cursor.moveToNext()) {
            Mensagem objMsg = new Mensagem();
            objMsg.setId(cursor.getInt(cursor.getColumnIndex("id")));
            objMsg.setConteudo(cursor.getString(cursor.getColumnIndex("conteudo")));
            objMsg.setIdResposta(cursor.getInt(cursor.getColumnIndex("fk_resposta")));

            AutorDAO objAutorDAO = new AutorDAO(context);
            Autor objAutor = new Autor();
            objAutor.setId(cursor.getInt(cursor.getColumnIndex("fk_autor")));
            objMsg.setAutor(objAutorDAO.buscar(objAutor));

            mensagens.add(objMsg);
        }
        cursor.close();
        return mensagens;
    }

}
