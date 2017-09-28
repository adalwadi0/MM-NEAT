package edu.southwestern.tasks.innovationengines;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.deeplearning4j.zoo.util.imagenet.ImageNetLabels;
import org.nd4j.linalg.api.ndarray.INDArray;

import edu.southwestern.MMNEAT.MMNEAT;
import edu.southwestern.evolution.genotypes.Genotype;
import edu.southwestern.evolution.mapelites.Archive;
import edu.southwestern.evolution.mapelites.MAPElites;
import edu.southwestern.networks.Network;
import edu.southwestern.parameters.CommonConstants;
import edu.southwestern.parameters.Parameters;
import edu.southwestern.scores.Score;
import edu.southwestern.tasks.LonerTask;
import edu.southwestern.tasks.interactive.objectbreeder.ThreeDimensionalObjectBreederTask;
import edu.southwestern.util.MiscUtil;
import edu.southwestern.util.datastructures.ArrayUtil;
import edu.southwestern.util.datastructures.Triangle;
import edu.southwestern.util.graphics.AnimationUtil;
import edu.southwestern.util.graphics.DrawingPanel;
import edu.southwestern.util.graphics.GraphicsUtil;
import edu.southwestern.util.graphics.ImageNetClassification;
import edu.southwestern.util.graphics.ThreeDimensionalUtil;

public class ShapeInnovationTask<T extends Network> extends LonerTask<T> {
	// Not sure if this is necessary. Does the pre-processing do more than just resize the image?
	// Because the image is already the correct size. However, I read something about additional
	// processing steps somewhere in a DL4J example.
	private static final boolean PREPROCESS = true;
	private double pictureInnovationSaveThreshold = Parameters.parameters.doubleParameter("pictureInnovationSaveThreshold");
	
	private double pitch = (Parameters.parameters.integerParameter("defaultPitch")/(double) ThreeDimensionalObjectBreederTask.MAX_ROTATION) * 2 * Math.PI; 
	private double heading = (Parameters.parameters.integerParameter("defaultHeading")/(double) ThreeDimensionalObjectBreederTask.MAX_ROTATION) * 2 * Math.PI;
	private boolean vertical = false;
	
	@Override
	public int numObjectives() {
		return 0; // None: only behavior characterization
	}

	@Override
	public double getTimeStamp() {
		return 0; // Not used
	}

	@Override
	public Score<T> evaluate(Genotype<T> individual) {
		Network cppn = individual.getPhenotype();
		// Get the shape
		List<Triangle> tris = ThreeDimensionalUtil.trianglesFromCPPN(cppn, ImageNetClassification.IMAGE_NET_INPUT_WIDTH, ImageNetClassification.IMAGE_NET_INPUT_HEIGHT, ThreeDimensionalObjectBreederTask.CUBE_SIDE_LENGTH, ThreeDimensionalObjectBreederTask.SHAPE_WIDTH, ThreeDimensionalObjectBreederTask.SHAPE_HEIGHT, ThreeDimensionalObjectBreederTask.SHAPE_DEPTH, null, ArrayUtil.doubleOnes(numCPPNInputs()));
		// Get image from multiple angles
		Color evolvedColor = new Color(223,233,244); // TODO: Need to change
		BufferedImage[] images = ThreeDimensionalUtil.imagesFromTriangles(tris, ImageNetClassification.IMAGE_NET_INPUT_WIDTH, ImageNetClassification.IMAGE_NET_INPUT_HEIGHT, 0, 3, heading, pitch, evolvedColor, vertical);
		ArrayList<INDArray> scoresFromAngles = new ArrayList<>(images.length);
		for(int i = 0; i < images.length; i++) {
			INDArray imageArray = ImageNetClassification.bufferedImageToINDArray(images[i]);
			INDArray scores = ImageNetClassification.getImageNetPredictions(imageArray, PREPROCESS);
			scoresFromAngles.add(scores);
		}
		// Compute average (Make a util method)
		INDArray scores = scoresFromAngles.get(0);
		for(int i = 1; i < scoresFromAngles.size(); i++) {
			scores.add(scoresFromAngles.get(i)); // sum
		}
		scores.div(scoresFromAngles.size()); // divide to get average
		
		ArrayList<Double> behaviorVector = ArrayUtil.doubleVectorFromINDArray(scores);
		Score<T> result = new Score<>(individual, new double[]{}, behaviorVector);

		if(CommonConstants.watch) {
			DrawingPanel picture = GraphicsUtil.drawImage(images[0], "Image", ImageNetClassification.IMAGE_NET_INPUT_WIDTH, ImageNetClassification.IMAGE_NET_INPUT_HEIGHT);
			// Prints top 4 labels
			String decodedLabels = new ImageNetLabels().decodePredictions(scores);
			System.out.println(decodedLabels);
			// Wait for user
			MiscUtil.waitForReadStringAndEnterKeyPress();
			picture.dispose();
		}
//		if(CommonConstants.netio) {
//			// Lot of duplication of computation from Archive. Can that be fixed?
//			@SuppressWarnings("unchecked")
//			Archive<T> archive = ((MAPElites<T>) MMNEAT.ea).getArchive();
//			List<String> binLabels = archive.getBinMapping().binLabels();
//			for(int i = 0; i < binLabels.size(); i++) {
//				Score<T> elite = archive.getElite(i);
//				// If the bin is empty, or the candidate is better than the elite for that bin's score
//				double binScore = result.behaviorVector.get(i);
//				if(elite == null || binScore > elite.behaviorVector.get(i)) {
//					if(binScore > pictureInnovationSaveThreshold) {
//						String fileName = String.format("%7.5f", binScore) + binLabels.get(i) + individual.getId() + ".jpg";
//						String binPath = archive.getArchiveDirectory() + File.separator + binLabels.get(i);
//						String fullName = binPath + File.separator + fileName;
//						System.out.println(fullName);
//						GraphicsUtil.saveImage(image, fullName);
//					}
//				}
//			}
//		}
		return result;
	}
	
