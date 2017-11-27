package kbasesearchengine.main;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Optional;

import kbasesearchengine.events.EventQueue;
import kbasesearchengine.events.StatusEventProcessingState;
import kbasesearchengine.events.StoredStatusEvent;
import kbasesearchengine.events.exceptions.FatalIndexingException;
import kbasesearchengine.events.exceptions.IndexingException;
import kbasesearchengine.events.exceptions.RetriableIndexingException;
import kbasesearchengine.events.exceptions.Retrier;
import kbasesearchengine.events.storage.StatusEventStorage;
import kbasesearchengine.tools.Utils;

/** Coordinates which events will get processed by {@link IndexerWorker}s based on the
 * {@link EventQueue}. The responsibility of the coordinator is to periodically update the event
 * state in the {@link StatusEventStorage} such that the workers process the correct events.
 * 
 * Only one indexer coordinator should run at one time.
 * 
 * This class is not thread safe.
 * @author gaprice@lbl.gov
 *
 */
public class IndexerCoordinator {
    
    private static final int RETRY_COUNT = 5;
    private static final int RETRY_SLEEP_MS = 1000;
    private static final List<Integer> RETRY_FATAL_BACKOFF_MS_DEFAULT = Arrays.asList(
            1000, 2000, 4000, 8000, 16000);
    
    private final StatusEventStorage storage;
    private final LineLogger logger;
    private final ScheduledExecutorService executor;
    private final EventQueue queue;
    
    private final int maxQueueSize;
    private int continuousCycles = 0;
    
    private final Retrier retrier;

    /** Create the indexer coordinator. Only one coordinator should run at one time.
     * @param storage the storage system containing events.
     * @param logger a logger.
     * @param maximumQueueSize the maximum number of events in the internal in-memory queue.
     * This should be a fairly large number because events may not arrive in the storage system
     * in the ordering of their timestamps, and so the queue acts as a buffer so events can be
     * sorted before processing if an event arrives late.
     * @throws InterruptedException if the thread is interrupted while attempting to initialize
     * the coordinator.
     * @throws IndexingException if an exception occurs while trying to initialize the
     * coordinator.
     */
    public IndexerCoordinator(
            final StatusEventStorage storage,
            final LineLogger logger,
            final int maximumQueueSize)
            throws InterruptedException, IndexingException {
        this(storage, logger, maximumQueueSize, Executors.newSingleThreadScheduledExecutor(),
                RETRY_FATAL_BACKOFF_MS_DEFAULT);
    }
    
    /** Create an indexer coordinator solely for the purposes of testing. This constructor should
     * not be used for any other purpose.
     * @param storage the storage system containing events.
     * @param logger a logger.
     * @param maximumQueueSize the maximum number of events in the internal in-memory queue.
     * @param testExecutor a single thread executor for testing purposes, usually a mock.
     * @throws InterruptedException if the thread is interrupted while attempting to initialize
     * the coordinator.
     * @throws IndexingException if an exception occurs while trying to initialize the
     * coordinator.
     */
    public IndexerCoordinator(
            final StatusEventStorage storage,
            final LineLogger logger,
            final int maximumQueueSize,
            final ScheduledExecutorService testExecutor,
            final List<Integer> retryFatalBackoffMS)
            throws InterruptedException, IndexingException {
        Utils.nonNull(storage, "storage");
        Utils.nonNull(logger, "logger");
        if (maximumQueueSize < 1) {
            throw new IllegalArgumentException("maximumQueueSize must be at least 1");
        }
        this.maxQueueSize = maximumQueueSize;
        this.logger = logger;
        this.storage = storage;
        final List<StoredStatusEvent> all = new LinkedList<>();
        retrier = new Retrier(RETRY_COUNT, RETRY_SLEEP_MS, retryFatalBackoffMS,
                (retrycount, event, except) -> logError(retrycount, event, except));
        all.addAll(retrier.retryFunc(
                s -> s.get(StatusEventProcessingState.READY, maxQueueSize), storage, null));
        all.addAll(retrier.retryFunc(
                s -> s.get(StatusEventProcessingState.PROC, maxQueueSize), storage, null));
        queue = new EventQueue(all);
        executor = testExecutor;
    }
    
    /** Get the maximum size of the in memory queue.
     * @return
     */
    public int getMaximumQueueSize() {
        return maxQueueSize;
    }
    
    /** Start the indexer. */
    public void startIndexer() {
        // may want to make this configurable
        executor.scheduleAtFixedRate(new IndexerRunner(), 0, 1000, TimeUnit.MILLISECONDS);
    }
    
    private class IndexerRunner implements Runnable {

        @Override
        public void run() {
            try {
                runOneCycle();
            } catch (InterruptedException | FatalIndexingException e) {
                logError(ErrorType.FATAL, e);
                executor.shutdown();
            } catch (Throwable e) {
                logError(ErrorType.UNEXPECTED, e);
            }
        }
    }
    
