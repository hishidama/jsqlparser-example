package jp.hishidama.jsqlparser.ddl;

public class DdlCounter {

	private int table = 0;
	private int commentTable = 0;
	private int commentColumn = 0;

	public void addTableCount(int add) {
		table += add;
	}

	public int getTableCount() {
		return table;
	}

	public void addCommentTableCount(int add) {
		commentTable += add;
	}

	public int getCommentTableCount() {
		return commentTable;
	}

	public void addCommentColumnCount(int add) {
		commentColumn += add;
	}

	public int getCommentColumnCount() {
		return commentColumn;
	}
}