	/**
	 * Save fresh archive of only the final images
	 */
	public void finalCleanup() {
		System.out.println("Save gifs of all final elites");
		int saveWidth = Parameters.parameters.integerParameter("imageWidth"); 
		int saveHeight = Parameters.parameters.integerParameter("imageHeight");
		// Save a collection of only the final images from each MAP Elites bin
		if(CommonConstants.netio) {
			@SuppressWarnings("unchecked")
			Archive<T> archive = ((MAPElites<T>) MMNEAT.ea).getArchive();
			String finalArchive = archive.getArchiveDirectory() + "Final";
			new File(finalArchive).mkdir(); // Make different directory
			List<String> binLabels = archive.getBinMapping().binLabels();
			for(int i = 0; i < binLabels.size(); i++) {
				String label = binLabels.get(i);
				Score<T> score = archive.getElite(i);
				Network cppn = score.individual.getPhenotype();
				
				// Get the shape
				List<Triangle> tris = ThreeDimensionalUtil.trianglesFromCPPN(cppn, saveWidth, saveHeight, ThreeDimensionalObjectBreederTask.CUBE_SIDE_LENGTH, ThreeDimensionalObjectBreederTask.SHAPE_WIDTH, ThreeDimensionalObjectBreederTask.SHAPE_HEIGHT, ThreeDimensionalObjectBreederTask.SHAPE_DEPTH, null, ArrayUtil.doubleOnes(numCPPNInputs()));
				// Render and rotate
				Color evolvedColor = new Color(223,233,244); // TODO: Need to change
				BufferedImage[] images = ThreeDimensionalUtil.imagesFromTriangles(tris, saveWidth, saveHeight, 0, (int) (AnimationUtil.FRAMES_PER_SEC * 3), heading, pitch, evolvedColor, vertical);
				
				double binScore = score.behaviorVector.get(i);
				String fileName = String.format("%7.5f", binScore) + label + ".gif";
				String fullName = finalArchive + File.separator + fileName;
				// Save gif to fullName
				try {
					AnimationUtil.createGif(images, Parameters.parameters.integerParameter("defaultFramePause"), fullName);
				} catch (IOException e) {
					System.out.println("Failed to save gif: " + fullName);
					e.printStackTrace();
					System.exit(1);
				}				
			}
		}
	}


	public int numCPPNInputs() {
		return ThreeDimensionalObjectBreederTask.CPPN_NUM_INPUTS;
	}

	public int numCPPNOutputs() {
		return ThreeDimensionalObjectBreederTask.CPPN_NUM_OUTPUTS;
	}

	public static void main(String[] args) throws FileNotFoundException, NoSuchMethodException {
		MMNEAT.main(new String[]{"runNumber:0","randomSeed:0","base:innovation","mu:400","maxGens:200", //0000",
				"io:true","netio:true","mating:true","task:edu.southwestern.tasks.innovationengines.ShapeInnovationTask",
				"log:InnovationShapes-AllAverage","saveTo:AllAverage","allowMultipleFunctions:true","ftype:0","netChangeActivationRate:0.3",
				"cleanFrequency:400","recurrency:false","logTWEANNData:false","logMutationAndLineage:true",
				"ea:edu.southwestern.evolution.mapelites.MAPElites",
				"watch:false",
				"experiment:edu.southwestern.experiment.evolution.SteadyStateExperiment",
				"mapElitesBinLabels:edu.southwestern.tasks.innovationengines.ImageNetBinMapping","fs:true",
				//"imageNetModel:edu.southwestern.networks.dl4j.VGG19Wrapper",
				//"imageNetModel:edu.southwestern.networks.dl4j.VGG16Wrapper",
				"imageNetModel:edu.southwestern.networks.dl4j.AverageAllZooModelImageNetModels",
				//"imageNetModel:edu.southwestern.networks.dl4j.MinAllZooModelImageNetModels",
				"pictureInnovationSaveThreshold:0.3",
				"imageWidth:500","imageHeight:500", // Final save size
				"includeFullSigmoidFunction:true", // In original Innovation Engine
				"includeTanhFunction:false",
				"includeIdFunction:false",
				"includeFullApproxFunction:false",
				"includeApproxFunction:false",
				"includeFullGaussFunction:true", // In original Innovation Engine
				"includeSineFunction:true", // In original Innovation Engine
				"includeSawtoothFunction:true", // Added 
				"includeAbsValFunction:true", // Added
				"includeHalfLinearPiecewiseFunction:true", // In original Innovation Engine
				"includeStretchedTanhFunction:false",
				"includeReLUFunction:false",
				"includeSoftplusFunction:false",
				"includeLeakyReLUFunction:false",
				"includeFullSawtoothFunction:false",
				"includeTriangleWaveFunction:false", 
				"includeSquareWaveFunction:false"}); 
	}
}