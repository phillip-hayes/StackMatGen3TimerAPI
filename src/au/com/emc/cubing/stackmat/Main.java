package au.com.emc.cubing.stackmat;

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

    public static void main(String[] args) {

        StackmatManager smm = StackmatManager.getInstance();
        
        smm.register(new StackmatReporterConsole());
        
        smm.setSamplingRate(16000);
        smm.setMixerNumber(5);
        smm.setSwitchThreshold(100);
        
        smm.start();    
        
        System.console().readLine();
        
    }
}
