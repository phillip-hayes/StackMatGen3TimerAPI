package au.com.emc.cubing.stackmat;

import java.util.List;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;

class StackmatSample {

    private int Value;
    private double Time;

    public StackmatSample(int theValue, double theTime) {
        this.Value = theValue;
        this.Time = theTime;
    }

    public double getTime() {
        return this.Time;
    }

    public int bitValue() {
        return (this.Value > 0) ? 1 : 0;
    }

    public int bitValue(boolean inverted) {
        int retVal = this.bitValue();
        if (inverted) {
            retVal = (retVal == 0) ? 1 : 0;
        }
        return retVal;
    }
}

public class StackmatInterpreter {

    private final static Logger LOGGER = Logger.getLogger(StackmatInterpreter.class.getName()); 
    
    private static final int BAUD = 1200;
    private static final int BITS_PER_BYTE = 8;

    // this is a Gen3 timer
    private static final int BYTES_PER_MESSAGE = 10;

    // there is a spacet of 2 bits inbetween each byte of a stackmat message
    private static final int SPACER_BIT_SIZE = 2;

    // the start spacer bit is special
    private static final int startSpacer = SPACER_BIT_SIZE / 2;

    // the total number of bits in a stackmat message
    private static final int bitsPerMessage = startSpacer
            + BYTES_PER_MESSAGE * 8
            + SPACER_BIT_SIZE * 9;

    private static final double messageLength = ((double) bitsPerMessage) / ((double) BAUD);

    // the length of sequence of '1' between stackmat messages
    private static final double syncThreshold = 0.5 * messageLength;

    private int samplingRate;
    private double samplesPerStackmatMessage;
    private int switchThreshold;
    private int theMixerNo;

    private AudioFormat format;
    public DataLine.Info info;

    private TargetDataLine line;

    private StackmatState state = null;

    private boolean enabled = true;
    private int bitValueBetweenMessages = -1;

    private static Mixer.Info[] aInfos = AudioSystem.getMixerInfo();

    public StackmatInterpreter(int samplingRate, int mixerNumber, int switchThreshold) {
        initialize(samplingRate, mixerNumber, switchThreshold);
    }

    private void initialize(int samplingRate, int mixerNum, int switchThreshold) {
        
        LOGGER.info("Initialising stackmat gen 3 interpreter");
        
        this.samplingRate = samplingRate;
        this.switchThreshold = switchThreshold;
        this.samplesPerStackmatMessage = this.samplingRate * messageLength;

        format = new AudioFormat(samplingRate
                , 8 // sampleSizeInBits
                , 1 // mono
                , true // signed
                , false // bigEndian
        );
        info = new DataLine.Info(TargetDataLine.class, format);

        // store mixer number
        theMixerNo = mixerNum;
 
    }

    private void cleanup() {
        if (line != null) {
            line.stop();
            line.close();
        }
        line = null;
    }

    public void stop() {
        this.enabled = false;
    }

    protected static int resolveMixerIndex(String stackmatTimerInputDeviceName) {
        int retVal = -1;
        if (aInfos != null) {
            for (int i = 0; i < aInfos.length; i++) {
                if (stackmatTimerInputDeviceName.equals(aInfos[i].getName())) {
                    retVal = i;
                    break;
                }
            }
        }
        return retVal;
    }

    protected void changeLine(int mixerNum) {
        if (mixerNum < 0 || mixerNum >= aInfos.length) {
            if (line != null) {
                cleanup();
            }
            return;
        }

        try {
            Mixer mixer = AudioSystem.getMixer(aInfos[mixerNum]);
            if (mixer.isLineSupported(info)) {
                if (line != null) {
                    cleanup();
                }
                
                line = (TargetDataLine) mixer.getLine(info);
                line.open(format);
                line.start();
            }
        } catch (LineUnavailableException e) {
            LOGGER.log(Level.WARNING, "Detected mixer line unavailable", e);            
            cleanup();
        }

        synchronized (this) {
            notify();
        }
    }

    private double readLine(List<StackmatSample> currentPeriod, double timeCounter) {
        int currentSample = 0;

        int totalSamplesNeeded = (int) this.samplesPerStackmatMessage * 4;
        int additionalSamplesNeeded = totalSamplesNeeded - currentPeriod.size();

        if (additionalSamplesNeeded > 0) {
            byte[] buffer = new byte[additionalSamplesNeeded];
            if (line.read(buffer, 0, buffer.length) > 0) {
                for (int c = 0; c < buffer.length; ++c) {
                    //little-endian encoding, bytes are in increasing order
                    currentSample = 0;
                    currentSample |= buffer[c]; //we don't mask with 255 so we don't lost the sign
                    currentPeriod.add(new StackmatSample(currentSample, timeCounter));
                    timeCounter += 1 / ((double) this.samplingRate);
                }
            }
        }

        return timeCounter;
    }

    private int syncMessageStartPoint(int testForBitValue, List<StackmatSample> sampleBuffer) {
        int syncIndex = -1;
        double startOnTime = -1;
        
        for (int idx = 0; idx < sampleBuffer.size(); ++idx) {
            StackmatSample sms = sampleBuffer.get(idx);
            if (sms.bitValue() == testForBitValue) {
                if (startOnTime < 0) {
                    startOnTime = sms.getTime();
                }
            } else if (startOnTime > 0) {
                if ((sms.getTime() - startOnTime) > syncThreshold) {
                    syncIndex = idx;
                    break;
                } else {
                    startOnTime = -1;
                }
            } else {
                startOnTime = -1;
            }
        }
        return syncIndex;
    }

