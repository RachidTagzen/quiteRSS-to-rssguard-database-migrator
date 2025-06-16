package quitersstorssguard.records;

import java.util.Objects;

public record Store_RSSGuard(
		String author,
		String contents,
		String custom_id,
		String date_created,
		int feed,
		int id,
		int is_deleted,
		int is_pdeleted,
		int is_important,
		int is_read,
		String title,
		String url
		) {
	// Compact constructor for defaults
	public Store_RSSGuard {
		author = Objects.requireNonNullElse(author, "");
		contents = Objects.requireNonNullElse(contents, "");
		custom_id = Objects.requireNonNullElse(custom_id, "");
		date_created = Objects.requireNonNullElse(date_created, "");
		title = Objects.requireNonNullElse(title, "");
		url = Objects.requireNonNullElse(url, "");
	}


	// Default constructor
	public Store_RSSGuard() {
		this("", "", "", "", -1, -1, -1, -1, -1, -1, "", "");
	}

}