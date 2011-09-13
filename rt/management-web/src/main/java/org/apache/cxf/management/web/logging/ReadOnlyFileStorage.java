/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cxf.management.web.logging;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.cxf.common.util.SystemPropertyAction;
import org.apache.cxf.jaxrs.ext.search.SearchCondition;

/**
 * Facilitates reading the log entries from the existing log files.
 */
public class ReadOnlyFileStorage implements ReadableLogStorage {
    
    public static final String LEVEL_PROPERTY = "level";
    public static final String DATE_PROPERTY = "date";
    public static final String MESSAGE_PROPERTY = "message";
    public static final String CATEGORY_PROPERTY = "category";
    public static final String THREAD_PROPERTY = "thread";
    
    public static final String DATE_ONLY_FORMAT = "yyyy-MM-dd";
        
    private static final String LINE_SEP = SystemPropertyAction.getProperty("line.separator"); 
    private static final String DEFAULT_COLUMN_SEP = "|";
    
    private String columnSep = DEFAULT_COLUMN_SEP;
    private int numberOfColumns;
    private boolean startsFromSeparator;
    private boolean endsWithSeparator;
    
    private SimpleDateFormat recordDateFormat;
    private boolean useFileModifiedDate;
    private Pattern fileNameDatePattern;
    private String fileNameDateFormat;
    
    private File logDirectory; 
    private Comparator<String> fileNameComparator;
    
    private Map<Integer, String> columnsMap;
    private List<FileInfo> logFiles = new LinkedList<FileInfo>();  
    private Map<String, String> levelsMap;
    private Map<Integer, PageInfo> pagesMap = new HashMap<Integer, PageInfo>();
    
    /**
     * {@inheritDoc}
     */
    public int getSize() {
        return -1;
    }

    /**
     * {@inheritDoc}
     */
    // Synchronization can not be avoided at the moment as
    // the file position is continuously changed.
    // Realistically, the log files will probably be viewed
    // by the admin so it's not a problem. However the memory mapping can
    // help in making it more flexible
    public synchronized int load(List<LogRecord> list, 
                     SearchCondition<LogRecord> condition, 
                     int pageNumber,
                     int pageSize) {
        FileInfo logFileInfo = getLogFileInfo(pageNumber);
        if (logFileInfo == null) {
            return pageNumber;
        }
        
        int recordCount = 0;
        int currentIndex = 0;
        while (true) {
            LogRecord record = readRecord(logFileInfo);
            if (record == null) {
                logFileInfo = getNextLogFileInfo(logFileInfo, true);
                if (logFileInfo != null) {
                    continue;
                } else {
                    return pageNumber;
                }
            }
            if (condition == null || condition.isMet(record)) {
                list.add(record);
                if (++recordCount == pageSize) {
                    saveNextPagePosition(pageNumber + 1, logFileInfo);
                    break;
                }
            }
            if (++currentIndex == pageSize) {
                pageNumber++;
                recordCount = 0;
                currentIndex = 0;
            }
        }
        return pageNumber;
    }

    /**
     * If no more records is available in the current file then try to get 
     * the next one with an optional scanning 
     **/
    private FileInfo getNextLogFileInfo(FileInfo logFileInfo, boolean firstTry) {
        for (int i = 0; i < logFiles.size(); i++) {
            FileInfo fileInfo = logFiles.get(i);
            if (fileInfo == logFileInfo) {
                if (i + 1 < logFiles.size()) {    
                    return setFilePosition(logFiles.get(i + 1), logFiles.get(i + 1).getStartPosition());
                } else {
                    break;
                }
            }
        }
        if (firstTry && logDirectory != null && scanLogDirectory()) {
            return getNextLogFileInfo(logFileInfo, false);
        }
        return null;
    }

    private FileInfo setFilePosition(FileInfo fileInfo, long pos) {
        try {
            fileInfo.getFile().seek(pos);
            return fileInfo;
        } catch (IOException ex) {
            System.err.println("Problem setting a page position in " + fileInfo.getFileName());
            return null;
        }
    }
    
    /**
     * Gets the file corresponding to the current page.
     */
    private FileInfo getLogFileInfo(int pageNumber) {
        PageInfo pageInfo = pagesMap.get(pageNumber);
        if (pageInfo != null) {
            return setFilePosition(pageInfo.getFileInfo(), pageInfo.getPosition());
        }
        int oldSize = logFiles.size();
        if (logDirectory != null 
            && scanLogDirectory()) {
            FileInfo fileInfo = logFiles.get(oldSize);
            saveNextPagePosition(pageNumber, fileInfo);
            return fileInfo;
        }
        return null;
    }
    
