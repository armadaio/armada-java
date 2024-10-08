/*
 * Copyright JAMF Software, LLC
 */

package com.jamf.regatta.core.options;

import com.jamf.regatta.core.api.ByteSequence;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class OptionsUtilTest {

    static void check(byte[] prefix, byte[] expectedPrefixEndOf) {
        ByteSequence actual = OptionsUtil.prefixEndOf(ByteSequence.from(prefix));
        assertThat(actual).isEqualTo(ByteSequence.from(expectedPrefixEndOf));
    }

    @Test
    void aaPlus1() {
        check(new byte[]{(byte) 'a', (byte) 'a'}, new byte[]{(byte) 'a', (byte) 'b'});
    }

    @Test
    void axffPlus1() {
        check(new byte[]{(byte) 'a', (byte) 0xff}, new byte[]{(byte) 'b'});
    }

    @Test
    void xffPlus1() {
        check(new byte[]{(byte) 0xff}, new byte[]{(byte) 0x00});
    }
}
