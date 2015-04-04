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
public class StackmatReporterConsole implements StackmatListener {

    public void stateUpdate(StackmatState oldState, StackmatState newState) {
        System.out.println(newState.getFormattedSolutionTime());
    }
}
