package org.apache.cxf.jaxrs.client;


import java.net.URI;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.random.RandomGenerator;
import java.util.stream.Stream;

import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.junit.Before;
import org.junit.Test;

import jakarta.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class ThreadLocalClientStateTest {

    private static final int STATE_TIME = 1000;

    LocalClientState localClientState;

    ThreadLocalClientState sut;

    @Before
    public void setup() {
        localClientState = randomClientState();
        sut = new ThreadLocalClientState(localClientState, 0);
    }

    @Test
    public void testDelegateMethods() {
        verifyClientState(localClientState);
        var newLcsCurrentThread = randomClientState();
        setClientState(newLcsCurrentThread);
        verifyClientState(newLcsCurrentThread);

        CompletableFuture.runAsync(() -> {
            verifyClientState(localClientState);
            var newLcsOtherThread = randomClientState();
            setClientState(newLcsOtherThread);
            verifyClientState(newLcsOtherThread);
        }).join();
    }

    @Test
    public void resetDropsState() {
        verifyClientState(localClientState);
        var newLcs = randomClientState();
        setClientState(newLcs);
        verifyClientState(newLcs);
        sut.reset();
        verifyClientState(localClientState);
    }

    @Test
    public void resetWithTimeToKeepDropsState() {
        sut.setTimeToKeepState(STATE_TIME);
        resetDropsState();
    }


    @Test
    public void timeToKeepStateDoesNotAffectAlreadyExistingLocalStates() {
        verifyClientState(localClientState);
        var newLcs = randomClientState();
        setClientState(newLcs);
        sut.setTimeToKeepState(STATE_TIME);
        verifyClientState(newLcs);

        // other threads use the old definition
        CompletableFuture.runAsync(() -> verifyClientState(localClientState)).join();
    }

    @Test
    public void resettingTimeToKeepStateDoesNotAffectAlreadyExistingLocalStates() {
        sut.setTimeToKeepState(STATE_TIME);
        verifyClientState(localClientState);
        var newLcs = randomClientState();
        setClientState(newLcs);

        sut.setTimeToKeepState(0);
        verifyClientState(newLcs);

        // other threads use the old definition
        CompletableFuture.runAsync(() -> verifyClientState(localClientState)).join();
    }

    @Test
    public void timeToKeepStateDropsStateAfterTimeout() throws Exception {
        sut.setTimeToKeepState(10);
        verifyClientState(localClientState);
        var newLcs = randomClientState();
        setClientState(newLcs);
        verifyClientState(newLcs);

        Thread.sleep(200);
        // Old data is restored
        verifyClientState(localClientState);
    }

    @Test
    public void newStateCreatesFullThreadLocalCopy() {
        var currentLcs = randomClientState();
        setClientState(currentLcs);
        verifyClientState(currentLcs);

        var newLcs = randomClientState();
        var newState = sut.newState(newLcs.getBaseURI(), newLcs.getRequestHeaders(), newLcs.getTemplates());
        setClientState(newState, newLcs);
        verifyClientState(currentLcs);
        verifyClientState(newState, newLcs);

        var newLcs2 = randomClientState();
        var newState2 = newState.newState(newLcs.getBaseURI(), newLcs.getRequestHeaders(), newLcs.getTemplates(), Map.of());
        setClientState(newState2, newLcs2);
        verifyClientState(currentLcs);
        verifyClientState(newState, newLcs);
        verifyClientState(newState2, newLcs2);
    }

    @Test
    public void newStateWithKeepTimeoutCreatesFullThreadLocalCopy() {
        sut.setTimeToKeepState(STATE_TIME);
        newStateCreatesFullThreadLocalCopy();
    }

    /**
     * Tests that multiple invocations of this class always yield the expected and independent ThreadLocal and that even
     * very many invocations do not take up more memory than necessary (which would result in an OOME). Warning: up to
     * 200MB of Heap are allocated for this test in up to 20 separate chunks.
     */
    @Test
    public void testThreadAndMemorySafety() {
        record TestData(Thread t, byte[] bytes, int id) {
        }
        var testDataList = Stream.generate(() -> {
            var b = new byte[10 * 1024 * 1024];
            RandomGenerator.getDefault().nextBytes(b);
            return new TestData(Thread.currentThread(), b, Arrays.hashCode(b));
        }).parallel().limit(10).toList();
        for (int i = 0; i < 40; i++) {
            testDataList.stream().map(testData -> CompletableFuture.runAsync(() -> {
                sut.setResponse(Response.ok(new TestData(Thread.currentThread(), testData.bytes.clone(), testData.id)).build());
                assertEquals(testData.id, ((TestData) sut.getResponse().getEntity()).id);
            }, r -> new Thread(r).start())).toList().forEach(CompletableFuture::join);
        }
    }

    private LocalClientState randomClientState() {
        var rng = RandomGenerator.getDefault();
        var lcs = new LocalClientState(URI.create("http://host" + rng.nextLong()), Map.of("key" + rng.nextLong(), rng.nextLong()));
        lcs.setRequestHeaders(new MetadataMap<>(Map.of("header" + rng.nextLong(), rng.ints().limit(5).mapToObj(String::valueOf).toList())));
        lcs.setTemplates(new MetadataMap<>(Map.of("template" + rng.nextLong(), rng.ints().limit(5).mapToObj(String::valueOf).toList())));
        lcs.setResponse(Response.ok(rng.nextLong()).build());
        return lcs;
    }

    private void verifyClientState(LocalClientState expected) {
        verifyClientState(sut, expected);
    }

    private static void verifyClientState(ClientState current, LocalClientState expected) {
        var templatesResult = new MetadataMap<>(current.getTemplates());
        templatesResult.putAll(expected.getTemplates());
        assertEquals(expected.getBaseURI(), current.getBaseURI());
        assertEquals(expected.getBaseURI(), current.getCurrentBuilder().build());
        assertEquals(templatesResult, current.getTemplates());
        assertEquals(expected.getRequestHeaders(), current.getRequestHeaders());
        assertSame(expected.getResponse(), current.getResponse());
    }


    private void setClientState(LocalClientState lcs) {
        setClientState(sut, lcs);
    }

    private static void setClientState(ClientState current, LocalClientState lcs) {
        current.setBaseURI(lcs.getBaseURI());
        current.setRequestHeaders(lcs.getRequestHeaders());
        current.setTemplates(lcs.getTemplates());
        current.setResponse(lcs.getResponse());
    }
}
