package com.mygolem.protection;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProtectionDecisionTest {

    @Test
    void deniesWhenAnyStrictCheckDenies() {
        ProtectionDecision decision = ProtectionDecision.fromChecks(true, false, true);

        assertFalse(decision.allowed());
    }

    @Test
    void allowsWhenAllEnabledChecksAllow() {
        ProtectionDecision decision = ProtectionDecision.fromChecks(true, true, true);

        assertTrue(decision.allowed());
    }
}
