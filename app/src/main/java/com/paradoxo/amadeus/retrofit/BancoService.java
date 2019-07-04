package com.paradoxo.amadeus.retrofit;

import com.paradoxo.amadeus.modelo.Banco;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface BancoService {

    @POST("bancosUsu.json/")
    Call<Void> inserir(@Body Banco banco, @Query("auth") String tokenAcesso);
}
