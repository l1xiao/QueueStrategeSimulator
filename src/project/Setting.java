package project;

import java.util.Random;

public class Setting {
	public static final int endTime = 1000;
	public static final int timePort = 8081;
	public static final int processorPort = 8080;
	public static final int[] distributionForPriority = { 1, 1, 1, 5, 5, 4, 2,
			2, 2, 3, 4, 4, 4, 5, 5 };
	public static final int model = 1;
	public static Random randomno = new Random(22);

	public static int getPriority() {
		int index = randomno.nextInt(100) % distributionForPriority.length;
		return distributionForPriority[index];
	}

	public static int getTolerance() {
		return (int) ((1 / (1 + Math.exp(-randomno.nextGaussian()))) * 40 + 5);
	}

	public static int getCost(int tolerance) {
		return (int) ((tolerance - 5) * 0.3 + 1);
	}
	public static void main(String[] args) {
		for(int i = 0; i < 10; i++) {
//			System.out.println((int)(1 / (1 + Math.exp(-randomno.nextGaussian()))) * 100 + 1);
			System.out.println((int)(1 / (1 + Math.exp(-randomno.nextGaussian()))) );
			
		}
		
	}
}
