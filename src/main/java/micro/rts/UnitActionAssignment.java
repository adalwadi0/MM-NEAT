package micro.rts;

import java.io.Serializable;

import micro.rts.units.Unit;

/**
 *
 * @author santi
 */
@SuppressWarnings("serial")
public class UnitActionAssignment implements Serializable {
    public Unit unit;
    public UnitAction action;
    public int time;
    
    public UnitActionAssignment(Unit a_unit, UnitAction a_action, int a_time) {
        unit = a_unit;
        action = a_action;
        if (action==null) {
            System.err.println("UnitActionAssignment with null action!");
        }
        time = a_time;
    }
    
    public String toString() {
        return unit + " assigned action " + action + " at time " + time;
    }
}
