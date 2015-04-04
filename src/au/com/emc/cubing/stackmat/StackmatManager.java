package au.com.emc.cubing.stackmat;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.util.List;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author User
 */
public class StackmatManager extends Object {

    private final static Logger LOGGER = Logger.getLogger(StackmatManager.class.getName()); 
    
    private volatile StackmatState State;
    private List<StackmatListener> Listeners;
    private boolean changed;
    private final Object MUTEX = new Object();
    private int samplingRate = 16000;
    private int mixerNumber = 5;
    private int switchThreshold = 100;
    private StackmatInterpreter si;
    
    //PRIVATE constructor
    private StackmatManager() {
        super();
        changed = false;
        Listeners = new ArrayList<StackmatListener>();
    }

    /**
     * @return the samplingRate
     */
    public int getSamplingRate() {
        return samplingRate;
    }

    /**
     * @param samplingRate the samplingRate to set
     */
    public void setSamplingRate(int samplingRate) {
        this.samplingRate = samplingRate;
    }

    /**
     * @return the mixerNumber
     */
    public int getMixerNumber() {
        return mixerNumber;
    }

    /**
     * @param mixerNumber the mixerNumber to set
     */
    public void setMixerNumber(int mixerNumber) {
        this.mixerNumber = mixerNumber;
        if (si != null) {
            si.changeLine(mixerNumber);
        }
    }

    public int resolveMixerName(String theName) {
        return StackmatInterpreter.resolveMixerIndex(theName);
    }
    
    /**
     * @return the switchThreshold
     */
    public int getSwitchThreshold() {
        return switchThreshold;
    }

    /**
     * @param switchThreshold the switchThreshold to set
     */
    public void setSwitchThreshold(int switchThreshold) {
        this.switchThreshold = switchThreshold;
    }

    private static class SingletonHolder {

        private static StackmatManager instance = new StackmatManager();
    }

    public static StackmatManager getInstance() {
        return SingletonHolder.instance;
    }

    public void register(StackmatListener l) {
        synchronized (MUTEX) {
            if (!Listeners.contains(l)) {
                Listeners.add(l);
            }
        }
    }

    public void unregister(StackmatListener obj) {
        synchronized (MUTEX) {
            Listeners.remove(obj);
        }
    }
    
    
    public void notifyObservers(StackmatState oldState, StackmatState newState) {
        List<StackmatListener> observersLocal = null;
        //synchronization is used to make sure any observer registered after message is received is not notified
        synchronized (MUTEX) {
            if (!changed)
                return;
            observersLocal = new ArrayList<>(this.Listeners);
            this.changed=false;
        }
        for (StackmatListener obj : observersLocal) {
            obj.stateUpdate(oldState, newState);
        }
 
    }
    

    /**
     * @return the State
     */
    public StackmatState getState() {
        return State;
    }

    /**
     * @param State the State to set
     */
    protected List<String> setState(StackmatMessage theMessage) {

        List<String> errList = new ArrayList<String>();
        
        // is instruction valid ?
        if (! " ACILRS".contains(String.valueOf(theMessage.Instruction))) {
            errList.add("Invalid instruction '" + theMessage.Instruction + "'");
        }
        
        // checksum (64 + sum of time digits)
        if (theMessage.Time == null) {
            errList.add("Timer value missing");
        } else {
            int checksum = 64;
            for (int loop = 0; loop < theMessage.Time.length(); ++loop) {
                if (Character.isDigit(theMessage.Time.charAt(loop))) {
                    checksum += Integer.parseInt(String.valueOf(theMessage.Time.charAt(loop)));
                } else {
                    errList.add("Invalid character in timer value: " + theMessage.Time.charAt(loop));
                }
            }
            if (theMessage.Checksum != checksum) {
                
            }
        }
        
        // message end signature check
        if (theMessage.CR != '\r') {
            errList.add("Invalid message end sequence: CR expected, found char " + ((int) theMessage.CR));
        }
        if (theMessage.LF != '\n') {
            errList.add("Invalid message end sequence: LF expected, found char " + ((int) theMessage.LF));
        }
        

        // message is valid, parse and set state
        if (errList.isEmpty()) {
            
            // log the message but only do the string concat etc if the message is loggable
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine("New stackmat message: " + theMessage.Instruction + " : " + theMessage.Time + " : CHKSUM=" + theMessage.Checksum);
            }
            
            StackmatState newState = new StackmatState();

            newState.setMinutes(Integer.parseInt(Character.toString(theMessage.Time.charAt(0))));
            newState.setSeconds(Integer.parseInt(theMessage.Time.substring(1, 3)));
            newState.setThousands(Integer.parseInt(theMessage.Time.substring(3)));

            //  'I': timer initialized and reset to 0 (both hand pads open)
            //  'A': timer ready to begin, both hand-pads covered
            //  ' ': timer running/counting (both hand-pads open)
            //  'S': timing complete (both hand-pads open)
            //  'L': left hand-pad covered (overrides 'I', ' ', 'S')
            //  'R': right hand-pad covered (overrides 'I', ' ', 'S')
            //  'C': both hand-pads covered (overrides 'I', ' ', 'S')
            newState.setLeftHand(false);
            newState.setRightHand(false);
            newState.setRunning(false);
            if (this.State != null) {
                newState.setRunning(this.State.isRunning());
            }
            newState.setReset(false);
            newState.setGreenLight(false);

            switch (theMessage.Instruction) {
                case 'I':
                    newState.setReset(true);
                    break;
                case 'A':
                    newState.setGreenLight(true);
                    newState.setLeftHand(true);
                    newState.setRightHand(true);
                    break;
                case ' ':
                    newState.setRunning(true);
                    break;
                case 'S':
                    newState.setRunning(false);
                    break;
                case 'L':
                    newState.setLeftHand(true);
                    break;
                case 'R':
                    newState.setRightHand(true);
                    break;
                case 'C':
                    newState.setLeftHand(true);
                    newState.setRightHand(true);
                    break;
            }
            
            newState.setTimestamp(new java.util.Date());

            StackmatState old = this.State;
            if (old == null) {
                changed = true;
            } else {
                changed =
                        old.getLeftHand() != newState.getLeftHand()
                        || old.getRightHand() != newState.getRightHand()
                        || old.isGreenLight() != newState.isGreenLight()
                        || old.isReset()!= newState.isReset()
                        || old.isRunning()!= newState.isRunning()
                        || old.getMinutes()!= newState.getMinutes()
                        || old.getSeconds()!= newState.getSeconds()
                        || old.getThousands()!= newState.getThousands()
                        ;
            }
            
            this.notifyObservers(this.State, newState);

            this.State = newState;
            
        }
        
        return errList;
    }

    
    public void start() {
        
        LOGGER.info("Starting Stackmat gen 3 manager");
        
        // get instance of interpreter
        if (si != null) {
            si.stop();
            si = null;
        }
        si = new StackmatInterpreter(samplingRate, mixerNumber, switchThreshold);

        // run in background thread
        new Thread()
        {
            public void run() {
                si.doInBackground();
            }
        }.start();          
    }
    
    public void stop() {
        
        LOGGER.info("Stopping Stackmat gen 3 manager");
        
        // send stop signal to running thread
        if (si != null) {
            si.stop();
            si = null;
        }        
    }
}