    /**
     * Save the position of the next page 
     */
    private void saveNextPagePosition(int pageNumber, FileInfo fileInfo) {
        try {
            long pos = fileInfo.getFile().getFilePointer();
            if (pos < fileInfo.getFile().length()) {
                pagesMap.put(pageNumber, new PageInfo(fileInfo, pos));
            } else {
                FileInfo nextFileInfo = getNextLogFileInfo(fileInfo, false);
                if (nextFileInfo != null) {
                    pagesMap.put(pageNumber, 
                                 new PageInfo(nextFileInfo, nextFileInfo.getFile().getFilePointer()));
                }
            }
        } catch (IOException ex) {
            // ignore
        }
        
    }
    
    protected LogRecord readRecord(FileInfo logFileInfo) {
        try {
            Map<Integer, String> map = new HashMap<Integer, String>(numberOfColumns);
            readTheLine(logFileInfo.getFile(), map, 1);
            return createRecord(map, logFileInfo);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        
    }
    
    protected LogRecord createRecord(Map<Integer, String> map, FileInfo logFileInfo) {
        if (map.isEmpty()) {
            return null;
        }
        LogRecord record = new LogRecord();
        for (Map.Entry<Integer, String> entry : map.entrySet()) {
            String propertyName = columnsMap.get(entry.getKey());
            if (LEVEL_PROPERTY.equals(propertyName)) {
                setLogRecordLevel(record, entry.getValue());
            } else if (DATE_PROPERTY.equals(propertyName)) {
                setLogRecordDate(record, entry.getValue(), logFileInfo);
            } else if (MESSAGE_PROPERTY.equals(propertyName)) {
                record.setMessage(entry.getValue());
            } else if (CATEGORY_PROPERTY.equals(propertyName)) {
                record.setLoggerName(entry.getValue());
            } else if (THREAD_PROPERTY.equals(propertyName)) {
                record.setThreadName(entry.getValue());
            }
        }
        return record;
    }
    
    protected void setLogRecordLevel(LogRecord record, String logLevel) {
        if (levelsMap != null) {
            logLevel = levelsMap.get(logLevel);
        }
        if (logLevel != null) {
            record.setLevel(LogLevel.valueOf(logLevel));
        }
    }
    
    protected void setLogRecordDate(LogRecord record, String logDate, FileInfo logFileInfo) {
        if (recordDateFormat != null) {
            try {
                String fileModifiedDate = logFileInfo.getFileModified();
                logDate = fileModifiedDate != null ? fileModifiedDate + " " + logDate : logDate;
                Date date = recordDateFormat.parse(logDate);
                record.setDate(date);
            } catch (Exception ex) {
                // ignore
            }
        }
    }
    
    protected void readTheLine(RandomAccessFile logFile, Map<Integer, String> map, int columnIndex) 
        throws IOException {
        
        long nextPos = logFile.getFilePointer();
        if (nextPos >= logFile.length()) {
            return;
        }
        
        String line = logFile.readLine();
        
        int lastIndex = 0;
        if (columnIndex == 1 && startsFromSeparator) {
            lastIndex = 1;
        }
            
        Set<Integer> requestedColumns = columnsMap.keySet();
        int startingColumn = columnIndex;
        while (lastIndex < line.length()) {
        
            int sepIndex = line.indexOf(columnSep, lastIndex);
            
            if (sepIndex != -1 && startingColumn == numberOfColumns && !endsWithSeparator) {
                logFile.seek(nextPos);
                return;
            }
            
            int actualIndex = sepIndex  == -1 ? line.length() : sepIndex;
            
            if (requestedColumns.contains(columnIndex)) {
                String value = line.substring(lastIndex, actualIndex).trim();
                String existingValue = map.get(columnIndex);
                map.put(columnIndex, existingValue == null 
                                     ? value : existingValue + LINE_SEP + value);
            }
            
            lastIndex = actualIndex + 1;
            if (sepIndex != -1 && columnIndex != numberOfColumns) {
                columnIndex++;
            }
        }
        
        if (columnIndex == numberOfColumns) {
            readTheLine(logFile, map, columnIndex);
        }
    }
    
    /**
     * Log column separator such as '|'
     * @param columnSep the separator
     */
    public void setColumnSep(String columnSep) {
        this.columnSep = columnSep;
    }

    /**
     * Sets the number of columns per record 
     * @param number the number of columns per record
     */
    public void setNumberOfColumns(String number) {
        this.numberOfColumns = Integer.parseInt(number);
    }

    /**
     * Identifies the columns which this reader should use
     * when creating a LogRecord. Example, given a 7-columns
     * record a user may only need the information from 1, 2, 
     * and the last column. Regular expressions are not suitable.
     * 
     * @param columnsMap the map, the key is the column number (starting from 1)
     *        and the value is the name of the property such as 'message'.
     *        The following properties are supported at the moment: 
     *        'date', 'level' 'category', 'thread', 'message'.
     */
    public void setColumnsMap(Map<Integer, String> columnsMap) {
        this.columnsMap = columnsMap;
    }

    /**
     * A list of log files, the oldest files are expected to be in the top
     * of the list
     * @param locations the locations
     */
    public void setLogLocations(List<String> locations) {
        logFiles = new LinkedList<FileInfo>();
        for (int i = 0; i < locations.size(); i++) {
            String realPath = getRealLocation(locations.get(i));
            
            try {
                processNewLogFile(new File(realPath));
            } catch (IOException ex) {
                throw new RuntimeException("The log file " + realPath + " can not be opened: "
                                           + ex.getMessage());
            }
        }
    }
    
    
    /**
     * It may make sense to map logFile.getChannel() to memory for large files
     * >= 1MB
     */
    private void processNewLogFile(File file) throws IOException {
        RandomAccessFile logFile = new RandomAccessFile(file, "r");
        
        String fileModifiedDate = null;
        if (useFileModifiedDate) {
            if (fileNameDatePattern != null) {
                fileModifiedDate = getDateFromFileName(file.getName());
            }
            if (fileModifiedDate == null) {
                Date fileDate = new Date(file.lastModified());
                fileModifiedDate = getLogDateFormat().format(fileDate);
            }
        }
        skipIgnorableRecords(logFile);
        FileInfo fileInfo = new FileInfo(logFile, 
                                         file.getName(), 
                                         fileModifiedDate,
                                         logFile.getFilePointer());
        if (logFiles.size() == 0) {
            pagesMap.put(1, new PageInfo(fileInfo, fileInfo.getStartPosition()));
        }
        logFiles.add(fileInfo);
    }
    
    private String getDateFromFileName(String name) {
        Matcher m = fileNameDatePattern.matcher(name);
        if (m.matches() && m.groupCount() > 0) {
            return m.group(1);
        } else {
            return null;
        }
    }
    
    private SimpleDateFormat getLogDateFormat() {
        String format = fileNameDateFormat == null ? DATE_ONLY_FORMAT : fileNameDateFormat;
        return new SimpleDateFormat(format);
    }
    
    private String getRealLocation(String location) {
        int indexOpen = location.indexOf("{");
        int indexClose = location.indexOf("}");
        String realPath = null;
        if (indexOpen == 0 && indexClose != -1) {
            String property = location.substring(1, indexClose);
            String resolvedPath = SystemPropertyAction.getProperty(property);
            if (resolvedPath == null) {
                throw new IllegalArgumentException("System property " + property + " can not be resolved");
            }
            realPath = resolvedPath + location.substring(indexClose + 1);
            
        } else {
            realPath = location;
        }
        return realPath;
    }
    
    /**
     * Sets the log location. 
     * @param location the location, if it is a directory then 
     *        the on-demand scanning will be enabled
     */
    public void setLogLocation(String location) {
        String realPath = getRealLocation(location);
        File file = new File(realPath);
        if (file.isDirectory()) {
            logDirectory = file;
        } else {
            setLogLocations(Collections.singletonList(realPath));
        }
    }
    
    /**
     * Skip the records at the top of the file which have no column separators 
     */
    private void skipIgnorableRecords(RandomAccessFile file) throws IOException {
        long nextPos = file.getFilePointer();
        if (nextPos == file.length()) {
            return;
        }
        String line = file.readLine();
        if (line.contains(columnSep)) {
            file.seek(nextPos);
        } else {
            skipIgnorableRecords(file);
        }
        
    }
    //CHECKSTYLE:OFF
    /**
     * The format for parsing the log date
     * <p>
     * Please see <a href="http://download.oracle.com/javase/1.5.0/docs/api/java/text/SimpleDateFormat.html">SimpleDateFormat</a>
     * </p>
     */
    //CHECKSTYLE:ON
    public void setRecordDateFormat(String format) {
        recordDateFormat = new SimpleDateFormat(format);
    }
    
    /**
     * Optional map for converting the levels.
     * This map is not required if the log records have levels
     * with one of the following values: 
     * 'WARN', 'ERROR', 'DEBUG', 'TRACE', 'INFO', 'FATAL'.  
     * @param map the map of levels
     */
    public void setLevelsMap(Map<String, String> map) {
        this.levelsMap = map;
    }
    
    /**
     * Optional comparator which can be used for sorting the 
     * new log files found after the latest scan iteration.
     * 
     * If scanning is enabled then by default the file names are compared
     * using either the embedded date (provided the fileNameDatePattern is set)
     * or the last segment in the file name which is expected to be a number.
     * The files with the oldest dates or bigger indexes will be positioned first. 
     *  
     * @param comp the comparator
     */
    public void setFileNameComparator(Comparator<String> comp) {
        this.fileNameComparator = comp;
    }
    
    public void close() {
        for (FileInfo fileInfo : logFiles) {
            try {
                fileInfo.getFile().close();
            } catch (IOException ex) {
                // ignore
            }
        }
    }

    /**
     * Indicates if the file modified date needs to be used for
     * creating a LogRecord date - in case the actual log record
     * contains no year/month/hour information.
     */
    public void setUseFileModifiedDate(boolean useFileModifiedDate) {
        this.useFileModifiedDate = useFileModifiedDate;
    }

    private boolean scanLogDirectory() {
        int oldSize = logFiles.size();
        for (File file : logDirectory.listFiles()) {
            if (file.isDirectory() || file.isHidden()
                || fileNameDatePattern != null 
                   && getDateFromFileName(file.getName()) == null) {
                continue;
            }
            
            boolean isNew = true;
            for (FileInfo fInfo : logFiles) {
                if (fInfo.getFileName().equalsIgnoreCase(file.getName())) {
                    isNew = false;
                    break;
                }
            }
            if (isNew) {
                try {
                    processNewLogFile(file);
                } catch (IOException ex) {
                    System.out.println("Log file " + file.getName() + " can not be opened");
                }
            }
        }
        if (logFiles.size() > oldSize) {
            Collections.sort(logFiles, new FileInfoComparator());
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * Sets the regular expression for capturing the date from the file name
     * If set then it must contain a single capturing group only.
     * @param fileNameDatePattern
     */
    public void setFileNameDatePattern(String fileNameDatePattern) {
        this.fileNameDatePattern = Pattern.compile(fileNameDatePattern);
    }

    /**
     * Optional pattern for parsing the file date
     * @param fileNameDateFormat
     */
    public void setFileNameDateFormat(String fileNameDateFormat) {
        this.fileNameDateFormat = fileNameDateFormat;
    }

    protected static class PageInfo {
        private FileInfo fileInfo;
        private long pos;
        public PageInfo(FileInfo fileInfo, long pos) {
            this.fileInfo = fileInfo;
            this.pos = pos;
        }
        
        public FileInfo getFileInfo() {
            return fileInfo;
        }
        public long getPosition() {
            return pos;
        }
    }
    
    protected static class FileInfo {
        private RandomAccessFile file;
        private String fileModified;
        private String fileName;
        private long startPosition;
        
        public FileInfo(RandomAccessFile file, String fileName, String fileModified, long startPos) {
            this.file = file;
            this.fileModified = fileModified;
            this.fileName = fileName;
            this.startPosition = startPos;
        }
        
        public RandomAccessFile getFile() {
            return file;
        }
        public String getFileModified() {
            return fileModified;
        }
        public String getFileName() {
            return fileName;
        }
        public long getStartPosition() {
            return startPosition;
        }
    }
 
    protected class FileInfoComparator implements Comparator<FileInfo> {

        public int compare(FileInfo info1, FileInfo info2) {
            
            if (useFileModifiedDate && fileNameDatePattern != null) {
                SimpleDateFormat dateFormat = getLogDateFormat();
                try {
                    Date date1 = dateFormat.parse(info1.getFileModified());
                    Date date2 = dateFormat.parse(info2.getFileModified());
                    return date1.compareTo(date2);
                } catch (Exception ex) {
                    // continue
                }
            }
            
            String name1 = info1.getFileName();
            String name2 = info2.getFileName();
            if (fileNameComparator != null) {
                return fileNameComparator.compare(name1, name2);
            }
            Integer logIndex1 = getLogIndex(name1);
            Integer logIndex2 = getLogIndex(name2);
            return logIndex1.compareTo(logIndex2) * -1;
        } 
        
        private int getLogIndex(String name) {
            int index = name.lastIndexOf('.');
            try {
                return Integer.valueOf(name.substring(index + 1));
            } catch (Exception ex) {
                return 0;
            }    
        }
    }
}
