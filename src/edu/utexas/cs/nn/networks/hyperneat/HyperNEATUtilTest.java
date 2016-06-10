package edu.utexas.cs.nn.networks.hyperneat;

import static org.junit.Assert.*;

import java.awt.Color;
import java.util.ArrayList;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import edu.utexas.cs.nn.MMNEAT.MMNEAT;
import edu.utexas.cs.nn.evolution.genotypes.TWEANNGenotype;
import edu.utexas.cs.nn.graphics.DrawingPanel;
import edu.utexas.cs.nn.networks.ActivationFunctions;
import edu.utexas.cs.nn.networks.TWEANN;
import edu.utexas.cs.nn.networks.TWEANN.Node;
import edu.utexas.cs.nn.parameters.Parameters;
import edu.utexas.cs.nn.util.MiscUtil;
import edu.utexas.cs.nn.util.datastructures.Pair;
import edu.utexas.cs.nn.util.datastructures.Triple;

public class HyperNEATUtilTest {

	Substrate sub;
	ArrayList<Node> nodes;
	@Before
	public void setUp() throws Exception {

		Parameters.initializeParameterCollections(
				new String[] { "io:false", "netio:false", "allowMultipleFunctions:true"});
		MMNEAT.loadClasses();
		TWEANNGenotype tg = new TWEANNGenotype();
		TWEANN whyDoINeedYouBitch = new TWEANN(tg);
		sub = new Substrate(new Pair<Integer, Integer>(5, 5), 0, new Triple<Integer, Integer, Integer>(0, 0, 0),
				"I_0");
		nodes = new ArrayList<Node>();
		long l = 0;
		for(int i = 0; i < sub.size.t1 * sub.size.t2; i ++) {
			nodes.add(whyDoINeedYouBitch.new Node(ActivationFunctions.randomFunction(), Node.NTYPE_INPUT, l++))	;
		}
	}

	@After
	public void tearDown() throws Exception {
		sub = null;
		nodes = null;
		MMNEAT.clearClasses();
	}

	@Test
	public void testDrawSubstrate() {
			DrawingPanel dp = HyperNEATUtil.drawSubstrate(sub, nodes, Color.magenta);
			MiscUtil.waitForReadStringAndEnterKeyPress();
			HyperNEATUtil.drawSubstrate(dp, sub, nodes, Color.yellow);
			MiscUtil.waitForReadStringAndEnterKeyPress();
		}
}