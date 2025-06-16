package quitersstorssguard.operations;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.sqlite.SQLiteConfig;

import quitersstorssguard.records.SqlitePragmaSettings;
import quitersstorssguard.records.Store_QuiteRSS;
import quitersstorssguard.records.Store_QuiteRssFeed;
import quitersstorssguard.utils.Miscellaneous;



public class DatabaseManager implements AutoCloseable {

	@FunctionalInterface
	public interface BatchProcessor<T> {
		void processBatch(List<T> batch) throws SQLException;
	}


	// **************************************************************************************************************************************************
	// ******************************************************************* Constants ********************************************************************
	// **************************************************************************************************************************************************
	private static final String LOAD_QUITERSS_FEEDS_QUERY = Miscellaneous.readFileFromResources("resources/load_quiterss_feeds.sql");
	private static final String LOAD_QUITERSS_NEWS_QUERY = Miscellaneous.readFileFromResources("resources/load_quiterss_news.sql");
	private static final String INSERT_RSSGUARD_CATEGORIES_QUERY = Miscellaneous.readFileFromResources("resources/insert_rssguard_categories.sql");
	private static final String INSERT_RSSGUARD_FEEDS_QUERY = Miscellaneous.readFileFromResources("resources/insert_rssguard_feeds.sql");
	private static final String INSERT_RSSGUARD_MESSAGES_QUERY = Miscellaneous.readFileFromResources("resources/insert_rssguard_messages.sql");

	private static final int FEED_BATCH_SIZE = 2000;  // Process feeds in chunks of 2000
	private static final int ARTICLE_BATCH_SIZE = 5000; // Process articles in chunks of 5000
	private static final int INSERT_BATCH_SIZE = 2000;


	// **************************************************************************************************************************************************
	// ****************************************************************** Declarations ******************************************************************
	// **************************************************************************************************************************************************
	private final String quiteRSS_DB_URL;
	private final String rssGuard_DB_URL;
	private Connection sourceConnection;
	private Connection targetConnection;
	private SqlitePragmaSettings originalPragmaSettings;

	private List<String> migrationLog = new ArrayList<>();
	private Duration totalElapsedTime = Duration.ZERO;
	private Instant lastOperationStartTime;



	// **************************************************************************************************************************************************
	// ****************************************************************** Constructors ******************************************************************
	// **************************************************************************************************************************************************
	public DatabaseManager(String quiteRSS_DB_URL, String rssGuard_DB_URL) throws SQLException, ClassNotFoundException {
		this.quiteRSS_DB_URL = quiteRSS_DB_URL;
		this.rssGuard_DB_URL = rssGuard_DB_URL;
		configureConnections();
	}


	// **************************************************************************************************************************************************
	// ***************************************************************** Initialization *****************************************************************
	// **************************************************************************************************************************************************
	private void configureConnections() throws SQLException, ClassNotFoundException {

		Class.forName("org.sqlite.JDBC");

		// Store original values BEFORE changing them
		try (Connection tempConn = DriverManager.getConnection(rssGuard_DB_URL)) {
			this.originalPragmaSettings = readCurrentPragmaSettings(tempConn);
		}


		// For better migration performance
		SQLiteConfig sourceConfig = new SQLiteConfig();
		sourceConfig.setReadOnly(true);
		sourceConfig.setPragma(SQLiteConfig.Pragma.CACHE_SIZE, "20000");
		sourceConfig.setPragma(SQLiteConfig.Pragma.TEMP_STORE, "MEMORY");
		sourceConfig.setPragma(SQLiteConfig.Pragma.BUSY_TIMEOUT, "30000");


		// For better migration performance
		SQLiteConfig targetConfig = new SQLiteConfig();
		targetConfig.enforceForeignKeys(true);
		targetConfig.setPragma(SQLiteConfig.Pragma.CACHE_SIZE, "20000");
		targetConfig.setPragma(SQLiteConfig.Pragma.PAGE_SIZE, "4096");
		targetConfig.setPragma(SQLiteConfig.Pragma.TEMP_STORE, "MEMORY");
		targetConfig.setPragma(SQLiteConfig.Pragma.LOCKING_MODE, "EXCLUSIVE");
		targetConfig.setPragma(SQLiteConfig.Pragma.BUSY_TIMEOUT, "30000");

		// Only use these settings during migration of non-critical data
		targetConfig.setPragma(SQLiteConfig.Pragma.SYNCHRONOUS, "OFF"); // Most dangerous but fastest & can corrupt database if system crashes
		targetConfig.setPragma(SQLiteConfig.Pragma.JOURNAL_MODE, "OFF"); // More aggressive than MEMORY & removes crash protection completely
		// config.setPragma(SQLiteConfig.Pragma.JOURNAL_MODE, "MEMORY");



		sourceConnection = DriverManager.getConnection(quiteRSS_DB_URL, sourceConfig.toProperties());
		targetConnection = DriverManager.getConnection(rssGuard_DB_URL, targetConfig.toProperties());

		//		try (Statement stmt = targetConnection.createStatement()) {
		//			stmt.execute("PRAGMA optimize"); // Initial optimization
		//		}
	}



