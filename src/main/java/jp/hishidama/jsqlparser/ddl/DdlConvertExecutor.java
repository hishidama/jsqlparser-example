package jp.hishidama.jsqlparser.ddl;

import java.io.IOException;
import java.io.Reader;

import net.sf.jsqlparser.parser.CCJSqlParser;
import net.sf.jsqlparser.parser.ParseException;
import net.sf.jsqlparser.parser.StreamProvider;
import net.sf.jsqlparser.statement.StatementVisitorAdapter;
import net.sf.jsqlparser.statement.Statements;
import net.sf.jsqlparser.statement.comment.Comment;
import net.sf.jsqlparser.statement.create.table.CreateTable;

public abstract class DdlConvertExecutor {

	public void execute() throws IOException {
		System.out.println("----collect start----");
		DdlCollector collector = collect();

		System.out.println("----convert start----");
		DdlConverter converter = createDdlConverter();
		converter.convert(collector);

		System.out.println("----convert end----");
	}

	protected abstract DdlCollector collect() throws IOException;

	protected abstract DdlConverter createDdlConverter();

	protected void collect(Reader reader, String group, DdlCollector collector) throws ParseException {
		CCJSqlParser parser = new CCJSqlParser(new StreamProvider(reader));
		Statements ss = parser.Statements();

		DdlCounter counter = new DdlCounter();
		collector.setCounter(counter);

		DdlVisitor visitor = createDdlVisitor(group, collector);
		ss.accept(visitor);

		System.out.printf("\ttable=%d, comment=%d+%d\n", counter.getTableCount(), counter.getCommentTableCount(),
				counter.getCommentColumnCount());
	}

	protected DdlVisitor createDdlVisitor(String group, DdlCollector collector) {
		return new DdlVisitor(group, collector);
	}

	protected static class DdlVisitor extends StatementVisitorAdapter {
		private final String group;
		private final DdlCollector collector;

		public DdlVisitor(String group, DdlCollector collector) {
			this.group = group;
			this.collector = collector;
		}

		// https://github.com/JSQLParser/JSqlParser/pull/685
		@Override
		public void visit(Comment comment) {
			collector.addComment(group, comment);
		}

		@Override
		public void visit(CreateTable createTable) {
			collector.addCreateTable(group, createTable);
		}
	}
}
