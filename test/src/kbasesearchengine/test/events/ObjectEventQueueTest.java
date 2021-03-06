package kbasesearchengine.test.events;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static kbasesearchengine.test.common.TestCommon.set;

import org.junit.Test;

import com.google.common.base.Optional;

import kbasesearchengine.events.ObjectEventQueue;
import kbasesearchengine.events.exceptions.NoSuchEventException;
import kbasesearchengine.events.StatusEvent;
import kbasesearchengine.events.StatusEventID;
import kbasesearchengine.events.StatusEventProcessingState;
import kbasesearchengine.events.StatusEventType;
import kbasesearchengine.events.StoredStatusEvent;
import kbasesearchengine.test.common.TestCommon;

public class ObjectEventQueueTest {
    
    @Test
    public void isVersionLevelEvent() {
        isVersionLevelEvent(StatusEventType.NEW_VERSION, true);
        
        isVersionLevelEvent(StatusEventType.COPY_ACCESS_GROUP, false);
        isVersionLevelEvent(StatusEventType.DELETE_ACCESS_GROUP, false);
        isVersionLevelEvent(StatusEventType.DELETE_ALL_VERSIONS, false);
        isVersionLevelEvent(StatusEventType.NEW_ALL_VERSIONS, false);
        isVersionLevelEvent(StatusEventType.PUBLISH_ACCESS_GROUP, false);
        isVersionLevelEvent(StatusEventType.PUBLISH_ALL_VERSIONS, false);
        isVersionLevelEvent(StatusEventType.RENAME_ALL_VERSIONS, false);
        isVersionLevelEvent(StatusEventType.UNDELETE_ALL_VERSIONS, false);
        isVersionLevelEvent(StatusEventType.UNPUBLISH_ACCESS_GROUP, false);
        isVersionLevelEvent(StatusEventType.UNPUBLISH_ALL_VERSIONS, false);
    }

    private void isVersionLevelEvent(final StatusEventType type, final boolean expected) {
        final StoredStatusEvent sse = StoredStatusEvent.getBuilder(StatusEvent.getBuilder(
                "bar", Instant.ofEpochMilli(10000), type)
                .build(),
                new StatusEventID("foo"), StatusEventProcessingState.UNPROC).build();
        
        assertThat("incorrect isVersion", ObjectEventQueue.isVersionLevelEvent(sse),
                is(expected));
    }
    
    @Test
    public void isVersionLevelEventFail() {
        try {
            ObjectEventQueue.isVersionLevelEvent(null);
            fail("expected exception");
        } catch (Exception got) {
            TestCommon.assertExceptionCorrect(got, new NullPointerException("event"));
        }
    }

    /* this assert does not mutate the queue state */
    private void assertQueueState(
            final ObjectEventQueue queue,
            final Set<StoredStatusEvent> ready,
            final Set<StoredStatusEvent> processing,
            final int size) {
        assertThat("incorrect ready", queue.getReadyForProcessing(), is(ready));
        assertThat("incorrect hasReady", queue.hasReady(), is(!ready.isEmpty()));
        assertThat("incorrect get processing", queue.getProcessing(), is(processing));
        assertThat("incorrect is processing", queue.isProcessing(), is(!processing.isEmpty()));
        assertThat("incorrect is proc or ready", queue.isProcessingOrReady(),
                is(!processing.isEmpty() || !ready.isEmpty()));
        assertThat("incorrect size", queue.size(), is(size));
        assertThat("incorrect isEmpty", queue.isEmpty(), is(size == 0));
    }
    
    /* this assert does not mutate the queue state */
    private void assertEmpty(final ObjectEventQueue queue) {
        assertQueueState(queue, set(), set(), 0);
        assertMoveToReadyCorrect(queue, set());
        assertMoveToProcessingCorrect(queue, set());
    }
    
    /* note this assert may change the queue state. */
    private void assertMoveToProcessingCorrect(
            final ObjectEventQueue queue,
            final Set<StoredStatusEvent> moveToProcessing) {
        assertThat("incorrect move", queue.moveReadyToProcessing(), is(moveToProcessing));
    }
    
    /* note this assert may change the queue state. */
    private void assertMoveToReadyCorrect(
            final ObjectEventQueue queue,
            final Set<StoredStatusEvent> moveToReady) {
        assertThat("incorrect move", queue.moveToReady(), is(moveToReady));
    }
    
    @Test
    public void constructEmpty() {
        assertEmpty(new ObjectEventQueue());
    }
    
    @Test
    public void loadOneObjectLevelEventAndProcess() {
        final ObjectEventQueue q = new ObjectEventQueue();
        final StoredStatusEvent sse = StoredStatusEvent.getBuilder(StatusEvent.getBuilder(
                "bar", Instant.ofEpochMilli(10000), StatusEventType.DELETE_ALL_VERSIONS)
                .build(),
                new StatusEventID("foo"), StatusEventProcessingState.UNPROC).build();
        
        assertEmpty(q);
        
        q.load(sse);
        assertQueueState(q, set(), set(), 1);
        assertMoveToReadyCorrect(q, set(sse)); //mutates queue
        assertQueueState(q, set(sse), set(), 1);
        assertMoveToProcessingCorrect(q, set(sse)); //mutates queue
        assertQueueState(q, set(), set(sse), 1);
        
        q.setProcessingComplete(sse);
        assertEmpty(q);
    }
    