	// **************************************************************************************************************************************************
	// ******************************************************************** Methods *********************************************************************
	// **************************************************************************************************************************************************
	public List<String> performFullMigration() throws Exception {

		try {
			lastOperationStartTime = Instant.now();

			// --------------------------
			// Categories & Feeds
			// --------------------------
			System.out.println("QuiteRSS\t: Retrieving Categories & Feeds ...");
			processFeedsInBatches(this::insertCategoriesAndFeeds);
			logElapsedTime("  - %s ms\t: Categories & Feeds processed.", -1);

			// --------------------------
			// Articles
			// --------------------------
			System.out.println("QuiteRSS\t: Retrieving articles ...");
			processArticlesInBatches(this::insertArticles);
			logElapsedTime("  - %s ms\t: Articles processed.", -1);

			// --------------------------
			// Vacuum
			// --------------------------
			try (Statement stmt = targetConnection.createStatement()) {
				// First run optimization analysis
				// stmt.execute("PRAGMA optimize");
				// logElapsedTime("  - %s ms\t: RSS Guard database optimization analysis completed", -1);

				// Then perform vacuum
				stmt.execute("VACUUM");
				logElapsedTime("  - %s ms\t: RSS Guard database vacuum completed", -1);
			} catch (SQLException e) {
				System.err.println("RSS Guard database optimization failed : " + e.getMessage());
				migrationLog.add("  - WARNING: Database optimization failed: " + e.getMessage());
			}

			validateMigration();
			migrationLog.add(1, "Operation details :");



			return migrationLog;

		} catch (SQLException e) {
			throw new Exception("Database migration failed", e);
		}

	}


	private void processFeedsInBatches(BatchProcessor<Store_QuiteRssFeed> processor) throws SQLException {

		try (PreparedStatement stmt = sourceConnection.prepareStatement(LOAD_QUITERSS_FEEDS_QUERY);
				ResultSet rs = stmt.executeQuery()) {

			List<Store_QuiteRssFeed> batch = new ArrayList<>(FEED_BATCH_SIZE);
			int totalProcessed = 0;

			while (rs.next()) {
				batch.add(new Store_QuiteRssFeed(
						rs.getInt("rowToParent"),
						rs.getInt("parentId"),
						rs.getInt("id"),
						stripToEmpty(rs.getString("text")),
						stripToEmpty(rs.getString("description")),
						Miscellaneous.convertToTimestamp(rs.getString("created")),
						stripToEmpty(rs.getString("xmlUrl")),
						rs.getBytes("image"),
						rs.getInt("disableUpdate"),
						rs.getInt("layoutDirection"),
						rs.getInt("addSingleNewsAnyDateOn")
						));

				if (batch.size() >= FEED_BATCH_SIZE) {
					processor.processBatch(batch);
					totalProcessed += batch.size();
					System.out.printf("Processed %d feeds...%n", totalProcessed);
					batch.clear();
				}
			}

			// Process remaining items in last batch
			if (!batch.isEmpty()) {
				processor.processBatch(batch);
				totalProcessed += batch.size();
				System.out.printf("Finished processing %d total feeds.%n", totalProcessed);
			}
		}
	}


