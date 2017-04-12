package au.com.emc.cubing.stackmat;

import java.util.logging.Logger;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author User
 */
public class Main {

    private final static Logger LOGGER = Logger.getLogger(Main.class.getName()); 

    public static void main(String[] args) throws Exception {

        StackmatManager smm = StackmatManager.getInstance();

        smm.register(new StackmatReporterConsole());

        smm.setSamplingRate(16000);
        smm.setMixerNumber(5);
        smm.setSwitchThreshold(100);

        smm.start();

        try {
            // There may be no console when running in Eclipse or other IDE 
            if( System.console() != null ) {
                LOGGER.info("Press Enter to continue...");
                System.console().readLine();
            } else {
                LOGGER.info("Waiting 60 seconds...");
                Thread.sleep(60l * 1000);
            }
        } finally {
            LOGGER.info("Stopping manager...");
            smm.stop();
        }

    }
}