    @Test
    public void loadMultipleObjectLevelEventsAndProcess() {
        final ObjectEventQueue q = new ObjectEventQueue();
        
        final StoredStatusEvent sse = StoredStatusEvent.getBuilder(StatusEvent.getBuilder(
                "bar", Instant.ofEpochMilli(10000), StatusEventType.DELETE_ALL_VERSIONS)
                .build(),
                new StatusEventID("foo"), StatusEventProcessingState.UNPROC).build();
        final StoredStatusEvent sse1 = StoredStatusEvent.getBuilder(StatusEvent.getBuilder(
                "bar1", Instant.ofEpochMilli(20000), StatusEventType.NEW_ALL_VERSIONS)
                .build(),
                new StatusEventID("foo1"), StatusEventProcessingState.UNPROC).build();
        final StoredStatusEvent sse2 = StoredStatusEvent.getBuilder(StatusEvent.getBuilder(
                "bar2", Instant.ofEpochMilli(30000), StatusEventType.RENAME_ALL_VERSIONS)
                .build(),
                new StatusEventID("foo2"), StatusEventProcessingState.UNPROC).build();
        
        assertEmpty(q);
        
        q.load(sse2);
        q.load(sse1);
        q.load(sse);
        assertQueueState(q, set(), set(), 3);
        assertMoveToReadyCorrect(q, set(sse));
        assertQueueState(q, set(sse), set(), 3);
        //check queue is blocked
        assertMoveToReadyCorrect(q, set());
        assertMoveToProcessingCorrect(q, set(sse));
        assertQueueState(q, set(), set(sse), 3);
        //check queue is blocked
        assertMoveToReadyCorrect(q, set());
        assertMoveToProcessingCorrect(q, set());
        
        q.setProcessingComplete(sse); // calls move to ready
        assertQueueState(q, set(sse1), set(), 2);
        //check queue is blocked
        assertMoveToReadyCorrect(q, set());
        assertMoveToProcessingCorrect(q, set(sse1));
        assertQueueState(q, set(), set(sse1), 2);
        //check queue is blocked
        assertMoveToReadyCorrect(q, set());
        assertMoveToProcessingCorrect(q, set());
        
        q.setProcessingComplete(sse1);
        assertQueueState(q, set(sse2), set(), 1);
        //check queue is blocked
        assertMoveToReadyCorrect(q, set());
        assertMoveToProcessingCorrect(q, set(sse2));
        assertQueueState(q, set(), set(sse2), 1);
        //check queue is blocked
        assertMoveToReadyCorrect(q, set());
        assertMoveToProcessingCorrect(q, set());
        
        q.setProcessingComplete(sse2);
        assertEmpty(q);
    }
    
    
    @Test
    public void loadOneVersionLevelEventAndProcess() {
        final ObjectEventQueue q = new ObjectEventQueue();
        
        final StoredStatusEvent sse = StoredStatusEvent.getBuilder(StatusEvent.getBuilder(
                "bar", Instant.ofEpochMilli(10000), StatusEventType.NEW_VERSION)
                .build(),
                new StatusEventID("foo"), StatusEventProcessingState.UNPROC).build();
        
        assertEmpty(q);
        
        q.load(sse);
        assertQueueState(q, set(), set(), 1);
        assertMoveToReadyCorrect(q, set(sse));
        assertQueueState(q, set(sse), set(), 1);
        assertMoveToProcessingCorrect(q, set(sse));
        assertQueueState(q, set(), set(sse), 1);
        
        q.setProcessingComplete(sse);
        assertEmpty(q);
    }
    
    @Test
    public void loadMultipleVersionLevelEventsAndProcess() {
        final ObjectEventQueue q = new ObjectEventQueue();
        
        final StoredStatusEvent sse = StoredStatusEvent.getBuilder(StatusEvent.getBuilder(
                "bar", Instant.ofEpochMilli(10000), StatusEventType.NEW_VERSION)
                .build(),
                new StatusEventID("foo"), StatusEventProcessingState.UNPROC).build();
        final StoredStatusEvent sse1 = StoredStatusEvent.getBuilder(StatusEvent.getBuilder(
                "bar1", Instant.ofEpochMilli(20000), StatusEventType.NEW_VERSION)
                .build(),
                new StatusEventID("foo1"), StatusEventProcessingState.UNPROC).build();
        final StoredStatusEvent sse2 = StoredStatusEvent.getBuilder(StatusEvent.getBuilder(
                "bar2", Instant.ofEpochMilli(30000), StatusEventType.NEW_VERSION)
                .build(),
                new StatusEventID("foo2"), StatusEventProcessingState.UNPROC).build();
        
        assertEmpty(q);
        
        q.load(sse2);
        q.load(sse);
        assertQueueState(q, set(), set(), 2);
        assertMoveToReadyCorrect(q, set(sse, sse2)); //mutates queue
        assertQueueState(q, set(sse, sse2), set(), 2);
        assertMoveToProcessingCorrect(q, set(sse, sse2)); //mutates queue
        assertQueueState(q, set(), set(sse, sse2), 2);
        
        q.load(sse1);
        assertQueueState(q, set(), set(sse, sse2), 3);
        
        q.setProcessingComplete(sse2); // calls move to ready
        assertQueueState(q, set(sse1), set(sse), 2);
        assertMoveToReadyCorrect(q, set()); // does not mutate queue
        assertQueueState(q, set(sse1), set(sse), 2);
        assertMoveToProcessingCorrect(q, set(sse1)); //mutates queue
        assertQueueState(q, set(), set(sse, sse1), 2);

        q.setProcessingComplete(sse1);
        assertQueueState(q, set(), set(sse), 1);

        q.setProcessingComplete(sse);
        assertEmpty(q);
    }
    
