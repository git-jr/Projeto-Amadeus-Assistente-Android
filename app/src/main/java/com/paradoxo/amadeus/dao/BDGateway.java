package com.paradoxo.amadeus.dao;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

public class BDGateway {

    private static BDGateway bdGateway;
    private SQLiteDatabase db;

    public BDGateway(Context ctx) {
        BDHelper helper = new BDHelper(ctx);
        db = helper.getWritableDatabase();
    }

    public static BDGateway getInstance(Context ctx) {
        if (bdGateway == null)
            bdGateway = new BDGateway(ctx);
        return bdGateway;
    }

    public SQLiteDatabase getDatabase() {
        return this.db;
    }
}