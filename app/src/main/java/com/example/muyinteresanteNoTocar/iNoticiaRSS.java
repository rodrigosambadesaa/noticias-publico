package com.example.muyinteresanteNoTocar;

import java.util.ArrayList;

public interface iNoticiaRSS {
	void onRecibeNoticiasRSS(ArrayList<NoticiaRSS> listaNoticias);

	default void onError(DescargaNoticiasRSS.DownloadError error) {
		// Existing RSS consumers may handle failures through the null result.
	}
}