    @Test
    public void blockVersionEventsWithObjectLevelEvent() {
        final ObjectEventQueue q = new ObjectEventQueue();
        
        final StoredStatusEvent sse = StoredStatusEvent.getBuilder(StatusEvent.getBuilder(
                "bar", Instant.ofEpochMilli(10000), StatusEventType.NEW_VERSION)
                .build(),
                new StatusEventID("foo"), StatusEventProcessingState.UNPROC).build();
        final StoredStatusEvent sse1 = StoredStatusEvent.getBuilder(StatusEvent.getBuilder(
                "bar1", Instant.ofEpochMilli(20000), StatusEventType.NEW_VERSION)
                .build(),
                new StatusEventID("foo1"), StatusEventProcessingState.UNPROC).build();
        final StoredStatusEvent sse2 = StoredStatusEvent.getBuilder(StatusEvent.getBuilder(
                "bar2", Instant.ofEpochMilli(30000), StatusEventType.NEW_VERSION)
                .build(),
                new StatusEventID("foo2"), StatusEventProcessingState.UNPROC).build();
        final StoredStatusEvent sse3 = StoredStatusEvent.getBuilder(StatusEvent.getBuilder(
                "bar3", Instant.ofEpochMilli(40000), StatusEventType.NEW_VERSION)
                .build(),
                new StatusEventID("foo4"), StatusEventProcessingState.UNPROC).build();
        
        final StoredStatusEvent blocking = StoredStatusEvent.getBuilder(StatusEvent.getBuilder(
                "blocker", Instant.ofEpochMilli(25000), StatusEventType.DELETE_ALL_VERSIONS)
                .build(),
                new StatusEventID("blk"), StatusEventProcessingState.UNPROC).build();
        
        assertEmpty(q);
        
        q.load(sse3);
        q.load(sse2);
        q.load(sse1);
        q.load(sse);
        q.load(blocking);
        assertQueueState(q, set(), set(), 5);
        assertMoveToReadyCorrect(q, set(sse, sse1));
        assertQueueState(q, set(sse, sse1), set(), 5);
        // check the queue is now blocked
        assertMoveToReadyCorrect(q, set());
        assertQueueState(q, set(sse, sse1), set(), 5);
        assertMoveToProcessingCorrect(q, set(sse, sse1));
        assertQueueState(q, set(), set(sse, sse1), 5);
        // check queue is still blocked
        assertMoveToReadyCorrect(q, set());
        assertMoveToProcessingCorrect(q, set());
        
        q.setProcessingComplete(sse1);
        assertQueueState(q, set(), set(sse), 4);
        // check queue is still blocked
        assertMoveToReadyCorrect(q, set());
        assertMoveToProcessingCorrect(q, set());
        
        q.setProcessingComplete(sse); // calls move to ready
        assertQueueState(q, set(blocking), set(), 3);
        // check queue is blocked
        assertMoveToReadyCorrect(q, set());
        assertQueueState(q, set(blocking), set(), 3);
        assertMoveToProcessingCorrect(q, set(blocking));
        assertQueueState(q, set(), set(blocking), 3);
        //check queue is still blocked
        assertMoveToReadyCorrect(q, set());
        assertMoveToProcessingCorrect(q, set());
        assertQueueState(q, set(), set(blocking), 3);
        
        q.setProcessingComplete(blocking); // calls move to ready
        assertQueueState(q, set(sse2, sse3), set(), 2);
        assertMoveToReadyCorrect(q, set());
        assertMoveToProcessingCorrect(q, set(sse2, sse3));
        assertQueueState(q, set(), set(sse2, sse3), 2);
        
        q.setProcessingComplete(sse2);
        q.setProcessingComplete(sse3);
        assertEmpty(q);
    }
    
    @Test
    public void constructWithReadyObjectLevelEvent() {
        for (final StatusEventType type: Arrays.asList(StatusEventType.DELETE_ALL_VERSIONS,
                StatusEventType.NEW_ALL_VERSIONS, StatusEventType.PUBLISH_ALL_VERSIONS,
                StatusEventType.RENAME_ALL_VERSIONS, StatusEventType.UNDELETE_ALL_VERSIONS)) {
            assertSingleObjectLevelReadyEvent(type);
            
        }
    }

    private void assertSingleObjectLevelReadyEvent(final StatusEventType type) {
        final StoredStatusEvent sse = StoredStatusEvent.getBuilder(StatusEvent.getBuilder(
                "bar", Instant.ofEpochMilli(10000), type)
                .build(),
                new StatusEventID("foo"), StatusEventProcessingState.READY).build();
        final ObjectEventQueue q = new ObjectEventQueue(sse);
        assertQueueState(q, set(sse), set(), 1);
    }
    