    private int syncMessageEndPoint(List<StackmatSample> sampleBuffer) {
        int retVal = this.syncMessageStartPoint(this.bitValueBetweenMessages, sampleBuffer);
        if (retVal > 0) {
            --retVal;
            StackmatSample nextStart = sampleBuffer.get(retVal);
            while (sampleBuffer.get(retVal).bitValue() == nextStart.bitValue()) {
                --retVal;
            }
        }

        return retVal;
    }

    private StackmatMessage parseMessageData(List<StackmatSample> messageSamples) {

        double samplesPerBit = ((double) messageSamples.size()) / ((double) bitsPerMessage);

        int spacerSize = startSpacer;

        // work out what the points are we will use in the samples
        double currIndex = 0;
        int[][] sampleIdx = new int[BYTES_PER_MESSAGE][BITS_PER_BYTE];
        for (int loop = 0; loop < BYTES_PER_MESSAGE; ++loop) {

            // skip past spacer
            currIndex += spacerSize * samplesPerBit;

            // index for each of the bits in the message
            for (int loop2 = 0; loop2 < BITS_PER_BYTE; ++loop2) {

                // use the sample in the middle of the band as this will give the most reliable reading
                currIndex += (samplesPerBit / 2);
                sampleIdx[loop][loop2] = (int) currIndex;
                currIndex += (samplesPerBit / 2);
            }

            // spacers are a set size past the first
            spacerSize = SPACER_BIT_SIZE;
        }

        // now parse the message using the samples
        StackmatMessage smm = new StackmatMessage();
        for (int loop = 0; loop < BYTES_PER_MESSAGE; ++loop) {
            StringBuilder currByte = new StringBuilder();
            for (int loop2 = 0; loop2 < BITS_PER_BYTE; ++loop2) {                              
                currByte.append(
                        messageSamples.get(
                                sampleIdx[loop][loop2]
                        ).bitValue((this.bitValueBetweenMessages == 0))
                );
            }         
            int foo = Integer.parseInt(currByte.reverse().toString(), 2);
            if (loop == 0) {
                smm.Instruction = (char) foo;
            } else if (loop < 7) {
                if (smm.Time == null) {
                    smm.Time = Character.toString((char) foo);
                } else {
                    smm.Time += (char) foo;
                }
            } else if (loop == 7) {
                smm.Checksum = foo;
            } else if (loop == 8) {
                smm.LF = (char) foo;
            } else {
                smm.CR = (char) foo;

            }
        }

        return smm;
    }

    public void doInBackground() {

        LOGGER.info("Starting Stackmat background thread");
        
        double timeCounter = 0;

        List<StackmatSample> sampleBuffer = new ArrayList<StackmatSample>();

        timeCounter = 0;
        while (this.enabled) {

            // sleep until we have a viable input signal
            while (line == null) {
                try {
                    LOGGER.fine("Waiting for LINE");
                    changeLine(this.theMixerNo); 
                    Thread.sleep(500);
                } catch (Exception ignore) {
                }
            }

            try {
                int syncIndex = -1;
                int thebitValueBetweenMessages = 1;
                while (this.enabled && (syncIndex < 0)) {
                    
                    if (this.bitValueBetweenMessages < 0) {
                        thebitValueBetweenMessages = (thebitValueBetweenMessages == 0) ? 1 : 0;
                        LOGGER.fine("Attempting message synchronisation on bit value " + thebitValueBetweenMessages);
                    } else {
                        thebitValueBetweenMessages = this.bitValueBetweenMessages;
                    }
                    
                    timeCounter = this.readLine(sampleBuffer, timeCounter);
                    syncIndex = this.syncMessageStartPoint(thebitValueBetweenMessages, sampleBuffer);

                    // if we could not locate a starting sync point then clear off buffer and try again
                    // the buffer is sized to hold multiple stackmat messages 
                    if (syncIndex < 0) {
                        sampleBuffer.clear();
                    } else if (this.bitValueBetweenMessages < 0) {
                        
                        LOGGER.fine("Achieved message synchronisation on bit value " + thebitValueBetweenMessages);
                        
                        this.bitValueBetweenMessages = thebitValueBetweenMessages;
                    }
                }

                // only continue processinf if we have not exited the sync loop
                // a as result of a cancel instruction
                if (this.enabled) {

                    // we have a flip and at sync point, clear out all preceding data from buffer
                    for (int idx = 0; idx < syncIndex; ++idx) {
                        sampleBuffer.remove(0);
                    }

                    // find the end of the current message begininng at the sync point
                    int endIndex = this.syncMessageEndPoint(sampleBuffer);
                    if (endIndex > 0) {

                        // now extract out the message payload
                        List<StackmatSample> messageSamples = new ArrayList<StackmatSample>();
                        for (int idx = 0; idx <= endIndex; ++idx) {
                            messageSamples.add(sampleBuffer.get(idx));;
                        }
                        sampleBuffer.removeAll(messageSamples);

                        StackmatMessage smm = this.parseMessageData(messageSamples);
                        if (smm != null) {
                            StackmatManager sm = StackmatManager.getInstance();
                            List<String> errList = sm.setState(smm);
                            if (!errList.isEmpty()) {
                                StringBuffer sb = new StringBuffer();
                                sb.append("Detected invalid message: ");
                                sb.append(System.lineSeparator());
                                for (String em : errList) {
                                    sb.append("   ");
                                    sb.append(em);
                                    sb.append(System.lineSeparator());
                                }
                                LOGGER.log(Level.WARNING, sb.toString());
                            }
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Detected error processing stackmat signal: " + e.getMessage(), e);
            }

        }
        
        // release handles to audio input
        this.cleanup();

    }

    public int getStackmatValue() {
        return switchThreshold;
    }

    public void setStackmatValue(int value) {
        this.switchThreshold = value;
    }
}
