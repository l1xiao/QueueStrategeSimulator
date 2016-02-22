package project;

import java.util.Random;

public class Setting {
	public static final int endTime = 10000;
	public static final int timePort = 8081;
	public static final int processorPort = 8080;
	public static final int[] distributionForPriority = { 1, 1, 1, 1, 1, 2, 2,
			2, 2, 3, 4, 4, 4, 5, 5 };
	public static Random randomno = new Random(22);

	public static int getPriority() {
		int index = randomno.nextInt(100) % distributionForPriority.length;
		return distributionForPriority[index];
	}

	public static int getTolerance() {
		return (int) ((1 / (1 + Math.exp(-randomno.nextGaussian()))) * 100 + 1);
	}

	public static int getCost(int tolerance) {
		return (int) (tolerance * 0.5 + 1);
	}
	public static void main(String[] args) {
//		for(int i = 0; i < 10; i++) {
//			System.out.println((int)((1 / (1 + Math.exp(-randomno.nextGaussian()))) * 100 + 1));
////			System.out.println(1 / (1 + Math.exp(-randomno.nextGaussian())) * 100 + 1);
//			
//		}
		
	}
}
