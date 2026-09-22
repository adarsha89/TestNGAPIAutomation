package com.reqres.automation.testdata;

/**
 * One immutable row of {@code row-locked-user-pool.csv} - carries every
 * field needed across all six in-scope endpoints (superset-of-fields
 * approach) so a single claimed row can drive GET/POST/PUT/DELETE/login/
 * register without cross-row lookups. {@code email}/{@code loginPassword}/
 * {@code registerPassword} share the one reqres fixture identity on every
 * row - see {@link UserRowPool}.
 */
public record UserPoolRow(String rowKey, int userId, String name, String job, String email, String loginPassword,
        String registerPassword) {
}
