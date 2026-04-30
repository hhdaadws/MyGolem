package com.mygolem.protection;

public record ProtectionDecision(boolean allowed) {

    public static ProtectionDecision fromChecks(boolean... checks) {
        for (boolean check : checks) {
            if (!check) {
                return new ProtectionDecision(false);
            }
        }
        return new ProtectionDecision(true);
    }
}
