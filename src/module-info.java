module quitersstorssguard {

	// ------------------------------
	// Exports
	// ------------------------------
	exports quitersstorssguard;
	exports quitersstorssguard.records;
	exports quitersstorssguard.utils;

	// ------------------------------
	// Requires transitive
	// ------------------------------
	requires transitive java.desktop;
	requires transitive java.sql;

	// ------------------------------
	// Requires
	// ------------------------------
	requires org.xerial.sqlitejdbc;
	requires java.compiler;

}