    @Test
    public void constructWithProcessingObjectLevelEvent() {
        for (final StatusEventType type: Arrays.asList(StatusEventType.DELETE_ALL_VERSIONS,
                StatusEventType.NEW_ALL_VERSIONS, StatusEventType.PUBLISH_ALL_VERSIONS,
                StatusEventType.RENAME_ALL_VERSIONS, StatusEventType.UNDELETE_ALL_VERSIONS)) {
            assertSingleObjectLevelProcessingEvent(type);
            
        }
    }
    
    private void assertSingleObjectLevelProcessingEvent(final StatusEventType type) {
        final StoredStatusEvent sse = StoredStatusEvent.getBuilder(StatusEvent.getBuilder(
                "bar", Instant.ofEpochMilli(10000), type)
                .build(),
                new StatusEventID("foo"), StatusEventProcessingState.PROC).build();
        final ObjectEventQueue q = new ObjectEventQueue(sse);
        assertQueueState(q, set(), set(sse), 1);
    }
    
    @Test
    public void constructWithVersionLevelEvents() {
        final StoredStatusEvent sse = StoredStatusEvent.getBuilder(StatusEvent.getBuilder(
                "bar", Instant.ofEpochMilli(10000), StatusEventType.NEW_VERSION)
                .build(),
                new StatusEventID("foo"), StatusEventProcessingState.READY).build();
        final StoredStatusEvent sse1 = StoredStatusEvent.getBuilder(StatusEvent.getBuilder(
                "bar1", Instant.ofEpochMilli(20000), StatusEventType.NEW_VERSION)
                .build(),
                new StatusEventID("foo1"), StatusEventProcessingState.READY).build();
        final StoredStatusEvent sse2 = StoredStatusEvent.getBuilder(StatusEvent.getBuilder(
                "bar2", Instant.ofEpochMilli(30000), StatusEventType.NEW_VERSION)
                .build(),
                new StatusEventID("foo2"), StatusEventProcessingState.PROC).build();
        final StoredStatusEvent sse3 = StoredStatusEvent.getBuilder(StatusEvent.getBuilder(
                "bar3", Instant.ofEpochMilli(40000), StatusEventType.NEW_VERSION)
                .build(),
                new StatusEventID("foo4"), StatusEventProcessingState.PROC).build();
        
        assertEmpty(new ObjectEventQueue(new LinkedList<>(), new LinkedList<>()));
        
        final ObjectEventQueue q1 = new ObjectEventQueue(
                Arrays.asList(sse1), new LinkedList<>());
        assertQueueState(q1, set(sse1), set(), 1);
        
        final ObjectEventQueue q2 = new ObjectEventQueue(
                Arrays.asList(sse, sse1), new LinkedList<>());
        assertQueueState(q2, set(sse, sse1), set(), 2);
        
        final ObjectEventQueue q3 = new ObjectEventQueue(
                new LinkedList<>(), Arrays.asList(sse2));
        assertQueueState(q3, set(), set(sse2), 1);
        
        final ObjectEventQueue q4 = new ObjectEventQueue(
                new LinkedList<>(), Arrays.asList(sse2, sse3));
        assertQueueState(q4, set(), set(sse2, sse3), 2);
        
        final ObjectEventQueue q5 = new ObjectEventQueue(
                Arrays.asList(sse1), Arrays.asList(sse2));
        assertQueueState(q5, set(sse1), set(sse2), 2);
        
        final ObjectEventQueue q6 = new ObjectEventQueue(
                Arrays.asList(sse1, sse), Arrays.asList(sse2, sse3));
        assertQueueState(q6, set(sse1, sse), set(sse2, sse3), 4);
    }
    
    @Test
    public void setProcessedWithMutatedEvent() {
        // in practice we expect the events passed into setProcessed() to have mutated slightly
        // from the original load()ed event, so check that works.
        // the status event itself and the id should not mutate, but other fields are fair game.
        
        final StoredStatusEvent sse = StoredStatusEvent.getBuilder(StatusEvent.getBuilder(
                "bar", Instant.ofEpochMilli(10000), StatusEventType.NEW_VERSION)
                .build(),
                new StatusEventID("foo"), StatusEventProcessingState.UNPROC).build();
        
        final ObjectEventQueue q = new ObjectEventQueue();
        q.load(sse);
        q.moveToReady();
        q.moveReadyToProcessing();
        assertQueueState(q, set(), set(sse), 1);
        
        final StoredStatusEvent hideousmutant = StoredStatusEvent.getBuilder(
                StatusEvent.getBuilder("bar", Instant.ofEpochMilli(10000),
                        StatusEventType.NEW_VERSION).build(),
                new StatusEventID("foo"), StatusEventProcessingState.INDX)
                .withNullableUpdate(Instant.ofEpochMilli(10000), "whee")
                .build();
        
        q.setProcessingComplete(hideousmutant);
        assertEmpty(q);
    }
    
