package jp.hishidama.jsqlparser.ddl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.comment.Comment;
import net.sf.jsqlparser.statement.create.table.CreateTable;

public class DdlCollector {

	protected DdlCounter counter = null;
	protected final Map<String, String> commentTableMap = new TreeMap<>();
	protected final Map<String, String> commentColumnMap = new TreeMap<>();
	protected final Map<String, List<CreateTable>> createTableMap = new LinkedHashMap<>();

	public void setCounter(DdlCounter counter) {
		this.counter = counter;
	}

	public DdlCounter getCounter() {
		return counter;
	}

	public Map<String, String> getCommentTableMap() {
		return commentTableMap;
	}

	public Map<String, String> getCommentColumnMap() {
		return commentColumnMap;
	}

	public Map<String, List<CreateTable>> getCreateTableMap() {
		return createTableMap;
	}

	public void addComment(String group, Comment comment) {
		Table table = comment.getTable();
		if (table != null) {
			String name = table.getName().toUpperCase();
			commentTableMap.put(name, comment.getComment().getValue());
			if (counter != null) {
				counter.addCommentTableCount(1);
			}
		}

		Column column = comment.getColumn();
		if (column != null) {
			String name = column.getFullyQualifiedName().toUpperCase();
			commentColumnMap.put(name, comment.getComment().getValue());
			if (counter != null) {
				counter.addCommentColumnCount(1);
			}
		}
	}

	public void addCreateTable(String group, CreateTable createTable) {
		List<CreateTable> list = createTableMap.computeIfAbsent(group, key -> new ArrayList<>());
		list.add(createTable);
		if (counter != null) {
			counter.addTableCount(1);
		}
	}
}