	private void processArticlesInBatches(BatchProcessor<Store_QuiteRSS> processor) throws SQLException {
		try (PreparedStatement stmt = sourceConnection.prepareStatement(LOAD_QUITERSS_NEWS_QUERY);
				ResultSet rs = stmt.executeQuery()) {

			List<Store_QuiteRSS> batch = new ArrayList<>(ARTICLE_BATCH_SIZE);
			int totalProcessed = 0;

			while (rs.next()) {
				batch.add(new Store_QuiteRSS(
						rs.getString("author_name"),
						rs.getString("description"),
						rs.getString("guid"),
						Miscellaneous.convertToTimestamp(rs.getString("received")),
						rs.getInt("feedId"),
						rs.getInt("id"),
						rs.getInt("deleted"),
						rs.getInt("starred"),
						rs.getInt("read"),
						rs.getString("title"),
						rs.getString("link_href")
						));

				if (batch.size() >= ARTICLE_BATCH_SIZE) {
					processor.processBatch(batch);
					totalProcessed += batch.size();
					System.out.printf("Processed %d articles...%n", totalProcessed);
					batch.clear();
				}
			}

			// Process remaining items in last batch
			if (!batch.isEmpty()) {
				processor.processBatch(batch);
				totalProcessed += batch.size();
				System.out.printf("Finished processing %d total articles.%n", totalProcessed);
			}
		}
	}


	private void insertCategoriesAndFeeds(List<Store_QuiteRssFeed> batch) throws SQLException {
		try {
			targetConnection.setAutoCommit(false);
			insertCategories(batch);
			insertFeeds(batch);
			targetConnection.commit();
		} catch (SQLException e) {
			targetConnection.rollback();
			throw e;
		} finally {
			targetConnection.setAutoCommit(true);
		}
	}


	private void insertArticles(List<Store_QuiteRSS> batch) throws SQLException {
		try {
			targetConnection.setAutoCommit(false);

			try (PreparedStatement stmt = targetConnection.prepareStatement(INSERT_RSSGUARD_MESSAGES_QUERY)) {
				for (Store_QuiteRSS article : batch) {
					stmt.setString(1, article.author_name());
					stmt.setString(2, article.description());
					stmt.setString(3, article.guid());
					stmt.setLong(4, article.received());
					stmt.setInt(5, article.feedId());
					stmt.setInt(6, article.id());
					stmt.setInt(7, article.deleted());
					stmt.setInt(8, 0);
					stmt.setInt(9, article.starred());
					stmt.setInt(10, article.read());
					stmt.setString(11, article.title());
					stmt.setString(12, article.link_href());
					stmt.setInt(13, 1);
					stmt.addBatch();
				}
				stmt.executeBatch();
			}

			targetConnection.commit();
		} catch (SQLException e) {
			targetConnection.rollback();
			throw e;
		} finally {
			targetConnection.setAutoCommit(true);
		}
	}


	private void insertCategories(List<Store_QuiteRssFeed> feeds) throws SQLException {

		try (PreparedStatement stmt = targetConnection.prepareStatement(INSERT_RSSGUARD_CATEGORIES_QUERY)) {
			int batchCount = 0;

			for (Store_QuiteRssFeed feed : feeds) {
				if (!feed.isCategory())
					continue;

				stmt.setInt(1, feed.order());
				stmt.setInt(2, feed.id() == 122 ? -1 : feed.parentId());
				stmt.setInt(3, feed.id());
				stmt.setString(4, feed.title());
				stmt.setLong(5, feed.created());
				stmt.setInt(6, 1);
				stmt.setString(7, "");

				stmt.addBatch();

				if (++batchCount % INSERT_BATCH_SIZE == 0)
					stmt.executeBatch();
			}

			stmt.executeBatch();
		}
	}