    @Test
    public void immutableGetReady() {
        // test both getReady paths
        final StoredStatusEvent sse = StoredStatusEvent.getBuilder(StatusEvent.getBuilder(
                "bar", Instant.ofEpochMilli(10000), StatusEventType.DELETE_ALL_VERSIONS)
                .build(),
                new StatusEventID("foo"), StatusEventProcessingState.READY).build();
        final StoredStatusEvent sse2 = StoredStatusEvent.getBuilder(StatusEvent.getBuilder(
                "bar2", Instant.ofEpochMilli(10000), StatusEventType.NEW_VERSION)
                .build(),
                new StatusEventID("foo"), StatusEventProcessingState.READY).build();
        
        final ObjectEventQueue q = new ObjectEventQueue(sse);
        assertGetReadyReturnIsImmutable(sse2, q);
        
        final ObjectEventQueue q2 = new ObjectEventQueue(Arrays.asList(sse2), new LinkedList<>());
        assertGetReadyReturnIsImmutable(sse, q2);
    }

    private void assertGetReadyReturnIsImmutable(
            final StoredStatusEvent sse,
            final ObjectEventQueue q) {
        try {
            q.getReadyForProcessing().add(sse);
            fail("expected exception");
        } catch (UnsupportedOperationException e) {
            //test passed
        }
    }
    
    @Test
    public void immutableGetProcessing() {
        // test both getProcessing paths
        final StoredStatusEvent sse = StoredStatusEvent.getBuilder(StatusEvent.getBuilder(
                "bar", Instant.ofEpochMilli(10000), StatusEventType.DELETE_ALL_VERSIONS)
                .build(),
                new StatusEventID("foo"), StatusEventProcessingState.PROC).build();
        final StoredStatusEvent sse2 = StoredStatusEvent.getBuilder(StatusEvent.getBuilder(
                "bar2", Instant.ofEpochMilli(10000), StatusEventType.NEW_VERSION)
                .build(),
                new StatusEventID("foo"), StatusEventProcessingState.PROC).build();
        
        final ObjectEventQueue q = new ObjectEventQueue(sse);
        assertGetProcessingReturnIsImmutable(sse2, q);
        
        final ObjectEventQueue q2 = new ObjectEventQueue(new LinkedList<>(), Arrays.asList(sse2));
        assertGetProcessingReturnIsImmutable(sse, q2);
    }

    private void assertGetProcessingReturnIsImmutable(
            final StoredStatusEvent sse,
            final ObjectEventQueue q) {
        try {
            q.getProcessing().add(sse);
            fail("expected exception");
        } catch (UnsupportedOperationException e) {
            //test passed
        }
    }
    
    @Test
    public void immutableMoveReady() {
        // test both moveReady paths
        final StoredStatusEvent sse = StoredStatusEvent.getBuilder(StatusEvent.getBuilder(
                "bar", Instant.ofEpochMilli(10000), StatusEventType.DELETE_ALL_VERSIONS)
                .build(),
                new StatusEventID("foo"), StatusEventProcessingState.UNPROC).build();
        final StoredStatusEvent sse2 = StoredStatusEvent.getBuilder(StatusEvent.getBuilder(
                "bar2", Instant.ofEpochMilli(10000), StatusEventType.NEW_VERSION)
                .build(),
                new StatusEventID("foo"), StatusEventProcessingState.UNPROC).build();
        
        final ObjectEventQueue q = new ObjectEventQueue();
        q.load(sse);
        assertMoveReadyReturnIsImmutable(sse2, q);
        
        final ObjectEventQueue q2 = new ObjectEventQueue();
        q2.load(sse2);
        assertMoveReadyReturnIsImmutable(sse, q2);
        
        final ObjectEventQueue q3 = new ObjectEventQueue();
        q3.load(sse);
        q3.moveToReady();
        assertMoveReadyReturnIsImmutable(sse2, q3);
    }

    private void assertMoveReadyReturnIsImmutable(
            final StoredStatusEvent sse,
            final ObjectEventQueue q) {
        try {
            q.moveToReady().add(sse);
            fail("expected exception");
        } catch (UnsupportedOperationException e) {
            // test passed
        }
    }
    
    @Test
    public void immutableMoveProcessing() {
        // test both moveProcessing paths
        final StoredStatusEvent sse = StoredStatusEvent.getBuilder(StatusEvent.getBuilder(
                "bar", Instant.ofEpochMilli(10000), StatusEventType.DELETE_ALL_VERSIONS)
                .build(),
                new StatusEventID("foo"), StatusEventProcessingState.READY).build();
        final StoredStatusEvent sse2 = StoredStatusEvent.getBuilder(StatusEvent.getBuilder(
                "bar2", Instant.ofEpochMilli(10000), StatusEventType.NEW_VERSION)
                .build(),
                new StatusEventID("foo"), StatusEventProcessingState.READY).build();
        
        final ObjectEventQueue q = new ObjectEventQueue(sse);
        assertMoveProcessingReturnIsImmutable(sse2, q);
        
        final ObjectEventQueue q2 = new ObjectEventQueue(Arrays.asList(sse2), new LinkedList<>());
        assertMoveProcessingReturnIsImmutable(sse, q2);
    }

    private void assertMoveProcessingReturnIsImmutable(
            final StoredStatusEvent sse,
            final ObjectEventQueue q) {
        try {
            q.moveReadyToProcessing().add(sse);
            fail("expected exception");
        } catch (UnsupportedOperationException e) {
            // test passed
        }
    }
    
