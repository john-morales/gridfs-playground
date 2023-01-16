package com.jmo.mongo.javadriver.gridfs;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.util.StatusPrinter;
import com.google.common.io.CountingInputStream;
import com.google.common.util.concurrent.RateLimiter;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.MongoCommandException;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;

import java.io.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import java.util.concurrent.atomic.AtomicLong;
import org.bson.BsonString;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

public class GridFS {

  private static final Logger LOG = LoggerFactory.getLogger(GridFS.class);

  private static final int BUFFER_SIZE = 1 << 16;

  private static final String DEFAULT_DATABASE_NAME = "gridfs";
  private static final String DEFAULT_BUCKET_NAME = "bucket";
  private static final int DEFAULT_CHUNK_SIZE_BYTES = 358400;
  private static final int DEFAULT_FILES_CHUNKS = 32;
  private static final int DEFAULT_CHUNKS_CHUNKS = 32;
  private static final long DEFAULT_MAX_BYTES_PER_SECOND = Long.MAX_VALUE;
  private static final long DEFAULT_LOG_INTERVAL_MILLIS = 10000L;
  private static final int DEFAULT_THREADS = 8;

  private static void gridfsIngest(final String[] args) throws InterruptedException {
    if (args.length < 2) {
      LOG.error("Required parameters missing for {}", GridFS.class.getName());
      LOG.error("Usage: "
          + "-Dgridfs.infiniteModeEnabled=false "
          + "-Dgridfs.num.threads={} "
          + "-Dgridfs.database={} "
          + "-Dgridfs.bucket={} "
          + "-Dgridfs.chunksSizeBytes={} "
          + "-Dgridfs.status.logIntervalMS={} "
          + "-Dgridfs.maxBytesPerSecond={} "
          + "-Dgridfs.sharding.enabled=false "
          + "-Dgridfs.sharding.presplit.enabled=false "
          + "-Dgridfs.sharding.presplit.files.chunks={} "
          + "-Dgridfs.sharding.presplit.chunks.chunks={} "
          + "{} [mongoUri] [file1] [file2] ...",
          DEFAULT_THREADS,
          DEFAULT_DATABASE_NAME,
          DEFAULT_BUCKET_NAME,
          DEFAULT_CHUNK_SIZE_BYTES,
          DEFAULT_LOG_INTERVAL_MILLIS,
          DEFAULT_MAX_BYTES_PER_SECOND,
          DEFAULT_FILES_CHUNKS,
          DEFAULT_CHUNKS_CHUNKS,
          GridFS.class.getName());
      System.exit(1);
    }

    final boolean infiniteModeEnabled = Boolean.getBoolean("gridfs.infiniteModeEnabled");
    final String database =
        System.getProperty("gridfs.database", DEFAULT_DATABASE_NAME);
    final String bucket =
        System.getProperty("gridfs.bucket", DEFAULT_BUCKET_NAME);
    final Integer chunkSizeBytes =
        Integer.getInteger("gridfs.chunksSizeBytes",DEFAULT_CHUNK_SIZE_BYTES);
    final Integer threads = Integer.getInteger("gridfs.num.threads", DEFAULT_THREADS);
    final Long statusIntervalMS =
        Long.getLong("gridfs.status.logIntervalMS", DEFAULT_LOG_INTERVAL_MILLIS);
    final double maxBytesPerSecond =
        Long.getLong("gridfs.maxBytesPerSecond", DEFAULT_MAX_BYTES_PER_SECOND);
    final boolean shardingEnabled = Boolean.getBoolean("gridfs.sharding.enabled");
    final boolean shardingPresplit = Boolean.getBoolean("gridfs.sharding.presplit.enabled");
    final int shardingPresplitFilesChunks =
        Integer.getInteger("gridfs.sharding.presplit.files.chunks", DEFAULT_FILES_CHUNKS);
    final int shardingPresplitChunksChunks =
        Integer.getInteger("gridfs.sharding.presplit.chunks.chunks", DEFAULT_CHUNKS_CHUNKS);
    final ExecutorService executorService = Executors.newFixedThreadPool(threads);

    LOG.info("Connecting to client at '{}' with {} thread(s) with collection '{}.{}.*' @ max {} B/s",
        args[0], threads, database, bucket, maxBytesPerSecond);

    final RateLimiter limiter = RateLimiter.create(maxBytesPerSecond);

    final StatusThread statusThread = new StatusThread(statusIntervalMS);
    statusThread.start();

    final String[] fileList = Arrays.copyOfRange(args, 1, args.length);
    try (final MongoClient client = new MongoClient(new MongoClientURI(args[0]))) {
      ensureSharding(client, database, bucket, shardingEnabled,
          shardingPresplit, shardingPresplitFilesChunks, shardingPresplitChunksChunks);

      final MongoDatabase gridfs = client.getDatabase(database);
      final GridFSBucket filesBucket = GridFSBuckets.create(gridfs, bucket);

      do {
        final CountDownLatch latch = new CountDownLatch(fileList.length);

        for (int i = 0; i < fileList.length; i++) {
          final int fileIndex = i;
          final String filename = fileList[i];
          executorService.submit(() -> {
            final long start = System.currentTimeMillis();
            final String fileId = UUID.randomUUID().toString();
            final File file = new File(filename);
            final long fileLength = file.length();
            LOG.info("Saving file {}/{}: '{}', {} bytes",
                fileIndex + 1, fileList.length, filename,
                String.format("%,.0f", (double) fileLength));

            try (final CountingInputStream uploadStream = new CountingInputStream(
                new RateLimitedStream(limiter,
                    new BufferedInputStream(new FileInputStream(filename), BUFFER_SIZE)))) {

              statusThread.add(fileId, file, uploadStream);

              // Create some custom options
              GridFSUploadOptions options = new GridFSUploadOptions()
                  .chunkSizeBytes(chunkSizeBytes)
                  .metadata(new Document()
                      .append("type", "iso")
                      .append("lastModified", new Date(file.lastModified())));

              filesBucket.uploadFromStream(
                  new BsonString(fileId), file.getName(), uploadStream, options);

              final long durationMillis = System.currentTimeMillis() - start;
              final double megabytesPerSecond =
                  fileLength / ((double) durationMillis / 1000.0) / 1e6;
              LOG.info("Saved filename {} fileId {} size {}, after {} @ {} MB/s",
                  file.getName(), fileId, fileLength,
                  Duration.ofMillis(durationMillis), String.format("%.1f", megabytesPerSecond));
            } catch (FileNotFoundException pE) {
              LOG.error("File '{}' wasn't found. Err: {}", filename, pE.getMessage());
            } catch (Throwable pE) {
              LOG.error("Encountered failure", pE);
            } finally {
              latch.countDown();
              statusThread.remove(fileId);
            }
          });
        }
        
        LOG.info("Awaiting copy to complete for {} files", fileList.length);
        latch.await();
      } while (infiniteModeEnabled);
    } finally {
      executorService.shutdownNow();

      final long secondsElapsed = statusThread.getTotalMillisElapsed() / 1000L;
      final long minutesElapsed = secondsElapsed / 60;
      LOG.info("Overall performance {} MB/s, {} bytes read, over {}min {}sec",
          String.format("%.1f", statusThread.getTotalCumulativeRatePerSecond() / 1e6),
          String.format("%,.0f", (double)statusThread.getTotalCumulativeCount()),
          minutesElapsed,
          secondsElapsed % 60);
      LOG.info("Exiting");
    }
  }

