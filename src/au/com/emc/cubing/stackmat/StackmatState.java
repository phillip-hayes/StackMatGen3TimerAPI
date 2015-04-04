package au.com.emc.cubing.stackmat;

import java.util.Date;

public class StackmatState {
    
        private Date Timestamp;
	private Boolean RightHand = false;
	private Boolean LeftHand = false;
	private boolean Running = false;
	private boolean Reset = true;
	private int Minutes = 0;
	private int Seconds = 0;
	private int Thousands = 0;
	private boolean GreenLight = false;

    /**
     * @return the Timestamp
     */
    public Date getTimestamp() {
        return Timestamp;
    }

    /**
     * @param Timestamp the Timestamp to set
     */
    public void setTimestamp(Date Timestamp) {
        this.Timestamp = Timestamp;
    }

    /**
     * @return the RightHand
     */
    public Boolean getRightHand() {
        return RightHand;
    }

    /**
     * @param RightHand the RightHand to set
     */
    public void setRightHand(Boolean RightHand) {
        this.RightHand = RightHand;
    }

    /**
     * @return the LeftHand
     */
    public Boolean getLeftHand() {
        return LeftHand;
    }

    /**
     * @param LeftHand the LeftHand to set
     */
    public void setLeftHand(Boolean LeftHand) {
        this.LeftHand = LeftHand;
    }

    /**
     * @return the Running
     */
    public boolean isRunning() {
        return Running;
    }

    /**
     * @param Running the Running to set
     */
    public void setRunning(boolean Running) {
        this.Running = Running;
    }

    /**
     * @return the Reset
     */
    public boolean isReset() {
        return Reset;
    }

    /**
     * @param Reset the Reset to set
     */
    public void setReset(boolean Reset) {
        this.Reset = Reset;
    }

    /**
     * @return the Minutes
     */
    public int getMinutes() {
        return Minutes;
    }

    /**
     * @param Minutes the Minutes to set
     */
    public void setMinutes(int Minutes) {
        this.Minutes = Minutes;
    }

    /**
     * @return the Seconds
     */
    public int getSeconds() {
        return Seconds;
    }

    /**
     * @param Seconds the Seconds to set
     */
    public void setSeconds(int Seconds) {
        this.Seconds = Seconds;
    }

    /**
     * @return the Thousands
     */
    public int getThousands() {
        return Thousands;
    }

    /**
     * @param Thousands the Thousands to set
     */
    public void setThousands(int Hundredths) {
        this.Thousands = Hundredths;
    }

    /**
     * @return the GreenLight
     */
    public boolean isGreenLight() {
        return GreenLight;
    }

    /**
     * @param GreenLight the GreenLight to set
     */
    public void setGreenLight(boolean GreenLight) {
        this.GreenLight = GreenLight;
    }

    public String getFormattedSolutionTime() {
        return String.format("%01d:%02d:%03d"
                , this.Minutes
                , this.Seconds
                , this.Thousands
        );

        
    }
}
