package com.paradoxo.amadeus.retrofit;

import com.paradoxo.amadeus.modelo.Usu;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface UsuarioService {

    @GET("logados/{uid}/nomeunico.json/")
    Call<String> jaLogouAntes(@Path("uid") String uid, @Query("auth") String tokenAcesso);

    @GET("usus/{uid}/nomeunico.json/")
    Call<String> verificarExistencia(@Path("uid") String uid, @Query("auth") String tokenAcesso);

    @PUT("logados/{uid}.json/")
    Call<Void> inserir(@Path("uid") String uid, @Body Usu usu, @Query("auth") String tokenAcesso);

    @PUT("usus/{userId}.json/")
    Call<Void> registrarIdUnico(@Path("userId") String nomeUsu, @Body Usu usu, @Query("auth") String tokenAcesso);
}