    @Test
    public void constructFailWithVersionLevelEvents() {
        final StatusEvent se = StatusEvent.getBuilder(
                "foo", Instant.ofEpochMilli(10000), StatusEventType.NEW_VERSION).build();
        final StatusEventID id = new StatusEventID("some id");
        final List<StoredStatusEvent> mt = new LinkedList<>();
        
        // nulls
        failConstructWithVersionLevelEvents(null, mt, new NullPointerException("initialReady"));
        failConstructWithVersionLevelEvents(mt, null,
                new NullPointerException("initialProcessing"));
        
        // bad status
        for (final StatusEventProcessingState state: Arrays.asList(
                StatusEventProcessingState.FAIL, StatusEventProcessingState.INDX,
                StatusEventProcessingState.UNINDX, StatusEventProcessingState.UNPROC)) {
            failConstructWithVersionLevelEvents(
                    Arrays.asList(StoredStatusEvent.getBuilder(se, id, state).build()),
                    new LinkedList<>(),
                    new IllegalArgumentException("Illegal initial event state: " + state));
            failConstructWithVersionLevelEvents(new LinkedList<>(),
                    Arrays.asList(StoredStatusEvent.getBuilder(se, id, state).build()),
                    new IllegalArgumentException("Illegal initial event state: " + state));
        }
        
        failConstructWithVersionLevelEvents(
                Arrays.asList(StoredStatusEvent.getBuilder(
                        se, id, StatusEventProcessingState.PROC).build()),
                new LinkedList<>(),
                new IllegalArgumentException("Illegal initial event state: PROC"));
        failConstructWithVersionLevelEvents(new LinkedList<>(),
                Arrays.asList(StoredStatusEvent.getBuilder(
                        se, id, StatusEventProcessingState.READY).build()),
                new IllegalArgumentException("Illegal initial event state: READY"));
        
        // bad event types
        for (final StatusEventType type: Arrays.asList(
                StatusEventType.COPY_ACCESS_GROUP, StatusEventType.DELETE_ACCESS_GROUP,
                StatusEventType.DELETE_ALL_VERSIONS, StatusEventType.NEW_ALL_VERSIONS,
                StatusEventType.PUBLISH_ACCESS_GROUP, StatusEventType.PUBLISH_ALL_VERSIONS,
                StatusEventType.RENAME_ALL_VERSIONS, StatusEventType.UNDELETE_ALL_VERSIONS,
                StatusEventType.UNPUBLISH_ACCESS_GROUP, StatusEventType.UNPUBLISH_ALL_VERSIONS)) {
            final StoredStatusEvent setype = StoredStatusEvent.getBuilder(StatusEvent.getBuilder(
                    "foo", Instant.ofEpochMilli(10000), type).build(),
                    id, StatusEventProcessingState.READY).build();
            failConstructWithVersionLevelEvents(Arrays.asList(setype), new LinkedList<>(),
                    new IllegalArgumentException("Illegal initial event type: " + type));
            final StoredStatusEvent setype2 = StoredStatusEvent.getBuilder(StatusEvent.getBuilder(
                    "foo", Instant.ofEpochMilli(10000), type).build(),
                    id, StatusEventProcessingState.PROC).build();
            failConstructWithVersionLevelEvents(new LinkedList<>(), Arrays.asList(setype2),
                    new IllegalArgumentException("Illegal initial event type: " + type));
        }
    }
    
    private void failConstructWithVersionLevelEvents(
            final List<StoredStatusEvent> ready,
            final List<StoredStatusEvent> processing,
            final Exception expected) {
        try {
            new ObjectEventQueue(ready, processing);
            fail("expected exception");
        } catch (Exception got) {
            TestCommon.assertExceptionCorrect(got, expected);
        }
    }
    
    @Test
    public void constructFailWithObjectLevelEvent() {
        final StatusEvent se = StatusEvent.getBuilder(
                "foo", Instant.ofEpochMilli(10000), StatusEventType.DELETE_ALL_VERSIONS).build();
        final StatusEventID id = new StatusEventID("some id");
        
        failConstructWithObjectLevelEvent(null, new NullPointerException("initialEvent"));
        
        // bad status
        for (final StatusEventProcessingState state: Arrays.asList(
                StatusEventProcessingState.FAIL, StatusEventProcessingState.INDX,
                StatusEventProcessingState.UNINDX, StatusEventProcessingState.UNPROC)) {
            failConstructWithObjectLevelEvent(StoredStatusEvent.getBuilder(se, id, state).build(),
                    new IllegalArgumentException("Illegal initial event state: " + state));
        }
        
        // bad event types
        for (final StatusEventType type: Arrays.asList(
                StatusEventType.COPY_ACCESS_GROUP, StatusEventType.DELETE_ACCESS_GROUP,
                StatusEventType.NEW_VERSION, StatusEventType.PUBLISH_ACCESS_GROUP)) {
            final StoredStatusEvent setype = StoredStatusEvent.getBuilder(StatusEvent.getBuilder(
                    "foo", Instant.ofEpochMilli(10000), type).build(),
                    id, StatusEventProcessingState.READY).build();
            failConstructWithObjectLevelEvent(setype,
                    new IllegalArgumentException("Illegal initial event type: " + type));
        }
        
    }
    
    private void failConstructWithObjectLevelEvent(
            final StoredStatusEvent event,
            final Exception expected) {
        try {
            new ObjectEventQueue(event);
            fail("expected exception");
        } catch (Exception got) {
            TestCommon.assertExceptionCorrect(got, expected);
        }
    }
    