  private static void ensureSharding(
      final MongoClient pClient,
      final String pDatabase,
      final String pBucket,
      final boolean pShardingEnabled,
      final boolean pShardingPresplit,
      final int pShardingPresplitFilesChunks,
      final int pShardingPresplitChunksChunks) {
    if (!pShardingEnabled) {
      return;
    }

    final String filesNamespace = String.format("%s.%s.files", pDatabase, pBucket);
    final String chunksNamespace = String.format("%s.%s.chunks", pDatabase, pBucket);
    LOG.info("Enabling sharding for '{}', '{}'", filesNamespace, chunksNamespace);
    
    final MongoDatabase admin = pClient.getDatabase("admin");
    admin.runCommand(new Document("enableSharding", pDatabase));

    admin.runCommand(new Document()
        .append("shardCollection", filesNamespace)
        .append("key", new Document()
            .append("_id", 1))
        .append("unique", true));

    admin.runCommand(new Document()
        .append("shardCollection", chunksNamespace)
        .append("key", new Document()
            .append("files_id", 1)
            .append("n", 1))
        .append("unique", true));

    if (!pShardingPresplit) {
      return;
    }

    LOG.info("Pre-splitting '{}' with {} chunks", filesNamespace, pShardingPresplitFilesChunks);
    for (final String uuidBoundary : getUUIDBuckets(pShardingPresplitFilesChunks)) {
      retryingSplitAt(admin, filesNamespace, new Document("_id", uuidBoundary));
    }

    LOG.info("Pre-splitting '{}' with {} chunks", chunksNamespace, pShardingPresplitChunksChunks);
    for (final String uuidBoundary : getUUIDBuckets(pShardingPresplitChunksChunks)) {
      final Document middle = new Document()
          .append("files_id", uuidBoundary)
          .append("n", 0);
      retryingSplitAt(admin, chunksNamespace, middle);
    }
  }

