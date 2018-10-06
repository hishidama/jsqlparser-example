package jp.hishidama.asakusafw.ddl2dmdl;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import jp.hishidama.jsqlparser.ddl.DdlCollector;
import jp.hishidama.jsqlparser.ddl.DdlConvertExecutor;
import jp.hishidama.jsqlparser.ddl.DdlConverter;
import net.sf.jsqlparser.parser.ParseException;

public class DmdlGenerator extends DdlConvertExecutor {
	private final Path srcDir;
	private final Path dstDir;

	public DmdlGenerator(Path srcDir, Path dstDir) {
		this.srcDir = srcDir;
		this.dstDir = dstDir;
	}

	@Override
	protected DdlCollector collect() throws IOException {
		DdlCollector collector = new DdlCollector();

		try (Stream<Path> stream = Files.walk(srcDir)) {
			stream.filter(this::filter).forEach(path -> {
				System.out.println(path);

				Charset cs = getCharset(path);
				String group = path.toString();
				try (Reader reader = Files.newBufferedReader(path, cs)) {
					collect(reader, group, collector);
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				} catch (ParseException e) {
					e.printStackTrace();
					System.err.println(path + " skipped");
				}
			});
		}

		return collector;
	}

	protected boolean filter(Path path) {
		return !Files.isDirectory(path);
	}

	protected Charset getCharset(Path path) {
		return StandardCharsets.UTF_8;
	}

	@Override
	protected DdlConverter createDdlConverter() {
		return new DmdlWriter(srcDir, dstDir);
	}

	public static void main(String... args) throws IOException {
		if (args.length != 2) {
			System.err.println("usage: srcDir dstDir");
			System.exit(1);
		}

		Path src = Paths.get(args[0]);
		Path dst = Paths.get(args[1]);
		new DmdlGenerator(src, dst).execute();
	}
}