    @Test
    public void loadFail() {
        final StatusEvent se = StatusEvent.getBuilder(
                "foo", Instant.ofEpochMilli(10000), StatusEventType.DELETE_ALL_VERSIONS).build();
        final StatusEventID id = new StatusEventID("some id");
        
        // null
        failLoad(null, new NullPointerException("event"));
        
        //bad state
        for (final StatusEventProcessingState state: Arrays.asList(
                StatusEventProcessingState.FAIL, StatusEventProcessingState.INDX,
                StatusEventProcessingState.UNINDX, StatusEventProcessingState.READY,
                StatusEventProcessingState.PROC)) {
            failLoad(StoredStatusEvent.getBuilder(se, id, state).build(),
                    new IllegalArgumentException("Illegal state for loading event: " + state));
        }
        
        // bad event types
        for (final StatusEventType type: Arrays.asList(
                StatusEventType.COPY_ACCESS_GROUP, StatusEventType.DELETE_ACCESS_GROUP,
                StatusEventType.PUBLISH_ACCESS_GROUP)) {
            final StoredStatusEvent setype = StoredStatusEvent.getBuilder(StatusEvent.getBuilder(
                    "foo", Instant.ofEpochMilli(10000), type).build(),
                    id, StatusEventProcessingState.UNPROC).build();
            failLoad(setype,
                    new IllegalArgumentException("Illegal type for loading event: " + type));
        }
    }
    
    private void failLoad(final StoredStatusEvent sse, final Exception expected) {
        try {
            new ObjectEventQueue().load(sse);
            fail("expected exception");
        } catch (Exception got) {
            TestCommon.assertExceptionCorrect(got, expected);
        }
    }
    
    @Test
    public void setProcessingCompleteFail() {
        final ObjectEventQueue q = new ObjectEventQueue();
        final StoredStatusEvent sse = StoredStatusEvent.getBuilder(StatusEvent.getBuilder(
                "bar", Instant.ofEpochMilli(10000), StatusEventType.DELETE_ALL_VERSIONS)
                .build(),
                new StatusEventID("foo"), StatusEventProcessingState.READY).build();
        
        //nulls
        failSetProcessingComplete(q, null, new NullPointerException("event"));
        
        //empty queue
        failSetProcessingComplete(q, sse, new NoSuchEventException(sse));
        
        // with object level event in processed state
        final ObjectEventQueue q2 = new ObjectEventQueue(StoredStatusEvent.getBuilder(
                StatusEvent.getBuilder(
                        "bar", Instant.ofEpochMilli(10000), StatusEventType.DELETE_ALL_VERSIONS)
                        .build(),
                        new StatusEventID("foo2"), StatusEventProcessingState.PROC).build());
        failSetProcessingComplete(q2, sse, new NoSuchEventException(sse));
        
        // with version level event in processed state
        final ObjectEventQueue q3 = new ObjectEventQueue(new LinkedList<>(), Arrays.asList(
                StoredStatusEvent.getBuilder(StatusEvent.getBuilder(
                        "bar", Instant.ofEpochMilli(10000), StatusEventType.NEW_VERSION)
                        .build(),
                        new StatusEventID("foo2"), StatusEventProcessingState.PROC).build()));
        failSetProcessingComplete(q3, sse, new NoSuchEventException(sse));
    }
    
    private void failSetProcessingComplete(
            final ObjectEventQueue queue,
            final StoredStatusEvent sse,
            final Exception expected) {
        try {
            queue.setProcessingComplete(sse);
            fail("expected exception");
        } catch (Exception got) {
            TestCommon.assertExceptionCorrect(got, expected);
        }
    }
    
    @Test
    public void setGetAndRemoveBlock() {
        final ObjectEventQueue q = new ObjectEventQueue();
        
        assertThat("incorrect block time", q.getBlockTime(), is(Optional.absent()));
        q.removeBlock(); // noop
        assertThat("incorrect block time", q.getBlockTime(), is(Optional.absent()));
        
        q.drainAndBlockAt(Instant.ofEpochMilli(10000));
        assertThat("incorrect block time", q.getBlockTime(),
                is(Optional.of(Instant.ofEpochMilli(10000))));
        
        q.removeBlock();
        assertThat("incorrect block time", q.getBlockTime(), is(Optional.absent()));
    }
    
    @Test
    public void drainAndBlockAtFail() {
        final ObjectEventQueue q = new ObjectEventQueue();
        try {
            q.drainAndBlockAt(null);
            fail("expected exception");
        } catch (Exception got) {
            TestCommon.assertExceptionCorrect(got, new NullPointerException("blockTime"));
        }
    }
    
