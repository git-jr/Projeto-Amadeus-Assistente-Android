//package com.paradoxo.amadeus.util;
//
//import android.content.Context;
//import android.content.Intent;
//import android.content.pm.PackageManager;
//import android.content.pm.ResolveInfo;
//import android.net.Uri;
//import android.util.Log;
//
//import com.paradoxo.amadeus.R;
//import com.paradoxo.amadeus.enums.AcaoEnum;
//import com.paradoxo.amadeus.enums.ItemEnum;
//import com.paradoxo.amadeus.modelo.Sentenca;
//
//import org.greenrobot.eventbus.EventBus;
//
//import java.util.List;
//
//import static android.content.ContentValues.TAG;
//
//public class GerenciaApp {
//
//    public static void encontrarApp(String entrada, Context context) {
//        final PackageManager packageManager = context.getPackageManager();
//        Intent launcherIntent = new Intent(Intent.ACTION_MAIN);
//        launcherIntent.addCategory(Intent.CATEGORY_LAUNCHER);
//        List<ResolveInfo> appsInstalados = packageManager.queryIntentActivities(launcherIntent, 0);
//
//
//        for (ResolveInfo packageInfo : appsInstalados) {
//            String nomeApp = packageInfo.loadLabel(packageManager).toString().toLowerCase();
//            try {
//                if (nomeApp.equals(entrada)) {
//                    Intent intentAppParaAbrir = packageManager.getLaunchIntentForPackage(packageInfo.activityInfo.packageName);
//                    context.startActivity(intentAppParaAbrir);
//                    Log.e(TAG, "Abrindo" + nomeApp);
//                    notificarOutput("Abrindo " + nomeApp);
//                    return;
//                }
//
//            } catch (Exception e) {
//                Log.e(TAG, "ERRO" + "Ao listar o app");
//                notificarOutput(context.getString(R.string.erro_abrir_app) + nomeApp);
//            }
//        }
//
//        int indexEspaco = entrada.indexOf(" ");
//        if (indexEspaco == -1) {
//            notificarOutputErroApp(entrada, context);
//            return;
//        }
//        String nomeDoApp = entrada.substring(indexEspaco).trim();
//        if (entrada.length() == nomeDoApp.length()) {
//            notificarOutputErroApp(entrada, context);
//        } else {
//            encontrarApp(nomeDoApp, context);
//            Log.e(TAG, "ERRO" + "Ao listar o app");
//        }
//    }
//
//    private static void notificarOutput(String mensagem) {
//        Sentenca sentenca = new Sentenca(mensagem);
//        EventBus.getDefault().post(sentenca);
//    }
//
//    private static void notificarOutputErroApp(String nomeApp, Context context) {
//        Log.e(TAG, "ERRO" + "App não existe");
//
//        Sentenca sentenca = new Sentenca(nomeApp + context.getString(R.string.app_nao_instalado_no_momento), AcaoEnum.ACAO_ERRO_APP_NAO_EXISTE);
//        sentenca.setTipo_item(ItemEnum.ITEM_CARD.ordinal());
//        sentenca.addResposta(context.getString(R.string.app_nao_encontrado));
//
//        EventBus.getDefault().post(sentenca);
//    }
//
//    public static void buscarAppOnline(String entrada, Context context) {
//        if (!entrada.isEmpty()) {
//            Intent intenteBuscarApp = new Intent(Intent.ACTION_VIEW);
//            intenteBuscarApp.setData(Uri.parse("https://play.google.com/store/search?q=" + entrada + "&c=apps"));
//            context.startActivity(intenteBuscarApp);
//        }
//    }
//}
