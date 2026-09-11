package dev.romildesai.modernization.modern;

import dev.romildesai.modernization.common.Money;

import java.util.List;

/**
 * Folds a stream of ledger events into a balance.
 *
 * <p>Java 21: pattern-matching {@code switch} over a sealed interface. There is no
 * {@code default} branch — if a fifth event type is added to {@link LedgerEvent},
 * this file stops compiling instead of silently mis-computing a balance. That
 * property is the whole point of the migration.</p>
 */
public final class BalanceProjector {

    private BalanceProjector() {
    }

    public static Money project(String currencyCode, List<LedgerEvent> events) {
        Money balance = Money.zero(currencyCode);
        for (LedgerEvent event : events) {
            balance = apply(balance, event);
        }
        return balance;
    }

    public static Money apply(Money balance, LedgerEvent event) {
        return switch (event) {
            case LedgerEvent.Deposit d -> balance.plus(d.amount());
            case LedgerEvent.Withdrawal w -> balance.minus(w.amount());
            case LedgerEvent.Transfer t -> balance.minus(t.amount());
            case LedgerEvent.Reversal r -> balance.plus(r.amount());
        };
    }

    /** Guarded patterns replace nested if/else on the unwrapped value. */
    public static String describe(LedgerEvent event) {
        return switch (event) {
            case LedgerEvent.Withdrawal w when w.amount().isNegative() ->
                    "invalid withdrawal (negative amount) on " + w.accountId();
            case LedgerEvent.Withdrawal w -> "withdrawal of " + w.amount() + " from " + w.accountId();
            case LedgerEvent.Deposit d -> "deposit of " + d.amount() + " to " + d.accountId();
            case LedgerEvent.Transfer t ->
                    "transfer of " + t.amount() + " from " + t.accountId() + " to " + t.counterpartyAccountId();
            case LedgerEvent.Reversal r -> "reversal of " + r.reversedEventId() + " on " + r.accountId();
        };
    }
}