	private void insertFeeds(List<Store_QuiteRssFeed> feeds) throws SQLException {

		try (PreparedStatement stmt = targetConnection.prepareStatement(INSERT_RSSGUARD_FEEDS_QUERY)) {
			int batchCount = 0;

			for (Store_QuiteRssFeed feed : feeds) {
				if (feed.isCategory())
					continue;

				stmt.setInt(1, feed.id());
				stmt.setInt(2, feed.order());
				stmt.setString(3, feed.title());
				stmt.setString(4, feed.description());
				stmt.setLong(5, feed.created());
				stmt.setBytes(6, feed.icon());
				stmt.setInt(7, feed.parentId());
				stmt.setString(8, feed.feedURL());
				stmt.setInt(9, 1);
				stmt.setInt(10, 900);
				stmt.setInt(11, feed.disableUpdate());
				stmt.setInt(12, 0);
				stmt.setInt(13, feed.layoutDirection());
				stmt.setInt(14, feed.addSingleNewsAnyDateOn());
				stmt.setInt(15, 0);
				stmt.setInt(16, 0);
				stmt.setInt(17, 0);
				stmt.setInt(18, 1);
				stmt.setInt(19, 1);
				stmt.setInt(20, 1);
				stmt.setInt(21, 0);
				stmt.setInt(22, 1);
				stmt.setInt(23, feed.id());
				stmt.setString(24, "");

				stmt.addBatch();

				if (++batchCount % INSERT_BATCH_SIZE == 0)
					stmt.executeBatch();
			}

			stmt.executeBatch();
		}
	}



	private void validateMigration() throws SQLException {

		try (Statement stmt = targetConnection.createStatement();
				ResultSet rs = stmt.executeQuery(
						"SELECT (SELECT COUNT(*) FROM feeds) AS feed_count, (SELECT COUNT(*) FROM messages) AS message_count")) {
			if (rs.next()) {
				migrationLog.add(0, String.format("Migration completed successfully (%d feeds, %d messages) in %s.",
						rs.getInt("feed_count"),
						rs.getInt("message_count"),
						Miscellaneous.durationToHumanReadable(totalElapsedTime)
						));
			}
		}
	}


	// **************************************************************************************************************************************************
	// ******************************************************************** Helpers *********************************************************************
	// **************************************************************************************************************************************************
	private void logElapsedTime(String message, int itemCount) {

		Duration duration = Duration.between(lastOperationStartTime, Instant.now());
		totalElapsedTime = totalElapsedTime.plus(duration);

		if (itemCount == -1)
			migrationLog.add(String.format(message, duration.toMillis()));
		else
			migrationLog.add(String.format(message, duration.toMillis(), itemCount));

		lastOperationStartTime = Instant.now();
	}


	private String stripToEmpty(String input) {
		return input == null ? "" : input.strip();
	}


	private void closeQuietly(AutoCloseable closeable) {
		if (closeable == null)
			return;

		try {
			closeable.close();
		} catch (Exception e) {
			System.err.println("Error closing resource: " + e.getMessage());
		}
	}



	public void logMemoryUsage() {
		Runtime runtime = Runtime.getRuntime();
		long usedMB = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
		long maxMB = runtime.maxMemory() / (1024 * 1024);
		long totalMB = runtime.totalMemory() / (1024 * 1024);

		System.out.printf("Memory: Used=%dMB, Allocated=%dMB, MaxAvailable=%dMB%n", usedMB, totalMB, maxMB);

		// Optional: Log to migration log as well
		// migrationLog.add(String.format("Memory snapshot: Used=%dMB, Allocated=%dMB, Max=%dMB", usedMB, totalMB, maxMB));
	}



