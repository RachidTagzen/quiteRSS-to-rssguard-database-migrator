package quitersstorssguard.records;

import java.util.Objects;

public record Store_QuiteRSS(
		String author_name,
		String description,
		String guid,
		long received,
		int feedId,
		int id,
		int deleted,
		int starred,
		int read,
		String title,
		String link_href
		) {
	// Compact constructor for validation/defaults
	public Store_QuiteRSS {
		author_name = Objects.requireNonNullElse(author_name, "");
		description = Objects.requireNonNullElse(description, "");
		guid = Objects.requireNonNullElse(guid, "");
		title = Objects.requireNonNullElse(title, "");
		link_href = Objects.requireNonNullElse(link_href, "");
		read = read != 0 ? 1 : 0;
	}


	// Default constructor
	public Store_QuiteRSS() {
		this("", "", "", 119731017, -1, -1, -1, -1, -1, "", "");
	}

	// Print method
	public void print() {
		System.out.printf("""
				Feed Id    : %d
				Id         : %d
				Title      : %s
				Description: %s
				Author     : %s
				GUID       : %s
				Received   : %d
				Deleted    : %d
				Starred    : %d
				Read       : %d
				Source     : %s
				---------------------------------------------------------
				%n""",
				feedId, id, title, description, author_name, guid,
				received, deleted, starred, read, link_href);
	}
}


