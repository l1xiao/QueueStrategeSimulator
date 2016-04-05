package project;

import java.util.Random;

public class Setting {
	public static final int endTime = 1000;
	public static final int timePort = 8081;
	public static final int processorPort = 8080;

	// default priority distribution
	private static final int[][] distributionForPriority = {
		{ 1, 1, 1, 1, 5, 4, 2, 2, 3, 3, 4, 4, 4, 1, 5 },
		{ 1, 1, 5, 5, 5, 4, 2, 2, 3, 3, 4, 4, 4, 1, 5 },
		{ 1, 1, 1, 1, 1, 1, 2, 2, 2, 3, 3, 3, 4, 1, 5 },
		{ 1, 1, 4, 5, 5, 4, 3, 2, 3, 3, 4, 4, 4, 1, 5 }
	};

	public static final int model = 2;
	public static Random randomno = new Random(22);
	private static int count = 0;
	@SuppressWarnings("unused")
	public static int getPriority() {

		int index = 0;
		int first = 0;
		if (Setting.model == -1) {
			first = 0;
			index = randomno.nextInt(100) % distributionForPriority[0].length;
		} else {
			if (count < endTime/4) {
				first = 1;
				index = randomno.nextInt(100) % distributionForPriority[1].length;
			} else if (count < endTime/4 * 3) {
				first = 2;
				index = randomno.nextInt(100) % distributionForPriority[2].length;
			} else {
				first = 3;
				index = randomno.nextInt(100) % distributionForPriority[3].length;
			}
			count++;
			
		}
		return distributionForPriority[first][index];

	}

	public static int getTolerance() {
		return (int) ((1 / (1 + Math.exp(-randomno.nextGaussian()))) * 40 + 5);
	}

	public static int getCost(int tolerance) {
		return (int) ((tolerance - 5) * 0.6 + 1);
	}

	public static void main(String[] args) {
		for (int i = 0; i < 10; i++) {
			// System.out.println((int)(1 / (1 +
			// Math.exp(-randomno.nextGaussian()))) * 100 + 1);
			System.out.println((int) (1 / (1 + Math.exp(-randomno
					.nextGaussian()))));

		}

	}
}
