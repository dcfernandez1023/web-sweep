package com.io;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class SweepRunIO {
	public static void initSweepRun(String path) throws IOException {
		Path filePath = Path.of(path);
		Files.createFile(filePath);
	}
	
	public static void completeSweepRun(String path, String runId, long startTime, String status, String resultsPath) {
		try {
			long endTime = System.currentTimeMillis();
			try (BufferedWriter writer = new BufferedWriter(new FileWriter(path))) {
				writer.write(runId);
				writer.newLine();
				writer.write(Long.toString(startTime));
				writer.newLine();
				writer.write(Long.toString(endTime));
				writer.newLine();
				writer.write(status);
				writer.newLine();
				writer.write(resultsPath);
			}
		} catch (Exception e) {
			System.out.println("Failed to complete sweep: " + e.getMessage());
		}
	}
}
