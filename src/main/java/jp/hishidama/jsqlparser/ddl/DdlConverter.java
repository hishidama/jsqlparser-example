package jp.hishidama.jsqlparser.ddl;

import java.io.IOException;

public interface DdlConverter {

	public void convert(DdlCollector collector) throws IOException;
}
