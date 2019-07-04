package com.paradoxo.amadeus.util;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.paradoxo.amadeus.R;
import com.paradoxo.amadeus.modelo.Autor;
import com.paradoxo.amadeus.modelo.Banco;
import com.paradoxo.amadeus.modelo.Mensageiro;
import com.paradoxo.amadeus.modelo.Mensagem;

import org.joda.time.DateTime;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class Arquivo {

    public static void criarPasta(String nomePasta, Context context) {
        try {
            File novaPasta = new File(Environment.getExternalStorageDirectory() + "/" + nomePasta);
            if (!novaPasta.exists()) {
                novaPasta.mkdirs();
            }
        } catch (Exception e) {
            Toast.makeText(context, context.getString(R.string.erro_criar_sistema_arquivos), Toast.LENGTH_SHORT).show();
        }
    }

    public List<Banco> listarBancoBaixados() {
        List<Banco> listaBancos = new ArrayList<>();
        File[] files = getAmadeusCaminhoRoot().listFiles();

        for (File file : files) {
            String nomeArquivo = file.getName();

            if (nomeArquivo.endsWith(".db")) {
                Banco banco = new Banco();
                banco.setNome(nomeArquivo.replace(".db", ""));
                banco.setTamanho(calcularTamnhoArquivo(file));

                DateTime ultimaVezAtualizado = new DateTime(file.lastModified());
                banco.setDtAtualizadoExibicao(ultimaVezAtualizado.toString("dd-MM-YYYY HH:mm"));

                banco.setBaixado(true);
                listaBancos.add(banco);
            }
        }
        return listaBancos;
    }

    public boolean arquivoExiste(String caminho) {
        try {
            String caminhoArquivo = Environment.getExternalStorageDirectory() + "/" + caminho;
            FileReader fileReader = new FileReader(caminhoArquivo);
            BufferedReader leitor = new BufferedReader(fileReader);
            leitor.close();
            fileReader.close();
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;

        }
    }

    private static ArrayList<String> lerArquivoTexto(String caminho, Context context) {
        ArrayList<String> conteudoArquivo = new ArrayList();
        try {
            FileReader fileReader = new FileReader(caminho);
            BufferedReader leitor = new BufferedReader(fileReader);

            String linha;
            while ((linha = leitor.readLine()) != null) {
                conteudoArquivo.add(linha);
            }

            leitor.close();
            fileReader.close();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, context.getString(R.string.falha_ler_arquivo), Toast.LENGTH_SHORT).show();
        }

        return conteudoArquivo;
    }

    public static Mensageiro importarQpython(String caminho, Context context) {
        Mensageiro mensageiro = new Mensageiro();

        try {
            String resp = caminho.replace("infos.txt", "BD/txt/i.txt");
            String perg = caminho.replace("infos.txt", "BD/txt/o.txt");

            ArrayList<String> info = Arquivo.lerArquivoTexto(caminho, context);
            ArrayList<String> resps = Arquivo.lerArquivoTexto(resp, context);
            ArrayList<String> pergs = Arquivo.lerArquivoTexto(perg, context);

            for (int i = 0; i < resps.size(); i++) {
                Mensagem objMensagem = new Mensagem(resps.get(i), pergs.get(i));
                mensageiro.mensagem.add(objMensagem);
            }

            Mensagem mensagemIa = new Mensagem();
            Mensagem mensagemUsu = new Mensagem();

            Autor objAutorIA = new Autor(1, info.get(1));
            mensagemIa.setAutor(objAutorIA);
            Autor objAutorUSU = new Autor(2, info.get(0));
            mensagemUsu.setAutor(objAutorUSU);

            mensageiro.mensagem.add(mensagemIa);
            mensageiro.mensagem.add(mensagemUsu);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return mensageiro;
    }

    public File getAmadeusCaminhoRoot() {
        return new File(Environment.getExternalStorageDirectory() + "/.Amadeus");
    }

    public static String getCaminhoPadraoAmadeus(Context c) {
        // Retorna o caminho que o banco de dados deverá ficar na memória do dispositivo do usuário
        return Environment.getDataDirectory() +
                File.separator + "data" +
                File.separator + c.getPackageName() +
                File.separator + "databases" +
                File.separator + "Amadeus.db";
    }

    public String calcularTamnhoArquivo(File file) {
        long size = getTamanhoPasta(file) / 1024;
        // De bytes para kbytes
        if (size >= 1024) {
            return (size / 1024) + " MB";
        } else {
            return size + " KB";
        }
    }

    private static long getTamanhoPasta(File file) {
        long size = 0;
        if (file.isDirectory()) {
            for (File child : file.listFiles()) {
                size += getTamanhoPasta(child);
            }
        } else {
            size = file.length();
        }
        return size;
    }

    public static void importarBancoPrimeiroUso(String dbname, Context c) throws IOException {
        // Copia um banco de dados pré pronto que enviamos por padrão na pasta "assets" para a pasta no celular do usuário
        // aonde ficará o banco do app

        InputStream bancoAssets = c.getAssets().open(dbname);
        OutputStream bancoInterno = new FileOutputStream(getCaminhoPadraoAmadeus(c));

        byte[] buffer = new byte[1024];
        int length;
        while ((length = bancoAssets.read(buffer)) > 0) {
            bancoInterno.write(buffer, 0, length);
        }

        bancoInterno.flush();
        bancoInterno.close();
        bancoAssets.close();
    }

    public static void copiarBDExternoParaInterno(String nomeBanco, Context context, boolean deletarAofim) throws IOException {
        // Copia um banco de dados pré pronto(provavelmente baixado) para o local do banco de dados atual do app

        try {
            String caminhoNovoBanco = Environment.getExternalStorageDirectory() + "/.Amadeus/" + nomeBanco + ".db";
            InputStream bancoExterno = new FileInputStream(caminhoNovoBanco);

            OutputStream bancoInterno = new FileOutputStream(Environment.getDataDirectory() +
                    File.separator + "data" +
                    File.separator + context.getPackageName() +
                    File.separator + "databases" +
                    File.separator + nomeBanco + ".db");

            byte[] buffer = new byte[1024];
            int length;
            while ((length = bancoExterno.read(buffer)) > 0) {
                bancoInterno.write(buffer, 0, length);
            }

            bancoInterno.flush();
            bancoInterno.close();
            bancoExterno.close();

            if(deletarAofim){
                boolean arquivoDeletado = new File(caminhoNovoBanco).delete();
                // Por fim deletamos o arquivo original do local anterior para evitar erros
                if (arquivoDeletado) {
                    Log.e("Deletado", "Sucesso Na cópia do banco");
                } else {
                    Log.e("ERRO", "Arquivo não deletado");
                }
            }

        } catch (Exception e) {
            Log.e("ERRO", "Na cópia do banco");
            e.printStackTrace();
        }
    }

    public static void moverBancoAntigoParaBancosBaixados(String nomeBanco, Context context) throws IOException {
        try {
            String localCaminhoAnterior = Environment.getDataDirectory() +
                    File.separator + "data" +
                    File.separator + context.getPackageName() +
                    File.separator + "databases" +
                    File.separator + nomeBanco + ".db";

            InputStream bancoAntigo = new FileInputStream(localCaminhoAnterior);

            OutputStream bancosBaixados = new FileOutputStream(Environment.getExternalStorageDirectory() + "/.Amadeus/" + nomeBanco + ".db");

            byte[] buffer = new byte[1024];
            int length;
            while ((length = bancoAntigo.read(buffer)) > 0) {
                bancosBaixados.write(buffer, 0, length);
            }

            bancosBaixados.flush();
            bancosBaixados.close();
            bancoAntigo.close();

            boolean arquivoDletado = context.deleteDatabase(nomeBanco + ".db");
            if (arquivoDletado) {
                Log.e("Deletado", "Sucesso Na cópia do banco");
            } else {
                Log.e("ERRO", "Arquivo não deletado");
            }

        } catch (Exception e) {
            Log.e("ERRO", "Na cópia do banco");
            e.printStackTrace();
        }
    }

    public boolean excluirBanco(String nomeBanco) {
        String caminhoBanco = getAmadeusCaminhoRoot() + "/" + nomeBanco + ".db";
        File file = new File(caminhoBanco);
        return file.delete();
    }
}



