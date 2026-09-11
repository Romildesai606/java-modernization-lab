package dev.romildesai.modernization.modern;

import dev.romildesai.modernization.common.Money;

import java.time.Instant;

/**
 * Sealed hierarchy of ledger events.
 *
 * <p>In Java 8 this was an abstract class plus an {@code EventType} enum plus a
 * chain of {@code instanceof} casts in every handler, with no compiler guarantee
 * that a new subtype was handled everywhere. Sealing the interface turns that
 * into a compile-time exhaustiveness check in {@code switch}.</p>
 */
public sealed interface LedgerEvent
        permits LedgerEvent.Deposit, LedgerEvent.Withdrawal, LedgerEvent.Transfer, LedgerEvent.Reversal {

    String accountId();

    Instant occurredAt();

    record Deposit(String accountId, Money amount, Instant occurredAt) implements LedgerEvent {}

    record Withdrawal(String accountId, Money amount, Instant occurredAt) implements LedgerEvent {}

    record Transfer(String accountId, String counterpartyAccountId, Money amount, Instant occurredAt)
            implements LedgerEvent {}

    record Reversal(String accountId, String reversedEventId, Money amount, Instant occurredAt)
            implements LedgerEvent {}
}
