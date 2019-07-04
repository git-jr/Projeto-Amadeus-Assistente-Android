package com.paradoxo.amadeus.retrofit;

import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

public class RetrofitInicializador {

    private final Retrofit retrofit;

    public RetrofitInicializador() {
        retrofit = new Retrofit.Builder().baseUrl("https://amadeus-92c26.firebaseio.com")
                .addConverterFactory(JacksonConverterFactory.create())
                .build();
    }

    public BancoService getBancoService() {
        return retrofit.create(BancoService.class);
    }

    public UsuarioService getUsuarioService(){
        return retrofit.create(UsuarioService.class);
    }
}
