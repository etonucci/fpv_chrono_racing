package com.emanueletonucci.etchronorace;


import com.google.android.gms.maps.model.LatLng;

/**
 * Created by Emanuele on 28/09/2016.
 */

public class CrossingPoint {

    private int[] ATTESA_NUOVA_ENTRATA = new int[4];
    private String[] ZONA = new String[4];
    private String[] ZONA_ATTUALE = new String[4];
    private int MAX_RAGGIO = 1750;

    int cost = 10000000;

    public String TestCrossingPoint(int test, LatLng a, LatLng b, Double gps_speed, Double LATITUDINE_POINT, Double LONGITUDINE_POINT, long velocita_minima)
    {

        long LONGITUDINE_POINT_NEW = (long) (LONGITUDINE_POINT * cost);
        long LATITUDINE_POINT_NEW = (long) (LATITUDINE_POINT * cost);
        long LATITUDINE = (long)(a.latitude * cost);
        long LONGITUDINE = (long)(a.longitude * cost);
        long LATITUDINE_PREC = (long)(b.latitude * cost);
        long LONGITUDINE_PREC = (long)(b.longitude * cost);

        long DISTANZA_LAT_POINT = (LATITUDINE - LATITUDINE_POINT_NEW);
        long DISTANZA_LON_POINT = (LONGITUDINE - LONGITUDINE_POINT_NEW);

        long distanza_rilevamento = MAX_RAGGIO; //raggioDaFile; //<----- VEDERE

        if ((Math.abs(DISTANZA_LAT_POINT) < distanza_rilevamento) & (Math.abs(DISTANZA_LON_POINT) < distanza_rilevamento) & (gps_speed >= velocita_minima))
        {
            long COEFA = LATITUDINE - LATITUDINE_PREC;
            long COEFB = LONGITUDINE_PREC - LONGITUDINE;

            if ((DISTANZA_LAT_POINT * COEFA) > (DISTANZA_LON_POINT * COEFB))
                ZONA[test] = "P";
            else
                ZONA[test] = "N";

            if (ATTESA_NUOVA_ENTRATA[test] == 1)
            {
                //SONO APPENA ENTRATO NEL RAGGIO DI CALCOLO
                ZONA_ATTUALE[test] = ZONA[test];
                ATTESA_NUOVA_ENTRATA[test] = 0;
            }
            else
            {
                //SONO GIA' NEL RAGGIO DI CALCOLO DA ALMENO UN CAMPIONE QUINDI CONTROLLO SE E' CAMBIATA LA ZONA
                if (ZONA[test] != ZONA_ATTUALE[test])
                {
                    //MEMORIZZO L'ATTRAVERSAMENTO DEL POINT
                    ATTESA_NUOVA_ENTRATA[test] = 1;
                    //ENTRATA_RAGGIO[test] = 1;
                    if (test == 0) return "FL";
                    if (test == 1) return "S1";
                    if (test == 2) return "S2";
                    if (test == 3) return "S3";
                }
            }
        }
        else
        {
            //SONO ALL'ESTERNO DEL RAGGIO DI CALCOLO
            ATTESA_NUOVA_ENTRATA[test] = 1;
        }
        return "";
    }
}