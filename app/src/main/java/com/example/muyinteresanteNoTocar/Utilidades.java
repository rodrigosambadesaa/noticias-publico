package com.example.muyinteresanteNoTocar;

import android.content.Context;
import java.io.File;

/** Cache privada de la aplicación: no necesita permiso de almacenamiento externo. */
public class Utilidades {
    static File getDiretorioCache(Context contexto) {
        return contexto.getCacheDir();
    }
}
