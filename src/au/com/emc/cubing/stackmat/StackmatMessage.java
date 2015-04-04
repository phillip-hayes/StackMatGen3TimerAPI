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
public class StackmatMessage {
    
    public char Instruction;
    public String Time;
    public int Checksum;
    public char CR;
    public char LF;

    @Override
    public String toString() {
        return "Time: " + Time
                + ", Instruction: " + Instruction + System.lineSeparator()
                + ", Time: " + Time + System.lineSeparator()
                + ", Checksum: " + Checksum;
    }    
}
