package quitersstorssguard;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import quitersstorssguard.operations.DatabaseManager;


public class Main {

	public static void main(String[] args) {

		// --------------------------
		// Arguments
		// --------------------------
		if (args.length != 2) {
			printUsage();
			System.exit(1);
		}

		// Validate input paths
		Path quiteRssPath = Paths.get(args[0]);
		Path rssGuardPath = Paths.get(args[1]);

		if (!Files.isReadable(quiteRssPath)) {
			System.err.println("Error: Cannot read QuiteRSS database at " + quiteRssPath);
			System.exit(1);
		}

		if (!Files.isWritable(rssGuardPath.getParent())) {
			System.err.println("Error: Cannot write to RSS Guard directory at " + rssGuardPath.getParent());
			System.exit(1);
		}


		// --------------------------
		// Database migration
		// --------------------------
		final String quiteRSS_DB_URL = "jdbc:sqlite:" + quiteRssPath.toString();
		final String rssGuard_DB_URL = "jdbc:sqlite:" + rssGuardPath.toString();


		System.out.println("--------------------------------------------------------------------------------------------");
		System.out.println("--------------------------------- Application Information ----------------------------------");
		System.out.println("--------------------------------------------------------------------------------------------");
		System.out.println("Name	: QuiteRSS to RSSGuard Database Migrator");
		System.out.println("Version	: v1.0");
		System.out.println("Date	: 16-06-2025 (17h47)");
		System.out.println("Source	: " + quiteRssPath);
		System.out.println("Destination : " + rssGuardPath);
		System.out.println("");

		System.out.println("--------------------------------------------------------------------------------------------");
		System.out.println("--------------------------------------- Processing -----------------------------------------");
		System.out.println("--------------------------------------------------------------------------------------------");

		try (DatabaseManager databaseManager = new DatabaseManager(quiteRSS_DB_URL, rssGuard_DB_URL)) {
			List<String> elapsedTimes = databaseManager.performFullMigration();

			System.out.println("--------------------------------------------------------------------------------------------");
			elapsedTimes.forEach(System.out::println);
			System.out.println("--------------------------------------------------------------------------------------------");

		} catch (Exception e) {
			System.err.println("\nERROR: Migration failed");
			System.err.println("Reason: " + e.getMessage());
			e.printStackTrace();
			System.exit(1);
		}
	}


	private static void printUsage() {
		System.out.println("Usage: java -jar migrator.jar <QuiteRSS_DB_Path> <RSSGuard_DB_Path>");
		System.out.println("\nExample:");
		System.out.println("  java -jar migrator.jar /path/to/quiterss.db /path/to/rssguard.db");
		System.out.println("\nNotes:");
		System.out.println("  1. QuiteRSS database must exist and be readable");
		System.out.println("  2. RSSGuard database directory must be writable");
	}


}



