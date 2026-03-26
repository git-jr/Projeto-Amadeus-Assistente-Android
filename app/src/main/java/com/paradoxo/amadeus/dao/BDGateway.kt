package com.paradoxo.amadeus.dao

import android.content.Context
import android.database.sqlite.SQLiteDatabase

class BDGateway private constructor(ctx: Context) {
    val database: SQLiteDatabase = BDHelper(ctx).writableDatabase

    companion object {
        private var instance: BDGateway? = null

        fun getInstance(ctx: Context): BDGateway {
            if (instance == null) instance = BDGateway(ctx)
            return instance!!
        }
    }
}
