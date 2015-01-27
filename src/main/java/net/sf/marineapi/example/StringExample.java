package net.sf.marineapi.example;

import net.sf.marineapi.nmea.event.AbstractSentenceListener;
import net.sf.marineapi.nmea.event.SentenceEvent;
import net.sf.marineapi.nmea.event.SentenceListener;
import net.sf.marineapi.nmea.io.DataListener;
import net.sf.marineapi.nmea.io.StringSentenceReader;
import net.sf.marineapi.nmea.sentence.*;

public class StringExample {

    public StringExample() {
        StringSentenceReader reader = new StringSentenceReader();
        reader.addSentenceListener(new GSAListener());
        reader.addSentenceListener(new MultiSentenceListener());
        reader.addSentenceListener(new SingleSentenceListener(), SentenceId.GSV);
        reader.setDataListener(new DataListener() {
            @Override
            public void dataRead(String data) {
                System.out.println("Unhandled sentence: " + data);
            }
        });


        reader.read("$GNGSA,A,3,,,,,,,,,,,,,5.9,4.5,3.8*2A");
        reader.read("$GPG,08,78,09,346,,70,47,040,,86,85,310,,79,06,031,*62");
        reader.read("$GLGSV,2,2,08,87,26,322,24,85,39,142,,71,76,216,,72,22,219,29*65");
        reader.read("$GPGGA,143734.970,4807.20438,N,00137.97846,W,1,03,99.0,$GLGSV,2,1,08,78,09,346,,70,47,040,,86,85,310,,79,06,031,*62");
    }

    /**
     * Startup method, no arguments required.
     *
     * @param args
     *         None
     */
    public static void main(String[] args) {
        new StringExample();
    }

    public class MultiSentenceListener implements SentenceListener {
        @Override
        public void readingPaused() {
        }

        @Override
        public void readingStarted() {
        }

        @Override
        public void readingStopped() {
        }

        @Override
        public void sentenceRead(SentenceEvent event) {
            Sentence s = event.getSentence();
            if ("GLL".equals(s.getSentenceId())) {
                GLLSentence gll = (GLLSentence) s;
                System.out.println("GLL position: " + gll.getPosition());
            }
            else if ("GGA".equals(s.getSentenceId())) {
                GGASentence gga = (GGASentence) s;
                System.out.println("GGA position: " + gga.getPosition());
            }
        }
    }

    public class SingleSentenceListener implements SentenceListener {
        @Override
        public void readingPaused() {
        }

        @Override
        public void readingStarted() {
        }

        @Override
        public void readingStopped() {
        }

        @Override
        public void sentenceRead(SentenceEvent event) {
            GSVSentence gsv = (GSVSentence) event.getSentence();
            System.out.println("GSV satellites in view: " + gsv.getSatelliteCount());
        }
    }

    public class GSAListener extends AbstractSentenceListener<GSASentence> {
        @Override
        public void sentenceRead(GSASentence gsa) {
            System.out.println("GSA position DOP: " + gsa.getPositionDOP());
        }
    }
}
