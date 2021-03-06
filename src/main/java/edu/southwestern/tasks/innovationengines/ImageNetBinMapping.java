package edu.southwestern.tasks.innovationengines;

import java.util.List;

import org.deeplearning4j.nn.modelimport.keras.trainedmodels.Utils.ImageNetLabels;

import edu.southwestern.evolution.mapelites.BinLabels;
import edu.southwestern.networks.Network;

/**
 * Return names for all ImageNet classes
 * @author Jacob Schrum
 */
public class ImageNetBinMapping<T extends Network> implements BinLabels<T> {
	/**
	 * All 1000 ImageNet labels
	 */
	@Override
	public List<String> binLabels() {
		return ImageNetLabels.getLabels();
	}

}