  static void retryingSplitAt(final MongoDatabase pAdmin, final String pNamespace, final Document pMiddle) {
    while (true) {
      try {
        final Document response = pAdmin.runCommand(new Document()
            .append("split", pNamespace)
            .append("middle", pMiddle));
        LOG.info("{} boundary {} response: {}", pNamespace, pMiddle, response);
        return;
      } catch (MongoCommandException pE) {
        if (!"LockBusy".equalsIgnoreCase(pE.getErrorCodeName())) {
          throw pE;
        }

        LOG.warn("Waiting on LockBusy response. Sleeping for 1s before trying again...");
      }

      try {
        Thread.sleep(1000);
      } catch (InterruptedException pE) {
        throw new IllegalStateException(pE);
      }
    }
  }

  static List<String> getUUIDBuckets(final int pChunks) {
    final List<String> uuids = new ArrayList<>(pChunks);

    final long increment = (long)Math.floor(Math.pow(2,32) / pChunks);
    for (long i = 0; i < pChunks; i++) {
      uuids.add(String.format("%08x", i*increment) + "-0000-0000-0000-000000000000");
    }

    return uuids;
  }

  static class StatusThread extends Thread {
    private static final Logger LOG = LoggerFactory.getLogger(StatusThread.class);

    private final ConcurrentHashMap<String, StatusEntry> _actives;
    private final long _logIntervalMillis;
    private final AtomicLong _totalByteCount;
    private final long _createdMillisEpoch;

    StatusThread(final long logIntervalMillis) {
      super("StatusThread");
      setDaemon(true);
      setUncaughtExceptionHandler((t, e) -> {
        LOG.error("Uncaught exception", e);
      });
      
      _actives = new ConcurrentHashMap<>();
      _logIntervalMillis = logIntervalMillis;
      _totalByteCount = new AtomicLong(0L);
      _createdMillisEpoch = System.currentTimeMillis();
    }

    void add(final String pUUID, final File pFile, final CountingInputStream pStream) {
      _actives.put(pUUID, new StatusEntry(pUUID, pFile.getName(), pFile.length(), pStream));
    }

    void remove(final String pUUID) {
      final StatusEntry status = _actives.remove(pUUID);
      LOG.info("Removing status entry {}", pUUID);
      log(System.currentTimeMillis(), status);

      _totalByteCount.addAndGet(status.getCount());
    }

    long getTotalCumulativeCount() {
      return _totalByteCount.get();
    }

    long getTotalMillisElapsed() {
      return System.currentTimeMillis() - _createdMillisEpoch;
    }

    double getTotalCumulativeRatePerSecond() {
      final double totalBytes = _totalByteCount.doubleValue();
      final double totalMillisElapsed = getTotalMillisElapsed();
      return totalBytes / (totalMillisElapsed / 1000.0);
    }

    synchronized void log(final long pNow, final StatusEntry pStatus) {
      final double previousCount = pStatus.getPreviousCount();
      final double cumulativeCount = pStatus.getAndSetCount();

      final double cumulativeElapsedSeconds = (pNow - pStatus.getStartDate().getTime()) / 1000.0;
      final double lastRatePerSecond = (cumulativeCount - previousCount) / ((double)_logIntervalMillis / 1000.0);
      final double cumulativeRatePerSecond = cumulativeCount / cumulativeElapsedSeconds;

      LOG.info("Status: {} {} Last: {} MB/s, Cumulative: {} MB/s, Elapsed: {} s, Completed: {}/{} bytes ({}%)",
          pStatus.getUuid(), pStatus.getFilename(),
          String.format("%.2f", lastRatePerSecond / 1e6),
          String.format("%.1f", cumulativeRatePerSecond / 1e6),
          String.format("%.1f", cumulativeElapsedSeconds),
          String.format("%,.0f", cumulativeCount),
          String.format("%,.0f", (double)pStatus.getFileSize()),
          String.format("%.1f", pStatus.getPercentComplete()));
    }

