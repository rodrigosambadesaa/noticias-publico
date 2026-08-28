package com.example.muyinteresanteNoTocar;

import java.io.File;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.support.v4.content.ContextCompat;

public class Utilidades {

	static File getDiretorioCache(Context contexto){
	  File f=null;
	  String estadoSoporteExterno = Environment.getExternalStorageState();
      int permisoEscrituraSDConcedido= ContextCompat.checkSelfPermission(contexto, Manifest.permission.WRITE_EXTERNAL_STORAGE);
	  if (permisoEscrituraSDConcedido == PackageManager.PERMISSION_GRANTED && Environment.MEDIA_MOUNTED.equals(estadoSoporteExterno)) { /* Cache externa */
		  f = contexto.getExternalCacheDir(); 
		  if (!f.exists()) f.mkdirs();
	  }

	  else { /* Cache interna */
		  f = contexto.getCacheDir(); 
		  if (!f.exists()) f.mkdirs();
	  }
	  
	  
	  return f;
  }
	

}
