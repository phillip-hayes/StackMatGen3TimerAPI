package au.com.emc.cubing.stackmat;


import au.com.emc.cubing.stackmat.StackmatState;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author User
 */
public interface StackmatListener {
    public void stateUpdate(StackmatState oldState, StackmatState newState);
}
