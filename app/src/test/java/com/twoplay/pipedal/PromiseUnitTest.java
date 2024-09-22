package com.twoplay.pipedal;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class PromiseUnitTest {
    Promise<Integer> asyncGetValue() {
        return new Promise<Integer>((completion)->
        {
            completion.fulfill(7);
        });
    }
    @Test
    public void syntax_isCorrect() {
        final boolean[] wasCalled = new boolean[4];
        asyncGetValue()
        .andThen((v)->
        {
            wasCalled[0] = true;
            assertEquals((int)v,7);
            return (Integer)((int)v+2);
        })
        .andThen((v2)->{
            wasCalled[1] = true;
            assertEquals((int)v2,9);
        }).then((v) -> {
            wasCalled[2] = true;
            return asyncGetValue();
        })
        .andThen((v3)-> {
            wasCalled[3] = true;
            assertEquals((int)v3,7);
        });
        assert(wasCalled[0]);
        assert(wasCalled[1]);
        assert(wasCalled[2]);
        assert(wasCalled[3]);
    }
}