    @Override
    public void run() {
      LOG.info("Starting status logging interval: {}ms", _logIntervalMillis);
      
      while (!Thread.currentThread().isInterrupted()) {
        try {
          Thread.sleep(_logIntervalMillis);
          final long now = System.currentTimeMillis();

          long totalCount = _totalByteCount.get();

          final List<Entry<String, StatusEntry>> entries = new ArrayList<>(_actives.entrySet());
          entries.sort(Entry.comparingByValue());

          for (final Map.Entry<String, StatusEntry> entry : entries) {
            final StatusEntry status = entry.getValue();
            totalCount += status.getCount();
            log(now, status);
          }

          final double cumulativeElapsedSeconds = (now - _createdMillisEpoch) / 1000.0;
          final double cumulativeRatePerSecond = totalCount / cumulativeElapsedSeconds;
          LOG.info("Cumulative Status: {} kB/s, Elapsed: {} s",
              String.format("%,.1f", cumulativeRatePerSecond / 1e3),
              String.format("%.1f", cumulativeElapsedSeconds));
        } catch (InterruptedException e) {
          LOG.info("Interrupted. Exiting.");
          return;
        } catch (Exception e) {
          LOG.warn("Failure in status thread", e);
        }
      }
    }
  }

  static class StatusEntry implements Comparable<StatusEntry> {
    private final String _uuid;
    private final String _filename;
    private final long _fileSize;
    private final CountingInputStream _stream;
    private final Date _startDate;

    private long _previousCount;

    StatusEntry(final String pUuid, final String pFilename, final long pFileSize, final CountingInputStream pStream) {
      _uuid = pUuid;
      _filename = pFilename;
      _fileSize = pFileSize;
      _stream = pStream;
      _startDate = new Date();
      _previousCount = 0L;
    }

    public String getUuid() {
      return _uuid;
    }

    public String getFilename() {
      return _filename;
    }

    public long getPreviousCount() {
      return _previousCount;
    }

    public long getAndSetCount() {
      final long count = getCount();
      _previousCount = count;
      return count;
    }

    public long getCount() {
      return _stream.getCount();
    }

    public Date getStartDate() {
      return _startDate;
    }

    public long getFileSize() {
      return _fileSize;
    }

    public double getPercentComplete() {
      return (double)getCount() / (double)getFileSize() * 100.0;
    }

    @Override
    public boolean equals(final Object pO) {
      if (this == pO) {
        return true;
      }
      if (pO == null || getClass() != pO.getClass()) {
        return false;
      }
      final StatusEntry that = (StatusEntry) pO;
      return Objects.equals(_uuid, that._uuid);
    }

    @Override
    public int hashCode() {
      return Objects.hash(_uuid);
    }

    // Descending by start date, ascending by uuid
    @Override
    public int compareTo(final StatusEntry o) {
      if (o == null) {
        return -1;
      }
      final int compare = _startDate.compareTo(o._startDate);
      if (compare != 0) {
        return compare;
      }
      return _uuid.compareTo(o._uuid);
    }

    @Override
    public String toString() {
      return "StatusEntry{" +
          "_uuid='" + _uuid + '\'' +
          ", _filename='" + _filename + '\'' +
          ", _start=" + _startDate +
          ", _previousCount=" + _previousCount +
          '}';
    }
  }

  static class RateLimitedStream extends InputStream {
    private final RateLimiter _limiter;
    private final InputStream _stream;

    RateLimitedStream(final RateLimiter pLimiter, final InputStream pStream) {
      _limiter = pLimiter;
      _stream = pStream;
    }

    @Override
    public int read() throws IOException {
      _limiter.acquire(1);
      return _stream.read();
    }

    @Override
    public int read(final byte[] b) throws IOException {
      _limiter.acquire(b.length);
      return _stream.read(b);
    }

    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
      _limiter.acquire(len);
      return _stream.read(b, off, len);
    }

    @Override
    public long skip(final long n) throws IOException {
      return _stream.skip(n);
    }

    @Override
    public int available() throws IOException {
      return _stream.available();
    }

    @Override
    public void close() throws IOException {
      _stream.close();
    }

    @Override
    public void mark(final int readlimit) {
      _stream.mark(readlimit);
    }

    @Override
    public void reset() throws IOException {
      _stream.reset();
    }

    @Override
    public boolean markSupported() {
      return _stream.markSupported();
    }
    
  }

  public static void main(String[] args) throws Exception {
    // Move the output from JUL to Slf4j
    SLF4JBridgeHandler.removeHandlersForRootLogger();
    SLF4JBridgeHandler.install();

    final LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
    // Required to ensure source method name and line number are correctly attributed for morphia
    // messages.
    loggerContext.getFrameworkPackages().add("dev.morphia.logging");
    loggerContext.getFrameworkPackages().add("com.mongodb.diagnostics.logging");

    // Enable this to debug logging problems.
    // StatusPrinter.print(loggerContext);
    StatusPrinter.printInCaseOfErrorsOrWarnings(loggerContext);

    gridfsIngest(args);
  }
}