    /** Stop the indexer. The current indexer cycle will complete and the indexer will then
     * process no more events.
     */
    public void stopIndexer() {
        executor.shutdown();
    }
    
    private enum ErrorType {
        FATAL, UNEXPECTED;
    }
    
    private void logError(final ErrorType errtype, final Throwable e) {
        final String msg;
        if (ErrorType.FATAL.equals(errtype)) {
            msg = "Fatal error in indexer, shutting down";
        } else { // has to be UNEXPECTED
            msg = "Unexpected error in indexer";
        }
        logError(msg, e);
    }

    private void logError(final String msg, final Throwable e) {
        // TODO LOG make log method that takes msg + e and have the logger figure out how to log it correctly
        logger.logError(msg + ": " + e);
        logger.logError(e);
    }

    private void logError(
            final int retrycount,
            final Optional<StoredStatusEvent> event,
            final RetriableIndexingException e) {
        final String msg;
        if (event.isPresent()) {
            msg = String.format("Retriable error in indexer for event %s %s, retry %s",
                    event.get().getEvent().getEventType(), event.get().getId().getId(),
                    retrycount);
        } else {
            msg = String.format("Retriable error in indexer, retry %s", retrycount);
        }
        logError(msg, e);
    }
    
    private void runOneCycle() throws InterruptedException, IndexingException {
        /* some of the operations in the submethods could be batched if they prove to be a
         * bottleneck
         * but the mongo client keeps a connection open so it's not that expensive to 
         * run one at a time
         * also the bottleneck is almost assuredly the workers
         */
        continuousCycles = 0;
        //TODO QUEUE check for stalled events
        boolean noWait = true;
        while (noWait) {
            final boolean loadedEvents = loadEventsIntoQueue();
            queue.moveToReady();
            setEventsAsReadyInStorage();
            // so we don't run through the same events again next loop
            queue.moveReadyToProcessing();
            checkOnEventsInProcess();
            // start the cycle immediately if there were events in storage and the queue isn't full
            noWait = loadedEvents && queue.size() < maxQueueSize;
            continuousCycles++;
        }
    }
    

    private boolean loadEventsIntoQueue() throws InterruptedException, IndexingException {
        final boolean loaded;
        final int loadSize = maxQueueSize - queue.size();
        if (loadSize > 0) {
            final List<StoredStatusEvent> events = retrier.retryFunc(
                    s -> s.get(StatusEventProcessingState.UNPROC, loadSize), storage, null);
            events.stream().forEach(e -> queue.load(e));
            loaded = !events.isEmpty();
        } else {
            loaded = false;
        }
        return loaded;
    }

    private void setEventsAsReadyInStorage() throws InterruptedException, IndexingException {
        for (final StoredStatusEvent sse: queue.getReadyForProcessing()) {
            // since the queue doesn't mutate the state, if the state is not UNPROC
            // it's not in that state in the DB either
            if (sse.getState().equals(StatusEventProcessingState.UNPROC)) {
                //TODO QUEUE mark with timestamp
                retrier.retryCons(e -> storage.setProcessingState(e.getId(),
                        StatusEventProcessingState.UNPROC, StatusEventProcessingState.READY),
                        sse, sse);
                logger.logInfo(String.format("Moved event %s %s %s from %s to %s",
                        sse.getId().getId(), sse.getEvent().getEventType(),
                        sse.getEvent().toGUID(), StatusEventProcessingState.UNPROC,
                        StatusEventProcessingState.READY));
            }
        }
    }
    
    private void checkOnEventsInProcess() throws InterruptedException, IndexingException {
        for (final StoredStatusEvent sse: queue.getProcessing()) {
            final Optional<StoredStatusEvent> fromStorage =
                    retrier.retryFunc(s -> s.get(sse.getId()), storage, sse);
            if (fromStorage.isPresent()) {
                final StoredStatusEvent e = fromStorage.get();
                final StatusEventProcessingState state = e.getState();
                if (!state.equals(StatusEventProcessingState.PROC) &&
                        !state.equals(StatusEventProcessingState.READY)) {
                    queue.setProcessingComplete(e);
                    logger.logInfo(String.format(
                            "Event %s %s %s completed processing with state %s",
                            e.getId().getId(), e.getEvent().getEventType(),
                            e.getEvent().toGUID(), state));
                } else {
                    //TODO QUEUE check time since last update and log if > X min (log periodically, maybe 1 /hr)
                }
            } else {
                logger.logError(String.format("Event %s is in the in-memory queue but not " +
                        "in the storage system. Removing from queue", sse.getId().getId()));
                queue.setProcessingComplete(sse);
            }
        }
    }

    /** Returns the number of cycles the indexer has run without pausing (e.g. without the
     * indexer cycle being scheduled). This information is mostly useful for test purposes.
     * @return 
     */
    public int getContinuousCycles() {
        return continuousCycles;
    }
    
    /** Returns the current size of the queue.
     * @return the queue size.
     */
    public int getQueueSize() {
        return queue.size();
    }
}
