/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.southwestern.evolution.nsga2.bd.vectors;

import edu.southwestern.util.stats.StatisticsUtilities;
import java.util.BitSet;
import java.util.ArrayList;

/**
 * Gets the highest bit behavior vector for behavioral diversity
 * 
 * @author Jacob Schrum
 * @commented Lauren Gillespie
 */
public class HighestBitBehaviorVector extends BitBehaviorVector {

	/**
	 * Constructor
	 * 
	 * @param xs
	 *            syllabus(??)
	 * @param groupSize
	 *            size of group toget behavior vectors
	 */
	public HighestBitBehaviorVector(ArrayList<Double> xs, int groupSize) {
		super(groupBits(xs, groupSize));
	}

	/**
	 * For every set of outputs (groupSize) one action is chosen, which has the
	 * highest output value. That position is mapped to 1 and the un-chosen
	 * actions map to 0.
	 */
	public static BitSet groupBits(ArrayList<Double> xs, int groupSize) {
		BitSet bs = new BitSet(xs.size());
		for (int i = 0; i < xs.size(); i += groupSize) {
			double[] set = new double[groupSize];
			for (int j = 0; j < groupSize; j++) {
				set[j] = xs.get(i + j);
			}
			int highest = StatisticsUtilities.argmax(set);
			bs.set(i + highest);
		}
		return bs;
	}
}
