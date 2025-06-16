package quitersstorssguard.records;

import java.util.Arrays;
import java.util.Objects;

public record Store_QuiteRssFeed(
		int order,
		int parentId,
		int id,
		String title,
		String description,
		long created,
		String feedURL,
		byte[] icon,
		int disableUpdate,
		int layoutDirection,
		int addSingleNewsAnyDateOn
		) {
	// Compact constructor for validation/defaults
	public Store_QuiteRssFeed {
		title = Objects.requireNonNullElse(title, "");
		description = Objects.requireNonNullElse(description, "");
		feedURL = Objects.requireNonNullElse(feedURL, "");
		icon = icon != null ? icon : new byte[0];
	}


	// Default constructor
	public Store_QuiteRssFeed() {
		this(-1, -1, -1, "", "", 119731017, "", new byte[0], -1, -1, -1);
	}


	// Derived property
	public boolean isCategory() {
		return feedURL.isBlank();
	}


	// Print method
	public void print() {
		System.out.printf("""
				Is Category: %b
				Order      : %d
				Parent Id  : %d
				Id        : %d
				Title     : %s
				Description: %s
				Created   : %d
				Source    : %s
				Icon      : %s
				Disable Update: %d
				Is RTL    : %d
				Add Single News Any Date On: %d
				---------------------------------------------------------
				%n""",
				isCategory(), order, parentId, id, title, description,
				created, feedURL, Arrays.toString(icon), disableUpdate,
				layoutDirection, addSingleNewsAnyDateOn);
	}


	// Custom equals/hashCode to handle byte[] comparison
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Store_QuiteRssFeed that = (Store_QuiteRssFeed) o;
		return order == that.order &&
				parentId == that.parentId &&
				id == that.id &&
				created == that.created &&
				disableUpdate == that.disableUpdate &&
				layoutDirection == that.layoutDirection &&
				addSingleNewsAnyDateOn == that.addSingleNewsAnyDateOn &&
				title.equals(that.title) &&
				description.equals(that.description) &&
				feedURL.equals(that.feedURL) &&
				Arrays.equals(icon, that.icon);
	}


	@Override
	public int hashCode() {
		int result = Objects.hash(order, parentId, id, title, description, created, feedURL, disableUpdate, layoutDirection, addSingleNewsAnyDateOn);
		result = 31 * result + Arrays.hashCode(icon);
		return result;
	}


}

