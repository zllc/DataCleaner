/**
 * DataCleaner (community edition)
 * Copyright (C) 2014 Neopost - Customer Information Management
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.datacleaner.output.csv;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.datacleaner.api.InputColumn;
import org.datacleaner.output.OutputWriter;
import org.apache.metamodel.UpdateCallback;
import org.apache.metamodel.UpdateScript;
import org.apache.metamodel.UpdateableDataContext;
import org.apache.metamodel.create.TableCreationBuilder;
import org.apache.metamodel.csv.CsvConfiguration;
import org.apache.metamodel.csv.CsvDataContext;
import org.apache.metamodel.schema.Schema;
import org.apache.metamodel.schema.Table;
import org.apache.metamodel.util.FileHelper;
import org.apache.metamodel.util.FileResource;
import org.apache.metamodel.util.Resource;

public final class CsvOutputWriterFactory {

    private static final Map<String, AtomicInteger> counters = new HashMap<String, AtomicInteger>();
    private static final Map<String, UpdateableDataContext> dataContexts = new HashMap<String, UpdateableDataContext>();

    /**
     * Creates a CSV output writer with default configuration
     * 
     * @param filename
     * @param columns
     * @return
     */
    public static OutputWriter getWriter(String filename, List<InputColumn<?>> columns) {
        final InputColumn<?>[] columnArray = columns.toArray(new InputColumn<?>[columns.size()]);
        final String[] headers = new String[columnArray.length];
        for (int i = 0; i < headers.length; i++) {
            headers[i] = columnArray[i].getName();
        }
        return getWriter(filename, headers, ',', '"', '\\', true, columnArray);
    }

    /**
     * Creates a CSV output writer
     * 
     * @param filename
     * @param headers
     * @param separatorChar
     * @param quoteChar
     * @param escapeChar
     * @param includeHeader
     * @param columns
     * @return
     */
    public static OutputWriter getWriter(String filename, final String[] headers, char separatorChar, char quoteChar,
            char escapeChar, boolean includeHeader, final InputColumn<?>... columns) {
        return getWriter(new FileResource(filename), headers, FileHelper.DEFAULT_ENCODING, separatorChar, quoteChar,
                escapeChar, includeHeader, columns);
    }

    public static OutputWriter getWriter(Resource resource, final String[] headers, String encoding,
            char separatorChar, char quoteChar, char escapeChar, boolean includeHeader, final InputColumn<?>... columns) {
        final CsvOutputWriter outputWriter;
        final String qualifiedPath = resource.getQualifiedPath();
        synchronized (dataContexts) {
            UpdateableDataContext dataContext = dataContexts.get(qualifiedPath);
            if (dataContext == null) {

                if (resource instanceof FileResource) {
                    final File file = ((FileResource) resource).getFile();
                    final File parentFile = file.getParentFile();
                    if (parentFile != null && !parentFile.exists()) {
                        parentFile.mkdirs();
                    }
                }

                dataContext = new CsvDataContext(resource, getConfiguration(encoding, separatorChar, quoteChar,
                        escapeChar, includeHeader));

                final Schema schema = dataContext.getDefaultSchema();
                dataContext.executeUpdate(new UpdateScript() {
                    @Override
                    public void run(UpdateCallback callback) {
                        TableCreationBuilder tableBuilder = callback.createTable(schema, "table");
                        for (String header : headers) {
                            tableBuilder.withColumn(header);
                        }
                        tableBuilder.execute();
                    }
                });

                final Table table = dataContext.getDefaultSchema().getTables()[0];

                dataContexts.put(qualifiedPath, dataContext);
                counters.put(qualifiedPath, new AtomicInteger(1));
                outputWriter = new CsvOutputWriter(dataContext, qualifiedPath, table, columns);

                // write the headers
            } else {
                Table table = dataContext.getDefaultSchema().getTables()[0];
                outputWriter = new CsvOutputWriter(dataContext, qualifiedPath, table, columns);
                counters.get(qualifiedPath).incrementAndGet();
            }
        }

        return outputWriter;
    }

    private static CsvConfiguration getConfiguration(String encoding, char separatorChar, char quoteChar,
            char escapeChar, boolean includeHeader) {
        final int headerLine;
        if (includeHeader) {
            headerLine = CsvConfiguration.DEFAULT_COLUMN_NAME_LINE;
        } else {
            headerLine = CsvConfiguration.NO_COLUMN_NAME_LINE;
        }
        return new CsvConfiguration(headerLine, encoding, separatorChar, quoteChar, escapeChar);
    }

    protected static void release(String filename) {
        int count = counters.get(filename).decrementAndGet();
        if (count == 0) {
            synchronized (dataContexts) {
                dataContexts.remove(filename);
            }
        }
    }

}
