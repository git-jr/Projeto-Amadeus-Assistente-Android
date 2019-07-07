package com.paradoxo.amadeus.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class EscutadaoraService extends Service {
    public List<Worker> workers = new ArrayList<>();

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e("Script", "OnCreate é chamado apeans uma vez");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e("Script", "onStartCommand");

        Worker worker = new Worker(startId);
        worker.start();

        workers.add(worker);

        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    class Worker extends Thread {
        int count = 0, startId;
        boolean ativo = true;

        public Worker(int startId) {
            this.startId = startId;
        }

        @Override
        public void run() {
            while (ativo) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                count++;
                Log.e("Script", "Count:" + count);
            }
            stopSelf(startId);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        for (Worker worker : workers) {
            worker.ativo = false;
        }

    }
}
