package jp.hishidama.asakusafw.ddl2dmdl;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import jp.hishidama.jsqlparser.ddl.DdlCollector;
import jp.hishidama.jsqlparser.ddl.DdlConverter;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.create.table.ColDataType;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.create.table.CreateTable;

public class DmdlWriter implements DdlConverter {

	private final Path srcDir;
	private final Path dstDir;
	private DdlCollector collector;

	public DmdlWriter(Path srcDir, Path dstDir) {
		this.srcDir = srcDir;
		this.dstDir = dstDir;
	}

	@Override
	public void convert(DdlCollector collector) throws IOException {
		this.collector = collector;

		Map<String, List<CreateTable>> map = collector.getCreateTableMap();
		for (Entry<String, List<CreateTable>> entry : map.entrySet()) {
			Path dst = getDstPath(entry.getKey());
			convert(dst, entry.getValue());
		}
	}

	protected Path getDstPath(String group) {
		Path src = Paths.get(group + ".dmdl");
		return dstDir.resolve(srcDir.relativize(src));
	}

	private void convert(Path dst, List<CreateTable> list) throws IOException {
		System.out.println(dst);

		Files.createDirectories(dst.getParent());
		try (BufferedWriter writer = Files.newBufferedWriter(dst)) {
			Converter converter = createConverter(writer);
			converter.convert(list);
		}
	}

	protected Converter createConverter(BufferedWriter writer) {
		return new Converter(writer);
	}

	protected class Converter {
		private final BufferedWriter writer;

		public Converter(BufferedWriter writer) {
			this.writer = writer;
		}

		public void convert(List<CreateTable> list) throws IOException {
			boolean first = true;
			for (CreateTable create : list) {
				if (first) {
					first = false;
				} else {
					writeln("");
				}

				initializeSpec(create);

				Table table = create.getTable();
				writeHeader(table);

				List<ColumnDefinition> columnList = create.getColumnDefinitions();
				if (columnList != null) {
					for (ColumnDefinition column : columnList) {
						writeProperty(table, column);
					}
				}

				writeFooter();
			}
		}

		protected final Map<String, String> specMap = new HashMap<>();

		@SuppressWarnings("unchecked")
		protected void initializeSpec(CreateTable create) {
			specMap.clear();

			for (ColumnDefinition column : create.getColumnDefinitions()) {
				List<String> spec = column.getColumnSpecStrings();
				if (spec != null) {
					String s = spec.stream().collect(Collectors.joining(" ")).toUpperCase();
					String name = column.getColumnName();
					specMap.put(name.toUpperCase(), s);
				}
			}

			List<String> list = getPrimaryKey((List<String>) create.getTableOptionsStrings());
			for (String name : list) {
				String key = name.toUpperCase();
				String s = specMap.get(key);
				if (s == null) {
					specMap.put(key, "PRIMARY KEY");
				} else {
					if (!s.contains("PRIMARY KEY")) {
						s += " " + "PRIMARY KEY";
						specMap.put(key, s);
					}
				}
			}
		}

		protected List<String> getPrimaryKey(List<String> optionList) {
			if (optionList == null) {
				return Collections.emptyList();
			}

			List<String> result = new ArrayList<>();
			for (int i = 0; i < optionList.size(); i++) {
				String arg = optionList.get(i);
				if (arg.equalsIgnoreCase("primary") && i + 2 < optionList.size()) {
					String arg1 = optionList.get(i + 1);
					if (arg1.equalsIgnoreCase("key")) {
						String arg2 = optionList.get(i + 2);
						int n = arg2.indexOf('(');
						if (n < 0) {
							n = 0;
						} else {
							n++;
						}
						int m = arg2.lastIndexOf(')');
						if (m < 0) {
							m = arg2.length();
						}
						String s = arg2.substring(n, m);
						String[] ss = s.split(",");
						for (String name : ss) {
							result.add(name.trim());
						}
					}
				}
			}
			return result;
		}

		protected String getSpec(String columnName) {
			return specMap.get(columnName.toUpperCase());
		}

		protected void writeHeader(Table table) throws IOException {
			String tableName = table.getName();
			String tableDesc = getDescription(table);
			writeln("\"%s\"", tableDesc);
			writeln("@windgate.jdbc.table(name = \"%s\")", tableName.toUpperCase());
			writeln("%s = {", tableName.toLowerCase());
		}

		protected void writeProperty(Table table, ColumnDefinition column) throws IOException {
			String tab = "    ";

			writeln("");
			String spec = getSpec(column.getColumnName());
			if (spec != null) {
				writeln(tab + "// %s", spec);
			}
			String name = column.getColumnName();
			String desc = getDescription(table, column);
			String type = getDataType(table, column);
			writeln(tab + "\"%s\"", desc);
			writeln(tab + "@windgate.jdbc.column(name = \"%s\")", name.toUpperCase());
			writeln(tab + "%s : %s; // %s", name.toLowerCase(), type, column.getColDataType());
		}

		protected void writeFooter() throws IOException {
			writeln("};");
		}

		protected void writeln(String format, Object... args) throws IOException {
			writer.write(String.format(format + "\n", args));
		}
	}

	protected String getDescription(Table table) {
		Map<String, String> map = collector.getCommentTableMap();

		String name = table.getName();
		String desc = map.get(name.toUpperCase());
		if (desc != null) {
			return desc;
		}
		return name;
	}

	protected String getDescription(Table table, ColumnDefinition column) {
		Map<String, String> map = collector.getCommentColumnMap();

		String name = column.getColumnName();
		String key = (table.getName() + "." + name).toUpperCase();
		String desc = map.get(key);
		if (desc != null) {
			return desc;
		}
		return name;
	}

	protected String getDataType(Table table, ColumnDefinition column) {
		ColDataType type = column.getColDataType();
		switch (type.getDataType().toUpperCase()) {
		case "CHAR":
		case "VARCHAR":
		case "VARCHAR2":
			return "TEXT";
		case "NUMBER":
			return getDataTypeNumber(table, column);
		case "DATE":
			return getDataTypeDate(table, column);
		case "TIMESTAMP":
			return getDataTypeTimestamp(table, column);
		default:
			return "/*FIXME " + type + " */";
		}
	}

	protected String getDataTypeNumber(Table table, ColumnDefinition column) {
		ColDataType type = column.getColDataType();
		try {
			List<String> args = type.getArgumentsStringList();
			if (args == null) {
				return "DECIMAL";
			}
			if (args.size() >= 2) {
				int scale = Integer.parseInt(args.get(1));
				if (scale != 0) {
					return "DECIMAL";
				}
			}
			if (args.size() >= 1) {
				String s = args.get(0);
				if (s.equals("*")) {
					return "DECIMAL";
				}
				int precision = Integer.parseInt(s);
				if (precision <= 9) {
					return "INT";
				}
				if (precision <= 18) {
					return "LONG";
				}
			}

			return "DECIMAL";
		} catch (NumberFormatException e) {
			return "DECIMAL /* FIXME " + type + " */";
		}
	}

	protected String getDataTypeDate(Table table, ColumnDefinition column) {
		// RDBのDATEはYMDhms
		// AsakusaFWのDATEはYMD
		String s = column.getColumnName().toLowerCase();
		if (s.endsWith("_date")) {
			return "DATE";
		}
		return "DATETIME";
	}

	protected String getDataTypeTimestamp(Table table, ColumnDefinition column) {
		return "DATETIME";
	}
}
