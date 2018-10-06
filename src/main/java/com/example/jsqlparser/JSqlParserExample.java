package com.example.jsqlparser;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParser;
import net.sf.jsqlparser.parser.CCJSqlParserDefaultVisitor;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.parser.CCJSqlParserVisitor;
import net.sf.jsqlparser.parser.Node;
import net.sf.jsqlparser.parser.ParseException;
import net.sf.jsqlparser.parser.SimpleNode;
import net.sf.jsqlparser.parser.StringProvider;
import net.sf.jsqlparser.parser.Token;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.Statements;

public class JSqlParserExample {

	public static void main(String[] args) throws Exception {
		// singleStatement();
		// multiStatement();
		// multiStatement2();
		// node();
		// node0();
		specialToken();
	}

	static void singleStatement() throws JSQLParserException {
		String sql = "select * from dual d;";
		Statement s = CCJSqlParserUtil.parse(sql);
		System.out.println(s);
	}

	static void multiStatement() throws ParseException {
		String sql = "select * from dual;\nselect * from t";
		CCJSqlParser parser = new CCJSqlParser(new StringProvider(sql));
		Statements r = parser.Statements();
		System.out.println(r);
	}

	static void multiStatement2() throws JSQLParserException {
		String sql = "select * from dual;\nselect * from t";
		Statements r = CCJSqlParserUtil.parseStatements(sql);
		System.out.println(r);
	}

	static void node() throws JSQLParserException {
		String sql = "--test\n" //
				+ "select a, --aaa\n" //
				+ "b, --bbb\n" //
				+ "c --ccc\n" //
				+ "from dual as d --ddd\n" //
				+ "; --eee";
		Node node = CCJSqlParserUtil.parseAST(sql);
		// System.out.println(node);
		// System.out.println(node.getClass());
		// SimpleNode snode = (SimpleNode) node;
		// System.out.println(snode.jjtGetValue());
		// System.out.println(snode.jjtGetFirstToken());

		CCJSqlParserVisitor visitor = new MyVisitor();
		Context data = new Context(0);
		node.jjtAccept(visitor, data);
	}

	static class Context {
		int dept;

		public Context(int dept) {
			this.dept = dept;
		}
	}

	static class MyVisitor extends CCJSqlParserDefaultVisitor {
		@Override
		public Object visit(SimpleNode node, Object data) {
			Context context = (Context) data;
			System.out.printf("====%d====%n", context.dept);
			System.out.println(node);
			// System.out.println(node.getClass());
			if (node.toString().equals("Table")) {
				System.out.println(node.jjtGetFirstToken());
			}

			Token first = node.jjtGetFirstToken();
			Token last = node.jjtGetLastToken();
			for (Token token = first; token != null; token = token.next) {
				Token s = token.specialToken;
				if (s != null) {
					System.out.println(s);
					// System.out.println(s.hashCode());
					System.out.printf("%d:%d-%d:%d%n", s.beginLine, s.beginColumn, s.endLine, s.endColumn);
				}
				if (token == last) {
					break;
				}
			}

			Object value = node.jjtGetValue();
			if (value != null) {
				System.out.println("value.class\t" + value.getClass().getName());
				System.out.println("value\t" + value);
			}

			return super.visit(node, new Context(context.dept + 1));
		}
	}

	static void node0() throws JSQLParserException {
		String sql = "select * from dual";
		Node node = CCJSqlParserUtil.parseAST(sql);
		SimpleNode s = (SimpleNode) node;
		Token first = s.jjtGetFirstToken();
		Token last = s.jjtGetLastToken();
		System.out.printf("%d:%d-%d:%d%n", first.beginLine, first.beginColumn, last.endLine, last.endColumn);
	}

	static void specialToken() throws JSQLParserException {
		String sql = "select a, --aaa\n" //
				+ "b --bbb\n" //
				+ "from dual";
		SimpleNode node = (SimpleNode) CCJSqlParserUtil.parseAST(sql);

		for (Token token = node.jjtGetFirstToken(); token != null; token = token.next) {
			Token s = token.specialToken;
			if (s != null) {
				print(s);
			}
			print(token);
		}
	}

	private static void print(Token token) {
		System.out.printf("%2d:%2d-%2d:%2d\t%s%n", token.beginLine, token.beginColumn, token.endLine, token.endColumn,
				token.image);
	}
}