	private SqlitePragmaSettings readCurrentPragmaSettings(Connection conn) throws SQLException {

		// Default conservative values
		String synchronous = "FULL";
		String journalMode = "DELETE";
		int cacheSize = -2000;
		int pageSize = 1024;
		String tempStore = "DEFAULT";
		String lockingMode = "NORMAL";
		int mmapSize = 0;
		int autoVacuum = 1;

		// Read each pragma with individual try-catch blocks
		try (Statement stmt = conn.createStatement();
				ResultSet rs = stmt.executeQuery("PRAGMA synchronous")) {
			if (rs.next())
				synchronous = rs.getString(1);
		} catch (SQLException e) {
			System.err.println("Warning: Could not read synchronous pragma: " + e.getMessage());
		}

		try (Statement stmt = conn.createStatement();
				ResultSet rs = stmt.executeQuery("PRAGMA journal_mode")) {
			if (rs.next())
				journalMode = rs.getString(1);
		} catch (SQLException e) {
			System.err.println("Warning: Could not read journal_mode pragma: " + e.getMessage());
		}

		try (Statement stmt = conn.createStatement();
				ResultSet rs = stmt.executeQuery("PRAGMA cache_size")) {
			if (rs.next())
				cacheSize = rs.getInt(1);
		} catch (SQLException e) {
			System.err.println("Warning: Could not read cache_size pragma: " + e.getMessage());
		}

		try (Statement stmt = conn.createStatement();
				ResultSet rs = stmt.executeQuery("PRAGMA page_size")) {
			if (rs.next())
				pageSize = rs.getInt(1);
		} catch (SQLException e) {
			System.err.println("Warning: Could not read page_size pragma: " + e.getMessage());
		}

		try (Statement stmt = conn.createStatement();
				ResultSet rs = stmt.executeQuery("PRAGMA temp_store")) {
			if (rs.next())
				tempStore = rs.getString(1);
		} catch (SQLException e) {
			System.err.println("Warning: Could not read temp_store pragma: " + e.getMessage());
		}

		try (Statement stmt = conn.createStatement();
				ResultSet rs = stmt.executeQuery("PRAGMA locking_mode")) {
			if (rs.next())
				lockingMode = rs.getString(1);
		} catch (SQLException e) {
			System.err.println("Warning: Could not read locking_mode pragma: " + e.getMessage());
		}

		try (Statement stmt = conn.createStatement();
				ResultSet rs = stmt.executeQuery("PRAGMA mmap_size")) {
			if (rs.next()) mmapSize = rs.getInt(1);
		} catch (SQLException e) {
			System.err.println("Warning: Could not read mmap_size pragma: " + e.getMessage());
		}

		try (Statement stmt = conn.createStatement();
				ResultSet rs = stmt.executeQuery("PRAGMA auto_vacuum")) {
			if (rs.next())
				autoVacuum = rs.getInt(1);
		} catch (SQLException e) {
			System.err.println("Warning: Could not read auto_vacuum pragma: " + e.getMessage());
		}

		return new SqlitePragmaSettings(synchronous, journalMode, cacheSize, pageSize, tempStore, lockingMode, mmapSize, autoVacuum);
	}



	private void restoreOriginalPragmaSettings(Connection conn) throws SQLException {

		if (originalPragmaSettings == null)
			return;

		try (Statement stmt = conn.createStatement()) {
			// Restore all original settings
			stmt.execute("PRAGMA synchronous = " + originalPragmaSettings.synchronous());
			stmt.execute("PRAGMA journal_mode = " + originalPragmaSettings.journalMode());
			stmt.execute("PRAGMA cache_size = " + originalPragmaSettings.cacheSize());
			stmt.execute("PRAGMA page_size = " + originalPragmaSettings.pageSize());
			stmt.execute("PRAGMA temp_store = " + originalPragmaSettings.tempStore());
			stmt.execute("PRAGMA locking_mode = " + originalPragmaSettings.lockingMode());
			stmt.execute("PRAGMA mmap_size = " + originalPragmaSettings.mmapSize());
			stmt.execute("PRAGMA auto_vacuum = " + originalPragmaSettings.autoVacuum());

			// Additional cleanup for WAL mode
			if ("WAL".equalsIgnoreCase(originalPragmaSettings.journalMode())) {
				stmt.execute("PRAGMA wal_checkpoint(TRUNCATE)");
				stmt.execute("PRAGMA journal_size_limit = 0"); // Reset any limit
			}
		}
	}



	// **************************************************************************************************************************************************
	// ****************************************************************** Overrrides ********************************************************************
	// **************************************************************************************************************************************************
	@Override
	public void close() {
		try {
			if (targetConnection != null && !targetConnection.isClosed())
				restoreOriginalPragmaSettings(targetConnection);

		} catch (SQLException e) {
			System.err.println("Error restoring pragma settings: " + e.getMessage());
		} finally {
			closeQuietly(sourceConnection);
			closeQuietly(targetConnection);
		}
	}

}