    @Test
    public void drainAndBlockVersionLevelEventsWithEventInReadyAndNoDrain() {
        final ObjectEventQueue q = new ObjectEventQueue();
        
        final StoredStatusEvent sse = StoredStatusEvent.getBuilder(StatusEvent.getBuilder(
                "bar", Instant.ofEpochMilli(10000), StatusEventType.NEW_VERSION)
                .build(),
                new StatusEventID("foo"), StatusEventProcessingState.UNPROC).build();
        final StoredStatusEvent sse1 = StoredStatusEvent.getBuilder(StatusEvent.getBuilder(
                "bar1", Instant.ofEpochMilli(20000), StatusEventType.NEW_VERSION)
                .build(),
                new StatusEventID("foo1"), StatusEventProcessingState.UNPROC).build();
        final StoredStatusEvent sse2 = StoredStatusEvent.getBuilder(StatusEvent.getBuilder(
                "bar2", Instant.ofEpochMilli(30000), StatusEventType.NEW_VERSION)
                .build(),
                new StatusEventID("foo2"), StatusEventProcessingState.UNPROC).build();
        
        assertEmpty(q);
        
        q.load(sse);
        assertMoveToReadyCorrect(q, set(sse));
        q.load(sse1);
        q.load(sse2);
        assertQueueState(q, set(sse), set(), 3);
        q.drainAndBlockAt(Instant.ofEpochMilli(5000));
        assertMoveToReadyCorrect(q, set());
        assertQueueState(q, set(sse), set(), 3);
        assertMoveToProcessingCorrect(q, set(sse));
        assertQueueState(q, set(), set(sse), 3);
        q.setProcessingComplete(sse);
        assertQueueState(q, set(), set(), 2);
        assertMoveToReadyCorrect(q, set());
        assertQueueState(q, set(), set(), 2);
        
        q.removeBlock();
        assertMoveToReadyCorrect(q, set(sse2, sse1));
        assertQueueState(q, set(sse1, sse2), set(), 2);
    }
    
    @Test
    public void drainAndBlockVersionLevelEventsWithDrain() {
        final ObjectEventQueue q = new ObjectEventQueue();
        
        final StoredStatusEvent sse = StoredStatusEvent.getBuilder(StatusEvent.getBuilder(
                "bar", Instant.ofEpochMilli(10000), StatusEventType.NEW_VERSION)
                .build(),
                new StatusEventID("foo"), StatusEventProcessingState.UNPROC).build();
        final StoredStatusEvent sse1 = StoredStatusEvent.getBuilder(StatusEvent.getBuilder(
                "bar1", Instant.ofEpochMilli(20000), StatusEventType.NEW_VERSION)
                .build(),
                new StatusEventID("foo1"), StatusEventProcessingState.UNPROC).build();
        final StoredStatusEvent sse2 = StoredStatusEvent.getBuilder(StatusEvent.getBuilder(
                "bar2", Instant.ofEpochMilli(30000), StatusEventType.NEW_VERSION)
                .build(),
                new StatusEventID("foo2"), StatusEventProcessingState.UNPROC).build();
        
        assertEmpty(q);
        
        q.load(sse2);
        q.load(sse);
        q.load(sse1);
        q.drainAndBlockAt(Instant.ofEpochMilli(25000));
        assertMoveToReadyCorrect(q, set(sse, sse1));
        assertQueueState(q, set(sse, sse1), set(), 3);
        assertMoveToProcessingCorrect(q, set(sse, sse1));
        assertQueueState(q, set(), set(sse, sse1), 3);
        q.removeBlock();
        assertMoveToReadyCorrect(q, set(sse2));
        assertQueueState(q, set(sse2), set(sse, sse1), 3);
    }
    
    @Test
    public void drainAndBlockObjectLevelEvents() {
        final ObjectEventQueue q = new ObjectEventQueue();
        
        final StoredStatusEvent sse = StoredStatusEvent.getBuilder(StatusEvent.getBuilder(
                "bar", Instant.ofEpochMilli(10000), StatusEventType.DELETE_ALL_VERSIONS)
                .build(),
                new StatusEventID("foo"), StatusEventProcessingState.UNPROC).build();
        final StoredStatusEvent sse1 = StoredStatusEvent.getBuilder(StatusEvent.getBuilder(
                "bar1", Instant.ofEpochMilli(20000), StatusEventType.NEW_ALL_VERSIONS)
                .build(),
                new StatusEventID("foo1"), StatusEventProcessingState.UNPROC).build();
        final StoredStatusEvent sse2 = StoredStatusEvent.getBuilder(StatusEvent.getBuilder(
                "bar2", Instant.ofEpochMilli(30000), StatusEventType.RENAME_ALL_VERSIONS)
                .build(),
                new StatusEventID("foo2"), StatusEventProcessingState.UNPROC).build();
        
        assertEmpty(q);
        
        q.load(sse2);
        q.load(sse1);
        q.load(sse);
        
        q.drainAndBlockAt(Instant.ofEpochMilli(25000));
        
        assertMoveToReadyCorrect(q, set(sse));
        assertMoveToProcessingCorrect(q, set(sse));
        assertQueueState(q, set(), set(sse), 3);
        q.setProcessingComplete(sse); // moves next to ready
        assertQueueState(q, set(sse1), set(), 2);
        
        assertMoveToReadyCorrect(q, set());
        assertMoveToProcessingCorrect(q, set(sse1));
        q.setProcessingComplete(sse1);
        assertQueueState(q, set(), set(), 1);
        
        assertMoveToReadyCorrect(q, set()); // queue blocked
        assertMoveToProcessingCorrect(q, set());
        
        q.removeBlock();
        assertMoveToReadyCorrect(q, set(sse2));
        assertMoveToProcessingCorrect(q, set(sse2));
        q.setProcessingComplete(sse2);
        assertEmpty(q);
    }
}
